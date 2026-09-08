import { request } from "@/utils/http";

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
  employeeName: string;
  departmentName?: string;
  ccUserIds: string[];
  ccNames: string[];
  createdAt: string;
}

export interface ReportCcOption {
  id: string;
  name: string;
}

export interface ReportProjectOption {
  id: string;
  code: string;
  name: string;
}

export const listMyReports = () =>
  request<ReportRecord[]>({ url: "/reports/my" });

export const createReport = (data: Record<string, unknown>) =>
  request<ReportRecord>({ url: "/reports", method: "POST", data });

export const listCcCandidates = () =>
  request<ReportCcOption[]>({ url: "/reports/cc-candidates" });

export const listMyProjects = () =>
  request<ReportProjectOption[]>({ url: "/reports/my-projects" });
