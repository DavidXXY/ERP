<template>
  <div class="page-stack">
    <a-card :title="pageTitle">
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
        <a-col :xs="12" :lg="6"><a-statistic title="高风险项目" :value="highRiskProjectCount" suffix="个" :value-style="{color:highRiskProjectCount>0?'#ff4d4f':'#52c41a'}" /></a-col>
      </a-row>

      <template v-if="pageMode === 'list'">
      <section class="project-workbench">
        <div class="workbench-title">
          <div>
            <h3>项目利润复盘</h3>
            <p>关闭项目自动进入复盘视角，核对合同额、预算、实际成本和毛利。</p>
          </div>
          <a-tag :color="closedReviewRows.length ? 'blue' : 'default'">已关闭 {{ closedReviewRows.length }} 个</a-tag>
        </div>
        <div class="project-summary-grid">
          <button v-for="item in profitReviewCards" :key="item.label" class="summary-card" type="button" @click="item.action">
            <span>{{ item.label }}</span>
            <strong :class="{ 'text-danger': item.danger }">{{ item.value }}</strong>
            <em>{{ item.hint }}</em>
          </button>
        </div>
      </section>
      <a-table
        :columns="profitabilityColumns"
        :data-source="profitabilityHighlights"
        :loading="loading"
        :pagination="false"
        :row-key="(record: ProjectProfitability) => record.projectId"
        :scroll="{ x: 980 }"
        size="small"
        style="margin-bottom:16px"
      >
        <template #title>项目利润与预算风险</template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'project'">
            <a-button type="link" class="table-link" @click="openProfitabilityProject(record.projectId)">{{ record.projectName }}</a-button>
            <span class="table-subtitle">{{ record.projectCode || '-' }} · {{ record.customerName || '未关联客户' }}</span>
          </template>
          <template v-else-if="column.key === 'gross'">
            <strong :class="{ 'text-danger': record.grossMargin < 0 }">{{ formatMoney(record.grossMargin) }}</strong>
            <span class="table-subtitle">毛利率 {{ formatPercent(record.grossMarginRate) }}</span>
          </template>
          <template v-else-if="column.key === 'budget'">
            {{ formatMoney(record.actualCost) }}
            <span class="table-subtitle">预算使用 {{ formatPercent(record.budgetUsageRate) }}</span>
          </template>
          <template v-else-if="column.key === 'risk'">
            <a-tag :color="riskColor(record.riskLevel)">{{ riskLabel(record.riskLevel) }}</a-tag>
            <span class="table-subtitle">{{ record.riskMessage }}</span>
          </template>
        </template>
        <template #emptyText>暂无项目利润风险</template>
      </a-table>

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
      </template>

      <template v-else-if="pageMode === 'budget'">
        <section class="project-workbench">
          <div class="workbench-title">
            <div>
              <h3>预算执行概览</h3>
              <p>按项目跟踪预算使用、实际成本、预算余额和利润风险。</p>
            </div>
            <a-tag :color="budgetOverrunCount > 0 ? 'red' : 'green'">超预算 {{ budgetOverrunCount }} 个</a-tag>
          </div>
          <div class="project-summary-grid">
            <button v-for="item in budgetCards" :key="item.label" class="summary-card" type="button" @click="item.action">
              <span>{{ item.label }}</span>
              <strong :class="{ 'text-danger': item.danger }">{{ item.value }}</strong>
              <em>{{ item.hint }}</em>
            </button>
          </div>
        </section>
        <a-table
          :columns="budgetExecutionColumns"
          :data-source="budgetExecutionRows"
          :loading="loading"
          :pagination="{ pageSize: 10 }"
          :row-key="(record: ProjectBudgetRow) => record.project.id"
          :scroll="{ x: 1120 }"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'project'">
              <a-button type="link" class="table-link" @click="openDetail(record.project)">{{ record.project.name }}</a-button>
              <span class="table-subtitle">{{ record.project.code }} · {{ record.project.managerName }}</span>
            </template>
            <template v-else-if="column.key === 'budget'">
              <strong>{{ formatMoney(record.project.budgetAmount) }}</strong>
              <span class="table-subtitle">合同 {{ formatMoney(record.project.contractAmount) }}</span>
            </template>
            <template v-else-if="column.key === 'actual'">
              {{ formatMoney(record.project.actualCost) }}
              <a-progress :percent="Math.min(record.usageRate, 100)" size="small" :show-info="false" :stroke-color="record.usageRate >= 100 ? '#ff4d4f' : record.usageRate >= 85 ? '#faad14' : '#52c41a'" />
            </template>
            <template v-else-if="column.key === 'variance'"><span :class="{ 'text-danger': record.project.budgetVariance < 0 }">{{ formatMoney(record.project.budgetVariance) }}</span></template>
            <template v-else-if="column.key === 'risk'">
              <a-tag :color="riskColor(record.riskLevel)">{{ riskLabel(record.riskLevel) }}</a-tag>
              <span class="table-subtitle">{{ record.riskMessage }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" size="small" @click="openDetail(record.project)">查看预算</a-button>
            </template>
          </template>
        </a-table>
      </template>

      <template v-else-if="pageMode === 'costs'">
        <section class="project-workbench">
          <div class="workbench-title">
            <div>
              <h3>成本明细</h3>
              <p>汇总项目成本来源，快速定位人工、材料、外包等成本归集。</p>
            </div>
            <a-button :loading="detailHydrating" @click="hydrateProjectDetails(true)">刷新明细</a-button>
          </div>
          <div class="project-summary-grid">
            <button v-for="item in costCards" :key="item.label" class="summary-card" type="button" @click="item.action">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <em>{{ item.hint }}</em>
            </button>
          </div>
        </section>
        <a-table
          :columns="flatCostColumns"
          :data-source="flatCostEntries"
          :loading="loading || detailHydrating"
          :pagination="{ pageSize: 12 }"
          :row-key="(record: FlatCostEntry) => record.id"
          :scroll="{ x: 980 }"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'project'">
              <a-button type="link" class="table-link" @click="openDetail(record.project)">{{ record.project.name }}</a-button>
              <span class="table-subtitle">{{ record.project.code }}</span>
            </template>
            <template v-else-if="column.key === 'category'"><a-tag>{{ categoryLabel(record.category) }}</a-tag></template>
            <template v-else-if="column.key === 'source'">{{ sourceLabel(record.sourceType) }}<span class="table-subtitle">{{ record.sourceNo || '无来源单号' }}</span></template>
            <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
          </template>
          <template #emptyText>暂无成本明细，可进入项目详情登记成本</template>
        </a-table>
      </template>

      <template v-else>
        <section class="project-workbench">
          <div class="workbench-title">
            <div>
              <h3>阶段履历</h3>
              <p>按项目阶段查看推进分布和历史变更，关注长期停留与关闭进度。</p>
            </div>
            <a-button :loading="detailHydrating" @click="hydrateProjectDetails(true)">刷新履历</a-button>
          </div>
          <div class="stage-kanban-grid">
            <button v-for="item in stageCards" :key="item.stage" class="stage-card" type="button" @click="stageFilter = item.stage">
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
              <em>{{ item.amount }}</em>
            </button>
          </div>
          <div class="stage-gate-list">
            <div v-for="item in stageGateRules" :key="item.stage" class="stage-gate-row">
              <a-tag :color="stageColor(item.stage)">{{ stageLabel(item.stage) }}</a-tag>
              <span>{{ item.rule }}</span>
              <em>{{ item.owner }}</em>
            </div>
          </div>
        </section>
        <a-table
          :columns="flatStageColumns"
          :data-source="flatStageRecords"
          :loading="loading || detailHydrating"
          :pagination="{ pageSize: 12 }"
          :row-key="(record: FlatStageRecord) => record.id"
          :scroll="{ x: 980 }"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'project'">
              <a-button type="link" class="table-link" @click="openDetail(record.project)">{{ record.project.name }}</a-button>
              <span class="table-subtitle">{{ record.project.managerName }}</span>
            </template>
            <template v-else-if="column.key === 'change'">
              <a-tag :color="stageColor(record.fromStage)">{{ stageLabel(record.fromStage) }}</a-tag>
              →
              <a-tag :color="stageColor(record.toStage)">{{ stageLabel(record.toStage) }}</a-tag>
              <span class="table-subtitle">进度 {{ record.progress }}%</span>
            </template>
            <template v-else-if="column.key === 'operator'">{{ record.operatorName }}<span class="table-subtitle">{{ formatDateTime(record.changedAt) }}</span></template>
          </template>
          <template #emptyText>暂无阶段推进记录</template>
        </a-table>
      </template>
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
            <a-tab-pane key="trace" tab="经营链路">
              <BusinessTraceTimeline :project-detail="detail" />
            </a-tab-pane>
          </a-tabs>
        </template>
      </a-spin>
    </a-drawer>

        <ProjectModals
      v-model:create-open="createOpen"
      v-model:approval-open="approvalOpen"
      v-model:stage-open="stageOpen"
      v-model:cost-open="costOpen"
      :saving="saving"
      :customer-options="customerOptions"
      :category-options="categoryOptions"
      :project-type-options="projectTypeOptions"
      :source-options="sourceOptions"
      :detail="detail"
      :active-project="activeProject"
      :next-stage="nextStage"
      @created="createOpen = false; loadData()"
      @updated="loadData()"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { useRoute } from "vue-router";
import { listCustomers, type CustomerSummary } from "@/api/crm";
import { listProjects, getProject, listProjectProfitability } from "@/api/project";
import {
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
type ProjectProfitability,
} from "@/api/project";
import { useAuthStore } from "@/stores/auth";
import ProjectModals from "./ProjectModals.vue";
import BusinessTraceTimeline from "@/components/business/BusinessTraceTimeline.vue";


const auth = useAuthStore();
const route = useRoute();
const projects = ref<Project[]>([]);
const profitabilityRows = ref<ProjectProfitability[]>([]);
const detailCache = ref<Record<string, ProjectDetail>>({});
const pageMeta = ref({ totalElements: 0, totalPages: 0, number: 0, size: 999 });
const customers = ref<CustomerSummary[]>([]);
const detail = ref<ProjectDetail | null>(null);
const activeProject = ref<Project | null>(null);
const loading = ref(false);
const detailLoading = ref(false);
const detailHydrating = ref(false);
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
type PageMode = "list" | "budget" | "costs" | "stages";
type ProjectBudgetRow = { project: Project; riskLevel: string; riskMessage: string; usageRate: number };
type FlatCostEntry = ProjectCostEntry & { project: Project };
type FlatStageRecord = ProjectStageRecord & { project: Project };

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
const pageMode = computed<PageMode>(() => {
  if (route.path.endsWith("/budget")) return "budget";
  if (route.path.endsWith("/costs")) return "costs";
  if (route.path.endsWith("/stages")) return "stages";
  return "list";
});
const pageTitle = computed(() => ({ list: "项目列表", budget: "预算执行", costs: "成本明细", stages: "阶段履历" } as Record<PageMode, string>)[pageMode.value]);
const columns = [
  { title: "项目 / 客户", key: "name", width: 280 }, { title: "类型 / 负责人", key: "owner", width: 190 },
  { title: "阶段 / 进度", key: "stage", width: 170 }, { title: "立项审批", key: "approval", width: 220 },
  { title: "合同金额", key: "contract", width: 140 }, { title: "预算 / 实际", key: "cost", width: 180 },
  { title: "毛利 / 余额", key: "gross", width: 180 }, { title: "操作", key: "action", width: 130, fixed: "right" },
];
const profitabilityColumns = [
  { title: "项目 / 客户", key: "project", width: 280 }, { title: "毛利", key: "gross", width: 170 },
  { title: "实际成本 / 预算", key: "budget", width: 180 }, { title: "风险", key: "risk", width: 320 },
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
const budgetExecutionColumns = [
  { title: "项目 / 负责人", key: "project", width: 280 }, { title: "预算 / 合同", key: "budget", width: 190 },
  { title: "实际成本 / 使用率", key: "actual", width: 210 }, { title: "预算余额", key: "variance", width: 150 },
  { title: "风险", key: "risk", width: 300 }, { title: "操作", key: "action", width: 110, fixed: "right" as const },
];
const flatCostColumns = [
  { title: "项目", key: "project", width: 240 }, { title: "分类", key: "category", width: 120 },
  { title: "来源", key: "source", width: 180 }, { title: "成本说明", dataIndex: "description" },
  { title: "发生日期", dataIndex: "incurredDate", width: 120 }, { title: "金额", key: "amount", width: 140 },
];
const flatStageColumns = [
  { title: "项目", key: "project", width: 240 }, { title: "阶段变化", key: "change", width: 260 },
  { title: "节点说明", dataIndex: "comment" }, { title: "操作记录", key: "operator", width: 220 },
];

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
const pendingApprovalCount = computed(() => projects.value.filter((item) => item.approvalStatus === "PENDING").length);
const highRiskProjectCount = computed(() => profitabilityRows.value.filter((item) => item.riskLevel === "HIGH").length);
const budgetOverrunCount = computed(() => projects.value.filter((item) => Number(item.budgetVariance || 0) < 0).length);
const profitabilityHighlights = computed(() => [...profitabilityRows.value]
  .sort((a, b) => riskRank(b.riskLevel) - riskRank(a.riskLevel) || Number(b.budgetUsageRate || 0) - Number(a.budgetUsageRate || 0))
  .slice(0, 5));
const nextStage = computed<ProjectStage | null>(() => {
  if (!detail.value || detail.value.project.stage === "CLOSED") return null;
  return stageOptions[stageOptions.findIndex((item) => item.value === detail.value?.project.stage) + 1]?.value || null;
});
const profitabilityMap = computed(() => new Map(profitabilityRows.value.map((item) => [item.projectId, item])));
const budgetExecutionRows = computed<ProjectBudgetRow[]>(() => filteredProjects.value.map((project) => {
  const risk = profitabilityMap.value.get(project.id);
  return {
    project,
    riskLevel: risk?.riskLevel || (project.budgetVariance < 0 ? "HIGH" : "LOW"),
    riskMessage: risk?.riskMessage || (project.budgetVariance < 0 ? "实际成本已超过预算" : "预算执行正常"),
    usageRate: project.budgetAmount > 0 ? Math.round((Number(project.actualCost || 0) / Number(project.budgetAmount || 1)) * 100) : 0,
  };
}).sort((a, b) => b.usageRate - a.usageRate));
const detailRows = computed(() => Object.values(detailCache.value));
const flatCostEntries = computed<FlatCostEntry[]>(() => detailRows.value
  .flatMap((row) => row.costEntries.map((item) => ({ ...item, project: row.project })))
  .sort((a, b) => (b.incurredDate || "").localeCompare(a.incurredDate || "")));
const flatStageRecords = computed<FlatStageRecord[]>(() => detailRows.value
  .flatMap((row) => row.stageRecords.map((item) => ({ ...item, project: row.project })))
  .filter((item) => stageFilter.value === "ALL" || item.toStage === stageFilter.value || item.project.stage === stageFilter.value)
  .sort((a, b) => (b.changedAt || "").localeCompare(a.changedAt || "")));
const budgetCards = computed(() => [
  { label: "预算使用率", value: formatPercent(totalBudget.value ? totalActualCost.value / totalBudget.value * 100 : 0), hint: `${formatMoney(totalActualCost.value)} / ${formatMoney(totalBudget.value)}`, danger: totalActualCost.value > totalBudget.value, action: () => { stageFilter.value = "ALL"; } },
  { label: "预算余额", value: formatMoney(totalBudget.value - totalActualCost.value), hint: "全部项目合计", danger: totalActualCost.value > totalBudget.value, action: () => { approvalFilter.value = "ALL"; } },
  { label: "超预算项目", value: `${budgetOverrunCount.value} 个`, hint: "实际成本超过预算", danger: budgetOverrunCount.value > 0, action: () => { keyword.value = ""; } },
  { label: "高风险项目", value: `${highRiskProjectCount.value} 个`, hint: "利润或预算异常", danger: highRiskProjectCount.value > 0, action: () => { keyword.value = ""; } },
]);
const costCards = computed(() => [
  { label: "成本记录", value: `${flatCostEntries.value.length} 条`, hint: "已登记明细", action: () => hydrateProjectDetails(true) },
  { label: "成本总额", value: formatMoney(flatCostEntries.value.reduce((sum, item) => sum + Number(item.amount || 0), 0)), hint: "按明细汇总", action: () => {} },
  { label: "材料成本", value: formatMoney(flatCostEntries.value.filter((item) => item.category === "MATERIAL").reduce((sum, item) => sum + Number(item.amount || 0), 0)), hint: "仓库/采购相关", action: () => {} },
  { label: "外包成本", value: formatMoney(flatCostEntries.value.filter((item) => item.category === "SUBCONTRACT").reduce((sum, item) => sum + Number(item.amount || 0), 0)), hint: "外包与分包", action: () => {} },
]);
const stageCards = computed(() => stageOptions.map((stage) => {
  const rows = projects.value.filter((item) => item.stage === stage.value);
  return { stage: stage.value, label: stage.label, count: rows.length, amount: formatMoney(rows.reduce((sum, item) => sum + Number(item.contractAmount || 0), 0)) };
}));
const closedReviewRows = computed(() => projects.value.filter((item) => item.stage === "CLOSED"));
const profitReviewCards = computed(() => {
  const contractAmount = closedReviewRows.value.reduce((sum, item) => sum + Number(item.contractAmount || 0), 0);
  const actualCost = closedReviewRows.value.reduce((sum, item) => sum + Number(item.actualCost || 0), 0);
  const gross = closedReviewRows.value.reduce((sum, item) => sum + Number(item.grossMargin || 0), 0);
  const marginRate = contractAmount > 0 ? gross / contractAmount * 100 : 0;
  return [
    { label: "关闭项目", value: `${closedReviewRows.value.length} 个`, hint: "已完成复盘对象", danger: false, action: () => { stageFilter.value = "CLOSED"; } },
    { label: "合同额", value: formatMoney(contractAmount), hint: "关闭项目合计", danger: false, action: () => {} },
    { label: "实际成本", value: formatMoney(actualCost), hint: "采购/领料/人工/外包", danger: false, action: () => {} },
    { label: "毛利率", value: formatPercent(marginRate), hint: `毛利 ${formatMoney(gross)}`, danger: marginRate < 10, action: () => {} },
  ];
});
const stageGateRules = [
  { stage: "BIDDING" as ProjectStage, rule: "投标前校验客户、预算估算和报价毛利", owner: "销售/项目" },
  { stage: "ENTRY" as ProjectStage, rule: "进场前校验合同生效、项目审批和预算科目", owner: "项目经理" },
  { stage: "CONSTRUCTION" as ProjectStage, rule: "施工前确认采购计划、库存物料和安全资料", owner: "项目/采购" },
  { stage: "INITIAL_ACCEPTANCE" as ProjectStage, rule: "初验前确认成本归集、变更单和验收资料", owner: "项目经理" },
  { stage: "CLOSED" as ProjectStage, rule: "关闭前确认应收回款、成本完整和利润复盘", owner: "财务/项目" },
];

onMounted(loadData);
watch(() => route.path, () => {
  if (pageMode.value === "costs" || pageMode.value === "stages") hydrateProjectDetails();
});

async function loadData() {
  loading.value = true; errorMessage.value = "";
  try { const [projectPage, customerRows, profitability] = await Promise.all([listProjects(0, 999), listCustomers(), listProjectProfitability()]);
      projects.value = projectPage.content;
      pageMeta.value = { totalElements: projectPage.totalElements, totalPages: projectPage.totalPages, number: projectPage.number, size: projectPage.size };
      customers.value = customerRows;
      profitabilityRows.value = profitability;
      if (pageMode.value === "costs" || pageMode.value === "stages") await hydrateProjectDetails(); }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : "项目数据加载失败"; }
  finally { loading.value = false; }
}
function openCreate() { createOpen.value = true; }
function openApproval(project: Project) { activeProject.value = project; approvalOpen.value = true; }
function openStage() { if (!detail.value || !nextStage.value) return; stageOpen.value = true; }
function openCost() { costOpen.value = true; }

async function openDetail(project: Project) {
  detailOpen.value = true; detailLoading.value = true;
  try {
    detail.value = await getProject(project.id);
    detailCache.value = { ...detailCache.value, [project.id]: detail.value };
  }
  catch (error) { message.error(error instanceof Error ? error.message : "项目详情加载失败"); }
  finally { detailLoading.value = false; }
}
async function hydrateProjectDetails(force = false) {
  if (detailHydrating.value) return;
  detailHydrating.value = true;
  try {
    const targets = projects.value.filter((project) => force || !detailCache.value[project.id]).slice(0, 80);
    const details = await Promise.all(targets.map((project) => getProject(project.id).catch(() => null)));
    detailCache.value = {
      ...detailCache.value,
      ...Object.fromEntries(details.filter((item): item is ProjectDetail => Boolean(item)).map((item) => [item.project.id, item])),
    };
  } finally {
    detailHydrating.value = false;
  }
}

function openProfitabilityProject(projectId: string) {
  const project = projects.value.find((item) => item.id === projectId);
  if (project) openDetail(project);
}


function canExecute(project: Project) { return project.approvalStatus === "APPROVED" && project.stage !== "CLOSED"; }
function canAdvance(project: Project) { return canExecute(project) && project.stage !== "CLOSED"; }
function stageLabel(stage: ProjectStage) { return stageOptions.find((item) => item.value === stage)?.label || stage; }
function stageColor(stage: ProjectStage) { return ({ INITIATED: "blue", BIDDING: "cyan", ENTRY: "geekblue", CONSTRUCTION: "orange", COMMISSIONING: "purple", INITIAL_ACCEPTANCE: "gold", FINAL_ACCEPTANCE: "green", WARRANTY: "lime", CLOSED: "default" } as Record<ProjectStage, string>)[stage]; }
function approvalLabel(status: ProjectApprovalStatus) { return ({ PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" } as Record<ProjectApprovalStatus, string>)[status]; }
function approvalColor(status: ProjectApprovalStatus) { return ({ PENDING: "orange", APPROVED: "green", REJECTED: "red" } as Record<ProjectApprovalStatus, string>)[status]; }
function projectTypeLabel(type: ProjectType) { return projectTypeOptions.find((item) => item.value === type)?.label || type; }
function categoryLabel(category: ProjectCostCategory) { return categoryOptions.find((item) => item.value === category)?.label || category; }
function sourceLabel(source: ProjectCostSource) { return sourceOptions.find((item) => item.value === source)?.label || source; }
function riskRank(level: string) { return ({ HIGH: 3, MEDIUM: 2, LOW: 1 } as Record<string, number>)[level] || 0; }
function riskLabel(level: string) { return ({ HIGH: "高风险", MEDIUM: "关注", LOW: "正常" } as Record<string, string>)[level] || level; }
function riskColor(level: string) { return ({ HIGH: "red", MEDIUM: "orange", LOW: "green" } as Record<string, string>)[level] || "default"; }
function formatMoney(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0); }
function moneyFormatter(value: number | string) { return formatMoney(Number(value)); }
function formatPercent(value: number) { return `${Number(value || 0).toFixed(1)}%`; }
function formatDateTime(value: string) { return value ? new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "-"; }
</script>

<style scoped>
.drawer-metrics {
  margin: 20px 0 8px;
}
.project-workbench {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  background: #fbfcfe;
}
.workbench-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}
.workbench-title h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}
.workbench-title p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
}
.project-summary-grid,
.stage-kanban-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.stage-kanban-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}
.stage-gate-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}
.stage-gate-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
}
.stage-gate-row span {
  overflow: hidden;
  color: #344054;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.stage-gate-row em {
  color: #667085;
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
}
.summary-card,
.stage-card {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}
.summary-card:hover,
.stage-card:hover {
  border-color: #91caff;
  background: #f6faff;
}
.summary-card span,
.stage-card span {
  color: #667085;
  font-size: 12px;
}
.summary-card strong,
.stage-card strong {
  overflow: hidden;
  color: #101828;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.summary-card em,
.stage-card em {
  overflow: hidden;
  color: #98a2b3;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 900px) {
  .workbench-title {
    flex-direction: column;
  }
  .project-summary-grid,
  .stage-kanban-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .stage-gate-row {
    grid-template-columns: 1fr;
  }
}
</style>
