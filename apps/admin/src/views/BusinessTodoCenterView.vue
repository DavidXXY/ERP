<template>
  <div class="business-todo-page">
    <a-card :bordered="false" class="todo-hero">
      <div class="hero-main">
        <div>
          <a-typography-title :level="3">业务待办中心</a-typography-title>
          <a-typography-text type="secondary">
            统一处理业务待办和审批事项；消息中心只保留通知阅读。
          </a-typography-text>
        </div>
        <a-space>
          <a-button @click="go('/office/notifications')">消息中心</a-button>
          <a-button @click="resetFilters">重置筛选</a-button>
          <a-button type="primary" :loading="loading" @click="loadData"
            >刷新待办</a-button
          >
        </a-space>
      </div>

      <a-row :gutter="[16, 16]" class="metric-grid">
        <a-col :xs="12" :lg="6">
          <div class="metric-card">
            <span>全部待办</span>
            <strong>{{ filteredTodos.length }}</strong>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="metric-card high">
            <span>高优先</span>
            <strong>{{ highCount }}</strong>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="metric-card overdue">
            <span>已超时</span>
            <strong>{{ overdueCount }}</strong>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="metric-card amount">
            <span>金额敞口</span>
            <strong>{{ formatCurrency(totalExposure) }}</strong>
          </div>
        </a-col>
      </a-row>
    </a-card>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="6" v-for="card in commandCards" :key="card.key">
        <a-card :bordered="false" class="command-card">
          <div class="command-title">{{ card.title }}</div>
          <div class="command-count">{{ card.count }}</div>
          <a-typography-text type="secondary">{{
            card.description
          }}</a-typography-text>
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="todo-table-card">
      <template #title>统一待办队列</template>
      <template #extra>
        <a-space wrap>
          <a-input-search
            v-model:value="keyword"
            allow-clear
            placeholder="搜索客户、项目、单号、说明"
            style="width: 260px"
          />
          <a-select v-model:value="moduleFilter" style="width: 150px">
            <a-select-option value="ALL">全部模块</a-select-option>
            <a-select-option
              v-for="item in moduleOptions"
              :key="item.value"
              :value="item.value"
              >{{ item.label }}</a-select-option
            >
          </a-select>
          <a-select v-model:value="priorityFilter" style="width: 130px">
            <a-select-option value="ALL">全部优先级</a-select-option>
            <a-select-option value="HIGH">高优先</a-select-option>
            <a-select-option value="MEDIUM">中优先</a-select-option>
            <a-select-option value="LOW">低优先</a-select-option>
          </a-select>
          <a-select v-model:value="statusFilter" style="width: 130px">
            <a-select-option value="ALL">全部状态</a-select-option>
            <a-select-option value="OVERDUE">已超时</a-select-option>
            <a-select-option value="OPEN">待处理</a-select-option>
            <a-select-option value="PROCESSING">处理中</a-select-option>
            <a-select-option value="DONE">已处理</a-select-option>
          </a-select>
        </a-space>
      </template>

      <a-alert
        v-if="loadErrors.length"
        type="warning"
        show-icon
        class="load-alert"
        :message="`部分模块加载失败：${loadErrors.join('、')}`"
      />

      <a-table
        row-key="key"
        :columns="columns"
        :data-source="filteredTodos"
        :loading="loading"
        :pagination="{ pageSize: 12, showSizeChanger: true }"
        :custom-row="todoRowProps"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <div class="todo-title">{{ record.title }}</div>
            <div class="todo-subject">{{ record.subject }}</div>
          </template>
          <template v-else-if="column.key === 'module'">
            <a-tag>{{ record.moduleName }}</a-tag>
          </template>
          <template v-else-if="column.key === 'priority'">
            <a-tag :color="priorityColor(record.priority)">{{
              priorityLabel(record.priority)
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{
              statusLabel(record.status)
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ record.amount ? formatCurrency(record.amount) : "-" }}
          </template>
          <template v-else-if="column.key === 'dueDate'">
            {{ record.dueDate ? formatDate(record.dueDate) : "-" }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <span>{{ record.action }}</span>
              <a-button
                type="link"
                size="small"
                @click.stop="openTodoDetail(record)"
              >
                查看详情
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="待办详情" width="680px">
      <template v-if="selectedTodo">
        <a-space direction="vertical" size="middle" style="width: 100%">
          <a-descriptions bordered size="small" :column="2">
            <a-descriptions-item label="事项" :span="2">{{
              selectedTodo.title
            }}</a-descriptions-item>
            <a-descriptions-item label="说明" :span="2">{{
              selectedTodo.subject || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="模块">
              <a-tag>{{ selectedTodo.moduleName }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="优先级">
              <a-tag :color="priorityColor(selectedTodo.priority)">{{
                priorityLabel(selectedTodo.priority)
              }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="statusColor(selectedTodo.status)">{{
                statusLabel(selectedTodo.status)
              }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="责任人">{{
              selectedTodo.owner || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="金额">{{
              selectedTodo.amount ? formatCurrency(selectedTodo.amount) : "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="到期/时间">{{
              selectedTodo.dueDate ? formatDate(selectedTodo.dueDate) : "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="推荐动作" :span="2">{{
              selectedTodo.action
            }}</a-descriptions-item>
          </a-descriptions>

          <a-space wrap>
            <a-button type="primary" @click="handleTodoAction(selectedTodo)">
              {{
                isOfficeApprovalTodo(selectedTodo)
                  ? "打开审批详情"
                  : "进入业务页面"
              }}
            </a-button>
            <a-button @click="detailOpen = false">关闭</a-button>
          </a-space>
        </a-space>
      </template>
    </a-drawer>

    <ApprovalCenterView
      ref="approvalCenterRef"
      embedded
      drawer-only
      @changed="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { getCollaborationTodos } from "@/api/collaboration";
import { mapCanonicalTodo } from "@/utils/collaboration-todo";
import ApprovalCenterView from "@/views/office/ApprovalCenterView.vue";

type Priority = "HIGH" | "MEDIUM" | "LOW";
type TodoStatus = "OPEN" | "PROCESSING" | "OVERDUE" | "DONE";

type BusinessTodo = {
  key: string;
  module: string;
  moduleName: string;
  title: string;
  subject: string;
  priority: Priority;
  status: TodoStatus;
  owner?: string;
  amount?: number;
  dueDate?: string;
  route: string;
  action: string;
  entityId?: string;
};

const router = useRouter();
const loading = ref(false);
const approvalCenterRef = ref<InstanceType<typeof ApprovalCenterView>>();
const todos = ref<BusinessTodo[]>([]);
const selectedTodo = ref<BusinessTodo | null>(null);
const detailOpen = ref(false);
const loadErrors = ref<string[]>([]);
const keyword = ref("");
const moduleFilter = ref("ALL");
const priorityFilter = ref("ALL");
const statusFilter = ref("ALL");

const columns = [
  { title: "事项", key: "title", width: 300 },
  { title: "模块", key: "module", width: 120 },
  { title: "优先级", key: "priority", width: 100 },
  { title: "状态", key: "status", width: 100 },
  { title: "责任人", dataIndex: "owner", key: "owner", width: 120 },
  { title: "金额", key: "amount", width: 130 },
  { title: "到期/时间", key: "dueDate", width: 130 },
  { title: "推荐动作", key: "action", width: 210 },
];

const moduleOptions = computed(() => {
  const map = new Map<string, string>();
  todos.value.forEach((item) => {
    const value = normalizeModuleCode(item.module);
    if (!map.has(value)) map.set(value, item.moduleName);
  });
  return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
});

const filteredTodos = computed(() => {
  const text = keyword.value.trim().toLowerCase();
  return todos.value.filter((item) => {
    if (
      moduleFilter.value !== "ALL" &&
      normalizeModuleCode(item.module) !== moduleFilter.value
    )
      return false;
    if (
      priorityFilter.value !== "ALL" &&
      item.priority !== priorityFilter.value
    )
      return false;
    if (statusFilter.value !== "ALL" && item.status !== statusFilter.value)
      return false;
    if (!text) return true;
    return [item.title, item.subject, item.owner, item.action, item.moduleName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(text));
  });
});

function normalizeModuleCode(value?: string) {
  return (value || "").trim().toUpperCase();
}

const highCount = computed(
  () => filteredTodos.value.filter((item) => item.priority === "HIGH").length,
);
const overdueCount = computed(
  () => filteredTodos.value.filter((item) => item.status === "OVERDUE").length,
);
const totalExposure = computed(() =>
  filteredTodos.value.reduce((sum, item) => sum + (item.amount || 0), 0),
);

const commandCards = computed(() => [
  {
    key: "unassigned",
    title: "待认领/责任待定",
    count: filteredTodos.value.filter(
      (item) => !item.owner || item.owner === "待认领",
    ).length,
    description: "优先补齐责任人，避免闭环断点。",
  },
  {
    key: "today",
    title: "7日内到期",
    count: filteredTodos.value.filter(
      (item) => daysUntil(item.dueDate) >= 0 && daysUntil(item.dueDate) <= 7,
    ).length,
    description: "提前处理审批、回款、交付和续约节点。",
  },
  {
    key: "overdue",
    title: "超时阻塞",
    count: overdueCount.value,
    description: "需要业务负责人或财务/采购协同推进。",
  },
  {
    key: "amount",
    title: "大额事项",
    count: filteredTodos.value.filter((item) => (item.amount || 0) >= 100000)
      .length,
    description: "优先处理影响现金流和项目利润的事项。",
  },
]);

async function loadData() {
  loading.value = true;
  loadErrors.value = [];
  try {
    todos.value = (await getCollaborationTodos())
      .map(mapCanonicalTodo)
      .sort(sortTodos);
    if (!todos.value.length) message.success("当前没有待处理事项");
  } catch (error) {
    console.warn("[business-todos] canonical todo load failed", error);
    loadErrors.value = ["统一待办"];
    todos.value = [];
  } finally {
    loading.value = false;
  }
}

function sortTodos(a: BusinessTodo, b: BusinessTodo) {
  const statusRank = { OVERDUE: 0, OPEN: 1, PROCESSING: 2, DONE: 3 };
  const priorityRank = { HIGH: 0, MEDIUM: 1, LOW: 2 };
  return (
    statusRank[a.status] - statusRank[b.status] ||
    priorityRank[a.priority] - priorityRank[b.priority] ||
    daysUntil(a.dueDate) - daysUntil(b.dueDate)
  );
}

function daysUntil(value?: string) {
  if (!value) return Number.POSITIVE_INFINITY;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return Number.POSITIVE_INFINITY;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  date.setHours(0, 0, 0, 0);
  return Math.ceil((date.getTime() - today.getTime()) / 86400000);
}

function resetFilters() {
  keyword.value = "";
  moduleFilter.value = "ALL";
  priorityFilter.value = "ALL";
  statusFilter.value = "ALL";
}

function go(path: string) {
  router.push(path);
}

function todoRowProps(record: BusinessTodo) {
  return {
    onClick: () => openTodoDetail(record),
  };
}

function openTodoDetail(record: BusinessTodo) {
  selectedTodo.value = record;
  detailOpen.value = true;
}

function isOfficeApprovalTodo(record: BusinessTodo) {
  return (
    normalizeModuleCode(record.module) === "OFFICE" &&
    (record.route.includes("/office/approvals") ||
      record.route.includes("tab=approvals"))
  );
}

function handleTodoAction(record: BusinessTodo) {
  if (isOfficeApprovalTodo(record)) {
    if (record.entityId) {
      detailOpen.value = false;
      approvalCenterRef.value?.openApprovalById(record.entityId);
      return;
    }
    message.warning("缺少审批ID，无法打开详情");
    return;
  }
  detailOpen.value = false;
  router.push(record.route);
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(value);
}

function formatDate(value: string) {
  return value.slice(0, 10);
}

function priorityColor(value: Priority) {
  return value === "HIGH" ? "red" : value === "MEDIUM" ? "orange" : "green";
}

function priorityLabel(value: Priority) {
  return { HIGH: "高", MEDIUM: "中", LOW: "低" }[value];
}

function statusColor(value: TodoStatus) {
  return value === "OVERDUE"
    ? "red"
    : value === "PROCESSING"
      ? "blue"
      : value === "DONE"
        ? "green"
        : "gold";
}

function statusLabel(value: TodoStatus) {
  return {
    OVERDUE: "已超时",
    PROCESSING: "处理中",
    OPEN: "待处理",
    DONE: "已处理",
  }[value];
}

onMounted(loadData);
</script>

<style scoped>
.business-todo-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.todo-hero,
.todo-table-card,
.command-card {
  border-radius: 8px;
}

.hero-main {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.metric-grid {
  margin-top: 20px;
}

.metric-card {
  min-height: 96px;
  padding: 16px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-card span,
.command-title {
  color: #5f6b7a;
  font-size: 13px;
}

.metric-card strong,
.command-count {
  color: #172033;
  font-size: 28px;
  line-height: 1.1;
}

.metric-card.high {
  background: #fff7f7;
  border-color: #ffd6d6;
}

.metric-card.overdue {
  background: #fff8ed;
  border-color: #ffd8a8;
}

.metric-card.amount {
  background: #f2fbf8;
  border-color: #bcebdc;
}

.command-card {
  min-height: 130px;
}

.command-count {
  margin: 10px 0 8px;
  font-weight: 700;
}

.load-alert {
  margin-bottom: 16px;
}

.todo-title {
  font-weight: 600;
  color: #172033;
}

.todo-subject {
  margin-top: 4px;
  color: #6b778c;
  font-size: 12px;
}

.todo-table-card :deep(.ant-table-tbody > tr) {
  cursor: pointer;
}

@media (max-width: 768px) {
  .hero-main {
    flex-direction: column;
  }
}
</style>
