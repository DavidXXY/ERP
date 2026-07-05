<template>
  <div class="page-stack">
    <a-card title="入转调离管理">
      <template #extra>
        <a-space>
          <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
          <a-button v-if="canManage" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>新增流程
          </a-button>
        </a-space>
      </template>

      <a-tabs v-model:active-key="filterStatus">
        <a-tab-pane key="all" tab="全部" />
        <a-tab-pane key="PENDING" tab="待审批" />
        <a-tab-pane key="APPROVED" tab="已通过" />
        <a-tab-pane key="REJECTED" tab="已驳回" />
      </a-tabs>

      <a-table :loading="loading" :data-source="filteredData" :columns="columns" row-key="id" :pagination="{ pageSize: 15 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'lifecycleType'">
            <a-tag :color="typeColor(record.lifecycleType)">{{ typeLabel(record.lifecycleType) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'transfer'">
            <span v-if="record.lifecycleType === 'TRANSFER'">
              {{ record.fromOrganizationName || '-' }} <ArrowRightOutlined /> {{ record.toOrganizationName || '-' }}
            </span>
            <span v-else-if="record.lifecycleType === 'ONBOARDING'">
              → {{ record.toOrganizationName || '-' }} / {{ record.toPosition || '-' }}
            </span>
            <span v-else-if="record.lifecycleType === 'RESIGNATION'">
              {{ record.reason || '-' }}
            </span>
            <span v-else>{{ record.reason || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space v-if="record.status === 'PENDING' && canManage">
              <a-button type="link" size="small" @click="approve(record, true)">通过</a-button>
              <a-button type="link" danger size="small" @click="approve(record, false)">驳回</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Create Lifecycle Modal -->
    <a-modal v-model:open="createModal" title="新增入转调离流程" width="800px" :confirm-loading="saving" @ok="saveLifecycle">
      <a-form ref="lcFormRef" :model="lcForm" layout="vertical">
        <a-row :gutter="14">
          <a-col :span="12">
            <a-form-item label="员工" name="employeeId" :rules="requiredRule">
              <a-select v-model:value="lcForm.employeeId" show-search option-filter-prop="label" placeholder="搜索选择员工" :options="employeeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="流程类型" name="lifecycleType" :rules="requiredRule">
              <a-select v-model:value="lcForm.lifecycleType" :options="lifecycleTypeOptions" @change="onTypeChange" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="14">
          <a-col :span="12">
            <a-form-item label="生效日期" name="effectiveDate" :rules="requiredRule">
              <a-date-picker v-model:value="lcForm.effectiveDate" value-format="YYYY-MM-DD" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="lcForm.lifecycleType === 'TRANSFER'">
            <a-form-item label="调出组织">
              <a-input v-model:value="lcForm.fromOrganizationName" placeholder="如不选可手动输入" />
            </a-form-item>
          </a-col>
        </a-row>
        <template v-if="lcForm.lifecycleType === 'TRANSFER' || lcForm.lifecycleType === 'ONBOARDING'">
          <a-row :gutter="14">
            <a-col :span="12">
              <a-form-item label="调入组织">
                <a-input v-model:value="lcForm.toOrganizationName" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="调入岗位">
                <a-input v-model:value="lcForm.toPosition" />
              </a-form-item>
            </a-col>
          </a-row>
        </template>
        <a-form-item label="原因说明">
          <a-textarea v-model:value="lcForm.reason" :rows="3" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="lcForm.remark" :rows="2" />
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
import { PlusOutlined, ReloadOutlined, ArrowRightOutlined } from "@ant-design/icons-vue";
import { useAuthStore } from "@/stores/auth";
import { listQualificationEmployees, type QualificationEmployee } from "@/api/qualification";
import {
  listAllLifecycles, createLifecycle, approveLifecycle,
  type LifecycleRecord, type LifecyclePayload,
} from "@/api/hr";

const auth = useAuthStore();
const canManage = computed(() => auth.can("qualification:employee:manage"));

const loading = ref(false);
const saving = ref(false);
const filterStatus = ref("all");
const data = ref<LifecycleRecord[]>([]);
const employees = ref<QualificationEmployee[]>([]);

const employeeOptions = computed(() => {
  const active = employees.value.filter(e => e.employmentStatus === "ACTIVE");
  return active.map(e => ({ label: `${e.name} (${e.workNo || '无工号'})`, value: e.id }));
});

const filteredData = computed(() => {
  if (filterStatus.value === "all") return data.value;
  return data.value.filter(d => d.status === filterStatus.value);
});

const lifecycleTypeOptions = [
  { value: "ONBOARDING", label: "入职" },
  { value: "TRANSFER", label: "调岗" },
  { value: "RESIGNATION", label: "离职" },
];

const columns = [
  { title: "员工", dataIndex: "employeeName", width: 140 },
  { title: "类型", key: "lifecycleType", width: 100 },
  { title: "生效日期", dataIndex: "effectiveDate", width: 120 },
  { title: "变动内容", key: "transfer" },
  { title: "状态", key: "status", width: 100 },
  { title: "审批人", dataIndex: "approvedBy", width: 100 },
  { title: "操作", key: "actions", width: 130, fixed: "right" },
];

function typeColor(t: string) {
  return ({ ONBOARDING: "green", TRANSFER: "blue", RESIGNATION: "red" } as Record<string, string>)[t] || "default";
}
function typeLabel(t: string) {
  return ({ ONBOARDING: "入职", TRANSFER: "调岗", RESIGNATION: "离职" } as Record<string, string>)[t] || t;
}
function statusColor(s: string) {
  return ({ PENDING: "orange", APPROVED: "green", REJECTED: "red" } as Record<string, string>)[s] || "default";
}
function statusLabel(s: string) {
  return ({ PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" } as Record<string, string>)[s] || s;
}

// Create form
const createModal = ref(false);
const lcFormRef = ref();
const requiredRule = [{ required: true, message: "请填写必填项" }];
const lcForm = reactive<LifecyclePayload & { employeeId: string }>({
  employeeId: "", lifecycleType: "ONBOARDING", effectiveDate: "",
  fromOrganizationId: "", fromOrganizationName: "", fromPosition: "",
  toOrganizationId: "", toOrganizationName: "", toPosition: "",
  reason: "", remark: "",
});

function onTypeChange() {
  if (lcForm.lifecycleType === "ONBOARDING") {
    lcForm.fromOrganizationName = "";
    lcForm.fromPosition = "";
  } else if (lcForm.lifecycleType === "RESIGNATION") {
    lcForm.toOrganizationName = "";
    lcForm.toPosition = "";
  }
}

function openCreate() {
  Object.assign(lcForm, { employeeId: "", lifecycleType: "ONBOARDING", effectiveDate: "", fromOrganizationId: "", fromOrganizationName: "", fromPosition: "", toOrganizationId: "", toOrganizationName: "", toPosition: "", reason: "", remark: "" });
  createModal.value = true;
}

async function saveLifecycle() {
  await lcFormRef.value?.validate();
  saving.value = true;
  try {
    const { employeeId, ...payload } = lcForm;
    await createLifecycle(employeeId, payload);
    createModal.value = false;
    message.success("流程已提交");
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
const approveRecord = ref<LifecycleRecord>();
const approveRemark = ref("");

function approve(record: LifecycleRecord, approved: boolean) {
  approveRecord.value = record;
  approveAction.value = approved;
  approveRemark.value = "";
  approveModal.value = true;
}

async function confirmApprove() {
  if (!approveRecord.value) return;
  saving.value = true;
  try {
    await approveLifecycle(approveRecord.value.id, {
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
    const [lifecycles, emps] = await Promise.all([
      listAllLifecycles(),
      listQualificationEmployees({}),
    ]);
    data.value = lifecycles;
    employees.value = emps;
  } catch (error: any) {
    message.error(error.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>
