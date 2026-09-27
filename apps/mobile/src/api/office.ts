import type { Approval, NotificationRecord } from "@/types/domain";
import { request, requestAllPages } from "@/utils/http";

// 审批/通知为个人量级列表，但为避免后端 totalPages 异常放大导致无界拉取，
// 上限 10 页（size=200 → 2000 条）足以覆盖真实场景。
const MAX_PAGES = 10;

export const listApprovals = () => requestAllPages<Approval>("/mobile/approvals", 200, { maxPages: MAX_PAGES });
export const getApproval = (id: string) => request<Approval>({ url: `/office/approvals/${id}` });
export const processApproval = (id: string, decision: "APPROVED" | "REJECTED", comment: string, operatorName: string) =>
  request<Approval>({ url: `/hr/self/approvals/${id}/process`, method: "POST", data: { decision, comment, approverName: operatorName } });
export const listNotifications = () => requestAllPages<NotificationRecord>("/office/notifications", 200, { maxPages: MAX_PAGES });
export const markNotificationRead = (id: string) => request<NotificationRecord>({ url: `/office/notifications/${id}/read`, method: "POST" });
export const refreshNotifications = () => request<number>({ url: "/office/notifications/refresh", method: "POST" });

export const createExpense = (data: Record<string, unknown>) => request({ url: "/office/expenses", method: "POST", data });
export const createTravel = (data: Record<string, unknown>) => request({ url: "/office/travels", method: "POST", data });
export const listExpenses = () => request<Array<Record<string, unknown>>>({ url: "/office/expenses" });
export const listTravelApplications = () => request<Array<Record<string, unknown>>>({ url: "/office/travels" });
