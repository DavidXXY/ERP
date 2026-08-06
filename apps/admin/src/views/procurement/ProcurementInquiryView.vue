<template>
  <div class="page-stack">
    <a-card title="询价管理">
      <template #extra>
        <a-space>
          <a-button @click="router.push('/procurement/purchase-pool')">
            待采购清单
          </a-button>
          <a-button :loading="loading" @click="load">刷新</a-button>
        </a-space>
      </template>

      <a-space class="table-toolbar">
        <a-button
          v-if="auth.can('procurement:purchase:create')"
          type="primary"
          @click="inquiryOpen = true"
        >
          发起单项询价
        </a-button>
      </a-space>

      <a-table
        :data-source="inquiries"
        :columns="inquiryColumns"
        row-key="id"
        :loading="loading"
        :pagination="{ pageSize: 12 }"
        :scroll="{ x: 1180 }"
      >
        <template #expandedRowRender="{ record }">
          <a-alert
            v-if="record.selectionReason"
            type="success"
            :message="`定标依据：${record.selectionReason}`"
            style="margin-bottom: 8px"
          />
          <a-divider orientation="left">采购申请来源</a-divider>
          <a-table
            size="small"
            :data-source="record.requestLines || []"
            :columns="requestSourceColumns"
            row-key="requestId"
            :pagination="false"
          />
          <a-divider orientation="left">供应商报价</a-divider>
          <div v-if="record.invitations?.length" class="invitation-strip">
            <span>已邀请</span>
            <a-tag v-for="invitation in record.invitations" :key="invitation.id" :color="invitation.status === 'DECLINED' ? 'red' : 'cyan'">
              {{ invitation.supplierName }} · {{ invitationStatusText(invitation.status) }} · {{ deliveryStatusText(invitation.deliveryStatus) }}
            </a-tag>
          </div>
          <a-table
            size="small"
            :data-source="record.quotes"
            :columns="quoteColumns"
            row-key="id"
            :pagination="false"
          >
            <template #expandedRowRender="{ record: quote }">
              <a-table
                size="small"
                :data-source="quote.lines || []"
                :columns="quoteLineColumns"
                row-key="requestId"
                :pagination="false"
              >
                <template #bodyCell="{ column, record: line }">
                  <template v-if="column.key === 'linePrice'">
                    {{ money(line.unitPrice) }}
                  </template>
                  <template v-else-if="column.key === 'lineAmount'">
                    {{ money(line.amount) }}
                  </template>
                </template>
              </a-table>
              <a-button size="small" style="margin-top: 8px" @click="openQuoteAttachments(quote)">
                查看报价附件
              </a-button>
            </template>
            <template #bodyCell="{ column, record: quote }">
              <template v-if="column.key === 'price'">
                <strong>{{ money(quote.totalAmount) }}</strong>
                <span class="table-subtitle">
                  物料 {{ money(quote.materialAmount) }} + 运杂费
                  {{
                    money(
                      Number(quote.freightAmount || 0) +
                        Number(quote.otherCostAmount || 0),
                    )
                  }}
                </span>
              </template>
              <template v-else-if="column.key === 'score'">
                技术 {{ quote.technicalScore }} / 商务
                {{ quote.commercialScore }} /
                <strong>{{ quote.totalScore }}</strong>
                <a-button
                  v-if="record.status === 'OPEN' && auth.can('procurement:request:approve')"
                  type="link"
                  size="small"
                  @click="openScore(record, quote)"
                >评分</a-button>
              </template>
              <template v-else-if="column.key === 'source'">
                <a-tag :color="quote.submissionSource === 'SUPPLIER_PORTAL' ? 'green' : 'blue'">
                  {{ quote.submissionSource === "SUPPLIER_PORTAL" ? "供应商自报" : "采购代录" }}
                </a-tag>
                <span class="table-subtitle">{{ quote.submittedByName || "-" }}</span>
                <a-tag v-if="quote.submissionSource === 'INTERNAL_ENTRY'" :color="quote.confirmed ? 'green' : 'orange'">
                  {{ quote.confirmed ? "供应商已确认" : "待供应商确认" }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'select'">
                <a-button
                  v-if="
                    !quote.selected &&
                    record.status === 'OPEN' &&
                    auth.can('procurement:request:approve')
                  "
                  type="link"
                  @click="selectQuote(record, quote)"
                >
                  选定
                </a-button>
                <a-tag v-else-if="quote.selected" color="green">已选定</a-tag>
              </template>
            </template>
          </a-table>
          <a-button
            v-if="
              record.status === 'OPEN' &&
              auth.can('procurement:purchase:create')
            "
            size="small"
            style="margin-top: 8px"
            @click="openQuote(record)"
          >
            录入供应商报价
          </a-button>
          <a-button
            v-if="record.status === 'OPEN' && auth.can('procurement:purchase:create')"
            size="small"
            style="margin: 8px 0 0 8px"
            @click="openInvite(record)"
          >
            邀请供应商自助报价
          </a-button>
          <a-button
            v-if="auth.can('procurement:view')"
            size="small"
            style="margin: 8px 0 0 8px"
            @click="openClarifications(record)"
          >询价澄清</a-button>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'OPEN' ? 'blue' : 'green'">
              {{ record.status === "OPEN" ? "询价中" : "已定标" }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="record.status === 'OPEN' && auth.can('procurement:purchase:create')"
              type="link"
              size="small"
              @click.stop="openDeadlineEditor(record)"
            >调整截止日</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="inquiryOpen" title="发起单项询价" @ok="saveInquiry">
      <a-form layout="vertical">
        <a-form-item label="已审批采购申请">
          <a-select
            v-model:value="inquiryForm.requestId"
            :options="requestOptions"
          />
        </a-form-item>
        <a-form-item label="询价主题">
          <a-input v-model:value="inquiryForm.title" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="寻源方式">
              <a-select
                v-model:value="inquiryForm.sourcingMethod"
                :options="sourcingOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="最低有效报价数">
              <a-input-number
                v-model:value="inquiryForm.minQuoteCount"
                :min="1"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item
          v-if="inquiryForm.sourcingMethod === 'SINGLE_SOURCE'"
          label="单一来源例外原因"
        >
          <a-textarea v-model:value="inquiryForm.exceptionReason" />
        </a-form-item>
        <a-form-item label="截止日期">
          <a-input v-model:value="inquiryForm.deadline" type="date" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="quoteOpen"
      title="录入供应商分项报价"
      width="1050px"
      @ok="saveQuote"
    >
      <a-form layout="vertical">
        <a-form-item label="供应商">
          <a-select
            v-model:value="quoteForm.supplierId"
            :options="supplierOptions"
          />
        </a-form-item>
        <a-table
          size="small"
          :data-source="quoteLineForm"
          :columns="quoteLineInputColumns"
          row-key="requestId"
          :pagination="false"
          :scroll="{ x: 900 }"
        >
          <template #bodyCell="{ column, record: line }">
            <template v-if="column.key === 'quotePrice'">
              <a-input-number
                v-model:value="line.unitPrice"
                :min="0.01"
                style="width: 130px"
              />
            </template>
            <template v-else-if="column.key === 'quoteTax'">
              <a-input-number
                v-model:value="line.taxRate"
                :min="0"
                :max="100"
                style="width: 90px"
              />
            </template>
            <template v-else-if="column.key === 'quoteDelivery'">
              <a-input
                v-model:value="line.deliveryDate"
                type="date"
                style="width: 150px"
              />
            </template>
          </template>
        </a-table>
        <a-row :gutter="12" style="margin-top: 16px">
          <a-col :span="8">
            <a-form-item label="运费">
              <a-input-number
                v-model:value="quoteForm.freightAmount"
                :min="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="其他费用">
              <a-input-number
                v-model:value="quoteForm.otherCostAmount"
                :min="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="报价有效期">
              <a-input v-model:value="quoteForm.validUntil" type="date" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="付款条件">
          <a-input v-model:value="quoteForm.paymentTerms" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="inviteOpen" title="邀请供应商自助报价" @ok="saveInvitations">
      <a-form layout="vertical">
        <a-form-item label="供应商" required>
          <a-select v-model:value="invitedSupplierIds" mode="multiple" :options="inviteSupplierOptions" placeholder="选择一个或多个已准入供应商" />
        </a-form-item>
        <a-alert type="info" show-icon message="受邀供应商登录独立门户后，可查看该询价并自行提交报价。" />
      </a-form>
    </a-modal>

    <a-modal v-model:open="scoreOpen" title="内部报价评分" @ok="saveScore">
      <a-form layout="vertical">
        <a-alert type="info" show-icon message="评分仅供内部比选，不会向供应商展示。" style="margin-bottom: 16px" />
        <a-row :gutter="12"><a-col :span="12"><a-form-item label="技术评分"><a-input-number v-model:value="scoreForm.technicalScore" :min="0" :max="100" class="full-input" /></a-form-item></a-col><a-col :span="12"><a-form-item label="商务评分"><a-input-number v-model:value="scoreForm.commercialScore" :min="0" :max="100" class="full-input" /></a-form-item></a-col></a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="deadlineOpen" title="调整询价截止日期" @ok="saveDeadline">
      <a-form layout="vertical">
        <a-alert type="warning" show-icon message="调整后供应商门户将立即展示新截止日期，请另行通知已受邀供应商。" style="margin-bottom: 16px" />
        <a-form-item label="新截止日期" required>
          <a-input v-model:value="deadlineValue" type="date" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="attachmentsOpen" width="min(620px, 100vw)" title="供应商报价附件">
      <a-list :data-source="quoteAttachments" :loading="attachmentsLoading" row-key="id">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="item.fileName" :description="`${formatFileSize(item.sizeBytes)} · SHA-256 ${item.sha256.slice(0, 12)}…`" />
            <a-button type="link" @click="api.downloadSupplierQuoteAttachment(item)">下载</a-button>
          </a-list-item>
        </template>
        <template #empty><a-empty description="该报价没有附件" /></template>
      </a-list>
    </a-drawer>

    <a-drawer v-model:open="clarificationsOpen" width="min(720px, 100vw)" title="询价澄清记录">
      <a-list :data-source="clarifications" :loading="clarificationsLoading" row-key="id">
        <template #renderItem="{ item }">
          <a-list-item>
            <div style="width: 100%">
              <strong>{{ item.supplierName || "供应商" }}</strong>
              <p>{{ item.question }}</p>
              <a-alert v-if="item.answer" type="success" :message="item.answer" :description="`${item.answeredByName || '采购方'} · ${item.answeredAt || ''}`" />
              <a-space-compact v-else style="width: 100%">
                <a-input v-model:value="clarificationAnswers[item.id]" placeholder="输入统一、明确的答复" />
                <a-button type="primary" :disabled="!clarificationAnswers[item.id]?.trim()" @click="answerClarification(item)">回复</a-button>
              </a-space-compact>
            </div>
          </a-list-item>
        </template>
        <template #empty><a-empty description="暂无澄清问题" /></template>
      </a-list>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message, Modal } from "ant-design-vue";
import { useAuthStore } from "@/stores/auth";
import * as api from "@/api/procurement";
import {
  sourcingMethodLabel,
  sourcingMethodOptions,
} from "@/utils/procurement-sourcing";

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const inquiries = ref<api.ProcurementInquiry[]>([]);
const requests = ref<api.PurchaseRequest[]>([]);
const suppliers = ref<api.Supplier[]>([]);
const inquiryOpen = ref(false);
const quoteOpen = ref(false);
const inviteOpen = ref(false);
const scoreOpen = ref(false);
const deadlineOpen = ref(false);
const deadlineValue = ref("");
const attachmentsOpen = ref(false);
const attachmentsLoading = ref(false);
const quoteAttachments = ref<api.SupplierQuoteAttachment[]>([]);
const clarificationsOpen = ref(false);
const clarificationsLoading = ref(false);
const clarifications = ref<api.InquiryClarification[]>([]);
const clarificationAnswers = reactive<Record<string, string>>({});
const selectedInquiry = ref<api.ProcurementInquiry | null>(null);
const selectedQuote = ref<api.SupplierQuotation | null>(null);
const invitedSupplierIds = ref<string[]>([]);
const quoteLineForm = ref<
  Array<{
    requestId: string;
    requestCode?: string;
    partName?: string;
    quantity: number;
    unitPrice: number;
    taxRate: number;
    deliveryDate?: string;
  }>
>([]);

const inquiryForm = reactive({
  requestId: "",
  title: "",
  deadline: "",
  createdByName: "",
  sourcingMethod: "COMPETITIVE",
  minQuoteCount: 3,
  exceptionReason: "",
});
const quoteForm = reactive({
  supplierId: "",
  paymentTerms: "",
  currency: "CNY",
  freightAmount: 0,
  otherCostAmount: 0,
  validUntil: "",
});
const scoreForm = reactive({ technicalScore: 0, commercialScore: 0 });
const sourcingOptions = sourcingMethodOptions;
const inquiryColumns = [
  { title: "询价单", dataIndex: "code", width: 190 },
  { title: "主题", dataIndex: "title", width: 260 },
  {
    title: "询价包",
    width: 170,
    customRender: ({ record }: any) =>
      `${record.materialCount || 1}种物料 · ${record.requestCount || 1}条申请`,
  },
  {
    title: "寻源方式",
    dataIndex: "sourcingMethod",
    width: 130,
    customRender: ({ text }: { text?: string }) => sourcingMethodLabel(text),
  },
  { title: "截止日", dataIndex: "deadline", width: 120 },
  {
    title: "报价数",
    width: 90,
    customRender: ({ record }: any) =>
      `${record.quotes.length}/${record.minQuoteCount}`,
  },
  { title: "状态", key: "status", width: 100 },
  { title: "操作", key: "action", width: 120 },
];
const requestSourceColumns = [
  { title: "采购申请", dataIndex: "requestCode" },
  { title: "批次", dataIndex: "batchCode" },
  { title: "物料", dataIndex: "partName" },
  { title: "纳入集采数量", dataIndex: "requestedQty" },
  { title: "成本归属", dataIndex: "costTargetName" },
  { title: "需求日期", dataIndex: "expectedDate" },
];
const quoteColumns = [
  { title: "供应商", dataIndex: "supplierName" },
  { title: "报价来源", key: "source", width: 150 },
  { title: "报价总额（含税，元）", key: "price", width: 190 },
  { title: "综合评分", key: "score" },
  { title: "最晚交期", dataIndex: "deliveryDate" },
  { title: "付款条件", dataIndex: "paymentTerms" },
  { title: "操作", key: "select" },
];
const quoteLineColumns = [
  { title: "采购申请", dataIndex: "requestCode" },
  { title: "物料", dataIndex: "partName" },
  { title: "数量", dataIndex: "quantity" },
  { title: "含税单价（元）", key: "linePrice", width: 170 },
  { title: "税率(%)", dataIndex: "taxRate" },
  { title: "交付日期", dataIndex: "deliveryDate" },
  { title: "分项金额（含税，元）", key: "lineAmount", width: 190 },
];
const quoteLineInputColumns = [
  { title: "采购申请", dataIndex: "requestCode", width: 150 },
  { title: "物料", dataIndex: "partName", width: 200 },
  { title: "数量", dataIndex: "quantity", width: 90 },
  { title: "含税单价（元）", key: "quotePrice", width: 170 },
  { title: "税率(%)", key: "quoteTax", width: 110 },
  { title: "交付日期", key: "quoteDelivery", width: 170 },
];

const activeInquiryRequestIds = computed(
  () =>
    new Set(
      inquiries.value
        .filter((item) => ["OPEN", "AWARDED"].includes(item.status))
        .flatMap((item) => item.requestIds || [item.requestId]),
    ),
);
const requestOptions = computed(() =>
  requests.value
    .filter(
      (item) =>
        item.approvalStatus === "APPROVED" &&
        item.status === "APPROVED" &&
        !activeInquiryRequestIds.value.has(item.id),
    )
    .map((item) => ({
      label: `${item.code} · ${item.partName}`,
      value: item.id,
    })),
);
const supplierOptions = computed(() =>
  suppliers.value
    .filter(
      (item) =>
        item.admissionStatus === "APPROVED" && item.riskStatus !== "BLOCKED",
    )
    .map((item) => ({ label: item.name, value: item.id })),
);
const inviteSupplierOptions = computed(() => {
  const alreadyInvited = new Set(
    selectedInquiry.value?.invitations?.map((item) => item.supplierId) || [],
  );
  return supplierOptions.value.filter((item) => !alreadyInvited.has(item.value));
});

onMounted(load);

async function load() {
  loading.value = true;
  try {
    const [inquiryResult, requestResult, supplierResult] = await Promise.all([
      api.listProcurementInquiries(),
      api.listPurchaseRequests({ page: 0, size: 999 }),
      api.listSuppliers(0, 999),
    ]);
    inquiries.value = inquiryResult;
    requests.value = requestResult.content;
    suppliers.value = supplierResult.content;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载失败");
  } finally {
    loading.value = false;
  }
}

function openQuote(inquiry: api.ProcurementInquiry) {
  selectedInquiry.value = inquiry;
  quoteLineForm.value = (inquiry.requestLines || []).map((line) => ({
    requestId: line.requestId,
    requestCode: line.requestCode,
    partName: line.partName,
    quantity: Number(line.requestedQty || 0),
    unitPrice: 0,
    taxRate: 13,
    deliveryDate: line.expectedDate,
  }));
  Object.assign(quoteForm, {
    supplierId: "",
    paymentTerms: "",
    currency: "CNY",
    freightAmount: 0,
    otherCostAmount: 0,
    validUntil: "",
  });
  quoteOpen.value = true;
}

async function saveInquiry() {
  if (!inquiryForm.requestId || !inquiryForm.title.trim()) {
    message.warning("请选择采购申请并填写询价主题");
    return;
  }
  if (
    inquiryForm.sourcingMethod === "SINGLE_SOURCE" &&
    !inquiryForm.exceptionReason.trim()
  ) {
    message.warning("请填写单一来源例外原因");
    return;
  }
  inquiryForm.createdByName = auth.user?.displayName || "采购员";
  await api.createProcurementInquiry({ ...inquiryForm });
  inquiryOpen.value = false;
  await load();
}

async function saveQuote() {
  if (!selectedInquiry.value) return;
  if (!quoteForm.supplierId) {
    message.warning("请选择供应商");
    return;
  }
  if (quoteLineForm.value.some((line) => Number(line.unitPrice) <= 0)) {
    message.warning("请填写全部物料的含税单价");
    return;
  }
  await api.addSupplierQuotation(selectedInquiry.value.id, {
    ...quoteForm,
    lines: quoteLineForm.value.map((line) => ({
      requestId: line.requestId,
      unitPrice: Number(line.unitPrice),
      taxRate: Number(line.taxRate),
      deliveryDate: line.deliveryDate || undefined,
    })),
  });
  quoteOpen.value = false;
  await load();
}

function openInvite(inquiry: api.ProcurementInquiry) {
  selectedInquiry.value = inquiry;
  invitedSupplierIds.value = [];
  inviteOpen.value = true;
}

async function saveInvitations() {
  if (!selectedInquiry.value || invitedSupplierIds.value.length === 0) {
    message.warning("请选择要邀请的供应商");
    return;
  }
  const result = await api.inviteInquirySuppliers(
    selectedInquiry.value.id,
    invitedSupplierIds.value,
  );
  inviteOpen.value = false;
  const codeEntries = Object.entries(result.registrationCodes || {});
  if (codeEntries.length) {
    const codeText = codeEntries
      .map(([supplierId, code]) => {
        const supplier = suppliers.value.find((item) => item.id === supplierId);
        return `${supplier?.name || supplierId}：${code}`;
      })
      .join("；");
    Modal.info({
      title: "邀请已创建，请发送注册信息",
      content: `${codeText}。注册码仅本次显示，7 天内有效；请通过可信渠道交给对应供应商。`,
      okText: "我已记录",
    });
  } else {
    message.info("所选供应商此前已邀请，本次未重复创建邀请");
  }
  await load();
}

function openDeadlineEditor(inquiry: api.ProcurementInquiry) {
  selectedInquiry.value = inquiry;
  deadlineValue.value = inquiry.deadline || "";
  deadlineOpen.value = true;
}

async function saveDeadline() {
  if (!selectedInquiry.value || !deadlineValue.value) {
    message.warning("请选择新截止日期");
    return;
  }
  await api.updateProcurementInquiryDeadline(
    selectedInquiry.value.id,
    deadlineValue.value,
  );
  deadlineOpen.value = false;
  message.success("询价截止日期已更新");
  await load();
}

async function openQuoteAttachments(quote: api.SupplierQuotation) {
  attachmentsOpen.value = true;
  attachmentsLoading.value = true;
  try {
    quoteAttachments.value = await api.listSupplierQuoteAttachments(quote.id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "附件加载失败");
  } finally {
    attachmentsLoading.value = false;
  }
}

async function openClarifications(inquiry: api.ProcurementInquiry) {
  selectedInquiry.value = inquiry;
  clarificationsOpen.value = true;
  clarificationsLoading.value = true;
  try {
    clarifications.value = await api.listInquiryClarifications(inquiry.id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "澄清记录加载失败");
  } finally {
    clarificationsLoading.value = false;
  }
}

async function answerClarification(item: api.InquiryClarification) {
  const answer = clarificationAnswers[item.id]?.trim();
  if (!answer || !selectedInquiry.value) return;
  await api.answerInquiryClarification(item.id, answer);
  clarificationAnswers[item.id] = "";
  clarifications.value = await api.listInquiryClarifications(selectedInquiry.value.id);
  message.success("澄清答复已提交");
}

function openScore(
  inquiry: api.ProcurementInquiry,
  quote: api.SupplierQuotation,
) {
  selectedInquiry.value = inquiry;
  selectedQuote.value = quote;
  scoreForm.technicalScore = Number(quote.technicalScore || 0);
  scoreForm.commercialScore = Number(quote.commercialScore || 0);
  scoreOpen.value = true;
}

async function saveScore() {
  if (!selectedInquiry.value || !selectedQuote.value) return;
  await api.scoreSupplierQuotation(
    selectedInquiry.value.id,
    selectedQuote.value.id,
    { ...scoreForm },
  );
  scoreOpen.value = false;
  message.success("内部评分已保存");
  await load();
}

function invitationStatusText(status: string) {
  return { INVITED: "待查看", VIEWED: "已查看", RESPONDED: "已响应", DECLINED: "已放弃" }[
    status
  ] || status;
}

function deliveryStatusText(status?: string) {
  return { PENDING: "待人工发送", DELIVERED: "已送达", FAILED: "发送失败" }[
    status || "PENDING"
  ] || status;
}

function selectQuote(
  inquiry: api.ProcurementInquiry,
  quote: api.SupplierQuotation,
) {
  Modal.confirm({
    title: `选定 ${quote.supplierName}？`,
    content: `报价总额 ${money(quote.totalAmount)}，综合评分 ${quote.totalScore}`,
    async onOk() {
      await api.selectSupplierQuotation(inquiry.id, quote.id, {
        operatorName: auth.user?.displayName || "审批人",
        reason: `综合分项价格、交期、技术与商务评分选定（综合分 ${quote.totalScore}）`,
      });
      await load();
    },
  });
}

function money(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
  }).format(Number(value || 0));
}

function formatFileSize(value: number) {
  return value >= 1024 * 1024
    ? `${(value / 1024 / 1024).toFixed(1)} MB`
    : `${Math.max(1, Math.ceil(value / 1024))} KB`;
}
</script>

<style scoped>
.invitation-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  color: #68756f;
}
</style>
