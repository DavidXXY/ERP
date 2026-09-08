import { request } from "./http";

// ====== Types ======

export type ReportType = "DAILY" | "WEEKLY" | "MONTHLY";
export type ReportCategory = "DAILY" | "ENGINEERING";
export type ReportStatus = "PENDING_CONFIRM" | "CONFIRMED" | "REJECTED";

export interface ReportRecord {
  id: string;
  reportType: ReportType;
  reportCategory: ReportCategory;
  projectId?: string;
  projectName?: string;
  additionalProjectId?: string;
  additionalProjectName?: string;
  hours?: number;
  reportStatus?: ReportStatus;
  approvalId?: string;
  reportDate: string;
  content: string;
  progressSummary?: string;
  planNext?: string;
  issue?: string;
  employeeId?: string;
  employeeName: string;
  departmentName?: string;
  ccUserIds: string[];
  ccNames: string[];
  createdAt: string;
  updatedAt: string;
}

export interface ReportPayload {
  reportType: ReportType;
  reportCategory?: ReportCategory;
  projectId?: string;
  additionalProjectId?: string;
  hours?: number;
  reportDate: string;
  content: string;
  progressSummary?: string;
  planNext?: string;
  issue?: string;
  ccUserIds: string[];
}

export interface ReportUserOption {
  id: string;
  name: string;
}

export interface ReportProjectOption {
  id: string;
  code: string;
  name: string;
}

export interface ReceivedReportQuery {
  type?: string;
  fromDate?: string;
  toDate?: string;
}

// ====== APIs ======

export function getMyReports() {
  return request<ReportRecord[]>({ method: "GET", url: "/reports/my" });
}

export function createReport(data: ReportPayload) {
  return request<ReportRecord>({ method: "POST", url: "/reports", data });
}

export function getCcCandidates() {
  return request<ReportUserOption[]>({
    method: "GET",
    url: "/reports/cc-candidates",
  });
}

export function getMyProjects() {
  return request<ReportProjectOption[]>({
    method: "GET",
    url: "/reports/my-projects",
  });
}

export function getReceivedReports(query: ReceivedReportQuery = {}) {
  return request<ReportRecord[]>({
    method: "GET",
    url: "/reports/received",
    params: query,
  });
}
