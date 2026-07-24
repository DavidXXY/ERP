<template>
  <div class="page-stack">
    <div class="dash-header">
      <h2>{{ isCompanyDashboard ? "经营驾驶舱" : "CRM仪表盘" }}</h2>
      <a-button :loading="loading" @click="loadData">刷新</a-button>
    </div>

    <a-row :gutter="[16, 16]" class="metric-row">
      <a-col
        v-for="item in dashboardMetrics"
        :key="item.label"
        :xs="12"
        :md="6"
      >
        <a-card class="metric-card" :loading="loading">
          <a-statistic
            :title="item.label"
            :value="item.value"
            :suffix="item.suffix"
            :value-style="{ color: item.color }"
          />
          <div class="metric-sub">{{ item.sub }}</div>
        </a-card>
      </a-col>
    </a-row>

    <a-card v-if="isCompanyDashboard" class="closure-panel" :loading="loading">
      <div class="closure-head">
        <div>
          <span>全业务闭环</span>
          <strong>从线索到现金、从采购到交付的断点雷达</strong>
        </div>
        <div class="closure-score">
          <span>闭环健康度</span>
          <strong
            :class="{ 'text-danger': detailComplete && closureHealth < 70 }"
            >{{ detailComplete ? closureHealth : "--" }}</strong
          >
        </div>
      </div>
      <div class="closure-grid">
        <button
          v-for="item in closureLoops"
          :key="item.key"
          class="closure-loop"
          type="button"
          @click="router.push(item.link)"
        >
          <span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.desc }}</small>
          </span>
          <em :class="item.tone">{{ item.value }}</em>
          <a-progress
            :percent="item.percent"
            :show-info="false"
            :stroke-color="item.stroke"
          />
        </button>
      </div>
      <div class="closure-alerts">
        <button
          v-for="item in closureAlerts"
          :key="item.key"
          class="closure-alert"
          type="button"
          @click="router.push(item.link)"
        >
          <a-tag :color="item.color">{{ item.type }}</a-tag>
          <span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.desc }}</small>
          </span>
          <em>{{ item.value }}</em>
        </button>
        <div v-if="closureAlerts.length === 0" class="closure-empty">
          {{
            detailComplete
              ? "当前没有跨模块断点，业务闭环运转平稳"
              : "部分业务明细受当前角色权限限制，请以可见指标为准"
          }}
        </div>
      </div>
    </a-card>

    <a-card
      v-if="!isCompanyDashboard"
      class="crm-closure-panel"
      :loading="loading"
    >
      <div class="closure-head">
        <div>
          <span>线索到合同闭环</span>
          <strong>转化率、卡点、责任人与销售预测</strong>
        </div>
        <a-button type="link" @click="router.push('/crm/opportunities')"
          >处理商机</a-button
        >
      </div>
      <div class="crm-pipeline-grid">
        <button
          v-for="item in crmPipelineSteps"
          :key="item.key"
          class="pipeline-step"
          type="button"
          @click="router.push(item.link)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </button>
      </div>
      <div class="crm-insight-grid">
        <div class="crm-insight-block">
          <div class="right-panel-head compact">
            <div>
              <span>销售预测</span>
              <strong>{{ formatMoney(salesForecast.weightedAmount) }}</strong>
            </div>
            <a-tag color="blue">{{ salesForecast.count }} 个机会</a-tag>
          </div>
          <div class="forecast-bars">
            <div
              v-for="item in forecastByStage"
              :key="item.stage"
              class="forecast-row"
            >
              <span>{{ item.stage }}</span>
              <div><i :style="{ width: item.percent + '%' }"></i></div>
              <em>{{ formatMoney(item.amount) }}</em>
            </div>
          </div>
        </div>
        <div class="crm-insight-block">
          <div class="right-panel-head compact">
            <div>
              <span>客户分层与风险</span>
              <strong>{{ customerRiskItems.length }} 个重点客户</strong>
            </div>
            <a-button
              type="link"
              size="small"
              @click="router.push('/crm/customers')"
              >客户池</a-button
            >
          </div>
          <button
            v-for="item in customerRiskItems.slice(0, 4)"
            :key="item.id"
            class="customer-risk-row"
            type="button"
            @click="router.push('/crm/customers')"
          >
            <a-tag :color="item.color">{{ item.level }}</a-tag>
            <span>
              <strong>{{ item.name }}</strong>
              <small>{{ item.reason }}</small>
            </span>
          </button>
          <a-empty
            v-if="customerRiskItems.length === 0"
            description="暂无重点客户风险"
          />
        </div>
      </div>
    </a-card>

    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :xs="24" :md="14">
        <a-card title="销售漏斗" :loading="loading">
          <div class="funnel-chart">
            <div v-for="item in stageData" :key="item.stage" class="funnel-row">
              <span class="funnel-label">{{ item.stage }}</span>
              <div class="funnel-bar-wrap">
                <div
                  class="funnel-bar"
                  :style="{ width: item.percent + '%', background: item.color }"
                >
                  <span v-if="item.percent > 15" class="funnel-bar-text"
                    >{{ item.count }} / {{ formatMoney(item.amount) }}</span
                  >
                </div>
              </div>
            </div>
          </div>
          <div class="win-rate">
            整体赢单率：
            <strong :style="{ color: winRate >= 50 ? '#52c41a' : '#ff4d4f' }"
              >{{ winRate }}%</strong
            >
            （赢单 {{ winCount }} / 已完结 {{ winCount + lostCount }}）
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="10">
        <a-card class="right-panel action-panel" :loading="loading">
          <div class="right-panel-head">
            <div>
              <span>{{ isCompanyDashboard ? "经营动作" : "CRM动作" }}</span>
              <strong>{{
                isCompanyDashboard ? "今日优先级" : "销售优先级"
              }}</strong>
            </div>
            <a-tag
              :color="
                !detailComplete ? 'default' : riskScore > 0 ? 'red' : 'green'
              "
              >{{
                !detailComplete ? "数据受限" : riskScore > 0 ? "需关注" : "平稳"
              }}</a-tag
            >
          </div>
          <div class="health-card">
            <div>
              <span>{{ isCompanyDashboard ? "经营健康度" : "CRM健康度" }}</span>
              <strong>{{ detailComplete ? healthScore : "--" }}</strong>
            </div>
            <a-progress
              type="circle"
              :percent="detailComplete ? healthScore : 0"
              :width="74"
              :stroke-color="healthScore >= 80 ? '#237804' : '#faad14'"
            />
          </div>
          <div class="action-list">
            <a-empty
              v-if="pendingList.length === 0"
              description="暂无待办事项"
            />
            <button
              v-for="item in visiblePendingList"
              :key="item.id"
              class="action-row"
              type="button"
              @click="router.push(item.link)"
            >
              <a-badge :color="item.color" />
              <span>
                <strong>{{ item.type }}</strong>
                <small>{{ item.desc }}</small>
              </span>
              <em>{{ item.time }}</em>
            </button>
          </div>
          <a-button
            v-if="isCompanyDashboard"
            block
            type="primary"
            ghost
            @click="router.push('/risk-center')"
            >进入风险中心</a-button
          >
          <a-button
            v-else
            block
            type="primary"
            ghost
            @click="router.push('/crm/follow-ups')"
            >进入跟进回访</a-button
          >
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :xs="24" :md="14">
        <a-card title="月度趋势" :loading="loading">
          <div class="trend-chart">
            <div class="trech-y-axis">
              <span v-for="val in trendYLabels" :key="val">{{ val }}</span>
            </div>
            <div class="trend-bars">
              <div v-for="m in trendData" :key="m.month" class="trend-col">
                <span class="trend-val">{{ formatMoney(m.amount) }}</span>
                <div
                  class="trend-bar"
                  :style="{ height: m.barHeight + '%' }"
                  :title="m.month + ' 成交'"
                ></div>
                <span class="trend-label">{{ m.month }}</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="10">
        <template v-if="isCompanyDashboard">
          <a-card class="right-panel cash-panel" :loading="loading">
            <div class="right-panel-head">
              <div>
                <span>现金与利润</span>
                <strong>资金压力</strong>
              </div>
              <a-tag :color="biSummary.netCashFlow < 0 ? 'red' : 'green'">{{
                biSummary.netCashFlow < 0 ? "净流出" : "净流入"
              }}</a-tag>
            </div>
            <div class="cash-focus">
              <span>净现金流</span>
              <strong :class="{ 'text-danger': biSummary.netCashFlow < 0 }">{{
                formatMoney(biSummary.netCashFlow)
              }}</strong>
              <small
                >回款 {{ formatMoney(biSummary.receivedAmount) }} / 待收
                {{ formatMoney(biSummary.receivableOutstanding) }}</small
              >
            </div>
            <div class="mini-metric-grid">
              <div v-for="item in rightMetrics" :key="item.label">
                <span>{{ item.label }}</span>
                <strong :class="{ 'text-danger': item.danger }">{{
                  item.value
                }}</strong>
              </div>
            </div>
          </a-card>
          <a-card class="right-panel risk-panel" :loading="loading">
            <div class="right-panel-head">
              <div>
                <span>风险清单</span>
                <strong>需要盯住的事项</strong>
              </div>
              <a-tag :color="riskScore > 0 ? 'red' : 'green'"
                >{{ riskScore }} 项</a-tag
              >
            </div>
            <div class="risk-check-list">
              <div
                v-for="item in riskItems"
                :key="item.label"
                class="risk-check-row"
              >
                <span :class="item.tone"></span>
                <div>
                  <strong>{{ item.label }}</strong>
                  <small>{{ item.desc }}</small>
                </div>
                <em>{{ item.value }}</em>
              </div>
            </div>
          </a-card>
        </template>
        <a-card v-else class="right-panel crm-focus-panel" :loading="loading">
          <div class="right-panel-head">
            <div>
              <span>CRM概览</span>
              <strong>客户、商机与跟进</strong>
            </div>
            <a-button
              type="link"
              size="small"
              @click="router.push('/crm/customers')"
              >客户池</a-button
            >
          </div>
          <div class="mini-metric-grid">
            <div v-for="item in crmMetrics" :key="item.label">
              <span>{{ item.label }}</span>
              <strong :class="{ 'text-danger': item.danger }">{{
                item.value
              }}</strong>
            </div>
          </div>
          <div class="crm-action-links">
            <a-button block @click="router.push('/crm/opportunities')"
              >商机管理</a-button
            >
            <a-button block @click="router.push('/crm/quotes')"
              >报价方案</a-button
            >
            <a-button block @click="router.push('/crm/contracts')"
              >客户合同</a-button
            >
          </div>
        </a-card>
        <a-card class="right-panel team-panel" :loading="loading">
          <div class="right-panel-head">
            <div>
              <span>团队产出</span>
              <strong>成交贡献</strong>
            </div>
            <a-button
              type="link"
              size="small"
              @click="router.push('/crm/opportunities')"
              >看商机</a-button
            >
          </div>
          <div class="team-rank-list">
            <div
              v-for="(item, index) in topTeamData"
              :key="item.name"
              class="team-rank-row"
            >
              <b :class="{ 'rank-top': index < 3 }">#{{ index + 1 }}</b>
              <span>
                <strong>{{ item.name }}</strong>
                <small>{{ item.opps }} 个商机 · 赢单 {{ item.won }} 单</small>
              </span>
              <em>{{ formatMoney(item.amount) }}</em>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { useRoute, useRouter } from "vue-router";
import {
  listCustomers,
  listOpportunities,
  listQuotes,
  listContracts,
  listReceivables,
  listFollowUps,
} from "@/api/crm";
import type {
  CustomerSummary,
  Opportunity,
  QuotePlan,
  ServiceContract,
} from "@/api/crm";
import { getExecutiveDashboard } from "@/api/bi";
import {
  listReplenishmentSuggestions,
  type ReplenishmentSuggestion,
} from "@/api/inventory";
import {
  listProcurementMatching,
  type ProcurementMatching,
} from "@/api/procurement";
import {
  listProjectProfitability,
  type ProjectProfitability,
} from "@/api/project";
import { formatMoney } from "./crm-options";
const moneyFormatter = (value: number | string) =>
  formatMoney(typeof value === "number" ? value : Number(value));

const router = useRouter();
const route = useRoute();
const loading = ref(true);
const winRate = ref(0);
const winCount = ref(0);
const lostCount = ref(0);

const stageColors: Record<string, string> = {
  LEAD: "#1890ff",
  QUALIFIED: "#52c41a",
  SOLUTION: "#13c2c2",
  QUOTATION: "#faad14",
  NEGOTIATION: "#f5222d",
  WON: "#52c41a",
  LOST: "#8c8c8c",
};
const stageLabels: Record<string, string> = {
  LEAD: "初步接触",
  QUALIFIED: "需求确认",
  SOLUTION: "方案制定",
  QUOTATION: "报价阶段",
  NEGOTIATION: "商务谈判",
  WON: "赢单",
  LOST: "丢单",
};

const biSummary = reactive({
  contractRevenue: 0,
  receivedAmount: 0,
  receivableOutstanding: 0,
  payableOutstanding: 0,
  projectCost: 0,
  workOrderCost: 0,
  inventoryValue: 0,
  netCashFlow: 0,
  activeProjects: 0,
  openWorkOrders: 0,
  completedWorkOrders: 0,
  lowStockParts: 0,
  renewalRisks: 0,
  complaints: 0,
});
const stats = reactive({
  customerCount: 0,
  newCustomerMonth: 0,
  activeOpportunityCount: 0,
  pipelineAmount: 0,
  monthContractCount: 0,
  monthContractAmount: 0,
  overdueCount: 0,
  overdueAmount: 0,
});
const stageData = ref<
  {
    stage: string;
    count: number;
    amount: number;
    percent: number;
    color: string;
  }[]
>([]);
const pendingList = ref<
  {
    id: string;
    type: string;
    desc: string;
    time: string;
    color: string;
    link: string;
  }[]
>([]);
const teamData = ref<
  { name: string; opps: number; won: number; amount: number }[]
>([]);
const trendData = ref<
  { month: string; count: number; amount: number; barHeight: number }[]
>([]);
const trendYLabels = ref<string[]>([]);
const customerItems = ref<CustomerSummary[]>([]);
const opportunityItems = ref<Opportunity[]>([]);
const quoteItems = ref<QuotePlan[]>([]);
const contractItems = ref<ServiceContract[]>([]);
const projectProfitItems = ref<ProjectProfitability[]>([]);
const procurementMatchItems = ref<ProcurementMatching[]>([]);
const replenishmentItems = ref<ReplenishmentSuggestion[]>([]);
type DetailSource =
  | "customers"
  | "opportunities"
  | "quotes"
  | "contracts"
  | "receivables"
  | "followUps"
  | "projects"
  | "procurement"
  | "inventory";
const detailAccess = reactive<Record<DetailSource, boolean>>({
  customers: true,
  opportunities: true,
  quotes: true,
  contracts: true,
  receivables: true,
  followUps: true,
  projects: true,
  procurement: true,
  inventory: true,
});
const detailComplete = computed(
  () =>
    detailAccess.contracts &&
    detailAccess.receivables &&
    detailAccess.projects &&
    detailAccess.procurement &&
    detailAccess.inventory,
);
const riskScore = computed(
  () =>
    stats.overdueCount +
    Number(biSummary.lowStockParts || 0) +
    Number(biSummary.renewalRisks || 0) +
    Number(biSummary.complaints || 0),
);
const healthScore = computed(() =>
  Math.max(
    0,
    Math.min(
      100,
      92 - riskScore.value * 6 - (biSummary.netCashFlow < 0 ? 8 : 0),
    ),
  ),
);
const visiblePendingList = computed(() => pendingList.value.slice(0, 4));
const topTeamData = computed(() => teamData.value.slice(0, 4));
const projectRiskItems = computed(() =>
  projectProfitItems.value.filter(
    (item) =>
      item.riskLevel !== "LOW" || Number(item.budgetUsageRate || 0) >= 90,
  ),
);
const procurementRiskItems = computed(() =>
  procurementMatchItems.value.filter((item) => item.matchStatus !== "MATCHED"),
);
const urgentReplenishmentItems = computed(() =>
  replenishmentItems.value.filter(
    (item) =>
      item.priority === "HIGH" ||
      Number(item.stockQty || 0) < Number(item.safetyQty || 0),
  ),
);
const isCompanyDashboard = computed(() => route.path === "/dashboard");
const closureIssueCount = computed(
  () =>
    stats.overdueCount +
    pendingList.value.length +
    projectRiskItems.value.length +
    procurementRiskItems.value.length +
    urgentReplenishmentItems.value.length,
);
const closureHealth = computed(() =>
  Math.max(
    0,
    Math.min(
      100,
      96 - closureIssueCount.value * 4 - (biSummary.netCashFlow < 0 ? 8 : 0),
    ),
  ),
);
const dashboardMetrics = computed(() =>
  isCompanyDashboard.value
    ? [
        {
          label: "合同收入",
          value: formatMoney(biSummary.contractRevenue),
          suffix: "",
          sub: `累计回款 ${formatMoney(biSummary.receivedAmount)}`,
          color: "#237804",
        },
        {
          label: "净现金流",
          value: formatMoney(biSummary.netCashFlow),
          suffix: "",
          sub: `待收 ${formatMoney(biSummary.receivableOutstanding)} / 待付 ${formatMoney(biSummary.payableOutstanding)}`,
          color: biSummary.netCashFlow < 0 ? "#ff4d4f" : "#237804",
        },
        {
          label: "在建项目",
          value: biSummary.activeProjects,
          suffix: "个",
          sub: `项目成本 ${formatMoney(biSummary.projectCost)}`,
          color: "#1890ff",
        },
        {
          label: "高优先风险",
          value: riskScore.value,
          suffix: "项",
          sub: `低库存 ${biSummary.lowStockParts} / 续约 ${biSummary.renewalRisks}`,
          color: riskScore.value > 0 ? "#ff4d4f" : "#52c41a",
        },
      ]
    : [
        {
          label: "客户总数",
          value: stats.customerCount,
          suffix: "家",
          sub: `${stats.newCustomerMonth} 家本月新增`,
          color: "#262626",
        },
        {
          label: "进行中商机",
          value: stats.activeOpportunityCount,
          suffix: "个",
          sub: `预计金额 ${formatMoney(stats.pipelineAmount)}`,
          color: "#1890ff",
        },
        {
          label: "本月签约",
          value: stats.monthContractCount,
          suffix: "份",
          sub: formatMoney(stats.monthContractAmount),
          color: stats.monthContractCount > 0 ? "#52c41a" : "#8c8c8c",
        },
        {
          label: "CRM逾期应收",
          value: stats.overdueCount,
          suffix: "笔",
          sub: `${formatMoney(stats.overdueAmount)} 未收回`,
          color: stats.overdueCount > 0 ? "#ff4d4f" : "#52c41a",
        },
      ],
);
const crmMetrics = computed(() => [
  { label: "客户总量", value: `${stats.customerCount} 家`, danger: false },
  {
    label: "商机金额",
    value: formatMoney(stats.pipelineAmount),
    danger: false,
  },
  {
    label: "待处理动作",
    value: `${pendingList.value.length} 项`,
    danger: pendingList.value.length > 0,
  },
  { label: "赢单率", value: `${winRate.value}%`, danger: winRate.value < 40 },
]);
const crmPipelineSteps = computed(() => {
  const customers = customerItems.value.length;
  const leads = opportunityItems.value.length;
  const quoted = quoteItems.value.filter(
    (item) => !["DRAFT", "REJECTED"].includes(item.status),
  ).length;
  const accepted = quoteItems.value.filter(
    (item) =>
      item.status === "CUSTOMER_ACCEPTED" || item.status === "CONVERTED",
  ).length;
  const contracts = contractItems.value.length;
  return [
    {
      key: "customer",
      label: "客户",
      value: customers,
      hint: `${stats.newCustomerMonth} 家本月新增`,
      link: "/crm/customers",
    },
    {
      key: "opp",
      label: "商机",
      value: leads,
      hint: customers
        ? `客户转商机 ${Math.round((leads / customers) * 100)}%`
        : "等待客户沉淀",
      link: "/crm/opportunities",
    },
    {
      key: "quote",
      label: "报价",
      value: quoted,
      hint: leads
        ? `商机转报价 ${Math.round((quoted / leads) * 100)}%`
        : "等待商机推进",
      link: "/crm/quotes",
    },
    {
      key: "accept",
      label: "客户接受",
      value: accepted,
      hint: quoted
        ? `报价接受 ${Math.round((accepted / quoted) * 100)}%`
        : "等待客户反馈",
      link: "/crm/quotes",
    },
    {
      key: "contract",
      label: "合同",
      value: contracts,
      hint: accepted
        ? `接受转合同 ${Math.round((contracts / accepted) * 100)}%`
        : "等待转合同",
      link: "/crm/contracts",
    },
  ];
});
const salesForecast = computed(() => {
  const open = opportunityItems.value.filter(
    (item) => !["WON", "LOST"].includes(item.stage),
  );
  return {
    count: open.length,
    weightedAmount: open.reduce(
      (sum, item) =>
        sum +
        (Number(item.expectedAmount || 0) * Number(item.probability || 0)) /
          100,
      0,
    ),
  };
});
const forecastByStage = computed(() => {
  const rows = Object.entries(
    opportunityItems.value
      .filter((item) => !["WON", "LOST"].includes(item.stage))
      .reduce(
        (map, item) => {
          const label = stageLabels[item.stage] || item.stage;
          map[label] =
            (map[label] || 0) +
            (Number(item.expectedAmount || 0) * Number(item.probability || 0)) /
              100;
          return map;
        },
        {} as Record<string, number>,
      ),
  ).map(([stage, amount]) => ({ stage, amount }));
  const max = Math.max(...rows.map((item) => item.amount), 1);
  return rows
    .map((item) => ({
      ...item,
      percent: Math.max(6, Math.round((item.amount / max) * 100)),
    }))
    .slice(0, 5);
});
const customerRiskItems = computed(() =>
  customerItems.value
    .filter(
      (item) =>
        item.riskStatus !== "NORMAL" ||
        item.level === "STRATEGIC" ||
        item.paymentHabit?.includes("慢"),
    )
    .map((item) => ({
      id: item.id,
      name: item.name,
      level:
        item.level === "STRATEGIC"
          ? "战略"
          : item.level === "KEY"
            ? "重点"
            : "普通",
      color:
        item.riskStatus === "OVERDUE"
          ? "red"
          : item.riskStatus === "RENEWAL_RISK"
            ? "orange"
            : item.level === "STRATEGIC"
              ? "blue"
              : "default",
      reason:
        item.riskStatus === "OVERDUE"
          ? "存在逾期风险"
          : item.riskStatus === "RENEWAL_RISK"
            ? "续约风险客户"
            : item.paymentHabit || "重点维护客户",
    }))
    .slice(0, 8),
);
const rightMetrics = computed(() => [
  {
    label: "合同收入",
    value: formatMoney(biSummary.contractRevenue),
    danger: false,
  },
  {
    label: "待收金额",
    value: formatMoney(biSummary.receivableOutstanding),
    danger: biSummary.receivableOutstanding > 0,
  },
  {
    label: "库存资产",
    value: formatMoney(biSummary.inventoryValue),
    danger: false,
  },
  {
    label: "待支付",
    value: formatMoney(biSummary.payableOutstanding),
    danger: biSummary.payableOutstanding > biSummary.receivedAmount,
  },
]);
const riskItems = computed(() => [
  {
    label: "逾期应收",
    value: detailAccess.receivables ? `${stats.overdueCount} 笔` : "--",
    desc: detailAccess.receivables
      ? formatMoney(stats.overdueAmount)
      : "当前角色无明细权限",
    tone: detailAccess.receivables
      ? stats.overdueCount > 0
        ? "danger"
        : "good"
      : "warn",
  },
  {
    label: "低库存",
    value: `${biSummary.lowStockParts} 项`,
    desc: "影响工单交付",
    tone: biSummary.lowStockParts > 0 ? "warn" : "good",
  },
  {
    label: "续约风险",
    value: `${biSummary.renewalRisks} 份`,
    desc: "30 天内需跟进",
    tone: biSummary.renewalRisks > 0 ? "warn" : "good",
  },
  {
    label: "客户投诉",
    value: `${biSummary.complaints} 件`,
    desc: "服务质量跟踪",
    tone: biSummary.complaints > 0 ? "danger" : "good",
  },
]);
const closureLoops = computed(() => [
  {
    key: "order-cash",
    title: "销售回款",
    desc:
      detailAccess.contracts && detailAccess.receivables
        ? `${stats.monthContractCount} 份本月签约，${stats.overdueCount} 笔逾期待回收`
        : "当前角色无销售明细权限",
    value:
      detailAccess.contracts && detailAccess.receivables
        ? stats.overdueCount > 0
          ? `${stats.overdueCount} 笔`
          : "正常"
        : "数据受限",
    percent:
      stats.overdueCount > 0 ? Math.max(8, 100 - stats.overdueCount * 12) : 100,
    stroke: stats.overdueCount > 0 ? "#ff4d4f" : "#52c41a",
    tone: stats.overdueCount > 0 ? "danger" : "good",
    link: "/finance/receivables",
  },
  {
    key: "project-profit",
    title: "项目利润",
    desc: detailAccess.projects
      ? `${projectProfitItems.value.length} 个项目纳入利润跟踪`
      : "当前角色无项目明细权限",
    value: !detailAccess.projects
      ? "数据受限"
      : projectRiskItems.value.length > 0
        ? `${projectRiskItems.value.length} 项`
        : "达标",
    percent: projectProfitItems.value.length
      ? Math.max(
          8,
          Math.round(
            ((projectProfitItems.value.length - projectRiskItems.value.length) /
              projectProfitItems.value.length) *
              100,
          ),
        )
      : 100,
    stroke: projectRiskItems.value.length > 0 ? "#faad14" : "#52c41a",
    tone: projectRiskItems.value.length > 0 ? "warn" : "good",
    link: "/projects/list",
  },
  {
    key: "procurement-pay",
    title: "采购付款",
    desc: detailAccess.procurement
      ? `${procurementMatchItems.value.length} 张订单完成 P2P 勾稽`
      : "当前角色无采购明细权限",
    value: !detailAccess.procurement
      ? "数据受限"
      : procurementRiskItems.value.length > 0
        ? `${procurementRiskItems.value.length} 条`
        : "匹配",
    percent: procurementMatchItems.value.length
      ? Math.max(
          8,
          Math.round(
            ((procurementMatchItems.value.length -
              procurementRiskItems.value.length) /
              procurementMatchItems.value.length) *
              100,
          ),
        )
      : 100,
    stroke: procurementRiskItems.value.length > 0 ? "#fa8c16" : "#52c41a",
    tone: procurementRiskItems.value.length > 0 ? "warn" : "good",
    link: "/procurement/p2p",
  },
  {
    key: "inventory-delivery",
    title: "库存交付",
    desc: detailAccess.inventory
      ? `${replenishmentItems.value.length} 条补货建议，优先保障交付`
      : "当前角色无库存明细权限",
    value: !detailAccess.inventory
      ? "数据受限"
      : urgentReplenishmentItems.value.length > 0
        ? `${urgentReplenishmentItems.value.length} 项`
        : "充足",
    percent: replenishmentItems.value.length
      ? Math.max(
          8,
          Math.round(
            ((replenishmentItems.value.length -
              urgentReplenishmentItems.value.length) /
              replenishmentItems.value.length) *
              100,
          ),
        )
      : 100,
    stroke: urgentReplenishmentItems.value.length > 0 ? "#faad14" : "#52c41a",
    tone: urgentReplenishmentItems.value.length > 0 ? "warn" : "good",
    link: "/inventory/analytics",
  },
]);
const closureAlerts = computed(() => {
  const alerts: Array<{
    key: string;
    type: string;
    title: string;
    desc: string;
    value: string;
    color: string;
    link: string;
  }> = [];
  if (stats.overdueCount > 0) {
    alerts.push({
      key: "overdue-receivable",
      type: "应收",
      title: "逾期回款需要财务与销售协同",
      desc: "优先处理账龄高、金额大的应收款",
      value: formatMoney(stats.overdueAmount),
      color: "red",
      link: "/finance/receivables",
    });
  }
  const projectRisk = projectRiskItems.value[0];
  if (projectRisk) {
    alerts.push({
      key: `project-${projectRisk.projectId}`,
      type: "项目",
      title: projectRisk.projectName,
      desc: projectRisk.riskMessage || "预算、成本或毛利需要复核",
      value: `${Math.round(Number(projectRisk.grossMarginRate || 0) * 100)}%`,
      color: projectRisk.riskLevel === "HIGH" ? "red" : "orange",
      link: "/projects/list",
    });
  }
  const procurementRisk = procurementRiskItems.value[0];
  if (procurementRisk) {
    alerts.push({
      key: `procurement-${procurementRisk.orderId}`,
      type: "采购",
      title: procurementRisk.orderCode || procurementRisk.partName,
      desc: procurementRisk.riskMessage || "订单、到货、应付或付款存在差异",
      value: procurementRisk.matchStatus,
      color: "orange",
      link: "/procurement/p2p",
    });
  }
  const replenishRisk = urgentReplenishmentItems.value[0];
  if (replenishRisk) {
    alerts.push({
      key: `inventory-${replenishRisk.partId}`,
      type: "库存",
      title: replenishRisk.partName,
      desc: replenishRisk.reason || "安全库存不足，需确认采购或调拨",
      value: `${replenishRisk.suggestedQty}`,
      color: "gold",
      link: "/inventory/analytics",
    });
  }
  return alerts.slice(0, 4);
});
const teamColumns = [
  { title: "#", key: "rank", width: 40 },
  { title: "负责人", dataIndex: "name", width: 80 },
  { title: "商机", dataIndex: "opps", width: 60 },
  { title: "赢单", key: "won", width: 70 },
  { title: "成交金额", dataIndex: "amount", width: 110 },
];

onMounted(loadData);

async function safeList<T>(
  loader: () => Promise<T[]>,
  source: DetailSource,
): Promise<T[]> {
  try {
    const rows = await loader();
    detailAccess[source] = true;
    return rows;
  } catch {
    detailAccess[source] = false;
    return [];
  }
}

async function loadData() {
  loading.value = true;
  try {
    const [
      customers,
      opps,
      quotes,
      contracts,
      receivables,
      followUps,
      projectProfitability,
      procurementMatching,
      replenishmentSuggestions,
    ] = await Promise.all([
      safeList(listCustomers, "customers"),
      safeList(listOpportunities, "opportunities"),
      safeList(listQuotes, "quotes"),
      safeList(listContracts, "contracts"),
      safeList(listReceivables, "receivables"),
      safeList(listFollowUps, "followUps"),
      safeList(listProjectProfitability, "projects"),
      safeList(listProcurementMatching, "procurement"),
      safeList(listReplenishmentSuggestions, "inventory"),
    ]);
    customerItems.value = customers;
    opportunityItems.value = opps;
    quoteItems.value = quotes;
    contractItems.value = contracts;
    projectProfitItems.value = projectProfitability;
    procurementMatchItems.value = procurementMatching;
    replenishmentItems.value = replenishmentSuggestions;
    const now = new Date();
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1)
      .toISOString()
      .slice(0, 10);
    const in30d = new Date(Date.now() + 30 * 86400000)
      .toISOString()
      .slice(0, 10);
    const activeOpps = opps.filter(
      (o) => o.stage !== "WON" && o.stage !== "LOST",
    );

    // Stats
    stats.customerCount = customers.length;
    stats.activeOpportunityCount = activeOpps.length;
    stats.pipelineAmount = activeOpps.reduce(
      (s, o) => s + Number(o.expectedAmount || 0),
      0,
    );

    const monthContracts = contracts.filter(
      (c) => c.status === "ACTIVE" && c.startDate >= monthStart,
    );
    stats.monthContractCount = monthContracts.length;
    stats.monthContractAmount = monthContracts.reduce(
      (s, c) => s + Number(c.amount || 0),
      0,
    );

    const overdueItems = receivables.filter(
      (r) =>
        r.status !== "SETTLED" &&
        r.dueDate &&
        r.dueDate < now.toISOString().slice(0, 10),
    );
    stats.overdueCount = overdueItems.length;
    stats.overdueAmount = overdueItems.reduce(
      (s, r) => s + Number(r.outstandingAmount || 0),
      0,
    );

    // Win rate
    winCount.value = opps.filter((o) => o.stage === "WON").length;
    lostCount.value = opps.filter((o) => o.stage === "LOST").length;
    const closed = winCount.value + lostCount.value;
    winRate.value =
      closed > 0 ? Math.round((winCount.value / closed) * 100) : 0;

    // Pipeline funnel
    const stageOrder = [
      "LEAD",
      "QUALIFIED",
      "SOLUTION",
      "QUOTATION",
      "NEGOTIATION",
      "WON",
      "LOST",
    ];
    const stageCounts: Record<string, number> = {};
    const stageAmounts: Record<string, number> = {};
    opps.forEach((o) => {
      stageCounts[o.stage] = (stageCounts[o.stage] || 0) + 1;
      stageAmounts[o.stage] =
        (stageAmounts[o.stage] || 0) + Number(o.expectedAmount || 0);
    });
    const maxCount = Math.max(...Object.values(stageCounts), 1);
    stageData.value = stageOrder
      .filter((s) => stageCounts[s])
      .map((s) => ({
        stage: stageLabels[s] || s,
        count: stageCounts[s],
        amount: stageAmounts[s] || 0,
        percent: Math.round(((stageCounts[s] || 0) / maxCount) * 100),
        color: stageColors[s] || "#8c8c8c",
      }));

    // Monthly trend (past 6 months)
    const monthMap: Record<string, { count: number; amount: number }> = {};
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
      monthMap[key] = { count: 0, amount: 0 };
    }
    contracts
      .filter((c) => c.status === "ACTIVE" || c.status === "CLOSED")
      .forEach((c) => {
        if (c.startDate) {
          const key = c.startDate.slice(0, 7);
          if (monthMap[key]) {
            monthMap[key].count++;
            monthMap[key].amount += Number(c.amount || 0);
          }
        }
      });
    const maxAmount = Math.max(
      ...Object.values(monthMap).map((m) => m.amount),
      1,
    );
    trendData.value = Object.entries(monthMap).map(([month, data]) => ({
      month: month.slice(5),
      count: data.count,
      amount: data.amount,
      barHeight: Math.max(Math.round((data.amount / maxAmount) * 100), 2),
    }));
    const yMax = Math.ceil(maxAmount / 100000) * 100000;
    trendYLabels.value = [
      formatMoney(yMax),
      formatMoney(Math.round(yMax / 2)),
      "¥0",
    ];

    // Team leaderboard
    const teamMap: Record<
      string,
      { opps: number; won: number; amount: number }
    > = {};
    opps.forEach((o) => {
      const name = o.ownerName || "未分配";
      if (!teamMap[name]) teamMap[name] = { opps: 0, won: 0, amount: 0 };
      teamMap[name].opps++;
      if (o.stage === "WON") {
        teamMap[name].won++;
        teamMap[name].amount += Number(o.expectedAmount || 0);
      }
    });
    teamData.value = Object.entries(teamMap)
      .map(([name, data]) => ({ name, ...data }))
      .sort((a, b) => b.won - a.won || b.amount - a.amount);

    // Pending items
    const todos: any[] = [];
    const pendingQuotes = quotes.filter((q) => q.status === "PENDING_APPROVAL");
    pendingQuotes.forEach((q) =>
      todos.push({
        id: q.id,
        type: "报价审批",
        desc: `${q.code} ${q.customerName}`,
        time:
          daysSince(q.updatedAt) > 3
            ? `超 ${daysSince(q.updatedAt)} 天`
            : "待处理",
        color: daysSince(q.updatedAt) > 3 ? "red" : "orange",
        link: `/crm/quotes/${q.id}`,
      }),
    );
    activeOpps
      .filter(
        (o) =>
          !o.nextActionAt ||
          daysSince(o.nextActionAt) > 7 ||
          daysSince(o.updatedAt) > 14,
      )
      .slice(0, 8)
      .forEach((o) =>
        todos.push({
          id: `opp-${o.id}`,
          type: "商机跟进",
          desc: `${o.code || "商机"} ${o.customerName}`,
          time: o.nextActionAt ? `计划 ${o.nextActionAt}` : "缺计划",
          color: "blue",
          link: `/crm/opportunities/${o.id}`,
        }),
      );
    const expiring = contracts.filter(
      (c) => c.status === "ACTIVE" && c.endDate && c.endDate <= in30d,
    );
    expiring.forEach((c) =>
      todos.push({
        id: c.id,
        type: "合同到期",
        desc: `${c.projectName} ${c.code}`,
        time: c.endDate || "未知",
        color: "red",
        link: `/crm/contracts/${c.id}`,
      }),
    );
    overdueItems.forEach((r) =>
      todos.push({
        id: r.id,
        type: "应收逾期",
        desc: `${r.code} ${r.customerName}`,
        time: formatMoney(r.outstandingAmount),
        color: "red",
        link: "/crm/receivables",
      }),
    );
    // Overdue next actions from follow-ups
    const nowStr = now.toISOString().slice(0, 10);
    followUps
      .filter((f) => f.nextAction && f.opportunityId)
      .forEach((f) => {
        if (!todos.some((t) => t.id === f.opportunityId)) {
          // We'll just take a sample
        }
      });
    pendingList.value = todos.slice(0, 20);
    try {
      const bi = await getExecutiveDashboard();
      Object.assign(biSummary, bi.summary);
    } catch {
      /* BI data is supplementary */
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : "数据加载失败");
  } finally {
    loading.value = false;
  }
}

function daysSince(value?: string) {
  if (!value) return 999;
  return Math.floor((Date.now() - new Date(value).getTime()) / 86400000);
}
</script>

<style scoped>
.dash-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.dash-header h2 {
  margin: 0;
}
.dash-header .ant-btn {
  margin: 0;
}
.metric-card .ant-card-body {
  padding: 18px 20px;
}
.metric-sub {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}
.closure-panel {
  margin-top: 16px;
}
.closure-panel :deep(.ant-card-body) {
  padding: 18px;
}
.closure-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.closure-head > div:first-child {
  display: grid;
  gap: 4px;
  min-width: 0;
}
.closure-head span,
.closure-score span {
  color: #8c8c8c;
  font-size: 12px;
  font-weight: 600;
}
.closure-head strong {
  color: #1f1f1f;
  font-size: 17px;
  line-height: 1.35;
}
.closure-score {
  display: grid;
  gap: 4px;
  min-width: 96px;
  text-align: right;
}
.closure-score strong {
  color: #237804;
  font-size: 28px;
  line-height: 1;
}
.closure-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.closure-loop {
  display: grid;
  gap: 10px;
  width: 100%;
  min-height: 118px;
  padding: 14px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}
.closure-loop:hover,
.closure-alert:hover {
  border-color: #91caff;
  box-shadow: 0 4px 14px rgba(22, 119, 255, 0.08);
}
.closure-loop span,
.closure-alert span {
  display: grid;
  gap: 4px;
  min-width: 0;
}
.closure-loop strong,
.closure-alert strong {
  overflow: hidden;
  color: #262626;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.closure-loop small,
.closure-alert small {
  overflow: hidden;
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.closure-loop em {
  align-self: end;
  color: #237804;
  font-size: 22px;
  font-style: normal;
  font-weight: 700;
  line-height: 1;
}
.closure-loop em.warn {
  color: #d48806;
}
.closure-loop em.danger {
  color: #cf1322;
}
.closure-alerts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}
.closure-alert {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fbfbfb;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}
.closure-alert em {
  color: #595959;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  white-space: nowrap;
}
.closure-empty {
  grid-column: 1 / -1;
  padding: 12px;
  border: 1px dashed #d9f7be;
  border-radius: 8px;
  background: #fcfffa;
  color: #389e0d;
  font-size: 13px;
  text-align: center;
}
.crm-closure-panel {
  margin-top: 16px;
}
.crm-closure-panel :deep(.ant-card-body) {
  padding: 18px;
}
.crm-pipeline-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}
.pipeline-step {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}
.pipeline-step:hover,
.customer-risk-row:hover {
  border-color: #91caff;
  background: #f6faff;
}
.pipeline-step span {
  color: #667085;
  font-size: 12px;
}
.pipeline-step strong {
  color: #101828;
  font-size: 22px;
  line-height: 1;
}
.pipeline-step em {
  overflow: hidden;
  color: #98a2b3;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.crm-insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
  margin-top: 12px;
}
.crm-insight-block {
  min-width: 0;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfcfe;
}
.right-panel-head.compact {
  margin-bottom: 10px;
}
.forecast-bars {
  display: grid;
  gap: 8px;
}
.forecast-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 110px;
  gap: 8px;
  align-items: center;
}
.forecast-row span,
.forecast-row em {
  overflow: hidden;
  color: #667085;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.forecast-row em {
  color: #1f2937;
  font-style: normal;
  text-align: right;
}
.forecast-row div {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2f7;
}
.forecast-row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #1677ff;
}
.customer-risk-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  width: 100%;
  padding: 8px 0;
  border: 0;
  border-bottom: 1px solid #eef2f7;
  background: transparent;
  cursor: pointer;
  text-align: left;
}
.customer-risk-row:last-child {
  border-bottom: 0;
}
.customer-risk-row span {
  display: grid;
  gap: 2px;
  min-width: 0;
}
.customer-risk-row strong,
.customer-risk-row small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.customer-risk-row strong {
  color: #1f2937;
  font-size: 13px;
}
.customer-risk-row small {
  color: #8c8c8c;
  font-size: 12px;
}
.funnel-chart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px 0;
}
.funnel-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.funnel-label {
  width: 80px;
  font-size: 13px;
  color: #333;
  text-align: right;
  flex-shrink: 0;
}
.funnel-bar-wrap {
  flex: 1;
  height: 28px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}
.funnel-bar {
  height: 100%;
  border-radius: 4px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  transition: width 0.4s;
  min-width: 0;
}
.funnel-bar-text {
  color: #fff;
  font-size: 12px;
  white-space: nowrap;
}
.win-rate {
  margin-top: 12px;
  font-size: 14px;
  text-align: center;
}
.todo-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}
.todo-item:last-child {
  border-bottom: none;
}
.todo-body {
  flex: 1;
  min-width: 0;
}
.todo-desc {
  font-size: 13px;
  line-height: 1.4;
}
.todo-meta {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}
.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 200px;
  padding: 8px 0;
}
.trech-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  font-size: 11px;
  color: #8c8c8c;
  text-align: right;
  padding-right: 6px;
}
.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex: 1;
  height: 100%;
}
.trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
}
.trend-val {
  font-size: 10px;
  color: #8c8c8c;
  margin-bottom: 4px;
  white-space: nowrap;
}
.trend-bar {
  width: 100%;
  max-width: 40px;
  background: linear-gradient(180deg, #1890ff, #69c0ff);
  border-radius: 4px 4px 0 0;
  transition: height 0.4s;
  min-height: 2px;
}
.trend-label {
  font-size: 12px;
  color: #595959;
  margin-top: 6px;
}
.rank-top {
  color: #f5222d;
  font-weight: 700;
}
.right-panel {
  margin-bottom: 16px;
}
.right-panel:last-child {
  margin-bottom: 0;
}
.right-panel :deep(.ant-card-body) {
  padding: 16px;
}
.right-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.right-panel-head div {
  display: grid;
  gap: 3px;
  min-width: 0;
}
.right-panel-head span {
  color: #8c8c8c;
  font-size: 12px;
  font-weight: 600;
}
.right-panel-head strong {
  color: #1f1f1f;
  font-size: 16px;
  line-height: 1.3;
}
.health-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fafcff;
}
.health-card > div {
  display: grid;
  gap: 4px;
}
.health-card span,
.cash-focus span,
.mini-metric-grid span {
  color: #8c8c8c;
  font-size: 12px;
}
.health-card strong {
  color: #237804;
  font-size: 34px;
  line-height: 1;
}
.action-list {
  display: grid;
  gap: 8px;
  margin: 12px 0;
}
.action-row {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  width: 100%;
  padding: 10px 0;
  border: 0;
  border-bottom: 1px solid #f0f0f0;
  background: transparent;
  text-align: left;
}
.action-row:last-child {
  border-bottom: 0;
}
.action-row span {
  display: grid;
  gap: 2px;
  min-width: 0;
}
.action-row strong,
.risk-check-row strong,
.team-rank-row strong {
  color: #262626;
  font-size: 13px;
}
.action-row small,
.risk-check-row small,
.team-rank-row small,
.cash-focus small {
  overflow: hidden;
  color: #8c8c8c;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.action-row em,
.risk-check-row em,
.team-rank-row em {
  color: #595959;
  font-style: normal;
  font-size: 12px;
  white-space: nowrap;
}
.cash-focus {
  display: grid;
  gap: 4px;
  padding: 14px;
  border-radius: 8px;
  background: #f6ffed;
}
.cash-focus strong {
  color: #237804;
  font-size: 22px;
  line-height: 1.2;
}
.mini-metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}
.mini-metric-grid > div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
}
.mini-metric-grid strong {
  overflow: hidden;
  color: #262626;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.crm-action-links {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}
.crm-action-links :deep(.ant-btn) {
  padding-inline: 8px;
}
.risk-check-list,
.team-rank-list {
  display: grid;
  gap: 10px;
}
.risk-check-row,
.team-rank-row {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}
.risk-check-row:last-child,
.team-rank-row:last-child {
  border-bottom: 0;
}
.risk-check-row > span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c41a;
}
.risk-check-row > span.warn {
  background: #faad14;
}
.risk-check-row > span.danger {
  background: #ff4d4f;
}
.team-rank-row {
  grid-template-columns: 34px minmax(0, 1fr) auto;
}
.team-rank-row b {
  color: #8c8c8c;
}
.team-rank-row span {
  display: grid;
  gap: 2px;
  min-width: 0;
}
@media (max-width: 720px) {
  .trend-chart {
    height: 160px;
  }
  .mini-metric-grid,
  .crm-action-links {
    grid-template-columns: 1fr;
  }
  .closure-head {
    display: grid;
  }
  .closure-score {
    text-align: left;
  }
  .closure-grid,
  .closure-alerts,
  .crm-pipeline-grid,
  .crm-insight-grid {
    grid-template-columns: 1fr;
  }
  .closure-loop {
    min-height: 104px;
  }
  .forecast-row {
    grid-template-columns: 76px minmax(0, 1fr);
  }
  .forecast-row em {
    grid-column: 2;
    text-align: left;
  }
}
</style>
