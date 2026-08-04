import { request } from "./http";

export type OperationsOverview = {
  pendingPeriodJobs: number;
  failedVoucherRequests: number;
  unreconciledPartners: number;
  unlockedTaxPeriods: number;
  draftConsolidations: number;
  snapshots: number;
  budgetVariance: number;
  forecastLiquidity: number;
};
export type PeriodJob = {
  id: string;
  fiscalYear: number;
  periodNo: number;
  processType: string;
  description: string;
  amount: number;
  debitAccountCode: string;
  creditAccountCode: string;
  autoReverse: boolean;
  reversalDate?: string;
  status: string;
  voucherId?: string;
  reversalVoucherId?: string;
  idempotencyKey: string;
  executedAt?: string;
  executedBy?: string;
};
export type OpeningValidation = {
  fiscalYear: number;
  valid: boolean;
  totalDebit: number;
  totalCredit: number;
  difference: number;
  issues: Array<{
    key: string;
    severity: string;
    message: string;
    difference: number;
  }>;
};
export type BudgetVariance = {
  controlId: string;
  controlCode: string;
  name: string;
  owner: string;
  budget: number;
  committed: number;
  actual: number;
  forecast: number;
  variance: number;
  usageRate: number;
  status: string;
};
export type PartnerStatement = {
  partnerType: "CUSTOMER" | "SUPPLIER";
  partnerId: string;
  partnerCode: string;
  partnerName: string;
  periodEnd: string;
  ledgerBalance: number;
  confirmedBalance: number;
  difference: number;
  status: string;
  reconciliationId?: string;
  confirmationNote?: string;
  confirmedAt?: string;
  confirmedBy?: string;
};
export type CashScenario = {
  id: string;
  name: string;
  asOfDate: string;
  horizonDays: number;
  openingCash: number;
  expectedReceipts: number;
  expectedPayments: number;
  receiptAdjustment: number;
  paymentAdjustment: number;
  forecastCash: number;
  status: string;
  assumptions?: string;
  createdAt: string;
};
export type TaxFiling = {
  id: string;
  fiscalYear: number;
  periodNo: number;
  outputTax: number;
  inputTax: number;
  taxPayable: number;
  ledgerTax: number;
  difference: number;
  status: string;
  filingReference?: string;
  lockedAt?: string;
  lockedBy?: string;
  snapshotId?: string;
};
export type Consolidation = {
  id: string;
  fiscalYear: number;
  periodNo: number;
  name: string;
  entityCount: number;
  combinedRevenue: number;
  combinedExpense: number;
  intercompanyRevenue: number;
  intercompanyExpense: number;
  consolidatedProfit: number;
  status: string;
  snapshotId?: string;
  completedAt?: string;
  completedBy?: string;
};
export type ReportSnapshot = {
  id: string;
  reportType: string;
  scopeKey: string;
  fiscalYear?: number;
  periodNo?: number;
  contentHash: string;
  evidenceNote?: string;
  capturedAt: string;
  capturedBy: string;
};
export type VoucherRequest = {
  id: string;
  idempotencyKey: string;
  sourceType: string;
  businessNo: string;
  status: string;
  attemptCount: number;
  voucherId?: string;
  lastError?: string;
  lastAttemptAt?: string;
  completedAt?: string;
};

const base = "/finance/operations";
export const getOperationsOverview = () =>
  request<OperationsOverview>({ method: "GET", url: `${base}/overview` });
export const listPeriodJobs = (params?: { year?: number; month?: number }) =>
  request<PeriodJob[]>({ method: "GET", url: `${base}/period-jobs`, params });
export const createPeriodJob = (
  data: Omit<
    PeriodJob,
    | "id"
    | "status"
    | "voucherId"
    | "reversalVoucherId"
    | "executedAt"
    | "executedBy"
  >,
) => request<PeriodJob>({ method: "POST", url: `${base}/period-jobs`, data });
export const executePeriodJob = (id: string) =>
  request<PeriodJob>({
    method: "POST",
    url: `${base}/period-jobs/${id}/execute`,
  });
export const reverseDuePeriodJobs = (asOf?: string) =>
  request<PeriodJob[]>({
    method: "POST",
    url: `${base}/period-jobs/reverse-due`,
    params: { asOf },
  });
export const validateOpening = (year: number) =>
  request<OpeningValidation>({
    method: "GET",
    url: `${base}/opening-validation/${year}`,
  });
export const listBudgetVariance = () =>
  request<BudgetVariance[]>({ method: "GET", url: `${base}/budget-variance` });
export const listPartnerStatements = (type: string, periodEnd: string) =>
  request<PartnerStatement[]>({
    method: "GET",
    url: `${base}/partner-statements`,
    params: { type, periodEnd },
  });
export const confirmPartnerStatement = (
  type: string,
  partnerId: string,
  periodEnd: string,
  data: { statementBalance: number; status: string; note: string },
) =>
  request<PartnerStatement>({
    method: "POST",
    url: `${base}/partner-statements/${type}/${partnerId}/confirm`,
    params: { periodEnd },
    data,
  });
export const listCashScenarios = () =>
  request<CashScenario[]>({ method: "GET", url: `${base}/cash-scenarios` });
export const createCashScenario = (data: {
  name: string;
  asOfDate: string;
  horizonDays: number;
  openingCash: number;
  receiptAdjustment: number;
  paymentAdjustment: number;
  assumptions?: string;
}) =>
  request<CashScenario>({
    method: "POST",
    url: `${base}/cash-scenarios`,
    data,
  });
export const listTaxFilings = () =>
  request<TaxFiling[]>({ method: "GET", url: `${base}/tax-filings` });
export const reconcileTaxFiling = (year: number, month: number) =>
  request<TaxFiling>({
    method: "POST",
    url: `${base}/tax-filings/${year}/${month}/reconcile`,
  });
export const lockTaxFiling = (
  year: number,
  month: number,
  filingReference: string,
) =>
  request<TaxFiling>({
    method: "POST",
    url: `${base}/tax-filings/${year}/${month}/lock`,
    data: { filingReference },
  });
export const listConsolidations = () =>
  request<Consolidation[]>({ method: "GET", url: `${base}/consolidations` });
export const createConsolidation = (data: {
  fiscalYear: number;
  periodNo: number;
  name: string;
  entities: Array<{
    entityCode: string;
    entityName: string;
    revenue: number;
    expense: number;
  }>;
  intercompanyRevenue: number;
  intercompanyExpense: number;
}) =>
  request<Consolidation>({
    method: "POST",
    url: `${base}/consolidations`,
    data,
  });
export const completeConsolidation = (id: string) =>
  request<Consolidation>({
    method: "POST",
    url: `${base}/consolidations/${id}/complete`,
  });
export const listReportSnapshots = () =>
  request<ReportSnapshot[]>({ method: "GET", url: `${base}/snapshots` });
export const captureReportSnapshot = (data: {
  reportType: string;
  scopeKey: string;
  fiscalYear?: number;
  periodNo?: number;
  payload: string;
  evidenceNote?: string;
}) =>
  request<ReportSnapshot>({ method: "POST", url: `${base}/snapshots`, data });
export const listVoucherRequests = () =>
  request<VoucherRequest[]>({ method: "GET", url: `${base}/voucher-requests` });
