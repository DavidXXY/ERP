<template>
  <div class="self-page">
    <a-row :gutter="16" class="welcome-row">
      <a-col :span="24">
        <a-card class="welcome-card">
          <div class="welcome-content">
            <div>
              <h2>{{ greeting }}，{{ employee?.name || '加载中' }}</h2>
              <p class="welcome-sub" v-if="employee">{{ employee.position || '' }} · {{ employee.organizationName || '未分配组织' }}</p>
            </div>
            <a-space>
              <a-button type="primary" size="large" @click="$router.push('/self/leaves/new')">
                <template #icon><PlusOutlined /></template>提交请假
              </a-button>
            </a-space>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- Todo Section -->
    <a-row :gutter="16" class="todo-row">
      <a-col :span="24">
        <a-card title="待办事项" class="todo-card">
          <a-list :data-source="todos" size="small" v-if="todos.length">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a-space>
                      <a-tag v-if="item.priority === 'HIGH'" color="red">紧急</a-tag>
                      <a-tag v-else-if="item.priority === 'MEDIUM'" color="orange">提醒</a-tag>
                      <a-tag v-else color="blue">待办</a-tag>
                      <span>{{ item.title }}</span>
                    </a-space>
                  </template>
                  <template #description>{{ item.description }}</template>
                </a-list-item-meta>
                <template #actions>
                  <a-button type="link" size="small" @click="$router.push(item.link)">查看</a-button>
                </template>
              </a-list-item>
            </template>
          </a-list>
          <a-empty v-else description="暂无待办事项，一切顺利！" />
        </a-card>
      </a-col>
    </a-row>
    
    <a-row :gutter="16" class="stats-row">
      <a-col :xs="12" :sm="6">
        <a-card><a-statistic title="总人数" :value="analytics.totalEmployees" /></a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card><a-statistic title="在职人数" :value="analytics.activeEmployees" :value-style="{ color: '#3f8600' }" /></a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card><a-statistic title="本月新增" :value="analytics.newThisMonth" :value-style="{ color: '#1890ff' }" /></a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card><a-statistic title="待审批请假" :value="analytics.leavePendingCount" :value-style="{ color: analytics.leavePendingCount > 0 ? '#faad14' : undefined }" /></a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" class="balance-row">
      <a-col :xs="24" :lg="16">
        <a-card title="我的请假额度">
          <template #extra><a-button type="link" @click="$router.push('/self/balances')">查看全部</a-button></template>
          <a-row :gutter="16" v-if="balances.length">
            <a-col :xs="12" v-for="b in balances.slice(0, 4)" :key="b.id">
              <div class="balance-item">
                <div class="balance-header"><span>{{ typeLabel(b.leaveType) }}</span><span class="balance-num">{{ b.remainingDays.toFixed(1) }} / {{ b.totalDays }}天</span></div>
                <a-progress :percent="calcPct(b)" :stroke-color="b.remainingDays < 2 ? '#ff4d4f' : b.remainingDays < 5 ? '#faad14' : '#52c41a'" :format="() => ''" />
              </div>
            </a-col>
          </a-row>
          <a-empty v-else description="暂无额度数据" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="快捷操作" class="quick-actions">
          <a-space direction="vertical" style="width:100%">
            <a-button block @click="$router.push('/self/leaves/new')"><template #icon><PlusOutlined /></template>提交请假</a-button>
            <a-button block @click="$router.push('/self/leaves')"><template #icon><OrderedListOutlined /></template>我的请假</a-button>
            <a-button block @click="$router.push('/self/profile')"><template #icon><UserOutlined /></template>我的档案</a-button>
            <a-button block @click="$router.push('/self/balances')"><template #icon><BarChartOutlined /></template>我的额度</a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>

    <a-card title="最近请假记录" class="recent-leaves">
      <template #extra><a-button type="link" @click="$router.push('/self/leaves')">查看全部</a-button></template>
      <a-table :data-source="recentLeaves" :columns="leafCols" size="small" row-key="id" :pagination="{ pageSize: 5 }" :loading="loading">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'leaveType'"><a-tag :color="typeColor(record.leaveType)">{{ typeLabel(record.leaveType) }}</a-tag></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag></template>
          <template v-else-if="column.key === 'period'">{{ record.startDate }} ~ {{ record.endDate }}<br><small>共 {{ record.totalDays }} 天</small></template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { PlusOutlined, OrderedListOutlined, UserOutlined, BarChartOutlined } from "@ant-design/icons-vue";
import { useAuthStore } from "@/stores/auth";
import { getSelfProfile, getSelfLeaveBalances, getSelfLeaves, getSelfTodos, type LeaveBalanceRecord, type LeaveRecord, type TodoItem } from "@/api/hr";
import { getHrAnalytics, type HrAnalytics } from "@/api/hr";

const auth = useAuthStore();
const loading = ref(false);
const employee = ref<{ name: string; position?: string; organizationName?: string }>();
const balances = ref<LeaveBalanceRecord[]>([]);
const recentLeaves = ref<LeaveRecord[]>([]);
const todos = ref<TodoItem[]>([]);
const analytics = ref<HrAnalytics>({ totalEmployees: 0, activeEmployees: 0, leftEmployees: 0, newThisMonth: 0, leavePendingCount: 0, educationDistribution: [], statusDistribution: [], organizationDistribution: [], recentLifecycles: [] });

const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 6) return "夜深了"; if (h < 12) return "上午好"; if (h < 14) return "中午好"; if (h < 18) return "下午好"; return "晚上好";
});

function calcPct(b: LeaveBalanceRecord) { return Math.round((b.remainingDays / Math.max(b.totalDays, 1)) * 100); }
function typeColor(t: string) { const c: Record<string,string> = {ANNUAL:"blue",SICK:"red",PERSONAL:"default",MARRIAGE:"pink",MATERNITY:"purple",COMPENSATORY:"cyan"}; return c[t]||"default"; }
function typeLabel(t: string) { const l: Record<string,string> = {ANNUAL:"年假",SICK:"病假",PERSONAL:"事假",MARRIAGE:"婚假",MATERNITY:"产假",COMPENSATORY:"调休"}; return l[t]||t; }
function statusColor(s: string) { return ({PENDING:"orange",APPROVED:"green",REJECTED:"red"})[s]||"default"; }
function statusLabel(s: string) { return ({PENDING:"待审批",APPROVED:"已通过",REJECTED:"已驳回"})[s]||s; }

const leafCols = [
  { title: "类型", key: "leaveType", width: 80 },
  { title: "时间", key: "period", width: 240 },
  { title: "原因", dataIndex: "reason", ellipsis: true },
  { title: "状态", key: "status", width: 90 },
  { title: "审批人", dataIndex: "approvedBy", width: 100 },
];

async function loadData() {
  loading.value = true;
  try {
    const [profile, bal, leaves, hrA, todosData] = await Promise.all([
      getSelfProfile().catch(() => null),
      getSelfLeaveBalances().catch(() => []),
      getSelfLeaves().catch(() => []),
      getHrAnalytics().catch(() => analytics.value),
      getSelfTodos().catch(() => []),
    ]);
    todos.value = todosData || [];
    if (profile) employee.value = { name: profile.employee.name, position: profile.employee.position, organizationName: profile.employee.organizationName };
    balances.value = bal;
    recentLeaves.value = leaves.slice(0, 10);
    analytics.value = hrA;
  } catch {}
  finally { loading.value = false; }
}
onMounted(loadData);
</script>

<style scoped>
.self-page { max-width: 1200px; }
.welcome-card { background: linear-gradient(135deg, #185f52 0%, #237867 100%); border-radius: 10px; }
.welcome-content { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; }
.welcome-content h2 { margin: 0; color: #fff; font-size: 22px; }
.welcome-sub { color: rgba(255,255,255,0.8); margin: 4px 0 0; font-size: 14px; }
.stats-row, .balance-row, .recent-leaves { margin-top: 16px; }
.balance-item { margin-bottom: 12px; }
.balance-header { display: flex; justify-content: space-between; margin-bottom: 4px; font-size: 13px; color: #17212b; }
.balance-num { color: #65717e; }
.quick-actions :deep(.ant-btn) { text-align: left; }
.todo-row { margin-top: 16px; }
.todo-card :deep(.ant-list-item) { padding: 8px 0; }
</style>
