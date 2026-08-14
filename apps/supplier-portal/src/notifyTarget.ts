import type { PortalNotification } from "./api";

/** 通知点击后的跳转目标：按 relatedType 路由到对应页面。 */
export function notificationRoute(item: Pick<PortalNotification, "relatedType" | "relatedId">): {
  path: string;
  query?: Record<string, string>;
} {
  const type = item.relatedType || "";
  const inquiryTypes = new Set([
    "INQUIRY",
    "INQUIRY_INVITATION",
    "CLARIFICATION_ANSWER",
    "AWARD",
    "NOT_AWARDED",
  ]);
  const orderTypes = new Set([
    "ORDER",
    "ORDER_DOCUMENT",
    "ORDER_CHANGE",
    "SHIPMENT",
    "RECEIPT",
    "INSPECTION",
    "CONTRACT",
  ]);
  const financeTypes = new Set(["INVOICE", "PAYABLE", "RECONCILIATION"]);
  if (inquiryTypes.has(type)) {
    return item.relatedId
      ? { path: "/inquiries", query: { inquiry: item.relatedId } }
      : { path: "/inquiries" };
  }
  if (orderTypes.has(type)) {
    return item.relatedId
      ? { path: "/orders", query: { order: item.relatedId } }
      : { path: "/orders" };
  }
  if (financeTypes.has(type)) {
    if (type === "INVOICE" && item.relatedId) {
      return { path: "/finance", query: { invoice: item.relatedId } };
    }
    if (item.relatedId) {
      return { path: "/finance", query: { payable: item.relatedId } };
    }
    return { path: "/finance" };
  }
  if (type === "PERFORMANCE") return { path: "/dashboard" };
  if (type === "CHANGE_REQUEST") return { path: "/profile" };
  if (type === "DOCUMENT" || type === "QUALIFICATION") return { path: "/documents" };
  if (type === "ACCOUNT") return { path: "/account" };
  return { path: "/dashboard" };
}
