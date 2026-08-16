<template>
  <div class="page-stack">
    <a-card>
      <template #title>出差申请</template>
      <template #extra>
        <a-space>
          <a-button @click="router.push('/office')">返回办公室</a-button>
          <a-button :loading="loading" @click="loadData">
            <template #icon><ReloadOutlined /></template>刷新
          </a-button>
        </a-space>
      </template>

      <a-space wrap class="table-toolbar">
        <a-button
          v-if="auth.can('office:travel:create')"
          type="primary"
          @click="openCreate"
        >
          <template #icon><PlusOutlined /></template>新增出差申请
        </a-button>
      </a-space>

      <a-table
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(record: TravelApplication) => record.id"
        :scroll="{ x: 1460 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'application'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.purpose }}</span>
          </template>
          <template v-else-if="column.key === 'route'">
            <strong>{{ record.destination }}</strong>
            <span class="table-subtitle">{{ record.transportType }}</span>
          </template>
          <template v-else-if="column.key === 'period'">
            {{ record.startDate }} 至 {{ record.endDate }}
            <span class="table-subtitle">共 {{ record.travelDays }} 天</span>
          </template>
          <template v-else-if="column.key === 'amount'">
            <strong>{{ formatMoney(record.estimatedAmount) }}</strong>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{
              statusLabel(record.status)
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openApproval(record)"
              >查看/审批</a-button
            >
          </template>
        </template>
      </a-table>
    </a-card>

    <ApprovalCenterView
      ref="approvalCenterRef"
      embedded
      drawer-only
      @changed="loadData"
    />

    <a-modal
      v-model:open="createOpen"
      title="新增出差申请"
      width="860px"
      :confirm-loading="saving"
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="申请人" name="applicantId">
              <a-select
                v-model:value="form.applicantId"
                show-search
                option-filter-prop="label"
                :options="userOptions"
                @change="syncApplicant"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="部门" name="departmentName">
              <a-input
                v-model:value="form.departmentName"
                placeholder="填写所属部门"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="绑定项目">
              <a-select
                v-model:value="form.projectId"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="projectOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="目的地" name="destination">
              <a-input
                v-model:value="form.destination"
                placeholder="城市或具体地点"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="交通方式" name="transportType">
              <a-select
                v-model:value="form.transportType"
                :options="transportOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="预算金额（含税，元）" name="estimatedAmount">
              <a-input-number
                v-model:value="form.estimatedAmount"
                :min="0"
                :precision="2"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="开始日期" name="startDate">
              <a-input v-model:value="form.startDate" type="date" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="结束日期" name="endDate">
              <a-input v-model:value="form.endDate" type="date" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-alert
              type="info"
              show-icon
              :message="`出差时长：${travelDays} 天`"
              class="form-summary"
            />
          </a-col>
          <a-col :span="24">
            <a-form-item label="出差事由" name="purpose">
              <a-textarea
                v-model:value="form.purpose"
                :rows="3"
                placeholder="说明出差目标、工作安排和预期成果"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="同行人员">
              <a-input
                v-model:value="form.companionNames"
                placeholder="多人可用顿号分隔"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { generateCode } from "@/utils/code";
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { useRouter } from "vue-router";
import {
  createTravelApplication,
  getOfficeReferences,
  listTravelApplications,
  type OfficeApplicationStatus,
  type TravelApplication,
} from "@/api/office";
import { useAuthStore } from "@/stores/auth";
import ApprovalCenterView from "@/views/office/ApprovalCenterView.vue";

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const items = ref<TravelApplication[]>([]);
const references = reactive({ users: [], projects: [] } as any);
const formRef = ref();
const approvalCenterRef = ref<InstanceType<typeof ApprovalCenterView>>();
const form = reactive({
  code: "",
  applicantId: "",
  applicantName: "",
  departmentName: "",
  projectId: undefined as string | undefined,
  destination: "",
  purpose: "",
  transportType: "高铁",
  startDate: today(),
  endDate: today(),
  estimatedAmount: 0,
  companionNames: "",
});
const columns = [
  { title: "申请单", key: "application", width: 280 },
  { title: "申请人", dataIndex: "applicantName", width: 120 },
  { title: "部门", dataIndex: "departmentName", width: 150 },
  { title: "行程", key: "route", width: 190 },
  { title: "出差日期", key: "period", width: 230 },
  { title: "项目", dataIndex: "projectCode", width: 150 },
  { title: "同行人员", dataIndex: "companionNames", width: 150 },
  { title: "预算金额（含税，元）", key: "amount", width: 190 },
  { title: "状态", key: "status", width: 120 },
  { title: "操作", key: "action", width: 110, fixed: "right" as const },
];
const rules = {
  applicantId: [{ required: true, message: "请选择申请人" }],
  departmentName: [{ required: true, message: "请填写部门" }],
  destination: [{ required: true, message: "请填写目的地" }],
  transportType: [{ required: true, message: "请选择交通方式" }],
  startDate: [{ required: true, message: "请选择开始日期" }],
  endDate: [{ required: true, message: "请选择结束日期" }],
  estimatedAmount: [{ required: true, message: "请填写预算金额" }],
  purpose: [{ required: true, message: "请填写出差事由" }],
};
const transportOptions = ["飞机", "高铁", "火车", "汽车", "自驾", "其他"].map(
  (value) => ({ label: value, value }),
);
const userOptions = computed(() =>
  references.users
    .filter((item: any) => item.enabled)
    .map((item: any) => ({ label: item.displayName, value: item.id })),
);
const projectOptions = computed(() =>
  references.projects.map((item: any) => ({
    label: `${item.code} · ${item.name}`,
    value: item.id,
  })),
);
const travelDays = computed(() => {
  const start = new Date(`${form.startDate}T00:00:00`);
  const end = new Date(`${form.endDate}T00:00:00`);
  if (
    Number.isNaN(start.getTime()) ||
    Number.isNaN(end.getTime()) ||
    end < start
  )
    return 0;
  return Math.round((end.getTime() - start.getTime()) / 86400000) + 1;
});

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [applications, referenceData] = await Promise.all([
      listTravelApplications(),
      getOfficeReferences(),
    ]);
    items.value = applications || [];
    references.users = referenceData.users || [];
    references.projects = referenceData.projects || [];
  } catch (error) {
    message.error(error instanceof Error ? error.message : "数据加载失败");
  } finally {
    loading.value = false;
  }
}
function openCreate() {
  Object.assign(form, {
    code: generateCode("CC"),
    applicantId: auth.user?.id || "",
    applicantName: auth.user?.displayName || "",
    departmentName: "",
    projectId: undefined,
    destination: "",
    purpose: "",
    transportType: "高铁",
    startDate: today(),
    endDate: today(),
    estimatedAmount: 0,
    companionNames: "",
  });
  createOpen.value = true;
}
function syncApplicant(id: string) {
  form.applicantName =
    references.users.find((item: any) => item.id === id)?.displayName || "";
}
function openApproval(record: TravelApplication) {
  approvalCenterRef.value?.openApprovalById(record.approvalRequestId);
}
async function submit() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  if (travelDays.value < 1) {
    message.error("结束日期不能早于开始日期");
    return;
  }
  saving.value = true;
  try {
    await createTravelApplication({
      ...form,
      projectId: form.projectId || undefined,
      companionNames: form.companionNames || undefined,
    });
    createOpen.value = false;
    message.success("出差申请已提交审批");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "出差申请提交失败");
  } finally {
    saving.value = false;
  }
}
function statusLabel(value: OfficeApplicationStatus) {
  return (
    {
      PENDING_APPROVAL: "待审批",
      APPROVED: "已通过",
      REJECTED: "已驳回",
      COMPLETED: "已完成",
      CANCELLED: "已取消",
    } as Record<OfficeApplicationStatus, string>
  )[value];
}
function statusColor(value: OfficeApplicationStatus) {
  return (
    {
      PENDING_APPROVAL: "orange",
      APPROVED: "green",
      REJECTED: "red",
      COMPLETED: "blue",
      CANCELLED: "default",
    } as Record<OfficeApplicationStatus, string>
  )[value];
}
function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(value || 0);
}
function today() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
</script>

<style scoped>
.form-summary {
  margin-bottom: 16px;
}
</style>
