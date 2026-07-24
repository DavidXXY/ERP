<template>
  <a-card :bordered="false" class="trace-card">
    <template #title>经营穿透链路</template>
    <template #extra>
      <a-button size="small" :loading="loading" @click="loadData"
        >刷新链路</a-button
      >
    </template>

    <a-spin :spinning="loading">
      <a-alert
        v-if="loadErrors.length"
        type="warning"
        show-icon
        class="trace-alert"
        :message="`部分链路数据加载失败：${loadErrors.join('、')}`"
      />

      <a-row :gutter="[16, 16]" class="trace-insights">
        <a-col :xs="12" :lg="6">
          <div class="insight-card">
            <span>链路完整度</span>
            <strong>{{ traceScore }}%</strong>
            <a-progress
              :percent="traceScore"
              size="small"
              :show-info="false"
              :stroke-color="
                traceScore >= 80
                  ? '#52c41a'
                  : traceScore >= 55
                    ? '#faad14'
                    : '#ff4d4f'
              "
            />
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="insight-card">
            <span>链路断点</span>
            <strong>{{ gapAlerts.length }}</strong>
            <p>{{ gapAlerts[0]?.title || "关键链路完整" }}</p>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="insight-card">
            <span>回款率</span>
            <strong>{{ formatPercent(collectionRate) }}</strong>
            <p>
              {{ formatMoney(summary.receivedAmount) }} /
              {{ formatMoney(summary.receivableAmount) }}
            </p>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="insight-card">
            <span>毛利率</span>
            <strong :class="{ danger: marginRate < 10 }">{{
              formatPercent(marginRate)
            }}</strong>
            <p>{{ profitability?.riskMessage || "按当前链路成本估算" }}</p>
          </div>
        </a-col>
      </a-row>

      <div v-if="gapAlerts.length" class="gap-list">
        <div
          v-for="item in gapAlerts"
          :key="item.key"
          class="gap-item"
          :class="item.level"
        >
          <a-tag :color="item.level === 'high' ? 'red' : 'orange'">{{
            item.level === "high" ? "高优先" : "关注"
          }}</a-tag>
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.description }}</p>
          </div>
          <a-button
            v-if="item.route"
            size="small"
            type="link"
            @click="go(item.route)"
            >处理</a-button
          >
        </div>
      </div>

      <div class="trace-flow">
        <button
          v-for="step in steps"
          :key="step.key"
          class="trace-step"
          :class="{
            active: step.count > 0,
            selected: stageFilter === step.label,
          }"
          type="button"
          @click="stageFilter = stageFilter === step.label ? 'ALL' : step.label"
        >
          <div class="step-head">
            <span>{{ step.label }}</span>
            <a-tag :color="step.count > 0 ? 'blue' : 'default'">{{
              step.count
            }}</a-tag>
          </div>
          <strong>{{
            step.amount != null ? formatMoney(step.amount) : step.value
          }}</strong>
          <p>{{ step.hint }}</p>
        </button>
      </div>

      <a-row :gutter="[16, 16]" class="trace-metrics">
        <a-col :xs="12" :lg="6">
          <a-statistic
            title="合同额"
            :value="summary.contractAmount"
            :formatter="moneyFormatter"
          />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic
            title="采购/领料成本"
            :value="summary.supplyCost"
            :formatter="moneyFormatter"
          />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic
            title="已回款"
            :value="summary.receivedAmount"
            :formatter="moneyFormatter"
          />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic
            title="毛利"
            :value="summary.grossProfit"
            :formatter="moneyFormatter"
          />
        </a-col>
      </a-row>

      <div class="waterfall">
        <div
          v-for="item in waterfallItems"
          :key="item.key"
          class="waterfall-item"
        >
          <span>{{ item.label }}</span>
          <strong :class="{ negative: item.value < 0 }">{{
            formatMoney(item.value)
          }}</strong>
          <div class="waterfall-track">
            <div
              class="waterfall-bar"
              :class="{ negative: item.value < 0 }"
              :style="{ width: item.width + '%' }"
            ></div>
          </div>
        </div>
      </div>

      <a-table
        class="trace-table"
        row-key="key"
        size="small"
        :columns="columns"
        :data-source="filteredEvents"
        :pagination="{ pageSize: 8 }"
      >
        <template #title>
          <a-space wrap>
            <a-radio-group v-model:value="stageFilter" size="small">
              <a-radio-button value="ALL">全部</a-radio-button>
              <a-radio-button
                v-for="item in eventStageOptions"
                :key="item"
                :value="item"
                >{{ item }}</a-radio-button
              >
            </a-radio-group>
          </a-space>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'stage'">
            <a-tag>{{ record.stage }}</a-tag>
          </template>
          <template v-else-if="column.key === 'title'">
            <strong>{{ record.title }}</strong>
            <span class="table-subtitle">{{ record.description }}</span>
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ record.amount != null ? formatMoney(record.amount) : "-" }}
          </template>
          <template v-else-if="column.key === 'route'">
            <a-button
              v-if="record.route"
              type="link"
              size="small"
              @click="go(record.route)"
              >查看</a-button
            >
          </template>
        </template>
        <template #emptyText>暂无可穿透数据</template>
      </a-table>
    </a-spin>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import {
  getOpportunity,
  getQuote,
  listReceivables,
  type Opportunity,
  type QuotePlan,
  type Receivable,
  type ServiceContract,
} from "@/api/crm";
import { listMaterialIssues, type MaterialIssue } from "@/api/inventory";
import {
  listGoodsReceipts,
  listProcurementCostAllocations,
  listPurchaseOrders,
  listPurchaseRequests,
  type GoodsReceipt,
  type ProcurementCostAllocation,
  type PurchaseOrder,
  type PurchaseRequest,
} from "@/api/procurement";
import {
  getProject,
  listProjectProfitability,
  listProjects,
  type Project,
  type ProjectDetail,
  type ProjectProfitability,
} from "@/api/project";

type TraceEvent = {
  key: string;
  stage: string;
  title: string;
  description?: string;
  amount?: number;
  date?: string;
  route?: string;
};

type TraceStep = {
  key: string;
  label: string;
  count: number;
  amount?: number;
  value: string;
  hint: string;
};

const props = defineProps<{
  contract?: ServiceContract | null;
  project?: Project | null;
  projectDetail?: ProjectDetail | null;
}>();

const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const loadErrors = ref<string[]>([]);
const relatedQuote = ref<QuotePlan | null>(null);
const relatedOpportunity = ref<Opportunity | null>(null);
const relatedContract = ref<ServiceContract | null>(null);
const relatedProject = ref<Project | null>(null);
const relatedProjectDetail = ref<ProjectDetail | null>(null);
const profitability = ref<ProjectProfitability | null>(null);
const procurementRequests = ref<PurchaseRequest[]>([]);
const purchaseOrders = ref<PurchaseOrder[]>([]);
const goodsReceipts = ref<GoodsReceipt[]>([]);
const materialIssues = ref<MaterialIssue[]>([]);
const costAllocations = ref<ProcurementCostAllocation[]>([]);
const receivables = ref<Receivable[]>([]);
const stageFilter = ref("ALL");

const columns = [
  { title: "链路", key: "stage", width: 110 },
  { title: "事件", key: "title", width: 260 },
  { title: "金额", key: "amount", width: 130 },
  { title: "日期", dataIndex: "date", key: "date", width: 120 },
  { title: "穿透", key: "route", width: 80 },
];

const sourceKey = computed(
  () =>
    `${props.contract?.id || ""}-${props.project?.id || props.projectDetail?.project.id || ""}`,
);

const activeContract = computed(() => props.contract || relatedContract.value);
const activeProject = computed(
  () => props.projectDetail?.project || props.project || relatedProject.value,
);
const activeProjectDetail = computed(
  () => props.projectDetail || relatedProjectDetail.value,
);

const summary = computed(() => {
  const contractAmount = Number(
    activeContract.value?.amount || activeProject.value?.contractAmount || 0,
  );
  const procurementCost = costAllocations.value.reduce(
    (sum, item) => sum + Number(item.amount || 0),
    0,
  );
  const materialCost = materialIssues.value.reduce(
    (sum, item) => sum + Number(item.totalAmount || 0),
    0,
  );
  const projectCost = Number(
    activeProjectDetail.value?.project.actualCost || 0,
  );
  const supplyCost = Math.max(procurementCost + materialCost, projectCost);
  const receivedAmount = receivables.value.reduce(
    (sum, item) => sum + Number(item.settledAmount || 0),
    0,
  );
  const receivableAmount = receivables.value.reduce(
    (sum, item) => sum + Number(item.amount || 0),
    0,
  );
  const grossProfit = contractAmount - supplyCost;
  const outstandingAmount = receivableAmount - receivedAmount;
  return {
    contractAmount,
    supplyCost,
    receivableAmount,
    receivedAmount,
    outstandingAmount,
    grossProfit,
  };
});

const marginRate = computed(() =>
  summary.value.contractAmount > 0
    ? (summary.value.grossProfit / summary.value.contractAmount) * 100
    : 0,
);
const collectionRate = computed(() =>
  summary.value.receivableAmount > 0
    ? (summary.value.receivedAmount / summary.value.receivableAmount) * 100
    : 0,
);
const needsMaterialSupply = computed(() => {
  const materialBudget =
    activeProjectDetail.value?.budgetItems
      .filter((item) => item.category === "MATERIAL")
      .reduce((sum, item) => sum + Number(item.plannedAmount || 0), 0) || 0;
  const materialCost =
    activeProjectDetail.value?.costEntries
      .filter(
        (item) =>
          item.category === "MATERIAL" ||
          item.sourceType === "INVENTORY" ||
          item.sourceType === "PROCUREMENT",
      )
      .reduce((sum, item) => sum + Number(item.amount || 0), 0) || 0;
  return materialBudget > 0 || materialCost > 0;
});
const traceScore = computed(() => {
  const required = ["customer", "contract", "receivable", "profit"];
  const execution = activeProject.value ? ["project", "cost"] : [];
  const supply =
    activeProject.value &&
    (purchaseOrders.value.length ||
      materialIssues.value.length ||
      costAllocations.value.length)
      ? ["purchase", "cost"]
      : [];
  const keys = Array.from(new Set([...required, ...execution, ...supply]));
  const done = keys.filter((key) =>
    steps.value.find((item) => item.key === key && item.count > 0),
  ).length;
  return keys.length ? Math.round((done / keys.length) * 100) : 0;
});

const steps = computed<TraceStep[]>(() => [
  {
    key: "customer",
    label: "客户",
    count: activeContract.value || activeProject.value?.customerName ? 1 : 0,
    value:
      activeContract.value?.customerName ||
      activeProject.value?.customerName ||
      "-",
    hint: "经营对象源头",
  },
  {
    key: "opportunity",
    label: "商机",
    count: relatedOpportunity.value ? 1 : 0,
    amount: relatedOpportunity.value?.expectedAmount,
    value: relatedOpportunity.value?.code || "-",
    hint: relatedOpportunity.value?.stage || "未关联商机",
  },
  {
    key: "quote",
    label: "报价",
    count: relatedQuote.value ? 1 : 0,
    amount: relatedQuote.value?.amount,
    value: relatedQuote.value?.code || "-",
    hint: relatedQuote.value
      ? `V${relatedQuote.value.versionNo}`
      : "未关联报价",
  },
  {
    key: "contract",
    label: "合同",
    count: activeContract.value ? 1 : 0,
    amount: activeContract.value?.amount || activeProject.value?.contractAmount,
    value:
      activeContract.value?.code || activeProject.value?.contractCode || "-",
    hint:
      activeContract.value?.status ||
      activeProject.value?.contractStatus ||
      "合同信息",
  },
  {
    key: "project",
    label: "项目",
    count: activeProject.value ? 1 : 0,
    amount: activeProject.value?.actualCost,
    value: activeProject.value?.code || "-",
    hint: activeProject.value?.stage || "未转项目",
  },
  {
    key: "purchase",
    label: "采购",
    count: procurementRequests.value.length + purchaseOrders.value.length,
    amount: purchaseOrders.value.reduce(
      (sum, item) => sum + Number(item.orderAmount || item.amount || 0),
      0,
    ),
    value: `${procurementRequests.value.length}/${purchaseOrders.value.length}`,
    hint: "申请/订单",
  },
  {
    key: "receipt",
    label: "入库",
    count: goodsReceipts.value.length,
    amount: goodsReceipts.value.reduce(
      (sum, item) => sum + Number(item.amount || 0),
      0,
    ),
    value: String(goodsReceipts.value.length),
    hint: "到货入库",
  },
  {
    key: "issue",
    label: "领料",
    count: materialIssues.value.length,
    amount: materialIssues.value.reduce(
      (sum, item) => sum + Number(item.totalAmount || 0),
      0,
    ),
    value: String(materialIssues.value.length),
    hint: "项目领料",
  },
  {
    key: "cost",
    label: "成本",
    count:
      activeProjectDetail.value?.costEntries.length ||
      costAllocations.value.length,
    amount: summary.value.supplyCost,
    value: String(
      activeProjectDetail.value?.costEntries.length ||
        costAllocations.value.length,
    ),
    hint: "成本归集",
  },
  {
    key: "receivable",
    label: "应收",
    count: receivables.value.length,
    amount: receivables.value.reduce(
      (sum, item) => sum + Number(item.amount || 0),
      0,
    ),
    value: String(receivables.value.length),
    hint: "合同应收",
  },
  {
    key: "receipt-money",
    label: "回款",
    count: receivables.value.filter(
      (item) => Number(item.settledAmount || 0) > 0,
    ).length,
    amount: summary.value.receivedAmount,
    value: "-",
    hint: "已登记回款",
  },
  {
    key: "profit",
    label: "利润",
    count: activeProject.value || activeContract.value ? 1 : 0,
    amount: summary.value.grossProfit,
    value: "-",
    hint: profitability.value?.riskMessage || "合同额 - 成本",
  },
]);

const events = computed<TraceEvent[]>(() => {
  const rows: TraceEvent[] = [];
  if (activeContract.value)
    rows.push({
      key: "contract",
      stage: "合同",
      title: activeContract.value.code || activeContract.value.projectName,
      description: activeContract.value.projectName,
      amount: activeContract.value.amount,
      date: activeContract.value.startDate,
      route: `/crm/contracts/${activeContract.value.id}`,
    });
  if (relatedOpportunity.value)
    rows.push({
      key: "opportunity",
      stage: "商机",
      title:
        relatedOpportunity.value.code || relatedOpportunity.value.needSummary,
      description: relatedOpportunity.value.needSummary,
      amount: relatedOpportunity.value.expectedAmount,
      date: formatDate(relatedOpportunity.value.updatedAt),
      route: `/crm/opportunities/${relatedOpportunity.value.id}`,
    });
  if (relatedQuote.value)
    rows.push({
      key: "quote",
      stage: "报价",
      title: relatedQuote.value.code || "报价方案",
      description: relatedQuote.value.serviceScope,
      amount: relatedQuote.value.amount,
      date: formatDate(relatedQuote.value.updatedAt),
      route: `/crm/quotes/${relatedQuote.value.id}`,
    });
  if (activeProject.value)
    rows.push({
      key: "project",
      stage: "项目",
      title: activeProject.value.code || activeProject.value.name,
      description: activeProject.value.name,
      amount: activeProject.value.contractAmount,
      date: activeProject.value.plannedStartDate,
      route: "/projects/list",
    });
  procurementRequests.value.forEach((item) =>
    rows.push({
      key: `request-${item.id}`,
      stage: "采购",
      title: item.code || "采购申请",
      description: item.partName || item.materialName,
      amount: item.totalAmount,
      date: item.expectedDate || item.requiredDate,
      route: "/procurement/requests",
    }),
  );
  purchaseOrders.value.forEach((item) =>
    rows.push({
      key: `order-${item.id}`,
      stage: "采购",
      title: item.code || "采购订单",
      description: item.partName,
      amount: item.orderAmount || item.amount,
      date: item.expectedDeliveryDate || item.orderedAt,
      route: "/procurement/orders",
    }),
  );
  goodsReceipts.value.forEach((item) =>
    rows.push({
      key: `receipt-${item.id}`,
      stage: "入库",
      title: item.code || "到货入库",
      description: item.partName,
      amount: item.amount,
      date: item.receivedDate,
      route: "/procurement/receipts",
    }),
  );
  materialIssues.value.forEach((item) =>
    rows.push({
      key: `issue-${item.id}`,
      stage: "领料",
      title: item.code || "项目领料",
      description: item.purpose,
      amount: item.totalAmount,
      date: item.issueDate,
      route: "/inventory/issues",
    }),
  );
  activeProjectDetail.value?.costEntries.forEach((item) =>
    rows.push({
      key: `cost-${item.id}`,
      stage: "成本",
      title: item.sourceNo || item.description,
      description: item.description,
      amount: item.amount,
      date: item.incurredDate,
      route: "/projects/costs",
    }),
  );
  receivables.value.forEach((item) =>
    rows.push({
      key: `receivable-${item.id}`,
      stage: "应收",
      title: item.code || item.sourceNo,
      description: item.customerName,
      amount: item.amount,
      date: item.dueDate,
      route: "/finance/receivables",
    }),
  );
  return rows.sort((a, b) => (a.date || "").localeCompare(b.date || ""));
});
const eventStageOptions = computed(() =>
  Array.from(new Set(events.value.map((item) => item.stage))),
);
const filteredEvents = computed(() =>
  stageFilter.value === "ALL"
    ? events.value
    : events.value.filter((item) => item.stage === stageFilter.value),
);
const gapAlerts = computed(() => {
  const rows: Array<{
    key: string;
    level: "high" | "medium";
    title: string;
    description: string;
    route?: string;
  }> = [];
  if (activeContract.value?.quoteId && !relatedQuote.value)
    rows.push({
      key: "missing-quote",
      level: "medium",
      title: "报价链路未打通",
      description: "合同存在报价ID，但报价详情未能加载，建议核对报价归档。",
      route: "/crm/quotes",
    });
  if (
    activeContract.value &&
    !activeProject.value &&
    activeContract.value.status === "ACTIVE"
  )
    rows.push({
      key: "missing-project",
      level: "high",
      title: "合同已生效但未承接项目",
      description: "后续采购、领料、成本和利润无法完整归集。",
      route: `/crm/contracts/${activeContract.value.id}`,
    });
  if (
    activeProject.value &&
    needsMaterialSupply.value &&
    procurementRequests.value.length === 0 &&
    purchaseOrders.value.length === 0 &&
    materialIssues.value.length === 0
  )
    rows.push({
      key: "missing-supply",
      level: "medium",
      title: "项目执行无采购/领料记录",
      description:
        "项目预算或成本包含材料类费用，建议补齐采购、入库或领料数据。",
      route: "/procurement/requests",
    });
  if (
    (activeContract.value || activeProject.value) &&
    receivables.value.length === 0
  )
    rows.push({
      key: "missing-receivable",
      level: "high",
      title: "缺少合同应收计划",
      description: "无法跟踪回款和现金流，请补齐应收节点。",
      route: "/crm/receivables",
    });
  if (
    summary.value.outstandingAmount > 0 &&
    receivables.value.some((item) => item.status === "OVERDUE")
  )
    rows.push({
      key: "overdue-receivable",
      level: "high",
      title: "存在逾期应收",
      description: `未回款 ${formatMoney(summary.value.outstandingAmount)}，建议生成催收跟进。`,
      route: "/finance/receivables",
    });
  if (summary.value.grossProfit < 0)
    rows.push({
      key: "negative-profit",
      level: "high",
      title: "项目毛利为负",
      description:
        "当前成本已超过合同额，需要复核成本归集或发起预算/变更审批。",
      route: "/projects/budget",
    });
  return rows;
});
const waterfallItems = computed(() => {
  const base = Math.max(
    summary.value.contractAmount,
    summary.value.supplyCost,
    summary.value.receivableAmount,
    1,
  );
  return [
    { key: "contract", label: "合同额", value: summary.value.contractAmount },
    { key: "cost", label: "成本", value: -summary.value.supplyCost },
    { key: "profit", label: "毛利", value: summary.value.grossProfit },
    { key: "received", label: "已回款", value: summary.value.receivedAmount },
    {
      key: "outstanding",
      label: "未回款",
      value: -summary.value.outstandingAmount,
    },
  ].map((item) => ({
    ...item,
    width: Math.max(6, Math.min(100, (Math.abs(item.value) / base) * 100)),
  }));
});

async function loadData() {
  loading.value = true;
  loadErrors.value = [];
  reset();
  try {
    relatedContract.value = props.contract || null;
    relatedProject.value =
      props.projectDetail?.project ||
      props.project ||
      projectSummaryFromContract(props.contract) ||
      null;
    relatedProjectDetail.value = props.projectDetail || null;

    await loadCrmChain();
    await loadProjectChain();
    await Promise.all([
      ...(auth.can("procurement:view")
        ? [collect("采购", loadProcurementChain)]
        : []),
      ...(auth.can("inventory:view")
        ? [collect("库存领料", loadInventoryChain)]
        : []),
      collect("应收回款", loadReceivableChain),
      ...(auth.can("project:view")
        ? [collect("利润", loadProfitability)]
        : []),
    ]);
  } finally {
    loading.value = false;
  }
}

async function loadCrmChain() {
  if (activeContract.value?.quoteId) {
    relatedQuote.value = await getQuote(activeContract.value.quoteId).catch(
      () => null,
    );
    if (relatedQuote.value?.opportunityId) {
      relatedOpportunity.value = await getOpportunity(
        relatedQuote.value.opportunityId,
      ).catch(() => null);
    }
  }
}

async function loadProjectChain() {
  if (!activeProject.value && activeContract.value) {
    const projects = await listProjects(0, 500).catch(() => ({ content: [] }));
    relatedProject.value =
      projects.content.find(
        (item) =>
          item.contractId === activeContract.value?.id ||
          item.contractCode === activeContract.value?.code,
      ) || projectSummaryFromContract(activeContract.value);
  }
  if (!relatedProjectDetail.value && activeProject.value?.id) {
    relatedProjectDetail.value = await getProject(activeProject.value.id).catch(
      () => null,
    );
  }
}

async function loadProcurementChain() {
  if (!activeProject.value) return;
  const [requests, orders, receipts, allocations] = await Promise.all([
    listPurchaseRequests({ costType: "PROJECT", page: 0, size: 500 }),
    listPurchaseOrders({ costType: "PROJECT", page: 0, size: 500 }),
    listGoodsReceipts(),
    listProcurementCostAllocations(),
  ]);
  procurementRequests.value = requests.content.filter(matchProjectCostTarget);
  purchaseOrders.value = orders.content.filter(matchProjectCostTarget);
  goodsReceipts.value = receipts.filter(matchProjectCostTarget);
  costAllocations.value = allocations.filter(matchProjectCostTarget);
}

async function loadInventoryChain() {
  if (!activeProject.value) return;
  const issues = await listMaterialIssues();
  materialIssues.value = issues.filter(
    (item) =>
      item.projectId === activeProject.value?.id ||
      item.projectCode === activeProject.value?.code ||
      item.projectName === activeProject.value?.name,
  );
}

async function loadReceivableChain() {
  const all = await listReceivables();
  receivables.value = all.filter((item) => {
    if (
      activeContract.value &&
      (item.contractId === activeContract.value.id ||
        item.contractCode === activeContract.value.code ||
        item.sourceNo === activeContract.value.code)
    )
      return true;
    return (
      Boolean(
        activeProject.value?.contractId &&
          item.contractId === activeProject.value.contractId,
      ) ||
      Boolean(
        activeProject.value?.contractCode &&
          (item.contractCode === activeProject.value.contractCode ||
            item.sourceNo === activeProject.value.contractCode),
      )
    );
  });
}

async function loadProfitability() {
  if (!activeProject.value) return;
  const rows = await listProjectProfitability();
  profitability.value =
    rows.find(
      (item) =>
        item.projectId === activeProject.value?.id ||
        item.projectCode === activeProject.value?.code,
    ) || null;
}

async function collect(label: string, loader: () => Promise<void>) {
  try {
    await loader();
  } catch (error) {
    console.warn(`[business-trace] ${label} load failed`, error);
    loadErrors.value.push(label);
  }
}

function matchProjectCostTarget(item: {
  costType?: string;
  costTargetId?: string;
  costTargetCode?: string;
  costTargetName?: string;
}) {
  if (!activeProject.value || item.costType !== "PROJECT") return false;
  return (
    item.costTargetId === activeProject.value.id ||
    item.costTargetCode === activeProject.value.code ||
    item.costTargetName === activeProject.value.name
  );
}

function reset() {
  relatedQuote.value = null;
  relatedOpportunity.value = null;
  relatedContract.value = null;
  relatedProject.value = null;
  relatedProjectDetail.value = null;
  profitability.value = null;
  procurementRequests.value = [];
  purchaseOrders.value = [];
  goodsReceipts.value = [];
  materialIssues.value = [];
  costAllocations.value = [];
  receivables.value = [];
}

function formatMoney(value?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(Number(value || 0));
}

function formatPercent(value: number) {
  return `${Number(value || 0).toFixed(1)}%`;
}

function projectSummaryFromContract(contract?: ServiceContract | null): Project | null {
  if (!contract?.projectId) return null;
  return {
    id: contract.projectId,
    customerId: contract.customerId,
    customerName: contract.customerName,
    contractId: contract.id,
    contractCode: contract.code,
    contractStatus: contract.status,
    code: contract.projectCode || "已承接项目",
    name: contract.projectName,
    managerName: contract.projectManagerName || "待项目管理部门分配",
    contractAmount: contract.amount,
    plannedStartDate: contract.startDate,
    plannedEndDate: contract.endDate,
    stage: contract.projectStage || "ENTRY",
    approvalStatus: contract.projectApprovalStatus || "PENDING",
    budgetAmount: 0,
    actualCost: 0,
    progress: 0,
  } as Project;
}

function moneyFormatter({ value }: { value: number | string }) {
  return formatMoney(Number(value || 0));
}

function formatDate(value?: string) {
  return value?.slice(0, 10);
}

function go(path: string) {
  router.push(path);
}

watch(sourceKey, loadData);
onMounted(loadData);
</script>

<style scoped>
.trace-card {
  margin-top: 16px;
  border-radius: 8px;
}

.trace-alert,
.trace-insights,
.trace-metrics,
.trace-table {
  margin-top: 16px;
}

.insight-card {
  min-height: 112px;
  padding: 14px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fbfcfe;
}

.insight-card span {
  color: #667085;
  font-size: 12px;
}

.insight-card strong {
  display: block;
  margin: 8px 0 6px;
  color: #172033;
  font-size: 24px;
  line-height: 1.1;
}

.insight-card strong.danger {
  color: #cf1322;
}

.insight-card p {
  overflow: hidden;
  margin: 0;
  color: #7a8699;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gap-list {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

.gap-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid #ffe7ba;
  border-radius: 8px;
  background: #fffaf0;
}

.gap-item.high {
  border-color: #ffd6d6;
  background: #fff7f7;
}

.gap-item strong {
  color: #172033;
}

.gap-item p {
  margin: 2px 0 0;
  color: #7a8699;
  font-size: 12px;
}

.trace-flow {
  display: grid;
  margin-top: 16px;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.trace-step {
  min-height: 118px;
  padding: 12px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
}

.trace-step.active {
  border-color: #b7d7ff;
  background: #f4f9ff;
}

.trace-step.selected {
  border-color: #1677ff;
  box-shadow: 0 0 0 1px #1677ff inset;
}

.step-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  color: #667085;
  font-size: 12px;
}

.trace-step strong {
  display: block;
  overflow: hidden;
  margin-top: 10px;
  color: #172033;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trace-step p {
  display: -webkit-box;
  overflow: hidden;
  margin: 6px 0 0;
  color: #7a8699;
  font-size: 12px;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.waterfall {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.waterfall-item {
  padding: 12px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
}

.waterfall-item span {
  color: #667085;
  font-size: 12px;
}

.waterfall-item strong {
  display: block;
  margin: 6px 0;
  color: #172033;
  font-size: 16px;
}

.waterfall-item strong.negative {
  color: #cf1322;
}

.waterfall-track {
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2f7;
}

.waterfall-bar {
  height: 100%;
  border-radius: inherit;
  background: #1677ff;
}

.waterfall-bar.negative {
  background: #ff4d4f;
}

@media (max-width: 1200px) {
  .trace-flow {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .waterfall {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .trace-flow {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .waterfall {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .gap-item {
    grid-template-columns: 1fr;
  }
}
</style>
