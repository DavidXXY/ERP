<template>
  <div class="page-stack">
    <a-card>
      <template #title>项目管理</template>
      <template #extra>
        <a-space>
          <a-button @click="loadData">刷新</a-button>
          <a-button v-if="auth.can('project:create')" type="primary" @click="createOpen = true">
            新增项目
          </a-button>
        </a-space>
      </template>

      <a-alert
        v-if="errorMessage"
        class="section-alert"
        type="warning"
        show-icon
        :message="errorMessage"
        description="项目接口需要后端服务和数据库迁移正常启动。"
      />

      <a-table
        :columns="columns"
        :data-source="projects"
        :loading="loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(record: Project) => record.id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <strong>{{ record.name }}</strong>
            <span class="table-subtitle">{{ record.code }} · {{ record.customerName || "未关联客户" }}</span>
          </template>
          <template v-else-if="column.key === 'stage'">
            <a-tag :color="stageColor(record.stage)">{{ stageLabel(record.stage) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'budget'">
            <strong>{{ formatMoney(record.budgetAmount) }}</strong>
            <span class="table-subtitle">已归集 {{ formatMoney(record.actualCost) }}</span>
          </template>
          <template v-else-if="column.key === 'gross'">
            <strong>{{ formatMoney(record.budgetAmount - record.actualCost) }}</strong>
          </template>
          <template v-else-if="column.key === 'progress'">
            <span>{{ record.progress }}%</span>
          </template>
          <template v-else-if="column.key === 'warranty'">
            {{ record.warrantyEndDate || "-" }}
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="createOpen"
      title="新增项目"
      width="760px"
      :confirm-loading="saving"
      @ok="handleCreate"
    >
      <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="项目编码" name="code">
              <a-input v-model:value="formState.code" placeholder="XM-2026-020" />
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item label="项目名称" name="name">
              <a-input v-model:value="formState.name" placeholder="客户现场改造 / 新建工程名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="关联客户">
              <a-select
                v-model:value="formState.customerId"
                :options="customerOptions"
                allow-clear
                show-search
                option-filter-prop="label"
                placeholder="选择客户"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="阶段">
              <a-select v-model:value="formState.stage" :options="stageOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="预算金额">
              <a-input-number v-model:value="formState.budgetAmount" :min="0" :precision="2" class="full-input" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="已归集成本">
              <a-input-number v-model:value="formState.actualCost" :min="0" :precision="2" class="full-input" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="进度">
              <a-input-number v-model:value="formState.progress" :min="0" :max="100" class="full-input" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="质保截止日期">
              <a-input v-model:value="formState.warrantyEndDate" type="date" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { listCustomers, type CustomerSummary } from "@/api/crm";
import {
  createProject,
  listProjects,
  type CreateProjectPayload,
  type Project,
  type ProjectStage,
} from "@/api/core-business";
import { useAuthStore } from "@/stores/auth";

type ProjectFormState = {
  customerId?: string;
  code: string;
  name: string;
  stage: ProjectStage;
  budgetAmount: number;
  actualCost: number;
  progress: number;
  warrantyEndDate?: string;
};

const auth = useAuthStore();
const projects = ref<Project[]>([]);
const customers = ref<CustomerSummary[]>([]);
const loading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const errorMessage = ref("");
const formRef = ref();
const formState = reactive<ProjectFormState>(initialForm());

const columns = [
  { title: "项目", key: "name", dataIndex: "name", width: 320 },
  { title: "阶段", key: "stage", dataIndex: "stage", width: 130 },
  { title: "预算 / 成本", key: "budget", width: 180 },
  { title: "预计毛利", key: "gross", width: 140 },
  { title: "进度", key: "progress", dataIndex: "progress", width: 100 },
  { title: "质保截止", key: "warranty", dataIndex: "warrantyEndDate", width: 140 },
];

const stageOptions = [
  { label: "立项", value: "INITIATED" },
  { label: "投标", value: "BIDDING" },
  { label: "进场", value: "ENTRY" },
  { label: "施工", value: "CONSTRUCTION" },
  { label: "调试", value: "COMMISSIONING" },
  { label: "初验", value: "INITIAL_ACCEPTANCE" },
  { label: "终验", value: "FINAL_ACCEPTANCE" },
  { label: "质保", value: "WARRANTY" },
  { label: "关闭", value: "CLOSED" },
];

const rules = {
  code: [{ required: true, message: "请输入项目编码" }],
  name: [{ required: true, message: "请输入项目名称" }],
};

const customerOptions = computed(() =>
  customers.value.map((customer) => ({
    label: `${customer.name} (${customer.code})`,
    value: customer.id,
  })),
);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [projectData, customerData] = await Promise.all([listProjects(), listCustomers()]);
    projects.value = projectData;
    customers.value = customerData;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "项目数据加载失败";
  } finally {
    loading.value = false;
  }
}

async function handleCreate() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload: CreateProjectPayload = {
      customerId: formState.customerId,
      code: formState.code,
      name: formState.name,
      stage: formState.stage,
      budgetAmount: formState.budgetAmount,
      actualCost: formState.actualCost,
      progress: formState.progress,
      warrantyEndDate: formState.warrantyEndDate || undefined,
    };
    await createProject(payload);
    Object.assign(formState, initialForm());
    createOpen.value = false;
    message.success("项目已新增");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目新增失败");
  } finally {
    saving.value = false;
  }
}

function initialForm(): ProjectFormState {
  return {
    code: "",
    name: "",
    stage: "INITIATED",
    budgetAmount: 0,
    actualCost: 0,
    progress: 0,
    warrantyEndDate: undefined,
  };
}

function stageLabel(stage: ProjectStage) {
  return stageOptions.find((option) => option.value === stage)?.label || stage;
}

function stageColor(stage: ProjectStage) {
  const colors: Record<ProjectStage, string> = {
    INITIATED: "blue",
    BIDDING: "cyan",
    ENTRY: "geekblue",
    CONSTRUCTION: "orange",
    COMMISSIONING: "purple",
    INITIAL_ACCEPTANCE: "gold",
    FINAL_ACCEPTANCE: "green",
    WARRANTY: "lime",
    CLOSED: "default",
  };
  return colors[stage];
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(value || 0);
}
</script>
