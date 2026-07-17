import { request } from "./http";
import { type PageResponse } from "./system";
import type { ApprovalDecision, QuoteCostRequest, QuotePlan } from "./crm";

export type ProjectStage =
  | "INITIATED"
  | "BIDDING"
  | "ENTRY"
  | "CONSTRUCTION"
  | "COMMISSIONING"
  | "INITIAL_ACCEPTANCE"
  | "FINAL_ACCEPTANCE"
  | "WARRANTY"
  | "CLOSED";

export type ProjectType = "NEW_CONSTRUCTION" | "RENOVATION" | "O_M_RENOVATION";
export type ProjectApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ProjectCostCategory = "LABOR" | "MATERIAL" | "SUBCONTRACT" | "TRAVEL" | "OTHER";
export type ProjectCostSource = "MANUAL" | "INVENTORY" | "PROCUREMENT" | "EXPENSE" | "SUBCONTRACT";

export type ContractSummary = { id: string; code: string; projectName: string; amount: number; startDate: string; endDate: string; status: string; };
export type Project = {
  id: string;
  customerId?: string;
  customerName?: string;
  code?: string;
  name: string;
  projectType: ProjectType;
  managerName: string;
  siteAddress: string;
  contractId?: string;
  contractCode?: string;
  contractProjectName?: string;
  contractStatus?: string;
  contractAmount: number;
  plannedStartDate?: string;
  plannedEndDate?: string;
  stage: ProjectStage;
  approvalStatus: ProjectApprovalStatus;
  approvalComment?: string;
  approverName?: string;
  approvedAt?: string;
  budgetAmount: number;
  actualCost: number;
  grossMargin: number;
  budgetVariance: number;
  progress: number;
  warrantyEndDate?: string;
};

export type ProjectBudgetInput = {
  category: ProjectCostCategory;
  plannedAmount: number;
  remark?: string;
};

export type CreateProjectPayload = {
  customerId: string;
  code?: string;
  name: string;
  projectType: ProjectType;
  managerName: string;
  siteAddress: string;
  contractAmount: number;
  plannedStartDate: string;
  plannedEndDate: string;
  budgetItems: ProjectBudgetInput[];
  warrantyEndDate?: string;
  contractId?: string;
};

export type ProjectBudgetItem = ProjectBudgetInput & {
  id: string;
  actualAmount: number;
  variance: number;
};

export type ProjectCostEntry = {
  id: string;
  category: ProjectCostCategory;
  sourceType: ProjectCostSource;
  sourceNo?: string;
  description: string;
  amount: number;
  incurredDate: string;
};

export type ProjectStageRecord = {
  id: string;
  fromStage: ProjectStage;
  toStage: ProjectStage;
  progress: number;
  comment: string;
  operatorName: string;
  changedAt: string;
};

export type ProjectDetail = {
  project: Project;
  budgetItems: ProjectBudgetItem[];
  costEntries: ProjectCostEntry[];
  stageRecords: ProjectStageRecord[];
};

export type ProjectProfitability = {
  projectId: string;
  projectCode?: string;
  projectName: string;
  customerName?: string;
  stage: ProjectStage;
  approvalStatus: ProjectApprovalStatus;
  contractAmount: number;
  budgetAmount: number;
  actualCost: number;
  grossMargin: number;
  grossMarginRate: number;
  budgetUsageRate: number;
  riskLevel: string;
  riskMessage: string;
};

export function listProjects(page?: number, size?: number) {
  return request<PageResponse<Project>>({ method: "GET", url: "/projects", params: { page, size } });
}

export function listProjectProfitability() {
  return request<ProjectProfitability[]>({ method: "GET", url: "/projects/profitability" });
}

export function listPreSalesSupport() {
  return request<QuotePlan[]>({ method: "GET", url: "/projects/presales-support" });
}

export function submitPreSalesCost(id: string, payload: { projectManager: string; laborCost?: number; laborTaxRate?: number; materialCost?: number; materialTaxRate?: number; subcontractCost?: number; subcontractTaxRate?: number; travelCost?: number; travelTaxRate?: number; equipmentCost?: number; equipmentTaxRate?: number; riskReserve?: number; riskReserveTaxRate?: number; otherCost?: number; otherTaxRate?: number; suggestedPrice?: number; costRemark?: string }) {
  return request<QuoteCostRequest>({ method: "POST", url: `/projects/presales-support/${id}/cost`, data: payload });
}

export function approvePreSalesCost(id: string, payload: { decision: ApprovalDecision; approverName: string; comment: string }) {
  return request<QuoteCostRequest>({ method: "POST", url: `/projects/presales-support/${id}/approval`, data: payload });
}

export function createProject(payload: CreateProjectPayload) {
  return request<ProjectDetail>({ method: "POST", url: "/projects", data: payload });
}

export function getProject(id: string) {
  return request<ProjectDetail>({ method: "GET", url: `/projects/${id}` });
}

export function processProjectApproval(id: string, payload: { decision: ProjectApprovalStatus; comment: string; approverName: string }) {
  return request<ProjectDetail>({ method: "POST", url: `/projects/${id}/approval`, data: payload });
}

export function assignProjectManager(id: string, payload: { managerName: string; operatorName: string; comment?: string }) {
  return request<ProjectDetail>({ method: "POST", url: `/projects/${id}/manager`, data: payload });
}

export function advanceProjectStage(id: string, payload: { targetStage: ProjectStage; comment: string; operatorName: string }) {
  return request<ProjectDetail>({ method: "POST", url: `/projects/${id}/stage`, data: payload });
}

export function createProjectCost(id: string, payload: { category: ProjectCostCategory; sourceType: ProjectCostSource; sourceNo?: string; description: string; amount: number; incurredDate: string }) {
  return request<ProjectDetail>({ method: "POST", url: `/projects/${id}/costs`, data: payload });
}

export function deleteProject(id: string) {
  return request<void>({ method: "DELETE", url: `/projects/${id}` });
}
