<template>
  <div class="page-stack">
    <a-card title="请假管理">
      <template #extra>
        <a-space>
          <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
          <a-button type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>新增请假
          </a-button>
        </a-space>
      </template>

      <a-row :gutter="16" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="待审批" :value="pendingCount" :value-style="{ color: '#faad14' }" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="本月请假" :value="monthlyCount" /></a-col>
      </a-row>

      <a-tabs v-model:active-key="filterStatus">
        <a-tab-pane key="all" tab="全部" />
        <a-tab-pane key="PENDING" tab="待审批" />
        <a-tab-pane key="APPROVED" tab="已通过" />
        <a-tab-pane key="REJECTED" tab="已驳回" />
      </a-tabs>

      <a-table :loading="loading" :data-source="filteredData" :columns="columns" row-key="id" :pagination="{ pageSize: 15 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'leaveType'">
            <a-tag :color="typeColor(record.leaveType)">{{ typeLabel(record.leaveType) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'period'">
            {{ record.startDate }} ~ {{ record.endDate }}
            <span class="table-subtitle">共 {{ record.totalDays }} 天</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space v-if="record.status === 'PENDING'">
              <a-button v-if="canManage" type="link" size="small" @click="approveLeave(record, true)">通过</a-button>
              <a-button v-if="canManage" type="link" danger size="small" @click="approveLeave(record, false)">驳回</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Create Leave Modal -->
    <a-modal v-model:open="createModal" title="新增请假申请" width="700px" :confirm-loading="saving" @ok="saveLeave">
      <a-form ref="leaveFormRef" :model="leaveForm" layout="vertical">
        <a-row :gutter="14">
          <a-col :span="12">
            <a-form-item label="员工" name="employeeId" :rules="requiredRule">
              <a-select v-model:value="leaveForm.employeeId" show-search option-filter-prop="label" placeholder="搜索选择员工" :options="employeeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="请假类型" name="leaveType" :rules="requiredRule">
              <a-select v-model:value="leaveForm.leaveType" :options="leaveTypeOptions" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="14">
          <a-col :span="8">
            <a-form-item label="开始日期" name="startDate" :rules="requiredRule">
              <a-date-picker v-model:value="leaveForm.startDate" value-format="YYYY-MM-DD" style="width:100%" @change="calcDays" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="结束日期" name="endDate" :rules="requiredRule">
              <a-date-picker v-model:value="leaveForm.endDate" value-format="YYYY-MM-DD" style="width:100%" @change="calcDays" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="共计天数">
              <a-input-number v-model:value="leaveForm.totalDays" :min="0.5" :step="0.5" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="请假原因">
          <a-textarea v-model:value="leaveForm.reason" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Approve Modal -->
    <a-modal v-model:open="approveModal" :title="approveAction === true ? '审批通过' : '驳回'" :confirm-loading="saving" @ok="confirmApprove">
      <a-form layout="vertical">
        <a-form-item label="审批意见">
          <a-textarea v-model:value="approveRemark" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message, DatePicker } from "ant-design-vue";
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import { useAuthStore } from "@/stores/auth";
import { listQualificationEmployees, type QualificationEmployee } from "@/api/qualification";
import {
  listAllLeaves, createLeave, approveLeave as approveLeaveApi,
  type LeaveRecord, type LeavePayload,
} from "@/api/hr";

const auth = useAuthStore();
const canManage = computed(() => auth.can("qualification:employee:manage"));

const loading = ref(false);
const saving = ref(false);
const filterStatus = ref("all");
const data = ref<LeaveRecord[]>([]);
const employees = ref<QualificationEmployee[]>([]);

const employeeOptions = computed(() => {
  const active = employees.value.filter(e => e.employmentStatus === "ACTIVE");
  return active.map(e => ({ label: `${e.name} (${e.workNo || '无工号'})`, value: e.id }));
});

const pendingCount = computed(() => data.value.filter(d => d.status === "PENDING").length);
const monthlyCount = computed(() => {
  const now = new Date();
  return data.value.filter(d => {
    const s = new Date(d.startDate);
    return s.getMonth() === now.getMonth() && s.getFullYear() === now.getFullYear();
  }).length;
});

const filteredData = computed(() => {
  if (filterStatus.value === "all") return data.value;
  return data.value.filter(d => d.status === filterStatus.value);
});

const leaveTypeOptions = [
  { value: "ANNUAL", label: "年假" },
  { value: "SICK", label: "病假" },
  { value: "PERSONAL", label: "事假" },
  { value: "MARRIAGE", label: "婚假" },
  { value: "MATERNITY", label: "产假" },
  { value: "PATERNITY", label: "陪产假" },
  { value: "BEREAVEMENT", label: "丧假" },
  { value: "COMPENSATORY", label: "调休" },
  { value: "OTHER", label: "其他" },
];

const columns = [
  { title: "员工", dataIndex: "employeeName", width: 130 },
  { title: "类型", key: "leaveType", width: 90 },
  { title: "请假时间", key: "period", width: 260 },
  { title: "原因", dataIndex: "reason", ellipsis: true },
  { title: "状态", key: "status", width: 100 },
  { title: "审批人", dataIndex: "approvedBy", width: 100 },
  { title: "操作", key: "actions", width: 130, fixed: "right" },
];

function typeColor(t: string) {
  const colors: Record<string, string> = { ANNUAL: "blue", SICK: "red", PERSONAL: "default", MARRIAGE: "pink", MATERNITY: "purple", PATERNITY: "purple", BEREAVEMENT: "grey", COMPENSATORY: "cyan", OTHER: "default" };
  return colors[t] || "default";
}
function typeLabel(t: string) {
  const labels: Record<string, string> = { ANNUAL: "年假", SICK: "病假", PERSONAL: "事假", MARRIAGE: "婚假", MATERNITY: "产假", PATERNITY: "陪产假", BEREAVEMENT: "丧假", COMPENSATORY: "调休", OTHER: "其他" };
  return labels[t] || t;
}
function statusColor(s: string) {
  return ({ PENDING: "orange", APPROVED: "green", REJECTED: "red" } as Record<string, string>)[s] || "default";
}
function statusLabel(s: string) {
  return ({ PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" } as Record<string, string>)[s] || s;
}

// Create form
const createModal = ref(false);
const leaveFormRef = ref();
const requiredRule = [{ required: true, message: "请填写必填项" }];
const leaveForm = reactive<LeavePayload & { employeeId: string }>({
  employeeId: "", leaveType: "ANNUAL", startDate: "", endDate: "", totalDays: 1, reason: "",
});

function calcDays() {
  if (leaveForm.startDate && leaveForm.endDate) {
    const start = new Date(leaveForm.startDate);
    const end = new Date(leaveForm.endDate);
    const diff = (end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24) + 1;
    leaveForm.totalDays = diff > 0 ? diff : 1;
  }
}

function openCreate() {
  Object.assign(leaveForm, { employeeId: "", leaveType: "ANNUAL", startDate: "", endDate: "", totalDays: 1, reason: "" });
  createModal.value = true;
}

async function saveLeave() {
  await leaveFormRef.value?.validate();
  saving.value = true;
  try {
    const { employeeId, ...payload } = leaveForm;
    await createLeave(employeeId, payload);
    createModal.value = false;
    message.success("请假申请已提交");
    await loadData();
  } catch (error: any) {
    message.error(error.message || "提交失败");
  } finally {
    saving.value = false;
  }
}

// Approve
const approveModal = ref(false);
const approveAction = ref<boolean | null>(null);
const approveRecord = ref<LeaveRecord>();
const approveRemark = ref("");

function approveLeave(record: LeaveRecord, approved: boolean) {
  approveRecord.value = record;
  approveAction.value = approved;
  approveRemark.value = "";
  approveModal.value = true;
}

async function confirmApprove() {
  if (!approveRecord.value) return;
  saving.value = true;
  try {
    await approveLeaveApi(approveRecord.value.id, {
      approved: approveAction.value!,
      remark: approveRemark.value,
      operatorName: auth.user?.displayName || "系统管理员",
    });
    approveModal.value = false;
    message.success(approveAction.value ? "已审批通过" : "已驳回");
    await loadData();
  } catch (error: any) {
    message.error(error.message || "操作失败");
  } finally {
    saving.value = false;
  }
}

async function loadData() {
  loading.value = true;
  try {
    const [leaves, emps] = await Promise.all([
      listAllLeaves(),
      listQualificationEmployees({}),
    ]);
    data.value = leaves;
    employees.value = emps;
  } catch (error: any) {
    message.error(error.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>
