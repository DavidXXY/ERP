<template>
  <a-modal
    :open="createOpen"
    @update:open="emit('update:createOpen', $event)"
    title="新增项目"
    width="860px"
    :confirm-loading="saving"
    @ok="handleCreate"
  >
    <a-form
      ref="createFormRef"
      :model="createForm"
      :rules="createRules"
      layout="vertical"
    >
      <a-row :gutter="16">
        <a-col :xs="24" :md="16"
          ><a-form-item label="项目名称" name="name"
            ><a-input v-model:value="createForm.name" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="12"
          ><a-form-item label="关联客户" name="customerId"
            ><a-select
              v-model:value="createForm.customerId"
              :options="customerOptions"
              show-search
              option-filter-prop="label" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="6"
          ><a-form-item label="项目类型" name="projectType"
            ><a-select
              v-model:value="createForm.projectType"
              :options="projectTypeOptions" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="6">
          <a-form-item label="项目负责人" name="managerName">
            <a-select
              v-model:value="createForm.managerName"
              :options="userOptions"
              show-search
              option-filter-prop="label"
              placeholder="选择项目负责人"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24"
          ><a-form-item label="现场地址" name="siteAddress"
            ><a-input v-model:value="createForm.siteAddress" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="合同金额" name="contractAmount"
            ><a-input-number
              v-model:value="createForm.contractAmount"
              :min="0"
              :precision="2"
              class="full-input" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="计划开始" name="plannedStartDate"
            ><a-input
              v-model:value="createForm.plannedStartDate"
              type="date" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="计划结束" name="plannedEndDate"
            ><a-input
              v-model:value="createForm.plannedEndDate"
              type="date" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="质保截止"
            ><a-input
              v-model:value="createForm.warrantyEndDate"
              type="date" /></a-form-item
        ></a-col>
      </a-row>
      <a-divider
        >分类预算 · 合计 {{ formatMoney(createBudgetTotal) }}</a-divider
      >
      <a-row :gutter="16">
        <a-col
          v-for="item in categoryOptions"
          :key="item.value"
          :xs="24"
          :md="8"
        >
          <a-form-item :label="item.label">
            <a-input-number
              v-model:value="createForm.budgets[(item as any).value]"
              :min="0"
              :precision="2"
              class="full-input"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>

  <a-modal
    :open="approvalOpen"
    @update:open="emit('update:approvalOpen', $event)"
    :title="managerModalTitle"
    width="700px"
    :confirm-loading="saving"
    @ok="handleApproval"
  >
    <a-alert
      v-if="activeProject"
      class="section-alert"
      type="info"
      :message="`${activeProject.code} · ${activeProject.name} · ${formatMoney(activeProject.contractAmount)}`"
    />
    <a-form
      ref="approvalFormRef"
      :model="approvalForm"
      :rules="approvalRules"
      layout="vertical"
    >
      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <a-form-item label="项目经理" name="managerName">
            <a-select
              v-model:value="approvalForm.managerName"
              :options="userOptions"
              show-search
              option-filter-prop="label"
              placeholder="选择项目经理"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12"
          ><a-form-item label="分配人" name="operatorName"
            ><a-input
              v-model:value="approvalForm.operatorName"
              disabled /></a-form-item
        ></a-col>
        <a-col :span="24"
          ><a-form-item label="分配说明"
            ><a-textarea
              v-model:value="approvalForm.comment"
              :rows="3" /></a-form-item
        ></a-col>
      </a-row>
    </a-form>
  </a-modal>

  <a-modal
    :open="stageOpen"
    @update:open="emit('update:stageOpen', $event)"
    title="推进项目阶段"
    width="700px"
    :confirm-loading="saving"
    @ok="handleAdvanceStage"
  >
    <a-alert
      v-if="detail && nextStage"
      class="section-alert"
      type="info"
      :message="`${stageLabel(detail.project.stage)} → ${stageLabel(nextStage)}`"
    />
    <a-form
      ref="stageFormRef"
      :model="stageForm"
      :rules="stageRules"
      layout="vertical"
    >
      <a-row :gutter="16">
        <a-col :xs="24" :md="8"
          ><a-form-item label="目标阶段"
            ><a-input
              :value="nextStage ? stageLabel(nextStage) : ''"
              disabled /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="操作人" name="operatorName"
            ><a-input v-model:value="stageForm.operatorName" /></a-form-item
        ></a-col>
        <a-col :span="24"
          ><a-form-item label="节点说明" name="comment"
            ><a-textarea
              v-model:value="stageForm.comment"
              :rows="3" /></a-form-item
        ></a-col>
      </a-row>
    </a-form>
  </a-modal>

  <a-modal
    :open="costOpen"
    @update:open="emit('update:costOpen', $event)"
    title="登记项目成本"
    width="760px"
    :confirm-loading="saving"
    @ok="handleCreateCost"
  >
    <a-alert
      v-if="detail"
      class="section-alert"
      type="info"
      :message="`${detail.project.code} · 预算余额 ${formatMoney(detail.project.budgetVariance)}`"
    />
    <a-alert
      v-if="detail"
      class="section-alert"
      :type="
        costBudgetOverrun
          ? 'error'
          : costBudgetUsageAfter >= 85
            ? 'warning'
            : 'success'
      "
      show-icon
      :message="`登记后预算使用率 ${costBudgetUsageAfter.toFixed(1)}%，预计余额 ${formatMoney(projectedBudgetVariance)}`"
      :description="
        costBudgetOverrun
          ? '该成本会导致项目超预算，保存时将被后端预算控制拦截，请先走预算调整或审批。'
          : '成本登记后会同步刷新项目实际成本和毛利。'
      "
    />
    <a-form
      ref="costFormRef"
      :model="costForm"
      :rules="costRules"
      layout="vertical"
    >
      <a-row :gutter="16">
        <a-col :xs="24" :md="8"
          ><a-form-item label="成本分类" name="category"
            ><a-select
              v-model:value="costForm.category"
              :options="categoryOptions" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="来源类型" name="sourceType"
            ><a-select
              v-model:value="costForm.sourceType"
              :options="sourceOptions" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="发生日期" name="incurredDate"
            ><a-input
              v-model:value="costForm.incurredDate"
              type="date" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="10"
          ><a-form-item label="来源单号"
            ><a-input
              v-model:value="costForm.sourceNo"
              placeholder="报销单、外包单、领料单" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="14"
          ><a-form-item label="成本说明" name="description"
            ><a-input v-model:value="costForm.description" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="成本金额" name="amount"
            ><a-input-number
              v-model:value="costForm.amount"
              :min="0.01"
              :precision="2"
              class="full-input" /></a-form-item
        ></a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import {
  createProject,
  createProjectCost,
  advanceProjectStage,
  assignProjectManager,
  type ProjectStage,
} from "@/api/project";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const emit = defineEmits([
  "update:createOpen",
  "update:approvalOpen",
  "update:stageOpen",
  "update:costOpen",
  "created",
  "updated",
]);
// @ts-ignore - props types are any for flexibility with option arrays
const props: any = defineProps([
  "createOpen",
  "approvalOpen",
  "stageOpen",
  "costOpen",
  "saving",
  "customerOptions",
  "categoryOptions",
  "projectTypeOptions",
  "sourceOptions",
  "userOptions",
  "detail",
  "activeProject",
  "nextStage",
]);

const createFormRef = ref();
const approvalFormRef = ref();
const stageFormRef = ref();
const costFormRef = ref();

const createForm = reactive({
  customerId: "",
  code: "",
  name: "",
  projectType: "RENOVATION",
  managerName: auth.user?.displayName || "",
  siteAddress: "",
  contractAmount: 0,
  plannedStartDate: dateAfter(0),
  plannedEndDate: dateAfter(90),
  warrantyEndDate: dateAfter(455),
  budgets: { LABOR: 0, MATERIAL: 0, SUBCONTRACT: 0, TRAVEL: 0, OTHER: 0 } as {
    [key: string]: number;
  },
});
const approvalForm = reactive({
  managerName: "",
  operatorName: auth.user?.displayName || "",
  comment: "",
});
const stageForm = reactive({
  comment: "",
  operatorName: auth.user?.displayName || "",
});
const costForm = reactive({
  category: "LABOR",
  sourceType: "MANUAL",
  sourceNo: "",
  description: "",
  amount: 0.01,
  incurredDate: dateAfter(0),
});

const createRules = {
  name: [{ required: true, message: "请输入项目名称" }],
  customerId: [{ required: true, message: "请选择客户" }],
  projectType: [{ required: true, message: "请选择项目类型" }],
  managerName: [{ required: true, message: "请选择项目负责人" }],
  siteAddress: [{ required: true, message: "请输入现场地址" }],
  contractAmount: [{ required: true, message: "请输入合同金额" }],
  plannedStartDate: [{ required: true, message: "请选择计划开始日期" }],
  plannedEndDate: [{ required: true, message: "请选择计划结束日期" }],
};
const approvalRules = {
  managerName: [{ required: true, message: "请选择项目经理" }],
  operatorName: [{ required: true, message: "请输入分配人" }],
};
const stageRules = {
  comment: [{ required: true, message: "请输入节点说明" }],
  operatorName: [{ required: true, message: "请输入操作人" }],
};
const costRules = {
  category: [{ required: true }],
  sourceType: [{ required: true }],
  incurredDate: [{ required: true }],
  description: [{ required: true, message: "请输入成本说明" }],
  amount: [{ required: true, message: "请输入成本金额" }],
};

const createBudgetTotal = computed(() =>
  Object.values(createForm.budgets).reduce((s, v) => s + Number(v || 0), 0),
);
const userOptions = computed(() => props.userOptions || []);
const managerModalTitle = computed(() =>
  props.activeProject?.approvalStatus === "APPROVED"
    ? "变更项目负责人"
    : "分配项目经理",
);
const projectedCostAfterEntry = computed(
  () =>
    Number(props.detail?.project?.actualCost || 0) +
    Number(costForm.amount || 0),
);
const projectedBudgetVariance = computed(
  () =>
    Number(props.detail?.project?.budgetAmount || 0) -
    projectedCostAfterEntry.value,
);
const costBudgetUsageAfter = computed(() => {
  const budget = Number(props.detail?.project?.budgetAmount || 0);
  return budget > 0 ? (projectedCostAfterEntry.value / budget) * 100 : 0;
});
const costBudgetOverrun = computed(() => projectedBudgetVariance.value < 0);

watch(
  () => props.approvalOpen,
  (open) => {
    if (!open) return;
    const currentManager = String(props.activeProject?.managerName || "");
    approvalForm.managerName = currentManager.startsWith("待")
      ? ""
      : currentManager;
    approvalForm.operatorName = auth.user?.displayName || "";
    approvalForm.comment = "";
  },
);

function dateAfter(days: number) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
}
function stageLabel(s: string) {
  if (s === "INITIATED" || s === "BIDDING") return "入场";
  const opts = [
    { label: "入场", value: "ENTRY" },
    { label: "施工", value: "CONSTRUCTION" },
    { label: "调试", value: "COMMISSIONING" },
    { label: "初验", value: "INITIAL_ACCEPTANCE" },
    { label: "终验", value: "FINAL_ACCEPTANCE" },
    { label: "质保", value: "WARRANTY" },
    { label: "关闭", value: "CLOSED" },
  ];
  return opts.find((o) => o.value === s)?.label || s;
}
function formatMoney(v: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(v || 0);
}

async function handleCreate() {
  await createFormRef.value?.validate();
  if (createBudgetTotal.value <= 0) {
    message.warning("请填写项目分类预算");
    return;
  }
  try {
    await createProject({
      customerId: createForm.customerId,
      code: createForm.code,
      name: createForm.name,
      projectType: createForm.projectType as any,
      managerName: createForm.managerName,
      siteAddress: createForm.siteAddress,
      contractAmount: createForm.contractAmount,
      plannedStartDate: createForm.plannedStartDate,
      plannedEndDate: createForm.plannedEndDate,
      warrantyEndDate: createForm.warrantyEndDate || undefined,
      budgetItems: props.categoryOptions.map((item: any) => ({
        category: item.value,
        plannedAmount: createForm.budgets[item.value],
        remark: item.label + "预算",
      })),
    });
    emit("created");
    emit("update:createOpen", false);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目新增失败");
  }
}

async function handleApproval() {
  if (!props.activeProject) return;
  await approvalFormRef.value?.validate();
  try {
    await assignProjectManager(props.activeProject.id, {
      ...approvalForm,
    } as any);
    emit("updated");
    emit("update:approvalOpen", false);
    message.success(
      props.activeProject.approvalStatus === "APPROVED"
        ? "项目负责人已变更"
        : "项目经理已分配，项目进入入场执行",
    );
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目经理分配失败");
  }
}

async function handleAdvanceStage() {
  if (!props.detail || !props.nextStage) return;
  await stageFormRef.value?.validate();
  try {
    await advanceProjectStage(props.detail.project.id, {
      targetStage: props.nextStage,
      ...stageForm,
    } as any);
    emit("updated");
    emit("update:stageOpen", false);
    message.success(
      "项目已进入" + stageLabel(props.detail.project.stage) + "阶段",
    );
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目阶段推进失败");
  }
}

async function handleCreateCost() {
  if (!props.detail) return;
  await costFormRef.value?.validate();
  if (costBudgetOverrun.value) {
    message.warning("登记后将超出项目预算，请先调整预算或走超预算审批");
    return;
  }
  try {
    await createProjectCost(props.detail.project.id, {
      ...costForm,
      sourceNo: costForm.sourceNo || undefined,
    } as any);
    emit("updated");
    emit("update:costOpen", false);
    message.success("项目成本已归集");
  } catch (error) {
    const msg = error instanceof Error ? error.message : "项目成本登记失败";
    message.error(
      msg.includes("Cost exceeds budget")
        ? "成本超过项目预算，已被预算控制拦截"
        : msg,
    );
  }
}
</script>
