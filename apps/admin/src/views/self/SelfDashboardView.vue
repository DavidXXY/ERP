<template>
  <div class="page-stack self-page">
    <a-card :bordered="false" class="self-hero-card">
      <div class="self-hero-main">
        <div>
          <a-typography-title :level="3"
            >{{ greeting }}，{{
              employee?.name || "加载中"
            }}</a-typography-title
          >
          <a-typography-text>{{
            employee
              ? `${employee.position || "未设置岗位"} · ${employee.organizationName || "未分配组织"}`
              : "正在同步员工档案"
          }}</a-typography-text>
        </div>
        <a-space wrap class="self-hero-actions">
          <a-button @click="$router.push('/workbench/todos?tab=approvals')"
            ><template #icon><CheckSquareOutlined /></template
            >业务待办</a-button
          >
          <a-button type="primary" @click="$router.push('/self/leaves/new')"
            ><template #icon><PlusOutlined /></template>提交请假</a-button
          >
        </a-space>
      </div>

      <a-row :gutter="[16, 16]" class="self-metric-grid">
        <a-col :xs="12" :lg="6"
          ><div class="self-metric-card">
            <span>待办事项</span><strong>{{ todos.length }}</strong>
          </div></a-col
        >
        <a-col :xs="12" :lg="6"
          ><div class="self-metric-card warn">
            <span>待办审批</span><strong>{{ approvalTodoCount }}</strong>
          </div></a-col
        >
        <a-col :xs="12" :lg="6"
          ><div class="self-metric-card">
            <span>本月新增</span><strong>{{ analytics.newThisMonth }}</strong>
          </div></a-col
        >
        <a-col :xs="12" :lg="6"
          ><div class="self-metric-card warn">
            <span>待审批请假</span
            ><strong>{{ analytics.leavePendingCount }}</strong>
          </div></a-col
        >
      </a-row>
    </a-card>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="15">
        <a-card title="待办事项" class="self-work-card">
          <template #extra>
            <a-button
              type="link"
              @click="$router.push('/workbench/todos?tab=approvals')"
              >业务待办 {{ approvalTodoCount }}</a-button
            >
          </template>
          <a-list :data-source="todos" size="small" v-if="todos.length">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a-space wrap>
                      <a-tag v-if="item.priority === 'HIGH'" color="red"
                        >紧急</a-tag
                      >
                      <a-tag
                        v-else-if="item.priority === 'MEDIUM'"
                        color="orange"
                        >提醒</a-tag
                      >
                      <a-tag v-else color="blue">待办</a-tag>
                      <span>{{ item.title }}</span>
                    </a-space>
                  </template>
                  <template #description>{{ item.description }}</template>
                </a-list-item-meta>
                <template #actions>
                  <a-button
                    type="link"
                    size="small"
                    @click="$router.push(item.link)"
                    >查看</a-button
                  >
                </template>
              </a-list-item>
            </template>
          </a-list>
          <a-empty v-else description="暂无待办事项，一切顺利！" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="9">
        <a-card title="快捷操作" class="quick-actions self-work-card">
          <a-row :gutter="[10, 10]">
            <a-col :xs="12"
              ><a-button block @click="$router.push('/self/leaves/new')"
                ><template #icon><PlusOutlined /></template>提交请假</a-button
              ></a-col
            >
            <a-col :xs="12"
              ><a-button
                block
                @click="$router.push('/workbench/todos?tab=approvals')"
                ><template #icon><CheckSquareOutlined /></template
                >业务待办</a-button
              ></a-col
            >
            <a-col :xs="12"
              ><a-button block @click="$router.push('/self/leaves')"
                ><template #icon><OrderedListOutlined /></template
                >我的请假</a-button
              ></a-col
            >
            <a-col :xs="12"
              ><a-button block @click="$router.push('/self/profile')"
                ><template #icon><UserOutlined /></template>我的档案</a-button
              ></a-col
            >
            <a-col :xs="24"
              ><a-button block @click="$router.push('/self/balances')"
                ><template #icon><BarChartOutlined /></template
                >我的额度</a-button
              ></a-col
            >
          </a-row>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="16">
        <a-card title="我的请假额度">
          <template #extra
            ><a-button type="link" @click="$router.push('/self/balances')"
              >查看全部</a-button
            ></template
          >
          <a-row :gutter="16" v-if="balances.length">
            <a-col
              :xs="24"
              :sm="12"
              v-for="b in balances.slice(0, 4)"
              :key="b.id"
            >
              <div class="balance-item">
                <div class="balance-header">
                  <span>{{ typeLabel(b.leaveType) }}</span
                  ><span class="balance-num"
                    >{{ b.remainingDays.toFixed(1) }} /
                    {{ b.totalDays }}天</span
                  >
                </div>
                <a-progress
                  :percent="calcPct(b)"
                  :stroke-color="
                    b.remainingDays < 2
                      ? '#ff4d4f'
                      : b.remainingDays < 5
                        ? '#faad14'
                        : '#52c41a'
                  "
                  :format="() => ''"
                />
              </div>
            </a-col>
          </a-row>
          <a-empty v-else description="暂无额度数据" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="员工概览">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="总人数">{{
              analytics.totalEmployees
            }}</a-descriptions-item>
            <a-descriptions-item label="在职人数">{{
              analytics.activeEmployees
            }}</a-descriptions-item>
            <a-descriptions-item label="当前组织">{{
              employee?.organizationName || "-"
            }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>

    <a-card title="最近请假记录" class="recent-leaves">
      <template #extra
        ><a-button type="link" @click="$router.push('/self/leaves')"
          >查看全部</a-button
        ></template
      >
      <a-table
        :data-source="recentLeaves"
        :columns="leafCols"
        size="small"
        row-key="id"
        :pagination="{ pageSize: 5 }"
        :loading="loading"
        :scroll="{ x: 760 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'leaveType'"
            ><a-tag :color="typeColor(record.leaveType)">{{
              typeLabel(record.leaveType)
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'status'"
            ><a-tag :color="statusColor(record.status)">{{
              statusLabel(record.status)
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'period'"
            >{{ record.startDate }} ~ {{ record.endDate }}<br /><small
              >共 {{ record.totalDays }} 天</small
            ></template
          >
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import {
  PlusOutlined,
  OrderedListOutlined,
  UserOutlined,
  BarChartOutlined,
  CheckSquareOutlined,
} from "@ant-design/icons-vue";
import { useAuthStore } from "@/stores/auth";
import {
  getSelfProfile,
  getSelfLeaveBalances,
  getSelfLeaves,
  getSelfTodos,
  type LeaveBalanceRecord,
  type LeaveRecord,
  type TodoItem,
} from "@/api/hr";
import { getHrAnalytics, type HrAnalytics } from "@/api/hr";

const auth = useAuthStore();
const loading = ref(false);
const employee = ref<{
  name: string;
  position?: string;
  organizationName?: string;
}>();
const balances = ref<LeaveBalanceRecord[]>([]);
const recentLeaves = ref<LeaveRecord[]>([]);
const todos = ref<TodoItem[]>([]);
const analytics = ref<HrAnalytics>({
  totalEmployees: 0,
  activeEmployees: 0,
  leftEmployees: 0,
  newThisMonth: 0,
  leavePendingCount: 0,
  educationDistribution: [],
  statusDistribution: [],
  organizationDistribution: [],
  recentLifecycles: [],
});

const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 6) return "夜深了";
  if (h < 12) return "上午好";
  if (h < 14) return "中午好";
  if (h < 18) return "下午好";
  return "晚上好";
});
const approvalTodoCount = computed(
  () => todos.value.filter((item) => item.type === "APPROVAL_PENDING").length,
);

function calcPct(b: LeaveBalanceRecord) {
  return Math.round((b.remainingDays / Math.max(b.totalDays, 1)) * 100);
}
function typeColor(t: string) {
  const c: Record<string, string> = {
    ANNUAL: "blue",
    SICK: "red",
    PERSONAL: "default",
    MARRIAGE: "pink",
    MATERNITY: "purple",
    COMPENSATORY: "cyan",
  };
  return c[t] || "default";
}
function typeLabel(t: string) {
  const l: Record<string, string> = {
    ANNUAL: "年假",
    SICK: "病假",
    PERSONAL: "事假",
    MARRIAGE: "婚假",
    MATERNITY: "产假",
    COMPENSATORY: "调休",
  };
  return l[t] || t;
}
function statusColor(s: string) {
  return (
    { PENDING: "orange", APPROVED: "green", REJECTED: "red" }[s] || "default"
  );
}
function statusLabel(s: string) {
  return { PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" }[s] || s;
}

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
    if (profile)
      employee.value = {
        name: profile.employee.name,
        position: profile.employee.position,
        organizationName: profile.employee.organizationName,
      };
    balances.value = bal;
    recentLeaves.value = leaves.slice(0, 10);
    analytics.value = hrA;
  } catch {
  } finally {
    loading.value = false;
  }
}
onMounted(loadData);
</script>

<style scoped>
.self-page {
  max-width: 1200px;
}
.self-hero-card {
  background: #f8fafc;
}
.self-hero-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}
.self-hero-main h3 {
  margin: 0 0 6px;
}
.self-hero-main :deep(.ant-typography-secondary),
.self-hero-main :deep(.ant-typography) {
  color: #64748b;
}
.self-hero-actions {
  flex: none;
}
.self-work-card {
  height: 100%;
}
.recent-leaves {
  margin-top: 0;
}
.balance-item {
  margin-bottom: 12px;
}
.balance-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 13px;
  color: #17212b;
}
.balance-num {
  color: #65717e;
}
.quick-actions :deep(.ant-btn) {
  text-align: left;
}
.todo-card :deep(.ant-list-item) {
  padding: 8px 0;
}

@media (max-width: 640px) {
  .self-hero-main {
    display: grid;
  }
  .self-hero-actions,
  .self-hero-actions :deep(.ant-space-item),
  .self-hero-actions :deep(.ant-btn) {
    width: 100%;
  }
  .todo-card :deep(.ant-list-item) {
    align-items: flex-start;
  }
  .todo-card :deep(.ant-list-item-action) {
    margin-left: 8px;
  }
  .balance-header {
    gap: 8px;
  }
}
</style>
