<template>
  <div class="page-stack">
    <a-card title="人力概况">
      <a-row :gutter="16" class="metric-row">
        <a-col :xs="12" :lg="6">
          <a-card><a-statistic title="总人数" :value="analytics.totalEmployees" /></a-card>
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-card><a-statistic title="在职人数" :value="analytics.activeEmployees" :value-style="{ color: '#3f8600' }" /></a-card>
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-card><a-statistic title="本月新增" :value="analytics.newThisMonth" :value-style="{ color: '#1890ff' }" /></a-card>
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-card><a-statistic title="待审批请假" :value="analytics.leavePendingCount" :value-style="{ color: analytics.leavePendingCount > 0 ? '#faad14' : undefined }" /></a-card>
        </a-col>
      </a-row>
    </a-card>

    <a-row :gutter="12">
      <a-col :xs="24" :lg="8">
        <a-card title="人员状态分布" class="analytics-card">
          <a-table size="small" :data-source="analytics.statusDistribution" :columns="distColumns" :pagination="false" row-key="name" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="学历分布" class="analytics-card">
          <a-table size="small" :data-source="analytics.educationDistribution" :columns="distColumns" :pagination="false" row-key="name" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="组织人数 TOP10" class="analytics-card">
          <a-table size="small" :data-source="topOrganizations" :columns="distColumns" :pagination="false" row-key="name" />
        </a-card>
      </a-col>
    </a-row>

    <a-card title="近期人员变动" class="analytics-card">
      <a-table size="small" :data-source="analytics.recentLifecycles" :columns="lcColumns" :pagination="false" row-key="(item: any) => item.date + item.employeeName">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="typeColor(record.type)">{{ record.type }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { getHrAnalytics, type HrAnalytics } from "@/api/hr";

const analytics = ref<HrAnalytics>({
  totalEmployees: 0, activeEmployees: 0, leftEmployees: 0,
  newThisMonth: 0, leavePendingCount: 0,
  educationDistribution: [], statusDistribution: [],
  organizationDistribution: [], recentLifecycles: [],
});

const topOrganizations = computed(() =>
  [...analytics.value.organizationDistribution].sort((a, b) => b.count - a.count).slice(0, 10)
);

const distColumns = [
  { title: "分类", dataIndex: "name" },
  { title: "人数", dataIndex: "count", width: 80 },
];

const lcColumns = [
  { title: "日期", dataIndex: "date", width: 120 },
  { title: "员工", dataIndex: "employeeName", width: 120 },
  { title: "类型", key: "type", width: 80 },
  { title: "变动内容", dataIndex: "detail" },
];

function typeColor(t: string) {
  return ({ 入职: "green", 调岗: "blue", 离职: "red" } as Record<string, string>)[t] || "default";
}

async function loadData() {
  try {
    analytics.value = await getHrAnalytics();
  } catch (error: any) {
    message.error(error.message || "加载失败");
  }
}

onMounted(loadData);
</script>

<style scoped>
.analytics-card { margin-top: 12px; }
</style>
