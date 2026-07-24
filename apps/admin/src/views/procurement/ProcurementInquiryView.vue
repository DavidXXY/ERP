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
              </template>
              <template v-else-if="column.key === 'select'">
                <a-button
                  v-if="!quote.selected && record.status === 'OPEN'"
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
            v-if="record.status === 'OPEN'"
            size="small"
            style="margin-top: 8px"
            @click="openQuote(record)"
          >
            录入供应商报价
          </a-button>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'OPEN' ? 'blue' : 'green'">
              {{ record.status === "OPEN" ? "询价中" : "已定标" }}
            </a-tag>
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
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="技术评分">
              <a-input-number
                v-model:value="quoteForm.technicalScore"
                :min="0"
                :max="100"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="商务评分">
              <a-input-number
                v-model:value="quoteForm.commercialScore"
                :min="0"
                :max="100"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="付款条件">
          <a-input v-model:value="quoteForm.paymentTerms" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message, Modal } from "ant-design-vue";
import { useAuthStore } from "@/stores/auth";
import * as api from "@/api/procurement";

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const inquiries = ref<api.ProcurementInquiry[]>([]);
const requests = ref<api.PurchaseRequest[]>([]);
const suppliers = ref<api.Supplier[]>([]);
const inquiryOpen = ref(false);
const quoteOpen = ref(false);
const selectedInquiry = ref<api.ProcurementInquiry | null>(null);
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
  technicalScore: 100,
  commercialScore: 100,
  validUntil: "",
});
const sourcingOptions = [
  { label: "竞争性询价", value: "COMPETITIVE" },
  { label: "单一来源", value: "SINGLE_SOURCE" },
  { label: "框架协议", value: "FRAMEWORK" },
];
const inquiryColumns = [
  { title: "询价单", dataIndex: "code", width: 190 },
  { title: "主题", dataIndex: "title", width: 260 },
  {
    title: "询价包",
    width: 170,
    customRender: ({ record }: any) =>
      `${record.materialCount || 1}种物料 · ${record.requestCount || 1}条申请`,
  },
  { title: "寻源方式", dataIndex: "sourcingMethod", width: 130 },
  { title: "截止日", dataIndex: "deadline", width: 120 },
  {
    title: "报价数",
    width: 90,
    customRender: ({ record }: any) =>
      `${record.quotes.length}/${record.minQuoteCount}`,
  },
  { title: "状态", key: "status", width: 100 },
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
  { title: "报价总额", key: "price" },
  { title: "综合评分", key: "score" },
  { title: "最晚交期", dataIndex: "deliveryDate" },
  { title: "付款条件", dataIndex: "paymentTerms" },
  { title: "操作", key: "select" },
];
const quoteLineColumns = [
  { title: "采购申请", dataIndex: "requestCode" },
  { title: "物料", dataIndex: "partName" },
  { title: "数量", dataIndex: "quantity" },
  { title: "含税单价", key: "linePrice" },
  { title: "税率(%)", dataIndex: "taxRate" },
  { title: "交付日期", dataIndex: "deliveryDate" },
  { title: "分项金额", key: "lineAmount" },
];
const quoteLineInputColumns = [
  { title: "采购申请", dataIndex: "requestCode", width: 150 },
  { title: "物料", dataIndex: "partName", width: 200 },
  { title: "数量", dataIndex: "quantity", width: 90 },
  { title: "含税单价", key: "quotePrice", width: 150 },
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
    technicalScore: 100,
    commercialScore: 100,
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
</script>
