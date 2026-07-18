<template>
  <div class="page-stack">
    <a-card>
      <template #title>外包服务</template>
      <template #extra
        ><a-space
          ><a-button @click="goBack">返回办公室</a-button
          ><a-button :loading="loading" @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          ></a-space
        ></template
      >
      <a-space wrap class="table-toolbar"
        ><a-button
          v-if="auth.can('office:outsource:create')"
          type="primary"
          @click="openOutsource"
          ><template #icon><PlusOutlined /></template>新增外包</a-button
        ></a-space
      >
      <a-table
        :columns="outsourceColumns"
        :data-source="outsourcing"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(r: any) => r.id"
        :scroll="{ x: 1300 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'order'"
            ><strong>{{ record.code }}</strong
            ><span class="table-subtitle">{{
              record.serviceType
            }}</span></template
          >
          <template v-else-if="column.key === 'binding'">{{
            record.projectCode || record.workOrderCode || "-"
          }}</template>
          <template v-else-if="column.key === 'amount'"
            ><strong>{{ formatMoney(record.amount) }}</strong></template
          >
          <template v-else-if="column.key === 'status'"
            ><a-tag :color="outsourceStatusColor(record.status)">{{
              outsourceStatusLabel(record.status)
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button
                v-if="record.approvalRequestId"
                type="link"
                size="small"
                @click="openApproval(record)"
                >查看/审批</a-button
              >
              <a-button
                v-if="
                  record.status === 'APPROVED' &&
                  auth.can('office:outsource:complete')
                "
                type="link"
                size="small"
                @click="openOutsourceComplete(record)"
                >验收结算</a-button
              >
            </a-space>
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
      v-model:open="outsourceOpen"
      title="新增外包服务"
      width="760px"
      :confirm-loading="saving"
      @ok="handleOutsource"
    >
      <a-form
        ref="outsourceFormRef"
        :model="outsourceForm"
        :rules="outsourceRules"
        layout="vertical"
      >
        <a-row :gutter="16"
          ><a-col :span="12"
            ><a-form-item label="服务商" name="supplierId"
              ><a-select
                v-model:value="outsourceForm.supplierId"
                show-search
                option-filter-prop="label"
                :options="supplierOptions" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="服务类型" name="serviceType"
              ><a-input
                v-model:value="
                  outsourceForm.serviceType
                " /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="16"
          ><a-col :span="8"
            ><a-form-item label="绑定项目"
              ><a-select
                v-model:value="outsourceForm.projectId"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="projectOptions" /></a-form-item></a-col
          ><a-col :span="8"
            ><a-form-item label="金额" name="amount"
              ><a-input-number
                v-model:value="outsourceForm.amount"
                :min="0"
                :precision="2"
                class="full-input" /></a-form-item></a-col
          ><a-col :span="8"
            ><a-form-item label="计划日期" name="plannedDate"
              ><a-input
                v-model:value="outsourceForm.plannedDate"
                type="date" /></a-form-item></a-col
        ></a-row>
        <a-form-item label="服务内容" name="description"
          ><a-textarea v-model:value="outsourceForm.description" :rows="2"
        /></a-form-item>
      </a-form>
    </a-modal>
    <a-modal
      v-model:open="outsourceCompleteOpen"
      title="外包验收"
      :confirm-loading="saving"
      @ok="handleOutsourceComplete"
    >
      <a-alert
        v-if="selectedOutsource"
        class="section-alert"
        type="warning"
        :message="`${selectedOutsource.code} · ${selectedOutsource.supplierName} · ${formatMoney(selectedOutsource.amount)}`"
      />
      <a-form
        ref="outsourceCompleteFormRef"
        :model="outsourceCompleteForm"
        :rules="outsourceCompleteRules"
        layout="vertical"
        ><a-form-item label="验收意见" name="acceptanceNote"
          ><a-textarea
            v-model:value="outsourceCompleteForm.acceptanceNote"
            :rows="3" /></a-form-item
      ></a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  completeOutsource,
  createOutsource,
  getOfficeReferences,
  listOutsourcing,
  type Outsource,
  type OutsourceStatus,
} from "@/api/office";
import { useAuthStore } from "@/stores/auth";
import ApprovalCenterView from "@/views/office/ApprovalCenterView.vue";
const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const outsourcing = ref<Outsource[]>([]);
const references = reactive({
  suppliers: [],
  projects: [],
  workOrders: [],
} as any);
const approvalCenterRef = ref<InstanceType<typeof ApprovalCenterView>>();
const outsourceOpen = ref(false);
const outsourceCompleteOpen = ref(false);
const outsourceFormRef = ref();
const outsourceCompleteFormRef = ref();
const selectedOutsource = ref<Outsource | null>(null);
const outsourceForm = reactive({
  code: "",
  supplierId: "",
  projectId: undefined as string | undefined,
  workOrderId: undefined as string | undefined,
  serviceType: "",
  description: "",
  amount: 0,
  plannedDate: today(),
});
const outsourceCompleteForm = reactive({ acceptanceNote: "验收合格" });
const outsourceColumns = [
  { title: "外包单", key: "order", width: 230 },
  { title: "服务商", dataIndex: "supplierName", width: 220 },
  { title: "绑定业务", key: "binding", width: 170 },
  { title: "计划日期", dataIndex: "plannedDate", width: 120 },
  { title: "金额", key: "amount", width: 140 },
  { title: "状态", key: "status", width: 130 },
  { title: "说明", dataIndex: "description", width: 260 },
  { title: "操作", key: "action", width: 120, fixed: "right" as const },
];
const outsourceRules = {
  supplierId: [{ required: true }],
  serviceType: [{ required: true }],
  amount: [{ required: true }],
  plannedDate: [{ required: true }],
  description: [{ required: true }],
};
const outsourceCompleteRules = { acceptanceNote: [{ required: true }] };
const supplierOptions = computed(() =>
  references.suppliers.map((i: any) => ({
    label: i.code + " · " + i.name,
    value: i.id,
  })),
);
const projectOptions = computed(() =>
  references.projects.map((i: any) => ({
    label: i.code + " · " + i.name,
    value: i.id,
  })),
);
const workOrderOptions = computed(() =>
  references.workOrders.map((i: any) => ({
    label: i.code + " · " + i.title,
    value: i.id,
  })),
);
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const r = await getOfficeReferences();
    Object.assign(references, r);
    outsourcing.value = await listOutsourcing();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "数据加载失败");
  } finally {
    loading.value = false;
  }
}
function goBack() {
  router.push("/office");
}
function openOutsource() {
  Object.assign(outsourceForm, {
    code: generateCode("WB"),
    supplierId: "",
    projectId: undefined,
    workOrderId: undefined,
    serviceType: "",
    description: "",
    amount: 0,
    plannedDate: today(),
  });
  outsourceOpen.value = true;
}
async function handleOutsource() {
  await outsourceFormRef.value?.validate();
  saving.value = true;
  try {
    await createOutsource({
      ...outsourceForm,
      applicantName: auth.user?.displayName || "",
    });
    outsourceOpen.value = false;
    message.success("外包单已提交审批");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "外包单提交失败");
  } finally {
    saving.value = false;
  }
}
function openOutsourceComplete(item: Outsource) {
  selectedOutsource.value = item;
  outsourceCompleteForm.acceptanceNote = "验收合格";
  outsourceCompleteOpen.value = true;
}
function openApproval(record: Outsource) {
  approvalCenterRef.value?.openApprovalById(record.approvalRequestId);
}
async function handleOutsourceComplete() {
  await outsourceCompleteFormRef.value?.validate();
  if (!selectedOutsource.value) return;
  saving.value = true;
  try {
    await completeOutsource(selectedOutsource.value.id, {
      ...outsourceCompleteForm,
    });
    outsourceCompleteOpen.value = false;
    message.success("外包服务已验收");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "外包验收失败");
  } finally {
    saving.value = false;
  }
}
function generateCode(prefix: string) {
  const d = new Date();
  return (
    prefix +
    "-" +
    d.getFullYear() +
    String(d.getMonth() + 1).padStart(2, "0") +
    String(d.getDate()).padStart(2, "0") +
    "-" +
    String(d.getHours()).padStart(2, "0") +
    String(d.getMinutes()).padStart(2, "0")
  );
}
function today() {
  const d = new Date();
  return (
    d.getFullYear() +
    "-" +
    String(d.getMonth() + 1).padStart(2, "0") +
    "-" +
    String(d.getDate()).padStart(2, "0")
  );
}
function outsourceStatusLabel(v: OutsourceStatus) {
  return (
    {
      PENDING_APPROVAL: "待审批",
      APPROVED: "待执行",
      IN_PROGRESS: "执行中",
      COMPLETED: "已验收",
      SETTLED: "已结算",
      REJECTED: "已驳回",
    } as Record<OutsourceStatus, string>
  )[v];
}
function outsourceStatusColor(v: OutsourceStatus) {
  return (
    {
      PENDING_APPROVAL: "orange",
      APPROVED: "blue",
      IN_PROGRESS: "cyan",
      COMPLETED: "purple",
      SETTLED: "green",
      REJECTED: "red",
    } as Record<OutsourceStatus, string>
  )[v];
}
function formatMoney(v: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(v || 0);
}
</script>
