import { createRouter, createWebHistory, type RouteRecordRaw } from "vue-router";
import AppLayout from "@/layouts/AppLayout.vue";
import DashboardView from "@/views/dashboard/DashboardView.vue";
import CustomerPoolView from "@/views/crm/CustomerPoolView.vue";
import InventoryPartsView from "@/views/inventory/InventoryPartsView.vue";
import ProcurementView from "@/views/procurement/ProcurementView.vue";
import ProjectManagementView from "@/views/project/ProjectManagementView.vue";
import LoginView from "@/views/system/LoginView.vue";
import PlaceholderView from "@/views/system/PlaceholderView.vue";
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
        path: "crm/customers",
        name: "crm-customers",
        component: CustomerPoolView,
        meta: { title: "客户池", permission: "crm:customer:view" },
      },
      {
        path: "crm/opportunities",
        name: "crm-opportunities",
        component: PlaceholderView,
        meta: { title: "线索商机", permission: "crm:opportunity:view", description: "来源、需求、阶段、预计金额、成功率、下次动作。" },
      },
      {
        path: "crm/contracts",
        name: "crm-contracts",
        component: PlaceholderView,
        meta: { title: "维保合同", permission: "crm:contract:view", description: "合同期限、服务范围、巡检频次、付款节点、续约提醒。" },
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
        meta: { title: "备件仓储", permission: "inventory:view", description: "备件台账、入库、领用、归还、盘点、安全库存。" },
      },
      {
        path: "finance",
        name: "finance",
        component: PlaceholderView,
        meta: { title: "财务资金", permission: "finance:view", description: "应收、开票、回款核销、应付、费用、总账凭证。" },
      },
      {
        path: "system",
        name: "system",
        component: PlaceholderView,
        meta: { title: "系统设置", permission: "system:view", description: "组织、用户、角色、数据范围、操作日志。" },
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
  return true;
});

export default router;
