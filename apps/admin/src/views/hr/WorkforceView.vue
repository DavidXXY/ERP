<template>
  <div class="workforce-view">
    <div class="workforce-summary">
      <div><span>待排工单</span><strong>{{ unscheduledOrders.length }}</strong></div>
      <div><span>今日排班</span><strong>{{ todaySchedules }}</strong></div>
      <div><span>已签到</span><strong>{{ attendance.length }}</strong></div>
      <div><span>异常考勤</span><strong class="danger">{{ anomalies }}</strong></div>
    </div>

    <a-tabs v-model:active-key="tab">
      <a-tab-pane key="schedule" tab="现场排班">
        <div class="toolbar">
          <a-input-search v-model:value="keyword" allow-clear placeholder="搜索工单或负责人" style="width: 280px" />
          <a-button v-if="canManage" type="primary" @click="openSchedule">新增排班</a-button>
          <a-button :loading="loading" @click="load">刷新</a-button>
        </div>
        <a-table :columns="scheduleColumns" :data-source="filteredSchedules" :loading="loading" row-key="id" :pagination="{ pageSize: 10 }" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'order'"><strong>{{ record.orderCode }}</strong><div class="sub">{{ record.title }}</div></template>
            <template v-else-if="column.key === 'scheduledAt'">{{ formatTime(record.scheduledAt) }}</template>
            <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag></template>
            <template v-else-if="column.key === 'checkIn'">{{ record.checkInAt ? formatTime(record.checkInAt) : '未签到' }}<div v-if="record.checkInLocation" class="sub">{{ record.checkInLocation }}</div></template>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="attendance" tab="考勤记录">
        <a-table :columns="attendanceColumns" :data-source="attendance" :loading="loading" row-key="id" :pagination="{ pageSize: 10 }" :scroll="{ x: 820 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'checkInAt'">{{ formatTime(record.checkInAt) }}</template>
            <template v-else-if="column.key === 'checkOutAt'">{{ record.checkOutAt ? formatTime(record.checkOutAt) : '作业中' }}</template>
            <template v-else-if="column.key === 'result'"><a-tag :color="record.checkOutAt ? 'green' : 'gold'">{{ record.checkOutAt ? '完整' : '未签退' }}</a-tag></template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <a-modal v-model:open="scheduleOpen" title="安排现场任务" :confirm-loading="saving" @ok="saveSchedule">
      <a-form layout="vertical">
        <a-form-item label="待安排工单" required>
          <a-select v-model:value="scheduleForm.orderId" show-search option-filter-prop="label" :options="orderOptions" />
        </a-form-item>
        <a-form-item label="现场负责人" required>
          <a-select v-model:value="scheduleForm.engineerId" show-search option-filter-prop="label" :options="assigneeOptions" />
        </a-form-item>
        <a-form-item label="计划到场时间" required><a-date-picker v-model:value="scheduleForm.scheduledAt" show-time format="YYYY-MM-DD HH:mm" style="width: 100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { message } from "ant-design-vue";
import { useAuthStore } from "@/stores/auth";
import { createSchedule, listAssignees, listAttendance, listSchedules, listWorkOrders, type Assignee, type Attendance, type Schedule, type WorkOrder } from "@/api/maintenance";

const auth = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const tab = ref("schedule");
const keyword = ref("");
const schedules = ref<Schedule[]>([]);
const attendance = ref<Attendance[]>([]);
const orders = ref<WorkOrder[]>([]);
const assignees = ref<Assignee[]>([]);
const scheduleOpen = ref(false);
const scheduleForm = reactive<{ orderId?: string; engineerId?: string; scheduledAt?: Dayjs }>({});
const canManage = computed(() => auth.can("maintenance:order:manage"));
const unscheduledOrders = computed(() => orders.value.filter((item) => !item.plannedDate || !item.assigneeId));
const todaySchedules = computed(() => schedules.value.filter((item) => item.scheduledAt?.startsWith(dayjs().format("YYYY-MM-DD"))).length);
const anomalies = computed(() => attendance.value.filter((item) => !item.checkOutAt && dayjs(item.checkInAt).isBefore(dayjs().subtract(12, "hour"))).length);
const filteredSchedules = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  return value ? schedules.value.filter((item) => `${item.orderCode} ${item.title} ${item.engineerName || ""}`.toLowerCase().includes(value)) : schedules.value;
});
const orderOptions = computed(() => orders.value.filter((item) => !["ACCEPTED", "CANCELLED"].includes(item.status)).map((item) => ({ value: item.id, label: `${item.code} · ${item.title}` })));
const assigneeOptions = computed(() => assignees.value.map((item) => ({ value: item.id, label: item.displayName })));
const scheduleColumns = [
  { title: "工单", key: "order", width: 260 }, { title: "负责人", dataIndex: "engineerName", width: 120 },
  { title: "计划到场", key: "scheduledAt", width: 170 }, { title: "现场签到", key: "checkIn", width: 250 }, { title: "状态", key: "status", width: 100 },
];
const attendanceColumns = [
  { title: "工单", dataIndex: "orderCode", width: 150 }, { title: "负责人", dataIndex: "engineerName", width: 120 },
  { title: "签到时间", key: "checkInAt", width: 180 }, { title: "签到位置", dataIndex: "checkInLocation", width: 260 },
  { title: "签退/完工", key: "checkOutAt", width: 180 }, { title: "结果", key: "result", width: 100 },
];

async function load() {
  loading.value = true;
  try {
    [schedules.value, attendance.value, orders.value] = await Promise.all([listSchedules(), listAttendance(), listWorkOrders()]);
    if (canManage.value) assignees.value = await listAssignees();
  } catch (error: any) { message.error(error.message || "排班考勤加载失败"); }
  finally { loading.value = false; }
}
function openSchedule() {
  scheduleForm.orderId = undefined; scheduleForm.engineerId = undefined; scheduleForm.scheduledAt = dayjs().add(1, "day").hour(8).minute(30);
  scheduleOpen.value = true;
}
async function saveSchedule() {
  if (!scheduleForm.orderId || !scheduleForm.engineerId || !scheduleForm.scheduledAt) { message.warning("请完整填写排班信息"); return; }
  saving.value = true;
  try {
    await createSchedule({ orderId: scheduleForm.orderId, engineerId: scheduleForm.engineerId, scheduledAt: scheduleForm.scheduledAt.toISOString() });
    scheduleOpen.value = false; message.success("排班已保存"); await load();
  } catch (error: any) { message.error(error.message || "排班失败"); }
  finally { saving.value = false; }
}
function formatTime(value?: string) { return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-"; }
function statusLabel(value: string) { return ({ CREATED: "待派工", ASSIGNED: "待接单", IN_PROGRESS: "进行中", COMPLETED: "待验收", ACCEPTED: "已完成", CANCELLED: "已取消" } as Record<string, string>)[value] || value; }
function statusColor(value: string) { return ({ CREATED: "default", ASSIGNED: "blue", IN_PROGRESS: "gold", COMPLETED: "cyan", ACCEPTED: "green", CANCELLED: "red" } as Record<string, string>)[value] || "default"; }
onMounted(load);
</script>

<style scoped>
.workforce-view { min-width: 0; }
.workforce-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.workforce-summary > div { padding: 14px 16px; background: #fff; border: 1px solid #e1e6e8; border-radius: 6px; }
.workforce-summary span { display: block; color: #687580; font-size: 12px; }
.workforce-summary strong { display: block; margin-top: 4px; color: #17212b; font-size: 24px; }
.workforce-summary .danger { color: #cf3f35; }
.toolbar { display: flex; gap: 8px; justify-content: flex-end; margin-bottom: 12px; }
.sub { margin-top: 2px; color: #7a8791; font-size: 12px; }
@media (max-width: 760px) { .workforce-summary { grid-template-columns: repeat(2, 1fr); } .toolbar { flex-wrap: wrap; justify-content: flex-start; } }
</style>
