<template>
  <div class="page-stack">
    <a-card title="资金总览">
      <template #extra
        ><a-button :loading="loading" @click="loadData"
          ><template #icon><ReloadOutlined /></template>刷新</a-button
        ></template
      >
      <a-alert
        v-if="errorMessage"
        class="section-alert"
        type="warning"
        show-icon
        :message="errorMessage"
      />

      <a-row :gutter="[16, 20]" class="metric-row">
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="应收总额"
            :value="overview.receivableAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="已回款"
            :value="overview.receivedAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="待收金额"
            :value="overview.receivableOutstanding"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="逾期应收"
            :value="overview.receivableOverdue"
            :formatter="moneyFormatter"
            :value-style="dangerStyle(overview.receivableOverdue)"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="应付总额"
            :value="overview.payableAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="已付款"
            :value="overview.paidAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="待付金额"
            :value="overview.payableOutstanding"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="逾期应付"
            :value="overview.payableOverdue"
            :formatter="moneyFormatter"
            :value-style="dangerStyle(overview.payableOverdue)"
        /></a-col>
      </a-row>

      <a-descriptions bordered :column="2" size="middle">
        <a-descriptions-item label="资金净流入"
          ><strong :class="{ 'text-danger': overview.netCashInflow < 0 }">{{
            formatMoney(overview.netCashInflow)
          }}</strong></a-descriptions-item
        >
        <a-descriptions-item label="待审批付款申请"
          ><a-tag color="orange"
            >{{ overview.pendingPaymentApplications }} 笔</a-tag
          ></a-descriptions-item
        >
        <a-descriptions-item label="应收回款率"
          ><a-progress :percent="receivableRate" size="small"
        /></a-descriptions-item>
        <a-descriptions-item label="应付付款率"
          ><a-progress :percent="payableRate" size="small"
        /></a-descriptions-item>
      </a-descriptions>

      <section class="finance-panel monthly-panel">
        <div class="panel-heading monthly-heading">
          <div>
            <h3>月度收支</h3>
            <p>按实际入账日期统计每月收入与支出</p>
          </div>
          <a-select v-model:value="selectedYear" class="year-select">
            <a-select-option
              v-for="year in availableYears"
              :key="year"
              :value="year"
            >
              {{ year }} 年
            </a-select-option>
          </a-select>
        </div>
        <div class="annual-summary">
          <div>
            <span>年度收入</span
            ><strong class="income-text">{{
              formatMoney(annualTotals.income)
            }}</strong>
          </div>
          <div>
            <span>年度支出</span
            ><strong class="expense-text">{{
              formatMoney(annualTotals.expense)
            }}</strong>
          </div>
          <div>
            <span>年度净额</span
            ><strong
              :class="annualTotals.net < 0 ? 'expense-text' : 'income-text'"
              >{{ formatMoney(annualTotals.net) }}</strong
            >
          </div>
        </div>
        <div v-if="auth.can('finance:ledger:view')" class="monthly-table">
          <div class="monthly-row monthly-table-head">
            <span>月份</span><span>收支趋势</span><span>收入</span
            ><span>支出</span><span>净额</span>
          </div>
          <div
            v-for="item in monthlyFinance"
            :key="item.month"
            class="monthly-row"
          >
            <strong>{{ item.month }}月</strong>
            <div class="month-bars" aria-hidden="true">
              <i class="income-bar" :style="{ width: `${item.incomeRate}%` }" />
              <i
                class="expense-bar"
                :style="{ width: `${item.expenseRate}%` }"
              />
            </div>
            <span class="income-text">{{ formatMoney(item.income) }}</span>
            <span class="expense-text">{{ formatMoney(item.expense) }}</span>
            <strong :class="item.net < 0 ? 'expense-text' : 'income-text'">{{
              formatMoney(item.net)
            }}</strong>
          </div>
        </div>
        <a-empty
          v-else
          :image="simpleImage"
          description="需要总账查看权限才能查看月度收支"
        />
      </section>

      <section class="finance-control-grid">
        <div class="finance-panel">
          <div class="panel-heading">
            <h3>未来现金流</h3>
            <a-tag :color="forecastTotal.net >= 0 ? 'green' : 'red'"
              >{{ forecastTotal.net >= 0 ? "净流入" : "净流出" }}
              {{ formatMoney(Math.abs(forecastTotal.net)) }}</a-tag
            >
          </div>
          <div class="forecast-grid">
            <button
              v-for="bucket in forecastBuckets"
              :key="bucket.key"
              class="forecast-card"
              type="button"
              @click="openForecast(bucket.key)"
            >
              <span>{{ bucket.label }}</span>
              <strong :class="{ 'text-danger': bucket.net < 0 }">{{
                formatMoney(bucket.net)
              }}</strong>
              <small
                >应收 {{ formatMoney(bucket.receivable) }} / 应付
                {{ formatMoney(bucket.payable) }}</small
              >
            </button>
          </div>
        </div>

        <div class="finance-panel">
          <div class="panel-heading">
            <h3>账龄结构</h3>
            <a-tag color="blue">按未结金额</a-tag>
          </div>
          <div class="aging-list">
            <div v-for="item in agingBuckets" :key="item.key" class="aging-row">
              <span>{{ item.label }}</span>
              <div>
                <strong>{{ formatMoney(item.receivable) }}</strong>
                <small>应付 {{ formatMoney(item.payable) }}</small>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="finance-panel risk-panel">
        <div class="panel-heading">
          <h3>财务风险事项</h3>
          <a-button size="small" @click="router.push('/risk-center')"
            >进入风险中心</a-button
          >
        </div>
        <div class="risk-list">
          <button
            v-for="item in riskItems"
            :key="item.key"
            class="risk-row"
            type="button"
            @click="router.push(item.route)"
          >
            <a-tag :color="item.color">{{ item.level }}</a-tag>
            <span>{{ item.title }}</span>
            <strong>{{ item.value }}</strong>
          </button>
          <a-empty
            v-if="riskItems.length === 0"
            :image="simpleImage"
            description="暂无高优先级财务风险"
          />
        </div>
      </section>

      <section
        v-if="auth.can('finance:ledger:view')"
        class="finance-panel reconciliation-panel"
      >
        <div class="panel-heading">
          <h3>业务与总账一致性</h3>
          <a-button size="small" @click="router.push('/finance/ledger')"
            >打开总账</a-button
          >
        </div>
        <div class="reconciliation-grid">
          <div
            v-for="item in reconciliationItems"
            :key="item.key"
            class="reconciliation-card"
          >
            <span>{{ item.label }}</span>
            <strong :class="{ 'text-danger': item.diff !== 0 }">{{
              formatMoney(Math.abs(item.diff))
            }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>
        <a-alert
          v-if="reconciliationAlerts.length"
          class="section-alert"
          type="warning"
          show-icon
          :message="reconciliationAlerts.join('；')"
        />
      </section>

      <a-space wrap class="overview-actions">
        <a-button
          v-if="auth.can('finance:receivable:view')"
          @click="router.push('/finance/receivables')"
          >查看应收</a-button
        >
        <a-button
          v-if="auth.can('finance:payable:view')"
          @click="router.push('/finance/payables')"
          >查看应付</a-button
        >
        <a-button
          v-if="auth.can('finance:payable:view')"
          type="primary"
          @click="router.push('/finance/payment-applications')"
          >处理付款申请</a-button
        >
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Empty } from "ant-design-vue";
import { useRouter } from "vue-router";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import type { Receivable } from "@/api/crm";
import {
  getFinanceOverview,
  listFinancePayables,
  listFinanceReceivables,
  listPaymentApplications,
  type FinanceOverview,
  type FinancePayable,
  type PaymentApplication,
} from "@/api/finance";
import { listVouchers, type AccountingVoucher } from "@/api/ledger";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const errorMessage = ref("");
const overview = reactive<FinanceOverview>(emptyOverview());
const receivables = ref<Receivable[]>([]);
const payables = ref<FinancePayable[]>([]);
const applications = ref<PaymentApplication[]>([]);
const vouchers = ref<AccountingVoucher[]>([]);
const selectedYear = ref(new Date().getFullYear());
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
const receivableRate = computed(() =>
  ratio(overview.receivedAmount, overview.receivableAmount),
);
const payableRate = computed(() =>
  ratio(overview.paidAmount, overview.payableAmount),
);
const availableYears = computed(() => {
  const years = new Set(
    vouchers.value.map((item) => Number(item.voucherDate.slice(0, 4))),
  );
  years.add(new Date().getFullYear());
  return [...years].filter(Boolean).sort((a, b) => b - a);
});
const monthlyFinance = computed(() => {
  const months = Array.from({ length: 12 }, (_, index) => ({
    month: index + 1,
    income: 0,
    expense: 0,
  }));
  vouchers.value
    .filter(
      (item) =>
        item.status === "POSTED" &&
        Number(item.voucherDate.slice(0, 4)) === selectedYear.value,
    )
    .forEach((item) => {
      const month = Number(item.voucherDate.slice(5, 7)) - 1;
      if (item.bizType === "RECEIPT")
        months[month].income += Number(item.totalDebit || 0);
      if (item.bizType === "PAYMENT")
        months[month].expense += Number(item.totalDebit || 0);
    });
  const maximum = Math.max(
    1,
    ...months.flatMap((item) => [item.income, item.expense]),
  );
  return months.map((item) => ({
    ...item,
    net: item.income - item.expense,
    incomeRate: (item.income / maximum) * 100,
    expenseRate: (item.expense / maximum) * 100,
  }));
});
const annualTotals = computed(() => {
  const income = monthlyFinance.value.reduce(
    (sum, item) => sum + item.income,
    0,
  );
  const expense = monthlyFinance.value.reduce(
    (sum, item) => sum + item.expense,
    0,
  );
  return { income, expense, net: income - expense };
});
const forecastBuckets = computed(() => [
  buildForecastBucket("d7", "未来7天", 7),
  buildForecastBucket("d30", "未来30天", 30),
  buildForecastBucket("d60", "未来60天", 60),
]);
const forecastTotal = computed(
  () => forecastBuckets.value[2] ?? { net: 0, receivable: 0, payable: 0 },
);
const agingBuckets = computed(() => [
  buildAgingBucket("current", "未到期", Number.NEGATIVE_INFINITY, -1),
  buildAgingBucket("d1", "逾期1-30天", 1, 30),
  buildAgingBucket("d31", "逾期31-60天", 31, 60),
  buildAgingBucket("d61", "逾期60天以上", 61, Number.POSITIVE_INFINITY),
]);
const riskItems = computed(() => {
  const overdueReceivable = receivables.value
    .filter((item) => isOpenReceivable(item) && daysOverdue(item.dueDate) > 0)
    .reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0);
  const overduePayable = payables.value
    .filter((item) => item.overdue)
    .reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0);
  const pendingAmount = applications.value
    .filter((item) => item.status === "PENDING_APPROVAL")
    .reduce((sum, item) => sum + Number(item.requestedAmount || 0), 0);
  const largeApplication = applications.value
    .filter(
      (item) =>
        item.status === "PENDING_APPROVAL" || item.status === "APPROVED",
    )
    .sort(
      (a, b) => Number(b.requestedAmount || 0) - Number(a.requestedAmount || 0),
    )[0];
  return [
    overdueReceivable > 0
      ? {
          key: "ar",
          level: "高",
          color: "red",
          title: "逾期应收待催收",
          value: formatMoney(overdueReceivable),
          route: "/finance/receivables",
        }
      : null,
    overduePayable > 0
      ? {
          key: "ap",
          level: "中",
          color: "orange",
          title: "逾期应付待安排",
          value: formatMoney(overduePayable),
          route: "/finance/payables",
        }
      : null,
    pendingAmount > 0
      ? {
          key: "pending",
          level: "中",
          color: "blue",
          title: "付款申请待审批",
          value: formatMoney(pendingAmount),
          route: "/finance/payment-applications",
        }
      : null,
    largeApplication
      ? {
          key: "large",
          level: "关注",
          color: "purple",
          title: `大额付款 ${largeApplication.supplierName || largeApplication.payableCode}`,
          value: formatMoney(largeApplication.requestedAmount),
          route: "/finance/payment-applications",
        }
      : null,
  ].filter(Boolean) as Array<{
    key: string;
    level: string;
    color: string;
    title: string;
    value: string;
    route: string;
  }>;
});
const ledgerReceiptAmount = computed(() => voucherAmountByType("RECEIPT"));
const ledgerPaymentAmount = computed(() => voucherAmountByType("PAYMENT"));
const reconciliationItems = computed(() => [
  {
    key: "receipt",
    label: "回款入账差额",
    diff: roundMoney(
      Number(overview.receivedAmount || 0) - ledgerReceiptAmount.value,
    ),
    hint: `业务 ${formatMoney(overview.receivedAmount)} / 总账 ${formatMoney(ledgerReceiptAmount.value)}`,
  },
  {
    key: "payment",
    label: "付款入账差额",
    diff: roundMoney(
      Number(overview.paidAmount || 0) - ledgerPaymentAmount.value,
    ),
    hint: `业务 ${formatMoney(overview.paidAmount)} / 总账 ${formatMoney(ledgerPaymentAmount.value)}`,
  },
  {
    key: "cash",
    label: "净流量差额",
    diff: roundMoney(
      Number(overview.netCashInflow || 0) -
        (ledgerReceiptAmount.value - ledgerPaymentAmount.value),
    ),
    hint: "业务净流入 vs 总账现金流",
  },
]);
const reconciliationAlerts = computed(() =>
  reconciliationItems.value
    .filter((item) => item.diff !== 0)
    .map((item) => `${item.label} ${formatMoney(Math.abs(item.diff))}`),
);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [overviewData, receivableData, payableData, applicationData] =
      await Promise.all([
        getFinanceOverview(),
        auth.can("finance:receivable:view")
          ? listFinanceReceivables()
          : Promise.resolve([]),
        auth.can("finance:payable:view")
          ? listFinancePayables()
          : Promise.resolve([]),
        auth.can("finance:payable:view")
          ? listPaymentApplications()
          : Promise.resolve([]),
      ]);
    vouchers.value = auth.can("finance:ledger:view")
      ? await listVouchers()
      : [];
    Object.assign(overview, overviewData);
    receivables.value = receivableData;
    payables.value = payableData;
    applications.value = applicationData;
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : "资金数据加载失败";
  } finally {
    loading.value = false;
  }
}
function emptyOverview(): FinanceOverview {
  return {
    receivableAmount: 0,
    receivedAmount: 0,
    receivableOutstanding: 0,
    receivableOverdue: 0,
    payableAmount: 0,
    paidAmount: 0,
    payableOutstanding: 0,
    payableOverdue: 0,
    netCashInflow: 0,
    pendingPaymentApplications: 0,
  };
}
function ratio(value: number, total: number) {
  return total > 0 ? Math.min(100, Math.round((value / total) * 100)) : 0;
}
function dangerStyle(value: number) {
  return value > 0 ? { color: "#cf1322" } : {};
}
function voucherAmountByType(type: string) {
  return vouchers.value
    .filter((item) => item.status === "POSTED" && item.bizType === type)
    .reduce((sum, item) => sum + Number(item.totalDebit || 0), 0);
}
function roundMoney(value: number) {
  return Math.round(value * 100) / 100;
}
function buildForecastBucket(key: string, label: string, days: number) {
  const receivable = receivables.value
    .filter((item) => isOpenReceivable(item) && isDueWithin(item.dueDate, days))
    .reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0);
  const payable = payables.value
    .filter(
      (item) =>
        item.status !== "PAID" &&
        item.status !== "CANCELLED" &&
        isDueWithin(item.dueDate, days),
    )
    .reduce(
      (sum, item) =>
        sum + Number(item.availableAmount || item.outstandingAmount || 0),
      0,
    );
  return { key, label, receivable, payable, net: receivable - payable };
}
function buildAgingBucket(
  key: string,
  label: string,
  minDays: number,
  maxDays: number,
) {
  const receivable = receivables.value
    .filter(
      (item) =>
        isOpenReceivable(item) && inAgingBucket(item.dueDate, minDays, maxDays),
    )
    .reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0);
  const payable = payables.value
    .filter(
      (item) =>
        item.status !== "PAID" &&
        item.status !== "CANCELLED" &&
        inAgingBucket(item.dueDate, minDays, maxDays),
    )
    .reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0);
  return { key, label, receivable, payable };
}
function openForecast(key: string) {
  if (key === "d7" || key === "d30") router.push("/finance/receivables");
  else router.push("/finance/payables");
}
function isOpenReceivable(item: Receivable) {
  return item.status !== "SETTLED" && Number(item.outstandingAmount || 0) > 0;
}
function isDueWithin(value: string, days: number) {
  const diff = daysFromToday(value);
  return diff <= days;
}
function inAgingBucket(value: string, minDays: number, maxDays: number) {
  const overdue = daysOverdue(value);
  return overdue >= minDays && overdue <= maxDays;
}
function daysOverdue(value: string) {
  return Math.max(0, -daysFromToday(value));
}
function daysFromToday(value: string) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(value);
  target.setHours(0, 0, 0, 0);
  return Math.ceil((target.getTime() - today.getTime()) / 86400000);
}
function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value || 0);
}
function moneyFormatter(value: number | string) {
  return formatMoney(Number(value));
}
</script>

<style scoped>
.finance-control-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 16px;
  margin-top: 18px;
}

.finance-panel {
  padding: 16px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.monthly-panel {
  margin-top: 18px;
}

.monthly-heading p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 12px;
}

.year-select {
  width: 120px;
}

.annual-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.annual-summary > div {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 12px 14px;
  border-radius: 6px;
  background: #f8fafc;
}

.annual-summary span,
.monthly-table-head {
  color: #667085;
  font-size: 12px;
}

.annual-summary strong {
  font-size: 20px;
}

.monthly-table {
  overflow-x: auto;
}

.monthly-row {
  display: grid;
  grid-template-columns: 52px minmax(180px, 1fr) repeat(
      3,
      minmax(112px, 0.55fr)
    );
  align-items: center;
  gap: 16px;
  min-width: 720px;
  padding: 9px 8px;
  border-bottom: 1px solid #eef2f7;
  text-align: right;
}

.monthly-row > :first-child,
.monthly-row > :nth-child(2) {
  text-align: left;
}

.monthly-table-head {
  padding-top: 0;
  font-weight: 500;
}

.month-bars {
  display: flex;
  height: 16px;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
}

.month-bars i {
  display: block;
  min-width: 2px;
  height: 5px;
  border-radius: 3px;
}

.income-bar {
  background: #52c41a;
}
.expense-bar {
  background: #ff7875;
}
.income-text {
  color: #237804;
}
.expense-text {
  color: #cf1322;
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-heading h3 {
  margin: 0;
  color: #101828;
  font-size: 15px;
  font-weight: 600;
}

.forecast-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.forecast-card,
.risk-row {
  border: 1px solid #eef2f7;
  border-radius: 6px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
}

.forecast-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
}

.forecast-card span,
.forecast-card small,
.aging-row small {
  color: #667085;
  font-size: 12px;
}

.forecast-card strong {
  color: #101828;
  font-size: 20px;
  overflow-wrap: anywhere;
}

.aging-list,
.risk-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.aging-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #eef2f7;
}

.aging-row:last-child {
  border-bottom: 0;
}

.aging-row > span {
  color: #344054;
}

.aging-row div {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.risk-panel {
  margin-top: 16px;
}

.reconciliation-panel {
  margin-top: 16px;
}

.reconciliation-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.reconciliation-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 6px;
  background: #f8fafc;
}

.reconciliation-card span,
.reconciliation-card small {
  color: #667085;
  font-size: 12px;
}

.reconciliation-card strong {
  color: #101828;
  font-size: 18px;
  overflow-wrap: anywhere;
}

.risk-row {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
}

.risk-row span {
  color: #344054;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.risk-row strong {
  color: #101828;
  white-space: nowrap;
}

.overview-actions {
  margin-top: 18px;
}

@media (max-width: 1100px) {
  .finance-control-grid,
  .forecast-grid,
  .reconciliation-grid {
    grid-template-columns: 1fr;
  }

  .annual-summary {
    grid-template-columns: 1fr;
  }
}
</style>
