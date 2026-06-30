<template>
  <div class="page-stack">
    <a-card title="报价方案">
      <template #extra>
        <a-space>
          <a-button @click="loadData">刷新</a-button>
          <a-button v-if="auth.can('crm:quote:create')" type="primary" @click="createOpen = true">新增报价</a-button>
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="报价总数" :value="quotes.length" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="报价总额" :value="totalAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待提交" :value="draftCount" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="审批中" :value="pendingCount" suffix="份" /></a-col>
      </a-row>

      <a-table :columns="columns" :data-source="quotes" :loading="loading" :row-key="(record: QuotePlan) => record.id" :scroll="{ x: 1200 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quote'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.customerName }} · {{ record.opportunityCode || "未关联商机" }}</span>
          </template>
          <template v-else-if="column.key === 'scope'"><span class="line-clamp-2">{{ record.serviceScope }}</span></template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="quoteStatusColor(record.status)">{{ quoteStatusLabel(record.status) }}</a-tag>
            <span v-if="record.lastApproverName" class="table-subtitle">{{ record.lastApproverName }} · {{ record.lastApprovalComment }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-popconfirm
                v-if="auth.can('crm:quote:submit') && (record.status === 'DRAFT' || record.status === 'REJECTED')"
                title="提交后报价将进入审批中，确认提交？"
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
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="createOpen" title="新增报价方案" width="780px" :confirm-loading="saving" @ok="handleCreate">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="报价编号" name="code"><a-input v-model:value="form.code" /></a-form-item></a-col>
          <a-col :xs="24" :md="16"><a-form-item label="关联商机" name="opportunityId"><a-select v-model:value="form.opportunityId" show-search option-filter-prop="label" :options="opportunityOptions" @change="syncCustomer" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="服务范围" name="serviceScope"><a-textarea v-model:value="form.serviceScope" :rows="3" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="服务频次"><a-input v-model:value="form.inspectCycle" placeholder="例如：季度服务，年度检测" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="报价金额" name="amount"><a-input-number v-model:value="form.amount" :min="0" class="full-input" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="付款节点"><a-input v-model:value="form.paymentNodes" placeholder="例如：签约30%，半年节点30%，验收40%" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="approvalOpen" title="报价审批处理" width="860px" :confirm-loading="saving" @ok="handleApproval">
      <a-alert
        v-if="selectedQuote"
        class="section-alert"
        type="info"
        :message="`${selectedQuote.code} · ${selectedQuote.customerName} · ${formatMoney(selectedQuote.amount)}`"
      />
      <a-form ref="approvalFormRef" :model="approvalForm" :rules="approvalRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8">
            <a-form-item label="审批结论" name="decision">
              <a-radio-group v-model:value="approvalForm.decision" button-style="solid">
                <a-radio-button value="APPROVED">通过</a-radio-button>
                <a-radio-button value="REJECTED">驳回</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="16"><a-form-item label="审批人" name="approverName"><a-input v-model:value="approvalForm.approverName" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="审批意见" name="comment"><a-textarea v-model:value="approvalForm.comment" :rows="2" /></a-form-item></a-col>
        </a-row>

        <template v-if="approvalForm.decision === 'APPROVED'">
          <a-divider>合同生成信息</a-divider>
          <a-row :gutter="16">
            <a-col :xs="24" :md="8"><a-form-item label="合同编号" name="contractCode"><a-input v-model:value="approvalForm.contractCode" /></a-form-item></a-col>
            <a-col :xs="24" :md="10"><a-form-item label="合同项目名称" name="projectName"><a-input v-model:value="approvalForm.projectName" /></a-form-item></a-col>
            <a-col :xs="24" :md="6"><a-form-item label="合同类型" name="contractType"><a-select v-model:value="approvalForm.contractType" :options="contractTypeOptions" /></a-form-item></a-col>
            <a-col :xs="24" :md="8"><a-form-item label="开始日期" name="startDate"><a-input v-model:value="approvalForm.startDate" type="date" /></a-form-item></a-col>
            <a-col :xs="24" :md="8"><a-form-item label="结束日期" name="endDate"><a-input v-model:value="approvalForm.endDate" type="date" /></a-form-item></a-col>
            <a-col :xs="24" :md="8"><a-form-item label="服务周期"><a-input v-model:value="approvalForm.serviceCycle" /></a-form-item></a-col>
          </a-row>

          <a-divider>首期应收</a-divider>
          <a-row :gutter="16">
            <a-col :xs="24" :md="8"><a-form-item label="应收单号" name="receivableCode"><a-input v-model:value="approvalForm.receivableCode" /></a-form-item></a-col>
            <a-col :xs="24" :md="8"><a-form-item label="首期金额" name="firstReceivableAmount"><a-input-number v-model:value="approvalForm.firstReceivableAmount" :min="0" :max="selectedQuote?.amount" class="full-input" /></a-form-item></a-col>
            <a-col :xs="24" :md="8"><a-form-item label="到期日" name="firstReceivableDueDate"><a-input v-model:value="approvalForm.firstReceivableDueDate" type="date" /></a-form-item></a-col>
          </a-row>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import {
  createQuote,
  listOpportunities,
  listQuotes,
  processQuoteApproval,
  submitQuote,
  type ApprovalDecision,
  type Opportunity,
  type QuotePlan,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import { formatMoney, quoteStatusColor, quoteStatusLabel } from "./crm-options";

const auth = useAuthStore();
const quotes = ref<QuotePlan[]>([]);
const opportunities = ref<Opportunity[]>([]);
const loading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const approvalOpen = ref(false);
const formRef = ref();
const approvalFormRef = ref();
const selectedQuote = ref<QuotePlan | null>(null);
const form = reactive(initialForm());
const approvalForm = reactive(initialApprovalForm());
const rules = {
  code: [{ required: true, message: "请输入报价编号" }],
  opportunityId: [{ required: true, message: "请选择关联商机" }],
  serviceScope: [{ required: true, message: "请输入服务范围" }],
  amount: [{ required: true, message: "请输入报价金额" }],
};
const approvalRules = {
  decision: [{ required: true, message: "请选择审批结论" }],
  comment: [{ required: true, message: "请输入审批意见" }],
  approverName: [{ required: true, message: "请输入审批人" }],
  contractCode: [{ required: true, message: "请输入合同编号" }],
  projectName: [{ required: true, message: "请输入合同项目名称" }],
  contractType: [{ required: true, message: "请选择合同类型" }],
  startDate: [{ required: true, message: "请选择开始日期" }],
  endDate: [{ required: true, message: "请选择结束日期" }],
  receivableCode: [{ required: true, message: "请输入应收单号" }],
  firstReceivableAmount: [{ required: true, message: "请输入首期金额" }],
  firstReceivableDueDate: [{ required: true, message: "请选择应收到期日" }],
};
const contractTypeOptions = [
  { label: "年度服务合同", value: "年度服务合同" },
  { label: "单次服务合同", value: "单次服务合同" },
  { label: "驻点服务合同", value: "驻点服务合同" },
  { label: "设备质保合同", value: "设备质保合同" },
];
const columns = [
  { title: "报价 / 客户", key: "quote", width: 230 },
  { title: "服务范围", key: "scope", width: 320 },
  { title: "服务频次", dataIndex: "inspectCycle", width: 160 },
  { title: "付款节点", dataIndex: "paymentNodes", width: 240 },
  { title: "报价金额", key: "amount", width: 140 },
  { title: "状态", key: "status", width: 210 },
  { title: "操作", key: "action", width: 150, fixed: "right" },
];
const opportunityOptions = computed(() => opportunities.value
  .filter((item) => item.stage !== "LOST")
  .map((item) => ({ label: `${item.code} · ${item.customerName} · ${item.needSummary}`, value: item.id })));
const totalAmount = computed(() => quotes.value.reduce((sum, item) => sum + Number(item.amount || 0), 0));
const draftCount = computed(() => quotes.value.filter((item) => item.status === "DRAFT" || item.status === "REJECTED").length);
const pendingCount = computed(() => quotes.value.filter((item) => item.status === "PENDING_APPROVAL").length);

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

function syncCustomer(opportunityId: string) {
  form.customerId = opportunities.value.find((item) => item.id === opportunityId)?.customerId;
}

async function handleCreate() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await createQuote({ ...form });
    Object.assign(form, initialForm());
    createOpen.value = false;
    message.success("报价方案已保存为草稿");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "报价创建失败");
  } finally {
    saving.value = false;
  }
}

async function handleSubmit(record: QuotePlan) {
  try {
    await submitQuote(record.id);
    message.success("报价已提交审批");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "提交审批失败");
  }
}

function openApproval(record: QuotePlan) {
  selectedQuote.value = record;
  Object.assign(approvalForm, initialApprovalForm(record));
  approvalOpen.value = true;
}

async function handleApproval() {
  await approvalFormRef.value?.validate();
  if (!selectedQuote.value) return;
  saving.value = true;
  try {
    const result = await processQuoteApproval(selectedQuote.value.id, { ...approvalForm });
    approvalOpen.value = false;
    message.success(result.contract ? `审批通过，已生成合同 ${result.contract.code}` : "报价已驳回");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批处理失败");
  } finally {
    saving.value = false;
  }
}

function initialForm() {
  return { customerId: undefined as string | undefined, opportunityId: undefined as string | undefined, code: "", serviceScope: "", inspectCycle: "", paymentNodes: "", amount: 0 };
}

function initialApprovalForm(record?: QuotePlan) {
  const today = new Date();
  const end = new Date(today);
  end.setFullYear(end.getFullYear() + 1);
  end.setDate(end.getDate() - 1);
  const due = new Date(today);
  due.setDate(due.getDate() + 30);
  const suffix = record?.code.replace(/^BJ-?/, "") || "";
  return {
    decision: "APPROVED" as ApprovalDecision,
    comment: "同意报价，按合同节点执行并跟踪首期回款。",
    approverName: auth.user?.displayName || "",
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

function formatDate(value: Date) {
  return new Date(value.getTime() - value.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}
</script>
