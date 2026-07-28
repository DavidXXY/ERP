import { Modal } from "ant-design-vue";
import type { RouteLocationRaw, Router } from "vue-router";

const BUDGET_OVERRUN_MARKERS = [
  "项目预算不足",
  "超出项目预算",
  "超过项目预算",
  "Cost exceeds budget",
];

export function getErrorMessage(error: unknown, fallback = "操作失败") {
  if (error instanceof Error && error.message) return error.message;
  if (typeof error === "string" && error) return error;
  return fallback;
}

export function isBudgetOverrunMessage(value: string) {
  return BUDGET_OVERRUN_MARKERS.some((marker) => value.includes(marker));
}

export function budgetChangeRoute(projectId?: string): RouteLocationRaw {
  return {
    path: "/collaboration",
    query: {
      tab: "budget",
      action: "request-budget",
      ...(projectId ? { projectId } : {}),
    },
  };
}

export function openBudgetChangeRequest(router: Router, projectId?: string) {
  return router.push(budgetChangeRoute(projectId));
}

export function showBudgetOverrunPrompt(
  error: unknown,
  router: Router,
  projectId?: string,
) {
  const detail = getErrorMessage(error, "项目预算不足");
  if (!isBudgetOverrunMessage(detail)) return false;

  Modal.confirm({
    title: "项目预算不足",
    content: `${detail}\n\n请前往“跨部门协同中心 → 预算版本与控制”申请调整项目总预算，审批通过后再重新提交。`,
    okText: "申请预算变更",
    cancelText: "暂不申请",
    onOk: () => openBudgetChangeRequest(router, projectId),
  });
  return true;
}
