import type { Approval, NotificationRecord } from "@/types/domain";
import { request, requestAllPages } from "@/utils/http";

export const listApprovals = () => requestAllPages<Approval>("/mobile/approvals");
export const getApproval = (id: string) => request<Approval>({ url: `/office/approvals/${id}` });
export const processApproval = (id: string, decision: "APPROVED" | "REJECTED", comment: string, operatorName: string) =>
  request<Approval>({ url: `/hr/self/approvals/${id}/process`, method: "POST", data: { decision, comment, approverName: operatorName } });
export const listNotifications = () => requestAllPages<NotificationRecord>("/office/notifications");
export const markNotificationRead = (id: string) => request<NotificationRecord>({ url: `/office/notifications/${id}/read`, method: "POST" });
export const refreshNotifications = () => request<number>({ url: "/office/notifications/refresh", method: "POST" });

export const createExpense = (data: Record<string, unknown>) => request({ url: "/office/expenses", method: "POST", data });
export const createTravel = (data: Record<string, unknown>) => request({ url: "/office/travels", method: "POST", data });
export const listExpenses = () => request<Array<Record<string, unknown>>>({ url: "/office/expenses" });
export const listTravelApplications = () => request<Array<Record<string, unknown>>>({ url: "/office/travels" });
