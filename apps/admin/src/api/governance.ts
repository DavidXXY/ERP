import { request, requestAllPages } from "./http";

export type ControlStatus =
  | "DRAFT"
  | "ACTIVE"
  | "BLOCKED"
  | "COMPLETED"
  | "CANCELLED";
export type ReconciliationStatus = "UNMATCHED" | "SUGGESTED" | "MATCHED";
export type AccountingPeriodStatus = "OPEN" | "CLOSING" | "CLOSED";

export type ControlTypeOption = { type: string; domain: string; label: string };
export type ControlPayload = {
  controlType: string;
  businessId?: string;
  businessNo?: string;
  name: string;
  owner: string;
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
  plannedStart?: string;
  plannedEnd?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  budgetAmount?: number;
  committedAmount?: number;
  actualAmount?: number;
  forecastAmount?: number;
  progressPercent?: number;
  reviewFrequencyDays?: number;
  details?: Record<string, unknown>;
};
export type ControlRecord = ControlPayload & {
  id: string;
  controlCode: string;
  typeLabel: string;
  businessDomain: string;
  status: ControlStatus;
  lastReviewedOn?: string;
  nextReviewOn?: string;
  activatedAt?: string;
  completedAt?: string;
  completedBy?: string;
  completionNote?: string;
  createdAt: string;
  updatedAt: string;
};
export type ControlException = {
  key: string;
  controlId: string;
  controlCode: string;
  controlType: string;
  domain: string;
  name: string;
  owner: string;
  exceptionType: string;
  severity: "LOW" | "MEDIUM" | "HIGH";
  message: string;
  dueDate?: string;
  exposureAmount: number;
};
export type GovernanceAction = {
  id: string;
  entityType: "CONTROL" | "PERIOD" | "BANK_LINE";
  entityId: string;
  entityNo?: string;
  actionType: string;
  fromStatus?: string;
  toStatus?: string;
  operatorName: string;
  note?: string;
  createdAt: string;
};
export type DomainSummary = {
  domain: string;
  total: number;
  active: number;
  exceptionCount: number;
  exposureAmount: number;
};
export type GovernanceOverview = {
  totalControls: number;
  activeControls: number;
  blockedControls: number;
  overdueControls: number;
  highRiskControls: number;
  budgetAmount: number;
  committedAmount: number;
  actualAmount: number;
  forecastAmount: number;
  forecastVariance: number;
  unmatchedBankLines: number;
  matchedBankLines: number;
  closedPeriods: number;
  domains: DomainSummary[];
};
export type AccountingPeriod = {
  id: string;
  fiscalYear: number;
  periodNo: number;
  status: AccountingPeriodStatus;
  openedAt: string;
  closingStartedAt?: string;
  closedAt?: string;
  closedBy?: string;
  closeReason?: string;
  reopenedAt?: string;
  reopenedBy?: string;
  reopenReason?: string;
  pendingAction?: "FORCE_CLOSE" | "REOPEN";
  actionRequestedBy?: string;
  actionRequestedAt?: string;
  actionRequestReason?: string;
};
export type CloseReadiness = { ready: boolean; blockers: string[] };
export type BankLine = {
  id: string;
  accountNoMasked: string;
  transactionDate: string;
  direction: "IN" | "OUT";
  amount: number;
  counterparty?: string;
  bankReference: string;
  summary?: string;
  reconciliationStatus: ReconciliationStatus;
  matchedBizType?: string;
  matchedBizId?: string;
  matchedBizNo?: string;
  matchedAt?: string;
  matchedBy?: string;
  matchNote?: string;
};
export type BankImportLine = Omit<
  BankLine,
  | "id"
  | "reconciliationStatus"
  | "matchedBizType"
  | "matchedBizId"
  | "matchedBizNo"
  | "matchedAt"
  | "matchedBy"
  | "matchNote"
>;

export const getGovernanceOverview = () =>
  request<GovernanceOverview>({ method: "GET", url: "/governance/overview" });
export const listControlTypes = () =>
  request<ControlTypeOption[]>({
    method: "GET",
    url: "/governance/control-types",
  });
export const listControls = (params?: {
  type?: string;
  status?: string;
  keyword?: string;
}) =>
  requestAllPages<ControlRecord>(
    {
      method: "GET",
      url: "/governance/controls",
      params,
    },
    200,
  );
export const createControl = (data: ControlPayload) =>
  request<ControlRecord>({ method: "POST", url: "/governance/controls", data });
export const updateControl = (id: string, data: ControlPayload) =>
  request<ControlRecord>({
    method: "PUT",
    url: `/governance/controls/${id}`,
    data,
  });
export const transitionControl = (
  id: string,
  status: ControlStatus,
  note?: string,
) =>
  request<ControlRecord>({
    method: "POST",
    url: `/governance/controls/${id}/transition`,
    data: { status, note },
  });
export const reviewControl = (id: string, reviewedOn: string, note?: string) =>
  request<ControlRecord>({
    method: "POST",
    url: `/governance/controls/${id}/review`,
    data: { reviewedOn, note },
  });
export const listControlExceptions = () =>
  request<ControlException[]>({ method: "GET", url: "/governance/exceptions" });
export const listAccountingPeriods = () =>
  request<AccountingPeriod[]>({ method: "GET", url: "/governance/periods" });
export const openAccountingPeriod = (fiscalYear: number, periodNo: number) =>
  request<AccountingPeriod>({
    method: "POST",
    url: "/governance/periods",
    data: { fiscalYear, periodNo },
  });
export const listGovernanceActions = (entityType: string, entityId: string) =>
  request<GovernanceAction[]>({
    method: "GET",
    url: `/governance/actions/${entityType}/${entityId}`,
  });
export const getCloseReadiness = (year: number, month: number) =>
  request<CloseReadiness>({
    method: "GET",
    url: `/governance/periods/${year}/${month}/readiness`,
  });
export const closeAccountingPeriod = (
  year: number,
  month: number,
  force: boolean,
  reason?: string,
) =>
  request<AccountingPeriod>({
    method: "POST",
    url: `/governance/periods/${year}/${month}/close`,
    data: { force, reason },
  });
export const reopenAccountingPeriod = (
  year: number,
  month: number,
  reason: string,
) =>
  request<AccountingPeriod>({
    method: "POST",
    url: `/governance/periods/${year}/${month}/reopen`,
    data: { reason },
  });
export const listBankLines = (status?: ReconciliationStatus) =>
  requestAllPages<BankLine>(
    {
      method: "GET",
      url: "/governance/bank-lines",
      params: { status },
    },
    200,
  );
export const importBankLines = (lines: BankImportLine[]) =>
  request<{ imported: number; duplicates: number; suggested: number }>({
    method: "POST",
    url: "/governance/bank-lines/import",
    data: { lines },
  });
export const reconcileBankLine = (
  id: string,
  data: {
    businessType: string;
    businessId: string;
    businessNo: string;
    note?: string;
  },
) =>
  request<BankLine>({
    method: "POST",
    url: `/governance/bank-lines/${id}/reconcile`,
    data,
  });
export const unreconcileBankLine = (id: string, reason: string) =>
  request<BankLine>({
    method: "POST",
    url: `/governance/bank-lines/${id}/unreconcile`,
    params: { reason },
  });
