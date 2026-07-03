<template>
<div class="page-stack">
  <div class="dash-header">
    <h2>经营驾驶舱</h2>
    <a-button :loading="loading" @click="loadData">刷新</a-button>
  </div>

  <a-row :gutter="[16, 16]" class="metric-row">
    <a-col :xs="12" :md="6">
      <a-card class="metric-card" :loading="loading">
        <a-statistic title="客户总数" :value="stats.customerCount" suffix="家" />
        <div class="metric-sub">{{ stats.newCustomerMonth }} 家本月新增</div>
      </a-card>
    </a-col>
    <a-col :xs="12" :md="6">
      <a-card class="metric-card" :loading="loading">
        <a-statistic title="进行中商机" :value="stats.activeOpportunityCount" suffix="个" :value-style="{ color: '#1890ff' }" />
        <div class="metric-sub">预计金额 {{ formatMoney(stats.pipelineAmount) }}</div>
      </a-card>
    </a-col>
    <a-col :xs="12" :md="6">
      <a-card class="metric-card" :loading="loading">
        <a-statistic title="本月签约" :value="stats.monthContractCount" suffix="份" :value-style="{ color: stats.monthContractCount > 0 ? '#52c41a' : '#8c8c8c' }" />
        <div class="metric-sub">{{ formatMoney(stats.monthContractAmount) }}</div>
      </a-card>
    </a-col>
    <a-col :xs="12" :md="6">
      <a-card class="metric-card" :loading="loading">
        <a-statistic title="逾期应收" :value="stats.overdueCount" suffix="笔" :value-style="{ color: stats.overdueCount > 0 ? '#ff4d4f' : '#52c41a' }" />
        <div class="metric-sub">{{ formatMoney(stats.overdueAmount) }} 未收回</div>
      </a-card>
    </a-col>
  </a-row>

  <a-row :gutter="[16, 16]" style="margin-top: 16px">
    <a-col :xs="24" :md="14">
      <a-card title="销售漏斗" :loading="loading">
        <div class="funnel-chart">
          <div v-for="item in stageData" :key="item.stage" class="funnel-row">
            <span class="funnel-label">{{ item.stage }}</span>
            <div class="funnel-bar-wrap">
              <div class="funnel-bar" :style="{ width: item.percent + '%', background: item.color }">
                <span v-if="item.percent > 15" class="funnel-bar-text">{{ item.count }} / {{ formatMoney(item.amount) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="win-rate">
          整体赢单率：
          <strong :style="{ color: winRate >= 50 ? '#52c41a' : '#ff4d4f' }">{{ winRate }}%</strong>
          （赢单 {{ winCount }} / 已完结 {{ winCount + lostCount }}）
        </div>
      </a-card>
    </a-col>
    <a-col :xs="24" :md="10">
      <a-card title="待办事项" :loading="loading">
        <a-empty v-if="pendingList.length === 0" description="暂无待办事项" />
        <div v-for="item in pendingList" :key="item.id" class="todo-item" @click="router.push(item.link)">
          <a-badge :color="item.color" />
          <div class="todo-body">
            <div class="todo-desc">{{ item.desc }}</div>
            <div class="todo-meta">{{ item.type }} · {{ item.time }}</div>
          </div>
        </div>
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
              <div class="trend-bar" :style="{ height: m.barHeight + '%' }" :title="m.month + ' 成交'"></div>
              <span class="trend-label">{{ m.month }}</span>
            </div>
          </div>
        </div>
      </a-card>
    </a-col>
    <a-col :xs="24" :md="10">
      <a-card title="企业经营数据" :loading="loading">
        <a-row :gutter="[16, 8]">
          <a-col :xs="12" :md="6"><a-statistic title="合同收入" :value="biSummary.contractRevenue" :formatter="moneyFormatter" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="累计回款" :value="biSummary.receivedAmount" :formatter="moneyFormatter" :value-style="{color:'#52c41a'}" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="待收金额" :value="biSummary.receivableOutstanding" :formatter="moneyFormatter" :value-style="{color:biSummary.receivableOutstanding>0?'#faad14':'#52c41a'}" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="库存资产" :value="biSummary.inventoryValue" :formatter="moneyFormatter" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="在建项目" :value="biSummary.activeProjects" suffix="个" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="待续约合同" :value="biSummary.renewalRisks" suffix="份" :value-style="{color:biSummary.renewalRisks>0?'#ff4d4f':'#52c41a'}" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="待收金额" :value="biSummary.payableOutstanding" :formatter="moneyFormatter" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="净现金流" :value="biSummary.netCashFlow" :formatter="moneyFormatter" :value-style="{color:biSummary.netCashFlow<0?'#ff4d4f':'#237804'}" /></a-col>
        </a-row>
      </a-card>
      <a-card title="团队排行" :loading="loading">
        <a-table :data-source="teamData" :columns="teamColumns" :pagination="false" size="small">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'rank'">
              <span :class="{ 'rank-top': index < 3 }">#{{ index + 1 }}</span>
            </template>
            <template v-else-if="column.key === 'won'">
              <span :style="{ color: record.won > 0 ? '#52c41a' : '#8c8c8c' }">{{ record.won }} 单</span>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-col>
  </a-row>
</div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { useRouter } from "vue-router";
import { listCustomers, listOpportunities, listQuotes, listContracts, listReceivables, listFollowUps } from "@/api/crm";
import { getExecutiveDashboard } from "@/api/bi";
import { formatMoney } from "./crm-options";

const router = useRouter();
const loading = ref(true);
const winRate = ref(0);
const winCount = ref(0);
const lostCount = ref(0);

const stageColors: Record<string, string> = {
  LEAD: "#1890ff", QUALIFIED: "#52c41a", SOLUTION: "#13c2c2",
  QUOTATION: "#faad14", NEGOTIATION: "#f5222d", WON: "#52c41a", LOST: "#8c8c8c",
};
const stageLabels: Record<string, string> = {
  LEAD: "初步接触", QUALIFIED: "需求确认", SOLUTION: "方案制定",
  QUOTATION: "报价阶段", NEGOTIATION: "商务谈判", WON: "赢单", LOST: "丢单",
};

const biSummary = reactive({
  contractRevenue:0, receivedAmount:0, receivableOutstanding:0,
  payableOutstanding:0, projectCost:0, workOrderCost:0,
  inventoryValue:0, netCashFlow:0, activeProjects:0,
  openWorkOrders:0, completedWorkOrders:0, lowStockParts:0,
  renewalRisks:0, complaints:0,
});
const stats = reactive({
  customerCount: 0, newCustomerMonth: 0,
  activeOpportunityCount: 0, pipelineAmount: 0,
  monthContractCount: 0, monthContractAmount: 0,
  overdueCount: 0, overdueAmount: 0,
});
const stageData = ref<{ stage: string; count: number; amount: number; percent: number; color: string }[]>([]);
const pendingList = ref<{ id: string; type: string; desc: string; time: string; color: string; link: string }[]>([]);
const teamData = ref<{ name: string; opps: number; won: number; amount: number }[]>([]);
const trendData = ref<{ month: string; count: number; amount: number; barHeight: number }[]>([]);
const trendYLabels = ref<string[]>([]);
const teamColumns = [
  { title: "#", key: "rank", width: 40 },
  { title: "负责人", dataIndex: "name", width: 80 },
  { title: "商机", dataIndex: "opps", width: 60 },
  { title: "赢单", key: "won", width: 70 },
  { title: "成交金额", dataIndex: "amount", width: 110 },
];

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [customers, opps, quotes, contracts, receivables, followUps] = await Promise.all([
      listCustomers(), listOpportunities(), listQuotes(),
      listContracts(), listReceivables(), listFollowUps(),
    ]);
    const now = new Date();
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
    const in30d = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10);
    const activeOpps = opps.filter((o) => o.stage !== "WON" && o.stage !== "LOST");

    // Stats
    stats.customerCount = customers.length;
    stats.activeOpportunityCount = activeOpps.length;
    stats.pipelineAmount = activeOpps.reduce((s, o) => s + Number(o.expectedAmount || 0), 0);

    const monthContracts = contracts.filter((c) => c.status === "ACTIVE" && c.startDate >= monthStart);
    stats.monthContractCount = monthContracts.length;
    stats.monthContractAmount = monthContracts.reduce((s, c) => s + Number(c.amount || 0), 0);

    const overdueItems = receivables.filter((r) => r.status !== "SETTLED" && r.dueDate && r.dueDate < now.toISOString().slice(0, 10));
    stats.overdueCount = overdueItems.length;
    stats.overdueAmount = overdueItems.reduce((s, r) => s + Number(r.outstandingAmount || 0), 0);

    // Win rate
    winCount.value = opps.filter((o) => o.stage === "WON").length;
    lostCount.value = opps.filter((o) => o.stage === "LOST").length;
    const closed = winCount.value + lostCount.value;
    winRate.value = closed > 0 ? Math.round(winCount.value / closed * 100) : 0;

    // Pipeline funnel
    const stageOrder = ["LEAD", "QUALIFIED", "SOLUTION", "QUOTATION", "NEGOTIATION", "WON", "LOST"];
    const stageCounts: Record<string, number> = {};
    const stageAmounts: Record<string, number> = {};
    opps.forEach((o) => {
      stageCounts[o.stage] = (stageCounts[o.stage] || 0) + 1;
      stageAmounts[o.stage] = (stageAmounts[o.stage] || 0) + Number(o.expectedAmount || 0);
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
    const maxAmount = Math.max(...Object.values(monthMap).map((m) => m.amount), 1);
    trendData.value = Object.entries(monthMap).map(([month, data]) => ({
      month: month.slice(5),
      count: data.count,
      amount: data.amount,
      barHeight: Math.max(Math.round((data.amount / maxAmount) * 100), 2),
    }));
    const yMax = Math.ceil(maxAmount / 100000) * 100000;
    trendYLabels.value = [formatMoney(yMax), formatMoney(Math.round(yMax / 2)), "¥0"];

    // Team leaderboard
    const teamMap: Record<string, { opps: number; won: number; amount: number }> = {};
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
      todos.push({ id: q.id, type: "报价审批", desc: `${q.code} ${q.customerName}`, time: "待处理", color: "orange", link: `/crm/quotes/${q.id}` })
    );
    const expiring = contracts.filter((c) => c.status === "ACTIVE" && c.endDate && c.endDate <= in30d);
    expiring.forEach((c) =>
      todos.push({ id: c.id, type: "合同到期", desc: `${c.projectName} ${c.code}`, time: c.endDate || "未知", color: "red", link: `/crm/contracts/${c.id}` })
    );
    overdueItems.forEach((r) =>
      todos.push({ id: r.id, type: "应收逾期", desc: `${r.code} ${r.customerName}`, time: formatMoney(r.outstandingAmount), color: "red", link: "/crm/receivables" })
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
    } catch { /* BI data is supplementary */ }
  } catch (error) {
    message.error(error instanceof Error ? error.message : "数据加载失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.dash-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.dash-header h2 { margin: 0; }
.dash-header .ant-btn { margin: 0; }
.metric-card .ant-card-body { padding: 18px 20px; }
.metric-sub { font-size: 12px; color: #8c8c8c; margin-top: 2px; }
.funnel-chart { display: flex; flex-direction: column; gap: 8px; padding: 4px 0; }
.funnel-row { display: flex; align-items: center; gap: 12px; }
.funnel-label { width: 80px; font-size: 13px; color: #333; text-align: right; flex-shrink: 0; }
.funnel-bar-wrap { flex: 1; height: 28px; background: #f5f5f5; border-radius: 4px; overflow: hidden; }
.funnel-bar { height: 100%; border-radius: 4px; display: flex; align-items: center; padding: 0 10px; transition: width 0.4s; min-width: 0; }
.funnel-bar-text { color: #fff; font-size: 12px; white-space: nowrap; }
.win-rate { margin-top: 12px; font-size: 14px; text-align: center; }
.todo-item { display: flex; align-items: flex-start; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.todo-item:last-child { border-bottom: none; }
.todo-body { flex: 1; min-width: 0; }
.todo-desc { font-size: 13px; line-height: 1.4; }
.todo-meta { font-size: 12px; color: #8c8c8c; margin-top: 2px; }
.trend-chart { display: flex; align-items: flex-end; gap: 8px; height: 200px; padding: 8px 0; }
.trech-y-axis { display: flex; flex-direction: column; justify-content: space-between; height: 100%; font-size: 11px; color: #8c8c8c; text-align: right; padding-right: 6px; }
.trend-bars { display: flex; align-items: flex-end; gap: 12px; flex: 1; height: 100%; }
.trend-col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; }
.trend-val { font-size: 10px; color: #8c8c8c; margin-bottom: 4px; white-space: nowrap; }
.trend-bar { width: 100%; max-width: 40px; background: linear-gradient(180deg, #1890ff, #69c0ff); border-radius: 4px 4px 0 0; transition: height 0.4s; min-height: 2px; }
.trend-label { font-size: 12px; color: #595959; margin-top: 6px; }
.rank-top { color: #f5222d; font-weight: 700; }
@media (max-width: 720px) {
  .trend-chart { height: 160px; }
}
</style>
