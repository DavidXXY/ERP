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
export type Approval = { id: string; code: string; approvalType: ApprovalType; title: string; sourceNo?: string; amount: number; status: ApprovalStatus; applicantName: string; content: string; approverName?: string; approvalComment?: string; processedAt?: string; createdAt: string; departmentName?: string; businessType?: string; projectCode?: string; supplierRisk?: string; customerLevel?: string; approvalMode?: string; currentStep?: number; totalSteps?: number; currentApproverName?: string; matchedRuleText?: string; actions: Array<{ id: string; decision: ApprovalStatus; operatorName: string; comment: string; actionType?: string; stepNo?: number; createdAt: string }> };
export type Expense = { id: string; code: string; claimantId?: string; claimantName: string; projectId?: string; projectCode?: string; workOrderId?: string; workOrderCode?: string; expenseType: ExpenseType; amount: number; expenseDate: string; description: string; status: ExpenseStatus; approvalRequestId: string };
export type Outsource = { id: string; code: string; supplierId: string; supplierName: string; projectId?: string; projectCode?: string; workOrderId?: string; workOrderCode?: string; serviceType: string; description: string; amount: number; plannedDate: string; status: OutsourceStatus; approvalRequestId: string; acceptanceNote?: string };
export type DocumentRecord = { id: string; bizType: string; bizId?: string; fileName: string; contentType?: string; sizeBytes: number; createdAt: string };
export type NotificationRecord = { id: string; type: string; title: string; content: string; relatedType?: string; relatedId?: string; read: boolean; readAt?: string; createdAt: string };
export type AuditRecord = { id: string; username?: string; httpMethod: string; requestPath: string; responseStatus: number; clientIp?: string; durationMs: number; createdAt: string };
export type WorkbenchTodo = { type: string; id: string; title: string; subtitle: string; amount: number; priority: string; route: string; createdAt: string };
export type WorkbenchWarning = { type: string; id: string; title: string; content: string; severity: string; route: string; createdAt: string };
export type Workbench = { todos: WorkbenchTodo[]; warnings: WorkbenchWarning[]; pendingTodoCount: number; highSeverityWarningCount: number };

export function getOfficeOverview() { return request<OfficeOverview>({ method: "GET", url: "/office/overview" }); }
export function getOfficeWorkbench() { return request<Workbench>({ method: "GET", url: "/office/workbench" }); }
export function getOfficeReferences() { return request<OfficeReferences>({ method: "GET", url: "/office/references" }); }
export function listApprovals() { return request<Approval[]>({ method: "GET", url: "/office/approvals" }); }
export function createApproval(data: { code: string; approvalType: ApprovalType; title: string; sourceNo?: string; amount: number; applicantName: string; content: string; departmentName?: string; businessType?: string; projectCode?: string; supplierRisk?: string; customerLevel?: string }) { return request<Approval>({ method: "POST", url: "/office/approvals", data }); }
export function processApproval(id: string, data: { decision: "APPROVED" | "REJECTED"; comment: string; approverName: string }) { return request<Approval>({ method: "POST", url: `/office/approvals/${id}/process`, data }); }
export function transferApproval(id: string, data: { targetUserId: string; comment: string; operatorName: string }) { return request<Approval>({ method: "POST", url: `/office/approvals/${id}/transfer`, data }); }
export function addSignApproval(id: string, data: { targetUserId: string; comment: string; operatorName: string }) { return request<Approval>({ method: "POST", url: `/office/approvals/${id}/add-sign`, data }); }
export function withdrawApproval(id: string, data: { comment: string; operatorName: string }) { return request<Approval>({ method: "POST", url: `/office/approvals/${id}/withdraw`, data }); }
export function listExpenses() { return request<Expense[]>({ method: "GET", url: "/office/expenses" }); }
export function createExpense(data: { code: string; claimantId?: string; claimantName: string; projectId?: string; workOrderId?: string; expenseType: ExpenseType; amount: number; expenseDate: string; description: string }) { return request<Expense>({ method: "POST", url: "/office/expenses", data }); }
export function listOutsourcing() { return request<Outsource[]>({ method: "GET", url: "/office/outsourcing" }); }
export function createOutsource(data: { code: string; supplierId: string; projectId?: string; workOrderId?: string; serviceType: string; description: string; amount: number; plannedDate: string; applicantName: string }) { return request<Outsource>({ method: "POST", url: "/office/outsourcing", data }); }
export function completeOutsource(id: string, data: { acceptanceNote: string }) { return request<Outsource>({ method: "POST", url: `/office/outsourcing/${id}/complete`, data }); }
export function listDocuments() { return request<DocumentRecord[]>({ method: "GET", url: "/office/documents" }); }
export function searchDocuments(bizType?: string, bizId?: string, page = 0, size = 20) {
  const params: Record<string, string | number> = { page, size };
  if (bizType) params.bizType = bizType;
  if (bizId) params.bizId = bizId;
  return request<{ content: DocumentRecord[]; totalElements: number; number: number; size: number }>({ method: "GET", url: "/office/documents/search", params });
}
export function listDocumentsByBiz(bizType: string, bizId?: string) {
  const params: Record<string, string> = { bizType };
  if (bizId) params.bizId = bizId;
  return request<DocumentRecord[]>({ method: "GET", url: "/office/documents/by-biz", params });
}
export function getDocumentCount(bizType: string, bizId?: string) {
  const params: Record<string, string> = { bizType };
  if (bizId) params.bizId = bizId;
  return request<number>({ method: "GET", url: "/office/documents/count", params });
}
export function uploadDocument(data: { bizType: string; bizId?: string; file: File }) { const form = new FormData(); form.append("bizType", data.bizType); if (data.bizId) form.append("bizId", data.bizId); form.append("file", data.file); return request<DocumentRecord>({ method: "POST", url: "/office/documents", data: form }); }
export function uploadDocuments(data: { bizType: string; bizId?: string; files: File[] }) {
  const form = new FormData(); form.append("bizType", data.bizType);
  if (data.bizId) form.append("bizId", data.bizId);
  data.files.forEach(f => form.append("files", f));
  return request<DocumentRecord[]>({ method: "POST", url: "/office/documents/batch", data: form });
}
export function updateDocumentName(id: string, fileName: string) { return request<DocumentRecord>({ method: "PUT", url: `/office/documents/${id}`, params: { fileName } }); }
export function deleteDocument(id: string) { return request<void>({ method: "DELETE", url: `/office/documents/${id}` }); }
export function deleteDocumentsByBiz(bizType: string, bizId: string) { return request<void>({ method: "DELETE", url: "/office/documents/by-biz", params: { bizType, bizId } }); }
export async function downloadDocument(item: DocumentRecord) { const response = await http.get<Blob>(`/office/documents/${item.id}/download`, { responseType: "blob" }); const url = URL.createObjectURL(response.data); const anchor = document.createElement("a"); anchor.href = url; anchor.download = item.fileName; anchor.click(); URL.revokeObjectURL(url); }
export async function previewDocument(item: DocumentRecord) {
  const response = await http.get<Blob>(`/office/documents/${item.id}/preview`, { responseType: "blob" });
  const type = response.data.type || item.contentType || "";
  if (!type.startsWith("image/") && type !== "application/pdf") {
    await downloadDocument(item);
    return;
  }
  const url = URL.createObjectURL(response.data);
  window.open(url, "_blank", "noopener,noreferrer");
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
export function listNotifications() { return request<NotificationRecord[]>({ method: "GET", url: "/office/notifications" }); }
export function readNotification(id: string) { return request<NotificationRecord>({ method: "POST", url: `/office/notifications/${id}/read` }); }
export function refreshNotifications() { return request<number>({ method: "POST", url: "/office/notifications/refresh" }); }
export function listAudits() { return request<AuditRecord[]>({ method: "GET", url: "/office/audits" }); }


// ====== Audit Log ======
export type AuditLogRecord = {
  id: string;
  username?: string;
  httpMethod: string;
  requestPath: string;
  responseStatus: number;
  clientIp?: string;
  durationMs: number;
  queryString?: string;
  operationType?: string;
  bizModule?: string;
  bizObject?: string;
  createdAt: string;
};

export function listAuditLogs(page: number = 0, size: number = 20) {
  return request<{ content: AuditLogRecord[]; totalElements: number; totalPages: number; size: number; number: number }>({
    method: "GET", url: "/office/audits", params: { page, size }
  });
}

export function getUnreadNotificationCount() {
  return request<number>({ method: "GET", url: "/office/notifications/count" });
}
