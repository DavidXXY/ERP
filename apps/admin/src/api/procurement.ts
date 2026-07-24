import { request } from "./http";
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
  submittedAt?: string;
  closedAt?: string;
  orderVersion?: number;
};

export type CreateSupplierPayload = {
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
  quotes: SupplierQuotation[];
};

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
  lines: SupplierQuotationLine[];
  materialAmount: number;
  totalAmount: number;
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
    technicalScore?: number;
    commercialScore?: number;
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
