<template>
<div class="page-stack">
  <h2 style="margin-bottom: 16px">CRM 仪表盘</h2>
  <a-row :gutter="[16, 16]">
    <a-col :xs="12" :md="6"><a-card><a-statistic title="客户总数" :value="stats.customerCount" suffix="家" :loading="loading" /></a-card></a-col>
    <a-col :xs="12" :md="6"><a-card><a-statistic title="进行中商机" :value="stats.activeOpportunityCount" suffix="个" :loading="loading" /></a-card></a-col>
    <a-col :xs="12" :md="6"><a-card><a-statistic title="待审批报价" :value="stats.pendingQuoteCount" suffix="份" :loading="loading" :value-style="stats.pendingStyle" /></a-card></a-col>
    <a-col :xs="12" :md="6"><a-card><a-statistic title="到期合同" :value="stats.expiringContractCount" suffix="份" :loading="loading" :value-style="stats.expiringStyle" /></a-card></a-col>
  </a-row>
  <a-row :gutter="[16, 16]" style="margin-top: 16px">
    <a-col :xs="24" :md="14"><a-card title="商机阶段分布"><a-table :data-source="stageData" :columns="stageColumns" :pagination="false" :loading="loading" size="small"><template #bodyCell="{ column, record }"><template v-if="column.key === 'bar'"><a-progress :percent="record.percent" :stroke-color="record.color" :show-info="false" /></template></template></a-table></a-card></a-col>
    <a-col :xs="24" :md="10"><a-card title="待办事项"><a-table :data-source="pendingList" :columns="todoColumns" :pagination="false" :loading="loading" size="small"><template #bodyCell="{ column, record }"><template v-if="column.key === 'type'"><a-tag :color="record.color">{{ record.type }}</a-tag></template><template v-else-if="column.key === 'action'"><a-button type="link" size="small" @click="router.push(record.link)">处理</a-button></template></template></a-table></a-card></a-col>
  </a-row>
</div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { useRouter } from "vue-router";
import { listCustomers, listOpportunities, listQuotes, listContracts, listReceivables } from "@/api/crm";

const router = useRouter();
const loading = ref(true);
const stats = reactive({
  customerCount: 0, activeOpportunityCount: 0, pendingQuoteCount: 0, expiringContractCount: 0,
  pendingStyle: { color: '#faad14' }, expiringStyle: { color: '#52c41a' }
});
const stageData = ref<any[]>([]);
const pendingList = ref<any[]>([]);
const stageColumns = [{ title: "阶段", key: "stage", width: 120 }, { title: "数量", key: "count", width: 60 }, { title: "金额", key: "amount", width: 130 }, { title: "", key: "bar", width: 160 }];
const todoColumns = [{ title: "类型", key: "type", width: 100 }, { title: "内容", dataIndex: "desc", width: 200 }, { title: "操作", key: "action", width: 70 }];

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [opps, quotes, contracts, receivables] = await Promise.all([
      listOpportunities(), listQuotes(), listContracts(), listReceivables()
    ]);
    const active = opps.filter((o) => o.stage !== "WON" && o.stage !== "LOST");
    const pendingQuotes = quotes.filter((q) => q.status === "PENDING_APPROVAL");
    const now = new Date();
    const in30d = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10);
    const expiring = contracts.filter((c) => c.status === "ACTIVE" && c.endDate && c.endDate <= in30d);
    const overdue = receivables.filter((r) => r.status !== "SETTLED" && r.dueDate && r.dueDate < now.toISOString().slice(0, 10));

    stats.activeOpportunityCount = active.length;
    stats.pendingQuoteCount = pendingQuotes.length;
    stats.expiringContractCount = expiring.length;
    stats.pendingStyle = pendingQuotes.length > 0 ? { color: '#faad14' } : { color: '#52c41a' };
    stats.expiringStyle = expiring.length > 0 ? { color: '#ff4d4f' } : { color: '#52c41a' };

    const stageLabels: Record<string, string> = { LEAD: "初步接触", QUALIFIED: "需求确认", SOLUTION: "方案制定", QUOTATION: "报价阶段", NEGOTIATION: "商务谈判", WON: "赢单", LOST: "丢单" };
    const stageColors: Record<string, string> = { LEAD: "#1890ff", QUALIFIED: "#52c41a", SOLUTION: "#13c2c2", QUOTATION: "#faad14", NEGOTIATION: "#f5222d", WON: "#52c41a", LOST: "#8c8c8c" };
    const stageOrder = ["LEAD", "QUALIFIED", "SOLUTION", "QUOTATION", "NEGOTIATION", "WON", "LOST"];
    const stageCounts: Record<string, number> = {};
    opps.forEach((o) => { stageCounts[o.stage] = (stageCounts[o.stage] || 0) + 1; });
    const maxCount = Math.max(...Object.values(stageCounts), 1);
    stageData.value = stageOrder
      .filter((s) => stageCounts[s])
      .map((s) => ({ stage: stageLabels[s] || s, count: stageCounts[s], percent: Math.round((stageCounts[s] || 0) / maxCount * 100), color: stageColors[s] || "#8c8c8c" }))
      .reverse();

    const todos: any[] = [];
    pendingQuotes.forEach((q) => todos.push({ type: "报价审批", desc: q.code + " " + q.customerName, color: "orange", link: "/crm/quotes/" + q.id }));
    expiring.forEach((c) => todos.push({ type: "合同到期", desc: c.projectName + " " + c.code, color: "red", link: "/crm/contracts/" + c.id }));
    overdue.forEach((r) => todos.push({ type: "应收逾期", desc: r.code + " " + r.customerName, color: "red", link: "" }));
    pendingList.value = todos.slice(0, 20);

    try { stats.customerCount = (await listCustomers()).length; } catch { /* optional */ }
  } catch (error) {
    message.error(error instanceof Error ? error.message : "数据加载失败");
  } finally { loading.value = false; }
}
</script>