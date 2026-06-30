<template>
  <div class="page-stack">
    <a-card title="排班考勤">
      <template #extra><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="今日排班" :value="todayScheduleCount" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="今日在岗" :value="todayOnDutyCount" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="今日请假" :value="todayLeaveCount" :value-style="dangerStyle(todayLeaveCount)" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="当前未签退" :value="activeAttendanceCount" /></a-col>
      </a-row>
    </a-card>

    <a-card>
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="schedules" tab="排班管理">
          <a-space wrap class="table-toolbar"><a-button v-if="auth.can('workforce:schedule:create')" type="primary" @click="openSchedule"><template #icon><PlusOutlined /></template>新增排班</a-button></a-space>
          <a-table :columns="scheduleColumns" :data-source="schedules" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: FieldSchedule) => item.id">
            <template #bodyCell="{ column, record }"><template v-if="column.key === 'status'"><a-tag :color="scheduleStatusColor(record.status)">{{ scheduleStatusLabel(record.status) }}</a-tag></template></template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="attendance" tab="考勤记录">
          <a-table :columns="attendanceColumns" :data-source="attendance" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: FieldAttendance) => item.id" :scroll="{ x: 1100 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'checkIn'">{{ formatDateTime(record.checkInAt) }}<span class="table-subtitle">{{ record.checkInLocation }}</span></template>
              <template v-else-if="column.key === 'checkOut'"><template v-if="record.checkOutAt">{{ formatDateTime(record.checkOutAt) }}<span class="table-subtitle">{{ record.checkOutLocation }}</span></template><a-tag v-else color="blue">作业中</a-tag></template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="scheduleOpen" title="新增排班" :confirm-loading="saving" @ok="handleSchedule">
      <a-form ref="scheduleFormRef" :model="scheduleForm" :rules="scheduleRules" layout="vertical">
        <a-form-item label="员工" name="userId"><a-select v-model:value="scheduleForm.userId" show-search option-filter-prop="label" :options="userOptions" /></a-form-item>
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="日期" name="workDate"><a-input v-model:value="scheduleForm.workDate" type="date" /></a-form-item></a-col><a-col :span="12"><a-form-item label="班次" name="shiftName"><a-input v-model:value="scheduleForm.shiftName" placeholder="白班 08:30-17:30" /></a-form-item></a-col></a-row>
        <a-form-item label="工作地点"><a-input v-model:value="scheduleForm.siteName" /></a-form-item>
        <a-form-item label="状态" name="status"><a-radio-group v-model:value="scheduleForm.status" button-style="solid"><a-radio-button value="AVAILABLE">可安排</a-radio-button><a-radio-button value="ON_DUTY">在岗</a-radio-button><a-radio-button value="LEAVE">请假</a-radio-button></a-radio-group></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { createSchedule, getMaintenanceReferences, listAttendance, listSchedules, type FieldAttendance, type FieldSchedule, type MaintenanceReferences, type ScheduleStatus } from "@/api/maintenance";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore(); const activeTab = ref("schedules"); const loading = ref(false); const saving = ref(false); const scheduleOpen = ref(false);
const schedules = ref<FieldSchedule[]>([]); const attendance = ref<FieldAttendance[]>([]);
const references = reactive<MaintenanceReferences>({ customers: [], contracts: [], parts: [], users: [] }); const scheduleFormRef = ref();
const scheduleForm = reactive<{ userId: string; workDate: string; shiftName: string; siteName: string; status: ScheduleStatus }>({ userId: "", workDate: today(), shiftName: "白班 08:30-17:30", siteName: "", status: "AVAILABLE" });
const userOptions = computed(() => references.users.filter(item => item.enabled).map(item => ({ label: item.displayName, value: item.id })));
const todayScheduleCount = computed(() => schedules.value.filter(item => item.workDate === today()).length);
const todayOnDutyCount = computed(() => schedules.value.filter(item => item.workDate === today() && item.status === "ON_DUTY").length);
const todayLeaveCount = computed(() => schedules.value.filter(item => item.workDate === today() && item.status === "LEAVE").length);
const activeAttendanceCount = computed(() => attendance.value.filter(item => !item.checkOutAt).length);
const scheduleColumns = [{ title: "日期", dataIndex: "workDate", width: 130 }, { title: "员工", dataIndex: "employeeName", width: 160 }, { title: "班次", dataIndex: "shiftName", width: 200 }, { title: "工作地点", dataIndex: "siteName" }, { title: "状态", key: "status", width: 120 }];
const attendanceColumns = [{ title: "员工", dataIndex: "employeeName", width: 150 }, { title: "工单", dataIndex: "workOrderCode", width: 190 }, { title: "签到", key: "checkIn", width: 340 }, { title: "签退", key: "checkOut", width: 340 }];
const scheduleRules = { userId: [{ required: true }], workDate: [{ required: true }], shiftName: [{ required: true }], status: [{ required: true }] };
onMounted(loadData);
async function loadData() { loading.value = true; try { const [refs, scheduleRows, attendanceRows] = await Promise.all([getMaintenanceReferences(), listSchedules(), listAttendance()]); Object.assign(references, refs); schedules.value = scheduleRows; attendance.value = attendanceRows; } catch (error) { message.error(error instanceof Error ? error.message : "人事数据加载失败"); } finally { loading.value = false; } }
function openSchedule() { Object.assign(scheduleForm, { userId: "", workDate: today(), shiftName: "白班 08:30-17:30", siteName: "", status: "AVAILABLE" }); scheduleOpen.value = true; }
async function handleSchedule() { await scheduleFormRef.value?.validate(); saving.value = true; try { await createSchedule({ ...scheduleForm }); scheduleOpen.value = false; message.success("排班已登记"); await loadData(); } catch (error) { message.error(error instanceof Error ? error.message : "排班登记失败"); } finally { saving.value = false; } }
function scheduleStatusLabel(value: ScheduleStatus) { return ({ AVAILABLE: "可安排", ON_DUTY: "在岗", LEAVE: "请假" } as Record<ScheduleStatus, string>)[value]; }
function scheduleStatusColor(value: ScheduleStatus) { return ({ AVAILABLE: "green", ON_DUTY: "blue", LEAVE: "orange" } as Record<ScheduleStatus, string>)[value]; }
function today() { const value = new Date(); return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`; }
function formatDateTime(value?: string) { return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "-"; }
function dangerStyle(value: number) { return value > 0 ? { color: "#cf1322" } : {}; }
</script>
