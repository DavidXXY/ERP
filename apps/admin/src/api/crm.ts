import { http, request } from "./http";

export type CustomerLevel = "STRATEGIC" | "KEY" | "NORMAL";
export type RiskStatus = "NORMAL" | "OVERDUE" | "RENEWAL_RISK";
export type ContractStatus = "PENDING_APPROVAL" | "PENDING_SEAL" | "SEAL_APPROVAL" | "ACTIVE" | "RENEWAL_PENDING" | "OVERDUE_RISK" | "CLOSED";
export type ReceivableStatus = "INVOICE_PENDING" | "PAYMENT_PENDING" | "SETTLED" | "OVERDUE";
export type OpportunityStage = "LEAD" | "QUALIFIED" | "SOLUTION" | "QUOTATION" | "NEGOTIATION" | "WON" | "LOST";
export type QuoteStatus = "DRAFT" | "COST_REQUESTED" | "COSTING" | "COST_APPROVED" | "PENDING_APPROVAL" | "APPROVED" | "REJECTED" | "CUSTOMER_ACCEPTED" | "CUSTOMER_DECLINED" | "CONVERTED";
export type QuoteCostStatus = "REQUESTED" | "SUBMITTED" | "APPROVED" | "REJECTED";
export type ApprovalDecision = "APPROVED" | "REJECTED";
export type QuoteCustomerDecision = "ACCEPTED" | "DECLINED";
export type FollowUpType = "VISIT" | "PHONE" | "CALLBACK" | "COMPLAINT";

export type CustomerSummary = {
  id: string;
  code?: string;
  name: string;
  industry: string;
  level: CustomerLevel;
  ownerName: string;
  paymentHabit?: string;
  riskStatus: RiskStatus;
  contactCount: number;
  siteCount: number;
  primaryContact?: string;
};

export type CustomerDetail = CustomerSummary & {
  riskNote?: string;
  invoice: {
    title?: string;
    taxNo?: string;
    bankName?: string;
    bankAccount?: string;
    registeredAddress?: string;
    registeredPhone?: string;
  };
  contacts: Array<{
    id: string;
    name: string;
    title?: string;
    phone?: string;
    email?: string;
    primaryContact: boolean;
  }>;
  sites: Array<{
    id: string;
    name: string;
    address: string;
  }>;
  opportunities: Array<{
    id: string;
    code?: string;
    source?: string;
    needSummary: string;
    stage: OpportunityStage;
    expectedAmount: number;
    probability: number;
    nextAction?: string;
    nextActionAt?: string;
    ownerName: string;
  }>;
  contracts: Array<{
    id: string;
    code?: string;
    projectName: string;
    contractType: string;
    amount: number;
    startDate: string;
    endDate: string;
    serviceCycle?: string;
    status: ContractStatus;
  }>;
  receivables: Array<{
    id: string;
    code?: string;
    sourceNo: string;
    amount: number;
    dueDate: string;
    invoiceNo?: string;
    invoiceDate?: string;
    settledAmount: number;
    outstandingAmount: number;
    status: ReceivableStatus;
  }>;
  followUps: Array<{
    id: string;
    opportunityId?: string;
    opportunityCode?: string;
    type: FollowUpType;
    subject: string;
    content: string;
    followedAt: string;
    nextAction?: string;
    ownerName: string;
  }>;
  metrics: {
    contractCount: number;
    contractAmount: number;
    outstandingAmount: number;
    settledAmount: number;
  };
};

export type Opportunity = {
  id: string;
  customerId?: string;
  customerName: string;
  code?: string;
  source?: string;
  needSummary: string;
  stage: OpportunityStage;
  expectedAmount: number;
  probability: number;
  nextAction?: string;
  nextActionAt?: string;
  ownerName: string;
  updatedAt: string;
};

export type CreateOpportunityPayload = {
  customerId?: string;
  code?: string;
  source?: string;
  needSummary: string;
  stage?: OpportunityStage;
  expectedAmount?: number;
  probability?: number;
  nextAction?: string;
  nextActionAt?: string;
  ownerName: string;
};

export type AdvanceOpportunityPayload = {
  stage: OpportunityStage;
  nextAction: string;
  nextActionAt: string;
  probability: number;
};

export type QuotePlan = {
  id: string;
  customerId?: string;
  customerName: string;
  opportunityId?: string;
  opportunityCode?: string;
  code?: string;
  serviceScope: string;
  inspectCycle?: string;
  paymentNodes?: string;
  amount: number;
  taxRate?: number;
  netAmount?: number;
  laborBudget?: number;
  materialBudget?: number;
  subcontractBudget?: number;
  travelBudget?: number;
  otherBudget?: number;
  budgetAmount?: number;
  grossMargin?: number;
  grossMarginRate?: number;
  versionNo: number;
  status: QuoteStatus;
  lastApprovalComment?: string;
  lastApproverName?: string;
  lastApprovalAt?: string;
  customerDecision?: QuoteCustomerDecision;
  customerComment?: string;
  customerDecisionBy?: string;
  customerDecidedAt?: string;
  convertedContractId?: string;
  costRequest?: QuoteCostRequest;
  updatedAt: string;
};

export type QuoteCostRequest = {
  id: string;
  quoteId: string;
  opportunityId?: string;
  customerId?: string;
  status: QuoteCostStatus;
  requestedBy: string;
  requestedAt: string;
  projectManager?: string;
  laborCost?: number;
  materialCost?: number;
  subcontractCost?: number;
  travelCost?: number;
  travelTaxRate?: number;
  equipmentCost?: number;
  riskReserve?: number;
  riskReserveTaxRate?: number;
  otherCost?: number;
  otherTaxRate?: number;
  totalCost?: number;
  netTotalCost?: number;
  suggestedPrice?: number;
  costRemark?: string;
  submittedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
  approvalComment?: string;
};

export type CreateQuotePayload = {
  customerId?: string;
  opportunityId?: string;
  code?: string;
  serviceScope: string;
  inspectCycle?: string;
  paymentNodes?: string;
  amount: number;
  taxRate?: number;
  netAmount?: number;
  laborBudget?: number;
  materialBudget?: number;
  subcontractBudget?: number;
  travelBudget?: number;
  otherBudget?: number;
  editorName: string;
};

export type UpdateQuotePayload = {
  serviceScope: string;
  inspectCycle?: string;
  paymentNodes?: string;
  amount: number;
  taxRate?: number;
  laborBudget?: number;
  materialBudget?: number;
  subcontractBudget?: number;
  travelBudget?: number;
  otherBudget?: number;
  revisionNote: string;
  editorName: string;
};

export type ProcessQuoteApprovalPayload = {
  decision: ApprovalDecision;
  comment: string;
  approverName: string;
};

export type RequestQuoteCostPayload = {
  requestedBy: string;
};

export type SubmitQuoteCostPayload = {
  projectManager: string;
  laborCost?: number;
  materialCost?: number;
  subcontractCost?: number;
  travelCost?: number;
  travelTaxRate?: number;
  equipmentCost?: number;
  riskReserve?: number;
  riskReserveTaxRate?: number;
  otherCost?: number;
  otherTaxRate?: number;
  suggestedPrice?: number;
  costRemark?: string;
};

export type ApproveQuoteCostPayload = {
  decision: ApprovalDecision;
  approverName: string;
  comment: string;
};

export type ProcessQuoteCustomerResultPayload = {
  decision: QuoteCustomerDecision;
  comment: string;
  operatorName: string;
};

export type ReceivableItem = {
  receivableCode?: string;
  amount: number;
  dueDate: string;
};

export type ConvertQuotePayload = {
  contractCode?: string;
  projectName?: string;
  contractType?: string;
  startDate?: string;
  endDate?: string;
  serviceCycle?: string;
  receivables: ReceivableItem[];
};

export type QuoteRevision = {
  id: string;
  versionNo: number;
  code?: string;
  serviceScope: string;
  inspectCycle?: string;
  paymentNodes?: string;
  amount: number;
  taxRate?: number;
  laborBudget?: number;
  materialBudget?: number;
  subcontractBudget?: number;
  travelBudget?: number;
  otherBudget?: number;
  budgetAmount?: number;
  grossMargin?: number;
  grossMarginRate?: number;
  status: QuoteStatus;
  revisionNote: string;
  editorName: string;
  revisedAt: string;
};

export type FollowUp = {
  id: string;
  customerId: string;
  customerName: string;
  opportunityId?: string;
  opportunityCode?: string;
  type: FollowUpType;
  subject: string;
  content: string;
  followedAt: string;
  nextAction?: string;
  ownerName: string;
};

export type CreateFollowUpPayload = {
  customerId: string;
  opportunityId?: string;
  type: FollowUpType;
  subject: string;
  content: string;
  followedAt: string;
  nextAction?: string;
  ownerName: string;
};

export type ServiceContract = {
  id: string;
  quoteId?: string;
  customerId: string;
  customerName: string;
  code?: string;
  projectName: string;
  contractType: string;
  amount: number;
  taxRate?: number;
  netAmount?: number;
  startDate: string;
  endDate: string;
  salesOwnerName?: string;
  serviceCycle?: string;
  status: ContractStatus;
};

export type Receivable = {
  id: string;
  customerId: string;
  customerName: string;
  contractId?: string;
  contractCode: string;
  code?: string;
  sourceNo: string;
  amount: number;
  dueDate: string;
  invoiceNo?: string;
  invoiceDate?: string;
  invoiceRequested?: boolean;
  invoiceRequestedBy?: string;
  invoiceRequestedAt?: string;
  invoiceRequestRemark?: string;
  settledAmount: number;
  outstandingAmount: number;
  status: ReceivableStatus;
};

export type QuoteConversionResult = {
  quote: QuotePlan;
  contract?: ServiceContract;
  receivables: Receivable[];
};

export type Renewal = {
  contractId: string;
  customerId: string;
  customerName: string;
  contractCode: string;
  projectName: string;
  amount: number;
  endDate: string;
  daysRemaining: number;
  renewalRisk: "EXPIRED" | "HIGH" | "MEDIUM" | "LOW";
  outstandingAmount: number;
  status: ContractStatus;
};

export type CustomerProfile = {
  customerId: string;
  customerCode: string;
  customerName: string;
  industry: string;
  level: CustomerLevel;
  ownerName: string;
  riskStatus: RiskStatus;
  paymentHabit?: string;
  opportunityCount: number;
  opportunityAmount: number;
  contractCount: number;
  contractAmount: number;
  outstandingAmount: number;
  overdueAmount: number;
  nearestContractEndDate?: string;
};

export type CreateCustomerPayload = {
  code?: string;
  name: string;
  industry: string;
  level: CustomerLevel;
  ownerName: string;
  paymentHabit?: string;
  riskStatus?: RiskStatus;
  riskNote?: string;
  invoice?: {
    title?: string;
    taxNo?: string;
    bankName?: string;
    bankAccount?: string;
    registeredAddress?: string;
    registeredPhone?: string;
  };
  contacts?: Array<{
    name: string;
    title?: string;
    phone?: string;
    email?: string;
    primaryContact: boolean;
  }>;
  sites?: Array<{
    name: string;
    address: string;
  }>;
};

export type UpdateCustomerPayload = Omit<CreateCustomerPayload, "code">;

export function listCustomers() {
  return request<CustomerSummary[]>({
    method: "GET",
    url: "/crm/customers",
  });
}

export function getCustomer(id: string) {
  return request<CustomerDetail>({
    method: "GET",
    url: `/crm/customers/${id}`,
  });
}

export function createCustomer(payload: CreateCustomerPayload) {
  return request<CustomerDetail>({
    method: "POST",
    url: "/crm/customers",
    data: payload,
  });
}

export function updateCustomer(id: string, payload: UpdateCustomerPayload) {
  return request<CustomerDetail>({
    method: "PUT",
    url: `/crm/customers/${id}`,
    data: payload,
  });
}

export function transferCustomerOwner(id: string, ownerName: string) {
  return request<void>({
    method: "POST",
    url: `/crm/customers/${id}/transfer-owner`,
    data: { ownerName },
  });
}

export function deleteCustomer(id: string) {
  return request<void>({ method: "DELETE", url: "/crm/customers/" + id });
}

export function listOpportunities() {
  return request<Opportunity[]>({ method: "GET", url: "/crm/opportunities" });
}


export function getOpportunity(id: string) {
  return request<Opportunity>({ method: "GET", url: "/crm/opportunities/" + id });
}

export function updateOpportunity(id: string, payload: CreateOpportunityPayload) {
  return request<Opportunity>({ method: "PUT", url: "/crm/opportunities/" + id, data: payload });
}
export function createOpportunity(payload: CreateOpportunityPayload) {
  return request<Opportunity>({ method: "POST", url: "/crm/opportunities", data: payload });
}

export function advanceOpportunity(id: string, payload: AdvanceOpportunityPayload) {
  return request<Opportunity>({ method: "POST", url: `/crm/opportunities/${id}/advance`, data: payload });
}

export function listQuotes() {
  return request<QuotePlan[]>({ method: "GET", url: "/crm/quotes" });
}

export function createQuote(payload: CreateQuotePayload) {
  return request<QuotePlan>({ method: "POST", url: "/crm/quotes", data: payload });
}

export function updateQuote(id: string, payload: UpdateQuotePayload) {
  return request<QuotePlan>({ method: "PUT", url: `/crm/quotes/${id}`, data: payload });
}

export function listQuoteRevisions(id: string) {
  return request<QuoteRevision[]>({ method: "GET", url: `/crm/quotes/${id}/revisions` });
}

export function requestQuoteCost(id: string, payload: RequestQuoteCostPayload) {
  return request<QuoteCostRequest>({ method: "POST", url: `/crm/quotes/${id}/cost-requests`, data: payload });
}

export function listQuoteCostRequests(id: string) {
  return request<QuoteCostRequest[]>({ method: "GET", url: `/crm/quotes/${id}/cost-requests` });
}

export function submitQuoteCost(id: string, payload: SubmitQuoteCostPayload) {
  return request<QuoteCostRequest>({ method: "PUT", url: `/crm/quote-cost-requests/${id}`, data: payload });
}

export function approveQuoteCost(id: string, payload: ApproveQuoteCostPayload) {
  return request<QuoteCostRequest>({ method: "POST", url: `/crm/quote-cost-requests/${id}/approval`, data: payload });
}

export function submitQuote(id: string) {
  return request<QuotePlan>({ method: "POST", url: `/crm/quotes/${id}/submit` });
}

export function processQuoteApproval(id: string, payload: ProcessQuoteApprovalPayload) {
  return request<QuotePlan>({ method: "POST", url: `/crm/quotes/${id}/approval`, data: payload });
}

export function processQuoteCustomerResult(id: string, payload: ProcessQuoteCustomerResultPayload) {
  return request<QuotePlan>({ method: "POST", url: `/crm/quotes/${id}/customer-result`, data: payload });
}

export function convertQuote(id: string, payload: ConvertQuotePayload) {
  return request<QuoteConversionResult>({ method: "POST", url: `/crm/quotes/${id}/convert`, data: payload });
}

export function listContracts() {
  return request<ServiceContract[]>({ method: "GET", url: "/crm/contracts" });
}

export function getContract(id: string) {
  return request<ServiceContract>({ method: "GET", url: "/crm/contracts/" + id });
}

export function approveContract(id: string, payload: ApprovalActionPayload) {
  return request<ServiceContract>({ method: "POST", url: `/crm/contracts/${id}/approval`, data: payload });
}

export function submitSignedDocumentApproval(id: string, payload: ApprovalActionPayload) {
  return request<ServiceContract>({ method: "POST", url: `/crm/contracts/${id}/signed-doc-approval`, data: payload });
}

export function listFollowUps() {
  return request<FollowUp[]>({ method: "GET", url: "/crm/follow-ups" });
}

export function createFollowUp(payload: CreateFollowUpPayload) {
  return request<FollowUp>({ method: "POST", url: "/crm/follow-ups", data: payload });
}

export function listRenewals() {
  return request<Renewal[]>({ method: "GET", url: "/crm/renewals" });
}

export function listReceivables() {
  return request<Receivable[]>({ method: "GET", url: "/crm/receivables" });
}

export function getQuote(id: string) {
  return request<QuotePlan>({ method: "GET", url: "/crm/quotes/" + id });
}

export function deleteOpportunity(id: string) {
  return request<void>({ method: "DELETE", url: "/crm/opportunities/" + id });
}

export function deleteQuote(id: string) {
  return request<void>({ method: "DELETE", url: "/crm/quotes/" + id });
}

export function deleteContract(id: string) {
  return request<void>({ method: "DELETE", url: "/crm/contracts/" + id });
}

export function deleteFollowUp(id: string) {
  return request<void>({ method: "DELETE", url: "/crm/follow-ups/" + id });
}

export function registerReceivableInvoice(id: string, payload: { invoiceNo: string; invoiceDate: string }) {
  return request<Receivable>({ method: "POST", url: `/crm/receivables/${id}/invoice`, data: payload });
}

export function applyReceivableInvoice(id: string, payload: { applicantName: string; remark?: string }) {
  return request<Receivable>({ method: "POST", url: `/crm/receivables/${id}/invoice-request`, data: payload });
}

export function recordReceivableReceipt(id: string, payload: { amount: number; receivedDate: string; referenceNo: string; recorderName: string }) {
  return request<Receivable>({ method: "POST", url: `/crm/receivables/${id}/receipts`, data: payload });
}

export function listCustomerProfiles() {
  return request<CustomerProfile[]>({ method: "GET", url: "/crm/profiles" });
}

// ============ CrmAttachment ============
export type CrmAttachment = {
  id: string;
  entityType: string;
  entityId: string;
  attachmentType?: string;
  fileName: string;
  fileSize: number;
  mimeType?: string;
  uploadedAt: string;
  uploadedBy: string;
};

export function listAttachments(entityType: string, entityId: string) {
  return request<CrmAttachment[]>({ method: "GET", url: "/crm/attachments", params: { entityType, entityId } });
}

export function uploadAttachment(entityType: string, entityId: string, attachmentType: string | undefined, file: File) {
  const formData = new FormData();
  formData.append("entityType", entityType);
  formData.append("entityId", entityId);
  if (attachmentType) formData.append("attachmentType", attachmentType);
  formData.append("file", file);
  return request<CrmAttachment>({ method: "POST", url: "/crm/attachments/upload", data: formData, headers: { "Content-Type": "multipart/form-data" } });
}

export function deleteAttachment(id: string) {
  return request<void>({ method: "DELETE", url: "/crm/attachments/" + id });
}

export function getAttachmentDownloadUrl(id: string): string {
  return "/api/crm/attachments/" + id + "/download";
}

export async function downloadAttachment(item: CrmAttachment) {
  const response = await http.get<Blob>(`/crm/attachments/${item.id}/download`, { responseType: "blob" });
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = item.fileName;
  anchor.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export async function openAttachment(item: CrmAttachment) {
  const response = await http.get<Blob>(`/crm/attachments/${item.id}/download`, { responseType: "blob" });
  const type = response.data.type || item.mimeType || "";
  const url = URL.createObjectURL(response.data);
  if (type.startsWith("image/") || type === "application/pdf") {
    window.open(url, "_blank", "noopener,noreferrer");
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    return;
  }
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = item.fileName;
  anchor.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export type UpdateReceivablePayload = {
  sourceNo?: string;
  amount?: number;
  dueDate?: string;
};

export function updateReceivable(id: string, payload: UpdateReceivablePayload) {
  return request<Receivable>({
    method: "PUT",
    url: `/crm/receivables/${id}`,
    data: payload,
  });
}

export type UpdateContractPayload = {
  projectName?: string;
  contractType?: string;
  amount?: number;
  taxRate?: number;
  startDate?: string;
  endDate?: string;
  serviceCycle?: string;
};

export function updateContract(id: string, payload: UpdateContractPayload) {
  return request<ServiceContract>({
    method: "PUT",
    url: `/crm/contracts/${id}`,
    data: payload,
  });
}

export type CreateContractChangePayload = {
  changeData: string;
  reason: string;
  requestedBy: string;
};

export type ContractChangeResponse = {
  id: string;
  contractId: string;
  changeData: string;
  reason: string;
  status: string;
  requestedBy: string;
  requestedAt: string;
  approvedBy: string;
  approvedAt: string;
  approvalComment: string;
  createdAt: string;
};

export type ApprovalActionPayload = {
  operatorName: string;
  comment: string;
};

export function createContractChange(contractId: string, payload: CreateContractChangePayload) {
  return request<ContractChangeResponse>({
    method: "POST", url: `/crm/contracts/${contractId}/changes`, data: payload,
  });
}

export function approveContractChange(changeId: string, payload: ApprovalActionPayload) {
  return request<ContractChangeResponse>({
    method: "POST", url: `/crm/contract-changes/${changeId}/approve`, data: payload,
  });
}

export function rejectContractChange(changeId: string, payload: ApprovalActionPayload) {
  return request<ContractChangeResponse>({
    method: "POST", url: `/crm/contract-changes/${changeId}/reject`, data: payload,
  });
}

export function listContractChanges(contractId: string) {
  return request<ContractChangeResponse[]>({
    method: "GET", url: `/crm/contracts/${contractId}/changes`,
  });
}


// ====== Excel Export ======
export async function exportCustomersExcel() {
  const { http } = await import("./http");
  const response = await http.get<Blob>("/crm/export/customers", { responseType: "blob" });
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = url; anchor.download = "crm-customers.xlsx";
  anchor.click(); URL.revokeObjectURL(url);
}

export async function exportContractsExcel() {
  const { http } = await import("./http");
  const response = await http.get<Blob>("/crm/export/contracts", { responseType: "blob" });
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = url; anchor.download = "crm-contracts.xlsx";
  anchor.click(); URL.revokeObjectURL(url);
}

// ====== Import ======
export function importCustomersExcel(file: File) {
  const form = new FormData(); form.append("file", file);
  return request<{ success: number; fail: number; errors: string[] }>({ method: "POST", url: "/crm/customers/import", data: form });
}

// ====== Batch Delete ======
export function batchDeleteCustomers(ids: string[]) {
  return request<void>({ method: "POST", url: "/crm/customers/batch-delete", data: ids });
}

// ====== Contract Renewal ======
export function renewContract(id: string) {
  return request<QuotePlan>({ method: "POST", url: `/crm/contracts/${id}/renew` });
}
