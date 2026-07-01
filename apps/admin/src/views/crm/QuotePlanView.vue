<template>
  <div class="page-stack quote-page">
    <a-card title="报价方案">
      <template #extra>
        <a-space>
          <a-button @click="loadData">刷新</a-button>
          <a-button v-if="auth.can('crm:quote:create')" type="primary" @click="openCreate">新增报价</a-button>
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="报价总数" :value="quotes.length" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="当前报价总额" :value="totalAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待客户确认" :value="customerPendingCount" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="客户已接受" :value="customerAcceptedCount" suffix="份" /></a-col>
      </a-row>

      <div class="quote-toolbar">
        <a-input v-model:value="keyword" allow-clear placeholder="搜索报价编号、客户或服务范围" />
        <a-select v-model:value="statusFilter" :options="statusOptions" />
      </div>

      <a-table
        :columns="columns"
        :data-source="filteredQuotes"
        :loading="loading"
        :row-key="(record: QuotePlan) => record.id"
        :scroll="{ x: 1280 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quote'">
            <div class="quote-title-line">
              <strong>{{ record.code }}</strong>
              <a-tag>V{{ record.versionNo }}</a-tag>
            </div>
            <span class="table-subtitle">{{ record.customerName }} · {{ record.opportunityCode || "未关联商机" }}</span>
          </template>
          <template v-else-if="column.key === 'scope'">
            <span class="line-clamp-2">{{ record.serviceScope }}</span>
            <span v-if="record.inspectCycle" class="table-subtitle">{{ record.inspectCycle }}</span>
          </template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="quoteStatusColor(record.status)">{{ quoteStatusLabel(record.status) }}</a-tag>
            <span v-if="record.lastApproverName" class="table-subtitle">
              {{ record.lastApproverName }} · {{ record.lastApprovalComment }}
            </span>
          </template>
          <template v-else-if="column.key === 'customerResult'">
            <template v-if="record.customerDecision">
              <span class="result-text">{{ record.customerComment }}</span>
              <span class="table-subtitle">{{ record.customerDecisionBy }} · {{ formatDateTime(record.customerDecidedAt) }}</span>
            </template>
            <span v-else class="muted-text">尚未登记</span>
          </template>
          <template v-else-if="column.key === 'updatedAt'">{{ formatDateTime(record.updatedAt) }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a-button
                v-if="canEdit(record)"
                size="small"
                type="link"
                @click="openEdit(record)"
              >
                修订报价
              </a-button>
              <a-popconfirm
                v-if="auth.can('crm:quote:submit') && record.status === 'DRAFT'"
                title="当前版本将进入内部审批，确认提交？"
                @confirm="handleSubmit(record)"
              >
                <a-button size="small" type="link">提交审批</a-button>
              </a-popconfirm>
              <a-button
                v-if="auth.can('crm:quote:approve') && record.status === 'PENDING_APPROVAL'"
                size="small"
                type="link"
                @click="openApproval(record)"
              >
                审批处理
              </a-button>
              <a-button
                v-if="auth.can('crm:quote:customer-result') && record.status === 'APPROVED'"
                size="small"
                type="link"
                @click="openCustomerResult(record)"
              >
                客户结果
              </a-button>
              <a-button
                v-if="auth.can('crm:quote:convert') && record.status === 'CUSTOMER_ACCEPTED'"
                size="small"
                type="link"
                @click="openConversion(record)"
              >
                转合同
              </a-button>
              <a-button size="small" type="link" @click="openRevisions(record)">版本记录</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="editorOpen"
      :title="editingQuote ? `修订报价 · ${editingQuote.code} V${editingQuote.versionNo}` : '新增报价方案'"
      width="780px"
      :confirm-loading="saving"
      @ok="handleSaveQuote"
    >
      <a-alert
        v-if="editingQuote"
        class="section-alert"
        type="info"
        show-icon
        message="保存后将生成新版本并回到草稿状态，需重新提交内部审批。"
      />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8">
          </a-col>
          <a-col :xs="24" :md="16">
            <a-form-item label="关联商机" name="opportunityId">
              <a-select
                v-model:value="form.opportunityId"
                show-search
                option-filter-prop="label"
                :disabled="Boolean(editingQuote)"
                :options="opportunityOptions"
                @change="syncCustomer"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24"><a-form-item label="服务范围" name="serviceScope"><a-textarea v-model:value="form.serviceScope" :rows="3" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="服务频次"><a-input v-model:value="form.inspectCycle" placeholder="例如：季度服务，年度检测" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="报价金额" name="amount"><a-input-number v-model:value="form.amount" :min="0" class="full-input" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="付款节点"><a-input v-model:value="form.paymentNodes" placeholder="例如：签约30%，半年节点30%，验收40%" /></a-form-item></a-col>
          <a-col v-if="editingQuote" :span="24">
            <a-form-item label="本次修订说明" required>
              <a-textarea v-model:value="form.revisionNote" :rows="2" placeholder="说明本次价格、范围、频次或付款节点的调整" />
            </a-form-item>
          </a-col>
          <a-col :span="24"><a-form-item :label="editingQuote ? '修订人' : '创建人'"><a-input v-model:value="form.editorName" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="approvalOpen" title="内部报价审批" width="640px" :confirm-loading="saving" @ok="handleApproval">
      <a-alert
        v-if="selectedQuote"
        class="section-alert"
        type="info"
        show-icon
        :message="`${selectedQuote.code} V${selectedQuote.versionNo} · ${selectedQuote.customerName} · ${formatMoney(selectedQuote.amount)}`"
        description="审批通过仅代表允许向客户发送本版报价，不会生成合同或应收。"
      />
      <a-form ref="approvalFormRef" :model="approvalForm" :rules="approvalRules" layout="vertical">
        <a-form-item label="审批结论" name="decision">
          <a-radio-group v-model:value="approvalForm.decision" button-style="solid">
            <a-radio-button value="APPROVED">通过</a-radio-button>
            <a-radio-button value="REJECTED">驳回</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批人" name="approverName"><a-input v-model:value="approvalForm.approverName" /></a-form-item>
        <a-form-item label="审批意见" name="comment"><a-textarea v-model:value="approvalForm.comment" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="customerResultOpen" title="登记客户结果" width="640px" :confirm-loading="saving" @ok="handleCustomerResult">
      <a-alert
        v-if="selectedQuote"
        class="section-alert"
        type="info"
        show-icon
        :message="`${selectedQuote.code} V${selectedQuote.versionNo} · ${selectedQuote.customerName}`"
        description="客户拒绝后仍可继续修订报价；客户接受后才可以进入转合同流程。"
      />
      <a-form ref="customerResultFormRef" :model="customerResultForm" :rules="customerResultRules" layout="vertical">
        <a-form-item label="客户结果" name="decision">
          <a-radio-group v-model:value="customerResultForm.decision" button-style="solid" @change="syncCustomerComment">
            <a-radio-button value="ACCEPTED">客户接受</a-radio-button>
            <a-radio-button value="DECLINED">客户拒绝</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="登记人" name="operatorName"><a-input v-model:value="customerResultForm.operatorName" /></a-form-item>
        <a-form-item label="结果说明" name="comment"><a-textarea v-model:value="customerResultForm.comment" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="conversionOpen" title="报价转合同" width="860px" :confirm-loading="saving" @ok="handleConversion">
      <a-alert
        v-if="selectedQuote"
        class="section-alert"
        type="success"
        show-icon
        :message="`${selectedQuote.code} V${selectedQuote.versionNo} 已获客户接受`"
        description="确认后生成合同；如填写首期金额，同时生成首期应收并将关联商机标记为赢单。"
      />
      <a-form ref="conversionFormRef" :model="conversionForm" :rules="conversionRules" layout="vertical">
        <a-divider>合同信息</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="合同编号" name="contractCode"><a-input v-model:value="conversionForm.contractCode" /></a-form-item></a-col>
          <a-col :xs="24" :md="10"><a-form-item label="合同项目名称" name="projectName"><a-input v-model:value="conversionForm.projectName" /></a-form-item></a-col>
          <a-col :xs="24" :md="6"><a-form-item label="合同类型" name="contractType"><a-select v-model:value="conversionForm.contractType" :options="contractTypeOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="开始日期" name="startDate"><a-input v-model:value="conversionForm.startDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="结束日期" name="endDate"><a-input v-model:value="conversionForm.endDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="服务周期"><a-input v-model:value="conversionForm.serviceCycle" /></a-form-item></a-col>
        </a-row>

        <a-divider>首期应收</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="应收单号" name="receivableCode"><a-input v-model:value="conversionForm.receivableCode" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="首期金额" name="firstReceivableAmount"><a-input-number v-model:value="conversionForm.firstReceivableAmount" :min="0" :max="selectedQuote?.amount" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="到期日" name="firstReceivableDueDate"><a-input v-model:value="conversionForm.firstReceivableDueDate" type="date" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="revisionsOpen" width="720" title="报价版本记录">
      <div v-if="revisionQuote" class="revision-heading">
        <div>
          <strong>{{ revisionQuote.code }}</strong>
          <span>{{ revisionQuote.customerName }}</span>
        </div>
        <a-tag color="blue">当前 V{{ revisionQuote.versionNo }}</a-tag>
      </div>
      <a-table
        size="small"
        :columns="revisionColumns"
        :data-source="revisions"
        :loading="revisionLoading"
        :pagination="false"
        :row-key="(record: QuoteRevision) => record.id"
        :scroll="{ x: 760 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'version'"><strong>V{{ record.versionNo }}</strong></template>
          <template v-else-if="column.key === 'content'">
            <span class="line-clamp-2">{{ record.serviceScope }}</span>
            <span class="table-subtitle">{{ record.inspectCycle || "未设置频次" }} · {{ formatMoney(record.amount) }}</span>
          </template>
          <template v-else-if="column.key === 'revision'">
            <span>{{ record.revisionNote }}</span>
            <span class="table-subtitle">{{ record.editorName }} · {{ formatDateTime(record.revisedAt) }}</span>
          </template>
        </template>
      </a-table>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import {
  convertQuote,
  createQuote,
  listOpportunities,
  listQuoteRevisions,
  listQuotes,
  processQuoteApproval,
  processQuoteCustomerResult,
  submitQuote,
  updateQuote,
  type ApprovalDecision,
  type Opportunity,
  type QuoteCustomerDecision,
  type QuotePlan,
  type QuoteRevision,
  type QuoteStatus,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import { formatMoney, quoteStatusColor, quoteStatusLabel } from "./crm-options";

const auth = useAuthStore();
const quotes = ref<QuotePlan[]>([]);
const opportunities = ref<Opportunity[]>([]);
const revisions = ref<QuoteRevision[]>([]);
const loading = ref(false);
const saving = ref(false);
const revisionLoading = ref(false);
const editorOpen = ref(false);
const approvalOpen = ref(false);
const customerResultOpen = ref(false);
const conversionOpen = ref(false);
const revisionsOpen = ref(false);
const formRef = ref();
const approvalFormRef = ref();
const customerResultFormRef = ref();
const conversionFormRef = ref();
const selectedQuote = ref<QuotePlan | null>(null);
const editingQuote = ref<QuotePlan | null>(null);
const revisionQuote = ref<QuotePlan | null>(null);
const keyword = ref("");
const statusFilter = ref<QuoteStatus | "ALL">("ALL");
const form = reactive(initialForm());
const approvalForm = reactive(initialApprovalForm());
const customerResultForm = reactive(initialCustomerResultForm());
const conversionForm = reactive(initialConversionForm());

const rules = {
  code: [],
  opportunityId: [{ required: true, message: "请选择关联商机" }],
  serviceScope: [{ required: true, message: "请输入服务范围" }],
  amount: [{ required: true, message: "请输入报价金额" }],
};
const approvalRules = {
  decision: [{ required: true, message: "请选择审批结论" }],
  comment: [{ required: true, message: "请输入审批意见" }],
  approverName: [{ required: true, message: "请输入审批人" }],
};
const customerResultRules = {
  decision: [{ required: true, message: "请选择客户结果" }],
  comment: [{ required: true, message: "请输入结果说明" }],
  operatorName: [{ required: true, message: "请输入登记人" }],
};
const conversionRules = {
  contractCode: [{ required: true, message: "请输入合同编号" }],
  projectName: [{ required: true, message: "请输入合同项目名称" }],
  contractType: [{ required: true, message: "请选择合同类型" }],
  startDate: [{ required: true, message: "请选择开始日期" }],
  endDate: [{ required: true, message: "请选择结束日期" }],
};
const contractTypeOptions = [
  { label: "年度服务合同", value: "年度服务合同" },
  { label: "单次服务合同", value: "单次服务合同" },
  { label: "驻点服务合同", value: "驻点服务合同" },
  { label: "设备质保合同", value: "设备质保合同" },
];
const statusOptions = [
  { label: "全部状态", value: "ALL" },
  { label: "草稿", value: "DRAFT" },
  { label: "审批中", value: "PENDING_APPROVAL" },
  { label: "待客户确认", value: "APPROVED" },
  { label: "内部驳回", value: "REJECTED" },
  { label: "客户已接受", value: "CUSTOMER_ACCEPTED" },
  { label: "客户已拒绝", value: "CUSTOMER_DECLINED" },
  { label: "已转合同", value: "CONVERTED" },
];
const columns = [
  { title: "报价 / 客户", key: "quote", width: 250 },
  { title: "方案内容", key: "scope", width: 320 },
  { title: "报价金额", key: "amount", width: 140 },
  { title: "流程状态", key: "status", width: 220 },
  { title: "客户反馈", key: "customerResult", width: 260 },
  { title: "更新时间", key: "updatedAt", width: 150 },
  { title: "操作", key: "action", width: 280, fixed: "right" },
];
const revisionColumns = [
  { title: "版本", key: "version", width: 80 },
  { title: "报价内容", key: "content", width: 330 },
  { title: "修订记录", key: "revision", width: 320 },
];
const opportunityOptions = computed(() => opportunities.value
  .filter((item) => item.stage !== "LOST")
  .map((item) => ({ label: `${item.code} · ${item.customerName} · ${item.needSummary}`, value: item.id })));
const filteredQuotes = computed(() => {
  const normalized = keyword.value.trim().toLowerCase();
  return quotes.value.filter((item) => {
    const matchesStatus = statusFilter.value === "ALL" || item.status === statusFilter.value;
    const matchesKeyword = !normalized || [item.code, item.customerName, item.opportunityCode, item.serviceScope]
      .some((value) => value?.toLowerCase().includes(normalized));
    return matchesStatus && matchesKeyword;
  });
});
const totalAmount = computed(() => quotes.value.reduce((sum, item) => sum + Number(item.amount || 0), 0));
const customerPendingCount = computed(() => quotes.value.filter((item) => item.status === "APPROVED").length);
const customerAcceptedCount = computed(() => quotes.value.filter((item) => item.status === "CUSTOMER_ACCEPTED").length);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    [quotes.value, opportunities.value] = await Promise.all([listQuotes(), listOpportunities()]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "报价加载失败");
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingQuote.value = null;
  Object.assign(form, initialForm());
  editorOpen.value = true;
}

function openEdit(record: QuotePlan) {
  editingQuote.value = record;
  Object.assign(form, {
    customerId: record.customerId,
    opportunityId: record.opportunityId,
    code: record.code,
    serviceScope: record.serviceScope,
    inspectCycle: record.inspectCycle || "",
    paymentNodes: record.paymentNodes || "",
    amount: Number(record.amount),
    revisionNote: "",
    editorName: auth.user?.displayName || "",
  });
  editorOpen.value = true;
}

function syncCustomer(opportunityId: string) {
  form.customerId = opportunities.value.find((item) => item.id === opportunityId)?.customerId;
}

async function handleSaveQuote() {
  await formRef.value?.validate();
  if (!form.editorName.trim()) {
    message.warning("请输入创建人或修订人");
    return;
  }
  if (editingQuote.value && !form.revisionNote.trim()) {
    message.warning("请填写本次修订说明");
    return;
  }
  saving.value = true;
  try {
    if (editingQuote.value) {
      await updateQuote(editingQuote.value.id, {
        serviceScope: form.serviceScope,
        inspectCycle: form.inspectCycle,
        paymentNodes: form.paymentNodes,
        amount: form.amount,
        revisionNote: form.revisionNote,
        editorName: form.editorName,
      });
      message.success(`已生成 ${editingQuote.value.code} V${editingQuote.value.versionNo + 1}`);
    } else {
      await createQuote({
        customerId: form.customerId,
        opportunityId: form.opportunityId,
        code: form.code,
        serviceScope: form.serviceScope,
        inspectCycle: form.inspectCycle,
        paymentNodes: form.paymentNodes,
        amount: form.amount,
        editorName: form.editorName,
      });
      message.success("报价方案已保存为 V1 草稿");
    }
    editorOpen.value = false;
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "报价保存失败");
  } finally {
    saving.value = false;
  }
}

async function handleSubmit(record: QuotePlan) {
  try {
    await submitQuote(record.id);
    message.success(`${record.code} V${record.versionNo} 已提交内部审批`);
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "提交审批失败");
  }
}

function openApproval(record: QuotePlan) {
  selectedQuote.value = record;
  Object.assign(approvalForm, initialApprovalForm());
  approvalOpen.value = true;
}

async function handleApproval() {
  await approvalFormRef.value?.validate();
  if (!selectedQuote.value) return;
  saving.value = true;
  try {
    const result = await processQuoteApproval(selectedQuote.value.id, { ...approvalForm });
    approvalOpen.value = false;
    message.success(result.status === "APPROVED" ? "内部审批已通过，等待客户确认" : "报价已驳回，请修订后重新提交");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批处理失败");
  } finally {
    saving.value = false;
  }
}

function openCustomerResult(record: QuotePlan) {
  selectedQuote.value = record;
  Object.assign(customerResultForm, initialCustomerResultForm());
  customerResultOpen.value = true;
}

function syncCustomerComment() {
  customerResultForm.comment = customerResultForm.decision === "ACCEPTED"
    ? "客户确认接受本版报价，进入合同签署准备。"
    : "客户未接受本版报价，记录原因后继续修订。";
}

async function handleCustomerResult() {
  await customerResultFormRef.value?.validate();
  if (!selectedQuote.value) return;
  saving.value = true;
  try {
    const result = await processQuoteCustomerResult(selectedQuote.value.id, { ...customerResultForm });
    customerResultOpen.value = false;
    message.success(result.status === "CUSTOMER_ACCEPTED" ? "客户已接受，可继续转合同" : "已登记客户拒绝，可继续修订报价");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "客户结果登记失败");
  } finally {
    saving.value = false;
  }
}

function openConversion(record: QuotePlan) {
  selectedQuote.value = record;
  Object.assign(conversionForm, initialConversionForm(record));
  conversionOpen.value = true;
}

async function handleConversion() {
  await conversionFormRef.value?.validate();
  if (!selectedQuote.value) return;
  saving.value = true;
  try {
    const result = await convertQuote(selectedQuote.value.id, { ...conversionForm });
    conversionOpen.value = false;
    message.success(`已生成合同 ${result.contract?.code || ""}`);
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "转合同失败");
  } finally {
    saving.value = false;
  }
}

async function openRevisions(record: QuotePlan) {
  revisionQuote.value = record;
  revisions.value = [];
  revisionsOpen.value = true;
  revisionLoading.value = true;
  try {
    revisions.value = await listQuoteRevisions(record.id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "版本记录加载失败");
  } finally {
    revisionLoading.value = false;
  }
}

function canEdit(record: QuotePlan) {
  return auth.can("crm:quote:update") && record.status !== "PENDING_APPROVAL" && record.status !== "CONVERTED";
}

function initialForm() {
  return {
    customerId: undefined as string | undefined,
    opportunityId: undefined as string | undefined,
    code: generateQuoteCode(),
    serviceScope: "",
    inspectCycle: "",
    paymentNodes: "",
    amount: 0,
    revisionNote: "",
    editorName: auth.user?.displayName || "",
  };
}

function initialApprovalForm() {
  return {
    decision: "APPROVED" as ApprovalDecision,
    comment: "同意本版报价对外发送，后续跟踪客户反馈。",
    approverName: auth.user?.displayName || "",
  };
}

function initialCustomerResultForm() {
  return {
    decision: "ACCEPTED" as QuoteCustomerDecision,
    comment: "客户确认接受本版报价，进入合同签署准备。",
    operatorName: auth.user?.displayName || "",
  };
}

function initialConversionForm(record?: QuotePlan) {
  const today = new Date();
  const end = new Date(today);
  end.setFullYear(end.getFullYear() + 1);
  end.setDate(end.getDate() - 1);
  const due = new Date(today);
  due.setDate(due.getDate() + 30);
  const suffix = (record?.code || "").replace(/^BJ-?/, "") || "";
  return {
    contractCode: suffix ? `HT-${suffix}` : "",
    projectName: record?.serviceScope.slice(0, 40) || "",
    contractType: "年度服务合同",
    startDate: formatDate(today),
    endDate: formatDate(end),
    serviceCycle: record?.inspectCycle || "",
    receivableCode: suffix ? `YS-${suffix}-01` : "",
    firstReceivableAmount: Math.round(Number(record?.amount || 0) * 0.3),
    firstReceivableDueDate: formatDate(due),
  };
}

function generateQuoteCode() {
  const now = new Date();
  const stamp = [now.getFullYear(), now.getMonth() + 1, now.getDate(), now.getHours(), now.getMinutes(), now.getSeconds()]
    .map((value) => String(value).padStart(2, "0"))
    .join("");
  return `BJ-${stamp}`;
}

function formatDate(value: Date) {
  return new Date(value.getTime() - value.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function formatDateTime(value?: string) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}
</script>

<style scoped>
.quote-toolbar {
  display: flex;
  gap: 12px;
  margin: 20px 0 16px;
}

.quote-toolbar .ant-input-affix-wrapper {
  width: min(360px, 100%);
}

.quote-toolbar .ant-select {
  width: 180px;
}

.quote-title-line,
.revision-heading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quote-title-line .ant-tag {
  margin: 0;
}

.result-text,
.muted-text {
  display: block;
}

.muted-text {
  color: #8c8c8c;
}

.revision-heading {
  justify-content: space-between;
  padding: 0 0 18px;
}

.revision-heading div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.revision-heading span {
  color: #667085;
}

@media (max-width: 720px) {
  .quote-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .quote-toolbar .ant-input-affix-wrapper,
  .quote-toolbar .ant-select {
    width: 100%;
  }
}
</style>
