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
      <a-button
        type="primary"
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
            <a-descriptions-item label="质保截止">{{
              detail.project.warrantyEndDate || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="审批状态">{{
              approvalLabel(detail.project.approvalStatus)
            }}</a-descriptions-item>
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
      </a-tabs>
    </a-card>
  </BusinessDetailPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import BusinessDetailPage, {
  type DetailMetric,
} from "@/components/BusinessDetailPage.vue";
import {
  getProject,
  type ProjectDetail,
  type ProjectStage,
} from "@/api/project";
import { listPurchaseOrders, type PurchaseOrder } from "@/api/procurement";
import { listReceivables, type Receivable } from "@/api/crm";

const route = useRoute();
const router = useRouter();
const projectId = computed(() => String(route.params.id));
const loading = ref(false);
const detail = ref<ProjectDetail | null>(null);
const orders = ref<PurchaseOrder[]>([]);
const receivables = ref<Receivable[]>([]);
const metrics = computed<DetailMetric[]>(() =>
  detail.value
    ? [
        {
          label: "合同金额",
          value: money(detail.value.project.contractAmount),
        },
        { label: "预算成本", value: money(detail.value.project.budgetAmount) },
        {
          label: "实际成本",
          value: money(detail.value.project.actualCost),
          warning: detail.value.project.budgetVariance < 0,
        },
        {
          label: "当前毛利",
          value: money(detail.value.project.grossMargin),
          danger: detail.value.project.grossMargin < 0,
        },
        {
          label: "预算余额",
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
  { title: "余额", key: "variance", width: 150 },
  { title: "执行率", key: "rate" },
];
const costColumns = [
  { title: "类别", key: "category", width: 120 },
  { title: "来源", dataIndex: "sourceType", width: 130 },
  { title: "来源单号", dataIndex: "sourceNo", width: 180 },
  { title: "说明", dataIndex: "description" },
  { title: "金额", key: "amount", width: 150 },
  { title: "发生日期", dataIndex: "incurredDate", width: 120 },
];
const orderColumns = [
  { title: "采购订单", key: "order", width: 230 },
  { title: "供应商", dataIndex: "supplierName", width: 180 },
  { title: "金额", key: "amount", width: 140 },
  { title: "到货", key: "receipt", width: 100 },
  { title: "预计交付", dataIndex: "expectedDeliveryDate", width: 120 },
  { title: "状态", dataIndex: "status", width: 120 },
];
const receivableColumns = [
  { title: "应收单", key: "code", width: 200 },
  { title: "应收金额", key: "amount", width: 150 },
  { title: "待收", key: "outstanding", width: 150 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "状态", dataIndex: "status", width: 120 },
];
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const [project, orderPage, allReceivables] = await Promise.all([
      getProject(projectId.value),
      listPurchaseOrders({ page: 0, size: 999 }),
      listReceivables(),
    ]);
    detail.value = project;
    orders.value = orderPage.content.filter(
      (item) =>
        item.costType === "PROJECT" && item.costTargetId === projectId.value,
    );
    receivables.value = allReceivables.filter(
      (item) => item.contractId === project.project.contractId,
    );
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目详情加载失败");
  } finally {
    loading.value = false;
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
