import { http, request } from "./http";

export type CollaborationOverview = {
  cards: Record<string, number>;
  todos: any[];
  handovers: any[];
  budgets: any[];
  financialMilestones: any[];
  procurementReconciliation: any[];
  staffAssignments: any[];
  responsibilities: any[];
  timesheets: any[];
  resourceLoads: any[];
  budgetVersions: any[];
  periodLocks: any[];
  actionLogs: any[];
  managementDashboard: Record<string, number>;
};
export type CollaborationReferences = {
  users: Array<{ id: string; name: string; departmentId?: string }>;
  departments: Array<{ id: string; name: string }>;
  projects: Array<{ id: string; code: string; name: string }>;
};
export const getCollaborationOverview = (params?: { year?: number; month?: number; departmentId?: string }) =>
  request<CollaborationOverview>({ method: "GET", url: "/collaboration/overview", params });
export async function getCollaborationReferences() {
  const rows = await request<CollaborationReferences[]>({ method: "GET", url: "/collaboration/references" });
  return rows[0] || { users: [], departments: [], projects: [] };
}
export const acceptProjectHandover = (id: string, data: { comment?: string }) =>
  request<any>({ method: "POST", url: `/collaboration/handovers/${id}/accept`, data });
export const saveProjectHandover = (data: any) =>
  request<any>({ method: "POST", url: "/collaboration/handovers", data });
export const uploadHandoverMaterial = (id: string, file: File) => {
  const data = new FormData(); data.append("file", file);
  return request<any>({ method: "POST", url: `/collaboration/handovers/${id}/materials`, data });
};
export const saveResponsibility = (data: {
  sourceType: string; sourceId: string; ownerUserId?: string; departmentId?: string; collaboratorDepartmentIds: string[];
}) => request<any>({ method: "POST", url: "/collaboration/responsibilities", data });
export const assignProjectStaff = (data: {
  projectId: string; userId: string; roleName: string; plannedHours: number; hourlyCost: number; allocationPercent: number; startDate: string; endDate: string;
}) => request<any>({ method: "POST", url: "/collaboration/staff", data });
export const recordProjectHours = (id: string, actualHours: number) =>
  request<any>({ method: "PUT", url: `/collaboration/staff/${id}/hours`, data: { actualHours } });
export const actOnCollaborationTodo = (type: string, id: string, data: any) =>
  request<any>({ method: "POST", url: `/collaboration/todos/${type}/${id}/actions`, data });
export const submitTimesheet = (data: any) =>
  request<any>({ method: "POST", url: "/collaboration/timesheets", data });
export const reviewTimesheet = (id: string, decision: string, comment?: string) =>
  request<any>({ method: "POST", url: `/collaboration/timesheets/${id}/review`, data: { decision, comment } });
export const lockTimesheetPeriod = (yearMonth: string, reason: string) =>
  request<any>({ method: "POST", url: "/collaboration/timesheet-period-locks", data: { yearMonth, reason } });
export const unlockTimesheetPeriod = (yearMonth: string, comment?: string) =>
  request<any>({ method: "DELETE", url: `/collaboration/timesheet-period-locks/${yearMonth}`, params: { comment } });
export const requestBudgetChange = (data: any) =>
  request<any>({ method: "POST", url: "/collaboration/budget-changes", data });
export const reviewBudgetChange = (id: string, decision: string, comment?: string) =>
  request<any>({ method: "POST", url: `/collaboration/budget-changes/${id}/review`, data: { decision, comment } });
export async function exportCollaboration(params?: { year?: number; month?: number; departmentId?: string }) {
  const response = await http.get("/collaboration/export", { params, responseType: "blob" });
  const url = URL.createObjectURL(response.data); const link = document.createElement("a");
  link.href = url; link.download = "跨部门协同管理报表.csv"; link.click(); URL.revokeObjectURL(url);
}
