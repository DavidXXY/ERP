<template>
  <div class="page-stack">
    <a-card title="应付管理">
      <template #extra><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="应付余额" :value="outstandingAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="审批占用" :value="reservedAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="可申请付款" :value="availableAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="逾期应付" :value="overdueAmount" :formatter="moneyFormatter" /></a-col>
      </a-row>
      <a-space wrap class="table-toolbar">
        <a-input-search v-model:value="keyword" allow-clear placeholder="搜索应付单、供应商、采购单" style="width: 280px" />
        <a-select v-model:value="statusFilter" allow-clear placeholder="全部状态" :options="statusOptions" style="width: 140px" />
      </a-space>
      <a-table :columns="columns" :data-source="filteredItems" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: FinancePayable) => item.id" :scroll="{ x: 1280 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'payable'"><strong>{{ record.code }}</strong><span class="table-subtitle">采购单 {{ record.orderCode }}</span></template>
          <template v-else-if="column.key === 'supplier'">{{ record.supplierName }}</template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong><span class="table-subtitle">已付 {{ formatMoney(record.paidAmount) }} · 待付 {{ formatMoney(record.outstandingAmount) }}</span></template>
          <template v-else-if="column.key === 'available'">{{ formatMoney(record.availableAmount) }}<span class="table-subtitle">审批占用 {{ formatMoney(record.reservedAmount) }}</span></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag><a-tag v-if="record.overdue" color="red">逾期</a-tag></template>
          <template v-else-if="column.key === 'action'"><a-button v-if="auth.can('finance:payment:apply') && record.availableAmount > 0" type="link" size="small" @click="openApplication(record)">申请付款</a-button><span v-else class="muted">暂无可申请金额</span></template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="applicationOpen" title="新增付款申请" width="720px" :confirm-loading="saving" @ok="handleCreateApplication">
      <a-alert v-if="selectedItem" class="section-alert" type="info" :message="`${selectedItem.code} · ${selectedItem.supplierName} · 可申请 ${formatMoney(selectedItem.availableAmount)}`" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="申请金额" name="requestedAmount"><a-input-number v-model:value="form.requestedAmount" :min="0.01" :max="selectedItem?.availableAmount" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="申请日期" name="requestedDate"><a-input v-model:value="form.requestedDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="申请人" name="applicantName"><a-input v-model:value="form.applicantName" /></a-form-item></a-col>
          <a-col :xs="24" :md="16"><a-form-item label="付款用途" name="purpose"><a-input v-model:value="form.purpose" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { createPaymentApplication, listFinancePayables, type FinancePayable, type FinancePayableStatus } from "@/api/finance";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const items = ref<FinancePayable[]>([]);
const selectedItem = ref<FinancePayable | null>(null);
const loading = ref(false);
const saving = ref(false);
const applicationOpen = ref(false);
const keyword = ref("");
const statusFilter = ref<FinancePayableStatus>();
const formRef = ref();
const form = reactive({ code: "", requestedAmount: 0, requestedDate: today(), applicantName: "", purpose: "" });
const statusOptions = [
  { label: "待付款", value: "PENDING" }, { label: "部分付款", value: "PARTIAL_PAID" },
  { label: "已付款", value: "PAID" }, { label: "已取消", value: "CANCELLED" },
];
const columns = [
  { title: "应付单", key: "payable", width: 230 }, { title: "供应商", key: "supplier", width: 220 },
  { title: "应付 / 已付", key: "amount", width: 260 }, { title: "可申请 / 占用", key: "available", width: 210 },
  { title: "到期日", dataIndex: "dueDate", width: 120 }, { title: "状态", key: "status", width: 160 },
  { title: "操作", key: "action", width: 130, fixed: "right" },
];
const rules = { code: [], requestedAmount: [{ required: true, message: "请输入申请金额" }], requestedDate: [{ required: true }], applicantName: [{ required: true, message: "请输入申请人" }], purpose: [{ required: true, message: "请输入付款用途" }] };
const filteredItems = computed(() => items.value.filter((item) => {
  const term = keyword.value.trim().toLowerCase();
  const text = `${item.code} ${item.orderCode} ${item.supplierName}`.toLowerCase();
  return (!statusFilter.value || item.status === statusFilter.value) && (!term || text.includes(term));
}));
const outstandingAmount = computed(() => items.value.reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0));
const reservedAmount = computed(() => items.value.reduce((sum, item) => sum + Number(item.reservedAmount || 0), 0));
const availableAmount = computed(() => items.value.reduce((sum, item) => sum + Number(item.availableAmount || 0), 0));
const overdueAmount = computed(() => items.value.filter((item) => item.overdue).reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0));

onMounted(loadData);
async function loadData() { loading.value = true; try { items.value = await listFinancePayables(); } catch (error) { message.error(error instanceof Error ? error.message : "应付加载失败"); } finally { loading.value = false; } }
function openApplication(item: FinancePayable) { selectedItem.value = item; Object.assign(form, { code: generateCode(), requestedAmount: item.availableAmount, requestedDate: today(), applicantName: auth.user?.displayName || "", purpose: `支付${item.orderCode}采购货款` }); applicationOpen.value = true; }
async function handleCreateApplication() { await formRef.value?.validate(); if (!selectedItem.value) return; saving.value = true; try { await createPaymentApplication({ ...form, payableId: selectedItem.value.id }); applicationOpen.value = false; message.success("付款申请已提交审批"); await loadData(); } catch (error) { message.error(error instanceof Error ? error.message : "付款申请失败"); } finally { saving.value = false; } }
function today() { const value = new Date(); return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`; }
function generateCode() { const value = new Date(); return `FKSQ-${today().replaceAll("-", "")}-${String(value.getHours()).padStart(2, "0")}${String(value.getMinutes()).padStart(2, "0")}${String(value.getSeconds()).padStart(2, "0")}${String(value.getMilliseconds()).padStart(3, "0")}`; }
function statusLabel(status: FinancePayableStatus) { return ({ PENDING: "待付款", PARTIAL_PAID: "部分付款", PAID: "已付款", CANCELLED: "已取消" } as Record<FinancePayableStatus, string>)[status]; }
function statusColor(status: FinancePayableStatus) { return ({ PENDING: "orange", PARTIAL_PAID: "blue", PAID: "green", CANCELLED: "default" } as Record<FinancePayableStatus, string>)[status]; }
function formatMoney(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0); }
function moneyFormatter(value: number | string) { return formatMoney(Number(value)); }
</script>
