<template>
  <div class="page-stack">
    <a-card title="合同应收">
      <template #extra><a-button @click="loadData">刷新</a-button></template>

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

      <a-table :columns="columns" :data-source="filteredItems" :loading="loading" :row-key="(record: Receivable) => record.id" :scroll="{ x: 1060 }">
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
                v-if="auth.can('crm:receivable:invoice') && !record.invoiceNo && record.status !== 'SETTLED'"
                size="small"
                type="link"
                @click="openInvoice(record)"
              >
                登记开票
              </a-button>
              <a-button
                v-if="auth.can('crm:receivable:settle') && record.invoiceNo && record.outstandingAmount > 0"
                size="small"
                type="link"
                @click="openReceipt(record)"
              >
                登记回款
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="invoiceOpen" title="登记开票" :confirm-loading="saving" @ok="handleInvoice">
      <a-alert v-if="selectedItem" class="section-alert" type="info" :message="`${selectedItem.code} · ${selectedItem.customerName} · ${formatMoney(selectedItem.amount)}`" />
      <a-form ref="invoiceFormRef" :model="invoiceForm" :rules="invoiceRules" layout="vertical">
        <a-form-item label="发票号码" name="invoiceNo"><a-input v-model:value="invoiceForm.invoiceNo" /></a-form-item>
        <a-form-item label="开票日期" name="invoiceDate"><a-input v-model:value="invoiceForm.invoiceDate" type="date" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="receiptOpen" title="登记回款" :confirm-loading="saving" @ok="handleReceipt">
      <a-alert v-if="selectedItem" class="section-alert" type="info" :message="`${selectedItem.code} · 待收 ${formatMoney(selectedItem.outstandingAmount)}`" />
      <a-form ref="receiptFormRef" :model="receiptForm" :rules="receiptRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"><a-form-item label="本次回款" name="amount"><a-input-number v-model:value="receiptForm.amount" :min="0.01" :max="selectedItem?.outstandingAmount" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="回款日期" name="receivedDate"><a-input v-model:value="receiptForm.receivedDate" type="date" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="银行流水号" name="referenceNo"><a-input v-model:value="receiptForm.referenceNo" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="登记人" name="recorderName"><a-input v-model:value="receiptForm.recorderName" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import {
  listReceivables,
  recordReceivableReceipt,
  registerReceivableInvoice,
  type Receivable,
  type ReceivableStatus,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import { formatMoney, receivableStatusColor, receivableStatusLabel } from "./crm-options";

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
const invoiceForm = reactive({ invoiceNo: "", invoiceDate: today() });
const receiptForm = reactive({ amount: 0, receivedDate: today(), referenceNo: "", recorderName: "" });
const invoiceRules = {
  invoiceNo: [{ required: true, message: "请输入发票号码" }],
  invoiceDate: [{ required: true, message: "请选择开票日期" }],
};
const receiptRules = {
  amount: [{ required: true, message: "请输入回款金额" }],
  receivedDate: [{ required: true, message: "请选择回款日期" }],
  referenceNo: [{ required: true, message: "请输入银行流水号" }],
  recorderName: [{ required: true, message: "请输入登记人" }],
};
const statusOptions = [
  { label: "待开票", value: "INVOICE_PENDING" },
  { label: "待回款", value: "PAYMENT_PENDING" },
  { label: "已核销", value: "SETTLED" },
  { label: "逾期", value: "OVERDUE" },
];
const columns = [
  { title: "应收单 / 客户", key: "receivable", width: 240 },
  { title: "合同编号", key: "contract", width: 170 },
  { title: "来源单号", dataIndex: "sourceNo", width: 170 },
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

function openInvoice(record: Receivable) {
  selectedItem.value = record;
  Object.assign(invoiceForm, { invoiceNo: "", invoiceDate: today() });
  invoiceOpen.value = true;
}

async function handleInvoice() {
  await invoiceFormRef.value?.validate();
  if (!selectedItem.value) return;
  saving.value = true;
  try {
    await registerReceivableInvoice(selectedItem.value.id, { ...invoiceForm });
    invoiceOpen.value = false;
    message.success("开票信息已登记");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "开票登记失败");
  } finally {
    saving.value = false;
  }
}

function openReceipt(record: Receivable) {
  selectedItem.value = record;
  Object.assign(receiptForm, {
    amount: record.outstandingAmount,
    receivedDate: today(),
    referenceNo: "",
    recorderName: auth.user?.displayName || "",
  });
  receiptOpen.value = true;
}

async function handleReceipt() {
  await receiptFormRef.value?.validate();
  if (!selectedItem.value) return;
  saving.value = true;
  try {
    const updated = await recordReceivableReceipt(selectedItem.value.id, { ...receiptForm });
    receiptOpen.value = false;
    message.success(updated.status === "SETTLED" ? "回款已登记，应收已核销" : "部分回款已登记");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "回款登记失败");
  } finally {
    saving.value = false;
  }
}

function today() {
  const value = new Date();
  return new Date(value.getTime() - value.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}
</script>
