import { http, request } from "./http";

export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ApprovalType = "QUOTE" | "CONTRACT" | "PURCHASE" | "OUTSOURCE" | "EXPENSE" | "PAYMENT" | "SEAL" | "LEAVE" | "TRAVEL" | "OTHER";
export type ExpenseType = "TRAVEL" | "TRANSPORT" | "ACCOMMODATION" | "TOOL" | "OTHER";
export type ExpenseStatus = "PENDING_APPROVAL" | "APPROVED" | "REJECTED" | "PAID";
export type OutsourceStatus = "PENDING_APPROVAL" | "APPROVED" | "IN_PROGRESS" | "COMPLETED" | "SETTLED" | "REJECTED";
export type OfficeOverview = { pendingApprovals: number; pendingExpenseAmount: number; approvedExpenseAmount: number; activeOutsourceOrders: number; outsourceAmount: number; unreadNotifications: number; documentCount: number };
export type OfficeReferences = {
  suppliers: Array<{ id: string; code: string; name: string }>;
  projects: Array<{ id: string; code: string; name: string }>;
  workOrders: Array<{ id: string; code: string; title: string }>;
  users: Array<{ id: string; displayName: string; enabled: boolean }>;
};
export type Approval = { id: string; code: string; approvalType: ApprovalType; title: string; sourceNo?: string; amount: number; status: ApprovalStatus; applicantName: string; content: string; approverName?: string; approvalComment?: string; processedAt?: string; createdAt: string; actions: Array<{ id: string; decision: ApprovalStatus; operatorName: string; comment: string; createdAt: string }> };
export type Expense = { id: string; code: string; claimantId?: string; claimantName: string; projectId?: string; projectCode?: string; workOrderId?: string; workOrderCode?: string; expenseType: ExpenseType; amount: number; expenseDate: string; description: string; status: ExpenseStatus; approvalRequestId: string };
export type Outsource = { id: string; code: string; supplierId: string; supplierName: string; projectId?: string; projectCode?: string; workOrderId?: string; workOrderCode?: string; serviceType: string; description: string; amount: number; plannedDate: string; status: OutsourceStatus; approvalRequestId: string; acceptanceNote?: string };
export type DocumentRecord = { id: string; bizType: string; bizId?: string; fileName: string; contentType?: string; sizeBytes: number; createdAt: string };
export type NotificationRecord = { id: string; type: string; title: string; content: string; relatedType?: string; relatedId?: string; read: boolean; readAt?: string; createdAt: string };
export type AuditRecord = { id: string; username?: string; httpMethod: string; requestPath: string; responseStatus: number; clientIp?: string; durationMs: number; createdAt: string };

export function getOfficeOverview() { return request<OfficeOverview>({ method: "GET", url: "/office/overview" }); }
export function getOfficeReferences() { return request<OfficeReferences>({ method: "GET", url: "/office/references" }); }
export function listApprovals() { return request<Approval[]>({ method: "GET", url: "/office/approvals" }); }
export function createApproval(data: { code: string; approvalType: ApprovalType; title: string; sourceNo?: string; amount: number; applicantName: string; content: string }) { return request<Approval>({ method: "POST", url: "/office/approvals", data }); }
export function processApproval(id: string, data: { decision: "APPROVED" | "REJECTED"; comment: string; approverName: string }) { return request<Approval>({ method: "POST", url: `/office/approvals/${id}/process`, data }); }
export function listExpenses() { return request<Expense[]>({ method: "GET", url: "/office/expenses" }); }
export function createExpense(data: { code: string; claimantId?: string; claimantName: string; projectId?: string; workOrderId?: string; expenseType: ExpenseType; amount: number; expenseDate: string; description: string }) { return request<Expense>({ method: "POST", url: "/office/expenses", data }); }
export function listOutsourcing() { return request<Outsource[]>({ method: "GET", url: "/office/outsourcing" }); }
export function createOutsource(data: { code: string; supplierId: string; projectId?: string; workOrderId?: string; serviceType: string; description: string; amount: number; plannedDate: string; applicantName: string }) { return request<Outsource>({ method: "POST", url: "/office/outsourcing", data }); }
export function completeOutsource(id: string, data: { acceptanceNote: string }) { return request<Outsource>({ method: "POST", url: `/office/outsourcing/${id}/complete`, data }); }
export function listDocuments() { return request<DocumentRecord[]>({ method: "GET", url: "/office/documents" }); }
export function uploadDocument(data: { bizType: string; bizId?: string; file: File }) { const form = new FormData(); form.append("bizType", data.bizType); if (data.bizId) form.append("bizId", data.bizId); form.append("file", data.file); return request<DocumentRecord>({ method: "POST", url: "/office/documents", data: form }); }
export async function downloadDocument(item: DocumentRecord) { const response = await http.get<Blob>(`/office/documents/${item.id}/download`, { responseType: "blob" }); const url = URL.createObjectURL(response.data); const anchor = document.createElement("a"); anchor.href = url; anchor.download = item.fileName; anchor.click(); URL.revokeObjectURL(url); }
export function listNotifications() { return request<NotificationRecord[]>({ method: "GET", url: "/office/notifications" }); }
export function readNotification(id: string) { return request<NotificationRecord>({ method: "POST", url: `/office/notifications/${id}/read` }); }
export function refreshNotifications() { return request<number>({ method: "POST", url: "/office/notifications/refresh" }); }
export function listAudits() { return request<AuditRecord[]>({ method: "GET", url: "/office/audits" }); }
