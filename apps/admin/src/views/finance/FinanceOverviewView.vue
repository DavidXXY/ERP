<template>
  <div class="page-stack finance-console">
    <header class="console-header">
      <div>
        <span class="eyebrow">FINANCE CONTROL DESK</span>
        <h2>财务控制台</h2>
        <p>资金、账务、税务与关账异常集中核验</p>
      </div>
      <a-space wrap>
        <a-input v-model:value="asOf" type="date" class="date-input" />
        <a-select v-model:value="selectedYear" class="year-select">
          <a-select-option
            v-for="year in availableYears"
            :key="year"
            :value="year"
          >
            {{ year }} 年
          </a-select-option>
        </a-select>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>刷新
        </a-button>
      </a-space>
    </header>

    <a-alert
      v-if="errorMessage"
      type="warning"
      show-icon
      :message="errorMessage"
    />

    <section class="exception-strip" aria-label="财务异常状态">
      <button class="exception-cell" type="button" @click="openGovernance">
        <span>当前期间</span>
        <strong :class="periodTone">{{ periodLabel }}</strong>
        <small>{{
          currentPeriod
            ? `${currentPeriod.fiscalYear}-${pad(currentPeriod.periodNo)}`
            : "尚未建立期间"
        }}</small>
      </button>
      <button class="exception-cell" type="button" @click="openGovernance">
        <span>银行未完成对账</span>
        <strong
          :class="{ danger: analytics.reconciliation.unmatchedBankLines > 0 }"
        >
          {{ analytics.reconciliation.unmatchedBankLines }} 笔
        </strong>
        <small>{{
          formatMoney(analytics.reconciliation.unmatchedBankAmount)
        }}</small>
      </button>
      <button
        class="exception-cell"
        type="button"
        @click="router.push('/finance/ledger')"
      >
        <span>业务与总账差异</span>
        <strong :class="{ danger: reconciliationDifference !== 0 }">
          {{ formatMoney(reconciliationDifference) }}
        </strong>
        <small>{{ reconciliationExceptionCount }} 项待核验</small>
      </button>
      <button class="exception-cell" type="button" @click="openGovernance">
        <span>资金计划偏差</span>
        <strong :class="{ danger: analytics.cashPlan.variance > 0 }">
          {{ signedMoney(analytics.cashPlan.variance) }}
        </strong>
        <small>{{ analytics.cashPlan.activePlans }} 个执行中计划</small>
      </button>
    </section>

    <section class="metric-band">
      <div>
        <span>未来 60 天资金净额</span>
        <strong :class="{ danger: sixtyDayForecast.net < 0 }">{{
          signedMoney(sixtyDayForecast.net)
        }}</strong>
        <small
          >应收 {{ formatMoney(sixtyDayForecast.receivable) }} / 应付
          {{ formatMoney(sixtyDayForecast.payable) }}</small
        >
      </div>
      <div>
        <span>逾期应收</span>
        <strong :class="{ danger: overdueReceivable > 0 }">{{
          formatMoney(overdueReceivable)
        }}</strong>
        <small>{{ overdueReceivableCount }} 笔待催收</small>
      </div>
      <div>
        <span>本期应纳增值税</span>
        <strong :class="{ danger: analytics.tax.netTaxPayable > 0 }">{{
          signedMoney(analytics.tax.netTaxPayable)
        }}</strong>
        <small
          >销项 {{ formatMoney(analytics.tax.outputTax) }} / 进项
          {{ formatMoney(analytics.tax.inputTax) }}</small
        >
      </div>
      <div>
        <span>年度现金净流量</span>
        <strong :class="{ danger: annualTotals.net < 0 }">{{
          signedMoney(annualTotals.net)
        }}</strong>
        <small
          >流入 {{ formatMoney(annualTotals.receipt) }} / 流出
          {{ formatMoney(annualTotals.payment) }}</small
        >
      </div>
    </section>

    <div class="analysis-grid">
      <section class="work-panel cash-panel">
        <div class="panel-heading">
          <div>
            <h3>月度现金流</h3>
            <p>{{ selectedYear }} 年实际收付款走势</p>
          </div>
          <span class="panel-total"
            >净额 {{ signedMoney(annualTotals.net) }}</span
          >
        </div>
        <div
          class="cash-chart"
          role="img"
          :aria-label="`${selectedYear} 年月度现金流`"
        >
          <div
            v-for="item in monthlyChart"
            :key="item.month"
            class="cash-column"
          >
            <div class="bar-track">
              <i
                class="receipt-bar"
                :style="{ height: `${item.receiptRate}%` }"
              />
              <i
                class="payment-bar"
                :style="{ height: `${item.paymentRate}%` }"
              />
            </div>
            <span>{{ item.month }}</span>
          </div>
        </div>
        <div class="chart-legend">
          <span><i class="receipt-dot" />回款</span
          ><span><i class="payment-dot" />付款</span>
        </div>
      </section>

      <section class="work-panel forecast-panel">
        <div class="panel-heading">
          <div>
            <h3>现金流预测</h3>
            <p>按到期日滚动测算</p>
          </div>
        </div>
        <button
          v-for="item in analytics.forecast"
          :key="item.key"
          type="button"
          class="forecast-row"
          @click="openForecast(item.horizonDays)"
        >
          <span
            ><strong>{{ item.label }}</strong
            ><small>应收 {{ formatMoney(item.receivable) }}</small></span
          >
          <span class="forecast-payable"
            ><small>应付 {{ formatMoney(item.payable) }}</small
            ><strong :class="{ danger: item.net < 0 }">{{
              signedMoney(item.net)
            }}</strong></span
          >
        </button>
      </section>
    </div>

    <div class="analysis-grid lower-grid">
      <section class="work-panel">
        <div class="panel-heading">
          <div>
            <h3>应收应付账龄</h3>
            <p>按未结金额和到期日分层</p>
          </div>
        </div>
        <a-table
          :columns="agingColumns"
          :data-source="analytics.aging"
          :pagination="false"
          size="small"
          :row-key="(item: FinanceAgingBucket) => item.key"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'receivable'">
              <strong>{{ formatMoney(record.receivable) }}</strong
              ><small class="cell-note">{{ record.receivableCount }} 笔</small>
            </template>
            <template v-else-if="column.key === 'payable'">
              <strong>{{ formatMoney(record.payable) }}</strong
              ><small class="cell-note">{{ record.payableCount }} 笔</small>
            </template>
          </template>
        </a-table>
      </section>

      <section class="work-panel">
        <div class="panel-heading">
          <div>
            <h3>风险事项</h3>
            <p>按严重度和暴露金额排序</p>
          </div>
          <a-button size="small" @click="router.push('/risk-center')"
            >风险中心</a-button
          >
        </div>
        <div v-if="analytics.risks.length" class="risk-list">
          <button
            v-for="item in analytics.risks"
            :key="item.key"
            type="button"
            class="risk-row"
            @click="openRisk(item.category)"
          >
            <a-tag :color="severityColor(item.severity)">{{
              severityLabel(item.severity)
            }}</a-tag>
            <span
              ><strong>{{ item.title }}</strong
              ><small>{{ item.description }}</small></span
            >
            <span class="risk-value"
              ><strong>{{ formatMoney(item.amount) }}</strong
              ><small>{{ item.count }} 项</small></span
            >
          </button>
        </div>
        <a-empty v-else :image="simpleImage" description="暂无财务风险事项" />
      </section>
    </div>

    <section class="work-panel reconciliation-panel">
      <div class="panel-heading">
        <div>
          <h3>业务与总账勾稽</h3>
          <p>回款、付款及净现金逐项核对</p>
        </div>
        <a-space
          ><a-button size="small" @click="router.push('/finance/tax-ledger')"
            >税务台账</a-button
          ><a-button size="small" @click="router.push('/finance/ledger')"
            >总账报表</a-button
          ></a-space
        >
      </div>
      <div class="reconciliation-grid">
        <div v-for="item in analytics.reconciliation.ledger" :key="item.key">
          <span>{{ reconciliationLabel(item.key) }}</span>
          <strong :class="{ danger: item.difference !== 0 }">{{
            formatMoney(Math.abs(item.difference))
          }}</strong>
          <small
            >业务 {{ formatMoney(item.businessAmount) }} / 总账
            {{ formatMoney(item.ledgerAmount) }}</small
          >
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Empty } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { useRouter } from "vue-router";
import {
  getFinanceAnalytics,
  type FinanceAgingBucket,
  type FinanceAnalytics,
} from "@/api/finance";
import { listAccountingPeriods, type AccountingPeriod } from "@/api/governance";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const errorMessage = ref("");
const asOf = ref(today());
const selectedYear = ref(new Date().getFullYear());
const periods = ref<AccountingPeriod[]>([]);
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
const analytics = reactive<FinanceAnalytics>(emptyAnalytics());
const availableYears = computed(() =>
  Array.from({ length: 5 }, (_, index) => new Date().getFullYear() - index),
);
const currentPeriod = computed(() =>
  periods.value.find(
    (item) =>
      item.fiscalYear === Number(asOf.value.slice(0, 4)) &&
      item.periodNo === Number(asOf.value.slice(5, 7)),
  ),
);
const periodLabel = computed(() =>
  currentPeriod.value
    ? ({ OPEN: "开放", CLOSING: "关账中", CLOSED: "已关账" } as const)[
        currentPeriod.value.status
      ]
    : "未建立",
);
const periodTone = computed(() => ({
  danger: !currentPeriod.value || currentPeriod.value.status === "CLOSING",
  success: currentPeriod.value?.status === "CLOSED",
}));
const reconciliationDifference = computed(() =>
  analytics.reconciliation.ledger.reduce(
    (sum, item) => sum + Math.abs(Number(item.difference || 0)),
    0,
  ),
);
const reconciliationExceptionCount = computed(
  () =>
    analytics.reconciliation.ledger.filter(
      (item) => Number(item.difference) !== 0,
    ).length,
);
const sixtyDayForecast = computed(
  () =>
    analytics.forecast.find((item) => item.horizonDays === 60) ?? {
      receivable: 0,
      payable: 0,
      net: 0,
    },
);
const overdueReceivable = computed(() =>
  analytics.aging
    .filter((item) => item.key !== "CURRENT")
    .reduce((sum, item) => sum + Number(item.receivable), 0),
);
const overdueReceivableCount = computed(() =>
  analytics.aging
    .filter((item) => item.key !== "CURRENT")
    .reduce((sum, item) => sum + Number(item.receivableCount), 0),
);
const annualTotals = computed(() =>
  analytics.monthlyCashFlow.reduce(
    (totals, item) => ({
      receipt: totals.receipt + Number(item.receipt),
      payment: totals.payment + Number(item.payment),
      net: totals.net + Number(item.net),
    }),
    { receipt: 0, payment: 0, net: 0 },
  ),
);
const monthlyChart = computed(() => {
  const max = Math.max(
    1,
    ...analytics.monthlyCashFlow.flatMap((item) => [
      Number(item.receipt),
      Number(item.payment),
    ]),
  );
  return analytics.monthlyCashFlow.map((item) => ({
    ...item,
    receiptRate: Math.max(2, (Number(item.receipt) / max) * 100),
    paymentRate: Math.max(2, (Number(item.payment) / max) * 100),
  }));
});
const agingColumns = [
  { title: "账龄", dataIndex: "label", width: 130 },
  { title: "应收（含税）", key: "receivable" },
  { title: "应付（含税）", key: "payable" },
];

onMounted(loadData);
async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [data, periodData] = await Promise.all([
      getFinanceAnalytics({ asOf: asOf.value, year: selectedYear.value }),
      auth.can("governance:view")
        ? listAccountingPeriods()
        : Promise.resolve([]),
    ]);
    Object.assign(analytics, data);
    periods.value = periodData;
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : "财务分析加载失败";
  } finally {
    loading.value = false;
  }
}
function emptyAnalytics(): FinanceAnalytics {
  return {
    asOf: today(),
    fiscalYear: new Date().getFullYear(),
    monthlyCashFlow: [],
    forecast: [],
    aging: [],
    reconciliation: {
      ledger: [],
      bankLineCount: 0,
      matchedBankLines: 0,
      suggestedBankLines: 0,
      unmatchedBankLines: 0,
      unmatchedBankAmount: 0,
    },
    tax: {
      outputGross: 0,
      outputNet: 0,
      outputTax: 0,
      inputGross: 0,
      inputNet: 0,
      inputTax: 0,
      netTaxPayable: 0,
      pendingOutputInvoices: 0,
      inputInvoiceExceptions: 0,
      adjustedInvoices: 0,
    },
    cashPlan: {
      baseline: 0,
      committed: 0,
      actual: 0,
      forecast: 0,
      variance: 0,
      activePlans: 0,
    },
    risks: [],
  };
}
function openGovernance() {
  if (auth.can("governance:view")) router.push("/governance");
}
function openForecast(days: number) {
  router.push(days <= 30 ? "/finance/receivables" : "/finance/payables");
}
function openRisk(category: string) {
  const route =
    category === "RECEIVABLE"
      ? "/finance/receivables"
      : category === "PAYABLE"
        ? "/finance/payables"
        : category === "TAX"
          ? "/finance/tax-ledger"
          : "/governance";
  router.push(route);
}
function reconciliationLabel(key: string) {
  return (
    (
      {
        RECEIPT: "回款入账差额",
        PAYMENT: "付款入账差额",
        NET_CASH: "净现金差额",
      } as Record<string, string>
    )[key] ?? key
  );
}
function severityLabel(value: string) {
  return (
    ({ HIGH: "高", MEDIUM: "中", LOW: "低" } as Record<string, string>)[
      value
    ] ?? value
  );
}
function severityColor(value: string) {
  return value === "HIGH" ? "red" : value === "MEDIUM" ? "orange" : "green";
}
function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(Number(value || 0));
}
function signedMoney(value: number) {
  return `${Number(value) > 0 ? "+" : ""}${formatMoney(value)}`;
}
function today() {
  return new Date().toISOString().slice(0, 10);
}
function pad(value: number) {
  return String(value).padStart(2, "0");
}
</script>

<style scoped>
.finance-console {
  color: #1f2937;
}
.console-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 4px 2px;
}
.console-header h2 {
  margin: 3px 0 2px;
  font-size: 24px;
  line-height: 1.25;
}
.console-header p,
.panel-heading p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}
.eyebrow {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}
.date-input {
  width: 150px;
}
.year-select {
  width: 110px;
}
.exception-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid #d9dee7;
  border-left: 4px solid #334155;
  background: #fff;
}
.exception-cell {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 14px 16px;
  border: 0;
  border-right: 1px solid #e5e7eb;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.exception-cell:last-child {
  border-right: 0;
}
.exception-cell:hover {
  background: #f8fafc;
}
.exception-cell span,
.metric-band span {
  color: #64748b;
  font-size: 12px;
}
.exception-cell strong {
  font-size: 18px;
}
.exception-cell small,
.metric-band small,
.cell-note {
  color: #94a3b8;
  font-size: 12px;
}
.metric-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: #172033;
  color: #fff;
}
.metric-band > div {
  display: grid;
  gap: 7px;
  min-width: 0;
  padding: 18px;
  border-right: 1px solid #334155;
}
.metric-band > div:last-child {
  border-right: 0;
}
.metric-band strong {
  overflow-wrap: anywhere;
  font-size: 23px;
}
.metric-band small {
  line-height: 1.5;
}
.danger {
  color: #cf1322 !important;
}
.metric-band .danger {
  color: #ff7875 !important;
}
.success {
  color: #237804;
}
.analysis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.8fr);
  gap: 16px;
}
.lower-grid {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}
.work-panel {
  min-width: 0;
  padding: 18px;
  border: 1px solid #d9dee7;
  background: #fff;
}
.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}
.panel-heading h3 {
  margin: 0 0 3px;
  font-size: 16px;
}
.panel-total {
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}
.cash-chart {
  display: grid;
  grid-template-columns: repeat(12, minmax(20px, 1fr));
  align-items: end;
  height: 190px;
  gap: 8px;
  padding-top: 8px;
  border-bottom: 1px solid #cbd5e1;
}
.cash-column {
  display: grid;
  align-items: end;
  height: 100%;
  gap: 6px;
  text-align: center;
  color: #64748b;
  font-size: 11px;
}
.bar-track {
  display: flex;
  align-items: end;
  justify-content: center;
  gap: 3px;
  height: 160px;
}
.bar-track i {
  width: min(10px, 42%);
  min-height: 3px;
}
.receipt-bar {
  background: #1677ff;
}
.payment-bar {
  background: #f97316;
}
.chart-legend {
  display: flex;
  justify-content: center;
  gap: 22px;
  padding-top: 10px;
  color: #64748b;
  font-size: 12px;
}
.chart-legend i {
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-right: 6px;
}
.receipt-dot {
  background: #1677ff;
}
.payment-dot {
  background: #f97316;
}
.forecast-panel {
  display: flex;
  flex-direction: column;
}
.forecast-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border: 0;
  border-top: 1px solid #edf0f4;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.forecast-row:hover {
  background: #f8fafc;
}
.forecast-row > span {
  display: grid;
  gap: 4px;
}
.forecast-row small {
  color: #64748b;
  font-size: 12px;
}
.forecast-payable {
  text-align: right;
}
.risk-list {
  display: grid;
}
.risk-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 11px 0;
  border: 0;
  border-top: 1px solid #edf0f4;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.risk-row > span {
  display: grid;
  gap: 3px;
  min-width: 0;
}
.risk-row small {
  color: #64748b;
  font-size: 12px;
}
.risk-value {
  text-align: right;
}
.reconciliation-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  background: #d9dee7;
}
.reconciliation-grid > div {
  display: grid;
  gap: 5px;
  padding: 14px;
  background: #f8fafc;
}
.reconciliation-grid span,
.reconciliation-grid small {
  color: #64748b;
  font-size: 12px;
}
.cell-note {
  display: block;
}
@media (max-width: 900px) {
  .console-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .exception-strip,
  .metric-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .analysis-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }
  .exception-cell:nth-child(2) {
    border-right: 0;
  }
  .metric-band > div:nth-child(2) {
    border-right: 0;
  }
}
@media (max-width: 560px) {
  .exception-strip,
  .metric-band,
  .reconciliation-grid {
    grid-template-columns: 1fr;
  }
  .exception-cell,
  .metric-band > div {
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }
  .cash-chart {
    gap: 3px;
  }
  .bar-track {
    gap: 1px;
  }
  .risk-row {
    grid-template-columns: 42px minmax(0, 1fr);
  }
  .risk-value {
    grid-column: 2;
    text-align: left;
  }
}
</style>
