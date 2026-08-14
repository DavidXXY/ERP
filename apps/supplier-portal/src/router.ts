import { createRouter, createWebHistory } from "vue-router";
import { SUPPLIER_TOKEN_KEY } from "./api";

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

router.beforeEach((to) => {
  const hasToken = Boolean(localStorage.getItem(SUPPLIER_TOKEN_KEY));
  if (!hasToken && to.path !== "/login") return "/login";
  if (hasToken && to.path === "/login") return "/dashboard";
});

export default router;
