import { request } from "./http";

export type RiskWorkflowStatus = "UNCLAIMED" | "CLAIMED" | "PROCESSING" | "IGNORED" | "CLOSED";

export type RiskWorkflowResponse = {
  riskKey: string;
  status: RiskWorkflowStatus;
  owner?: string;
  note?: string;
  reason?: string;
  updatedByName?: string;
  processedAt?: string;
  updatedAt?: string;
};

export type RiskWorkflowActionResponse = {
  riskKey: string;
  fromStatus?: RiskWorkflowStatus;
  toStatus: RiskWorkflowStatus;
  operatorName?: string;
  owner?: string;
  note?: string;
  reason?: string;
  createdAt?: string;
};

export type RiskItemResponse = {
  key: string;
  module: string;
  moduleName: string;
  title: string;
  subject: string;
  description: string;
  severity: "HIGH" | "MEDIUM" | "LOW";
  status: "OPEN" | "PENDING" | "OVERDUE";
  amount?: number;
  date?: string;
  route: string;
  ruleCode?: string;
  slaHours?: number;
  dueAt?: string;
  slaOverdue: boolean;
  workflow?: RiskWorkflowResponse;
};

export type RiskRuleConfigResponse = {
  id?: string;
  ruleCode: string;
  name: string;
  module: string;
  enabled: boolean;
  highThreshold?: number;
  mediumThreshold?: number;
  warningDays?: number;
  slaHours?: number;
  defaultOwner?: string;
  escalationOwner?: string;
  remark?: string;
};

export type RiskModuleSummaryResponse = {
  module: string;
  moduleName: string;
  totalCount: number;
  highCount: number;
  overdueCount: number;
  slaOverdueCount: number;
  amount?: number;
};

export type RiskTrendPointResponse = {
  date: string;
  totalCount: number;
  highCount: number;
  overdueCount: number;
  closedCount: number;
  amount?: number;
};

export type RiskSummaryResponse = {
  totalCount: number;
  highCount: number;
  overdueCount: number;
  slaOverdueCount: number;
  closedCount: number;
  amount?: number;
  modules: RiskModuleSummaryResponse[];
  trends: RiskTrendPointResponse[];
};

export function listRiskItems() {
  return request<RiskItemResponse[]>({ method: "GET", url: "/risk/items" });
}

export function getRiskSummary(days = 14) {
  return request<RiskSummaryResponse>({ method: "GET", url: "/risk/summary", params: { days } });
}

export function snapshotRiskToday() {
  return request<number>({ method: "POST", url: "/risk/snapshots/today" });
}

export function scanRiskEscalations() {
  return request<number>({ method: "POST", url: "/risk/escalations/scan" });
}

export function listRiskRules() {
  return request<RiskRuleConfigResponse[]>({ method: "GET", url: "/risk/rules" });
}

export function listRiskWorkflows() {
  return request<RiskWorkflowResponse[]>({ method: "GET", url: "/risk/workflows" });
}

export function listRiskWorkflowActions(riskKey: string) {
  return request<RiskWorkflowActionResponse[]>({ method: "GET", url: "/risk/workflows/actions", params: { riskKey } });
}

export function updateRiskWorkflow(data: { riskKey: string; status: RiskWorkflowStatus; owner?: string; note?: string; reason?: string }) {
  return request<RiskWorkflowResponse>({ method: "POST", url: "/risk/workflows", data });
}

export function batchUpdateRiskWorkflow(data: { riskKeys: string[]; status: RiskWorkflowStatus; owner?: string; note?: string; reason?: string }) {
  return request<RiskWorkflowResponse[]>({ method: "POST", url: "/risk/workflows/batch", data });
}
