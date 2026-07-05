<template>
  <div class="self-page">
    <a-page-header title="我的请假记录" @back="$router.back()">
      <template #extra>
        <a-button type="primary" @click="$router.push('/self/leaves/new')"><PlusOutlined />提交请假</a-button>
      </template>
    </a-page-header>

    <a-card>
      <a-tabs v-model:active-key="statusFilter">
        <a-tab-pane key="all" tab="全部" />
        <a-tab-pane key="PENDING" tab="待审批" />
        <a-tab-pane key="APPROVED" tab="已通过" />
        <a-tab-pane key="REJECTED" tab="已驳回" />
      </a-tabs>
      <a-table :data-source="filteredData" :columns="columns" row-key="id" size="middle" :pagination="{ pageSize: 10 }" :loading="loading">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'leaveType'"><a-tag :color="typeColor(record.leaveType)">{{ typeLabel(record.leaveType) }}</a-tag></template>
          <template v-else-if="column.key === 'period'">{{ record.startDate }} ~ {{ record.endDate }}<br><small>共 {{ record.totalDays }} 天</small></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag></template>
          <template v-else-if="column.key === 'reason'">{{ record.reason || '-' }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { getSelfLeaves, type LeaveRecord } from "@/api/hr";
import { message } from "ant-design-vue";

const loading = ref(false);
const data = ref<LeaveRecord[]>([]);
const statusFilter = ref("all");

const filteredData = computed(() => statusFilter.value === "all" ? data.value : data.value.filter(d => d.status === statusFilter.value));

function typeColor(t: string) { const c: Record<string,string> = {ANNUAL:"blue",SICK:"red",PERSONAL:"default",MARRIAGE:"pink",MATERNITY:"purple",COMPENSATORY:"cyan",OTHER:"default"}; return c[t]||"default"; }
function typeLabel(t: string) { const l: Record<string,string> = {ANNUAL:"年假",SICK:"病假",PERSONAL:"事假",MARRIAGE:"婚假",MATERNITY:"产假",COMPENSATORY:"调休",OTHER:"其他"}; return l[t]||t; }
function statusColor(s: string) { return ({PENDING:"orange",APPROVED:"green",REJECTED:"red"})[s]||"default"; }
function statusLabel(s: string) { return ({PENDING:"待审批",APPROVED:"已通过",REJECTED:"已驳回"})[s]||s; }

const columns = [
  { title: "类型", key: "leaveType", width: 80 },
  { title: "时间", key: "period", width: 240 },
  { title: "原因", key: "reason", ellipsis: true },
  { title: "状态", key: "status", width: 90 },
  { title: "审批意见", dataIndex: "approvalRemark", ellipsis: true },
  { title: "审批人", dataIndex: "approvedBy", width: 100 },
];

onMounted(async () => {
  loading.value = true;
  try { data.value = await getSelfLeaves(); }
  catch (error: any) { message.error(error.message || "加载失败"); }
  finally { loading.value = false; }
});
</script>

<style scoped>
.self-page { max-width: 1000px; }
</style>
