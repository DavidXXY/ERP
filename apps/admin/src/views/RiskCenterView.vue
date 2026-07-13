<template>
  <div class="page-stack">
    <a-card title="统一风险中心">
      <template #extra>
        <a-space>
          <a-button @click="showHighRisks">只看高风险</a-button>
          <a-button @click="showOverdueRisks">只看逾期</a-button>
          <a-button @click="exportRisks">导出</a-button>
          <a-button @click="handleSnapshot">今日快照</a-button>
          <a-button @click="handleEscalationScan">升级扫描</a-button>
          <a-button :loading="loading" @click="loadData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="风险总数" :value="filteredItems.length" suffix="项" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="高风险" :value="severityCount('HIGH')" suffix="项" :value-style="{ color: severityCount('HIGH') > 0 ? '#cf1322' : '#237804' }" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="SLA超时" :value="slaOverdueCount" suffix="项" :value-style="{ color: slaOverdueCount > 0 ? '#cf1322' : '#237804' }" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="已关闭/忽略" :value="resolvedCount" suffix="项" /></a-col>
      </a-row>

      <a-row :gutter="[16, 16]" class="metric-row" v-if="summary">
        <a-col :xs="24" :lg="12">
          <section class="risk-summary-panel">
            <h3>模块风险分布</h3>
            <a-space wrap>
              <a-tag v-for="module in summary.modules" :key="module.module" :color="module.highCount ? 'red' : module.slaOverdueCount ? 'orange' : 'blue'">
                {{ module.moduleName }} {{ module.totalCount }} / 高 {{ module.highCount }} / SLA {{ module.slaOverdueCount }}
              </a-tag>
            </a-space>
          </section>
        </a-col>
        <a-col :xs="24" :lg="12">
          <section class="risk-summary-panel">
            <h3>近14日快照趋势</h3>
            <a-space wrap>
              <a-tag v-for="point in summary.trends.slice(-7)" :key="point.date">
                {{ point.date.slice(5) }} 总 {{ point.totalCount }} 高 {{ point.highCount }} 关 {{ point.closedCount }}
              </a-tag>
            </a-space>
          </section>
        </a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-input-search v-model:value="keyword" allow-clear placeholder="搜索风险、对象、说明" style="width: 280px" />
        <a-select v-model:value="moduleFilter" allow-clear placeholder="全部模块" :options="moduleOptions" style="width: 150px" />
        <a-select v-model:value="severityFilter" allow-clear placeholder="全部等级" :options="severityOptions" style="width: 140px" />
        <a-select v-model:value="statusFilter" allow-clear placeholder="全部状态" :options="statusOptions" style="width: 140px" />
        <a-select v-model:value="workflowFilter" allow-clear placeholder="处理状态" :options="workflowOptions" style="width: 150px" />
        <a-select v-model:value="batchStatus" placeholder="批量处理" :options="workflowOptions" style="width: 150px" />
        <a-button :disabled="!selectedRowKeys.length" @click="handleBatchWorkflow">批量更新</a-button>
      </a-space>

      <a-alert
        v-if="loadNotes.length"
        class="section-alert"
        type="info"
        show-icon
        :message="`已按当前权限加载风险数据，跳过 ${loadNotes.length} 个不可用来源`"
      />

      <a-table
        :columns="columns"
        :data-source="filteredItems"
        :loading="loading"
        :pagination="{ pageSize: 12 }"
        :row-key="(record: RiskItem) => record.key"
        :row-selection="{ selectedRowKeys, onChange: onSelectionChange }"
        :scroll="{ x: 1180 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'risk'">
            <a-button type="link" class="table-link" @click="goRoute(record.route)">{{ record.title }}</a-button>
            <span class="table-subtitle">{{ record.subject }}</span>
          </template>
          <template v-else-if="column.key === 'module'"><a-tag>{{ record.moduleName }}</a-tag></template>
          <template v-else-if="column.key === 'severity'"><a-tag :color="severityColor(record.severity)">{{ severityLabel(record.severity) }}</a-tag></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag></template>
          <template v-else-if="column.key === 'workflow'">
            <a-tag :color="workflowColor(record.workflow.status)">{{ workflowLabel(record.workflow.status) }}</a-tag>
            <span v-if="record.workflow.owner" class="table-subtitle">{{ record.workflow.owner }}</span>
            <span v-if="record.workflow.updatedAt" class="table-subtitle">{{ dateOnly(record.workflow.updatedAt) }}</span>
          </template>
          <template v-else-if="column.key === 'sla'">
            <a-tag :color="record.slaOverdue ? 'red' : record.dueAt ? 'blue' : 'default'">{{ record.slaOverdue ? '已超时' : record.dueAt ? 'SLA' : '-' }}</a-tag>
            <span v-if="record.dueAt" class="table-subtitle">{{ record.dueAt.slice(0, 16).replace('T', ' ') }}</span>
          </template>
          <template v-else-if="column.key === 'amount'">{{ record.amount ? formatMoney(record.amount) : '-' }}</template>
          <template v-else-if="column.key === 'date'">{{ record.date || '-' }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openWorkflow(record)">流转</a-button>
              <a-button type="link" size="small" @click="goRoute(record.route)">业务</a-button>
            </a-space>
          </template>
        </template>
        <template #emptyText>当前没有需要处理的业务风险</template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="workflowOpen" title="风险处理" :width="520">
      <template v-if="activeRisk">
        <a-descriptions bordered size="small" :column="1">
          <a-descriptions-item label="风险事项">{{ activeRisk.title }}</a-descriptions-item>
          <a-descriptions-item label="对象">{{ activeRisk.subject }}</a-descriptions-item>
          <a-descriptions-item label="说明">{{ activeRisk.description }}</a-descriptions-item>
          <a-descriptions-item label="等级"><a-tag :color="severityColor(activeRisk.severity)">{{ severityLabel(activeRisk.severity) }}</a-tag></a-descriptions-item>
        </a-descriptions>

        <a-form layout="vertical" style="margin-top:16px">
          <a-form-item label="处理状态">
            <a-radio-group v-model:value="workflowForm.status" button-style="solid">
              <a-radio-button value="UNCLAIMED">未认领</a-radio-button>
              <a-radio-button value="CLAIMED">已认领</a-radio-button>
              <a-radio-button value="PROCESSING">处理中</a-radio-button>
              <a-radio-button value="IGNORED">忽略</a-radio-button>
              <a-radio-button value="CLOSED">关闭</a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="处理人"><a-input v-model:value="workflowForm.owner" placeholder="默认当前用户" /></a-form-item>
          <a-form-item label="处理备注"><a-textarea v-model:value="workflowForm.note" :rows="3" /></a-form-item>
          <a-form-item v-if="workflowForm.status === 'CLOSED' || workflowForm.status === 'IGNORED'" label="关闭/忽略原因">
            <a-textarea v-model:value="workflowForm.reason" :rows="2" />
          </a-form-item>
        </a-form>
        <a-space>
          <a-button type="primary" @click="saveWorkflow">保存处理状态</a-button>
          <a-button @click="goRoute(activeRisk.route)">打开业务页面</a-button>
        </a-space>

        <a-divider>处理轨迹</a-divider>
        <a-spin :spinning="actionLoading">
          <a-empty v-if="workflowActions.length === 0" description="暂无处理记录" />
          <a-timeline v-else>
            <a-timeline-item v-for="action in workflowActions" :key="`${action.createdAt}-${action.toStatus}`" :color="workflowColor(action.toStatus)">
              <strong>{{ workflowLabel(action.fromStatus || 'UNCLAIMED') }} → {{ workflowLabel(action.toStatus) }}</strong>
              <p style="margin:2px 0;color:#8c8c8c;font-size:12px">{{ action.operatorName || '-' }} · {{ action.createdAt?.slice(0, 19)?.replace('T', ' ') || '-' }}</p>
              <p v-if="action.owner" style="margin:2px 0;font-size:12px">处理人：{{ action.owner }}</p>
              <p v-if="action.note" style="margin:2px 0;font-size:12px">备注：{{ action.note }}</p>
              <p v-if="action.reason" style="margin:2px 0;font-size:12px">原因：{{ action.reason }}</p>
            </a-timeline-item>
          </a-timeline>
        </a-spin>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { useRouter } from "vue-router";
import { listRenewals } from "@/api/crm";
import { listFinancePayables, listFinanceReceivables } from "@/api/finance";
import { listReplenishmentSuggestions } from "@/api/inventory";
import { getOfficeWorkbench } from "@/api/office";
import { listProcurementMatching } from "@/api/procurement";
import { listProjectProfitability } from "@/api/project";
import { listQualificationWarnings } from "@/api/qualification";
import { batchUpdateRiskWorkflow, getRiskSummary, listRiskItems, listRiskWorkflowActions, listRiskWorkflows, scanRiskEscalations, snapshotRiskToday, updateRiskWorkflow, type RiskItemResponse, type RiskSummaryResponse, type RiskWorkflowActionResponse, type RiskWorkflowStatus } from "@/api/risk";
import { useAuthStore } from "@/stores/auth";
import { downloadCsv } from "@/utils/csv";

type RiskSeverity = "HIGH" | "MEDIUM" | "LOW";
type RiskStatus = "OPEN" | "PENDING" | "OVERDUE";
type WorkflowStatus = RiskWorkflowStatus;
type RiskWorkflow = { status: WorkflowStatus; owner?: string; note?: string; reason?: string; updatedAt?: string };
type RiskItem = {
  key: string;
  module: string;
  moduleName: string;
  title: string;
  subject: string;
  description: string;
  severity: RiskSeverity;
  status: RiskStatus;
  amount?: number;
  date?: string;
  route: string;
  ruleCode?: string;
  slaHours?: number;
  dueAt?: string;
  slaOverdue: boolean;
  workflow: RiskWorkflow;
};

const FILTER_STORAGE_KEY = "ops.riskCenter.filters";
const WORKFLOW_STORAGE_KEY = "ops.riskCenter.workflow";
const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const items = ref<RiskItem[]>([]);
const summary = ref<RiskSummaryResponse>();
const loadNotes = ref<string[]>([]);
const keyword = ref("");
const moduleFilter = ref<string>();
const severityFilter = ref<RiskSeverity>();
const statusFilter = ref<RiskStatus>();
const workflowFilter = ref<WorkflowStatus>();
const workflowOpen = ref(false);
const activeRisk = ref<RiskItem | null>(null);
const workflowStore = ref<Record<string, RiskWorkflow>>({});
const workflowActions = ref<RiskWorkflowActionResponse[]>([]);
const actionLoading = ref(false);
const selectedRowKeys = ref<string[]>([]);
const batchStatus = ref<WorkflowStatus>("CLAIMED");
const workflowForm = reactive({ status: "UNCLAIMED" as WorkflowStatus, owner: "", note: "", reason: "" });

const columns = [
  { title: "风险事项 / 对象", key: "risk", width: 320 },
  { title: "模块", key: "module", width: 110 },
  { title: "等级", key: "severity", width: 100 },
  { title: "状态", key: "status", width: 100 },
  { title: "处理", key: "workflow", width: 140 },
  { title: "SLA", key: "sla", width: 140 },
  { title: "金额", key: "amount", width: 140 },
  { title: "日期", key: "date", width: 120 },
  { title: "说明", dataIndex: "description", width: 300 },
  { title: "操作", key: "action", width: 90, fixed: "right" as const },
];

const severityOptions = [
  { label: "高风险", value: "HIGH" },
  { label: "中风险", value: "MEDIUM" },
  { label: "低风险", value: "LOW" },
];
const statusOptions = [
  { label: "待处理", value: "OPEN" },
  { label: "处理中", value: "PENDING" },
  { label: "已逾期", value: "OVERDUE" },
];
const workflowOptions = [
  { label: "未认领", value: "UNCLAIMED" },
  { label: "已认领", value: "CLAIMED" },
  { label: "处理中", value: "PROCESSING" },
  { label: "已忽略", value: "IGNORED" },
  { label: "已关闭", value: "CLOSED" },
];

const moduleOptions = computed(() => Array.from(new Map(items.value.map((item) => [item.module, { label: item.moduleName, value: item.module }])).values()));
const filteredItems = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const text = `${item.title} ${item.subject} ${item.description}`.toLowerCase();
    return (!moduleFilter.value || item.module === moduleFilter.value)
      && (!severityFilter.value || item.severity === severityFilter.value)
      && (!statusFilter.value || item.status === statusFilter.value)
      && (!workflowFilter.value || item.workflow.status === workflowFilter.value)
      && (!term || text.includes(term));
  });
});
const riskAmount = computed(() => filteredItems.value.reduce((sum, item) => sum + Number(item.amount || 0), 0));
const resolvedCount = computed(() => items.value.filter((item) => item.workflow.status === "CLOSED" || item.workflow.status === "IGNORED").length);
const slaOverdueCount = computed(() => filteredItems.value.filter((item) => item.slaOverdue).length);

restoreWorkflow();
restoreFilters();
onMounted(loadData);
watch([keyword, moduleFilter, severityFilter, statusFilter, workflowFilter], persistFilters);

async function loadData() {
  loading.value = true;
  loadNotes.value = [];
  const rows: Array<Omit<RiskItem, "workflow">> = [];
  try {
    await loadSummary();
    if (await loadBackendRiskItems()) return;
    await Promise.all([
      loadWorkflowStore(),
      loadOffice(rows),
      loadInventory(rows),
      loadProcurement(rows),
      loadProjects(rows),
      loadFinance(rows),
      loadQualification(rows),
      loadRenewals(rows),
    ]);
    items.value = rows.map(applyWorkflow).sort((a, b) => severityRank(b.severity) - severityRank(a.severity) || (b.date || "").localeCompare(a.date || ""));
  } catch (error) {
    message.error(error instanceof Error ? error.message : "风险数据加载失败");
  } finally {
    loading.value = false;
  }
}

async function loadBackendRiskItems() {
  try {
    const rows = await listRiskItems();
    const workflowRows = rows
      .filter((item) => item.workflow)
      .map((item) => [item.key, toWorkflow(item.workflow as NonNullable<RiskItemResponse["workflow"]>)] as const);
    workflowStore.value = { ...workflowStore.value, ...Object.fromEntries(workflowRows) };
    localStorage.setItem(WORKFLOW_STORAGE_KEY, JSON.stringify(workflowStore.value));
    items.value = rows.map((item) => applyWorkflow({
      key: item.key,
      module: item.module,
      moduleName: item.moduleName,
      title: item.title,
      subject: item.subject,
      description: item.description,
      severity: item.severity,
      status: item.status,
      amount: Number(item.amount || 0),
      date: item.date,
      route: item.route,
      ruleCode: item.ruleCode,
      slaHours: item.slaHours,
      dueAt: item.dueAt,
      slaOverdue: item.slaOverdue,
    })).sort((a, b) => severityRank(b.severity) - severityRank(a.severity) || (b.date || "").localeCompare(a.date || ""));
    return true;
  } catch {
    loadNotes.value.push("后端风险聚合");
    return false;
  }
}

async function loadOffice(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!canAny(["office:approval:view", "office:notification:view", "office:expense:view", "office:outsource:view"])) return;
  await safeLoad("OA协同", async () => {
    const workbench = await getOfficeWorkbench();
    workbench.warnings.forEach((item) => rows.push({
      key: `office-warning-${item.type}-${item.id}`,
      module: "office",
      moduleName: "OA协同",
      title: item.title,
      subject: item.content,
      description: item.content,
      severity: normalizeSeverity(item.severity),
      status: item.type.includes("OVERDUE") ? "OVERDUE" : "OPEN",
      date: dateOnly(item.createdAt),
      route: item.route,
      slaOverdue: false,
    }));
    workbench.todos.slice(0, 20).forEach((item) => rows.push({
      key: `office-todo-${item.type}-${item.id}`,
      module: "office",
      moduleName: "OA协同",
      title: item.title,
      subject: item.subtitle,
      description: "待办事项未处理",
      severity: normalizeSeverity(item.priority),
      status: "PENDING",
      amount: Number(item.amount || 0),
      date: dateOnly(item.createdAt),
      route: item.route,
      slaOverdue: false,
    }));
  });
}

async function loadInventory(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!auth.can("inventory:view")) return;
  await safeLoad("库存管理", async () => {
    const suggestions = await listReplenishmentSuggestions();
    suggestions.forEach((item) => rows.push({
      key: `inventory-${item.partId}`,
      module: "inventory",
      moduleName: "库存管理",
      title: "补货建议",
      subject: `${item.partName} · ${item.partCode || "-"}`,
      description: item.reason,
      severity: normalizeSeverity(item.priority),
      status: "OPEN",
      amount: Number(item.suggestedQty || 0),
      route: "/inventory/analytics",
      slaOverdue: false,
    }));
  });
}

async function loadProcurement(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!auth.can("procurement:view")) return;
  await safeLoad("供应链采购", async () => {
    const matching = await listProcurementMatching();
    matching.filter((item) => item.matchStatus !== "MATCHED").forEach((item) => rows.push({
      key: `procurement-${item.orderId}`,
      module: "procurement",
      moduleName: "供应链采购",
      title: matchStatusLabel(item.matchStatus),
      subject: `${item.orderCode || "-"} · ${item.supplierName || "未知供应商"}`,
      description: item.riskMessage,
      severity: item.matchStatus === "AMOUNT_MISMATCH" ? "HIGH" : "MEDIUM",
      status: item.matchStatus === "RECEIVING" ? "PENDING" : "OPEN",
      amount: Number(item.payableAmount || item.receiptAmount || item.orderAmount || 0),
      route: "/procurement/p2p",
      slaOverdue: false,
    }));
  });
}

async function loadProjects(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!auth.can("project:view")) return;
  await safeLoad("项目管理", async () => {
    const profitability = await listProjectProfitability();
    profitability.filter((item) => item.riskLevel !== "LOW").forEach((item) => rows.push({
      key: `project-${item.projectId}`,
      module: "project",
      moduleName: "项目管理",
      title: item.riskMessage,
      subject: `${item.projectName} · ${item.customerName || "未关联客户"}`,
      description: `毛利率 ${formatPercent(item.grossMarginRate)}，预算使用 ${formatPercent(item.budgetUsageRate)}`,
      severity: normalizeSeverity(item.riskLevel),
      status: "OPEN",
      amount: Number(item.grossMargin || 0),
      route: "/projects/list",
      slaOverdue: false,
    }));
  });
}

async function loadFinance(rows: Array<Omit<RiskItem, "workflow">>) {
  await safeLoad("财务资金", async () => {
    if (auth.can("finance:receivable:view")) {
      const receivables = await listFinanceReceivables();
      receivables.filter((item) => item.status === "OVERDUE" || Number(item.outstandingAmount || 0) > 0 && isPast(item.dueDate)).forEach((item) => rows.push({
        key: `finance-receivable-${item.id}`,
        module: "finance",
        moduleName: "财务资金",
        title: "应收逾期",
        subject: `${item.customerName} · ${item.contractCode || item.sourceNo}`,
        description: `到期日 ${item.dueDate}`,
        severity: "HIGH",
        status: "OVERDUE",
        amount: Number(item.outstandingAmount || 0),
        date: item.dueDate,
        route: "/finance/receivables",
        slaOverdue: false,
      }));
    }
    if (auth.can("finance:payable:view")) {
      const payables = await listFinancePayables();
      payables.filter((item) => item.overdue).forEach((item) => rows.push({
        key: `finance-payable-${item.id}`,
        module: "finance",
        moduleName: "财务资金",
        title: "应付逾期",
        subject: `${item.supplierName} · ${item.orderCode}`,
        description: `到期日 ${item.dueDate}`,
        severity: "MEDIUM",
        status: "OVERDUE",
        amount: Number(item.outstandingAmount || 0),
        date: item.dueDate,
        route: "/finance/payables",
        slaOverdue: false,
      }));
    }
  });
}

async function loadQualification(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!auth.can("qualification:warning:view")) return;
  await safeLoad("资质管理", async () => {
    const warnings = await listQualificationWarnings();
    warnings.forEach((item) => rows.push({
      key: `qualification-${item.sourceType}-${item.sourceId}-${item.warningType}`,
      module: "qualification",
      moduleName: "资质管理",
      title: item.title,
      subject: item.sourceName,
      description: item.status === "OVERDUE" ? `已逾期 ${Math.abs(item.daysLeft)} 天` : `剩余 ${item.daysLeft} 天`,
      severity: item.level === "DANGER" ? "HIGH" : item.level === "WARNING" ? "MEDIUM" : "LOW",
      status: item.status === "OVERDUE" ? "OVERDUE" : "OPEN",
      date: item.dueDate,
      route: "/qualification/warnings",
      slaOverdue: false,
    }));
  });
}

async function loadRenewals(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!auth.can("crm:renewal:view")) return;
  await safeLoad("CRM", async () => {
    const renewals = await listRenewals();
    renewals.filter((item) => item.renewalRisk !== "LOW").forEach((item) => rows.push({
      key: `crm-renewal-${item.contractId}`,
      module: "crm",
      moduleName: "CRM",
      title: item.renewalRisk === "EXPIRED" ? "合同已到期" : "合同续约风险",
      subject: `${item.customerName} · ${item.contractCode}`,
      description: item.daysRemaining < 0 ? `已到期 ${Math.abs(item.daysRemaining)} 天` : `剩余 ${item.daysRemaining} 天`,
      severity: item.renewalRisk === "EXPIRED" || item.renewalRisk === "HIGH" ? "HIGH" : "MEDIUM",
      status: item.daysRemaining < 0 ? "OVERDUE" : "OPEN",
      amount: Number(item.outstandingAmount || item.amount || 0),
      date: item.endDate,
      route: "/crm/renewals",
      slaOverdue: false,
    }));
  });
}

async function safeLoad(source: string, loader: () => Promise<void>) {
  try {
    await loader();
  } catch {
    loadNotes.value.push(source);
  }
}

function canAny(permissions: string[]) { return permissions.some((permission) => auth.can(permission)); }
function goRoute(route: string) { if (route) router.push(route); }
function onSelectionChange(keys: string[]) { selectedRowKeys.value = keys; }
function showHighRisks() { severityFilter.value = "HIGH"; statusFilter.value = undefined; }
function showOverdueRisks() { statusFilter.value = "OVERDUE"; severityFilter.value = undefined; }
function exportRisks() {
  const headers = ["模块", "风险事项", "对象", "等级", "业务状态", "处理状态", "金额", "日期", "说明"];
  const rows = filteredItems.value.map((item) => [
    item.moduleName,
    item.title,
    item.subject,
    severityLabel(item.severity),
    statusLabel(item.status),
    workflowLabel(item.workflow.status),
    item.amount ? String(item.amount) : "",
    item.date || "",
    item.description,
  ]);
  downloadCsv(`risk-center-${new Date().toISOString().slice(0, 10)}.csv`, headers, rows);
}

async function loadSummary() {
  try {
    summary.value = await getRiskSummary(14);
  } catch {
    summary.value = undefined;
  }
}

async function handleBatchWorkflow() {
  if (!selectedRowKeys.value.length) return;
  try {
    const owner = auth.user?.displayName || "";
    const rows = await batchUpdateRiskWorkflow({
      riskKeys: selectedRowKeys.value,
      status: batchStatus.value,
      owner,
      note: "批量处理",
    });
    workflowStore.value = {
      ...workflowStore.value,
      ...Object.fromEntries(rows.map((item) => [item.riskKey, toWorkflow(item)])),
    };
    localStorage.setItem(WORKFLOW_STORAGE_KEY, JSON.stringify(workflowStore.value));
    selectedRowKeys.value = [];
    message.success(`已批量更新 ${rows.length} 项风险`);
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "批量处理失败");
  }
}

async function handleSnapshot() {
  try {
    const count = await snapshotRiskToday();
    message.success(count ? `已归档 ${count} 条今日风险快照` : "今日风险快照已是最新");
    await loadSummary();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "快照归档失败");
  }
}

async function handleEscalationScan() {
  try {
    const count = await scanRiskEscalations();
    message.success(count ? `已生成 ${count} 条升级通知` : "暂无需要升级的超时风险");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "升级扫描失败");
  }
}
function severityCount(severity: RiskSeverity) { return filteredItems.value.filter((item) => item.severity === severity).length; }
function severityRank(severity: string) { return ({ HIGH: 3, MEDIUM: 2, LOW: 1 } as Record<string, number>)[severity] || 0; }
function normalizeSeverity(value: string): RiskSeverity { return value === "HIGH" ? "HIGH" : value === "MEDIUM" ? "MEDIUM" : "LOW"; }
function severityLabel(value: RiskSeverity) { return ({ HIGH: "高风险", MEDIUM: "中风险", LOW: "低风险" } as Record<RiskSeverity, string>)[value]; }
function severityColor(value: RiskSeverity) { return ({ HIGH: "red", MEDIUM: "orange", LOW: "green" } as Record<RiskSeverity, string>)[value]; }
function statusLabel(value: RiskStatus) { return ({ OPEN: "待处理", PENDING: "处理中", OVERDUE: "已逾期" } as Record<RiskStatus, string>)[value]; }
function statusColor(value: RiskStatus) { return ({ OPEN: "blue", PENDING: "orange", OVERDUE: "red" } as Record<RiskStatus, string>)[value]; }
function workflowLabel(value: WorkflowStatus) { return ({ UNCLAIMED: "未认领", CLAIMED: "已认领", PROCESSING: "处理中", IGNORED: "已忽略", CLOSED: "已关闭" } as Record<WorkflowStatus, string>)[value]; }
function workflowColor(value: WorkflowStatus) { return ({ UNCLAIMED: "default", CLAIMED: "blue", PROCESSING: "orange", IGNORED: "purple", CLOSED: "green" } as Record<WorkflowStatus, string>)[value]; }
function matchStatusLabel(value: string) { return ({ RECEIVING: "待入库", PAYABLE_MISSING: "缺应付", AMOUNT_MISMATCH: "金额不一致", CANCELLED: "已取消" } as Record<string, string>)[value] || value; }
function isPast(value?: string) { return Boolean(value) && new Date(value as string).getTime() < Date.now(); }
function dateOnly(value?: string) { return value ? value.slice(0, 10) : undefined; }
function formatPercent(value: number) { return `${Number(value || 0).toFixed(1)}%`; }
function formatMoney(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0); }
function moneyFormatter(value: number | string) { return formatMoney(Number(value)); }

function applyWorkflow(item: Omit<RiskItem, "workflow"> | RiskItem): RiskItem {
  return { ...item, workflow: workflowStore.value[item.key] || { status: "UNCLAIMED" } };
}

function openWorkflow(record: RiskItem) {
  activeRisk.value = record;
  Object.assign(workflowForm, {
    status: record.workflow.status,
    owner: record.workflow.owner || auth.user?.displayName || "",
    note: record.workflow.note || "",
    reason: record.workflow.reason || "",
  });
  workflowOpen.value = true;
  loadWorkflowActions(record.key);
}

async function saveWorkflow() {
  if (!activeRisk.value) return;
  let next: RiskWorkflow;
  try {
    const saved = await updateRiskWorkflow({
      riskKey: activeRisk.value.key,
      status: workflowForm.status,
      owner: workflowForm.owner || auth.user?.displayName || "",
      note: workflowForm.note,
      reason: workflowForm.reason,
    });
    next = {
      status: saved.status,
      owner: saved.owner,
      note: saved.note,
      reason: saved.reason,
      updatedAt: saved.updatedAt || saved.processedAt,
    };
  } catch {
    next = {
      status: workflowForm.status,
      owner: workflowForm.owner || auth.user?.displayName || "",
      note: workflowForm.note,
      reason: workflowForm.reason,
      updatedAt: new Date().toISOString(),
    };
    message.warning("后端风险状态保存失败，已暂存到当前浏览器");
  }
  workflowStore.value = { ...workflowStore.value, [activeRisk.value.key]: next };
  localStorage.setItem(WORKFLOW_STORAGE_KEY, JSON.stringify(workflowStore.value));
  items.value = items.value.map((item) => item.key === activeRisk.value?.key ? { ...item, workflow: next } : item);
  await loadWorkflowActions(activeRisk.value.key);
  workflowOpen.value = false;
  message.success("风险处理状态已更新");
}

async function loadWorkflowActions(riskKey: string) {
  actionLoading.value = true;
  try {
    workflowActions.value = await listRiskWorkflowActions(riskKey);
  } catch {
    workflowActions.value = [];
  } finally {
    actionLoading.value = false;
  }
}

async function loadWorkflowStore() {
  try {
    const rows = await listRiskWorkflows();
    workflowStore.value = {
      ...workflowStore.value,
      ...Object.fromEntries(rows.map((item) => [item.riskKey, toWorkflow(item)])),
    };
  } catch {
    loadNotes.value.push("风险处理状态");
  }
}

function toWorkflow(item: NonNullable<RiskItemResponse["workflow"]>): RiskWorkflow {
  return {
    status: item.status,
    owner: item.owner,
    note: item.note,
    reason: item.reason,
    updatedAt: item.updatedAt || item.processedAt,
  };
}

function restoreWorkflow() {
  try {
    workflowStore.value = JSON.parse(localStorage.getItem(WORKFLOW_STORAGE_KEY) || "{}");
  } catch {
    workflowStore.value = {};
  }
}

function restoreFilters() {
  try {
    const filters = JSON.parse(localStorage.getItem(FILTER_STORAGE_KEY) || "{}");
    keyword.value = filters.keyword || "";
    moduleFilter.value = filters.moduleFilter;
    severityFilter.value = filters.severityFilter;
    statusFilter.value = filters.statusFilter;
    workflowFilter.value = filters.workflowFilter;
  } catch {
    // Ignore invalid local preference payloads.
  }
}

function persistFilters() {
  localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify({
    keyword: keyword.value,
    moduleFilter: moduleFilter.value,
    severityFilter: severityFilter.value,
    statusFilter: statusFilter.value,
    workflowFilter: workflowFilter.value,
  }));
}
</script>

<style scoped>
.risk-summary-panel {
  min-height: 76px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  background: #fafafa;
}
.risk-summary-panel h3 {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
}
</style>
