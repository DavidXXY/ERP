<template>
  <div class="page-stack">
    <a-card title="项目管理">
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="loadData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button v-if="auth.can('project:create')" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>
            新增项目
          </a-button>
        </a-space>
      </template>

      <a-alert v-if="errorMessage" class="section-alert" type="warning" show-icon :message="errorMessage" />

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="合同总额" :value="totalContract" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="项目预算" :value="totalBudget" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="已归集成本" :value="totalActualCost" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="当前毛利" :value="totalGrossMargin" :formatter="moneyFormatter" /></a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-input-search v-model:value="keyword" allow-clear placeholder="搜索项目、客户、负责人" style="width: 260px" />
        <a-select v-model:value="approvalFilter" :options="approvalFilterOptions" style="width: 140px" />
        <a-select v-model:value="stageFilter" :options="stageFilterOptions" style="width: 140px" />
        <a-tag color="orange">待审批 {{ pendingApprovalCount }}</a-tag>
      </a-space>

      <a-table
        :columns="columns"
        :data-source="filteredProjects"
        :loading="loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(record: Project) => record.id"
        :scroll="{ x: 1420 }"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-button type="link" class="table-link" @click="openDetail(record)">{{ record.name }}</a-button>
            <span class="table-subtitle">{{ record.code }} · {{ record.customerName || '未关联客户' }}</span>
          </template>
          <template v-else-if="column.key === 'owner'">
            {{ projectTypeLabel(record.projectType) }}
            <span class="table-subtitle">负责人 {{ record.managerName }}</span>
          </template>
          <template v-else-if="column.key === 'stage'">
            <a-tag :color="stageColor(record.stage)">{{ stageLabel(record.stage) }}</a-tag>
            <a-progress :percent="record.progress" size="small" :show-info="false" />
          </template>
          <template v-else-if="column.key === 'approval'">
            <a-tag :color="approvalColor(record.approvalStatus)">{{ approvalLabel(record.approvalStatus) }}</a-tag>
            <span v-if="record.approverName" class="table-subtitle">{{ record.approverName }} · {{ record.approvalComment }}</span>
          </template>
          <template v-else-if="column.key === 'contract'"><strong>{{ formatMoney(record.contractAmount) }}</strong></template>
          <template v-else-if="column.key === 'cost'">
            {{ formatMoney(record.budgetAmount) }}
            <span class="table-subtitle">实际 {{ formatMoney(record.actualCost) }}</span>
          </template>
          <template v-else-if="column.key === 'gross'">
            <strong :class="{ 'text-danger': record.grossMargin < 0 }">{{ formatMoney(record.grossMargin) }}</strong>
            <span class="table-subtitle">预算余额 {{ formatMoney(record.budgetVariance) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
              <a-button
                v-if="auth.can('project:approve') && record.approvalStatus === 'PENDING'"
                type="link"
                size="small"
                @click="openApproval(record)"
              >
                审批
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" :width="920" :destroy-on-close="false">
      <template #title>
        <div v-if="detail" class="detail-title">
          <span>{{ detail.project.name }}</span>
          <a-tag :color="stageColor(detail.project.stage)">{{ stageLabel(detail.project.stage) }}</a-tag>
          <a-tag :color="approvalColor(detail.project.approvalStatus)">{{ approvalLabel(detail.project.approvalStatus) }}</a-tag>
        </div>
        <span v-else>项目详情</span>
      </template>
      <template #extra>
        <a-space v-if="detail">
          <a-button
            v-if="auth.can('project:approve') && detail.project.approvalStatus === 'PENDING'"
            @click="openApproval(detail.project)"
          >
            立项审批
          </a-button>
          <a-button
            v-if="auth.can('project:cost:create') && canExecute(detail.project)"
            @click="openCost"
          >
            登记成本
          </a-button>
          <a-button
            v-if="auth.can('project:stage:update') && canAdvance(detail.project)"
            type="primary"
            @click="openStage"
          >
            推进阶段
          </a-button>
        </a-space>
      </template>

      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="项目编码">{{ detail.project.code }}</a-descriptions-item>
            <a-descriptions-item label="项目类型">{{ projectTypeLabel(detail.project.projectType) }}</a-descriptions-item>
            <a-descriptions-item label="客户">{{ detail.project.customerName }}</a-descriptions-item>
            <a-descriptions-item label="项目负责人">{{ detail.project.managerName }}</a-descriptions-item>
            <a-descriptions-item label="现场地址" :span="2">{{ detail.project.siteAddress }}</a-descriptions-item>
            <a-descriptions-item label="计划周期">{{ detail.project.plannedStartDate }} 至 {{ detail.project.plannedEndDate }}</a-descriptions-item>
            <a-descriptions-item label="质保截止">{{ detail.project.warrantyEndDate || '-' }}</a-descriptions-item>
            <a-descriptions-item label="合同金额">{{ formatMoney(detail.project.contractAmount) }}</a-descriptions-item>
            <a-descriptions-item label="当前毛利">{{ formatMoney(detail.project.grossMargin) }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.project.approvalComment" label="审批记录" :span="2">
              {{ detail.project.approverName }} · {{ detail.project.approvalComment }}
            </a-descriptions-item>
          </a-descriptions>

          <a-row :gutter="[16, 16]" class="drawer-metrics">
            <a-col :span="8"><a-statistic title="预算成本" :value="detail.project.budgetAmount" :formatter="moneyFormatter" /></a-col>
            <a-col :span="8"><a-statistic title="实际成本" :value="detail.project.actualCost" :formatter="moneyFormatter" /></a-col>
            <a-col :span="8"><a-statistic title="预算余额" :value="detail.project.budgetVariance" :formatter="moneyFormatter" /></a-col>
          </a-row>

          <a-tabs>
            <a-tab-pane key="budget" tab="预算执行">
              <a-table :columns="budgetColumns" :data-source="detail.budgetItems" :pagination="false" :row-key="(item: ProjectBudgetItem) => item.id" size="small">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'category'">{{ categoryLabel(record.category) }}</template>
                  <template v-else-if="column.key === 'planned'">{{ formatMoney(record.plannedAmount) }}</template>
                  <template v-else-if="column.key === 'actual'">{{ formatMoney(record.actualAmount) }}</template>
                  <template v-else-if="column.key === 'variance'"><span :class="{ 'text-danger': record.variance < 0 }">{{ formatMoney(record.variance) }}</span></template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="costs" :tab="`成本明细 (${detail.costEntries.length})`">
              <a-table :columns="costColumns" :data-source="detail.costEntries" :pagination="{ pageSize: 6 }" :row-key="(item: ProjectCostEntry) => item.id" size="small">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'category'"><a-tag>{{ categoryLabel(record.category) }}</a-tag></template>
                  <template v-else-if="column.key === 'source'">{{ sourceLabel(record.sourceType) }}<span class="table-subtitle">{{ record.sourceNo || '无来源单号' }}</span></template>
                  <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
                </template>
                <template #emptyText>暂无成本明细</template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="stages" :tab="`阶段履历 (${detail.stageRecords.length})`">
              <a-table :columns="stageRecordColumns" :data-source="detail.stageRecords" :pagination="{ pageSize: 6 }" :row-key="(item: ProjectStageRecord) => item.id" size="small">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'change'">{{ stageLabel(record.fromStage) }} → {{ stageLabel(record.toStage) }}<span class="table-subtitle">进度 {{ record.progress }}%</span></template>
                  <template v-else-if="column.key === 'operator'">{{ record.operatorName }}<span class="table-subtitle">{{ formatDateTime(record.changedAt) }}</span></template>
                </template>
                <template #emptyText>项目尚未推进阶段</template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
        </template>
      </a-spin>
    </a-drawer>

    <a-modal v-model:open="createOpen" title="新增项目立项" width="860px" :confirm-loading="saving" @ok="handleCreate">
      <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="项目编码" name="code"><a-input v-model:value="createForm.code" placeholder="XM-2026-020" /></a-form-item></a-col>
          <a-col :xs="24" :md="16"><a-form-item label="项目名称" name="name"><a-input v-model:value="createForm.name" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="关联客户" name="customerId"><a-select v-model:value="createForm.customerId" :options="customerOptions" show-search option-filter-prop="label" /></a-form-item></a-col>
          <a-col :xs="24" :md="6"><a-form-item label="项目类型" name="projectType"><a-select v-model:value="createForm.projectType" :options="projectTypeOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="6"><a-form-item label="项目负责人" name="managerName"><a-input v-model:value="createForm.managerName" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="现场地址" name="siteAddress"><a-input v-model:value="createForm.siteAddress" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="合同金额" name="contractAmount"><a-input-number v-model:value="createForm.contractAmount" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="计划开始" name="plannedStartDate"><a-input v-model:value="createForm.plannedStartDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="计划结束" name="plannedEndDate"><a-input v-model:value="createForm.plannedEndDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="质保截止"><a-input v-model:value="createForm.warrantyEndDate" type="date" /></a-form-item></a-col>
        </a-row>
        <a-divider>分类预算 · 合计 {{ formatMoney(createBudgetTotal) }}</a-divider>
        <a-row :gutter="16">
          <a-col v-for="item in categoryOptions" :key="item.value" :xs="24" :md="8">
            <a-form-item :label="item.label">
              <a-input-number v-model:value="createForm.budgets[item.value]" :min="0" :precision="2" class="full-input" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="approvalOpen" title="项目立项审批" width="700px" :confirm-loading="saving" @ok="handleApproval">
      <a-alert v-if="activeProject" class="section-alert" type="info" :message="`${activeProject.code} · ${activeProject.name} · ${formatMoney(activeProject.contractAmount)}`" />
      <a-form ref="approvalFormRef" :model="approvalForm" :rules="approvalRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="10"><a-form-item label="审批结论" name="decision"><a-radio-group v-model:value="approvalForm.decision" button-style="solid"><a-radio-button value="APPROVED">通过</a-radio-button><a-radio-button value="REJECTED">驳回</a-radio-button></a-radio-group></a-form-item></a-col>
          <a-col :xs="24" :md="14"><a-form-item label="审批人" name="approverName"><a-input v-model:value="approvalForm.approverName" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="审批意见" name="comment"><a-textarea v-model:value="approvalForm.comment" :rows="3" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="stageOpen" title="推进项目阶段" width="700px" :confirm-loading="saving" @ok="handleAdvanceStage">
      <a-alert v-if="detail && nextStage" class="section-alert" type="info" :message="`${stageLabel(detail.project.stage)} → ${stageLabel(nextStage)}`" />
      <a-form ref="stageFormRef" :model="stageForm" :rules="stageRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="目标阶段"><a-input :value="nextStage ? stageLabel(nextStage) : ''" disabled /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="完成进度" name="progress"><a-input-number v-model:value="stageForm.progress" :min="detail?.project.progress || 0" :max="100" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="操作人" name="operatorName"><a-input v-model:value="stageForm.operatorName" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="节点说明" name="comment"><a-textarea v-model:value="stageForm.comment" :rows="3" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="costOpen" title="登记项目成本" width="760px" :confirm-loading="saving" @ok="handleCreateCost">
      <a-alert v-if="detail" class="section-alert" type="info" :message="`${detail.project.code} · 预算余额 ${formatMoney(detail.project.budgetVariance)}`" />
      <a-form ref="costFormRef" :model="costForm" :rules="costRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="成本分类" name="category"><a-select v-model:value="costForm.category" :options="categoryOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="来源类型" name="sourceType"><a-select v-model:value="costForm.sourceType" :options="sourceOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="发生日期" name="incurredDate"><a-input v-model:value="costForm.incurredDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="10"><a-form-item label="来源单号"><a-input v-model:value="costForm.sourceNo" placeholder="报销单、外包单、领料单" /></a-form-item></a-col>
          <a-col :xs="24" :md="14"><a-form-item label="成本说明" name="description"><a-input v-model:value="costForm.description" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="成本金额" name="amount"><a-input-number v-model:value="costForm.amount" :min="0.01" :precision="2" class="full-input" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listCustomers, type CustomerSummary } from "@/api/crm";
import {
  advanceProjectStage,
  createProject,
  createProjectCost,
  getProject,
  listProjects,
  processProjectApproval,
  type CreateProjectPayload,
  type Project,
  type ProjectApprovalStatus,
  type ProjectBudgetItem,
  type ProjectCostCategory,
  type ProjectCostEntry,
  type ProjectCostSource,
  type ProjectDetail,
  type ProjectStage,
  type ProjectStageRecord,
  type ProjectType,
} from "@/api/core-business";
import { useAuthStore } from "@/stores/auth";

type ProjectCreateForm = Omit<CreateProjectPayload, "budgetItems"> & { budgets: Record<ProjectCostCategory, number> };
type ApprovalForm = { decision: ProjectApprovalStatus; comment: string; approverName: string };
type StageForm = { progress: number; comment: string; operatorName: string };
type CostForm = { category: ProjectCostCategory; sourceType: ProjectCostSource; sourceNo: string; description: string; amount: number; incurredDate: string };

const auth = useAuthStore();
const projects = ref<Project[]>([]);
const customers = ref<CustomerSummary[]>([]);
const detail = ref<ProjectDetail | null>(null);
const activeProject = ref<Project | null>(null);
const loading = ref(false);
const detailLoading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const detailOpen = ref(false);
const approvalOpen = ref(false);
const stageOpen = ref(false);
const costOpen = ref(false);
const errorMessage = ref("");
const keyword = ref("");
const approvalFilter = ref("ALL");
const stageFilter = ref("ALL");
const createFormRef = ref();
const approvalFormRef = ref();
const stageFormRef = ref();
const costFormRef = ref();
const createForm = reactive<ProjectCreateForm>(initialCreateForm());
const approvalForm = reactive<ApprovalForm>(initialApprovalForm());
const stageForm = reactive<StageForm>(initialStageForm());
const costForm = reactive<CostForm>(initialCostForm());

const stageOptions: Array<{ label: string; value: ProjectStage }> = [
  { label: "立项", value: "INITIATED" }, { label: "投标", value: "BIDDING" },
  { label: "进场", value: "ENTRY" }, { label: "施工", value: "CONSTRUCTION" },
  { label: "调试", value: "COMMISSIONING" }, { label: "初验", value: "INITIAL_ACCEPTANCE" },
  { label: "终验", value: "FINAL_ACCEPTANCE" }, { label: "质保", value: "WARRANTY" },
  { label: "关闭", value: "CLOSED" },
];
const projectTypeOptions = [
  { label: "新建工程", value: "NEW_CONSTRUCTION" },
  { label: "改造工程", value: "RENOVATION" },
  { label: "升级改造", value: "O_M_RENOVATION" },
];
const categoryOptions: Array<{ label: string; value: ProjectCostCategory }> = [
  { label: "人工", value: "LABOR" }, { label: "材料物料", value: "MATERIAL" },
  { label: "外包", value: "SUBCONTRACT" }, { label: "差旅", value: "TRAVEL" },
  { label: "其他", value: "OTHER" },
];
const sourceOptions = [
  { label: "手工登记", value: "MANUAL" }, { label: "仓库领料", value: "INVENTORY" },
  { label: "采购入库（自动）", value: "PROCUREMENT", disabled: true },
  { label: "费用报销", value: "EXPENSE" }, { label: "外包结算", value: "SUBCONTRACT" },
];
const approvalFilterOptions = [
  { label: "全部审批", value: "ALL" }, { label: "待审批", value: "PENDING" },
  { label: "已通过", value: "APPROVED" }, { label: "已驳回", value: "REJECTED" },
];
const stageFilterOptions = computed(() => [{ label: "全部阶段", value: "ALL" }, ...stageOptions]);
const columns = [
  { title: "项目 / 客户", key: "name", width: 280 }, { title: "类型 / 负责人", key: "owner", width: 190 },
  { title: "阶段 / 进度", key: "stage", width: 170 }, { title: "立项审批", key: "approval", width: 220 },
  { title: "合同金额", key: "contract", width: 140 }, { title: "预算 / 实际", key: "cost", width: 180 },
  { title: "毛利 / 余额", key: "gross", width: 180 }, { title: "操作", key: "action", width: 130, fixed: "right" },
];
const budgetColumns = [
  { title: "成本分类", key: "category", width: 140 }, { title: "预算金额", key: "planned", width: 150 },
  { title: "实际发生", key: "actual", width: 150 }, { title: "预算余额", key: "variance", width: 150 },
  { title: "备注", dataIndex: "remark" },
];
const costColumns = [
  { title: "分类", key: "category", width: 110 }, { title: "来源", key: "source", width: 170 },
  { title: "成本说明", dataIndex: "description" }, { title: "发生日期", dataIndex: "incurredDate", width: 120 },
  { title: "金额", key: "amount", width: 130 },
];
const stageRecordColumns = [
  { title: "阶段变化", key: "change", width: 220 }, { title: "节点说明", dataIndex: "comment" },
  { title: "操作记录", key: "operator", width: 190 },
];
const createRules = {
  code: [{ required: true, message: "请输入项目编码" }], name: [{ required: true, message: "请输入项目名称" }],
  customerId: [{ required: true, message: "请选择客户" }], projectType: [{ required: true, message: "请选择项目类型" }],
  managerName: [{ required: true, message: "请输入项目负责人" }], siteAddress: [{ required: true, message: "请输入现场地址" }],
  contractAmount: [{ required: true, message: "请输入合同金额" }], plannedStartDate: [{ required: true, message: "请选择计划开始日期" }],
  plannedEndDate: [{ required: true, message: "请选择计划结束日期" }],
};
const approvalRules = { decision: [{ required: true }], comment: [{ required: true, message: "请输入审批意见" }], approverName: [{ required: true, message: "请输入审批人" }] };
const stageRules = { progress: [{ required: true, message: "请输入完成进度" }], comment: [{ required: true, message: "请输入节点说明" }], operatorName: [{ required: true, message: "请输入操作人" }] };
const costRules = { category: [{ required: true }], sourceType: [{ required: true }], incurredDate: [{ required: true }], description: [{ required: true, message: "请输入成本说明" }], amount: [{ required: true, message: "请输入成本金额" }] };

const filteredProjects = computed(() => projects.value.filter((item) => {
  const search = keyword.value.trim().toLowerCase();
  const textMatched = !search || [item.code, item.name, item.customerName, item.managerName].some((value) => value?.toLowerCase().includes(search));
  return textMatched && (approvalFilter.value === "ALL" || item.approvalStatus === approvalFilter.value) && (stageFilter.value === "ALL" || item.stage === stageFilter.value);
}));
const customerOptions = computed(() => customers.value.map((item) => ({
  label: `${item.name} (${item.code})`,
  value: item.id,
})));
const totalContract = computed(() => projects.value.reduce((sum, item) => sum + Number(item.contractAmount || 0), 0));
const totalBudget = computed(() => projects.value.reduce((sum, item) => sum + Number(item.budgetAmount || 0), 0));
const totalActualCost = computed(() => projects.value.reduce((sum, item) => sum + Number(item.actualCost || 0), 0));
const totalGrossMargin = computed(() => projects.value.reduce((sum, item) => sum + Number(item.grossMargin || 0), 0));
const pendingApprovalCount = computed(() => projects.value.filter((item) => item.approvalStatus === "PENDING").length);
const createBudgetTotal = computed(() => Object.values(createForm.budgets).reduce((sum, value) => sum + Number(value || 0), 0));
const nextStage = computed<ProjectStage | null>(() => {
  if (!detail.value || detail.value.project.stage === "CLOSED") return null;
  return stageOptions[stageOptions.findIndex((item) => item.value === detail.value?.project.stage) + 1]?.value || null;
});

onMounted(loadData);

async function loadData() {
  loading.value = true; errorMessage.value = "";
  try { [projects.value, customers.value] = await Promise.all([listProjects(), listCustomers()]); }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : "项目数据加载失败"; }
  finally { loading.value = false; }
}
function openCreate() { Object.assign(createForm, initialCreateForm()); createOpen.value = true; }
async function openDetail(project: Project) {
  detailOpen.value = true; detailLoading.value = true;
  try { detail.value = await getProject(project.id); }
  catch (error) { message.error(error instanceof Error ? error.message : "项目详情加载失败"); }
  finally { detailLoading.value = false; }
}
function openApproval(project: Project) { activeProject.value = project; Object.assign(approvalForm, initialApprovalForm()); approvalOpen.value = true; }
function openStage() {
  if (!detail.value || !nextStage.value) return;
  Object.assign(stageForm, initialStageForm(), { progress: suggestedProgress(nextStage.value), comment: `进入${stageLabel(nextStage.value)}阶段` });
  stageOpen.value = true;
}
function openCost() { Object.assign(costForm, initialCostForm()); costOpen.value = true; }

async function handleCreate() {
  await createFormRef.value?.validate();
  if (createBudgetTotal.value <= 0) { message.warning("请填写项目分类预算"); return; }
  saving.value = true;
  try {
    await createProject({
      customerId: createForm.customerId, code: createForm.code, name: createForm.name, projectType: createForm.projectType,
      managerName: createForm.managerName, siteAddress: createForm.siteAddress, contractAmount: createForm.contractAmount,
      plannedStartDate: createForm.plannedStartDate, plannedEndDate: createForm.plannedEndDate,
      warrantyEndDate: createForm.warrantyEndDate || undefined,
      budgetItems: categoryOptions.map((item) => ({ category: item.value, plannedAmount: createForm.budgets[item.value], remark: `${item.label}预算` })),
    });
    createOpen.value = false; message.success("项目立项已提交审批"); await loadData();
  } catch (error) { message.error(error instanceof Error ? error.message : "项目新增失败"); }
  finally { saving.value = false; }
}
async function handleApproval() {
  if (!activeProject.value) return;
  await approvalFormRef.value?.validate(); saving.value = true;
  try {
    detail.value = await processProjectApproval(activeProject.value.id, { ...approvalForm });
    approvalOpen.value = false; message.success(approvalForm.decision === "APPROVED" ? "项目立项已审批通过" : "项目立项已驳回"); await loadData();
  } catch (error) { message.error(error instanceof Error ? error.message : "项目审批失败"); }
  finally { saving.value = false; }
}
async function handleAdvanceStage() {
  if (!detail.value || !nextStage.value) return;
  await stageFormRef.value?.validate(); saving.value = true;
  try {
    detail.value = await advanceProjectStage(detail.value.project.id, { targetStage: nextStage.value, ...stageForm });
    stageOpen.value = false; message.success(`项目已进入${stageLabel(detail.value.project.stage)}阶段`); await loadData();
  } catch (error) { message.error(error instanceof Error ? error.message : "项目阶段推进失败"); }
  finally { saving.value = false; }
}
async function handleCreateCost() {
  if (!detail.value) return;
  await costFormRef.value?.validate(); saving.value = true;
  try {
    detail.value = await createProjectCost(detail.value.project.id, { ...costForm, sourceNo: costForm.sourceNo || undefined });
    costOpen.value = false; message.success("项目成本已归集"); await loadData();
  } catch (error) { message.error(error instanceof Error ? error.message : "项目成本登记失败"); }
  finally { saving.value = false; }
}

function initialCreateForm(): ProjectCreateForm {
  return {
    customerId: "", code: "", name: "", projectType: "RENOVATION", managerName: auth.user?.displayName || "",
    siteAddress: "", contractAmount: 0, plannedStartDate: dateAfter(0), plannedEndDate: dateAfter(90), warrantyEndDate: dateAfter(455),
    budgets: { LABOR: 0, MATERIAL: 0, SUBCONTRACT: 0, TRAVEL: 0, OTHER: 0 },
  };
}
function initialApprovalForm(): ApprovalForm { return { decision: "APPROVED", comment: "同意立项，按预算执行", approverName: auth.user?.displayName || "" }; }
function initialStageForm(): StageForm { return { progress: detail.value?.project.progress || 0, comment: "", operatorName: auth.user?.displayName || "" }; }
function initialCostForm(): CostForm { return { category: "LABOR", sourceType: "MANUAL", sourceNo: "", description: "", amount: 0.01, incurredDate: dateAfter(0) }; }
function dateAfter(days: number) { const value = new Date(); value.setDate(value.getDate() + days); return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`; }
function canExecute(project: Project) { return project.approvalStatus === "APPROVED" && project.stage !== "CLOSED"; }
function canAdvance(project: Project) { return canExecute(project) && project.stage !== "CLOSED"; }
function suggestedProgress(stage: ProjectStage) { return ({ INITIATED: 0, BIDDING: 10, ENTRY: 20, CONSTRUCTION: 45, COMMISSIONING: 70, INITIAL_ACCEPTANCE: 82, FINAL_ACCEPTANCE: 92, WARRANTY: 98, CLOSED: 100 } as Record<ProjectStage, number>)[stage]; }
function stageLabel(stage: ProjectStage) { return stageOptions.find((item) => item.value === stage)?.label || stage; }
function stageColor(stage: ProjectStage) { return ({ INITIATED: "blue", BIDDING: "cyan", ENTRY: "geekblue", CONSTRUCTION: "orange", COMMISSIONING: "purple", INITIAL_ACCEPTANCE: "gold", FINAL_ACCEPTANCE: "green", WARRANTY: "lime", CLOSED: "default" } as Record<ProjectStage, string>)[stage]; }
function approvalLabel(status: ProjectApprovalStatus) { return ({ PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" } as Record<ProjectApprovalStatus, string>)[status]; }
function approvalColor(status: ProjectApprovalStatus) { return ({ PENDING: "orange", APPROVED: "green", REJECTED: "red" } as Record<ProjectApprovalStatus, string>)[status]; }
function projectTypeLabel(type: ProjectType) { return projectTypeOptions.find((item) => item.value === type)?.label || type; }
function categoryLabel(category: ProjectCostCategory) { return categoryOptions.find((item) => item.value === category)?.label || category; }
function sourceLabel(source: ProjectCostSource) { return sourceOptions.find((item) => item.value === source)?.label || source; }
function formatMoney(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0); }
function moneyFormatter(value: number | string) { return formatMoney(Number(value)); }
function formatDateTime(value: string) { return value ? new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "-"; }
</script>

<style scoped>
.drawer-metrics {
  margin: 20px 0 8px;
}
</style>
