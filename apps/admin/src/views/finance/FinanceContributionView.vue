<template>
  <div class="page-stack contribution-console">
    <header class="page-header">
      <div>
        <span class="eyebrow">CONTRIBUTION CONTROL</span>
        <h2>经营贡献分析</h2>
        <p>按销售归属核验项目毛利、回款、付款与资金占用</p>
      </div>
      <a-space wrap>
        <a-input v-model:value="asOf" type="date" class="date-input" />
        <a-select v-model:value="selectedYear" class="year-select">
          <a-select-option v-for="year in years" :key="year" :value="year">
            {{ year }} 年
          </a-select-option>
        </a-select>
        <a-button :loading="loading" @click="loadAnalytics">
          <template #icon><ReloadOutlined /></template>刷新
        </a-button>
      </a-space>
    </header>

    <section class="scope-workbench" aria-label="经营贡献查询范围">
      <div class="scope-controls">
        <a-segmented
          v-model:value="subjectType"
          :options="subjectOptions"
          @change="handleSubjectTypeChange"
        />
        <a-tree-select
          v-model:value="selectedOrganizationId"
          :tree-data="organizationOptions"
          allow-clear
          show-search
          tree-default-expand-all
          tree-node-filter-prop="title"
          placeholder="全部授权组织"
          class="organization-select"
          @change="handleOrganizationChange"
        />
        <a-select
          v-if="subjectType === 'USER'"
          v-model:value="selectedSalespersonId"
          show-search
          allow-clear
          option-filter-prop="label"
          :options="salespersonOptions"
          :loading="salespeopleLoading"
          placeholder="选择销售人员"
          class="salesperson-select"
          @change="handleSalespersonChange"
        />
        <label class="descendant-toggle">
          <a-switch
            v-model:checked="includeDescendants"
            size="small"
            :disabled="subjectType === 'USER' || !selectedOrganizationId"
            @change="handleDescendantChange"
          />
          <span>含下级</span>
        </label>
      </div>
      <div class="scope-result">
        <strong>{{ contribution.scope.subjectName }}</strong>
        <span>{{ contribution.scope.subjectPath }}</span>
        <a-tag color="blue">{{ contribution.scope.attributionBasis }}</a-tag>
      </div>
    </section>

    <a-alert
      v-if="errorMessage"
      type="warning"
      show-icon
      :message="errorMessage"
    />

    <section class="profit-cash-band">
      <div class="profit-lead">
        <span>累计项目毛利</span>
        <strong :class="{ danger: summary.grossProfit < 0 }">
          {{ money(summary.grossProfit) }}
        </strong>
        <small>
          合同 {{ money(summary.contractAmount) }} · 成本
          {{ money(summary.actualCost) }}
        </small>
      </div>
      <div>
        <span>毛利率</span>
        <strong :class="{ danger: summary.grossMarginRate < 10 }">
          {{ percent(summary.grossMarginRate) }}
        </strong>
        <small>{{ summary.projectCount }} 个归属项目</small>
      </div>
      <div class="cash-lead">
        <span>累计净现金流</span>
        <strong :class="{ danger: summary.netCashFlow < 0 }">
          {{ signedMoney(summary.netCashFlow) }}
        </strong>
        <small>
          回款 {{ money(summary.receivedAmount) }} · 付款
          {{ money(summary.paidAmount) }}
        </small>
      </div>
      <div>
        <span>回款率</span>
        <strong>{{ percent(summary.collectionRate) }}</strong>
        <small>按归属应收计划计算</small>
      </div>
    </section>

    <section class="exposure-band">
      <div>
        <span>未收应收</span>
        <strong>{{ money(summary.receivableOutstanding) }}</strong>
      </div>
      <div>
        <span>项目未付应付</span>
        <strong>{{ money(summary.payableOutstanding) }}</strong>
      </div>
      <div>
        <span>资金占用净额</span>
        <strong :class="{ danger: capitalExposure > 0 }">
          {{ signedMoney(capitalExposure) }}
        </strong>
      </div>
      <div>
        <span>本年现金净额</span>
        <strong :class="{ danger: annualCash.net < 0 }">
          {{ signedMoney(annualCash.net) }}
        </strong>
      </div>
    </section>

    <div class="analysis-grid">
      <section class="work-panel cash-trend-panel">
        <div class="panel-heading">
          <div>
            <h3>{{ selectedYear }} 年现金流趋势</h3>
            <p>实际回款与项目采购实际付款</p>
          </div>
          <strong :class="{ danger: annualCash.net < 0 }">
            净额 {{ signedMoney(annualCash.net) }}
          </strong>
        </div>
        <div
          class="cash-chart"
          role="img"
          :aria-label="`${selectedYear} 年现金流趋势`"
        >
          <div
            v-for="item in monthlyChart"
            :key="item.month"
            class="cash-month"
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
          <span><i class="receipt-dot" />回款</span>
          <span><i class="payment-dot" />付款</span>
        </div>
      </section>

      <section class="work-panel efficiency-panel">
        <div class="panel-heading">
          <div>
            <h3>利润与现金转换</h3>
            <p>利润不等于现金，需同时核验回款和资金占用</p>
          </div>
        </div>
        <div class="efficiency-row">
          <span>合同转毛利</span>
          <a-progress
            :percent="progressPercent(summary.grossMarginRate)"
            :stroke-color="summary.grossMarginRate < 10 ? '#cf1322' : '#1677ff'"
            :show-info="false"
          />
          <strong>{{ percent(summary.grossMarginRate) }}</strong>
        </div>
        <div class="efficiency-row">
          <span>应收转现金</span>
          <a-progress
            :percent="progressPercent(summary.collectionRate)"
            stroke-color="#0f766e"
            :show-info="false"
          />
          <strong>{{ percent(summary.collectionRate) }}</strong>
        </div>
        <div class="cash-bridge">
          <div>
            <span>累计回款</span
            ><strong>{{ money(summary.receivedAmount) }}</strong>
          </div>
          <i>−</i>
          <div>
            <span>累计付款</span
            ><strong>{{ money(summary.paidAmount) }}</strong>
          </div>
          <i>=</i>
          <div>
            <span>净现金</span
            ><strong>{{ signedMoney(summary.netCashFlow) }}</strong>
          </div>
        </div>
      </section>
    </div>

    <section class="work-panel project-panel">
      <div class="panel-heading">
        <div>
          <h3>逐项目经营贡献</h3>
          <p>从利润穿透到回款、付款和未结资金</p>
        </div>
        <a-tag>{{ contribution.projects.length }} 个项目</a-tag>
      </div>
      <a-table
        :columns="projectColumns"
        :data-source="contribution.projects"
        :loading="loading"
        :pagination="{ pageSize: 12, showSizeChanger: false }"
        :scroll="{ x: 1440 }"
        row-key="projectId"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'project'">
            <button
              class="project-link"
              type="button"
              @click="openProject(record.projectId)"
            >
              <strong>{{ record.projectName }}</strong>
              <small
                >{{ record.projectCode }} ·
                {{ record.customerName || "未关联客户" }}</small
              >
            </button>
          </template>
          <template v-else-if="column.key === 'owner'">
            {{ record.salesOwnerName || "未归属" }}
          </template>
          <template v-else-if="column.key === 'margin'">
            <strong :class="{ danger: record.grossProfit < 0 }">
              {{ money(record.grossProfit) }}
            </strong>
            <small class="cell-note">{{
              percent(record.grossMarginRate)
            }}</small>
          </template>
          <template v-else-if="column.key === 'cash'">
            <strong :class="{ danger: record.netCashFlow < 0 }">
              {{ signedMoney(record.netCashFlow) }}
            </strong>
            <small class="cell-note">
              收 {{ money(record.receivedAmount) }} / 付
              {{ money(record.paidAmount) }}
            </small>
          </template>
          <template v-else-if="column.key === 'outstanding'">
            <span>应收 {{ money(record.receivableOutstanding) }}</span>
            <small class="cell-note"
              >应付 {{ money(record.payableOutstanding) }}</small
            >
          </template>
          <template v-else-if="column.key === 'stage'">
            <a-tag :color="projectStageColor(record.stage)">
              {{ projectStageLabel(record.stage) }}
            </a-tag>
          </template>
          <template v-else-if="moneyColumnKeys.includes(column.key)">
            {{ money(record[column.dataIndex]) }}
          </template>
        </template>
        <template #emptyText>
          <a-empty :image="simpleImage" description="当前范围暂无归属项目" />
        </template>
      </a-table>
    </section>

    <a-alert
      v-if="hasDataQualityIssue"
      type="info"
      show-icon
      :message="dataQualityMessage"
      :description="contribution.dataQuality.note"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Empty } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { useRouter } from "vue-router";
import {
  getFinanceContribution,
  listContributionSalespeople,
  listFinanceOrganizations,
  type FinanceContribution,
  type FinanceOrganizationNode,
} from "@/api/finance";
import { projectStageColor, projectStageLabel } from "@/utils/project-stage";

type SubjectType = "ORGANIZATION" | "USER";
type OrganizationOption = {
  value: string;
  title: string;
  label: string;
  children: OrganizationOption[];
};

const router = useRouter();
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
const loading = ref(false);
const salespeopleLoading = ref(false);
const errorMessage = ref("");
const subjectType = ref<SubjectType>("ORGANIZATION");
const selectedOrganizationId = ref<string>();
const selectedSalespersonId = ref<string>();
const includeDescendants = ref(true);
const asOf = ref(today());
const selectedYear = ref(new Date().getFullYear());
const organizations = ref<FinanceOrganizationNode[]>([]);
const salespeople = ref<
  Awaited<ReturnType<typeof listContributionSalespeople>>
>([]);
const contribution = reactive<FinanceContribution>(emptyContribution());
const subjectOptions = [
  { label: "部门", value: "ORGANIZATION" },
  { label: "销售人员", value: "USER" },
];
const years = computed(() =>
  Array.from({ length: 5 }, (_, index) => new Date().getFullYear() - index),
);
const organizationOptions = computed(() =>
  organizations.value.map(toOrganizationOption),
);
const salespersonOptions = computed(() =>
  salespeople.value.map((item) => ({
    value: item.id,
    label: `${item.displayName} · ${item.organizationName}${item.enabled ? "" : "（停用）"}`,
  })),
);
const summary = computed(() => contribution.summary);
const annualCash = computed(() =>
  contribution.monthlyCashFlow.reduce(
    (total, item) => ({
      receipt: total.receipt + Number(item.receipt),
      payment: total.payment + Number(item.payment),
      net: total.net + Number(item.netCash),
    }),
    { receipt: 0, payment: 0, net: 0 },
  ),
);
const monthlyChart = computed(() => {
  const max = Math.max(
    1,
    ...contribution.monthlyCashFlow.flatMap((item) => [
      Number(item.receipt),
      Number(item.payment),
    ]),
  );
  return contribution.monthlyCashFlow.map((item) => ({
    ...item,
    receiptRate: Math.max(2, (Number(item.receipt) / max) * 100),
    paymentRate: Math.max(2, (Number(item.payment) / max) * 100),
  }));
});
const capitalExposure = computed(
  () =>
    Number(summary.value.receivableOutstanding) -
    Number(summary.value.payableOutstanding),
);
const hasDataQualityIssue = computed(
  () =>
    contribution.dataQuality.unattributedProjectCount > 0 ||
    contribution.dataQuality.unattributedReceivableCount > 0 ||
    contribution.dataQuality.unlinkedReceivableCount > 0,
);
const dataQualityMessage = computed(
  () =>
    `归属待完善：${contribution.dataQuality.unattributedProjectCount} 个项目、` +
    `${contribution.dataQuality.unattributedReceivableCount} 笔应收未归属，` +
    `${contribution.dataQuality.unlinkedReceivableCount} 笔应收未关联项目`,
);
const moneyColumnKeys = ["contract", "cost"];
const projectColumns = [
  { title: "项目", key: "project", width: 270, fixed: "left" as const },
  { title: "销售归属", key: "owner", width: 120 },
  {
    title: "合同额（含税，元）",
    key: "contract",
    dataIndex: "contractAmount",
    width: 180,
  },
  {
    title: "实际成本（税价随来源单据，元）",
    key: "cost",
    dataIndex: "actualCost",
    width: 230,
  },
  { title: "毛利 / 毛利率", key: "margin", width: 180 },
  { title: "净现金 / 收付款", key: "cash", width: 210 },
  { title: "未结资金", key: "outstanding", width: 200 },
  { title: "阶段", key: "stage", width: 130 },
];

onMounted(async () => {
  await loadOrganizations();
  await loadAnalytics();
});

async function loadOrganizations() {
  try {
    organizations.value = await listFinanceOrganizations();
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : "组织架构加载失败";
  }
}

async function loadSalespeople() {
  salespeopleLoading.value = true;
  try {
    salespeople.value = await listContributionSalespeople({
      organizationId: selectedOrganizationId.value,
      includeDescendants: includeDescendants.value,
    });
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : "销售人员加载失败";
  } finally {
    salespeopleLoading.value = false;
  }
}

async function loadAnalytics() {
  if (subjectType.value === "USER" && !selectedSalespersonId.value) return;
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await getFinanceContribution({
      subjectType: subjectType.value,
      subjectId:
        subjectType.value === "USER"
          ? selectedSalespersonId.value
          : selectedOrganizationId.value,
      includeDescendants: includeDescendants.value,
      asOf: asOf.value,
      year: selectedYear.value,
    });
    Object.assign(contribution, data);
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : "经营贡献加载失败";
  } finally {
    loading.value = false;
  }
}

async function handleSubjectTypeChange() {
  selectedSalespersonId.value = undefined;
  includeDescendants.value = true;
  if (subjectType.value === "USER") {
    await loadSalespeople();
    Object.assign(contribution, emptyContribution("USER"));
    return;
  }
  await loadAnalytics();
}

async function handleOrganizationChange() {
  if (!selectedOrganizationId.value) includeDescendants.value = true;
  selectedSalespersonId.value = undefined;
  if (subjectType.value === "USER") {
    await loadSalespeople();
    Object.assign(contribution, emptyContribution("USER"));
    return;
  }
  await loadAnalytics();
}

async function handleDescendantChange() {
  if (subjectType.value === "USER") return;
  await loadAnalytics();
}

async function handleSalespersonChange() {
  if (selectedSalespersonId.value) await loadAnalytics();
  else Object.assign(contribution, emptyContribution("USER"));
}

function toOrganizationOption(
  node: FinanceOrganizationNode,
): OrganizationOption {
  return {
    value: node.id,
    title: node.name,
    label: node.name,
    children: node.children.map(toOrganizationOption),
  };
}

function emptyContribution(
  type: SubjectType = "ORGANIZATION",
): FinanceContribution {
  return {
    asOf: today(),
    fiscalYear: new Date().getFullYear(),
    scope: {
      subjectType: type,
      subjectName: type === "USER" ? "请选择销售人员" : "全部销售归属",
      subjectPath:
        type === "USER" ? "按部门筛选销售人员" : "按角色数据范围汇总",
      includeDescendants: true,
      organizationCount: 0,
      attributionBasis: "销售归属快照",
    },
    summary: {
      contractAmount: 0,
      actualCost: 0,
      grossProfit: 0,
      grossMarginRate: 0,
      receivedAmount: 0,
      paidAmount: 0,
      netCashFlow: 0,
      receivableOutstanding: 0,
      payableOutstanding: 0,
      collectionRate: 0,
      projectCount: 0,
    },
    monthlyCashFlow: [],
    projects: [],
    dataQuality: {
      unattributedProjectCount: 0,
      unattributedReceivableCount: 0,
      unlinkedReceivableCount: 0,
      note: "利润按项目实际成本，现金按实际收付款归集",
    },
  };
}

function openProject(id: string) {
  router.push(`/projects/${id}`);
}
function progressPercent(value: number) {
  return Math.max(0, Math.min(100, Number(value || 0)));
}
function money(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(Number(value || 0));
}
function signedMoney(value: number) {
  return `${Number(value) > 0 ? "+" : ""}${money(value)}`;
}
function percent(value: number) {
  return `${Number(value || 0).toFixed(2)}%`;
}
function today() {
  return new Date().toISOString().slice(0, 10);
}
</script>

<style scoped>
.contribution-console {
  color: #1f2937;
}
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}
.page-header h2 {
  margin: 3px 0 2px;
  font-size: 24px;
  line-height: 1.25;
}
.page-header p,
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
.scope-workbench {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 62px;
  padding: 11px 14px;
  border: 1px solid #d9dee7;
  border-left: 4px solid #1677ff;
  background: #fff;
}
.scope-controls,
.scope-result,
.descendant-toggle {
  display: flex;
  align-items: center;
}
.scope-controls {
  gap: 10px;
  flex-wrap: wrap;
}
.organization-select {
  width: 250px;
}
.salesperson-select {
  width: 240px;
}
.descendant-toggle {
  gap: 7px;
  color: #475569;
  font-size: 13px;
  white-space: nowrap;
}
.scope-result {
  justify-content: flex-end;
  gap: 9px;
  min-width: 0;
  color: #64748b;
  font-size: 12px;
}
.scope-result strong {
  color: #1f2937;
  font-size: 13px;
  white-space: nowrap;
}
.scope-result > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.profit-cash-band {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr 1.25fr 0.75fr;
  background: #172033;
  color: #fff;
}
.profit-cash-band > div {
  display: grid;
  gap: 7px;
  min-width: 0;
  padding: 18px;
  border-right: 1px solid #334155;
}
.profit-cash-band > div:last-child {
  border-right: 0;
}
.profit-cash-band span {
  color: #94a3b8;
  font-size: 12px;
}
.profit-cash-band strong {
  overflow-wrap: anywhere;
  font-size: 23px;
}
.profit-cash-band small {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}
.profit-lead {
  box-shadow: inset 0 3px 0 #1677ff;
}
.cash-lead {
  box-shadow: inset 0 3px 0 #14b8a6;
}
.exposure-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid #d9dee7;
  background: #fff;
}
.exposure-band > div {
  display: grid;
  gap: 5px;
  padding: 13px 16px;
  border-right: 1px solid #e5e7eb;
}
.exposure-band > div:last-child {
  border-right: 0;
}
.exposure-band span {
  color: #64748b;
  font-size: 12px;
}
.exposure-band strong {
  font-size: 17px;
  overflow-wrap: anywhere;
}
.analysis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(360px, 0.9fr);
  gap: 16px;
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
.panel-heading > strong {
  color: #334155;
  font-size: 13px;
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
.cash-month {
  display: grid;
  align-items: end;
  height: 100%;
  gap: 6px;
  color: #64748b;
  text-align: center;
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
.receipt-bar,
.receipt-dot {
  background: #1677ff;
}
.payment-bar,
.payment-dot {
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
.efficiency-panel {
  display: flex;
  flex-direction: column;
}
.efficiency-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 58px;
  align-items: center;
  gap: 10px;
  margin: 11px 0;
  color: #475569;
  font-size: 12px;
}
.efficiency-row strong {
  text-align: right;
}
.cash-bridge {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto 1fr;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid #edf0f4;
}
.cash-bridge div {
  display: grid;
  gap: 5px;
  min-width: 0;
}
.cash-bridge span {
  color: #64748b;
  font-size: 11px;
}
.cash-bridge strong {
  overflow-wrap: anywhere;
  font-size: 13px;
}
.cash-bridge i {
  color: #94a3b8;
  font-style: normal;
}
.project-link {
  display: grid;
  gap: 3px;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.project-link strong {
  color: #1677ff;
}
.project-link small,
.cell-note {
  display: block;
  color: #94a3b8;
  font-size: 12px;
}
.danger {
  color: #cf1322 !important;
}
.profit-cash-band .danger {
  color: #ff7875 !important;
}
@media (max-width: 980px) {
  .page-header,
  .scope-workbench,
  .scope-result {
    align-items: flex-start;
    flex-direction: column;
  }
  .scope-result {
    gap: 4px;
  }
  .profit-cash-band,
  .exposure-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .profit-cash-band > div:nth-child(2),
  .exposure-band > div:nth-child(2) {
    border-right: 0;
  }
  .analysis-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 600px) {
  .scope-controls {
    width: 100%;
  }
  .organization-select,
  .salesperson-select {
    flex: 1 1 220px;
    width: auto;
  }
  .profit-cash-band,
  .exposure-band {
    grid-template-columns: 1fr;
  }
  .profit-cash-band > div,
  .exposure-band > div {
    border-right: 0;
    border-bottom: 1px solid #334155;
  }
  .exposure-band > div {
    border-bottom-color: #e5e7eb;
  }
  .cash-chart {
    gap: 3px;
  }
  .bar-track {
    gap: 1px;
  }
  .cash-bridge {
    grid-template-columns: 1fr;
  }
  .cash-bridge i {
    display: none;
  }
}
</style>
