<template>
  <div class="page-stack">
    <a-card title="付款申请">
      <template #extra>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="待审批" :value="countByStatus('PENDING_APPROVAL')" suffix="笔" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待付款" :value="countByStatus('APPROVED')" suffix="笔" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="审批占用金额" :value="reservedAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="累计实付" :value="paidAmount" :formatter="moneyFormatter" /></a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-input-search v-model:value="keyword" allow-clear placeholder="搜索申请单、应付单、供应商" style="width: 280px" />
        <a-select v-model:value="statusFilter" allow-clear placeholder="全部状态" :options="statusOptions" style="width: 150px" />
      </a-space>

      <a-table
        :columns="applicationColumns"
        :data-source="filteredApplications"
        :loading="loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(item: PaymentApplication) => item.id"
        :scroll="{ x: 1420 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'application'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">申请日期 {{ record.requestedDate }}</span>
          </template>
          <template v-else-if="column.key === 'payable'">
            {{ record.payableCode }}
            <span class="table-subtitle">{{ record.supplierName }}</span>
          </template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.requestedAmount) }}</strong></template>
          <template v-else-if="column.key === 'applicant'">
            {{ record.applicantName }}
            <span class="table-subtitle line-clamp-2">{{ record.purpose }}</span>
          </template>
          <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag></template>
          <template v-else-if="column.key === 'approval'">
            <template v-if="record.approverName">
              {{ record.approverName }}
              <span class="table-subtitle">{{ record.approvalComment || '-' }}</span>
            </template>
            <span v-else class="muted">尚未审批</span>
          </template>
          <template v-else-if="column.key === 'payment'">
            <template v-if="record.paymentCode">
              {{ record.paymentCode }}
              <span class="table-subtitle">已完成付款</span>
            </template>
            <span v-else class="muted">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button v-if="auth.can('finance:payment:approve') && record.status === 'PENDING_APPROVAL'" type="link" size="small" @click="openApproval(record)">
                <template #icon><AuditOutlined /></template>
                审批
              </a-button>
              <a-button v-if="auth.can('finance:payment:execute') && record.status === 'APPROVED'" type="link" size="small" @click="openPayment(record)">
                <template #icon><PayCircleOutlined /></template>
                执行付款
              </a-button>
              <span v-if="!hasAction(record)" class="muted">-</span>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card title="付款记录">
      <a-table
        :columns="paymentColumns"
        :data-source="payments"
        :loading="loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(item: PaymentRecord) => item.id"
        :scroll="{ x: 1120 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'payment'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">申请单 {{ record.applicationCode }}</span>
          </template>
          <template v-else-if="column.key === 'payable'">
            {{ record.payableCode }}
            <span class="table-subtitle">{{ record.supplierName }}</span>
          </template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
          <template v-else-if="column.key === 'method'">{{ methodLabel(record.paymentMethod) }}</template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="approvalOpen" title="审批付款申请" :confirm-loading="saving" @ok="handleApproval">
      <a-alert v-if="selectedApplication" class="section-alert" type="info" :message="`${selectedApplication.code} · ${selectedApplication.supplierName} · ${formatMoney(selectedApplication.requestedAmount)}`" />
      <a-form ref="approvalFormRef" :model="approvalForm" :rules="approvalRules" layout="vertical">
        <a-form-item label="审批结论" name="decision">
          <a-radio-group v-model:value="approvalForm.decision" button-style="solid">
            <a-radio-button value="APPROVED">通过</a-radio-button>
            <a-radio-button value="REJECTED">驳回</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批意见" name="comment"><a-textarea v-model:value="approvalForm.comment" :rows="3" placeholder="填写审批意见" /></a-form-item>
        <a-form-item label="审批人" name="approverName"><a-input v-model:value="approvalForm.approverName" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="paymentOpen" title="执行付款" width="680px" :confirm-loading="saving" @ok="handlePayment">
      <a-alert v-if="selectedApplication" class="section-alert" type="warning" :message="`${selectedApplication.code} · ${selectedApplication.supplierName} · 本次付款 ${formatMoney(selectedApplication.requestedAmount)}`" />
      <a-form ref="paymentFormRef" :model="paymentForm" :rules="paymentRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"><a-form-item label="付款单号" name="paymentCode"><a-input v-model:value="paymentForm.paymentCode" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="付款日期" name="paidDate"><a-input v-model:value="paymentForm.paidDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="付款方式" name="paymentMethod"><a-select v-model:value="paymentForm.paymentMethod" :options="methodOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="付款经办人" name="payerName"><a-input v-model:value="paymentForm.payerName" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="银行流水 / 付款凭证号" name="bankReference"><a-input v-model:value="paymentForm.bankReference" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import AuditOutlined from "@ant-design/icons-vue/AuditOutlined";
import PayCircleOutlined from "@ant-design/icons-vue/PayCircleOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  executePayment,
  listPaymentApplications,
  listPaymentRecords,
  processPaymentApplication,
  type PaymentApplication,
  type PaymentApplicationStatus,
  type PaymentMethod,
  type PaymentRecord,
} from "@/api/finance";
import { useAuthStore } from "@/stores/auth";

type ApprovalDecision = "APPROVED" | "REJECTED";

const auth = useAuthStore();
const applications = ref<PaymentApplication[]>([]);
const payments = ref<PaymentRecord[]>([]);
const selectedApplication = ref<PaymentApplication | null>(null);
const loading = ref(false);
const saving = ref(false);
const approvalOpen = ref(false);
const paymentOpen = ref(false);
const keyword = ref("");
const statusFilter = ref<PaymentApplicationStatus>();
const approvalFormRef = ref();
const paymentFormRef = ref();
const approvalForm = reactive<{ decision: ApprovalDecision; comment: string; approverName: string }>({ decision: "APPROVED", comment: "同意付款", approverName: "" });
const paymentForm = reactive<{ paymentCode: string; paidDate: string; paymentMethod: PaymentMethod; bankReference: string; payerName: string }>({ paymentCode: "", paidDate: today(), paymentMethod: "BANK_TRANSFER", bankReference: "", payerName: "" });

const statusOptions = [
  { label: "待审批", value: "PENDING_APPROVAL" }, { label: "待付款", value: "APPROVED" },
  { label: "已驳回", value: "REJECTED" }, { label: "已付款", value: "PAID" },
];
const methodOptions = [
  { label: "银行转账", value: "BANK_TRANSFER" }, { label: "支票", value: "CHECK" },
  { label: "现金", value: "CASH" }, { label: "其他", value: "OTHER" },
];
const applicationColumns = [
  { title: "申请单", key: "application", width: 200 }, { title: "应付单 / 供应商", key: "payable", width: 240 },
  { title: "申请金额", key: "amount", width: 150 }, { title: "申请人 / 用途", key: "applicant", width: 230 },
  { title: "状态", key: "status", width: 110 }, { title: "审批信息", key: "approval", width: 220 },
  { title: "付款单", key: "payment", width: 180 }, { title: "操作", key: "action", width: 190, fixed: "right" },
];
const paymentColumns = [
  { title: "付款单", key: "payment", width: 230 }, { title: "应付单 / 供应商", key: "payable", width: 250 },
  { title: "实付金额", key: "amount", width: 160 }, { title: "付款日期", dataIndex: "paidDate", width: 130 },
  { title: "付款方式", key: "method", width: 130 }, { title: "流水 / 凭证号", dataIndex: "bankReference", width: 190 },
  { title: "经办人", dataIndex: "payerName", width: 130 },
];
const approvalRules = { decision: [{ required: true }], comment: [{ required: true, message: "请填写审批意见" }], approverName: [{ required: true, message: "请输入审批人" }] };
const paymentRules = { paymentCode: [{ required: true, message: "请输入付款单号" }], paidDate: [{ required: true }], paymentMethod: [{ required: true }], bankReference: [{ required: true, message: "请输入银行流水或付款凭证号" }], payerName: [{ required: true, message: "请输入付款经办人" }] };

const filteredApplications = computed(() => applications.value.filter((item) => {
  const term = keyword.value.trim().toLowerCase();
  const text = `${item.code} ${item.payableCode} ${item.supplierName} ${item.applicantName} ${item.purpose}`.toLowerCase();
  return (!statusFilter.value || item.status === statusFilter.value) && (!term || text.includes(term));
}));
const reservedAmount = computed(() => applications.value.filter((item) => item.status === "PENDING_APPROVAL" || item.status === "APPROVED").reduce((sum, item) => sum + Number(item.requestedAmount || 0), 0));
const paidAmount = computed(() => payments.value.reduce((sum, item) => sum + Number(item.amount || 0), 0));

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    [applications.value, payments.value] = await Promise.all([listPaymentApplications(), listPaymentRecords()]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "付款数据加载失败");
  } finally {
    loading.value = false;
  }
}

function openApproval(item: PaymentApplication) {
  selectedApplication.value = item;
  Object.assign(approvalForm, { decision: "APPROVED", comment: "同意付款", approverName: auth.user?.displayName || "" });
  approvalOpen.value = true;
}

function openPayment(item: PaymentApplication) {
  selectedApplication.value = item;
  Object.assign(paymentForm, { paymentCode: generateCode("FK"), paidDate: today(), paymentMethod: "BANK_TRANSFER", bankReference: "", payerName: auth.user?.displayName || "" });
  paymentOpen.value = true;
}

async function handleApproval() {
  await approvalFormRef.value?.validate();
  if (!selectedApplication.value) return;
  saving.value = true;
  try {
    await processPaymentApplication(selectedApplication.value.id, { ...approvalForm });
    approvalOpen.value = false;
    message.success(approvalForm.decision === "APPROVED" ? "付款申请已通过" : "付款申请已驳回，占用金额已释放");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "付款审批失败");
  } finally {
    saving.value = false;
  }
}

async function handlePayment() {
  await paymentFormRef.value?.validate();
  if (!selectedApplication.value) return;
  saving.value = true;
  try {
    await executePayment(selectedApplication.value.id, { ...paymentForm });
    paymentOpen.value = false;
    message.success("付款已执行，应付余额已同步更新");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "付款执行失败");
  } finally {
    saving.value = false;
  }
}

function hasAction(item: PaymentApplication) {
  return (auth.can("finance:payment:approve") && item.status === "PENDING_APPROVAL") || (auth.can("finance:payment:execute") && item.status === "APPROVED");
}
function countByStatus(status: PaymentApplicationStatus) { return applications.value.filter((item) => item.status === status).length; }
function statusLabel(status: PaymentApplicationStatus) { return ({ PENDING_APPROVAL: "待审批", APPROVED: "待付款", REJECTED: "已驳回", PAID: "已付款" } as Record<PaymentApplicationStatus, string>)[status]; }
function statusColor(status: PaymentApplicationStatus) { return ({ PENDING_APPROVAL: "orange", APPROVED: "blue", REJECTED: "red", PAID: "green" } as Record<PaymentApplicationStatus, string>)[status]; }
function methodLabel(method: PaymentMethod) { return ({ BANK_TRANSFER: "银行转账", CHECK: "支票", CASH: "现金", OTHER: "其他" } as Record<PaymentMethod, string>)[method]; }
function today() { const value = new Date(); return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`; }
function generateCode(prefix: string) { const value = new Date(); return `${prefix}-${today().replaceAll("-", "")}-${String(value.getHours()).padStart(2, "0")}${String(value.getMinutes()).padStart(2, "0")}${String(value.getSeconds()).padStart(2, "0")}${String(value.getMilliseconds()).padStart(3, "0")}`; }
function formatMoney(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0); }
function moneyFormatter(value: number | string) { return formatMoney(Number(value)); }
</script>
