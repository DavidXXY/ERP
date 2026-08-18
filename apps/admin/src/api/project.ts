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
export type ProjectExecutionStatus =
  | "ACTIVE"
  | "PAUSED"
  | "CANCELLED"
  | "CLOSED";
export type ProjectCostCategory =
  | "LABOR"
  | "MATERIAL"
  | "SUBCONTRACT"
  | "TRAVEL"
  | "OTHER";
export type ProjectCostSource =
  | "MANUAL"
  | "INVENTORY"
  | "PROCUREMENT"
  | "EXPENSE"
  | "SUBCONTRACT";

export type ContractSummary = {
  id: string;
  code: string;
  projectName: string;
  amount: number;
  startDate: string;
  endDate: string;
  status: string;
};
export type Project = {
  id: string;
  customerId?: string;
  customerName?: string;
  code?: string;
  name: string;
  projectType: ProjectType;
  managerUserId?: string;
  managerName: string;
  managerAssignedByUserId?: string;
  managerAssignedByName?: string;
  managerAssignedAt?: string;
  managerAssignmentComment?: string;
  siteAddress: string;
  contractId?: string;
  contractCode?: string;
  contractProjectName?: string;
  contractStatus?: string;
  parentProjectId?: string;
  parentProjectCode?: string;
  parentProjectName?: string;
  childProjectCount: number;
  contractAmount: number;
  plannedStartDate?: string;
  plannedEndDate?: string;
  stage: ProjectStage;
  approvalStatus: ProjectApprovalStatus;
  approvalComment?: string;
  approverName?: string;
  approvedAt?: string;
  approverUserId?: string;
  executionStatus: ProjectExecutionStatus;
  statusComment?: string;
  statusChangedAt?: string;
  budgetAmount: number;
  actualCost: number;
  grossMargin: number;
  budgetVariance: number;
  progress: number;
  warrantyEndDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
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
  managerUserId?: string;
  siteAddress: string;
  contractAmount: number;
  plannedStartDate: string;
  plannedEndDate: string;
  budgetItems: ProjectBudgetInput[];
  warrantyEndDate?: string;
  contractId?: string;
  parentProjectId?: string;
  quoteId?: string;
};

export type UpdateProjectPayload = {
  name: string;
  siteAddress: string;
  contractAmount: number;
  plannedStartDate: string;
  plannedEndDate: string;
  warrantyEndDate?: string;
  budgetItems: ProjectBudgetInput[];
};

export type ProjectCloseoutReview = {
  id: string;
  projectId: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  requestComment?: string;
  reviewComment?: string;
  requestedBy?: string;
  requestedAt?: string;
  reviewedBy?: string;
  reviewedAt?: string;
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

export type MilestoneStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED";

export type ProjectMilestone = {
  id: string;
  projectId: string;
  name: string;
  plannedDate?: string;
  actualDate?: string;
  status: MilestoneStatus;
  sortOrder: number;
  remark?: string;
};

export type ProjectTimelineEntry = {
  type: "STAGE" | "COST" | "CLOSEOUT" | "BUDGET" | "MANAGER";
  occurredAt: string;
  actor?: string;
  title: string;
  detail?: string;
};

export type ProjectStaff = {
  id: string;
  userId?: string;
  displayName?: string;
  roleName: string;
  plannedHours: number;
  actualHours: number;
  allocationPercent: number;
  startDate: string;
  endDate: string;
  certificateStatus: string;
  status: string;
};

export type RiskSeverity = "LOW" | "MEDIUM" | "HIGH";
export type RiskStatus = "OPEN" | "MITIGATING" | "CLOSED";

export type ProjectRisk = {
  id: string;
  projectId: string;
  title: string;
  description?: string;
  severity: RiskSeverity;
  status: RiskStatus;
  ownerName?: string;
  dueDate?: string;
  resolution?: string;
  createdAt: string;
  createdBy?: string;
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

export type ProjectManagerOption = {
  id: string;
  username: string;
  displayName: string;
};

export type ProjectListParams = {
  keyword?: string;
  approvalStatus?: ProjectApprovalStatus;
  stage?: ProjectStage;
  executionStatus?: ProjectExecutionStatus;
  page?: number;
  size?: number;
};

export function listProjects(params?: ProjectListParams) {
  return request<PageResponse<Project>>({
    method: "GET",
    url: "/projects",
    params,
  });
}

export function listProjectPortfolio(params?: ProjectListParams) {
  return request<PageResponse<ProjectDetail>>({
    method: "GET",
    url: "/projects/portfolio",
    params,
  });
}

export function listProjectProfitability() {
  return request<ProjectProfitability[]>({
    method: "GET",
    url: "/projects/profitability",
  });
}

export function listProjectManagerOptions() {
  return request<ProjectManagerOption[]>({
    method: "GET",
    url: "/projects/manager-options",
  });
}

export function getProjectManagerAssignmentCapability() {
  return request<boolean>({
    method: "GET",
    url: "/projects/manager-assignment-capability",
  });
}

export function listPreSalesSupport(archived = false) {
  return request<QuotePlan[]>({
    method: "GET",
    url: "/projects/presales-support",
    params: { archived },
  });
}

export function archivePreSalesSupport(id: string) {
  return request<QuotePlan>({
    method: "POST",
    url: `/projects/presales-support/${id}/archive`,
  });
}

export function unarchivePreSalesSupport(id: string) {
  return request<QuotePlan>({
    method: "POST",
    url: `/projects/presales-support/${id}/unarchive`,
  });
}

export function submitPreSalesCost(
  id: string,
  payload: {
    projectManager: string;
    laborCost?: number;
    laborTaxRate?: number;
    materialCost?: number;
    materialTaxRate?: number;
    subcontractCost?: number;
    subcontractTaxRate?: number;
    travelCost?: number;
    travelTaxRate?: number;
    equipmentCost?: number;
    equipmentTaxRate?: number;
    riskReserve?: number;
    riskReserveTaxRate?: number;
    otherCost?: number;
    otherTaxRate?: number;
    suggestedPrice?: number;
    costRemark?: string;
  },
) {
  return request<QuoteCostRequest>({
    method: "POST",
    url: `/projects/presales-support/${id}/cost`,
    data: payload,
  });
}

export function approvePreSalesCost(
  id: string,
  payload: {
    decision: ApprovalDecision;
    approverName: string;
    comment: string;
  },
) {
  return request<QuoteCostRequest>({
    method: "POST",
    url: `/projects/presales-support/${id}/approval`,
    data: payload,
  });
}

export function createProject(payload: CreateProjectPayload) {
  return request<ProjectDetail>({
    method: "POST",
    url: "/projects",
    data: payload,
  });
}

export function updateProject(id: string, payload: UpdateProjectPayload) {
  return request<ProjectDetail>({
    method: "PUT",
    url: `/projects/${id}`,
    data: payload,
  });
}

export function getProject(id: string) {
  return request<ProjectDetail>({ method: "GET", url: `/projects/${id}` });
}

export function processProjectApproval(
  id: string,
  payload: {
    decision: ProjectApprovalStatus;
    comment: string;
  },
) {
  return request<ProjectDetail>({
    method: "POST",
    url: `/projects/${id}/approval`,
    data: payload,
  });
}

export function assignProjectManager(
  id: string,
  payload: {
    managerUserId: string;
    comment?: string;
    syncChildProjects?: boolean;
  },
) {
  return request<ProjectDetail>({
    method: "POST",
    url: `/projects/${id}/manager`,
    data: payload,
  });
}

export function prepareChildProject(
  id: string,
  payload: {
    siteAddress: string;
    plannedStartDate: string;
    plannedEndDate: string;
    warrantyEndDate?: string;
    budgetItems: ProjectBudgetInput[];
  },
) {
  return request<ProjectDetail>({
    method: "PUT",
    url: `/projects/${id}/preparation`,
    data: payload,
  });
}

export function advanceProjectStage(
  id: string,
  payload: { targetStage: ProjectStage; comment: string },
) {
  return request<ProjectDetail>({
    method: "POST",
    url: `/projects/${id}/stage`,
    data: payload,
  });
}

export function changeProjectExecutionStatus(
  id: string,
  payload: { status: ProjectExecutionStatus; comment: string },
) {
  return request<ProjectDetail>({
    method: "POST",
    url: `/projects/${id}/execution-status`,
    data: payload,
  });
}

export function createProjectCost(
  id: string,
  payload: {
    category: ProjectCostCategory;
    sourceType: ProjectCostSource;
    sourceNo?: string;
    description: string;
    amount: number;
    incurredDate: string;
  },
) {
  return request<ProjectDetail>({
    method: "POST",
    url: `/projects/${id}/costs`,
    data: payload,
  });
}

export function updateProjectCost(
  id: string,
  costId: string,
  payload: {
    category: ProjectCostCategory;
    description: string;
    amount: number;
    incurredDate: string;
  },
) {
  return request<ProjectDetail>({
    method: "PUT",
    url: `/projects/${id}/costs/${costId}`,
    data: payload,
  });
}

export function deleteProjectCost(id: string, costId: string) {
  return request<ProjectDetail>({
    method: "DELETE",
    url: `/projects/${id}/costs/${costId}`,
  });
}

export function rollbackProjectStage(id: string, payload: { comment: string }) {
  return request<ProjectDetail>({
    method: "POST",
    url: `/projects/${id}/stage/rollback`,
    data: payload,
  });
}

export function requestProjectCloseout(
  id: string,
  payload: { comment?: string },
) {
  return request<ProjectCloseoutReview>({
    method: "POST",
    url: `/projects/${id}/closeout/request`,
    data: payload,
  });
}

export function reviewProjectCloseout(
  id: string,
  payload: { decision: "APPROVED" | "REJECTED"; comment: string },
) {
  return request<ProjectCloseoutReview>({
    method: "POST",
    url: `/projects/${id}/closeout/review`,
    data: payload,
  });
}

export function getProjectCloseoutReview(id: string) {
  return request<ProjectCloseoutReview | null>({
    method: "GET",
    url: `/projects/${id}/closeout-review`,
  });
}

export function deleteProject(id: string) {
  return request<void>({ method: "DELETE", url: `/projects/${id}` });
}

export function getProjectTimeline(id: string) {
  return request<ProjectTimelineEntry[]>({
    method: "GET",
    url: `/projects/${id}/timeline`,
  });
}

export function getProjectStaff(id: string) {
  return request<ProjectStaff[]>({
    method: "GET",
    url: `/projects/${id}/staff`,
  });
}

export function listProjectMilestones(id: string) {
  return request<ProjectMilestone[]>({
    method: "GET",
    url: `/projects/${id}/milestones`,
  });
}

export function createProjectMilestone(
  id: string,
  payload: {
    name: string;
    plannedDate?: string;
    actualDate?: string;
    status?: MilestoneStatus;
    sortOrder?: number;
    remark?: string;
  },
) {
  return request<ProjectMilestone>({
    method: "POST",
    url: `/projects/${id}/milestones`,
    data: payload,
  });
}

export function updateProjectMilestone(
  id: string,
  milestoneId: string,
  payload: {
    name: string;
    plannedDate?: string;
    actualDate?: string;
    status?: MilestoneStatus;
    sortOrder?: number;
    remark?: string;
  },
) {
  return request<ProjectMilestone>({
    method: "PUT",
    url: `/projects/${id}/milestones/${milestoneId}`,
    data: payload,
  });
}

export function deleteProjectMilestone(id: string, milestoneId: string) {
  return request<void>({
    method: "DELETE",
    url: `/projects/${id}/milestones/${milestoneId}`,
  });
}

export function listProjectRisks(id: string) {
  return request<ProjectRisk[]>({
    method: "GET",
    url: `/projects/${id}/risks`,
  });
}

export function createProjectRisk(
  id: string,
  payload: {
    title: string;
    description?: string;
    severity?: RiskSeverity;
    status?: RiskStatus;
    ownerName?: string;
    dueDate?: string;
    resolution?: string;
  },
) {
  return request<ProjectRisk>({
    method: "POST",
    url: `/projects/${id}/risks`,
    data: payload,
  });
}

export function updateProjectRisk(
  id: string,
  riskId: string,
  payload: {
    title: string;
    description?: string;
    severity?: RiskSeverity;
    status?: RiskStatus;
    ownerName?: string;
    dueDate?: string;
    resolution?: string;
  },
) {
  return request<ProjectRisk>({
    method: "PUT",
    url: `/projects/${id}/risks/${riskId}`,
    data: payload,
  });
}

export function deleteProjectRisk(id: string, riskId: string) {
  return request<void>({
    method: "DELETE",
    url: `/projects/${id}/risks/${riskId}`,
  });
}
