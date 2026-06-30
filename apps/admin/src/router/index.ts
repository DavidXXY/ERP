import { createRouter, createWebHistory, type RouteRecordRaw } from "vue-router";
import AppLayout from "@/layouts/AppLayout.vue";
import DashboardView from "@/views/dashboard/DashboardView.vue";
import CustomerPoolView from "@/views/crm/CustomerPoolView.vue";
import InventoryPartsView from "@/views/inventory/InventoryPartsView.vue";
import ProcurementView from "@/views/procurement/ProcurementView.vue";
import ProjectManagementView from "@/views/project/ProjectManagementView.vue";
import HumanResourcesView from "@/views/hr/HumanResourcesView.vue";
import LoginView from "@/views/system/LoginView.vue";
import PlaceholderView from "@/views/system/PlaceholderView.vue";
import RoleManagementView from "@/views/system/RoleManagementView.vue";
import PermissionManagementView from "@/views/system/PermissionManagementView.vue";
import OrganizationView from "@/views/system/OrganizationView.vue";
import { useAuthStore } from "@/stores/auth";

export const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "login",
    component: LoginView,
    meta: { title: "登录", public: true },
  },
  {
    path: "/",
    component: AppLayout,
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        name: "dashboard",
        component: DashboardView,
        meta: { title: "经营驾驶舱", permission: "dashboard:view" },
      },
      {
        path: "profile",
        name: "personal-settings",
        component: () => import("@/views/personal/PersonalSettingsView.vue"),
        meta: { title: "个人设置" },
      },
      {
        path: "crm/customers",
        name: "crm-customers",
        component: CustomerPoolView,
        meta: { title: "客户池", permission: "crm:customer:view" },
      },
      {
        path: "crm/opportunities",
        name: "crm-opportunities",
        component: () => import("@/views/crm/OpportunityView.vue"),
        meta: { title: "线索商机", permission: "crm:opportunity:view" },
      },
      {
        path: "crm/quotes",
        name: "crm-quotes",
        component: () => import("@/views/crm/QuotePlanView.vue"),
        meta: { title: "报价方案", permission: "crm:quote:view" },
      },
      {
        path: "crm/contracts",
        name: "crm-contracts",
        component: () => import("@/views/crm/ContractView.vue"),
        meta: { title: "客户合同", permission: "crm:contract:view" },
      },
      {
        path: "crm/follow-ups",
        name: "crm-follow-ups",
        component: () => import("@/views/crm/FollowUpView.vue"),
        meta: { title: "跟进回访", permission: "crm:followup:view" },
      },
      {
        path: "crm/renewals",
        name: "crm-renewals",
        component: () => import("@/views/crm/RenewalView.vue"),
        meta: { title: "续约管理", permission: "crm:renewal:view" },
      },
      {
        path: "crm/receivables",
        name: "crm-receivables",
        component: () => import("@/views/crm/ReceivableView.vue"),
        meta: { title: "合同应收", permission: "crm:receivable:view" },
      },
      {
        path: "crm/profiles",
        name: "crm-profiles",
        component: () => import("@/views/crm/CustomerProfileView.vue"),
        meta: { title: "客户经营画像", permission: "crm:profile:view" },
      },
      {
        path: "procurement",
        name: "procurement",
        component: ProcurementView,
        meta: { title: "供应链采购", permission: "procurement:view", description: "采购申请、采购订单、供应商账期、入库应付联动。" },
      },
      {
        path: "projects",
        name: "projects",
        component: ProjectManagementView,
        meta: { title: "项目管理", permission: "project:view", description: "立项、预算、进度、成本归集、验收、质保。" },
      },
      {
        path: "inventory",
        name: "inventory",
        component: InventoryPartsView,
        meta: { title: "库存管理", permission: "inventory:view", description: "物料档案、入库、领用、归还、盘点和安全库存。" },
      },
      {
        path: "maintenance",
        redirect: "/maintenance/work-orders",
      },
      {
        path: "maintenance/work-orders",
        name: "maintenance-work-orders",
        component: () => import("@/views/maintenance/MaintenanceCenterView.vue"),
        meta: { title: "服务工单", permission: "maintenance:workorder:view" },
      },
      {
        path: "maintenance/equipment",
        name: "maintenance-equipment",
        component: () => import("@/views/maintenance/MaintenanceCenterView.vue"),
        meta: { title: "资产设备", permission: "maintenance:equipment:view" },
      },
      {
        path: "maintenance/plans",
        name: "maintenance-plans",
        component: () => import("@/views/maintenance/MaintenanceCenterView.vue"),
        meta: { title: "服务计划", permission: "maintenance:plan:view" },
      },
      {
        path: "hr",
        name: "human-resources",
        component: HumanResourcesView,
        meta: { title: "人事管理", permissions: ["qualification:employee:view", "qualification:certificate:view", "workforce:view"] },
      },
      {
        path: "workforce",
        redirect: { path: "/hr", query: { tab: "workforce" } },
      },
      {
        path: "qualification",
        redirect: "/qualification/dashboard",
      },
      {
        path: "qualification/dashboard",
        name: "qualification-dashboard",
        component: () => import("@/views/qualification/QualificationCenterView.vue"),
        meta: { title: "资质总览", permission: "qualification:view" },
      },
      {
        path: "qualification/companies",
        name: "qualification-companies",
        component: () => import("@/views/qualification/QualificationCenterView.vue"),
        meta: { title: "公司资质", permission: "qualification:company:view" },
      },
      {
        path: "qualification/employees",
        redirect: { path: "/hr", query: { tab: "employees" } },
      },
      {
        path: "qualification/certificates",
        redirect: { path: "/hr", query: { tab: "certificates" } },
      },
      {
        path: "qualification/performances",
        name: "qualification-performances",
        component: () => import("@/views/qualification/QualificationCenterView.vue"),
        meta: { title: "项目业绩", permission: "qualification:performance:view" },
      },
      {
        path: "qualification/tender",
        name: "qualification-tender",
        component: () => import("@/views/qualification/QualificationCenterView.vue"),
        meta: { title: "投标查询", permission: "qualification:tender:view" },
      },
      {
        path: "qualification/warnings",
        name: "qualification-warnings",
        component: () => import("@/views/qualification/QualificationCenterView.vue"),
        meta: { title: "资质预警", permission: "qualification:warning:view" },
      },
      {
        path: "office",
        redirect: "/office/approvals",
      },
      {
        path: "office/approvals",
        name: "office-approvals",
        component: () => import("@/views/office/OfficeCenterView.vue"),
        meta: { title: "审批中心", permission: "office:approval:view" },
      },
      {
        path: "office/expenses",
        name: "office-expenses",
        component: () => import("@/views/office/OfficeCenterView.vue"),
        meta: { title: "费用报销", permission: "office:expense:view" },
      },
      {
        path: "office/outsourcing",
        name: "office-outsourcing",
        component: () => import("@/views/office/OfficeCenterView.vue"),
        meta: { title: "外包服务", permission: "office:outsource:view" },
      },
      {
        path: "office/documents",
        name: "office-documents",
        component: () => import("@/views/office/OfficeCenterView.vue"),
        meta: { title: "电子档案", permission: "office:document:view" },
      },
      {
        path: "office/notifications",
        name: "office-notifications",
        component: () => import("@/views/office/OfficeCenterView.vue"),
        meta: { title: "消息中心", permission: "office:notification:view" },
      },
      {
        path: "office/audits",
        name: "office-audits",
        component: () => import("@/views/office/OfficeCenterView.vue"),
        meta: { title: "操作审计", permission: "office:audit:view" },
      },
      {
        path: "finance",
        redirect: "/finance/overview",
      },
      {
        path: "finance/overview",
        name: "finance-overview",
        component: () => import("@/views/finance/FinanceOverviewView.vue"),
        meta: { title: "资金概览", permission: "finance:view" },
      },
      {
        path: "finance/receivables",
        name: "finance-receivables",
        component: () => import("@/views/finance/FinanceReceivableView.vue"),
        meta: { title: "应收管理", permission: "finance:receivable:view" },
      },
      {
        path: "finance/payables",
        name: "finance-payables",
        component: () => import("@/views/finance/FinancePayableView.vue"),
        meta: { title: "应付管理", permission: "finance:payable:view" },
      },
      {
        path: "finance/payment-applications",
        name: "finance-payment-applications",
        component: () => import("@/views/finance/PaymentApplicationView.vue"),
        meta: { title: "付款申请", permissions: ["finance:payable:view", "finance:payment:approve", "finance:payment:execute"] },
      },
      {
        path: "finance/ledger",
        name: "finance-ledger",
        component: () => import("@/views/finance/LedgerView.vue"),
        meta: { title: "总账报表", permission: "finance:ledger:view" },
      },
      {
        path: "system",
        name: "system",
        component: PlaceholderView,
        meta: { title: "系统设置", permission: "system:view", description: "组织、用户、角色、数据范围、操作日志。" },
      },
      {
        path: "system/users",
        redirect: { path: "/hr", query: { tab: "employees" } },
      },
      {
        path: "system/organizations",
        name: "system-organizations",
        component: OrganizationView,
        meta: { title: "组织架构", permission: "system:organization:view" },
      },
      {
        path: "system/roles",
        name: "system-roles",
        component: RoleManagementView,
        meta: { title: "角色管理", permission: "system:role:view" },
      },
      {
        path: "system/permissions",
        name: "system-permissions",
        component: PermissionManagementView,
        meta: { title: "权限管理", permission: "system:permission:view" },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (to.meta.public) {
    return true;
  }
  if (!auth.isLoggedIn) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (!auth.initialized) {
    try {
      await auth.loadCurrentUser();
    } catch {
      auth.logout();
      return { path: "/login", query: { redirect: to.fullPath } };
    }
  }
  const permission = to.meta.permission;
  if (typeof permission === "string" && !auth.can(permission)) {
    return "/dashboard";
  }
  const permissions = to.meta.permissions;
  if (Array.isArray(permissions) && !permissions.some((item) => typeof item === "string" && auth.can(item))) {
    return "/dashboard";
  }
  return true;
});

export default router;
