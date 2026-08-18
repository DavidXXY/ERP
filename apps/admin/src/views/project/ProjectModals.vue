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
        <a-col :xs="24" :md="12">
          <a-form-item label="父项目（创建子项目时选择）">
            <a-select
              v-model:value="createForm.parentProjectId"
              :options="parentProjectOptions"
              allow-clear
              show-search
              option-filter-prop="label"
              placeholder="不选择则创建一级项目"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="6"
          ><a-form-item label="项目类型" name="projectType"
            ><a-select
              v-model:value="createForm.projectType"
              :options="projectTypeOptions" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="6">
          <a-form-item label="项目负责人（可稍后分配）" name="managerUserId">
            <a-select
              v-model:value="createForm.managerUserId"
              :options="userOptions"
              allow-clear
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
          ><a-form-item label="合同金额（含税，元）" name="contractAmount"
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
        <a-col :span="24"
          ><a-form-item label="售前成本核算（可选）">
            <a-select
              v-model:value="createForm.quoteId"
              :options="quoteOptions"
              allow-clear
              show-search
              option-filter-prop="label"
              placeholder="选择已审批的售前成本，自动带入分类预算"
              @change="applyQuoteBudget"
            /> </a-form-item
        ></a-col>
      </a-row>
      <a-divider
        >分类预算合计（含税，元） ·
        {{ formatMoney(createBudgetTotal) }}</a-divider
      >
      <a-row :gutter="16">
        <a-col
          v-for="item in categoryOptions"
          :key="item.value"
          :xs="24"
          :md="8"
        >
          <a-form-item :label="`${item.label}预算（含税，元）`">
            <a-input-number
              v-model:value="createForm.budgets[item.value]"
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
      :message="`${activeProject.code} · ${activeProject.name} · 合同金额（含税，元）${formatMoney(activeProject.contractAmount)}`"
    />
    <a-form
      ref="approvalFormRef"
      :model="approvalForm"
      :rules="approvalRules"
      layout="vertical"
    >
      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <a-form-item label="项目经理" name="managerUserId">
            <a-select
              v-model:value="approvalForm.managerUserId"
              :options="userOptions"
              show-search
              option-filter-prop="label"
              placeholder="选择项目经理"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24"
          ><a-form-item label="分配说明"
            ><a-textarea
              v-model:value="approvalForm.comment"
              :rows="3" /></a-form-item
        ></a-col>
        <a-col
          v-if="
            !activeProject?.parentProjectId &&
            Number(activeProject?.childProjectCount || 0) > 0
          "
          :span="24"
        >
          <a-checkbox v-model:checked="approvalForm.syncChildProjects">
            同步更新全部未结束子项目的项目经理
          </a-checkbox>
        </a-col>
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
    <a-alert
      v-if="detail && nextStage === 'CLOSED'"
      class="section-alert"
      type="warning"
      show-icon
      :message="
        closeoutReview?.status === 'APPROVED'
          ? '结项复核已通过，可以关闭项目'
          : '关闭项目前必须先提交结项申请并完成结项复核'
      "
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
    :title="editingCost ? '更正项目成本' : '登记项目成本'"
    width="760px"
    :confirm-loading="saving"
    @ok="handleCreateCost"
  >
    <a-alert
      v-if="detail"
      class="section-alert"
      type="info"
      :message="`${detail.project.code} · 预算余额（含税，元）${formatMoney(detail.project.budgetVariance)}`"
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
      :message="`${editingCost ? '更正后' : '登记后'}预算使用率 ${costBudgetUsageAfter.toFixed(1)}%，预计余额（含税，元）${formatMoney(projectedBudgetVariance)}`"
      :description="
        costBudgetOverrun
          ? '该成本会导致项目超预算，请申请调整项目总预算，审批通过后再登记。'
          : '成本更正会同步刷新项目实际成本和毛利。'
      "
    >
      <template v-if="costBudgetOverrun" #action>
        <a-button size="small" danger @click="goToBudgetChange">
          申请预算变更
        </a-button>
      </template>
    </a-alert>
    <a-alert
      v-if="editingCost"
      class="section-alert"
      type="info"
      show-icon
      message="仅人工登记成本可更正；来源单据生成的成本请通过来源单据更正。"
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
              :options="sourceOptions"
              :disabled="editingCost" /></a-form-item
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
              :disabled="editingCost"
              placeholder="报销单、外包单、领料单" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="14"
          ><a-form-item label="成本说明" name="description"
            ><a-input v-model:value="costForm.description" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="成本金额（含税，元）" name="amount"
            ><a-input-number
              v-model:value="costForm.amount"
              :min="0.01"
              :precision="2"
              class="full-input" /></a-form-item
        ></a-col>
      </a-row>
    </a-form>
    <template v-if="editingCost" #footer>
      <a-button danger @click="handleDeleteCost">删除该成本</a-button>
      <a-button @click="emit('update:costOpen', false)">取消</a-button>
      <a-button type="primary" :loading="saving" @click="handleCreateCost"
        >保存更正</a-button
      >
    </template>
  </a-modal>

  <a-modal
    :open="editOpen"
    @update:open="emit('update:editOpen', $event)"
    title="编辑项目并重新提交"
    width="860px"
    :confirm-loading="saving"
    @ok="handleEdit"
  >
    <a-alert
      v-if="editProject"
      class="section-alert"
      type="warning"
      show-icon
      :message="`${editProject.project.code} · 当前状态 ${approvalLabel(editProject.project.approvalStatus)}，保存后将重新进入待审批`"
    />
    <a-form
      ref="editFormRef"
      :model="editForm"
      :rules="createRules"
      layout="vertical"
    >
      <a-row :gutter="16">
        <a-col :xs="24" :md="16"
          ><a-form-item label="项目名称" name="name"
            ><a-input v-model:value="editForm.name" /></a-form-item
        ></a-col>
        <a-col :span="24"
          ><a-form-item label="现场地址" name="siteAddress"
            ><a-input v-model:value="editForm.siteAddress" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="合同金额（含税，元）" name="contractAmount"
            ><a-input-number
              v-model:value="editForm.contractAmount"
              :min="0"
              :precision="2"
              class="full-input" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="计划开始" name="plannedStartDate"
            ><a-input
              v-model:value="editForm.plannedStartDate"
              type="date" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="计划结束" name="plannedEndDate"
            ><a-input
              v-model:value="editForm.plannedEndDate"
              type="date" /></a-form-item
        ></a-col>
        <a-col :xs="24" :md="8"
          ><a-form-item label="质保截止"
            ><a-input
              v-model:value="editForm.warrantyEndDate"
              type="date" /></a-form-item
        ></a-col>
      </a-row>
      <a-divider
        >分类预算合计（含税，元） ·
        {{ formatMoney(editBudgetTotal) }}</a-divider
      >
      <a-row :gutter="16">
        <a-col
          v-for="item in categoryOptions"
          :key="item.value"
          :xs="24"
          :md="8"
        >
          <a-form-item :label="`${item.label}预算（含税，元）`">
            <a-input-number
              v-model:value="editForm.budgets[item.value]"
              :min="0"
              :precision="2"
              class="full-input"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { message, Modal } from "ant-design-vue";
import { useRouter } from "vue-router";
import { toLocalDateString } from "@/utils/date";
import {
  createProject,
  createProjectCost,
  updateProjectCost,
  deleteProjectCost,
  updateProject,
  advanceProjectStage,
  assignProjectManager,
  type Project,
  type ProjectType,
  type ProjectDetail,
  type ProjectStage,
  type ProjectCostCategory,
  type ProjectCostSource,
  type ProjectCostEntry,
  type ProjectCloseoutReview,
  type ProjectBudgetItem,
} from "@/api/project";
import {
  getErrorMessage,
  openBudgetChangeRequest,
  showBudgetOverrunPrompt,
} from "@/utils/budget-overrun";

const router = useRouter();

interface SelectOption {
  value: string;
  label: string;
}

interface CategoryOption {
  value: ProjectCostCategory;
  label: string;
}

interface QuoteOption extends SelectOption {
  costRequest?: {
    laborCost?: number;
    materialCost?: number;
    subcontractCost?: number;
    travelCost?: number;
    equipmentCost?: number;
    riskReserve?: number;
    otherCost?: number;
  };
}

interface Props {
  createOpen: boolean;
  approvalOpen: boolean;
  stageOpen: boolean;
  costOpen: boolean;
  editCostOpen: boolean;
  editCostEntry?: ProjectCostEntry | null;
  editOpen: boolean;
  editProject?: ProjectDetail | null;
  quoteOptions: QuoteOption[];
  closeoutReview?: ProjectCloseoutReview | null;
  saving: boolean;
  customerOptions: SelectOption[];
  parentProjectOptions: SelectOption[];
  defaultParentProjectId?: string;
  defaultCustomerId?: string;
  categoryOptions: CategoryOption[];
  projectTypeOptions: SelectOption[];
  sourceOptions: SelectOption[];
  userOptions: SelectOption[];
  detail?: ProjectDetail | null;
  activeProject?: Project | null;
  nextStage?: ProjectStage | null;
}

const emit = defineEmits<{
  "update:createOpen": [value: boolean];
  "update:approvalOpen": [value: boolean];
  "update:stageOpen": [value: boolean];
  "update:costOpen": [value: boolean];
  "update:editCostOpen": [value: boolean];
  "update:editOpen": [value: boolean];
  created: [];
  updated: [];
}>();

const props = defineProps<Props>();

const createFormRef = ref();
const approvalFormRef = ref();
const stageFormRef = ref();
const costFormRef = ref();
const editFormRef = ref();
const editBudgetTotal = computed(() =>
  Object.values(editForm.budgets).reduce((s, v) => s + Number(v || 0), 0),
);

const createForm = reactive({
  customerId: "",
  parentProjectId: undefined as string | undefined,
  code: "",
  name: "",
  projectType: "RENOVATION" as ProjectType,
  managerUserId: undefined as string | undefined,
  siteAddress: "",
  contractAmount: 0,
  plannedStartDate: dateAfter(0),
  plannedEndDate: dateAfter(90),
  warrantyEndDate: dateAfter(455),
  quoteId: undefined as string | undefined,
  budgets: { LABOR: 0, MATERIAL: 0, SUBCONTRACT: 0, TRAVEL: 0, OTHER: 0 } as {
    [key: string]: number;
  },
});
const approvalForm = reactive({
  managerUserId: "",
  comment: "",
  syncChildProjects: true,
});
const stageForm = reactive({
  comment: "",
});
const editForm = reactive({
  name: "",
  siteAddress: "",
  contractAmount: 0,
  plannedStartDate: dateAfter(0),
  plannedEndDate: dateAfter(90),
  warrantyEndDate: "",
  budgets: { LABOR: 0, MATERIAL: 0, SUBCONTRACT: 0, TRAVEL: 0, OTHER: 0 } as {
    [key: string]: number;
  },
});
const costForm = reactive({
  costId: "",
  category: "LABOR" as ProjectCostCategory,
  sourceType: "MANUAL" as ProjectCostSource,
  sourceNo: "",
  description: "",
  amount: 0.01,
  incurredDate: dateAfter(0),
});

const createRules = {
  name: [{ required: true, message: "请输入项目名称" }],
  customerId: [{ required: true, message: "请选择客户" }],
  projectType: [{ required: true, message: "请选择项目类型" }],
  siteAddress: [{ required: true, message: "请输入现场地址" }],
  contractAmount: [{ required: true, message: "请输入合同金额" }],
  plannedStartDate: [{ required: true, message: "请选择计划开始日期" }],
  plannedEndDate: [{ required: true, message: "请选择计划结束日期" }],
};
const approvalRules = {
  managerUserId: [{ required: true, message: "请选择项目经理" }],
};
const stageRules = {
  comment: [{ required: true, message: "请输入节点说明" }],
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
const editingCost = computed(() => Boolean(props.editCostEntry));
const quoteOptions = computed(() => props.quoteOptions || []);
const userOptions = computed(() => props.userOptions || []);
const managerModalTitle = computed(() =>
  props.activeProject?.approvalStatus === "APPROVED"
    ? "变更项目负责人"
    : "分配项目经理",
);
const projectedCostAfterEntry = computed(() => {
  const base =
    Number(props.detail?.project?.actualCost || 0) -
    (editingCost.value ? Number(props.editCostEntry?.amount || 0) : 0);
  return base + Number(costForm.amount || 0);
});
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
  () => props.createOpen,
  (open) => {
    if (open) {
      Object.assign(createForm, {
        customerId: props.defaultCustomerId || "",
        parentProjectId: props.defaultParentProjectId || undefined,
        code: "",
        name: "",
        projectType: "RENOVATION",
        managerUserId: undefined,
        siteAddress: "",
        contractAmount: 0,
        plannedStartDate: dateAfter(0),
        plannedEndDate: dateAfter(90),
        warrantyEndDate: dateAfter(455),
        quoteId: undefined,
        budgets: { LABOR: 0, MATERIAL: 0, SUBCONTRACT: 0, TRAVEL: 0, OTHER: 0 },
      });
    }
  },
);

watch(
  () => props.costOpen,
  (open) => {
    if (open && !props.editCostEntry) {
      Object.assign(costForm, {
        costId: "",
        category: "LABOR",
        sourceType: "MANUAL",
        sourceNo: "",
        description: "",
        amount: 0.01,
        incurredDate: dateAfter(0),
      });
    }
  },
);

watch(
  () => props.editCostOpen,
  (open) => {
    if (!open) return;
    const entry = props.editCostEntry;
    if (!entry) {
      Object.assign(costForm, {
        costId: "",
        category: "LABOR",
        sourceType: "MANUAL",
        sourceNo: "",
        description: "",
        amount: 0.01,
        incurredDate: dateAfter(0),
      });
      return;
    }
    Object.assign(costForm, {
      costId: entry.id,
      category: entry.category,
      sourceType: entry.sourceType,
      sourceNo: entry.sourceNo || "",
      description: entry.description,
      amount: Number(entry.amount || 0),
      incurredDate: entry.incurredDate,
    });
  },
);

function applyQuoteBudget(quoteId?: string) {
  if (!quoteId) return;
  const option = quoteOptions.value.find(
    (item) => String(item.value) === String(quoteId),
  );
  const cost = option?.costRequest;
  if (!cost) return;
  createForm.budgets = {
    LABOR: Number(cost.laborCost || 0),
    MATERIAL: Number(cost.materialCost || 0),
    SUBCONTRACT: Number(cost.subcontractCost || 0),
    TRAVEL: Number(cost.travelCost || 0),
    OTHER:
      Number(cost.equipmentCost || 0) +
      Number(cost.riskReserve || 0) +
      Number(cost.otherCost || 0),
  };
}

watch(
  () => props.approvalOpen,
  (open) => {
    if (!open) return;
    approvalForm.managerUserId = String(
      props.activeProject?.managerUserId || "",
    );
    approvalForm.comment = "";
    approvalForm.syncChildProjects = true;
  },
);

function dateAfter(days: number) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return toLocalDateString(d);
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
  try {
    await createFormRef.value?.validate();
  } catch {
    return;
  }
  if (createBudgetTotal.value <= 0) {
    message.warning("请填写项目分类预算");
    return;
  }
  try {
    await createProject({
      customerId: createForm.customerId,
      parentProjectId: createForm.parentProjectId,
      code: createForm.code,
      name: createForm.name,
      projectType: createForm.projectType,
      managerUserId: createForm.managerUserId,
      siteAddress: createForm.siteAddress,
      contractAmount: createForm.contractAmount,
      plannedStartDate: createForm.plannedStartDate,
      plannedEndDate: createForm.plannedEndDate,
      warrantyEndDate: createForm.warrantyEndDate || undefined,
      budgetItems: props.categoryOptions.map((item) => ({
        category: item.value,
        plannedAmount: createForm.budgets[item.value],
        remark: item.label + "预算",
      })),
      quoteId: createForm.quoteId || undefined,
    });
    emit("created");
    emit("update:createOpen", false);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目新增失败");
  }
}

async function handleApproval() {
  if (!props.activeProject) return;
  try {
    await approvalFormRef.value?.validate();
  } catch {
    return;
  }
  try {
    await assignProjectManager(props.activeProject.id, {
      managerUserId: approvalForm.managerUserId,
      comment: approvalForm.comment || undefined,
      syncChildProjects: approvalForm.syncChildProjects,
    });
    emit("updated");
    emit("update:approvalOpen", false);
    message.success(
      props.activeProject.approvalStatus === "APPROVED"
        ? "项目负责人已变更"
        : "项目经理已分配",
    );
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目经理分配失败");
  }
}

async function handleAdvanceStage() {
  if (!props.detail || !props.nextStage) return;
  try {
    await stageFormRef.value?.validate();
  } catch {
    return;
  }
  try {
    await advanceProjectStage(props.detail.project.id, {
      targetStage: props.nextStage,
      comment: stageForm.comment,
    });
    emit("updated");
    emit("update:stageOpen", false);
    message.success("项目已进入" + stageLabel(props.nextStage) + "阶段");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目阶段推进失败");
  }
}

async function handleCreateCost() {
  if (!props.detail) return;
  try {
    await costFormRef.value?.validate();
  } catch {
    return;
  }
  if (costBudgetOverrun.value) {
    showBudgetOverrunPrompt(
      editingCost.value ? "更正后将超出项目预算" : "登记后将超出项目预算",
      router,
      props.detail.project.id,
    );
    return;
  }
  try {
    if (editingCost.value && costForm.costId) {
      await updateProjectCost(props.detail.project.id, costForm.costId, {
        category: costForm.category,
        description: costForm.description,
        amount: costForm.amount,
        incurredDate: costForm.incurredDate,
      });
      emit("updated");
      emit("update:costOpen", false);
      message.success("项目成本已更正");
    } else {
      await createProjectCost(props.detail.project.id, {
        category: costForm.category,
        sourceType: costForm.sourceType,
        sourceNo: costForm.sourceNo || undefined,
        description: costForm.description,
        amount: costForm.amount,
        incurredDate: costForm.incurredDate,
      });
      emit("updated");
      emit("update:costOpen", false);
      message.success("项目成本已归集");
    }
  } catch (error) {
    const msg = error instanceof Error ? error.message : "项目成本登记失败";
    const displayMessage = msg;
    if (
      !showBudgetOverrunPrompt(displayMessage, router, props.detail.project.id)
    ) {
      message.error(getErrorMessage(error, "项目成本登记失败"));
    }
  }
}

function handleDeleteCost() {
  if (!props.detail || !costForm.costId) return;
  Modal.confirm({
    title: "删除该成本明细？",
    content: "删除后项目实际成本与毛利会同步刷新，该操作不可撤销。",
    okText: "删除",
    okButtonProps: { danger: true },
    cancelText: "取消",
    onOk: async () => {
      try {
        await deleteProjectCost(props.detail!.project.id, costForm.costId);
        emit("updated");
        emit("update:costOpen", false);
        message.success("成本明细已删除");
      } catch (error) {
        message.error(error instanceof Error ? error.message : "成本删除失败");
      }
    },
  });
}

function goToBudgetChange() {
  if (!props.detail) return;
  void openBudgetChangeRequest(router, props.detail.project.id);
}

function approvalLabel(status: string) {
  return (
    (
      { PENDING: "待分配", APPROVED: "已分配", REJECTED: "已退回" } as Record<
        string,
        string
      >
    )[status] || status
  );
}

watch(
  () => props.editOpen,
  (open) => {
    if (!open || !props.editProject) return;
    const detail = props.editProject;
    const project = detail.project;
    const budgets: { [key: string]: number } = {
      LABOR: 0,
      MATERIAL: 0,
      SUBCONTRACT: 0,
      TRAVEL: 0,
      OTHER: 0,
    };
    (detail.budgetItems || []).forEach((item) => {
      budgets[item.category] = Number(item.plannedAmount || 0);
    });
    Object.assign(editForm, {
      name: project.name,
      siteAddress: project.siteAddress || "",
      contractAmount: Number(project.contractAmount || 0),
      plannedStartDate: project.plannedStartDate || dateAfter(0),
      plannedEndDate: project.plannedEndDate || dateAfter(90),
      warrantyEndDate: project.warrantyEndDate || "",
      budgets,
    });
  },
);

async function handleEdit() {
  if (!props.editProject) return;
  try {
    await editFormRef.value?.validate();
  } catch {
    return;
  }
  if (editBudgetTotal.value <= 0) {
    message.warning("请填写项目分类预算");
    return;
  }
  try {
    await updateProject(props.editProject.project.id, {
      name: editForm.name,
      siteAddress: editForm.siteAddress,
      contractAmount: editForm.contractAmount,
      plannedStartDate: editForm.plannedStartDate,
      plannedEndDate: editForm.plannedEndDate,
      warrantyEndDate: editForm.warrantyEndDate || undefined,
      budgetItems: props.categoryOptions.map((item) => ({
        category: item.value,
        plannedAmount: editForm.budgets[item.value],
        remark: item.label + "预算",
      })),
    });
    emit("updated");
    emit("update:editOpen", false);
    message.success("项目资料已更新，已重新提交审批");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目编辑失败");
  }
}
</script>
