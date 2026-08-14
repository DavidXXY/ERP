import { request, requestAllPages } from "./http";
import type { ContractStatus, Receivable, ReceivableStatus } from "./crm";

export type FinanceOverview = {
  receivableAmount: number;
  receivedAmount: number;
  receivableOutstanding: number;
  receivableOverdue: number;
  payableAmount: number;
  paidAmount: number;
  payableOutstanding: number;
  payableOverdue: number;
  netCashInflow: number;
  pendingPaymentApplications: number;
};

export type MonthlyCashFlow = {
  month: number;
  receipt: number;
  payment: number;
  net: number;
};
export type FinanceForecastBucket = {
  key: string;
  label: string;
  horizonDays: number;
  receivable: number;
  payable: number;
  net: number;
};
export type FinanceAgingBucket = {
  key: string;
  label: string;
  receivable: number;
  payable: number;
  receivableCount: number;
  payableCount: number;
};
export type FinanceReconciliationItem = {
  key: string;
  businessAmount: number;
  ledgerAmount: number;
  difference: number;
};
export type FinanceAnalytics = {
  asOf: string;
  fiscalYear: number;
  scope: FinanceScope;
  monthlyCashFlow: MonthlyCashFlow[];
  forecast: FinanceForecastBucket[];
  aging: FinanceAgingBucket[];
  reconciliation: {
    ledger: FinanceReconciliationItem[];
    bankLineCount: number;
    matchedBankLines: number;
    suggestedBankLines: number;
    unmatchedBankLines: number;
    unmatchedBankAmount: number;
  };
  tax: {
    outputGross: number;
    outputNet: number;
    outputTax: number;
    inputGross: number;
    inputNet: number;
    inputTax: number;
    netTaxPayable: number;
    pendingOutputInvoices: number;
    inputInvoiceExceptions: number;
    adjustedInvoices: number;
  };
  cashPlan: {
    baseline: number;
    committed: number;
    actual: number;
    forecast: number;
    variance: number;
    activePlans: number;
  };
  risks: Array<{
    key: string;
    severity: "HIGH" | "MEDIUM" | "LOW";
    category: string;
    title: string;
    description: string;
    amount: number;
    count: number;
  }>;
};

export type FinanceScope = {
  organizationId?: string;
  organizationName: string;
  organizationPath: string;
  includeDescendants: boolean;
  organizationCount: number;
  unrestricted: boolean;
  unallocatedExcluded: boolean;
};

export type FinanceOrganizationNode = {
  id: string;
  name: string;
  type: string;
  fullPath: string;
  children: FinanceOrganizationNode[];
};

export type ContributionSalesperson = {
  id: string;
  displayName: string;
  organizationId: string;
  organizationName: string;
  organizationPath: string;
  enabled: boolean;
};

export type FinanceContribution = {
  asOf: string;
  fiscalYear: number;
  scope: {
    subjectType: "ORGANIZATION" | "USER";
    subjectId?: string;
    subjectName: string;
    subjectPath: string;
    includeDescendants: boolean;
    organizationCount: number;
    attributionBasis: string;
  };
  summary: {
    contractAmount: number;
    actualCost: number;
    grossProfit: number;
    grossMarginRate: number;
    receivedAmount: number;
    paidAmount: number;
    netCashFlow: number;
    receivableOutstanding: number;
    payableOutstanding: number;
    collectionRate: number;
    projectCount: number;
  };
  monthlyCashFlow: Array<{
    month: number;
    receipt: number;
    payment: number;
    netCash: number;
  }>;
  projects: Array<{
    projectId: string;
    projectCode: string;
    projectName: string;
    customerName?: string;
    stage: string;
    salesOwnerName?: string;
    contractAmount: number;
    actualCost: number;
    grossProfit: number;
    grossMarginRate: number;
    receivedAmount: number;
    paidAmount: number;
    netCashFlow: number;
    receivableOutstanding: number;
    payableOutstanding: number;
  }>;
  dataQuality: {
    unattributedProjectCount: number;
    unattributedReceivableCount: number;
    unlinkedReceivableCount: number;
    note: string;
  };
};

export type TaxInvoiceLine = {
  id: string;
  side: "OUTPUT" | "INPUT";
  businessNo: string;
  invoiceNo: string;
  partnerName: string;
  invoiceDate: string;
  grossAmount: number;
  netAmount: number;
  taxAmount: number;
  taxRate: number;
  status: "NORMAL" | "VOIDED" | "RED_FLUSHED";
  verificationStatus?: string;
  adjustmentReason?: string;
  adjustedAt?: string;
  adjustedBy?: string;
};

export type FinancePayableStatus =
  | "PENDING"
  | "PARTIAL_PAID"
  | "PAID"
  | "CANCELLED";

export type FinancePayable = {
  id: string;
  code?: string;
  supplierId: string;
  supplierName: string;
  orderId: string;
  orderCode: string;
  handlerName?: string;
  amount: number;
  adjustedAmount: number;
  effectiveAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  refundAmount: number;
  reservedAmount: number;
  availableAmount: number;
  dueDate: string;
  status: FinancePayableStatus;
  overdue: boolean;
};

export type PaymentApplicationStatus =
  | "PENDING_APPROVAL"
  | "APPROVED"
  | "REJECTED"
  | "PAID";
export type PaymentMethod = "BANK_TRANSFER" | "CHECK" | "CASH" | "OTHER";

export type PaymentApplication = {
  id: string;
  code?: string;
  payableId: string;
  payableCode: string;
  supplierId: string;
  supplierName: string;
  requestedAmount: number;
  requestedDate: string;
  applicantName: string;
  purpose: string;
  status: PaymentApplicationStatus;
  approvalComment?: string;
  approverName?: string;
  approvedAt?: string;
  paymentId?: string;
  paymentCode?: string;
  payableIds?: string[];
};

export type PaymentRecord = {
  id: string;
  code?: string;
  applicationId: string;
  applicationCode: string;
  payableId: string;
  payableCode: string;
  supplierId: string;
  supplierName: string;
  amount: number;
  paidDate: string;
  paymentMethod: PaymentMethod;
  bankReference: string;
  payerName: string;
  sourceType?: string;
  note?: string;
};

export type PaymentSplit = {
  payableId?: string;
  amount: number;
  paidDate: string;
  paymentMethod: PaymentMethod;
  bankReference: string;
  note?: string;
};

export type PaymentExecutionResult = {
  paymentCode: string;
  totalAmount: number;
  records: PaymentRecord[];
};

export type PayableAdjustmentType =
  | "CREDIT"
  | "CLAIM"
  | "CORRECTION"
  | "CANCELLATION";

export type PayableAdjustment = {
  id: string;
  code: string;
  payableId: string;
  orderId: string;
  supplierId: string;
  adjustmentType: PayableAdjustmentType;
  amount: number;
  reason?: string;
  operatorName: string;
  appliedAt: string;
  status: string;
  source: string;
  sourceId?: string;
};

export type FinanceReceivableDetail = {
  receivable: Receivable;
  customerInvoice: {
    customerId: string;
    customerCode?: string;
    customerName?: string;
    ownerName?: string;
    invoiceTitle?: string;
    taxNo?: string;
    bankName?: string;
    bankAccount?: string;
    registeredAddress?: string;
    registeredPhone?: string;
    paymentHabit?: string;
  };
  contract?: {
    id: string;
    quoteId?: string;
    code?: string;
    projectName: string;
    contractType: string;
    amount: number;
    taxRate: number;
    netAmount: number;
    startDate: string;
    endDate: string;
    serviceCycle?: string;
    status: ContractStatus;
    receivableStatus: ReceivableStatus;
    createdAt?: string;
  };
};

export function getFinanceOverview() {
  return request<FinanceOverview>({ method: "GET", url: "/finance/overview" });
}

export function getFinanceAnalytics(params?: {
  asOf?: string;
  year?: number;
  organizationId?: string;
  includeDescendants?: boolean;
}) {
  return request<FinanceAnalytics>({
    method: "GET",
    url: "/finance/analytics",
    params,
  });
}

export function listFinanceOrganizations() {
  return request<FinanceOrganizationNode[]>({
    method: "GET",
    url: "/finance/organizations",
  });
}

export function listContributionSalespeople(params?: {
  organizationId?: string;
  includeDescendants?: boolean;
}) {
  return request<ContributionSalesperson[]>({
    method: "GET",
    url: "/finance/contribution/salespeople",
    params,
  });
}

export function getFinanceContribution(params: {
  subjectType: "ORGANIZATION" | "USER";
  subjectId?: string;
  includeDescendants?: boolean;
  asOf?: string;
  year?: number;
}) {
  return request<FinanceContribution>({
    method: "GET",
    url: "/finance/contribution/analytics",
    params,
  });
}

export function listTaxLedger(params?: {
  from?: string;
  to?: string;
  side?: string;
  status?: string;
}) {
  return request<TaxInvoiceLine[]>({
    method: "GET",
    url: "/finance/tax-ledger",
    params,
  });
}

export function adjustTaxInvoice(
  side: TaxInvoiceLine["side"],
  id: string,
  payload: {
    status: "VOIDED" | "RED_FLUSHED";
    adjustmentDate: string;
    reason: string;
  },
) {
  return request<TaxInvoiceLine>({
    method: "POST",
    url: `/finance/tax-ledger/${side}/${id}/adjust`,
    data: payload,
  });
}

export function listFinanceReceivables() {
  return requestAllPages<Receivable>(
    { method: "GET", url: "/finance/receivables" },
    200,
  );
}

export function getFinanceReceivableDetail(id: string) {
  return request<FinanceReceivableDetail>({
    method: "GET",
    url: `/finance/receivables/${id}`,
  });
}

export function registerFinanceInvoice(
  id: string,
  payload: { invoiceNo: string; invoiceDate: string },
) {
  return request<Receivable>({
    method: "POST",
    url: `/finance/receivables/${id}/invoice`,
    data: payload,
  });
}

export function reviewFinanceInvoice(
  id: string,
  payload: {
    decision: "APPROVED" | "REJECTED";
    reviewerName: string;
    comment?: string;
  },
) {
  return request<Receivable>({
    method: "POST",
    url: `/finance/receivables/${id}/invoice-review`,
    data: payload,
  });
}

export function recordFinanceReceipt(
  id: string,
  payload: {
    amount: number;
    receivedDate: string;
    referenceNo: string;
    recorderName: string;
  },
) {
  return request<Receivable>({
    method: "POST",
    url: `/finance/receivables/${id}/receipts`,
    data: payload,
  });
}

export function listFinancePayables() {
  return requestAllPages<FinancePayable>(
    { method: "GET", url: "/finance/payables" },
    200,
  );
}

export function listPaymentApplications() {
  return requestAllPages<PaymentApplication>(
    {
      method: "GET",
      url: "/finance/payment-applications",
    },
    200,
  );
}

export function getPaymentApprovalCapability() {
  return request<boolean>({
    method: "GET",
    url: "/finance/payment-applications/can-approve",
  });
}

export function createPaymentApplication(payload: {
  code?: string;
  payableId: string;
  payableIds?: string[];
  requestedAmount: number;
  requestedDate: string;
  applicantName: string;
  purpose: string;
}) {
  return request<PaymentApplication>({
    method: "POST",
    url: "/finance/payment-applications",
    data: payload,
  });
}

export function processPaymentApplication(
  id: string,
  payload: {
    decision: "APPROVED" | "REJECTED";
    comment: string;
    approverName: string;
  },
) {
  return request<PaymentApplication>({
    method: "POST",
    url: `/finance/payment-applications/${id}/approval`,
    data: payload,
  });
}

export function executePayment(
  id: string,
  payload: {
    paymentCode: string;
    payments: PaymentSplit[];
  },
) {
  return request<PaymentExecutionResult>({
    method: "POST",
    url: `/finance/payment-applications/${id}/payment`,
    data: payload,
  });
}

export function cancelPayable(id: string, reason: string) {
  return request<FinancePayable>({
    method: "POST",
    url: `/finance/payables/${id}/cancel`,
    data: { reason },
  });
}

export function applyPayableAdjustment(
  id: string,
  payload: {
    adjustmentType: PayableAdjustmentType;
    amount: number;
    reason?: string;
    appliedAt?: string;
  },
) {
  return request<PayableAdjustment>({
    method: "POST",
    url: `/finance/payables/${id}/adjustments`,
    data: payload,
  });
}

export function listPayableAdjustments(id: string) {
  return request<PayableAdjustment[]>({
    method: "GET",
    url: `/finance/payables/${id}/adjustments`,
  });
}

export function listPaymentRecords() {
  return requestAllPages<PaymentRecord>(
    { method: "GET", url: "/finance/payments" },
    200,
  );
}
