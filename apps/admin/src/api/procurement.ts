import { http, request, requestAllPages } from "./http";
import { type PageResponse } from "./system";

export type SupplierRiskStatus = "NORMAL" | "WATCHLIST" | "BLOCKED";
export type PurchaseRequestStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "APPROVED"
  | "ORDERED"
  | "RECEIVED"
  | "CANCELLED";
export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";
export type PurchaseOrderStatus =
  | "DRAFT"
  | "ORDERED"
  | "PARTIAL_RECEIVED"
  | "RECEIVED"
  | "CLOSED"
  | "CANCELLED";
export type ProcurementCostType = "PROJECT" | "DEPARTMENT";

export type ProcurementMaterial = {
  id: string;
  code?: string;
  name: string;
  model?: string;
  category: string;
  stockQty: number;
  safetyQty: number;
  unitCost: number;
  lowStock: boolean;
};

export type CreateProcurementMaterialPayload = {
  code?: string;
  name: string;
  model?: string;
  category: string;
  safetyQty?: number;
  unitCost?: number;
};

export type UpdateProcurementMaterialPayload = Omit<
  CreateProcurementMaterialPayload,
  "code"
>;

export type MaterialCategory = {
  id: string;
  name: string;
  builtIn: boolean;
};

export type SupplierCategory = {
  id: string;
  name: string;
  description?: string;
  sortOrder: number;
  enabled: boolean;
  builtIn: boolean;
  supplierCount: number;
};

export type MaterialDeletionResult = {
  status: "DELETED" | "PENDING_APPROVAL";
  message: string;
};

export type ProcurementCostTargetOption = {
  id: string;
  code?: string;
  name: string;
};

export type ProcurementCostTargetOptions = {
  projects: ProcurementCostTargetOption[];
  departments: ProcurementCostTargetOption[];
};

export type Supplier = {
  id: string;
  code?: string;
  name: string;
  category?: string;
  contactName?: string;
  phone?: string;
  settlementTerms?: string;
  legalRepresentative?: string;
  unifiedSocialCreditCode?: string;
  registeredCapital?: string;
  registeredAddress?: string;
  businessScope?: string;
  licenseValidTo?: string;
  qualificationValidTo?: string;
  taxpayerType?: string;
  bankName?: string;
  bankAccount?: string;
  admissionStatus?: string;
  admissionSubmittedAt?: string;
  admissionReviewedAt?: string;
  admissionReviewerName?: string;
  admissionReviewComment?: string;
  remark?: string;
  riskStatus: SupplierRiskStatus;
  contractedAmount: number;
  payableAmount: number;
  paidAmount: number;
  outstandingAmount: number;
};

export type SupplierPortalAccount = {
  id: string;
  supplierId: string;
  supplierCode?: string;
  supplierName?: string;
  supplierAdmissionStatus?: string;
  email: string;
  phone?: string;
  contactName: string;
  status: "PENDING_REVIEW" | "ACTIVE" | "REJECTED" | "SUSPENDED";
  mustChangePassword: boolean;
  reviewComment?: string;
  reviewedByName?: string;
  reviewedAt?: string;
  lastLoginAt?: string;
  createdAt: string;
};

export type SupplierPortalDocument = {
  id: string;
  supplierId: string;
  documentType: string;
  documentName: string;
  contentType?: string;
  sizeBytes: number;
  validTo?: string;
  reviewStatus: "PENDING" | "APPROVED" | "REJECTED";
  reviewComment?: string;
  reviewedByName?: string;
  reviewedAt?: string;
  createdAt: string;
};

export type PurchaseRequest = {
  id: string;
  batchId: string;
  batchCode: string;
  batchName?: string;
  lineNo: number;
  code?: string;
  requesterName: string;
  applicantName?: string;
  partId?: string;
  partName: string;
  materialName?: string;
  materialSpec?: string;
  unit?: string;
  unitPrice?: number;
  taxRate?: number;
  totalAmount?: number;
  requiredDate?: string;
  description?: string;
  projectId?: string;
  departmentId?: string;
  quantity: number;
  expectedDate?: string;
  reason?: string;
  costType: ProcurementCostType;
  costTargetId: string;
  costTargetCode: string;
  costTargetName: string;
  status: PurchaseRequestStatus;
  approvalStatus: ApprovalStatus;
  lastApprovalComment?: string;
  lastApproverName?: string;
  lastApprovalAt?: string;
  approvalLevel?: string;
};

export type PurchaseOrder = {
  id: string;
  code?: string;
  orderedAt?: string;
  createdAt?: string;
  orderItems?: any[];
  totalAmount?: number;
  amount?: number;
  supplierId: string;
  supplierName?: string;
  requestId?: string;
  requestCode?: string;
  partId?: string;
  partName: string;
  orderedQty: number;
  receivedQty: number;
  unitPrice: number;
  taxRate?: number;
  orderAmount: number;
  expectedDeliveryDate?: string;
  costType: ProcurementCostType;
  costTargetId: string;
  costTargetCode: string;
  costTargetName: string;
  status: PurchaseOrderStatus;
  approvalStatus: ApprovalStatus;
  approvalComment?: string;
  approverName?: string;
  approvedAt?: string;
  inquiryId?: string;
  contractId?: string;
  currency?: string;
  freightAmount?: number;
  sourceReason?: string;
  responsibleName?: string;
  submittedAt?: string;
  closedAt?: string;
  orderVersion?: number;
  inquiryCode?: string;
  contractNo?: string;
  contractName?: string;
  contractPaymentTerms?: string;
  contractStartDate?: string;
  contractEndDate?: string;
  contractStatus?: string;
  contractSourceType?: string;
  contractAcknowledged?: boolean;
  contractAcknowledgedByName?: string;
};

export type ProcurementShipment = {
  id: string;
  orderId: string;
  orderCode?: string;
  supplierId: string;
  supplierName?: string;
  deliveryNo?: string;
  carrier?: string;
  expectedArrival?: string;
  remark?: string;
  status: string;
  createdByName?: string;
  createdAt: string;
  reviewComment?: string;
  reviewedBy?: string;
  reviewedAt?: string;
};

export type CreateSupplierPayload = {
  code?: string;
  name: string;
  category: string;
  contactName?: string;
  phone?: string;
  settlementTerms?: string;
  legalRepresentative?: string;
  unifiedSocialCreditCode?: string;
  registeredCapital?: string;
  registeredAddress?: string;
  businessScope?: string;
  licenseValidTo?: string;
  qualificationValidTo?: string;
  taxpayerType?: string;
  bankName?: string;
  bankAccount?: string;
  admissionStatus?: string;
  remark?: string;
  riskStatus?: SupplierRiskStatus;
};

export type CreatePurchaseRequestPayload = {
  code?: string;
  requesterName?: string;
  applicantName?: string;
  materialName?: string;
  materialSpec?: string;
  unit?: string;
  unitPrice?: number;
  taxRate?: number;
  totalAmount?: number;
  requiredDate?: string;
  description?: string;
  partId?: string;
  partName?: string;
  quantity: number;
  expectedDate?: string;
  reason?: string;
  costType: ProcurementCostType;
  projectId?: string;
  departmentId?: string;
};

export type ImportPurchaseRequestBatchResult = {
  batchId: string;
  batchCode: string;
  batchName: string;
  totalLines: number;
  totalAmount: number;
  items: PurchaseRequest[];
};

export type CreatePurchaseOrderPayload = {
  code?: string;
  supplierId: string;
  requestId: string;
  unitPrice: number;
  taxRate?: number;
  expectedDeliveryDate?: string;
  orderedQty?: number;
  inquiryId?: string;
  contractId?: string;
  currency?: string;
  freightAmount?: number;
  sourceReason?: string;
  generateContract?: boolean;
  contractNo?: string;
  contractName?: string;
  paymentTerms?: string;
  contractStartDate?: string;
  contractEndDate?: string;
};

export type GoodsReceipt = {
  id: string;
  code?: string;
  orderId: string;
  orderCode: string;
  partId: string;
  partName: string;
  quantity: number;
  unitPrice: number;
  taxRate?: number;
  amount: number;
  receivedDate: string;
  payableDueDate: string;
  deliveryNo: string;
  receiverName: string;
  costType: ProcurementCostType;
  costTargetId: string;
  costTargetCode: string;
  costTargetName: string;
  inspectionStatus?: "PENDING" | "PASSED" | "PARTIAL" | "REJECTED";
  qualifiedQty?: number;
  rejectedQty?: number;
  inspectorName?: string;
  inspectionComment?: string;
  inspectedAt?: string;
  clientRequestId?: string;
  asnNo?: string;
  appealStatus?: "NONE" | "PENDING" | "DISMISSED" | "REOPENED";
  appealReason?: string;
  appealedAt?: string;
  appealResolution?: "DISMISSED" | "REOPENED";
  appealReviewComment?: string;
  appealReviewedBy?: string;
  appealReviewedAt?: string;
};

export type GoodsReceiptAppeal = {
  id: string;
  code?: string;
  orderId: string;
  orderCode?: string;
  supplierId?: string;
  supplierName?: string;
  partName?: string;
  quantity: number;
  qualifiedQty?: number;
  rejectedQty?: number;
  inspectionStatus?: string;
  inspectorName?: string;
  inspectionComment?: string;
  inspectedAt?: string;
  receivedDate?: string;
  appealStatus?: string;
  appealReason?: string;
  appealedAt?: string;
  appealResolution?: string;
  appealReviewComment?: string;
  appealReviewedBy?: string;
  appealReviewedAt?: string;
};

export type OrderDocument = {
  id: string;
  orderId: string;
  orderCode?: string;
  fileName: string;
  contentType?: string;
  sizeBytes: number;
  docType?: string;
  uploadedBy?: string;
  uploadedAt?: string;
};

export type PayableStatus = "PENDING" | "PARTIAL_PAID" | "PAID" | "CANCELLED";

export type ProcurementPayable = {
  id: string;
  code?: string;
  supplierId: string;
  supplierName: string;
  orderId: string;
  orderCode: string;
  receiptId: string;
  amount: number;
  taxRate?: number;
  paidAmount: number;
  outstandingAmount: number;
  dueDate: string;
  paidAt?: string;
  paymentNote?: string;
  paymentReceiptFileName?: string;
  paymentReceiptContentType?: string;
  paymentReceiptSizeBytes?: number;
  paymentReceiptUploadedBy?: string;
  paymentReceiptUploadedAt?: string;
  costType: ProcurementCostType;
  costTargetId: string;
  costTargetCode: string;
  costTargetName: string;
  status: PayableStatus;
};

export type ProcurementCostAllocation = {
  id: string;
  orderId: string;
  orderCode: string;
  receiptId: string;
  receiptCode: string;
  costType: ProcurementCostType;
  costTargetId: string;
  costTargetCode: string;
  costTargetName: string;
  partName: string;
  amount: number;
  incurredDate: string;
};

export type ProcurementMatching = {
  orderId: string;
  orderCode?: string;
  supplierName?: string;
  partName: string;
  orderedQty: number;
  receivedQty: number;
  orderAmount: number;
  receiptAmount: number;
  payableAmount: number;
  invoiceAmount: number;
  matchedInvoiceAmount: number;
  paidAmount: number;
  matchStatus: string;
  riskMessage: string;
};

export type ReceivePurchaseOrderResult = {
  order: PurchaseOrder;
  receipt: GoodsReceipt;
  payable: ProcurementPayable | null;
  costAllocation: ProcurementCostAllocation | null;
  currentStockQty: number;
};
export type ProcurementInquiryRequestLine = {
  requestId: string;
  requestCode?: string;
  batchCode?: string;
  partId?: string;
  partName?: string;
  requestedQty: number;
  costTargetName?: string;
  expectedDate?: string;
};
export type ProcurementInquiry = {
  id: string;
  code: string;
  requestId: string;
  requestIds?: string[];
  requestCount?: number;
  materialCount?: number;
  totalRequestedQty?: number;
  requestLines?: ProcurementInquiryRequestLine[];
  partName?: string;
  title: string;
  deadline?: string;
  status: "OPEN" | "AWARDED" | "CLOSED";
  createdByName: string;
  sourcingMethod: string;
  minQuoteCount: number;
  exceptionReason?: string;
  selectedQuoteId?: string;
  selectionReason?: string;
  selectedByName?: string;
  selectedAt?: string;
  invitations?: ProcurementInquiryInvitation[];
  quotes: SupplierQuotation[];
};
export type ProcurementInquiryInvitation = {
  id: string;
  supplierId: string;
  supplierName?: string;
  status: "INVITED" | "VIEWED" | "RESPONDED" | "DECLINED";
  invitedByName?: string;
  invitedAt: string;
  viewedAt?: string;
  respondedAt?: string;
  deliveryStatus?: "PENDING" | "DELIVERED" | "FAILED";
  deliveryAttemptCount?: number;
  lastDeliveryAt?: string;
  deliveryError?: string;
  declinedAt?: string;
  declineReason?: string;
};

export type InviteSuppliersResult = ProcurementInquiry & {
  registrationCodes: Record<string, string>;
};

export type SupplierChangeRequest = {
  id: string;
  supplierId: string;
  changeType: string;
  proposedName?: string;
  proposedCreditCode?: string;
  proposedBankName?: string;
  proposedBankAccount?: string;
  proposedSettlementTerms?: string;
  reason: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  requestedByName?: string;
  requestSource?: string;
  reviewedByName?: string;
  reviewComment?: string;
  reviewedAt?: string;
  createdAt: string;
};

export function listSupplierChangeRequests() {
  return request<SupplierChangeRequest[]>({
    method: "GET",
    url: "/procurement/governance/supplier-changes",
  });
}

export function reviewSupplierChangeRequest(
  id: string,
  data: { decision: "APPROVED" | "REJECTED"; comment?: string },
) {
  return request<SupplierChangeRequest>({
    method: "POST",
    url: `/procurement/governance/supplier-changes/${id}/review`,
    data,
  });
}

export type ProcurementPurchasePoolItem = {
  requestId: string;
  requestCode: string;
  partName?: string;
  batchId: string;
  batchCode: string;
  batchName?: string;
  lineNo: number;
  requesterName: string;
  requestedQuantity: number;
  orderedQuantity: number;
  remainingQuantity: number;
  estimatedUnitPrice: number;
  estimatedAmount: number;
  taxRate: number;
  expectedDate?: string;
  costType: ProcurementCostType;
  costTargetId?: string;
  costTargetCode: string;
  costTargetName: string;
  reason?: string;
  approvedAt?: string;
};
export type ProcurementPurchasePoolGroup = {
  groupKey: string;
  partId?: string;
  partCode?: string;
  partName: string;
  requestCount: number;
  costTargetCount: number;
  totalRemainingQuantity: number;
  totalEstimatedAmount: number;
  earliestExpectedDate?: string;
  items: ProcurementPurchasePoolItem[];
};
export type ProcurementPurchasePool = {
  totalGroups: number;
  totalRequests: number;
  totalRemainingQuantity: number;
  totalEstimatedAmount: number;
  groups: ProcurementPurchasePoolGroup[];
};
export type SupplierQuotationLine = {
  requestId: string;
  requestCode?: string;
  partName?: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
  deliveryDate?: string;
  remark?: string;
  amount: number;
};
export type SupplierQuotation = {
  id: string;
  supplierId: string;
  supplierName: string;
  unitPrice: number;
  taxRate: number;
  deliveryDate?: string;
  paymentTerms?: string;
  remark?: string;
  selected: boolean;
  currency: string;
  freightAmount: number;
  otherCostAmount: number;
  technicalScore: number;
  commercialScore: number;
  totalScore: number;
  validUntil?: string;
  submissionSource: "INTERNAL_ENTRY" | "SUPPLIER_PORTAL";
  submissionStatus: "DRAFT" | "SUBMITTED" | "WITHDRAWN";
  versionNo: number;
  submittedByType: "INTERNAL_USER" | "SUPPLIER_ACCOUNT";
  submittedById?: string;
  submittedByName?: string;
  submittedAt?: string;
  confirmed: boolean;
  confirmedAt?: string;
  lines: SupplierQuotationLine[];
  materialAmount: number;
  totalAmount: number;
};
export type SupplierQuoteAttachment = {
  id: string;
  quoteId: string;
  attachmentType: string;
  fileName: string;
  contentType?: string;
  sizeBytes: number;
  sha256: string;
  createdAt: string;
};
export type InquiryClarification = {
  id: string;
  inquiryId: string;
  supplierId: string;
  supplierName?: string;
  question: string;
  askedAt: string;
  answer?: string;
  answeredByName?: string;
  answeredAt?: string;
  status: "OPEN" | "ANSWERED";
};
export type ProcurementReturnOrder = {
  id: string;
  code: string;
  orderId: string;
  receiptId: string;
  supplierId: string;
  quantity: number;
  amount: number;
  reason: string;
  returnDate: string;
  handlerName: string;
  status: string;
  replacementQty: number;
  creditAmount: number;
  claimAmount: number;
  correctiveAction?: string;
  supplierResponse?: string;
  completedAt?: string;
};
export type InvoiceSubmission = {
  id: string;
  orderId: string;
  orderCode?: string;
  supplierId: string;
  supplierName?: string;
  invoiceNo: string;
  amount: number;
  taxRate: number;
  invoiceDate: string;
  remark?: string;
  fileName: string;
  contentType?: string;
  sizeBytes: number;
  status: "PENDING" | "APPROVED" | "REJECTED";
  reviewComment?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  createdAt: string;
};

export type SupplierInvoice = {
  id: string;
  code: string;
  invoiceNo: string;
  orderId: string;
  supplierId: string;
  payableId?: string;
  receiptId?: string;
  amount: number;
  matchedAmount: number;
  taxRate: number;
  invoiceDate: string;
  status: string;
  matchStatus: "MATCHED" | "MISMATCH";
  differenceAmount: number;
  approvalStatus: string;
  verificationStatus: string;
  attachmentDocumentId?: string;
  remark?: string;
};

export function listProcurementMaterials() {
  return requestAllPages<ProcurementMaterial>({
    method: "GET",
    url: "/procurement/materials",
  });
}

export function createProcurementMaterial(
  payload: CreateProcurementMaterialPayload,
) {
  return request<ProcurementMaterial>({
    method: "POST",
    url: "/procurement/materials",
    data: payload,
  });
}

export function listMaterialCategories() {
  return request<MaterialCategory[]>({
    method: "GET",
    url: "/procurement/materials/categories",
  });
}

export function createMaterialCategory(name: string) {
  return request<MaterialCategory>({
    method: "POST",
    url: "/procurement/materials/categories",
    data: { name },
  });
}

export function updateProcurementMaterial(
  id: string,
  payload: UpdateProcurementMaterialPayload,
) {
  return request<ProcurementMaterial>({
    method: "PUT",
    url: `/procurement/materials/${id}`,
    data: payload,
  });
}

export function deleteProcurementMaterial(id: string) {
  return request<MaterialDeletionResult>({
    method: "DELETE",
    url: `/procurement/materials/${id}`,
  });
}

export function listSuppliers(page?: number, size?: number) {
  return request<PageResponse<Supplier>>({
    method: "GET",
    url: "/procurement/suppliers",
    params: { page, size },
  });
}

export function createSupplier(payload: CreateSupplierPayload) {
  return request<Supplier>({
    method: "POST",
    url: "/procurement/suppliers",
    data: payload,
  });
}

export function updateSupplier(id: string, payload: CreateSupplierPayload) {
  return request<Supplier>({
    method: "PUT",
    url: `/procurement/suppliers/${id}`,
    data: payload,
  });
}
export function listSupplierCategories() {
  return request<SupplierCategory[]>({
    method: "GET",
    url: "/procurement/supplier-categories",
  });
}
export function createSupplierCategory(payload: {
  name: string;
  description?: string;
  sortOrder?: number;
  enabled?: boolean;
}) {
  return request<SupplierCategory>({
    method: "POST",
    url: "/procurement/supplier-categories",
    data: payload,
  });
}
export function updateSupplierCategory(
  id: string,
  payload: {
    name: string;
    description?: string;
    sortOrder?: number;
    enabled?: boolean;
  },
) {
  return request<SupplierCategory>({
    method: "PUT",
    url: `/procurement/supplier-categories/${id}`,
    data: payload,
  });
}
export function listSupplierPortalAccounts() {
  return request<SupplierPortalAccount[]>({
    method: "GET",
    url: "/procurement/supplier-portal/accounts",
  });
}
export function openSupplierPortalAccount(
  supplierId: string,
  payload: { email: string; phone?: string; contactName: string },
) {
  return request<{ temporaryPassword: string; account: SupplierPortalAccount }>(
    {
      method: "POST",
      url: `/procurement/supplier-portal/suppliers/${supplierId}/account`,
      data: payload,
    },
  );
}
export function reviewSupplierPortalAccount(
  id: string,
  decision: "ACTIVE" | "REJECTED",
  comment?: string,
) {
  return request<SupplierPortalAccount>({
    method: "POST",
    url: `/procurement/supplier-portal/accounts/${id}/review`,
    data: { decision, comment },
  });
}
export function updateSupplierPortalAccountStatus(
  id: string,
  status: "ACTIVE" | "SUSPENDED",
  comment?: string,
) {
  return request<SupplierPortalAccount>({
    method: "POST",
    url: `/procurement/supplier-portal/accounts/${id}/status`,
    data: { status, comment },
  });
}
export function resetSupplierPortalPassword(id: string) {
  return request<{ temporaryPassword: string; account: SupplierPortalAccount }>(
    {
      method: "POST",
      url: `/procurement/supplier-portal/accounts/${id}/reset-password`,
    },
  );
}
export function listSupplierPortalDocuments(supplierId: string) {
  return request<SupplierPortalDocument[]>({
    method: "GET",
    url: `/procurement/supplier-portal/suppliers/${supplierId}/documents`,
  });
}
export function reviewSupplierPortalDocument(
  id: string,
  decision: "APPROVED" | "REJECTED",
  comment?: string,
) {
  return request<SupplierPortalDocument>({
    method: "POST",
    url: `/procurement/supplier-portal/documents/${id}/review`,
    data: { decision, comment },
  });
}

export function reviewSupplierAdmission(
  id: string,
  payload: { decision: "APPROVED" | "REJECTED"; comment?: string },
) {
  return request<Supplier>({
    method: "POST",
    url: `/procurement/suppliers/${id}/admission/review`,
    data: payload,
  });
}

export function listProcurementCostTargets() {
  return request<ProcurementCostTargetOptions>({
    method: "GET",
    url: "/procurement/cost-targets",
  });
}

export function listProcurementCostAllocations() {
  return request<ProcurementCostAllocation[]>({
    method: "GET",
    url: "/procurement/cost-allocations",
  });
}

export function listProcurementMatching() {
  return request<ProcurementMatching[]>({
    method: "GET",
    url: "/procurement/matching",
  });
}

export function listPurchaseRequests(params?: {
  status?: PurchaseRequestStatus;
  approvalStatus?: ApprovalStatus;
  costType?: ProcurementCostType;
  search?: string;
  page?: number;
  size?: number;
}) {
  return request<PageResponse<PurchaseRequest>>({
    method: "GET",
    url: "/procurement/requests",
    params,
  });
}

export function createPurchaseRequest(payload: CreatePurchaseRequestPayload) {
  return request<PurchaseRequest>({
    method: "POST",
    url: "/procurement/requests",
    data: payload,
  });
}

export function importPurchaseRequestBatch(data: {
  file: File;
  batchName: string;
  costType: ProcurementCostType;
  projectId?: string;
  departmentId?: string;
  sharedReason?: string;
}) {
  const form = new FormData();
  form.append("file", data.file);
  form.append("batchName", data.batchName);
  form.append("costType", data.costType);
  if (data.projectId) form.append("projectId", data.projectId);
  if (data.departmentId) form.append("departmentId", data.departmentId);
  if (data.sharedReason) form.append("sharedReason", data.sharedReason);
  return request<ImportPurchaseRequestBatchResult>({
    method: "POST",
    url: "/procurement/requests/import",
    data: form,
  });
}

export function processPurchaseRequestApproval(
  id: string,
  payload: { decision: ApprovalStatus; comment: string; approverName: string },
) {
  return request<PurchaseRequest>({
    method: "POST",
    url: `/procurement/requests/${id}/approval`,
    data: payload,
  });
}

export function processPurchaseRequestBatchApproval(
  batchId: string,
  payload: { decision: ApprovalStatus; comment: string; approverName: string },
) {
  return request<PurchaseRequest[]>({
    method: "POST",
    url: `/procurement/request-batches/${batchId}/approval`,
    data: payload,
  });
}

export function updatePurchaseRequest(
  id: string,
  payload: CreatePurchaseRequestPayload,
) {
  return request<PurchaseRequest>({
    method: "PUT",
    url: `/procurement/requests/${id}`,
    data: payload,
  });
}

export function listPurchaseOrders(params?: {
  status?: PurchaseOrderStatus;
  costType?: ProcurementCostType;
  projectId?: string;
  search?: string;
  page?: number;
  size?: number;
}) {
  return request<PageResponse<PurchaseOrder>>({
    method: "GET",
    url: "/procurement/orders",
    params,
  });
}

export function createPurchaseOrder(payload: CreatePurchaseOrderPayload) {
  return request<PurchaseOrder>({
    method: "POST",
    url: "/procurement/orders",
    data: payload,
  });
}

export function listOrderDocuments(orderId: string) {
  return request<OrderDocument[]>({
    method: "GET",
    url: `/procurement/orders/${orderId}/documents`,
  });
}

export async function uploadOrderDocument(
  orderId: string,
  file: File,
  docType?: string,
) {
  const form = new FormData();
  form.append("file", file);
  if (docType) {
    form.append("docType", docType);
  }
  return request<OrderDocument>({
    method: "POST",
    url: `/procurement/orders/${orderId}/documents`,
    data: form,
  });
}

export async function downloadOrderDocument(doc: OrderDocument) {
  const response = await http.get<Blob>(
    `/procurement/orders/${doc.orderId}/documents/${doc.id}/download`,
    { responseType: "blob" },
  );
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = doc.fileName;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function deleteOrderDocument(orderId: string, docId: string) {
  return request<void>({
    method: "DELETE",
    url: `/procurement/orders/${orderId}/documents/${docId}`,
  });
}

export function listOrderShipments(orderId: string) {
  return request<ProcurementShipment[]>({
    method: "GET",
    url: `/procurement/orders/${orderId}/shipments`,
  });
}

export function confirmOrderShipment(
  orderId: string,
  shipmentId: string,
  data: { action: "CONFIRMED" | "REJECTED"; comment?: string },
) {
  return request<ProcurementShipment>({
    method: "POST",
    url: `/procurement/orders/${orderId}/shipments/${shipmentId}/confirm`,
    data,
  });
}

export function cancelPurchaseOrder(id: string) {
  return request<PurchaseOrder>({
    method: "POST",
    url: `/procurement/orders/${id}/cancel`,
  });
}

export function closePurchaseOrder(id: string) {
  return request<PurchaseOrder>({
    method: "POST",
    url: `/procurement/orders/${id}/close`,
  });
}

export function receivePurchaseOrder(
  id: string,
  payload: {
    quantity: number;
    receivedDate: string;
    deliveryNo: string;
    receiverName: string;
    payableDueDate: string;
  },
) {
  return request<ReceivePurchaseOrderResult>({
    method: "POST",
    url: `/procurement/orders/${id}/receipts`,
    data: payload,
  });
}

export function listGoodsReceipts() {
  return request<GoodsReceipt[]>({
    method: "GET",
    url: "/procurement/receipts",
  });
}

export function listProcurementPayables() {
  return request<ProcurementPayable[]>({
    method: "GET",
    url: "/procurement/payables",
  });
}

export async function recordPayablePayment(
  payableId: string,
  data: { paidAmount: number; paidAt: string; paymentNote?: string },
  file?: File,
) {
  const form = new FormData();
  form.append(
    "metadata",
    new Blob([JSON.stringify(data)], { type: "application/json" }),
  );
  if (file) form.append("file", file);
  return request<ProcurementPayable>({
    method: "POST",
    url: `/procurement/payables/${payableId}/payment`,
    data: form,
  });
}

export async function downloadPaymentReceipt(payable: ProcurementPayable) {
  const response = await http.get<Blob>(
    `/procurement/payables/${payable.id}/receipt`,
    { responseType: "blob" },
  );
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = payable.paymentReceiptFileName || "payment-receipt";
  anchor.click();
  URL.revokeObjectURL(url);
}

export function submitPurchaseOrder(id: string) {
  return request<PurchaseOrder>({
    method: "POST",
    url: `/procurement/orders/${id}/submit`,
  });
}
export function approvePurchaseOrder(
  id: string,
  payload: {
    decision: "APPROVED" | "REJECTED";
    approverName: string;
    comment: string;
  },
) {
  return request<PurchaseOrder>({
    method: "POST",
    url: `/procurement/orders/${id}/approval`,
    data: payload,
  });
}
export function registerPurchaseArrival(
  id: string,
  payload: {
    quantity: number;
    receivedDate: string;
    deliveryNo: string;
    receiverName: string;
    payableDueDate: string;
    clientRequestId?: string;
    asnNo?: string;
  },
) {
  return request<GoodsReceipt>({
    method: "POST",
    url: `/procurement/orders/${id}/arrivals`,
    data: payload,
  });
}
export function inspectGoodsReceipt(
  id: string,
  payload: {
    qualifiedQty: number;
    rejectedQty: number;
    inspectorName: string;
    comment?: string;
    payableDueDate: string;
  },
) {
  return request<any>({
    method: "POST",
    url: `/procurement/receipts/${id}/inspection`,
    data: payload,
  });
}
export function listAppeals(status?: string) {
  return request<GoodsReceiptAppeal[]>({
    method: "GET",
    url: "/procurement/appeals",
    params: status ? { status } : undefined,
  });
}
export function resolveAppeal(
  id: string,
  payload: { action: "DISMISSED" | "REOPEN"; comment?: string },
) {
  return request<GoodsReceiptAppeal>({
    method: "POST",
    url: `/procurement/appeals/${id}/resolve`,
    data: payload,
  });
}
export function getPortalCollaborationSummary() {
  return request<{
    pendingAccounts: number;
    pendingAdmissions: number;
    pendingDocuments: number;
    pendingQuoteConfirmations: number;
    pendingChangeResponses: number;
    pendingChangeDecisions: number;
    pendingInvoiceSubmissions: number;
    pendingAppeals: number;
    pendingSupplierChanges: number;
    pendingPerformanceAppeals: number;
    updatedAt: string;
  }>({ method: "GET", url: "/procurement/portal-collaboration/summary" });
}

export type SupplierPerformanceReview = {
  id: string;
  supplierId: string;
  reviewPeriod: string;
  onTimeRate: number;
  qualityRate: number;
  invoiceMatchRate: number;
  responseScore: number;
  totalScore: number;
  grade: string;
  reviewerName?: string;
  improvementAction?: string;
  status: string;
  appealStatus?: string;
  appealReason?: string;
  appealedAt?: string;
  appealResolution?: string;
  appealReviewComment?: string;
  appealReviewedBy?: string;
  appealReviewedAt?: string;
  createdAt: string;
};

export function listSupplierReviews() {
  return request<SupplierPerformanceReview[]>({
    method: "GET",
    url: "/procurement/governance/supplier-reviews",
  });
}

export function listSupplierReviewAppeals(status?: string) {
  return request<SupplierPerformanceReview[]>({
    method: "GET",
    url: "/procurement/governance/supplier-reviews/appeals",
    params: status ? { status } : undefined,
  });
}

export function resolvePerformanceAppeal(
  id: string,
  payload: { action: "DISMISSED" | "REOPEN"; comment?: string },
) {
  return request<SupplierPerformanceReview>({
    method: "POST",
    url: `/procurement/governance/supplier-reviews/${id}/appeal/resolve`,
    data: payload,
  });
}

export function listProcurementInquiries() {
  return request<ProcurementInquiry[]>({
    method: "GET",
    url: "/procurement/inquiries",
  });
}
export function createProcurementInquiry(payload: {
  requestId: string;
  title: string;
  deadline?: string;
  createdByName: string;
  sourcingMethod?: string;
  minQuoteCount?: number;
  exceptionReason?: string;
}) {
  return request<ProcurementInquiry>({
    method: "POST",
    url: "/procurement/inquiries",
    data: payload,
  });
}
export function listProcurementPurchasePool() {
  return request<ProcurementPurchasePool>({
    method: "GET",
    url: "/procurement/purchase-pool",
  });
}
export function createConsolidatedProcurementInquiry(payload: {
  requestIds: string[];
  title: string;
  deadline?: string;
  sourcingMethod?: string;
  minQuoteCount?: number;
  exceptionReason?: string;
}) {
  return request<ProcurementInquiry>({
    method: "POST",
    url: "/procurement/purchase-pool/inquiries",
    data: payload,
  });
}
export function addSupplierQuotation(
  id: string,
  payload: {
    supplierId: string;
    unitPrice?: number;
    taxRate?: number;
    deliveryDate?: string;
    paymentTerms?: string;
    remark?: string;
    currency?: string;
    freightAmount?: number;
    otherCostAmount?: number;
    validUntil?: string;
    lines?: Array<{
      requestId: string;
      unitPrice: number;
      taxRate: number;
      deliveryDate?: string;
      remark?: string;
    }>;
  },
) {
  return request<SupplierQuotation>({
    method: "POST",
    url: `/procurement/inquiries/${id}/quotes`,
    data: payload,
  });
}
export function inviteInquirySuppliers(
  id: string,
  supplierIds: string[],
  contactEmails?: Record<string, string>,
) {
  return request<InviteSuppliersResult>({
    method: "POST",
    url: `/procurement/inquiries/${id}/invitations`,
    data: { supplierIds, contactEmails: contactEmails || {} },
  });
}
export function updateProcurementInquiryDeadline(id: string, deadline: string) {
  return request<ProcurementInquiry>({
    method: "POST",
    url: `/procurement/inquiries/${id}/deadline`,
    data: { deadline },
  });
}
export function updateProcurementInquiryMinQuotes(
  id: string,
  minQuoteCount: number,
) {
  return request<ProcurementInquiry>({
    method: "POST",
    url: `/procurement/inquiries/${id}/min-quotes`,
    data: { minQuoteCount },
  });
}
export function listSupplierQuoteAttachments(quoteId: string) {
  return request<SupplierQuoteAttachment[]>({
    method: "GET",
    url: `/procurement/supplier-portal/quotes/${quoteId}/attachments`,
  });
}
export async function downloadSupplierQuoteAttachment(
  attachment: SupplierQuoteAttachment,
) {
  const response = await http.get<Blob>(
    `/procurement/supplier-portal/quote-attachments/${attachment.id}/download`,
    { responseType: "blob" },
  );
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = attachment.fileName;
  anchor.click();
  URL.revokeObjectURL(url);
}
export function listInquiryClarifications(inquiryId: string) {
  return request<InquiryClarification[]>({
    method: "GET",
    url: `/procurement/supplier-portal/inquiries/${inquiryId}/clarifications`,
  });
}
export function answerInquiryClarification(id: string, answer: string) {
  return request<InquiryClarification>({
    method: "POST",
    url: `/procurement/supplier-portal/clarifications/${id}/answer`,
    data: { answer },
  });
}
export function scoreSupplierQuotation(
  id: string,
  quoteId: string,
  payload: { technicalScore: number; commercialScore: number },
) {
  return request<SupplierQuotation>({
    method: "POST",
    url: `/procurement/inquiries/${id}/quotes/${quoteId}/score`,
    data: payload,
  });
}
export function selectSupplierQuotation(
  id: string,
  quoteId: string,
  payload: { operatorName: string; reason: string },
) {
  return request<ProcurementInquiry>({
    method: "POST",
    url: `/procurement/inquiries/${id}/quotes/${quoteId}/select`,
    data: payload,
  });
}
export function listProcurementReturns() {
  return request<ProcurementReturnOrder[]>({
    method: "GET",
    url: "/procurement/returns",
  });
}
export function listSupplierInvoices() {
  return request<SupplierInvoice[]>({
    method: "GET",
    url: "/procurement/supplier-invoices",
  });
}
export function createSupplierInvoice(payload: {
  orderId: string;
  invoiceNo: string;
  amount: number;
  taxRate: number;
  invoiceDate: string;
  remark?: string;
  payableId?: string;
  receiptId?: string;
  clientRequestId?: string;
  attachmentDocumentId?: string;
}) {
  return request<SupplierInvoice>({
    method: "POST",
    url: "/procurement/supplier-invoices",
    data: payload,
  });
}
export function reviewSupplierInvoice(
  id: string,
  payload: {
    decision: "APPROVED" | "REJECTED";
    reviewerName: string;
    comment?: string;
  },
) {
  return request<SupplierInvoice>({
    method: "POST",
    url: `/procurement/supplier-invoices/${id}/review`,
    data: payload,
  });
}
export function listInvoiceSubmissions(status?: string) {
  return request<InvoiceSubmission[]>({
    method: "GET",
    url: "/procurement/invoice-submissions",
    params: status ? { status } : undefined,
  });
}
export function reviewInvoiceSubmission(
  id: string,
  payload: { action: "APPROVED" | "REJECTED"; comment?: string },
) {
  return request<InvoiceSubmission>({
    method: "POST",
    url: `/procurement/invoice-submissions/${id}/review`,
    data: payload,
  });
}
export function invoiceSubmissionDownloadUrl(id: string): string {
  return "/api/procurement/invoice-submissions/" + id + "/download";
}
export function resolveProcurementReturn(
  id: string,
  payload: {
    replacementQty?: number;
    creditAmount?: number;
    claimAmount?: number;
    correctiveAction?: string;
    supplierResponse?: string;
    handlerName: string;
  },
) {
  return request<ProcurementReturnOrder>({
    method: "POST",
    url: `/procurement/returns/${id}/resolve`,
    data: payload,
  });
}

// ---------- 订单变更单 ----------

export type PurchaseOrderChange = {
  id: string;
  orderId: string;
  orderCode?: string;
  changeNo: string;
  changeType: "QTY" | "PRICE" | "DATE" | "MIXED";
  quantityBefore?: number;
  quantityAfter?: number;
  unitPriceBefore?: number;
  unitPriceAfter?: number;
  expectedDateBefore?: string;
  expectedDateAfter?: string;
  reason: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdByName: string;
  decidedByName?: string;
  decisionComment?: string;
  orderVersionBefore?: number;
  orderVersionAfter?: number;
  appliedAt?: string;
  createdAt: string;
};

export function listOrderChanges(orderId: string) {
  return request<PurchaseOrderChange[]>({
    method: "GET",
    url: `/procurement/orders/${orderId}/changes`,
  });
}
export function createOrderChange(
  orderId: string,
  payload: {
    changeType?: string;
    quantityAfter?: number;
    unitPriceAfter?: number;
    expectedDateAfter?: string;
    reason: string;
  },
) {
  return request<PurchaseOrderChange>({
    method: "POST",
    url: `/procurement/orders/${orderId}/changes`,
    data: payload,
  });
}
export function decideOrderChange(
  id: string,
  payload: { decision: "APPROVED" | "REJECTED"; comment?: string },
) {
  return request<PurchaseOrderChange>({
    method: "POST",
    url: `/procurement/order-changes/${id}/decision`,
    data: payload,
  });
}

// ---------- 列表 Excel 导出 ----------

export async function exportProcurementRequests() {
  const response = await http.get<Blob>("/procurement/requests/export", {
    responseType: "blob",
  });
  downloadExcelBlob(response.data, "采购申请.xlsx");
}
export async function exportProcurementInquiries() {
  const response = await http.get<Blob>("/procurement/inquiries/export", {
    responseType: "blob",
  });
  downloadExcelBlob(response.data, "询价管理.xlsx");
}
export async function exportProcurementOrders() {
  const response = await http.get<Blob>("/procurement/orders/export", {
    responseType: "blob",
  });
  downloadExcelBlob(response.data, "采购订单.xlsx");
}
export async function exportProcurementSuppliers() {
  const response = await http.get<Blob>("/procurement/suppliers/export", {
    responseType: "blob",
  });
  downloadExcelBlob(response.data, "供应商.xlsx");
}
function downloadExcelBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

// ---------- 分级审批规则 ----------

export type ApprovalRule = {
  id: string;
  ruleName: string;
  minAmount?: number;
  maxAmount?: number;
  approvalLevel: string;
  requiredRoleCode?: string;
  enabled: boolean;
  sortOrder: number;
};
export type SaveApprovalRulePayload = {
  ruleName: string;
  minAmount?: number;
  maxAmount?: number;
  approvalLevel: string;
  requiredRoleCode?: string;
  enabled: boolean;
  sortOrder: number;
};
export function listApprovalRules() {
  return request<ApprovalRule[]>({
    method: "GET",
    url: "/procurement/approval-rules",
  });
}
export function createApprovalRule(payload: SaveApprovalRulePayload) {
  return request<ApprovalRule>({
    method: "POST",
    url: "/procurement/approval-rules",
    data: payload,
  });
}
export function updateApprovalRule(
  id: string,
  payload: SaveApprovalRulePayload,
) {
  return request<ApprovalRule>({
    method: "PUT",
    url: `/procurement/approval-rules/${id}`,
    data: payload,
  });
}
export function deleteApprovalRule(id: string) {
  return request<void>({
    method: "DELETE",
    url: `/procurement/approval-rules/${id}`,
  });
}

// ---------- 框架协议 ----------

export type FrameworkAgreementItem = {
  id: string;
  partId: string;
  partName: string;
  unitPrice: number;
  taxRate: number;
};
export type FrameworkAgreement = {
  id: string;
  code: string;
  title: string;
  supplierId: string;
  supplierName?: string;
  validFrom: string;
  validTo: string;
  status: "ACTIVE" | "CLOSED";
  remark?: string;
  createdByName?: string;
  items: FrameworkAgreementItem[];
};
export function listFrameworkAgreements() {
  return request<FrameworkAgreement[]>({
    method: "GET",
    url: "/procurement/framework-agreements",
  });
}
export function getFrameworkAgreement(id: string) {
  return request<FrameworkAgreement>({
    method: "GET",
    url: `/procurement/framework-agreements/${id}`,
  });
}
export function saveFrameworkAgreement(
  id: string | null,
  payload: {
    title: string;
    supplierId: string;
    validFrom: string;
    validTo: string;
    remark?: string;
    items: {
      partId: string;
      partName: string;
      unitPrice: number;
      taxRate?: number;
    }[];
  },
) {
  return request<FrameworkAgreement>({
    method: id ? "PUT" : "POST",
    url: id
      ? `/procurement/framework-agreements/${id}`
      : "/procurement/framework-agreements",
    data: payload,
  });
}
export function closeFrameworkAgreement(id: string) {
  return request<FrameworkAgreement>({
    method: "POST",
    url: `/procurement/framework-agreements/${id}/close`,
  });
}

// ---------- 集采计划 ----------

export type CentralPlanItem = {
  id: string;
  partId: string;
  partName: string;
  plannedQty: number;
  unitPrice: number;
  expectedDate?: string;
  requestId?: string;
  requestCode?: string;
  status: "PLANNED" | "REQUESTED";
};
export type CentralPlan = {
  id: string;
  code: string;
  name: string;
  periodYear: number;
  status: "DRAFT" | "ACTIVE" | "CLOSED";
  remark?: string;
  createdByName?: string;
  items: CentralPlanItem[];
};
export function listCentralPlans() {
  return request<CentralPlan[]>({
    method: "GET",
    url: "/procurement/central-plans",
  });
}
export function saveCentralPlan(
  id: string | null,
  payload: {
    name: string;
    periodYear: number;
    remark?: string;
    items: {
      partId: string;
      partName: string;
      plannedQty: number;
      unitPrice?: number;
      expectedDate?: string;
    }[];
  },
) {
  return request<CentralPlan>({
    method: id ? "PUT" : "POST",
    url: id ? `/procurement/central-plans/${id}` : "/procurement/central-plans",
    data: payload,
  });
}
export function updateCentralPlanStatus(id: string, status: string) {
  return request<CentralPlan>({
    method: "POST",
    url: `/procurement/central-plans/${id}/status?status=${encodeURIComponent(status)}`,
  });
}
export function convertCentralPlanItem(
  planId: string,
  itemId: string,
  departmentId?: string,
) {
  const params = new URLSearchParams();
  if (departmentId) params.set("departmentId", departmentId);
  const query = params.toString();
  return request<PurchaseRequest>({
    method: "POST",
    url: `/procurement/central-plans/${planId}/items/${itemId}/convert${query ? `?${query}` : ""}`,
  });
}
