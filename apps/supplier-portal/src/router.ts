import { createRouter, createWebHistory } from "vue-router";
import { SUPPLIER_TOKEN_KEY } from "./api";
import { usePortalStore } from "./store";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", component: () => import("./views/AuthView.vue") },
    {
      path: "/",
      component: () => import("./views/PortalLayout.vue"),
      children: [
        { path: "", redirect: "/dashboard" },
        { path: "dashboard", component: () => import("./views/DashboardView.vue") },
        { path: "notifications", component: () => import("./views/NotificationsView.vue") },
        { path: "finance", component: () => import("./views/FinanceView.vue") },
        { path: "profile", component: () => import("./views/ProfileView.vue") },
        { path: "documents", component: () => import("./views/DocumentsView.vue") },
        { path: "inquiries", component: () => import("./views/InquiriesView.vue") },
        { path: "orders", component: () => import("./views/OrdersView.vue") },
        { path: "account", component: () => import("./views/AccountView.vue") },
      ],
    },
  ],
});

router.beforeEach(async (to) => {
  const hasToken = Boolean(sessionStorage.getItem(SUPPLIER_TOKEN_KEY));
  if (!hasToken && to.path !== "/login") return "/login";
  if (hasToken && to.path === "/login") return "/dashboard";
  if (!hasToken) return true;
  const store = usePortalStore();
  // 校验令牌有效性（而非仅校验存在性），并强制必须改密的账号先完成改密
  if (!store.session) {
    try {
      await store.restore();
    } catch {
      store.logout();
      return "/login";
    }
  }
  if (store.session?.account.mustChangePassword && to.path !== "/account") {
    return "/account";
  }
  return true;
});

export default router;
