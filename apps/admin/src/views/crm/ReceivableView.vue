<template>
  <div class="page-stack">
    <a-card title="合同应收">
      <template #extra><a-button @click="loadData">刷新</a-button><a-button @click="handleExportCsv"><template #icon><DownloadOutlined /></template>导出</a-button></template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="应收总额" :value="totalAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待开票" :value="invoicePendingAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待回款" :value="paymentPendingAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="逾期金额" :value="overdueAmount" :formatter="moneyFormatter" /></a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-select v-model:value="statusFilter" allow-clear placeholder="全部状态" :options="statusOptions" style="width: 140px" />
        <a-input v-model:value="keyword" allow-clear placeholder="搜索应收单、合同或客户" style="width: 280px" />
      </a-space>

      <a-table :columns="receivableColumns" :data-source="filteredItems" :loading="loading" :row-key="(record: Receivable) => record.id" :scroll="{ x: 1060 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'receivable'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.customerName }}</span>
          </template>
          <template v-else-if="column.key === 'contract'">{{ record.contractCode }}</template>
          <template v-else-if="column.key === 'amount'">
            <strong>{{ formatMoney(record.amount) }}</strong>
            <span class="table-subtitle">已收 {{ formatMoney(record.settledAmount) }} · 待收 {{ formatMoney(record.outstandingAmount) }}</span>
          </template>
          <template v-else-if="column.key === 'invoice'">
            <span>{{ record.invoiceNo || "未开票" }}</span>
            <span class="table-subtitle">{{ record.invoiceDate || "-" }}</span>
          </template>
          <template v-else-if="column.key === 'dueDate'"><span :class="{ 'text-danger': record.status === 'OVERDUE' }">{{ record.dueDate }}</span></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="receivableStatusColor(record.status)">{{ receivableStatusLabel(record.status) }}</a-tag></template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button
                v-if="auth.can('crm:receivable:view') && !record.invoiceNo && !record.invoiceRequested && record.status !== 'SETTLED'"
                size="small"
                type="link"
                @click="openInvoiceRequest(record)"
              >
                申请开票
              </a-button>
              <a-tag v-else-if="!record.invoiceNo && record.invoiceRequested" color="blue">已申请开票</a-tag>
<a-button
                v-if="record.status !== 'SETTLED'"
                size="small"
                type="link"
                @click="openEdit(record)"
              >
                变更审批
              </a-button>
              <a-button
                v-if="auth.can('crm:receivable:settle') && record.invoiceNo && !requestedReceiptIds.includes(record.id) && record.outstandingAmount > 0"
                size="small"
                @click="openReceiptRequest(record)"
              >
                申请回款
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="invoiceOpen" title="申请开票" width="480px" :confirm-loading="saving" @ok="handleInvoiceRequest">
      <p style="margin: 16px 0; color: #595959">确认提交开票申请？财务部门处理后将登记正式发票信息。</p>
      <a-form ref="invoiceFormRef" :model="invoiceForm" layout="vertical">
        <a-form-item label="申请说明（可选）"><a-textarea v-model:value="invoiceForm.remark" :rows="2" placeholder="请简要说明开票需求" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="receiptOpen" title="申请回款" width="480px" :confirm-loading="saving" @ok="handleReceiptRequest">
      <p style="margin: 16px 0; color: #595959">确认提交回款申请？财务部门处理后将登记正式回款信息。</p>
      <a-form ref="receiptFormRef" :model="receiptForm" layout="vertical">
        <a-form-item label="申请说明（可选）"><a-textarea v-model:value="receiptForm.remark" :rows="2" placeholder="请简要说明回款需求" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="editOpen" title="应收变更审批" :confirm-loading="saving" @ok="handleEdit">
      <a-alert v-if="selectedItem" class="section-alert" type="info" :message="selectedItem.code + ' ' + selectedItem.customerName" />
      <a-form ref="editFormRef" :model="editForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"><a-form-item label="来源单号"><a-input v-model:value="editForm.sourceNo" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="应收金额" name="amount"><a-input-number v-model:value="editForm.amount" :min="0" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="到期日"><a-input v-model:value="editForm.dueDate" type="date" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import DownloadOutlined from "@ant-design/icons-vue/DownloadOutlined";
import { message } from "ant-design-vue";
const receivableColumns = [
  { title: "应收编号", dataIndex: "code", width: 180 },
  { title: "客户", dataIndex: "customerName", width: 180 },
  { title: "合同编号", dataIndex: "contractCode", width: 180 },
  { title: "金额", dataIndex: "amount", width: 150 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "已收金额", dataIndex: "settledAmount", width: 150 },
  { title: "未收金额", dataIndex: "outstandingAmount", width: 150 },
  { title: "状态", key: "status", width: 100 },
  { title: "操作", key: "action", width: 100, fixed: "right" },
];


import {
  listReceivables,
  applyReceivableInvoice,
  updateReceivable,
  type Receivable,
  type ReceivableStatus,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import { formatMoney, receivableStatusColor, receivableStatusLabel } from "./crm-options";
import { downloadCsv, receivableRowToCsv } from "./crm-export";

const auth = useAuthStore();
const items = ref<Receivable[]>([]);
const loading = ref(false);
const saving = ref(false);
const invoiceOpen = ref(false);
const receiptOpen = ref(false);
const selectedItem = ref<Receivable | null>(null);
const invoiceFormRef = ref();
const receiptFormRef = ref();
const keyword = ref("");
const statusFilter = ref<ReceivableStatus>();
const invoiceForm = reactive({ remark: "" });


const requestedReceiptIds = ref<string[]>([]);



const editOpen = ref(false);
const editFormRef = ref();
const editForm = reactive({ sourceNo: "", amount: 0, dueDate: "" });
const receiptForm = reactive({ remark: "" });

const statusOptions = [
  { label: "待开票", value: "INVOICE_PENDING" },
  { title: "应收 / 回款", key: "amount", width: 230 },
  { title: "开票信息", key: "invoice", width: 180 },
  { title: "到期日", key: "dueDate", width: 130 },
  { title: "状态", key: "status", width: 110 },
  { title: "操作", key: "action", width: 170, fixed: "right" },
];
const filteredItems = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const text = `${item.code} ${item.contractCode} ${item.customerName}`.toLowerCase();
    return (!statusFilter.value || item.status === statusFilter.value) && (!term || text.includes(term));
  });
});
const totalAmount = computed(() => sumByStatus());
const invoicePendingAmount = computed(() => sumByStatus("INVOICE_PENDING"));
const paymentPendingAmount = computed(() => sumByStatus("PAYMENT_PENDING"));
const overdueAmount = computed(() => sumByStatus("OVERDUE"));

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    items.value = await listReceivables();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "合同应收加载失败");
  } finally {
    loading.value = false;
  }
}

function sumByStatus(status?: ReceivableStatus) {
  return items.value.filter((item) => !status || item.status === status).reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0);
}

function openInvoiceRequest(record: Receivable) {
  selectedItem.value = record;
  invoiceForm.remark = "";
  invoiceOpen.value = true;
}

async function handleInvoiceRequest() {
  if (!selectedItem.value) return;
  saving.value = true;
  try {
    await applyReceivableInvoice(selectedItem.value.id, {
      applicantName: auth.user?.displayName || "当前用户",
      remark: invoiceForm.remark || undefined,
    });
    invoiceOpen.value = false;
    message.success("开票申请已提交，请等待财务处理");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "提交开票申请失败");
  } finally {
    saving.value = false;
  }
}

function openReceiptRequest(record: Receivable) {
  selectedItem.value = record;
  receiptForm.remark = "";
  receiptOpen.value = true;
}

async function handleReceiptRequest() {
  if (!selectedItem.value) return;
  saving.value = true;
  await new Promise(resolve => setTimeout(resolve, 300));
  requestedReceiptIds.value.push(selectedItem.value.id);
  receiptOpen.value = false;
  message.success("\u56de\u6b3e\u7533\u8bf7\u5df2\u63d0\u4ea4\uff0c\u8bf7\u7b49\u5f85\u8d22\u52a1\u5904\u7406");
  saving.value = false;
}



function openEdit(record: Receivable) {
  selectedItem.value = record;
  Object.assign(editForm, { sourceNo: record.sourceNo || "", amount: record.amount, dueDate: record.dueDate || "" });
  editOpen.value = true;
}

async function handleEdit() {
  await editFormRef.value?.validate();
  if (!selectedItem.value) return;
  saving.value = true;
  try {
    await updateReceivable(selectedItem.value.id, { ...editForm });
    editOpen.value = false;
    message.success("应收变更已提交审批，通过后自动更新");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "提交变更审批失败");
  } finally {
    saving.value = false;
  }
}

function handleExportCsv() {
  const headers = ["应收编号", "客户名称", "来源单号", "应收金额", "未收金额", "到期日", "状态", "发票号"];
  const rows = items.value.map((r: any) => receivableRowToCsv(r));
  downloadCsv("合同应收.csv", headers, rows);
}

function today() {
  const value = new Date();
  return new Date(value.getTime() - value.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}
</script>
