<template>
  <BusinessDetailPage
    :title="detail?.project.name"
    :code="detail?.project.code"
    :subtitle="
      detail
        ? `${detail.project.customerName || '未关联客户'} · 项目经理 ${detail.project.managerName || '待分配'}`
        : ''
    "
    :loading="loading"
    back-to="/projects/list"
    :status-label="stageLabel(detail?.project.stage)"
    :status-color="detail?.project.stage === 'CLOSED' ? 'default' : 'blue'"
    :risk-label="riskLabel"
    :risk-color="riskColor"
    :stats="metrics"
    @refresh="loadData"
  >
    <template #actions>
      <a-button
        v-if="detail?.project.customerId"
        @click="router.push(`/crm/customers/${detail.project.customerId}`)"
        >查看客户</a-button
      >
      <a-button
        v-if="detail?.project.contractId"
        @click="router.push(`/crm/contracts/${detail.project.contractId}`)"
        >查看合同</a-button
      >
      <a-button v-if="canEditProject" type="primary" @click="openPreparation">{{
        detail?.project.parentProjectId ? "完善立项资料" : "编辑并重新提交"
      }}</a-button>
      <a-button
        v-if="auth.can('procurement:purchase:create')"
        @click="router.push(`/procurement/requests?projectId=${projectId}`)"
        >发起采购</a-button
      >
    </template>
    <template #relations>
      <a-steps size="small" :current="stageIndex" responsive>
        <a-step
          title="合同承接"
          :description="detail?.project.contractCode || '独立立项'"
        />
        <a-step
          title="预算审批"
          :description="approvalLabel(detail?.project.approvalStatus)"
        />
        <a-step title="采购执行" :description="`${orders.length} 笔订单`" />
        <a-step
          title="交付验收"
          :description="stageLabel(detail?.project.stage)"
        />
        <a-step
          title="质保结项"
          :description="detail?.project.warrantyEndDate || '待维护'"
        />
      </a-steps>
    </template>

    <a-card v-if="detail" :bordered="false">
      <a-tabs>
        <a-tab-pane key="overview" tab="项目总览">
          <a-descriptions
            bordered
            :column="{ xs: 1, md: 2, xl: 3 }"
            size="small"
          >
            <a-descriptions-item label="项目类型">{{
              typeLabel(detail.project.projectType)
            }}</a-descriptions-item>
            <a-descriptions-item label="客户">{{
              detail.project.customerName || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="项目经理">{{
              detail.project.managerName || "待分配"
            }}</a-descriptions-item>
            <a-descriptions-item label="经理分配记录">
              {{ detail.project.managerAssignedByName || "-" }}
              <span v-if="detail.project.managerAssignedAt">
                · {{ dateTime(detail.project.managerAssignedAt) }}</span
              >
              <span v-if="detail.project.managerAssignmentComment">
                · {{ detail.project.managerAssignmentComment }}</span
              >
            </a-descriptions-item>
            <a-descriptions-item label="现场地址" :span="2">{{
              detail.project.siteAddress || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="项目进度"
              ><a-progress :percent="detail.project.progress" size="small"
            /></a-descriptions-item>
            <a-descriptions-item label="计划周期"
              >{{ detail.project.plannedStartDate || "-" }} 至
              {{ detail.project.plannedEndDate || "-" }}</a-descriptions-item
            >
            <a-descriptions-item label="实际周期"
              >{{ detail.project.actualStartDate || "未开始" }} 至
              {{ detail.project.actualEndDate || "-" }}</a-descriptions-item
            >
            <a-descriptions-item label="质保截止">{{
              detail.project.warrantyEndDate || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="审批状态">{{
              approvalLabel(detail.project.approvalStatus)
            }}</a-descriptions-item>
            <a-descriptions-item label="执行状态">
              <a-tag :color="executionColor(detail.project.executionStatus)">{{
                executionLabel(detail.project.executionStatus)
              }}</a-tag>
              <span v-if="detail.project.statusComment">{{
                detail.project.statusComment
              }}</span>
            </a-descriptions-item>
          </a-descriptions>
          <a-alert
            v-if="riskLabel !== '经营正常'"
            class="section-gap"
            type="warning"
            show-icon
            :message="riskLabel"
            :description="riskDescription"
          />
        </a-tab-pane>

        <a-tab-pane
          key="budget"
          :tab="`预算执行 (${detail.budgetItems.length})`"
        >
          <a-table
            :columns="budgetColumns"
            :data-source="detail.budgetItems"
            row-key="id"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'category'">{{
                categoryLabel(record.category)
              }}</template>
              <template
                v-else-if="
                  ['planned', 'actual', 'variance'].includes(column.key)
                "
                ><span
                  :class="{
                    danger: column.key === 'variance' && record.variance < 0,
                  }"
                  >{{
                    money(
                      column.key === "planned"
                        ? record.plannedAmount
                        : column.key === "actual"
                          ? record.actualAmount
                          : record.variance,
                    )
                  }}</span
                ></template
              >
              <template v-else-if="column.key === 'rate'"
                ><a-progress
                  :percent="
                    record.plannedAmount
                      ? Math.round(
                          (record.actualAmount / record.plannedAmount) * 100,
                        )
                      : 0
                  "
                  size="small"
                  :status="record.variance < 0 ? 'exception' : 'normal'"
              /></template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="cost" :tab="`成本明细 (${detail.costEntries.length})`">
          <a-table
            :columns="costColumns"
            :data-source="detail.costEntries"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'category'"
                ><a-tag>{{ categoryLabel(record.category) }}</a-tag></template
              >
              <template v-else-if="column.key === 'amount'"
                ><strong>{{ money(record.amount) }}</strong></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="procurement" :tab="`采购与物料 (${orders.length})`">
          <a-table
            :columns="orderColumns"
            :data-source="orders"
            row-key="id"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'order'"
                ><a @click="router.push(`/procurement/orders/${record.id}`)">{{
                  record.code || "采购订单"
                }}</a
                ><span class="sub">{{ record.partName }}</span></template
              >
              <template v-else-if="column.key === 'amount'">{{
                money(record.orderAmount)
              }}</template>
              <template v-else-if="column.key === 'receipt'"
                >{{ record.receivedQty }} / {{ record.orderedQty }}</template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="finance" tab="开票回款">
          <a-table
            :columns="receivableColumns"
            :data-source="receivables"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'"
                ><a @click="router.push(`/finance/receivables/${record.id}`)">{{
                  record.code || record.sourceNo
                }}</a></template
              >
              <template v-else-if="column.key === 'amount'">{{
                money(record.amount)
              }}</template>
              <template v-else-if="column.key === 'outstanding'">{{
                money(record.outstandingAmount)
              }}</template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane
          key="timeline"
          :tab="`阶段与审计 (${detail.stageRecords.length})`"
        >
          <a-timeline v-if="detail.stageRecords.length">
            <a-timeline-item
              v-for="item in [...detail.stageRecords].reverse()"
              :key="item.id"
            >
              <strong
                >{{ stageLabel(item.fromStage) }} →
                {{ stageLabel(item.toStage) }}</strong
              >
              · {{ item.operatorName }}
              <p>
                {{ item.comment || "项目阶段推进" }} · 进度 {{ item.progress }}%
              </p>
              <small>{{ dateTime(item.changedAt) }}</small>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else description="暂无阶段记录" />
        </a-tab-pane>

        <a-tab-pane key="milestones" :tab="`里程碑 (${milestones.length})`">
          <a-space direction="vertical" style="width: 100%">
            <a-form layout="inline">
              <a-form-item>
                <a-input
                  v-model:value="milestoneForm.name"
                  placeholder="里程碑名称"
                  style="width: 220px"
                />
              </a-form-item>
              <a-form-item>
                <a-input
                  v-model:value="milestoneForm.plannedDate"
                  type="date"
                />
              </a-form-item>
              <a-form-item>
                <a-select
                  v-model:value="milestoneForm.status"
                  :options="[
                    { value: 'PENDING', label: '待开始' },
                    { value: 'IN_PROGRESS', label: '进行中' },
                    { value: 'COMPLETED', label: '已完成' },
                  ]"
                  style="width: 130px"
                />
              </a-form-item>
              <a-form-item>
                <a-input
                  v-model:value="milestoneForm.remark"
                  placeholder="说明（可选）"
                  style="width: 200px"
                />
              </a-form-item>
              <a-form-item>
                <a-button
                  type="primary"
                  :loading="milestoneSaving"
                  @click="addMilestone"
                  >添加里程碑</a-button
                >
              </a-form-item>
            </a-form>
            <a-table
              :columns="milestoneColumns"
              :data-source="milestones"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <strong>{{ record.name }}</strong>
                </template>
                <template v-else-if="column.key === 'status'">
                  <a-tag :color="milestoneStatusColor(record.status)">{{
                    milestoneStatusLabel(record.status)
                  }}</a-tag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-space>
                    <a-button
                      type="link"
                      size="small"
                      @click="toggleMilestone(record)"
                      >{{
                        record.status === "COMPLETED" ? "重开" : "完成"
                      }}</a-button
                    >
                    <a-button
                      type="link"
                      size="small"
                      danger
                      @click="removeMilestone(record)"
                      >删除</a-button
                    >
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-space>
        </a-tab-pane>

        <a-tab-pane key="staff" :tab="`项目成员 (${staff.length})`">
          <a-table
            :columns="staffColumns"
            :data-source="staff"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'member'">
                <strong>{{ record.displayName || "-" }}</strong>
              </template>
              <template v-else-if="column.key === 'allocation'">
                {{ record.allocationPercent }}%
              </template>
              <template v-else-if="column.key === 'period'">
                {{ record.startDate }} ~ {{ record.endDate }}
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="audit" :tab="`经营审计 (${timeline.length})`">
          <a-timeline v-if="timeline.length">
            <a-timeline-item
              v-for="item in timeline"
              :key="`${item.type}-${item.occurredAt}-${item.title}`"
            >
              <a-tag :color="timelineTypeColor(item.type)">{{
                timelineTypeLabel(item.type)
              }}</a-tag>
              <strong>{{ item.title }}</strong>
              <span v-if="item.actor" class="sub"> · {{ item.actor }}</span>
              <p v-if="item.detail">{{ item.detail }}</p>
              <small>{{ dateTime(item.occurredAt) }}</small>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else description="暂无经营审计记录" />
        </a-tab-pane>

        <a-tab-pane key="risks" :tab="`风险跟踪 (${risks.length})`">
          <a-space direction="vertical" style="width: 100%">
            <a-form layout="inline">
              <a-form-item>
                <a-input
                  v-model:value="riskForm.title"
                  placeholder="风险标题"
                  style="width: 220px"
                />
              </a-form-item>
              <a-form-item>
                <a-select
                  v-model:value="riskForm.severity"
                  :options="[
                    { value: 'LOW', label: '低' },
                    { value: 'MEDIUM', label: '中' },
                    { value: 'HIGH', label: '高' },
                  ]"
                  style="width: 100px"
                />
              </a-form-item>
              <a-form-item>
                <a-input
                  v-model:value="riskForm.ownerName"
                  placeholder="责任人"
                  style="width: 120px"
                />
              </a-form-item>
              <a-form-item>
                <a-input v-model:value="riskForm.dueDate" type="date" />
              </a-form-item>
              <a-form-item>
                <a-input
                  v-model:value="riskForm.description"
                  placeholder="描述（可选）"
                  style="width: 200px"
                />
              </a-form-item>
              <a-form-item>
                <a-button
                  type="primary"
                  danger
                  :loading="riskSaving"
                  @click="addRisk"
                  >登记风险</a-button
                >
              </a-form-item>
            </a-form>
            <a-table
              :columns="riskColumns"
              :data-source="risks"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'title'">
                  <strong>{{ record.title }}</strong>
                  <span v-if="record.description" class="sub">{{
                    record.description
                  }}</span>
                </template>
                <template v-else-if="column.key === 'severity'">
                  <a-tag :color="riskSeverityColor(record.severity)">{{
                    riskSeverityLabel(record.severity)
                  }}</a-tag>
                </template>
                <template v-else-if="column.key === 'status'">
                  <a-tag :color="riskStatusColor(record.status)">{{
                    riskStatusLabel(record.status)
                  }}</a-tag>
                </template>
                <template v-else-if="column.key === 'resolution'">
                  {{ record.resolution || "-" }}
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-space>
                    <a-button
                      type="link"
                      size="small"
                      @click="cycleRiskStatus(record)"
                      >{{
                        riskStatusLabel(
                          record.status === "OPEN"
                            ? "MITIGATING"
                            : record.status === "MITIGATING"
                              ? "CLOSED"
                              : "OPEN",
                        )
                      }}</a-button
                    >
                    <a-button
                      type="link"
                      size="small"
                      danger
                      @click="removeRisk(record)"
                      >删除</a-button
                    >
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-space>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="preparationOpen"
      :title="
        detail?.project.parentProjectId
          ? '完善子项目立项资料'
          : '编辑项目并重新提交'
      "
      width="760px"
      :confirm-loading="preparationSaving"
      @ok="savePreparation"
    >
      <a-alert
        type="info"
        show-icon
        message="资料保存后进入待审批状态；需先分配项目经理并填写有效预算。"
        class="section-gap"
      />
      <a-form layout="vertical">
        <a-form-item label="现场地址" required>
          <a-input v-model:value="preparationForm.siteAddress" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :md="8">
            <a-form-item label="计划开始日期" required>
              <a-input
                v-model:value="preparationForm.plannedStartDate"
                type="date"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="8">
            <a-form-item label="计划结束日期" required>
              <a-input
                v-model:value="preparationForm.plannedEndDate"
                type="date"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="8">
            <a-form-item label="质保截止日期">
              <a-input
                v-model:value="preparationForm.warrantyEndDate"
                type="date"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider>分类预算（含税，元）</a-divider>
        <a-row :gutter="16">
          <a-col
            v-for="item in preparationCategories"
            :key="item.value"
            :xs="24"
            :md="8"
          >
            <a-form-item :label="item.label">
              <a-input-number
                v-model:value="preparationForm.budgets[item.value]"
                :min="0"
                :precision="2"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </BusinessDetailPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import BusinessDetailPage, {
  type DetailMetric,
} from "@/components/BusinessDetailPage.vue";
import {
  getProject,
  updateProject,
  getProjectTimeline,
  getProjectStaff,
  listProjectMilestones,
  createProjectMilestone,
  updateProjectMilestone,
  deleteProjectMilestone,
  listProjectRisks,
  createProjectRisk,
  updateProjectRisk,
  deleteProjectRisk,
  type ProjectDetail,
  type ProjectCostCategory,
  type ProjectStage,
  type ProjectMilestone,
  type MilestoneStatus,
  type ProjectStaff,
  type ProjectTimelineEntry,
  type ProjectRisk,
  type RiskSeverity,
  type RiskStatus,
} from "@/api/project";
import { listPurchaseOrders, type PurchaseOrder } from "@/api/procurement";
import { listReceivablesByContract, type Receivable } from "@/api/crm";
import { useAuthStore } from "@/stores/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const projectId = computed(() => String(route.params.id));
const loading = ref(false);
const detail = ref<ProjectDetail | null>(null);
const orders = ref<PurchaseOrder[]>([]);
const receivables = ref<Receivable[]>([]);
const milestones = ref<ProjectMilestone[]>([]);
const staff = ref<ProjectStaff[]>([]);
const timeline = ref<ProjectTimelineEntry[]>([]);
const risks = ref<ProjectRisk[]>([]);
const riskForm = reactive({
  title: "",
  description: "",
  severity: "MEDIUM" as RiskSeverity,
  ownerName: "",
  dueDate: "",
  resolution: "",
});
const riskSaving = ref(false);
const milestoneForm = reactive({
  name: "",
  plannedDate: "",
  status: "PENDING" as MilestoneStatus,
  remark: "",
});
const milestoneSaving = ref(false);
const preparationOpen = ref(false);
const preparationSaving = ref(false);
const preparationCategories: Array<{
  label: string;
  value: ProjectCostCategory;
}> = [
  { label: "人工预算", value: "LABOR" },
  { label: "物料预算", value: "MATERIAL" },
  { label: "外包预算", value: "SUBCONTRACT" },
  { label: "差旅预算", value: "TRAVEL" },
  { label: "其他预算", value: "OTHER" },
];
const preparationForm = reactive({
  siteAddress: "",
  plannedStartDate: "",
  plannedEndDate: "",
  warrantyEndDate: "",
  budgets: {
    LABOR: 0,
    MATERIAL: 0,
    SUBCONTRACT: 0,
    TRAVEL: 0,
    OTHER: 0,
  } as Record<ProjectCostCategory, number>,
});
const canEditProject = computed(
  () =>
    detail.value?.project.approvalStatus !== "APPROVED" &&
    (auth.can("project:create") ||
      auth.can("project:approve") ||
      auth.can("project:stage:update")),
);
const metrics = computed<DetailMetric[]>(() =>
  detail.value
    ? [
        {
          label: "合同金额（含税，元）",
          value: money(detail.value.project.contractAmount),
        },
        {
          label: "预算成本（含税，元）",
          value: money(detail.value.project.budgetAmount),
        },
        {
          label: "实际成本（含税，元）",
          value: money(detail.value.project.actualCost),
          warning: detail.value.project.budgetVariance < 0,
        },
        {
          label: "当前毛利（含税，元）",
          value: money(detail.value.project.grossMargin),
          danger: detail.value.project.grossMargin < 0,
        },
        {
          label: "预算余额（含税，元）",
          value: money(detail.value.project.budgetVariance),
          danger: detail.value.project.budgetVariance < 0,
        },
      ]
    : [],
);
const stages: ProjectStage[] = [
  "INITIATED",
  "BIDDING",
  "ENTRY",
  "CONSTRUCTION",
  "COMMISSIONING",
  "INITIAL_ACCEPTANCE",
  "FINAL_ACCEPTANCE",
  "WARRANTY",
  "CLOSED",
];
const stageIndex = computed(() =>
  detail.value
    ? Math.min(4, Math.floor(stages.indexOf(detail.value.project.stage) / 2))
    : 0,
);
const riskLabel = computed(() =>
  !detail.value
    ? ""
    : detail.value.project.budgetVariance < 0
      ? "预算已超支"
      : detail.value.project.grossMargin <
          detail.value.project.contractAmount * 0.1
        ? "毛利偏低"
        : "经营正常",
);
const riskColor = computed(() =>
  riskLabel.value === "经营正常"
    ? "green"
    : riskLabel.value === "预算已超支"
      ? "red"
      : "orange",
);
const riskDescription = computed(() =>
  detail.value?.project.budgetVariance &&
  detail.value.project.budgetVariance < 0
    ? `已超预算 ${money(Math.abs(detail.value.project.budgetVariance))}，请复核采购、领料和费用。`
    : "当前毛利率低于建议安全线，请关注后续采购和人工投入。",
);
const budgetColumns = [
  { title: "成本类别", key: "category", width: 130 },
  { title: "计划", key: "planned", width: 150 },
  { title: "实际", key: "actual", width: 150 },
  { title: "预算余额（含税，元）", key: "variance", width: 190 },
  { title: "执行率", key: "rate" },
];
const costColumns = [
  { title: "类别", key: "category", width: 120 },
  { title: "来源", dataIndex: "sourceType", width: 130 },
  { title: "来源单号", dataIndex: "sourceNo", width: 180 },
  { title: "说明", dataIndex: "description" },
  { title: "成本金额（含税，元）", key: "amount", width: 190 },
  { title: "发生日期", dataIndex: "incurredDate", width: 120 },
];
const orderColumns = [
  { title: "采购订单", key: "order", width: 230 },
  { title: "供应商", dataIndex: "supplierName", width: 180 },
  { title: "领料金额（含税，元）", key: "amount", width: 190 },
  { title: "到货", key: "receipt", width: 100 },
  { title: "预计交付", dataIndex: "expectedDeliveryDate", width: 120 },
  { title: "状态", dataIndex: "status", width: 120 },
];
const receivableColumns = [
  { title: "应收单", key: "code", width: 200 },
  { title: "应收金额（含税，元）", key: "amount", width: 190 },
  { title: "待收金额（含税，元）", key: "outstanding", width: 190 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "状态", dataIndex: "status", width: 120 },
];
const milestoneColumns = [
  { title: "里程碑", key: "name", width: 220 },
  { title: "计划日期", dataIndex: "plannedDate", width: 130 },
  { title: "实际日期", dataIndex: "actualDate", width: 130 },
  { title: "状态", key: "status", width: 120 },
  { title: "说明", dataIndex: "remark" },
  { title: "操作", key: "action", width: 160 },
];
const staffColumns = [
  { title: "成员", key: "member", width: 160 },
  { title: "角色", dataIndex: "roleName", width: 140 },
  { title: "计划工时", dataIndex: "plannedHours", width: 110 },
  { title: "实际工时", dataIndex: "actualHours", width: 110 },
  { title: "投入占比", key: "allocation", width: 100 },
  { title: "周期", key: "period", width: 210 },
  { title: "状态", dataIndex: "status", width: 110 },
];
const riskColumns = [
  { title: "风险项", key: "title", width: 240 },
  { title: "等级", key: "severity", width: 90 },
  { title: "状态", key: "status", width: 110 },
  { title: "责任人", dataIndex: "ownerName", width: 110 },
  { title: "到期日", dataIndex: "dueDate", width: 110 },
  { title: "应对/处置", key: "resolution" },
  { title: "操作", key: "action", width: 180 },
];
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const project = await getProject(projectId.value);
    const [
      orderPage,
      receivablePage,
      milestoneRows,
      staffRows,
      timelineRows,
      riskRows,
    ] = await Promise.all([
      auth.can("procurement:view")
        ? listPurchaseOrders({
            page: 0,
            size: 100,
            costType: "PROJECT",
            projectId: projectId.value,
          })
        : Promise.resolve({ content: [] } as { content: PurchaseOrder[] }),
      auth.can("crm:receivable:view") && project.project.contractId
        ? listReceivablesByContract(project.project.contractId)
        : Promise.resolve({ content: [] } as { content: Receivable[] }),
      listProjectMilestones(projectId.value),
      getProjectStaff(projectId.value),
      getProjectTimeline(projectId.value),
      listProjectRisks(projectId.value),
    ]);
    detail.value = project;
    orders.value = orderPage.content;
    receivables.value = receivablePage.content;
    milestones.value = milestoneRows;
    staff.value = staffRows;
    timeline.value = timelineRows;
    risks.value = riskRows;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目详情加载失败");
  } finally {
    loading.value = false;
  }
}
async function addMilestone() {
  if (!milestoneForm.name.trim()) {
    message.warning("请填写里程碑名称");
    return;
  }
  milestoneSaving.value = true;
  try {
    await createProjectMilestone(projectId.value, {
      name: milestoneForm.name.trim(),
      plannedDate: milestoneForm.plannedDate || undefined,
      status: milestoneForm.status,
      sortOrder: milestones.value.length,
      remark: milestoneForm.remark || undefined,
    });
    milestoneForm.name = "";
    milestoneForm.plannedDate = "";
    milestoneForm.remark = "";
    milestones.value = await listProjectMilestones(projectId.value);
    message.success("里程碑已添加");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "里程碑添加失败");
  } finally {
    milestoneSaving.value = false;
  }
}
async function toggleMilestone(record: ProjectMilestone) {
  const next: MilestoneStatus =
    record.status === "COMPLETED" ? "PENDING" : "COMPLETED";
  try {
    await updateProjectMilestone(projectId.value, record.id, {
      name: record.name,
      plannedDate: record.plannedDate,
      actualDate: next === "COMPLETED" ? today() : undefined,
      status: next,
      sortOrder: record.sortOrder,
      remark: record.remark,
    });
    milestones.value = await listProjectMilestones(projectId.value);
    message.success(next === "COMPLETED" ? "里程碑已完成" : "里程碑已重新打开");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "里程碑更新失败");
  }
}
async function removeMilestone(record: ProjectMilestone) {
  try {
    await deleteProjectMilestone(projectId.value, record.id);
    milestones.value = await listProjectMilestones(projectId.value);
    message.success("里程碑已删除");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "里程碑删除失败");
  }
}
function today() {
  return new Date().toISOString().slice(0, 10);
}
async function addRisk() {
  if (!riskForm.title.trim()) {
    message.warning("请填写风险标题");
    return;
  }
  riskSaving.value = true;
  try {
    await createProjectRisk(projectId.value, {
      title: riskForm.title.trim(),
      description: riskForm.description || undefined,
      severity: riskForm.severity,
      status: "OPEN",
      ownerName: riskForm.ownerName || undefined,
      dueDate: riskForm.dueDate || undefined,
      resolution: riskForm.resolution || undefined,
    });
    riskForm.title = "";
    riskForm.description = "";
    riskForm.ownerName = "";
    riskForm.dueDate = "";
    riskForm.resolution = "";
    risks.value = await listProjectRisks(projectId.value);
    message.success("风险已登记");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "风险登记失败");
  } finally {
    riskSaving.value = false;
  }
}
async function cycleRiskStatus(record: ProjectRisk) {
  const next: RiskStatus =
    record.status === "OPEN"
      ? "MITIGATING"
      : record.status === "MITIGATING"
        ? "CLOSED"
        : "OPEN";
  try {
    await updateProjectRisk(projectId.value, record.id, {
      title: record.title,
      description: record.description,
      severity: record.severity,
      status: next,
      ownerName: record.ownerName,
      dueDate: record.dueDate,
      resolution: record.resolution,
    });
    risks.value = await listProjectRisks(projectId.value);
    message.success("风险状态已更新");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "风险状态更新失败");
  }
}
async function removeRisk(record: ProjectRisk) {
  try {
    await deleteProjectRisk(projectId.value, record.id);
    risks.value = await listProjectRisks(projectId.value);
    message.success("风险已删除");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "风险删除失败");
  }
}
function riskSeverityLabel(value?: string) {
  return (
    ({ LOW: "低", MEDIUM: "中", HIGH: "高" } as Record<string, string>)[
      value || ""
    ] || "-"
  );
}
function riskSeverityColor(value?: string) {
  return (
    (
      { LOW: "default", MEDIUM: "orange", HIGH: "red" } as Record<
        string,
        string
      >
    )[value || ""] || "default"
  );
}
function riskStatusLabel(value?: string) {
  return (
    (
      {
        OPEN: "待处置",
        MITIGATING: "处置中",
        CLOSED: "已关闭",
      } as Record<string, string>
    )[value || ""] || "-"
  );
}
function riskStatusColor(value?: string) {
  return (
    (
      {
        OPEN: "red",
        MITIGATING: "orange",
        CLOSED: "default",
      } as Record<string, string>
    )[value || ""] || "default"
  );
}
function milestoneStatusLabel(value?: string) {
  return (
    (
      {
        PENDING: "待开始",
        IN_PROGRESS: "进行中",
        COMPLETED: "已完成",
      } as Record<string, string>
    )[value || ""] || "-"
  );
}
function milestoneStatusColor(value?: string) {
  return (
    (
      {
        PENDING: "default",
        IN_PROGRESS: "processing",
        COMPLETED: "success",
      } as Record<string, string>
    )[value || ""] || "default"
  );
}
function timelineTypeLabel(type: string) {
  return (
    (
      {
        STAGE: "阶段",
        COST: "成本",
        CLOSEOUT: "结项",
        BUDGET: "预算",
        MANAGER: "负责人",
      } as Record<string, string>
    )[type] || type
  );
}
function timelineTypeColor(type: string) {
  return (
    (
      {
        STAGE: "blue",
        COST: "orange",
        CLOSEOUT: "purple",
        BUDGET: "cyan",
        MANAGER: "green",
      } as Record<string, string>
    )[type] || "default"
  );
}
function openPreparation() {
  if (!detail.value) return;
  const budgets = Object.fromEntries(
    preparationCategories.map((item) => [item.value, 0]),
  ) as Record<ProjectCostCategory, number>;
  detail.value.budgetItems.forEach((item) => {
    budgets[item.category] = Number(item.plannedAmount || 0);
  });
  Object.assign(preparationForm, {
    siteAddress: detail.value.project.siteAddress || "",
    plannedStartDate: detail.value.project.plannedStartDate || "",
    plannedEndDate: detail.value.project.plannedEndDate || "",
    warrantyEndDate: detail.value.project.warrantyEndDate || "",
    budgets,
  });
  preparationOpen.value = true;
}
async function savePreparation() {
  if (
    !preparationForm.siteAddress.trim() ||
    !preparationForm.plannedStartDate ||
    !preparationForm.plannedEndDate
  ) {
    message.warning("请完整填写现场地址和计划周期");
    return;
  }
  const budgetTotal = Object.values(preparationForm.budgets).reduce(
    (sum, value) => sum + Number(value || 0),
    0,
  );
  if (budgetTotal <= 0) {
    message.warning("请至少填写一项有效预算");
    return;
  }
  preparationSaving.value = true;
  try {
    detail.value = await updateProject(projectId.value, {
      name: detail.value!.project.name,
      siteAddress: preparationForm.siteAddress.trim(),
      contractAmount: detail.value!.project.contractAmount,
      plannedStartDate: preparationForm.plannedStartDate,
      plannedEndDate: preparationForm.plannedEndDate,
      warrantyEndDate: preparationForm.warrantyEndDate || undefined,
      budgetItems: preparationCategories.map((item) => ({
        category: item.value,
        plannedAmount: Number(preparationForm.budgets[item.value] || 0),
        remark: `${item.label}（立项准备）`,
      })),
    });
    preparationOpen.value = false;
    message.success("项目资料已保存并重新提交审批");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "立项资料保存失败");
  } finally {
    preparationSaving.value = false;
  }
}
function money(value?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(Number(value || 0));
}
function dateTime(value?: string) {
  return value ? value.slice(0, 16).replace("T", " ") : "-";
}
function stageLabel(value?: string) {
  return (
    (
      {
        INITIATED: "立项",
        BIDDING: "招投标",
        ENTRY: "进场",
        CONSTRUCTION: "施工",
        COMMISSIONING: "调试",
        INITIAL_ACCEPTANCE: "初验",
        FINAL_ACCEPTANCE: "终验",
        WARRANTY: "质保",
        CLOSED: "结项",
      } as Record<string, string>
    )[value || ""] || ""
  );
}
function approvalLabel(value?: string) {
  return (
    (
      { PENDING: "待审批", APPROVED: "已批准", REJECTED: "已驳回" } as Record<
        string,
        string
      >
    )[value || ""] || "-"
  );
}
function executionLabel(value?: string) {
  return (
    (
      {
        ACTIVE: "执行中",
        PAUSED: "已暂停",
        CANCELLED: "已取消",
        CLOSED: "已结项",
      } as Record<string, string>
    )[value || ""] || "-"
  );
}
function executionColor(value?: string) {
  return (
    (
      {
        ACTIVE: "green",
        PAUSED: "orange",
        CANCELLED: "red",
        CLOSED: "default",
      } as Record<string, string>
    )[value || ""] || "default"
  );
}
function typeLabel(value?: string) {
  return (
    (
      {
        NEW_CONSTRUCTION: "新建项目",
        RENOVATION: "改造项目",
        O_M_RENOVATION: "运维改造",
      } as Record<string, string>
    )[value || ""] || "-"
  );
}
function categoryLabel(value: string) {
  return (
    (
      {
        LABOR: "人工",
        MATERIAL: "物料",
        SUBCONTRACT: "外包",
        TRAVEL: "差旅",
        OTHER: "其他",
      } as Record<string, string>
    )[value] || value
  );
}
</script>

<style scoped>
.section-gap {
  margin-top: 16px;
}
.danger {
  color: #cf1322;
  font-weight: 600;
}
.sub {
  display: block;
  color: #8c96a5;
  font-size: 12px;
}
.ant-timeline p {
  margin: 5px 0;
  color: #596579;
}
.ant-timeline small {
  color: #9aa4b2;
}
</style>
