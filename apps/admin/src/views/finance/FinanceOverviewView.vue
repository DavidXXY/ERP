<template>
  <div class="page-stack">
    <a-card title="资金总览">
      <template #extra><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-alert v-if="errorMessage" class="section-alert" type="warning" show-icon :message="errorMessage" />

      <a-row :gutter="[16, 20]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="应收总额" :value="overview.receivableAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="已回款" :value="overview.receivedAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待收金额" :value="overview.receivableOutstanding" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="逾期应收" :value="overview.receivableOverdue" :formatter="moneyFormatter" :value-style="dangerStyle(overview.receivableOverdue)" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="应付总额" :value="overview.payableAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="已付款" :value="overview.paidAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待付金额" :value="overview.payableOutstanding" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="逾期应付" :value="overview.payableOverdue" :formatter="moneyFormatter" :value-style="dangerStyle(overview.payableOverdue)" /></a-col>
      </a-row>

      <a-descriptions bordered :column="2" size="middle">
        <a-descriptions-item label="资金净流入"><strong :class="{ 'text-danger': overview.netCashInflow < 0 }">{{ formatMoney(overview.netCashInflow) }}</strong></a-descriptions-item>
        <a-descriptions-item label="待审批付款申请"><a-tag color="orange">{{ overview.pendingPaymentApplications }} 笔</a-tag></a-descriptions-item>
        <a-descriptions-item label="应收回款率"><a-progress :percent="receivableRate" size="small" /></a-descriptions-item>
        <a-descriptions-item label="应付付款率"><a-progress :percent="payableRate" size="small" /></a-descriptions-item>
      </a-descriptions>

      <a-space wrap class="overview-actions">
        <a-button v-if="auth.can('finance:receivable:view')" @click="router.push('/finance/receivables')">查看应收</a-button>
        <a-button v-if="auth.can('finance:payable:view')" @click="router.push('/finance/payables')">查看应付</a-button>
        <a-button v-if="auth.can('finance:payable:view')" type="primary" @click="router.push('/finance/payment-applications')">处理付款申请</a-button>
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { getFinanceOverview, type FinanceOverview } from "@/api/finance";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const errorMessage = ref("");
const overview = reactive<FinanceOverview>(emptyOverview());
const receivableRate = computed(() => ratio(overview.receivedAmount, overview.receivableAmount));
const payableRate = computed(() => ratio(overview.paidAmount, overview.payableAmount));

onMounted(loadData);

async function loadData() {
  loading.value = true; errorMessage.value = "";
  try { Object.assign(overview, await getFinanceOverview()); }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : "资金数据加载失败"; }
  finally { loading.value = false; }
}
function emptyOverview(): FinanceOverview { return { receivableAmount: 0, receivedAmount: 0, receivableOutstanding: 0, receivableOverdue: 0, payableAmount: 0, paidAmount: 0, payableOutstanding: 0, payableOverdue: 0, netCashInflow: 0, pendingPaymentApplications: 0 }; }
function ratio(value: number, total: number) { return total > 0 ? Math.min(100, Math.round(value / total * 100)) : 0; }
function dangerStyle(value: number) { return value > 0 ? { color: "#cf1322" } : {}; }
function formatMoney(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0); }
function moneyFormatter(value: number | string) { return formatMoney(Number(value)); }
</script>

<style scoped>
.overview-actions {
  margin-top: 18px;
}
</style>
