// ============================================================
// Finance API types — manually curated from backend DTOs
// Run `npm run gen:api` when backend is running to auto-generate
// ============================================================

// ---- Receivable (CRM module) ----
export type ReceivableStatus = "INVOICE_PENDING" | "PAYMENT_PENDING" | "SETTLED" | "OVERDUE";

export interface ReceivableDTO {
  id: string;
  customerId: string;
  customerName: string;
  contractId: string;
  contractCode: string;
  code: string;
  sourceNo: string;
  amount: number;
  dueDate: string;
  invoiceNo: string | null;
  invoiceDate: string | null;
  settledAmount: number;
  outstandingAmount: number;
  status: ReceivableStatus;
}

// ---- Payable (Procurement module) ----
export type PayableStatus = "PENDING" | "PARTIAL_PAID" | "PAID" | "CANCELLED";

export interface PayableDTO {
  id: string;
  code: string;
  supplierId: string;
  supplierName: string;
  orderId: string;
  orderCode: string;
  amount: number;
  paidAmount: number;
  outstandingAmount: number;
  reservedAmount: number;
  availableAmount: number;
  dueDate: string;
  status: PayableStatus;
  overdue: boolean;
}

// ---- Finance Overview ----
export interface FinanceOverviewDTO {
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
}

// ---- Ledger / Accounting ----
export type VoucherStatus = "DRAFT" | "APPROVED" | "POSTED";

export interface AccountingEntryDTO {
  id: string;
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
  summary: string;
}

export interface VoucherDTO {
  id: string;
  code: string;
  bizType: string;
  bizNo: string;
  voucherDate: string;
  description: string;
  status: VoucherStatus;
  totalDebit: number;
  totalCredit: number;
  entries: AccountingEntryDTO[];
}

export interface LedgerOverviewDTO {
  voucherCount: number;
  totalDebit: number;
  totalCredit: number;
  revenue: number;
  expense: number;
  profit: number;
  cashBalance: number;
}

export interface StatementLineDTO {
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
  balance: number;
}

export interface FinancialStatementsDTO {
  assets: StatementLineDTO[];
  liabilities: StatementLineDTO[];
  revenue: StatementLineDTO[];
  expenses: StatementLineDTO[];
  totalAssets: number;
  totalLiabilities: number;
  totalRevenue: number;
  totalExpense: number;
  profit: number;
  netCashFlow: number;
}

// ---- Payment Application (Finance module) ----
export type PaymentApplicationStatus = "PENDING" | "APPROVED" | "REJECTED" | "PAID" | "CANCELLED";

export interface PaymentApplicationDTO {
  id: string;
  code: string;
  payableId: string;
  payableCode: string;
  supplierId: string;
  supplierName: string;
  requestedAmount: number;
  requestedDate: string;
  applicantName: string;
  purpose: string;
  status: PaymentApplicationStatus;
  approvalComment: string | null;
  approverName: string | null;
  approvedAt: string | null;
  paymentId: string | null;
  paymentCode: string | null;
}

export type PaymentMethod = "BANK_TRANSFER" | "CHEQUE" | "CASH" | "WECHAT" | "ALIPAY";

export interface PaymentRecordDTO {
  id: string;
  code: string;
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
}

// ---- CRM DTOs ----
export interface CustomerDTO {
  id: string;
  name: string;
  level: string;
  industry: string;
  region: string;
  owner: string;
  status: string;
  contacts: string;
  phone: string;
  lastVisitDate: string | null;
  nextPlan: string | null;
  risk: string;
  source: string;
}

export interface ContractDTO {
  id: string;
  code: string;
  customerId: string;
  customerName: string;
  projectName: string;
  type: string;
  amount: number;
  startDate: string;
  endDate: string;
  serviceCycle: string;
  nextBillDate: string | null;
  nextInspectDate: string | null;
  receivableAmount: number;
  status: string;
  signedDate: string;
  companyId: string;
}
