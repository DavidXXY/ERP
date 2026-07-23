import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import AppLayout from "@/layouts/AppLayout.vue";
import LoginView from "@/views/system/LoginView.vue";
import { useAuthStore } from "@/stores/auth";

const rootRoutes: RouteRecordRaw[] = [
  {
    path: "dashboard",
    name: "dashboard",
    component: () => import("@/views/crm/CrmDashboardView.vue"),
    meta: {
      title: "\u7ecf\u8425\u9a7e\u9a76\u8231",
      permission: "dashboard:view",
    },
  },
  {
    path: "risk-center",
    name: "risk-center",
    component: () => import("@/views/RiskCenterView.vue"),
    meta: {
      title: "统一风险中心",
      permissions: [
        "risk:view",
        "dashboard:view",
        "office:approval:view",
        "office:notification:view",
        "inventory:view",
        "procurement:view",
        "project:view",
        "finance:receivable:view",
        "finance:payable:view",
        "qualification:warning:view",
        "crm:renewal:view",
      ],
    },
  },
  {
    path: "workbench/todos",
    name: "business-todos",
    component: () => import("@/views/BusinessTodoCenterView.vue"),
    meta: {
      title: "业务待办中心",
      permissions: [
        "dashboard:view",
        "risk:view",
        "office:approval:view",
        "office:notification:view",
        "crm:opportunity:view",
        "crm:quote:view",
        "crm:contract:view",
        "crm:receivable:view",
        "procurement:view",
        "inventory:view",
        "project:view",
        "finance:receivable:view",
        "finance:payable:view",
      ],
    },
  },
  {
    path: "collaboration",
    name: "collaboration-center",
    component: () => import("@/views/CollaborationCenterView.vue"),
    meta: {
      title: "跨部门协同中心",
      permissions: [
        "dashboard:view",
        "project:view",
        "office:approval:view",
        "procurement:view",
        "finance:view",
      ],
    },
  },
  {
    path: "profile",
    name: "personal-settings",
    component: () => import("@/views/personal/PersonalSettingsView.vue"),
    meta: { title: "\u4e2a\u4eba\u8bbe\u7f6e" },
  },
  // CRM
  {
    path: "crm/dashboard",
    name: "crm-dashboard",
    component: () => import("@/views/crm/CrmDashboardView.vue"),
    meta: { title: "CRM仪表盘", permission: "crm:dashboard:view" },
  },
  {
    path: "crm/customers",
    name: "crm-customers",
    component: () => import("@/views/crm/CustomerPoolView.vue"),
    meta: { title: "\u5ba2\u6237\u6c60", permission: "crm:customer:view" },
  },
  {
    path: "crm/customers/:id",
    name: "crm-customer-detail",
    component: () => import("@/views/crm/CustomerDetailView.vue"),
    meta: { title: "客户全景详情", permission: "crm:customer:view" },
  },
  {
    path: "crm/opportunities",
    name: "crm-opportunities",
    component: () => import("@/views/crm/OpportunityView.vue"),
    meta: {
      title: "\u7ebf\u7d22\u5546\u673a",
      permission: "crm:opportunity:view",
    },
  },
  {
    path: "crm/opportunities/:id",
    name: "crm-opportunity-detail",
    component: () => import("@/views/crm/OpportunityDetailView.vue"),
    meta: {
      title: "\u5546\u673a\u8be6\u60c5",
      permission: "crm:opportunity:view",
    },
  },
  {
    path: "crm/quotes",
    name: "crm-quotes",
    component: () => import("@/views/crm/QuotePlanView.vue"),
    meta: { title: "\u62a5\u4ef7\u65b9\u6848", permission: "crm:quote:view" },
  },
  {
    path: "crm/quotes/:id",
    name: "crm-quote-detail",
    component: () => import("@/views/crm/QuotePlanDetailView.vue"),
    meta: { title: "\u62a5\u4ef7\u8be6\u60c5", permission: "crm:quote:view" },
  },
  {
    path: "crm/contracts",
    name: "crm-contracts",
    component: () => import("@/views/crm/ContractView.vue"),
    meta: {
      title: "\u5ba2\u6237\u5408\u540c",
      permission: "crm:contract:view",
    },
  },
  {
    path: "crm/contracts/:id",
    name: "crm-contract-detail",
    component: () => import("@/views/crm/ContractDetailView.vue"),
    meta: {
      title: "\u5408\u540c\u8be6\u60c5",
      permissions: ["crm:contract:view", "finance:receivable:view"],
    },
  },
  {
    path: "crm/follow-ups",
    name: "crm-follow-ups",
    component: () => import("@/views/crm/FollowUpView.vue"),
    meta: {
      title: "\u8ddf\u8fdb\u56de\u8bbf",
      permission: "crm:followup:view",
    },
  },
  {
    path: "crm/renewals",
    name: "crm-renewals",
    component: () => import("@/views/crm/RenewalView.vue"),
    meta: { title: "\u7eed\u7ea6\u7ba1\u7406", permission: "crm:renewal:view" },
  },
  {
    path: "crm/receivables",
    name: "crm-receivables",
    component: () => import("@/views/crm/ReceivableView.vue"),
    meta: {
      title: "\u5408\u540c\u5e94\u6536",
      permission: "crm:receivable:view",
    },
  },
  { path: "crm/profiles", redirect: "/crm/customers" },
  // Procurement
  { path: "procurement", redirect: "/procurement/requests" },
  {
    path: "procurement/requests",
    name: "procurement-requests",
    component: () => import("@/views/procurement/PurchaseRequestsView.vue"),
    meta: { title: "\u91c7\u8d2d\u7533\u8bf7", permission: "procurement:view" },
  },
  {
    path: "procurement/orders",
    name: "procurement-orders",
    component: () => import("@/views/procurement/PurchaseOrdersView.vue"),
    meta: { title: "\u91c7\u8d2d\u8ba2\u5355", permission: "procurement:view" },
  },
  {
    path: "procurement/orders/:id",
    name: "procurement-order-detail",
    component: () => import("@/views/procurement/PurchaseOrderDetailView.vue"),
    meta: { title: "采购订单详情", permission: "procurement:view" },
  },
  {
    path: "procurement/receipts",
    name: "procurement-receipts",
    component: () => import("@/views/procurement/GoodsReceiptsView.vue"),
    meta: { title: "\u5230\u8d27\u5165\u5e93", permission: "procurement:view" },
  },
  {
    path: "procurement/costs",
    name: "procurement-costs",
    component: () => import("@/views/procurement/CostAllocationView.vue"),
    meta: { title: "\u6210\u672c\u5f52\u96c6", permission: "procurement:view" },
  },
  {
    path: "procurement/payables",
    name: "procurement-payables",
    component: () => import("@/views/procurement/ProcurementPayableView.vue"),
    meta: { title: "\u91c7\u8d2d\u5e94\u4ed8", permission: "procurement:view" },
  },
  {
    path: "procurement/analytics",
    name: "procurement-analytics",
    component: () => import("@/views/procurement/ProcurementAnalyticsView.vue"),
    meta: { title: "采购分析", permission: "procurement:view" },
  },
  {
    path: "procurement/p2p",
    name: "procurement-p2p",
    component: () => import("@/views/procurement/P2PTrackingView.vue"),
    meta: { title: "P2P全流程", permission: "procurement:view" },
  },
  {
    path: "procurement/controls",
    name: "procurement-controls",
    component: () => import("@/views/procurement/ProcurementControlView.vue"),
    meta: { title: "采购控制中心", permission: "procurement:view" },
  },
  {
    path: "procurement/suppliers",
    name: "procurement-suppliers",
    component: () => import("@/views/procurement/SupplierManagementView.vue"),
    meta: { title: "供应商", permission: "procurement:view" },
  },
  {
    path: "procurement/suppliers/:id",
    name: "procurement-supplier-detail",
    component: () => import("@/views/procurement/SupplierDetailView.vue"),
    meta: { title: "供应商全景详情", permission: "procurement:view" },
  },
  // Projects
  { path: "projects", redirect: "/projects/list" },
  {
    path: "projects/list",
    name: "projects-list",
    component: () => import("@/views/project/ProjectManagementView.vue"),
    meta: { title: "\u9879\u76ee\u5217\u8868", permission: "project:view" },
  },
  {
    path: "projects/:id",
    name: "project-detail",
    component: () => import("@/views/project/ProjectDetailView.vue"),
    meta: { title: "项目经营详情", permission: "project:view" },
  },
  {
    path: "projects/budget",
    name: "projects-budget",
    component: () => import("@/views/project/ProjectManagementView.vue"),
    meta: { title: "\u9884\u7b97\u6267\u884c", permission: "project:view" },
  },
  {
    path: "projects/costs",
    name: "projects-costs",
    component: () => import("@/views/project/ProjectManagementView.vue"),
    meta: { title: "\u6210\u672c\u660e\u7ec6", permission: "project:view" },
  },
  {
    path: "projects/stages",
    name: "projects-stages",
    component: () => import("@/views/project/ProjectManagementView.vue"),
    meta: { title: "\u9636\u6bb5\u5386\u7a0b", permission: "project:view" },
  },
  {
    path: "projects/presales-support",
    name: "projects-presales-support",
    component: () => import("@/views/project/ProjectManagementView.vue"),
    meta: { title: "售前支持", permission: "project:view" },
  },
  // Inventory
  { path: "inventory", redirect: "/inventory/parts" },
  {
    path: "inventory/parts",
    name: "inventory-parts",
    component: () => import("@/views/inventory/PartsLedgerView.vue"),
    meta: { title: "\u5e93\u5b58\u53f0\u8d26", permission: "inventory:view" },
  },
  {
    path: "inventory/parts/:id",
    name: "inventory-part-detail",
    component: () => import("@/views/inventory/InventoryPartDetailView.vue"),
    meta: { title: "物料库存详情", permission: "inventory:view" },
  },
  {
    path: "inventory/issues",
    name: "inventory-issues",
    component: () => import("@/views/inventory/MaterialIssuesView.vue"),
    meta: { title: "\u9886\u6599\u7ba1\u7406", permission: "inventory:view" },
  },
  {
    path: "inventory/analytics",
    name: "inventory-analytics",
    component: () => import("@/views/inventory/InventoryAnalyticsView.vue"),
    meta: { title: "库存分析", permission: "inventory:view" },
  },
  {
    path: "inventory/movements",
    name: "inventory-movements",
    component: () => import("@/views/inventory/StockMovementsView.vue"),
    meta: { title: "库存移动", permission: "inventory:view" },
  },
  // HR
  {
    path: "hr",
    name: "human-resources",
    component: () => import("@/views/hr/HumanResourcesView.vue"),
    meta: {
      title: "\u4eba\u4e8b\u7ba1\u7406",
      permissions: [
        "qualification:employee:view",
        "qualification:certificate:view",
        "workforce:view",
      ],
    },
  },
  { path: "workforce", redirect: "/hr?tab=workforce" },
  {
    path: "hr/employees/:employeeId",
    name: "hr-employee-detail",
    component: () => import("@/views/hr/EmployeeDetailView.vue"),
    meta: {
      title: "\u5458\u5de5\u6863\u6848\u8be6\u60c5",
      permission: "qualification:employee:view",
    },
  },
  {
    path: "hr/lifecycle",
    name: "hr-lifecycle",
    component: () => import("@/views/hr/EmployeeLifecycleView.vue"),
    meta: {
      title: "\u5165\u8f6c\u8c03\u79bb",
      permissions: ["qualification:employee:view"],
    },
  },
  {
    path: "hr/leaves",
    name: "hr-leaves",
    component: () => import("@/views/hr/LeaveManagementView.vue"),
    meta: {
      title: "\u8bf7\u5047\u7ba1\u7406",
      permissions: ["workforce:view", "qualification:employee:view"],
    },
  },
  {
    path: "hr/analytics",
    name: "hr-analytics",
    component: () => import("@/views/hr/HrAnalyticsView.vue"),
    meta: {
      title: "\u4eba\u529b\u5206\u6790",
      permissions: ["qualification:employee:view"],
    },
  },
  {
    path: "hr/leave-balances",
    name: "hr-leave-balances",
    component: () => import("@/views/hr/LeaveBalanceView.vue"),
    meta: {
      title: "\u8bf7\u5047\u989d\u5ea6",
      permissions: ["qualification:employee:manage"],
    },
  },
  // Qualification
  { path: "qualification", redirect: "/qualification/dashboard" },
  {
    path: "qualification/dashboard",
    name: "qualification-dashboard",
    component: () =>
      import("@/views/qualification/QualificationCenterView.vue"),
    meta: {
      title: "\u8d44\u8d28\u603b\u89c8",
      permission: "qualification:view",
    },
  },
  {
    path: "qualification/companies",
    name: "qualification-companies",
    component: () =>
      import("@/views/qualification/QualificationCenterView.vue"),
    meta: {
      title: "\u516c\u53f8\u8d44\u8d28",
      permission: "qualification:company:view",
    },
  },
  { path: "qualification/employees", redirect: "/hr?tab=employees" },
  { path: "qualification/certificates", redirect: "/hr?tab=certificates" },
  {
    path: "qualification/tender",
    name: "qualification-tender",
    component: () =>
      import("@/views/qualification/QualificationCenterView.vue"),
    meta: {
      title: "\u6295\u6807\u67e5\u8be2",
      permission: "qualification:tender:view",
    },
  },
  {
    path: "qualification/warnings",
    name: "qualification-warnings",
    component: () =>
      import("@/views/qualification/QualificationCenterView.vue"),
    meta: {
      title: "\u8d44\u8d28\u9884\u8b66",
      permission: "qualification:warning:view",
    },
  },
  // Office
  { path: "office", redirect: "/workbench/todos" },
  { path: "office/approvals", redirect: "/workbench/todos?tab=approvals" },
  {
    path: "office/approvals/:id",
    name: "office-approval-detail",
    component: () => import("@/views/office/ApprovalDetailView.vue"),
    meta: { title: "审批详情", permission: "office:approval:view" },
  },
  {
    path: "office/expenses",
    name: "office-expenses",
    component: () => import("@/views/office/ExpenseView.vue"),
    meta: {
      title: "\u8d39\u7528\u62a5\u9500",
      permission: "office:expense:view",
    },
  },
  {
    path: "office/outsourcing",
    name: "office-outsourcing",
    component: () => import("@/views/office/OutsourceView.vue"),
    meta: {
      title: "\u5916\u5305\u670d\u52a1",
      permission: "office:outsource:view",
    },
  },
  {
    path: "office/documents",
    name: "office-documents",
    component: () => import("@/views/office/DocumentArchiveView.vue"),
    meta: {
      title: "\u7535\u5b50\u6863\u6848",
      permission: "office:document:view",
    },
  },
  {
    path: "office/notifications",
    name: "office-notifications",
    component: () => import("@/views/office/NotificationCenterView.vue"),
    meta: {
      title: "\u6d88\u606f\u4e2d\u5fc3",
      permission: "office:notification:view",
    },
  },
  {
    path: "office/audits",
    name: "office-audits",
    component: () => import("@/views/office/AuditView.vue"),
    meta: {
      title: "\u64cd\u4f5c\u5ba1\u8ba1",
      permission: "office:audit:view",
    },
  },
  // Finance
  { path: "finance", redirect: "/finance/overview" },
  {
    path: "finance/overview",
    name: "finance-overview",
    component: () => import("@/views/finance/FinanceOverviewView.vue"),
    meta: { title: "\u8d44\u91d1\u6982\u89c8", permission: "finance:view" },
  },
  {
    path: "finance/receivables",
    name: "finance-receivables",
    component: () => import("@/views/finance/FinanceReceivableView.vue"),
    meta: {
      title: "\u5e94\u6536\u7ba1\u7406",
      permission: "finance:receivable:view",
    },
  },
  {
    path: "finance/receivables/:id",
    name: "finance-receivable-detail",
    component: () => import("@/views/finance/FinanceReceivableDetailView.vue"),
    meta: { title: "应收详情", permission: "finance:receivable:view" },
  },
  {
    path: "finance/payables",
    name: "finance-payables",
    component: () => import("@/views/finance/FinancePayableView.vue"),
    meta: {
      title: "\u5e94\u4ed8\u7ba1\u7406",
      permission: "finance:payable:view",
    },
  },
  {
    path: "finance/payables/:id",
    name: "finance-payable-detail",
    component: () => import("@/views/finance/FinancePayableDetailView.vue"),
    meta: { title: "应付详情", permission: "finance:payable:view" },
  },
  {
    path: "finance/payment-applications",
    name: "finance-payment-applications",
    component: () => import("@/views/finance/PaymentApplicationView.vue"),
    meta: {
      title: "\u4ed8\u6b3e\u7533\u8bf7",
      permissions: [
        "finance:payable:view",
        "finance:payment:approve",
        "finance:payment:execute",
      ],
    },
  },
  {
    path: "finance/payment-applications/:id",
    name: "finance-payment-application-detail",
    component: () => import("@/views/finance/PaymentApplicationDetailView.vue"),
    meta: {
      title: "付款申请详情",
      permissions: [
        "finance:payable:view",
        "finance:payment:approve",
        "finance:payment:execute",
      ],
    },
  },
  {
    path: "finance/ledger",
    name: "finance-ledger",
    component: () => import("@/views/finance/LedgerView.vue"),
    meta: {
      title: "\u603b\u8d26\u62a5\u8868",
      permission: "finance:ledger:view",
    },
  },
  // System
  { path: "system", redirect: "/system/health" },
  {
    path: "system/health",
    name: "system-health",
    component: () => import("@/views/system/SystemHealthView.vue"),
    meta: {
      title: "\u7cfb\u7edf\u8fd0\u884c\u60c5\u51b5",
      permission: "system:view",
    },
  },
  {
    path: "system/users",
    name: "system-users",
    component: () => import("@/views/system/UserManagementView.vue"),
    meta: { title: "账号管理", permission: "system:user:view" },
  },
  {
    path: "system/organizations",
    name: "system-organizations",
    component: () => import("@/views/system/OrganizationView.vue"),
    meta: {
      title: "\u7ec4\u7ec7\u67b6\u6784",
      permission: "system:organization:view",
    },
  },
  {
    path: "system/roles",
    name: "system-roles",
    component: () => import("@/views/system/RoleManagementView.vue"),
    meta: { title: "\u89d2\u8272\u7ba1\u7406", permission: "system:role:view" },
  },
  {
    path: "system/permissions",
    name: "system-permissions",
    component: () => import("@/views/system/PermissionManagementView.vue"),
    meta: {
      title: "\u6743\u9650\u7ba1\u7406",
      permission: "system:permission:view",
    },
  },
  {
    path: "system/approval-configs",
    name: "system-approval-configs",
    component: () => import("@/views/system/ApprovalConfigView.vue"),
    meta: { title: "审批人员配置", permission: "system:role:view" },
  },
  {
    path: "system/process-rules",
    name: "system-process-rules",
    component: () => import("@/views/system/ProcessRuleConfigView.vue"),
    meta: {
      title: "流程规则配置",
      permissions: ["system:role:view", "risk:update"],
    },
  },
  {
    path: "system/deleted-records",
    name: "system-deleted-records",
    component: () => import("@/views/system/DeletedRecordsView.vue"),
    meta: { title: "删除回收站", permission: "system:deleted-records:manage" },
  },
  // Self-service
  {
    path: "self",
    name: "self-dashboard",
    component: () => import("@/views/self/SelfDashboardView.vue"),
    meta: { title: "\u6211\u7684\u5de5\u4f5c\u53f0" },
  },
  {
    path: "self/profile",
    name: "self-profile",
    component: () => import("@/views/self/SelfProfileView.vue"),
    meta: { title: "\u6211\u7684\u6863\u6848" },
  },
  {
    path: "self/leaves",
    name: "self-leaves",
    component: () => import("@/views/self/SelfLeaveHistoryView.vue"),
    meta: { title: "\u6211\u7684\u8bf7\u5047" },
  },
  {
    path: "self/leaves/new",
    name: "self-leave-new",
    component: () => import("@/views/self/SelfLeaveSubmitView.vue"),
    meta: { title: "\u63d0\u4ea4\u8bf7\u5047" },
  },
  {
    path: "self/balances",
    name: "self-balances",
    component: () => import("@/views/self/SelfBalanceView.vue"),
    meta: { title: "\u6211\u7684\u989d\u5ea6" },
  },
  { path: "self/approvals", redirect: "/workbench/todos?tab=approvals" },
];

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: { title: "\u767b\u5f55", public: true },
    },
    {
      path: "/",
      component: AppLayout,
      redirect: "/crm/customers",
      children: rootRoutes,
    },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (to.meta.public) return true;
  if (!auth.isLoggedIn)
    return { path: "/login", query: { redirect: to.fullPath } };
  if (!auth.initialized) {
    try {
      await auth.loadCurrentUser();
    } catch {
      auth.logout();
      return { path: "/login", query: { redirect: to.fullPath } };
    }
  }
  const permission = to.meta.permission;
  if (typeof permission === "string" && !auth.can(permission))
    return { path: "/login", query: { redirect: to.fullPath } };
  const permissions = to.meta.permissions;
  if (
    Array.isArray(permissions) &&
    !permissions.some((item) => typeof item === "string" && auth.can(item))
  )
    return { path: "/login", query: { redirect: to.fullPath } };
  return true;
});

export default router;
