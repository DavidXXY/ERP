<template>
  <div class="collab-todo-tab">
    <a-alert
      class="todo-tip"
      type="info"
      show-icon
      message="统一处理业务待办与审批：可办理、转办、抄送、催办和设置期限；逾期任务由后台定时升级并发送站内消息。"
    />

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
          <span>金额敞口（元，税价随来源单据）</span>
          <strong>{{ formatCurrency(totalExposure) }}</strong>
        </div>
      </a-col>
    </a-row>

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
              <a-button type="link" size="small" @click.stop="openTodoDetail(record)"
                >详情</a-button
              >
              <a-button
                type="link"
                size="small"
                @click.stop="handleTodoAction(record)"
              >
                {{ isOfficeApprovalTodo(record) ? "处理审批" : "去办理" }}
              </a-button>
              <a-dropdown>
                <a-button type="link" size="small">协同操作</a-button>
                <template #overlay
                  ><a-menu
                    @click="({ key }: any) => openTodoAction(record, key)"
                  >
                    <a-menu-item key="COMPLETE">完成</a-menu-item
                    ><a-menu-item key="REOPEN">重新打开</a-menu-item>
                    <a-menu-item key="TRANSFER">转办</a-menu-item
                    ><a-menu-item key="CC">抄送</a-menu-item>
                    <a-menu-item key="REMIND">催办</a-menu-item
                    ><a-menu-item key="SET_DUE">设置期限</a-menu-item>
                  </a-menu></template
                >
              </a-dropdown>
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
            <a-descriptions-item label="金额（元，税价随来源单据）">{{
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
            <a-dropdown>
              <a-button>协同操作</a-button>
              <template #overlay
                ><a-menu
                  @click="({ key }: any) => openTodoAction(selectedTodo!, key)"
                >
                  <a-menu-item key="COMPLETE">完成</a-menu-item
                  ><a-menu-item key="REOPEN">重新打开</a-menu-item>
                  <a-menu-item key="TRANSFER">转办</a-menu-item
                  ><a-menu-item key="CC">抄送</a-menu-item>
                  <a-menu-item key="REMIND">催办</a-menu-item
                  ><a-menu-item key="SET_DUE">设置期限</a-menu-item>
                </a-menu></template
              >
            </a-dropdown>
            <a-button @click="detailOpen = false">关闭</a-button>
          </a-space>
        </a-space>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="todoOpen"
      title="协同待办操作"
      ok-text="确认"
      @ok="submitTodoAction"
    >
      <a-form layout="vertical">
        <a-form-item label="操作"
          ><a-tag>{{ actionText(todoForm.action) }}</a-tag></a-form-item
        >
        <a-form-item
          v-if="todoForm.action === 'TRANSFER'"
          label="转办给"
          required
          ><a-select
            v-model:value="todoForm.targetUserId"
            :options="userOptions"
        /></a-form-item>
        <a-form-item v-if="todoForm.action === 'CC'" label="抄送人" required
          ><a-select
            v-model:value="todoForm.ccUserIds"
            mode="multiple"
            :options="userOptions"
        /></a-form-item>
        <a-form-item
          v-if="todoForm.action === 'SET_DUE'"
          label="完成期限"
          required
          ><a-date-picker
            v-model:value="todoForm.dueDate"
            value-format="YYYY-MM-DD"
        /></a-form-item>
        <a-form-item label="处理意见"
          ><a-textarea v-model:value="todoForm.comment" :rows="3"
        /></a-form-item>
      </a-form>
    </a-modal>

    <ApprovalCenterView
      ref="approvalCenterRef"
      embedded
      drawer-only
      @changed="onApprovalChanged"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import {
  actOnCollaborationTodo,
  getCollaborationTodos,
} from "@/api/collaboration";
import { mapCanonicalTodo } from "@/utils/collaboration-todo";
import ApprovalCenterView from "@/views/office/ApprovalCenterView.vue";

withDefaults(
  defineProps<{ userOptions?: Array<{ label: string; value: string }> }>(),
  { userOptions: () => [] },
);
const emit = defineEmits<{ changed: [] }>();

type Priority = "HIGH" | "MEDIUM" | "LOW";
type TodoStatus = "OPEN" | "PROCESSING" | "OVERDUE" | "DONE";

type BusinessTodo = {
  key: string;
  type: string;
  id: string;
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

const todoOpen = ref(false);
const todoRecord = ref<BusinessTodo | null>(null);
const todoForm = reactive<any>({
  action: "",
  targetUserId: undefined,
  ccUserIds: [],
  dueDate: "",
  comment: "",
});

const columns = [
  { title: "事项", key: "title", width: 300 },
  { title: "模块", key: "module", width: 120 },
  { title: "优先级", key: "priority", width: 100 },
  { title: "状态", key: "status", width: 100 },
  { title: "责任人", dataIndex: "owner", key: "owner", width: 120 },
  { title: "金额（元，税价随来源单据）", key: "amount", width: 230 },
  { title: "到期/时间", key: "dueDate", width: 130 },
  { title: "操作", key: "action", width: 230 },
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
    console.warn("[collaboration-todos] canonical todo load failed", error);
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

function openTodoAction(record: BusinessTodo, action: string) {
  todoRecord.value = record;
  Object.assign(todoForm, {
    action,
    targetUserId: undefined,
    ccUserIds: [],
    dueDate: "",
    comment: "",
  });
  todoOpen.value = true;
}

async function submitTodoAction() {
  const record = todoRecord.value;
  if (!record) return;
  try {
    await actOnCollaborationTodo(record.type, record.id, todoForm);
    todoOpen.value = false;
    message.success("待办状态已更新");
    await loadData();
    emit("changed");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "待办操作失败");
  }
}

function onApprovalChanged() {
  void loadData();
  emit("changed");
}

function actionText(v: string) {
  return (
    ({
      COMPLETE: "完成",
      REOPEN: "重新打开",
      TRANSFER: "转办",
      CC: "抄送",
      REMIND: "催办",
      SET_DUE: "设置期限",
    } as any)[v] || v
  );
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
.collab-todo-tab {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.todo-tip {
  margin: 0;
}

.metric-grid {
  margin-top: 0;
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
  border-radius: 8px;
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

.todo-table-card {
  border-radius: 8px;
}

.todo-table-card :deep(.ant-table-tbody > tr) {
  cursor: pointer;
}
</style>
