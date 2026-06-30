<template>
  <div class="page-stack">
    <a-card title="应收管理">
      <template #extra><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="应收余额" :value="outstandingAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待开票" :value="amountByStatus('INVOICE_PENDING')" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待回款" :value="amountByStatus('PAYMENT_PENDING')" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="逾期应收" :value="amountByStatus('OVERDUE')" :formatter="moneyFormatter" /></a-col>
      </a-row>
      <a-space wrap class="table-toolbar">
        <a-input-search v-model:value="keyword" allow-clear placeholder="搜索应收单、客户、合同" style="width: 260px" />
        <a-select v-model:value="statusFilter" allow-clear placeholder="全部状态" :options="statusOptions" style="width: 140px" />
      </a-space>
      <a-table :columns="columns" :data-source="filteredItems" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: Receivable) => item.id" :scroll="{ x: 1320 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'receivable'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.customerName }}</span></template>
          <template v-else-if="column.key === 'contract'">{{ record.contractCode || '-' }}</template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong><span class="table-subtitle">已收 {{ formatMoney(record.settledAmount) }} · 待收 {{ formatMoney(record.outstandingAmount) }}</span></template>
          <template v-else-if="column.key === 'invoice'">{{ record.invoiceNo || '未开票' }}<span v-if="record.invoiceDate" class="table-subtitle">{{ record.invoiceDate }}</span></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag></template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button v-if="auth.can('finance:receivable:invoice') && !record.invoiceNo && record.status !== 'SETTLED'" type="link" size="small" @click="openInvoice(record)">登记开票</a-button>
              <a-button v-if="auth.can('finance:receivable:collect') && record.invoiceNo && record.status !== 'SETTLED'" type="link" size="small" @click="openReceipt(record)">登记回款</a-button>
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
          <a-col :span="12"><a-form-item label="本次回款" name="amount"><a-input-number v-model:value="receiptForm.amount" :min="0.01" :max="selectedItem?.outstandingAmount" class="full-input" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="回款日期" name="receivedDate"><a-input v-model:value="receiptForm.receivedDate" type="date" /></a-form-item></a-col>
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
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import type { Receivable, ReceivableStatus } from "@/api/crm";
import { listFinanceReceivables, recordFinanceReceipt, registerFinanceInvoice } from "@/api/finance";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const items = ref<Receivable[]>([]);
const loading = ref(false);
const saving = ref(false);
const invoiceOpen = ref(false);
const receiptOpen = ref(false);
const selectedItem = ref<Receivable | null>(null);
const keyword = ref("");
const statusFilter = ref<ReceivableStatus>();
const invoiceFormRef = ref();
const receiptFormRef = ref();
const invoiceForm = reactive({ invoiceNo: "", invoiceDate: today() });
const receiptForm = reactive({ amount: 0, receivedDate: today(), referenceNo: "", recorderName: "" });
const statusOptions = [
  { label: "待开票", value: "INVOICE_PENDING" }, { label: "待回款", value: "PAYMENT_PENDING" },
  { label: "已核销", value: "SETTLED" }, { label: "逾期", value: "OVERDUE" },
];
const columns = [
  { title: "应收单 / 客户", key: "receivable", width: 240 }, { title: "合同编号", key: "contract", width: 170 },
  { title: "来源单号", dataIndex: "sourceNo", width: 170 }, { title: "应收 / 回款", key: "amount", width: 260 },
  { title: "开票信息", key: "invoice", width: 180 }, { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "状态", key: "status", width: 110 }, { title: "操作", key: "action", width: 170, fixed: "right" },
];
const invoiceRules = { invoiceNo: [{ required: true, message: "请输入发票号码" }], invoiceDate: [{ required: true }] };
const receiptRules = { amount: [{ required: true, message: "请输入回款金额" }], receivedDate: [{ required: true }], referenceNo: [{ required: true, message: "请输入银行流水号" }], recorderName: [{ required: true, message: "请输入登记人" }] };
const filteredItems = computed(() => items.value.filter((item) => {
  const term = keyword.value.trim().toLowerCase();
  const text = `${item.code} ${item.contractCode || ""} ${item.customerName}`.toLowerCase();
  return (!statusFilter.value || item.status === statusFilter.value) && (!term || text.includes(term));
}));
const outstandingAmount = computed(() => items.value.reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0));

onMounted(loadData);
async function loadData() { loading.value = true; try { items.value = await listFinanceReceivables(); } catch (error) { message.error(error instanceof Error ? error.message : "应收加载失败"); } finally { loading.value = false; } }
function amountByStatus(status: ReceivableStatus) { return items.value.filter((item) => item.status === status).reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0); }
function openInvoice(item: Receivable) { selectedItem.value = item; Object.assign(invoiceForm, { invoiceNo: "", invoiceDate: today() }); invoiceOpen.value = true; }
function openReceipt(item: Receivable) { selectedItem.value = item; Object.assign(receiptForm, { amount: item.outstandingAmount, receivedDate: today(), referenceNo: "", recorderName: auth.user?.displayName || "" }); receiptOpen.value = true; }
async function handleInvoice() { await invoiceFormRef.value?.validate(); if (!selectedItem.value) return; saving.value = true; try { await registerFinanceInvoice(selectedItem.value.id, { ...invoiceForm }); invoiceOpen.value = false; message.success("开票信息已登记"); await loadData(); } catch (error) { message.error(error instanceof Error ? error.message : "开票登记失败"); } finally { saving.value = false; } }
async function handleReceipt() { await receiptFormRef.value?.validate(); if (!selectedItem.value) return; saving.value = true; try { const result = await recordFinanceReceipt(selectedItem.value.id, { ...receiptForm }); receiptOpen.value = false; message.success(result.status === "SETTLED" ? "应收已全部核销" : "部分回款已登记"); await loadData(); } catch (error) { message.error(error instanceof Error ? error.message : "回款登记失败"); } finally { saving.value = false; } }
function today() { const value = new Date(); return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`; }
function statusLabel(status: ReceivableStatus) { return ({ INVOICE_PENDING: "待开票", PAYMENT_PENDING: "待回款", SETTLED: "已核销", OVERDUE: "逾期" } as Record<ReceivableStatus, string>)[status]; }
function statusColor(status: ReceivableStatus) { return ({ INVOICE_PENDING: "orange", PAYMENT_PENDING: "blue", SETTLED: "green", OVERDUE: "red" } as Record<ReceivableStatus, string>)[status]; }
function formatMoney(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0); }
function moneyFormatter(value: number | string) { return formatMoney(Number(value)); }
</script>
