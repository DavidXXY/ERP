// API Client — connects the React prototype to the Spring Boot backend

const TOKEN_KEY = "ops_erp_token";

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface LoginResponse {
  token: string;
  user: { id: string; username: string; displayName: string; roles: string[]; permissions: string[] };
}

export type ReceivableStatus = "INVOICE_PENDING" | "PAYMENT_PENDING" | "SETTLED" | "OVERDUE";
export type FinancePayableStatus = "PENDING" | "PARTIAL_PAID" | "PAID" | "CANCELLED";

export interface ReceivableResponse {
  id: string; customerId: string; customerName: string; contractId: string;
  contractCode: string; code: string; sourceNo: string;
  amount: number; dueDate: string; invoiceNo: string | null;
  invoiceDate: string | null; settledAmount: number;
  outstandingAmount: number; status: ReceivableStatus;
}

export interface FinancePayableResponse {
  id: string; code: string; supplierId: string; supplierName: string;
  orderId: string; orderCode: string; amount: number; paidAmount: number;
  outstandingAmount: number; reservedAmount: number; availableAmount: number;
  dueDate: string; status: FinancePayableStatus; overdue: boolean;
}

export interface FinanceOverviewResponse {
  receivableAmount: number; receivedAmount: number;
  receivableOutstanding: number; receivableOverdue: number;
  payableAmount: number; paidAmount: number;
  payableOutstanding: number; payableOverdue: number;
  netCashInflow: number; pendingPaymentApplications: number;
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}
function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const res = await fetch("/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) throw new Error(`Login failed: ${res.status}`);
  const body: ApiResponse<LoginResponse> = await res.json();
  if (body.code !== 200) throw new Error(body.message || "Login failed");
  setToken(body.data.token);
  return body.data;
}

async function authFetch<T>(url: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = { ...(init?.headers as Record<string, string>) };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  if (!headers["Content-Type"]) headers["Content-Type"] = "application/json";
  const res = await fetch(url, { ...init, headers });
  if (res.status === 401) { clearToken(); throw new Error("unauthorized"); }
  if (!res.ok) throw new Error(`API error ${res.status}`);
  const body: ApiResponse<T> = await res.json();
  if (body.code !== 200) throw new Error(body.message || "API error");
  return body.data;
}

export function fetchFinanceOverview(): Promise<FinanceOverviewResponse> {
  return authFetch<FinanceOverviewResponse>("/api/finance/overview");
}
export function fetchReceivables(): Promise<ReceivableResponse[]> {
  return authFetch<ReceivableResponse[]>("/api/finance/receivables");
}
export function registerInvoice(id: string, invoiceNo: string, invoiceDate: string): Promise<ReceivableResponse> {
  return authFetch<ReceivableResponse>(`/api/finance/receivables/${id}/invoice`, {
    method: "POST", body: JSON.stringify({ invoiceNo, invoiceDate }),
  });
}
export function recordReceipt(id: string, amount: number, receivedDate: string, referenceNo: string, recorderName: string): Promise<ReceivableResponse> {
  return authFetch<ReceivableResponse>(`/api/finance/receivables/${id}/receipts`, {
    method: "POST", body: JSON.stringify({ amount, receivedDate, referenceNo, recorderName }),
  });
}
export function fetchPayables(): Promise<FinancePayableResponse[]> {
  return authFetch<FinancePayableResponse[]>("/api/finance/payables");
}

// ---- Ledger API ----

export interface VoucherEntryResponse {
  id: string;
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
  summary: string;
}

export interface VoucherResponse {
  id: string;
  code: string;
  bizType: string;
  bizNo: string;
  voucherDate: string;
  description: string;
  status: string;
  totalDebit: number;
  totalCredit: number;
  entries: VoucherEntryResponse[];
}

export interface LedgerOverviewResponse {
  voucherCount: number;
  totalDebit: number;
  totalCredit: number;
  revenue: number;
  expense: number;
  profit: number;
  cashBalance: number;
}

export interface StatementLine {
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
  balance: number;
}

export interface FinancialStatementsResponse {
  assets: StatementLine[];
  liabilities: StatementLine[];
  revenue: StatementLine[];
  expenses: StatementLine[];
  totalAssets: number;
  totalLiabilities: number;
  totalRevenue: number;
  totalExpense: number;
  profit: number;
  netCashFlow: number;
}

export function fetchLedgerOverview(): Promise<LedgerOverviewResponse> {
  return authFetch<LedgerOverviewResponse>("/api/finance/ledger/overview");
}

export function fetchLedgerVouchers(): Promise<VoucherResponse[]> {
  return authFetch<VoucherResponse[]>("/api/finance/ledger/vouchers");
}

export function fetchFinancialStatements(): Promise<FinancialStatementsResponse> {
  return authFetch<FinancialStatementsResponse>("/api/finance/ledger/statements");
}

// ---- CRM API ----

export interface CustomerResponse {
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

export interface OpportunityResponse {
  id: string;
  customerId: string;
  customerName: string;
  title: string;
  value: number;
  stage: string;
  owner: string;
  createdAt: string;
  expectedCloseDate: string;
  probability: number;
}

export interface QuotePlanResponse {
  id: string;
  code: string;
  customerId: string;
  customerName: string;
  title: string;
  totalAmount: number;
  status: string;
  version: number;
  createdAt: string;
  owner: string;
}

export interface ContractResponse {
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

export interface FollowUpResponse {
  id: string;
  customerId: string;
  customerName: string;
  type: string;
  content: string;
  owner: string;
  contactPerson: string;
  nextPlan: string | null;
  createdAt: string;
}

export interface RenewalResponse {
  id: string;
  contractId: string;
  contractCode: string;
  customerId: string;
  customerName: string;
  originalEndDate: string;
  newEndDate: string;
  renewedAmount: number;
  status: string;
  operator: string;
}

export interface CustomerProfileResponse {
  customerId: string;
  customerName: string;
  totalContractAmount: number;
  totalServiceCost: number;
  profitMargin: number;
  orderCount: number;
  faultCount: number;
  renewalRate: number;
  avgResponseHours: number;
  satisfactionScore: number;
}

// Customer detail endpoints
export function fetchCustomers(): Promise<CustomerResponse[]> {
  return authFetch<CustomerResponse[]>("/api/crm/customers");
}

export function fetchCustomerById(id: string): Promise<CustomerResponse> {
  return authFetch<CustomerResponse>(`/api/crm/customers/${id}`);
}

// Operation endpoints
export function fetchOpportunities(): Promise<OpportunityResponse[]> {
  return authFetch<OpportunityResponse[]>("/api/crm/opportunities");
}

export function fetchQuotes(): Promise<QuotePlanResponse[]> {
  return authFetch<QuotePlanResponse[]>("/api/crm/quotes");
}

export function fetchContracts(): Promise<ContractResponse[]> {
  return authFetch<ContractResponse[]>("/api/crm/contracts");
}

export function fetchFollowUps(): Promise<FollowUpResponse[]> {
  return authFetch<FollowUpResponse[]>("/api/crm/follow-ups");
}

export function fetchRenewals(): Promise<RenewalResponse[]> {
  return authFetch<RenewalResponse[]>("/api/crm/renewals");
}

export function fetchCustomerProfiles(): Promise<CustomerProfileResponse[]> {
  return authFetch<CustomerProfileResponse[]>("/api/crm/profiles");
}

// ---- Procurement / Supply Chain API ----

export interface SupplierResponse {
  id: string; name: string; contactPerson: string; phone: string;
  accountPeriod: string; status: string; bankName: string; bankAccount: string;
}

export interface PurchaseRequestResponse {
  id: string; code: string; applicantName: string; department: string;
  partName: string; quantity: number; estimatedAmount: number; status: string;
  createdAt: string; purpose: string;
}

export interface PurchaseOrderResponse {
  id: string; code: string; supplierId: string; supplierName: string;
  totalAmount: number; status: string; createdAt: string;
  items: Array<{ partName: string; quantity: number; unitPrice: number; amount: number }>;
}

export function fetchSuppliers(): Promise<SupplierResponse[]> {
  return authFetch<SupplierResponse[]>("/api/procurement/suppliers");
}
export function fetchPurchaseRequests(): Promise<PurchaseRequestResponse[]> {
  return authFetch<PurchaseRequestResponse[]>("/api/procurement/requests");
}
export function fetchPurchaseOrders(): Promise<PurchaseOrderResponse[]> {
  return authFetch<PurchaseOrderResponse[]>("/api/procurement/orders");
}

// ---- Inventory API ----

export interface PartResponse {
  id: string; code: string; name: string; model: string; category: string;
  unit: string; unitCost: number; stock: number; safetyStock: number;
  location: string; supplierName: string; status: string;
}

export interface InventoryMovementResponse {
  id: string; partId: string; partName: string; quantity: number;
  direction: string; type: string; createdAt: string; operatorName: string;
  referenceNo: string;
}

export function fetchParts(): Promise<PartResponse[]> {
  return authFetch<PartResponse[]>("/api/inventory/parts");
}

// ---- Project API ----

export interface ProjectResponse {
  id: string; code: string; name: string; customerName: string;
  status: string; budget: number; actualCost: number; startDate: string;
  endDate: string; managerName: string; stage: string;
}

export function fetchProjects(): Promise<ProjectResponse[]> {
  return authFetch<ProjectResponse[]>("/api/project");
}

// ---- Office / OA API ----

export interface ApprovalResponse {
  id: string; code: string; type: string; title: string; applicantName: string;
  amount: number; status: string; createdAt: string;
}

export interface ExpenseResponse {
  id: string; code: string; employeeName: string; category: string;
  amount: number; status: string; createdAt: string;
}

export interface OutsourceResponse {
  id: string; code: string; supplierName: string; projectName: string;
  amount: number; status: string; createdAt: string;
}

export interface DocumentResponse {
  id: string; fileName: string; fileSize: number; category: string;
  bizType: string; bizId: string; uploadedBy: string; createdAt: string;
}

export function fetchOfficeApprovals(): Promise<ApprovalResponse[]> {
  return authFetch<ApprovalResponse[]>("/api/office/approvals");
}
export function fetchOfficeExpenses(): Promise<ExpenseResponse[]> {
  return authFetch<ExpenseResponse[]>("/api/office/expenses");
}
export function fetchOfficeOutsourcing(): Promise<OutsourceResponse[]> {
  return authFetch<OutsourceResponse[]>("/api/office/outsourcing");
}
export function fetchOfficeDocuments(): Promise<DocumentResponse[]> {
  return authFetch<DocumentResponse[]>("/api/office/documents");
}

// ---- BI / Dashboard API ----

export interface BiDashboardResponse {
  revenue: number; revenueGrowth: number;
  cost: number; costGrowth: number;
  orderCompletionRate: number; customerRenewalRate: number;
  materialConsumption: number;
  monthlyRevenue: number[];
  monthlyCosts: number[];
  serviceRevenue: number; receivableRate: number;
  projectMargin: number; orderCount: number;
}

export function fetchBiDashboard(): Promise<BiDashboardResponse> {
  return authFetch<BiDashboardResponse>("/api/bi/dashboard");
}

// ---- Payment Application & Record API ----

export interface PaymentApplicationResponse {
  id: string; code: string; payableId: string; payableCode: string;
  supplierId: string; supplierName: string; requestedAmount: number;
  requestedDate: string; applicantName: string; purpose: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "PAID" | "CANCELLED";
  approvalComment: string | null; approverName: string | null;
  approvedAt: string | null; paymentId: string | null; paymentCode: string | null;
}
export interface PaymentRecordResponse {
  id: string; code: string; applicationId: string;
  payableId: string; payableCode: string; supplierName: string;
  amount: number; paidDate: string; paymentMethod: string;
  bankReference: string; payerName: string;
}

export interface CreatePaymentApplicationRequest {
  payableId: string; requestedAmount: number;
  requestedDate: string; applicantName: string; purpose: string;
}
export interface ProcessPaymentApplicationRequest {
  decision: "APPROVED" | "REJECTED"; comment: string; approverName: string;
}
export interface ExecutePaymentRequest {
  paidDate: string; paymentMethod: string; bankReference: string; payerName: string;
}

export function fetchPaymentApplications(): Promise<PaymentApplicationResponse[]> {
  return authFetch<PaymentApplicationResponse[]>("/api/finance/payment-applications");
}
export function createPaymentApplication(req: CreatePaymentApplicationRequest): Promise<PaymentApplicationResponse> {
  return authFetch<PaymentApplicationResponse>("/api/finance/payment-applications", {
    method: "POST", body: JSON.stringify(req),
  });
}
export function approvePaymentApplication(id: string, req: ProcessPaymentApplicationRequest): Promise<PaymentApplicationResponse> {
  return authFetch<PaymentApplicationResponse>(`/api/finance/payment-applications/${id}/approval`, {
    method: "POST", body: JSON.stringify(req),
  });
}
export function executePayment(id: string, req: ExecutePaymentRequest): Promise<PaymentRecordResponse> {
  return authFetch<PaymentRecordResponse>(`/api/finance/payment-applications/${id}/payment`, {
    method: "POST", body: JSON.stringify(req),
  });
}
export function fetchPaymentRecords(): Promise<PaymentRecordResponse[]> {
  return authFetch<PaymentRecordResponse[]>("/api/finance/payments");
}
