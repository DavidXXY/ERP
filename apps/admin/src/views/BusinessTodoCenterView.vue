<template>
  <div class="business-todo-page">
    <a-card :bordered="false" class="todo-hero">
      <div class="hero-main">
        <div>
          <a-typography-title :level="3">业务待办中心</a-typography-title>
          <a-typography-text type="secondary">
            统一处理业务待办和审批事项；消息中心只保留通知阅读。
          </a-typography-text>
        </div>
        <a-space>
          <a-button @click="go('/office/notifications')">消息中心</a-button>
          <a-button @click="resetFilters">重置筛选</a-button>
          <a-button type="primary" :loading="loading" @click="loadData">刷新待办</a-button>
        </a-space>
      </div>

      <a-row :gutter="[16, 16]" class="metric-grid">
        <a-col :xs="12" :lg="6">
          <div class="metric-card">
            <span>全部待办</span>
            <strong>{{ filteredTodos.length }}</strong>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="metric-card high">
            <span>高优先</span>
            <strong>{{ highCount }}</strong>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="metric-card overdue">
            <span>已超时</span>
            <strong>{{ overdueCount }}</strong>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="metric-card amount">
            <span>金额敞口</span>
            <strong>{{ formatCurrency(totalExposure) }}</strong>
          </div>
        </a-col>
      </a-row>
    </a-card>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="6" v-for="card in commandCards" :key="card.key">
        <a-card :bordered="false" class="command-card">
          <div class="command-title">{{ card.title }}</div>
          <div class="command-count">{{ card.count }}</div>
          <a-typography-text type="secondary">{{ card.description }}</a-typography-text>
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="todo-table-card">
      <template #title>统一待办队列</template>
      <template #extra>
        <a-space wrap>
          <a-input-search v-model:value="keyword" allow-clear placeholder="搜索客户、项目、单号、说明" style="width: 260px" />
          <a-select v-model:value="moduleFilter" style="width: 150px">
            <a-select-option value="ALL">全部模块</a-select-option>
            <a-select-option v-for="item in moduleOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
          </a-select>
          <a-select v-model:value="priorityFilter" style="width: 130px">
            <a-select-option value="ALL">全部优先级</a-select-option>
            <a-select-option value="HIGH">高优先</a-select-option>
            <a-select-option value="MEDIUM">中优先</a-select-option>
            <a-select-option value="LOW">低优先</a-select-option>
          </a-select>
          <a-select v-model:value="statusFilter" style="width: 130px">
            <a-select-option value="ALL">全部状态</a-select-option>
            <a-select-option value="OVERDUE">已超时</a-select-option>
            <a-select-option value="OPEN">待处理</a-select-option>
            <a-select-option value="PROCESSING">处理中</a-select-option>
            <a-select-option value="DONE">已处理</a-select-option>
          </a-select>
        </a-space>
      </template>

      <a-alert
        v-if="loadErrors.length"
        type="warning"
        show-icon
        class="load-alert"
        :message="`部分模块加载失败：${loadErrors.join('、')}`"
      />

      <a-table
        row-key="key"
        :columns="columns"
        :data-source="filteredTodos"
        :loading="loading"
        :pagination="{ pageSize: 12, showSizeChanger: true }"
        :custom-row="todoRowProps"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <div class="todo-title">{{ record.title }}</div>
            <div class="todo-subject">{{ record.subject }}</div>
          </template>
          <template v-else-if="column.key === 'module'">
            <a-tag>{{ record.moduleName }}</a-tag>
          </template>
          <template v-else-if="column.key === 'priority'">
            <a-tag :color="priorityColor(record.priority)">{{ priorityLabel(record.priority) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ record.amount ? formatCurrency(record.amount) : "-" }}
          </template>
          <template v-else-if="column.key === 'dueDate'">
            {{ record.dueDate ? formatDate(record.dueDate) : "-" }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <span>{{ record.action }}</span>
              <a-button
                v-if="record.automation"
                type="primary"
                size="small"
                :loading="actionLoadingKey === record.key"
                @click.stop="runAutomation(record)"
              >
                一键生成
              </a-button>
              <a-button type="link" size="small" @click.stop="openTodoDetail(record)">
                查看详情
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="待办详情" width="680px">
      <template v-if="selectedTodo">
        <a-space direction="vertical" size="middle" style="width: 100%">
          <a-descriptions bordered size="small" :column="2">
            <a-descriptions-item label="事项" :span="2">{{ selectedTodo.title }}</a-descriptions-item>
            <a-descriptions-item label="说明" :span="2">{{ selectedTodo.subject || "-" }}</a-descriptions-item>
            <a-descriptions-item label="模块">
              <a-tag>{{ selectedTodo.moduleName }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="优先级">
              <a-tag :color="priorityColor(selectedTodo.priority)">{{ priorityLabel(selectedTodo.priority) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="statusColor(selectedTodo.status)">{{ statusLabel(selectedTodo.status) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="责任人">{{ selectedTodo.owner || "-" }}</a-descriptions-item>
            <a-descriptions-item label="金额">{{ selectedTodo.amount ? formatCurrency(selectedTodo.amount) : "-" }}</a-descriptions-item>
            <a-descriptions-item label="到期/时间">{{ selectedTodo.dueDate ? formatDate(selectedTodo.dueDate) : "-" }}</a-descriptions-item>
            <a-descriptions-item label="推荐动作" :span="2">{{ selectedTodo.action }}</a-descriptions-item>
          </a-descriptions>

          <a-alert
            v-if="selectedTodo.automation"
            type="info"
            show-icon
            message="该待办支持一键生成后续业务单据"
          />

          <a-space wrap>
            <a-button
              v-if="selectedTodo.automation"
              type="primary"
              :loading="actionLoadingKey === selectedTodo.key"
              @click="runAutomation(selectedTodo)"
            >
              一键生成
            </a-button>
            <a-button type="primary" @click="handleTodoAction(selectedTodo)">
              {{ isOfficeApprovalTodo(selectedTodo) ? "打开审批详情" : "进入业务页面" }}
            </a-button>
            <a-button @click="detailOpen = false">关闭</a-button>
          </a-space>
        </a-space>
      </template>
    </a-drawer>

    <ApprovalCenterView ref="approvalCenterRef" embedded drawer-only @changed="loadData" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { createApproval, getOfficeWorkbench } from "@/api/office";
import { listRiskItems } from "@/api/risk";
import { createFollowUp, listContracts, listOpportunities, listQuotes } from "@/api/crm";
import { listFinancePayables, listFinanceReceivables, listPaymentApplications } from "@/api/finance";
import { listProcurementMatching, listPurchaseRequests } from "@/api/procurement";
import { createProject, listProjectProfitability } from "@/api/project";
import { useAuthStore } from "@/stores/auth";
import ApprovalCenterView from "@/views/office/ApprovalCenterView.vue";

type Priority = "HIGH" | "MEDIUM" | "LOW";
type TodoStatus = "OPEN" | "PROCESSING" | "OVERDUE" | "DONE";

type BusinessTodo = {
  key: string;
  module: string;
  moduleName: string;
  title: string;
  subject: string;
  priority: Priority;
  status: TodoStatus;
  owner?: string;
  amount?: number;
  dueDate?: string;
  route: string;
  action: string;
  entityId?: string;
  automation?: AutomationAction;
};

type AutomationAction =
  | {
      type: "CREATE_FOLLOW_UP";
      customerId: string;
      customerName: string;
      opportunityId?: string;
      opportunityCode?: string;
      ownerName?: string;
      subject: string;
      content: string;
      nextAction?: string;
    }
  | {
      type: "CREATE_PROJECT_DRAFT";
      contractId: string;
      customerId: string;
      customerName: string;
      projectName: string;
      contractCode?: string;
      contractAmount: number;
      startDate: string;
      endDate: string;
    }
  | {
      type: "CREATE_BUDGET_ADJUST_APPROVAL";
      projectCode?: string;
      projectName: string;
      amount: number;
      reason: string;
    };

const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const actionLoadingKey = ref("");
const approvalCenterRef = ref<InstanceType<typeof ApprovalCenterView>>();
const todos = ref<BusinessTodo[]>([]);
const selectedTodo = ref<BusinessTodo | null>(null);
const detailOpen = ref(false);
const loadErrors = ref<string[]>([]);
const keyword = ref("");
const moduleFilter = ref("ALL");
const priorityFilter = ref("ALL");
const statusFilter = ref("ALL");

const columns = [
  { title: "事项", key: "title", width: 300 },
  { title: "模块", key: "module", width: 120 },
  { title: "优先级", key: "priority", width: 100 },
  { title: "状态", key: "status", width: 100 },
  { title: "责任人", dataIndex: "owner", key: "owner", width: 120 },
  { title: "金额", key: "amount", width: 130 },
  { title: "到期/时间", key: "dueDate", width: 130 },
  { title: "推荐动作", key: "action", width: 210 },
];

const moduleOptions = computed(() => {
  const map = new Map<string, string>();
  todos.value.forEach((item) => {
    const value = normalizeModuleCode(item.module);
    if (!map.has(value)) map.set(value, item.moduleName);
  });
  return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
});

const filteredTodos = computed(() => {
  const text = keyword.value.trim().toLowerCase();
  return todos.value.filter((item) => {
    if (moduleFilter.value !== "ALL" && normalizeModuleCode(item.module) !== moduleFilter.value) return false;
    if (priorityFilter.value !== "ALL" && item.priority !== priorityFilter.value) return false;
    if (statusFilter.value !== "ALL" && item.status !== statusFilter.value) return false;
    if (!text) return true;
    return [item.title, item.subject, item.owner, item.action, item.moduleName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(text));
  });
});

function normalizeModuleCode(value?: string) {
  return (value || "").trim().toUpperCase();
}

const highCount = computed(() => filteredTodos.value.filter((item) => item.priority === "HIGH").length);
const overdueCount = computed(() => filteredTodos.value.filter((item) => item.status === "OVERDUE").length);
const totalExposure = computed(() => filteredTodos.value.reduce((sum, item) => sum + (item.amount || 0), 0));

const commandCards = computed(() => [
  {
    key: "unassigned",
    title: "待认领/责任待定",
    count: filteredTodos.value.filter((item) => !item.owner || item.owner === "待认领").length,
    description: "优先补齐责任人，避免闭环断点。",
  },
  {
    key: "today",
    title: "7日内到期",
    count: filteredTodos.value.filter((item) => daysUntil(item.dueDate) >= 0 && daysUntil(item.dueDate) <= 7).length,
    description: "提前处理审批、回款、交付和续约节点。",
  },
  {
    key: "overdue",
    title: "超时阻塞",
    count: overdueCount.value,
    description: "需要业务负责人或财务/采购协同推进。",
  },
  {
    key: "amount",
    title: "大额事项",
    count: filteredTodos.value.filter((item) => (item.amount || 0) >= 100000).length,
    description: "优先处理影响现金流和项目利润的事项。",
  },
]);

async function loadData() {
  loading.value = true;
  loadErrors.value = [];
  const next: BusinessTodo[] = [];

  await Promise.all([
    collect("统一风险", async () => {
      const items = await listRiskItems();
      items
        .filter((item) => !["CLOSED", "IGNORED"].includes(item.workflow?.status || ""))
        .forEach((item) => next.push({
          key: `risk-${item.key}`,
          module: item.module || "RISK",
          moduleName: item.moduleName || "风险",
          title: item.title,
          subject: item.subject || item.description,
          priority: normalizePriority(item.severity),
          status: item.status === "OVERDUE" || item.slaOverdue ? "OVERDUE" : item.workflow?.status === "PROCESSING" ? "PROCESSING" : "OPEN",
          owner: item.workflow?.owner || "待认领",
          amount: item.amount,
          dueDate: item.dueAt || item.date,
          route: item.route || "/risk-center",
          action: item.workflow?.note || item.description || "进入风险中心处理",
        }));
    }),
    collect("OA待办", async () => {
      const workbench = await getOfficeWorkbench();
      workbench.todos.forEach((item) => next.push({
        key: `office-todo-${item.type}-${item.id}`,
        module: "OFFICE",
        moduleName: "OA协同",
        title: item.title,
        subject: item.subtitle,
        priority: normalizePriority(item.priority),
        status: item.status === "APPROVED" || item.status === "REJECTED" ? "DONE" : isOverdue(item.createdAt, 2) ? "OVERDUE" : "OPEN",
        owner: "当前审批人",
        amount: item.amount,
        dueDate: item.createdAt,
        route: item.route || "/workbench/todos",
        action: item.status === "APPROVED" || item.status === "REJECTED" ? "查看已处理审批" : "处理审批或协同事项",
        entityId: item.id,
      }));
      workbench.warnings.forEach((item) => next.push({
        key: `office-warning-${item.type}-${item.id}`,
        module: "OFFICE",
        moduleName: "OA协同",
        title: item.title,
        subject: item.content,
        priority: normalizePriority(item.severity),
        status: "OPEN",
        owner: "业务负责人",
        dueDate: item.createdAt,
        route: item.route || "/office/notifications",
        action: "查看预警并确认处理方案",
      }));
    }),
    collect("CRM", async () => {
      const [opportunities, quotes, contracts] = await Promise.all([
        listOpportunities(),
        listQuotes(),
        listContracts(),
      ]);
      opportunities
        .filter((item) => !["WON", "LOST"].includes(item.stage))
        .filter((item) => !item.nextActionAt || daysUntil(item.nextActionAt) < 0 || daysSince(item.updatedAt) > 14)
        .forEach((item) => next.push({
          key: `crm-opportunity-${item.id}`,
          module: "CRM",
          moduleName: "CRM",
          title: "商机跟进卡点",
          subject: `${item.customerName} · ${item.needSummary}`,
          priority: item.expectedAmount >= 100000 ? "HIGH" : "MEDIUM",
          status: !item.nextActionAt || daysUntil(item.nextActionAt) < 0 ? "OVERDUE" : "OPEN",
          owner: item.ownerName,
          amount: item.expectedAmount,
          dueDate: item.nextActionAt || item.updatedAt,
          route: `/crm/opportunities/${item.id}`,
          action: "生成跟进任务并补齐下一步动作",
          automation: {
            type: "CREATE_FOLLOW_UP",
            customerId: item.customerId || "",
            customerName: item.customerName,
            opportunityId: item.id,
            opportunityCode: item.code,
            ownerName: item.ownerName,
            subject: "商机超期跟进",
            content: `商机 ${item.code || item.needSummary} 已出现跟进卡点，请确认客户需求、下一步动作和预计成交日期。`,
            nextAction: "确认客户意向并更新商机阶段",
          },
        }));
      quotes
        .filter((item) => item.status === "PENDING_APPROVAL")
        .forEach((item) => next.push({
          key: `crm-quote-${item.id}`,
          module: "CRM",
          moduleName: "CRM",
          title: "报价待审批",
          subject: `${item.customerName} · ${item.serviceScope}`,
          priority: item.amount >= 100000 || daysSince(item.updatedAt) > 3 ? "HIGH" : "MEDIUM",
          status: daysSince(item.updatedAt) > 3 ? "OVERDUE" : "OPEN",
          owner: item.lastApproverName || "审批人",
          amount: item.amount,
          dueDate: item.updatedAt,
          route: `/crm/quotes/${item.id}`,
          action: "校验毛利和折扣权限后审批",
        }));
      contracts
        .filter((item) => item.status === "ACTIVE" && daysUntil(item.startDate) <= 0)
        .forEach((item) => next.push({
          key: `crm-contract-project-${item.id}`,
          module: "PROJECT",
          moduleName: "项目管理",
          title: "合同生效待转项目",
          subject: `${item.customerName} · ${item.projectName}`,
          priority: item.amount >= 100000 ? "HIGH" : "MEDIUM",
          status: daysSince(item.startDate) > 3 ? "OVERDUE" : "OPEN",
          owner: "项目经理",
          amount: item.amount,
          dueDate: item.startDate,
          route: `/crm/contracts/${item.id}`,
          action: "生成项目草稿并带入合同信息",
          automation: {
            type: "CREATE_PROJECT_DRAFT",
            contractId: item.id,
            customerId: item.customerId,
            customerName: item.customerName,
            projectName: item.projectName,
            contractCode: item.code,
            contractAmount: item.amount,
            startDate: item.startDate,
            endDate: item.endDate,
          },
        }));
      contracts
        .filter((item) => item.status !== "CLOSED" && daysUntil(item.endDate) <= 30)
        .forEach((item) => next.push({
          key: `crm-contract-${item.id}`,
          module: "CRM",
          moduleName: "CRM",
          title: daysUntil(item.endDate) < 0 ? "合同已到期" : "合同即将到期",
          subject: `${item.customerName} · ${item.projectName}`,
          priority: daysUntil(item.endDate) < 0 ? "HIGH" : "MEDIUM",
          status: daysUntil(item.endDate) < 0 ? "OVERDUE" : "OPEN",
          owner: "客户经理",
          amount: item.amount,
          dueDate: item.endDate,
          route: `/crm/contracts/${item.id}`,
          action: "确认续约、关闭或转项目计划",
        }));
    }),
    collect("财务资金", async () => {
      const [receivables, payables, payments] = await Promise.all([
        listFinanceReceivables(),
        listFinancePayables(),
        listPaymentApplications(),
      ]);
      receivables
        .filter((item) => item.status !== "SETTLED" && (item.status === "OVERDUE" || daysUntil(item.dueDate) <= 7))
        .forEach((item) => next.push({
          key: `finance-receivable-${item.id}`,
          module: "FINANCE",
          moduleName: "财务资金",
          title: item.status === "OVERDUE" ? "应收逾期" : "应收临期",
          subject: `${item.customerName} · ${item.contractCode || item.sourceNo}`,
          priority: item.status === "OVERDUE" || item.outstandingAmount >= 100000 ? "HIGH" : "MEDIUM",
          status: item.status === "OVERDUE" || daysUntil(item.dueDate) < 0 ? "OVERDUE" : "OPEN",
          owner: "财务应收",
          amount: item.outstandingAmount,
          dueDate: item.dueDate,
          route: "/finance/receivables",
          action: item.status === "OVERDUE" || daysUntil(item.dueDate) < 0 ? "生成催收记录并推动回款" : "登记催收、开票或回款",
          automation: item.status === "OVERDUE" || daysUntil(item.dueDate) < 0 ? {
            type: "CREATE_FOLLOW_UP",
            customerId: item.customerId,
            customerName: item.customerName,
            ownerName: "财务应收",
            subject: "逾期应收催收",
            content: `应收 ${item.code || item.sourceNo} 已逾期，未收金额 ${formatCurrency(item.outstandingAmount)}，请跟进付款计划和预计回款日期。`,
            nextAction: "联系客户确认付款计划",
          } : undefined,
        }));
      payables
        .filter((item) => item.status !== "PAID" && item.status !== "CANCELLED" && (item.overdue || daysUntil(item.dueDate) <= 7))
        .forEach((item) => next.push({
          key: `finance-payable-${item.id}`,
          module: "FINANCE",
          moduleName: "财务资金",
          title: item.overdue ? "应付逾期" : "应付临期",
          subject: `${item.supplierName} · ${item.orderCode}`,
          priority: item.overdue || item.outstandingAmount >= 100000 ? "HIGH" : "MEDIUM",
          status: item.overdue || daysUntil(item.dueDate) < 0 ? "OVERDUE" : "OPEN",
          owner: "财务应付",
          amount: item.outstandingAmount,
          dueDate: item.dueDate,
          route: "/finance/payables",
          action: "确认付款计划或发起付款申请",
        }));
      payments
        .filter((item) => item.status === "PENDING_APPROVAL")
        .forEach((item) => next.push({
          key: `finance-payment-${item.id}`,
          module: "FINANCE",
          moduleName: "财务资金",
          title: "付款申请待审批",
          subject: `${item.supplierName} · ${item.purpose}`,
          priority: item.requestedAmount >= 100000 ? "HIGH" : "MEDIUM",
          status: isOverdue(item.requestedDate, 2) ? "OVERDUE" : "OPEN",
          owner: item.applicantName,
          amount: item.requestedAmount,
          dueDate: item.requestedDate,
          route: "/finance/payment-applications",
          action: "审批后安排付款执行",
        }));
    }),
    collect("供应链采购", async () => {
      const [requests, matching] = await Promise.all([
        listPurchaseRequests({ page: 0, size: 200 }),
        listProcurementMatching(),
      ]);
      requests.content
        .filter((item) => item.status === "SUBMITTED" || item.approvalStatus === "PENDING")
        .forEach((item) => next.push({
          key: `procurement-request-${item.id}`,
          module: "PROCUREMENT",
          moduleName: "供应链采购",
          title: "采购申请待审批",
          subject: `${item.partName || item.materialName} · ${item.costTargetName || item.reason}`,
          priority: (item.totalAmount || 0) >= 100000 ? "HIGH" : "MEDIUM",
          status: isOverdue(item.expectedDate || item.requiredDate, 0) ? "OVERDUE" : "OPEN",
          owner: item.requesterName || item.applicantName || "采购审批人",
          amount: item.totalAmount,
          dueDate: item.expectedDate || item.requiredDate,
          route: "/procurement/requests",
          action: "审批请购并确认预算归集对象",
        }));
      matching
        .filter((item) => item.matchStatus !== "MATCHED")
        .forEach((item) => next.push({
          key: `procurement-match-${item.orderId}`,
          module: "PROCUREMENT",
          moduleName: "供应链采购",
          title: "三单匹配差异",
          subject: `${item.supplierName || "供应商"} · ${item.partName}`,
          priority: item.matchStatus === "AMOUNT_MISMATCH" ? "HIGH" : "MEDIUM",
          status: "OPEN",
          owner: "采购/财务",
          amount: Math.max(item.orderAmount, item.receiptAmount, item.payableAmount, item.paidAmount),
          route: "/procurement/p2p",
          action: item.riskMessage || "核对订单、入库、应付和付款差异",
        }));
    }),
    collect("项目管理", async () => {
      const projects = await listProjectProfitability();
      projects
        .filter((item) => item.riskLevel !== "LOW")
        .forEach((item) => next.push({
          key: `project-profit-${item.projectId}`,
          module: "PROJECT",
          moduleName: "项目管理",
          title: "项目预算/利润预警",
          subject: `${item.projectName} · ${item.riskMessage}`,
          priority: item.riskLevel === "HIGH" ? "HIGH" : "MEDIUM",
          status: item.riskLevel === "HIGH" ? "OVERDUE" : "OPEN",
          owner: "项目经理",
          amount: Math.max(item.actualCost, item.budgetAmount),
          route: "/projects/budget",
          action: "生成预算调整申请并锁定成本风险",
          automation: {
            type: "CREATE_BUDGET_ADJUST_APPROVAL",
            projectCode: item.projectCode,
            projectName: item.projectName,
            amount: Math.max(0, item.actualCost - item.budgetAmount),
            reason: item.riskMessage,
          },
        }));
    }),
  ]);

  todos.value = dedupe(next).sort(sortTodos);
  loading.value = false;
  if (!todos.value.length && !loadErrors.value.length) message.success("当前没有待处理事项");
}

async function collect(label: string, loader: () => Promise<void>) {
  try {
    await loader();
  } catch (error) {
    console.warn(`[business-todos] ${label} load failed`, error);
    loadErrors.value.push(label);
  }
}

function dedupe(items: BusinessTodo[]) {
  const map = new Map<string, BusinessTodo>();
  items.forEach((item) => map.set(item.key, item));
  return Array.from(map.values());
}

function sortTodos(a: BusinessTodo, b: BusinessTodo) {
  const statusRank = { OVERDUE: 0, OPEN: 1, PROCESSING: 2, DONE: 3 };
  const priorityRank = { HIGH: 0, MEDIUM: 1, LOW: 2 };
  return statusRank[a.status] - statusRank[b.status]
    || priorityRank[a.priority] - priorityRank[b.priority]
    || daysUntil(a.dueDate) - daysUntil(b.dueDate);
}

function normalizePriority(value?: string): Priority {
  if (value === "HIGH" || value === "CRITICAL") return "HIGH";
  if (value === "LOW") return "LOW";
  return "MEDIUM";
}

function daysUntil(value?: string) {
  if (!value) return Number.POSITIVE_INFINITY;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return Number.POSITIVE_INFINITY;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  date.setHours(0, 0, 0, 0);
  return Math.ceil((date.getTime() - today.getTime()) / 86400000);
}

function daysSince(value?: string) {
  const days = daysUntil(value);
  return Number.isFinite(days) ? Math.max(0, -days) : 0;
}

function isOverdue(value?: string, graceDays = 0) {
  return Number.isFinite(daysUntil(value)) && daysUntil(value) < -graceDays;
}

function resetFilters() {
  keyword.value = "";
  moduleFilter.value = "ALL";
  priorityFilter.value = "ALL";
  statusFilter.value = "ALL";
}

function go(path: string) {
  router.push(path);
}

function todoRowProps(record: BusinessTodo) {
  return {
    onClick: () => openTodoDetail(record),
  };
}

function openTodoDetail(record: BusinessTodo) {
  selectedTodo.value = record;
  detailOpen.value = true;
}

function isOfficeApprovalTodo(record: BusinessTodo) {
  return normalizeModuleCode(record.module) === "OFFICE" && (record.route.includes("/office/approvals") || record.route.includes("tab=approvals"));
}

function handleTodoAction(record: BusinessTodo) {
  if (isOfficeApprovalTodo(record)) {
    if (record.entityId) {
      detailOpen.value = false;
      approvalCenterRef.value?.openApprovalById(record.entityId);
      return;
    }
    message.warning("缺少审批ID，无法打开详情");
    return;
  }
  detailOpen.value = false;
  router.push(record.route);
}

async function runAutomation(record: BusinessTodo) {
  if (!record.automation) return;
  actionLoadingKey.value = record.key;
  try {
    const currentUser = auth.user?.displayName || record.owner || "系统";
    if (record.automation.type === "CREATE_FOLLOW_UP") {
      if (!record.automation.customerId) {
        message.warning("缺少客户ID，已跳转到源业务页面手动处理");
        router.push(record.route);
        return;
      }
      await createFollowUp({
        customerId: record.automation.customerId,
        opportunityId: record.automation.opportunityId,
        type: "CALLBACK",
        subject: record.automation.subject,
        content: record.automation.content,
        followedAt: new Date().toISOString(),
        nextAction: record.automation.nextAction,
        ownerName: record.automation.ownerName || currentUser,
      });
      message.success("已生成跟进/催收记录");
    } else if (record.automation.type === "CREATE_PROJECT_DRAFT") {
      const amount = record.automation.contractAmount;
      await createProject({
        customerId: record.automation.customerId,
        code: `PRJ-${Date.now().toString().slice(-8)}`,
        name: record.automation.projectName,
        projectType: "NEW_CONSTRUCTION",
        managerName: currentUser,
        siteAddress: `${record.automation.customerName}项目现场`,
        contractAmount: amount,
        plannedStartDate: record.automation.startDate,
        plannedEndDate: record.automation.endDate,
        contractId: record.automation.contractId,
        budgetItems: [
          { category: "LABOR", plannedAmount: amount * 0.2, remark: "自动承接合同-人工预算" },
          { category: "MATERIAL", plannedAmount: amount * 0.45, remark: "自动承接合同-材料预算" },
          { category: "SUBCONTRACT", plannedAmount: amount * 0.2, remark: "自动承接合同-外包预算" },
          { category: "TRAVEL", plannedAmount: amount * 0.05, remark: "自动承接合同-差旅预算" },
          { category: "OTHER", plannedAmount: amount * 0.1, remark: "自动承接合同-其他预算" },
        ],
      });
      message.success("已生成项目草稿");
    } else if (record.automation.type === "CREATE_BUDGET_ADJUST_APPROVAL") {
      await createApproval({
        code: `BUD-${Date.now().toString().slice(-8)}`,
        approvalType: "OTHER",
        title: `预算调整申请：${record.automation.projectName}`,
        sourceNo: record.automation.projectCode,
        amount: record.automation.amount || record.amount || 0,
        applicantName: currentUser,
        businessType: "项目预算调整",
        projectCode: record.automation.projectCode,
        content: `项目 ${record.automation.projectName} 出现预算风险：${record.automation.reason}。请审批预算调整或成本控制方案。`,
      });
      message.success("已生成预算调整审批");
    }
    await loadData();
  } catch (error: any) {
    message.error(error?.message || "自动化动作执行失败");
  } finally {
    actionLoadingKey.value = "";
  }
}

function todayText() {
  return new Date().toISOString().slice(0, 10);
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", maximumFractionDigits: 0 }).format(value);
}

function formatDate(value: string) {
  return value.slice(0, 10);
}

function priorityColor(value: Priority) {
  return value === "HIGH" ? "red" : value === "MEDIUM" ? "orange" : "green";
}

function priorityLabel(value: Priority) {
  return { HIGH: "高", MEDIUM: "中", LOW: "低" }[value];
}

function statusColor(value: TodoStatus) {
  return value === "OVERDUE" ? "red" : value === "PROCESSING" ? "blue" : value === "DONE" ? "green" : "gold";
}

function statusLabel(value: TodoStatus) {
  return { OVERDUE: "已超时", PROCESSING: "处理中", OPEN: "待处理", DONE: "已处理" }[value];
}

onMounted(loadData);
</script>

<style scoped>
.business-todo-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.todo-hero,
.todo-table-card,
.command-card {
  border-radius: 8px;
}

.hero-main {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.metric-grid {
  margin-top: 20px;
}

.metric-card {
  min-height: 96px;
  padding: 16px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-card span,
.command-title {
  color: #5f6b7a;
  font-size: 13px;
}

.metric-card strong,
.command-count {
  color: #172033;
  font-size: 28px;
  line-height: 1.1;
}

.metric-card.high {
  background: #fff7f7;
  border-color: #ffd6d6;
}

.metric-card.overdue {
  background: #fff8ed;
  border-color: #ffd8a8;
}

.metric-card.amount {
  background: #f2fbf8;
  border-color: #bcebdc;
}

.command-card {
  min-height: 130px;
}

.command-count {
  margin: 10px 0 8px;
  font-weight: 700;
}

.load-alert {
  margin-bottom: 16px;
}

.todo-title {
  font-weight: 600;
  color: #172033;
}

.todo-subject {
  margin-top: 4px;
  color: #6b778c;
  font-size: 12px;
}

.todo-table-card :deep(.ant-table-tbody > tr) {
  cursor: pointer;
}

@media (max-width: 768px) {
  .hero-main {
    flex-direction: column;
  }
}
</style>
