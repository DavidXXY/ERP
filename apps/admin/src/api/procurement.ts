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
  remark?: string;
  riskStatus: SupplierRiskStatus;
  contractedAmount: number;
  payableAmount: number;
  paidAmount: number;
  outstandingAmount: number;
};

export type PurchaseRequest = {
  id: string;
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
export type ProcurementInquiry = { id:string; code:string; requestId:string; title:string; deadline?:string; status:"OPEN"|"AWARDED"|"CLOSED"; createdByName:string; sourcingMethod:string; minQuoteCount:number; exceptionReason?:string; selectedQuoteId?:string; selectionReason?:string; selectedByName?:string; selectedAt?:string; quotes:SupplierQuotation[] };
export type SupplierQuotation = { id:string; supplierId:string; supplierName:string; unitPrice:number; taxRate:number; deliveryDate?:string; paymentTerms?:string; remark?:string; selected:boolean; currency:string; freightAmount:number; otherCostAmount:number; technicalScore:number; commercialScore:number; totalScore:number; validUntil?:string };
export type ProcurementReturnOrder = { id:string; code:string; orderId:string; receiptId:string; supplierId:string; quantity:number; amount:number; reason:string; returnDate:string; handlerName:string; status:string; replacementQty:number; creditAmount:number; claimAmount:number; correctiveAction?:string; supplierResponse?:string; completedAt?:string };
export type SupplierInvoice = { id:string; code:string; invoiceNo:string; orderId:string; supplierId:string; payableId?:string; receiptId?:string; amount:number; matchedAmount:number; taxRate:number; invoiceDate:string; status:string; matchStatus:"MATCHED"|"MISMATCH"; differenceAmount:number; approvalStatus:string; verificationStatus:string; attachmentDocumentId?:string; remark?:string };

export type ProcurementContract = { id:string; contractNo:string; name:string; supplierId:string; amount:number; currency:string; startDate?:string; endDate?:string; paymentTerms?:string; status:string; approvalStatus:string; versionNo:number; parentContractId?:string; changeReason?:string; submittedByName?:string; submittedAt?:string; approvedByName?:string; approvalComment?:string; approvedAt?:string; remark?:string };
export type SupplierChangeRequest = { id:string; supplierId:string; changeType:string; proposedAdmissionStatus?:string; proposedRiskStatus?:string; proposedBankName?:string; proposedBankAccount?:string; proposedSettlementTerms?:string; reason:string; status:string; requestedByName:string; reviewedByName?:string; reviewComment?:string; reviewedAt?:string };
export type SupplierPerformanceReview = { id:string; supplierId:string; reviewPeriod:string; onTimeRate:number; qualityRate:number; invoiceMatchRate:number; responseScore:number; totalScore:number; grade:string; reviewerName:string; improvementAction?:string; status:string };
export type ProcurementCollaborationEvent = { id:string; supplierId:string; orderId?:string; eventType:string; referenceNo?:string; eventDate:string; promisedDate?:string; quantity?:number; status:string; content?:string; createdByName:string };

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

export function submitPurchaseOrder(id:string){return request<PurchaseOrder>({method:"POST",url:`/procurement/orders/${id}/submit`});}
export function approvePurchaseOrder(id:string,payload:{decision:"APPROVED"|"REJECTED";approverName:string;comment:string}){return request<PurchaseOrder>({method:"POST",url:`/procurement/orders/${id}/approval`,data:payload});}
export function registerPurchaseArrival(id:string,payload:{quantity:number;receivedDate:string;deliveryNo:string;receiverName:string;payableDueDate:string;clientRequestId?:string;asnNo?:string}){return request<GoodsReceipt>({method:"POST",url:`/procurement/orders/${id}/arrivals`,data:payload});}
export function inspectGoodsReceipt(id:string,payload:{qualifiedQty:number;rejectedQty:number;inspectorName:string;comment?:string;payableDueDate:string}){return request<any>({method:"POST",url:`/procurement/receipts/${id}/inspection`,data:payload});}
export function listProcurementInquiries(){return request<ProcurementInquiry[]>({method:"GET",url:"/procurement/inquiries"});}
export function createProcurementInquiry(payload:{requestId:string;title:string;deadline?:string;createdByName:string;sourcingMethod?:string;minQuoteCount?:number;exceptionReason?:string}){return request<ProcurementInquiry>({method:"POST",url:"/procurement/inquiries",data:payload});}
export function addSupplierQuotation(id:string,payload:{supplierId:string;unitPrice:number;taxRate:number;deliveryDate?:string;paymentTerms?:string;remark?:string;currency?:string;freightAmount?:number;otherCostAmount?:number;technicalScore?:number;commercialScore?:number;validUntil?:string}){return request<SupplierQuotation>({method:"POST",url:`/procurement/inquiries/${id}/quotes`,data:payload});}
export function selectSupplierQuotation(id:string,quoteId:string,payload:{operatorName:string;reason:string}){return request<ProcurementInquiry>({method:"POST",url:`/procurement/inquiries/${id}/quotes/${quoteId}/select`,data:payload});}
export function listProcurementReturns(){return request<ProcurementReturnOrder[]>({method:"GET",url:"/procurement/returns"});}
export function listSupplierInvoices(){return request<SupplierInvoice[]>({method:"GET",url:"/procurement/supplier-invoices"});}
export function createSupplierInvoice(payload:{orderId:string;invoiceNo:string;amount:number;taxRate:number;invoiceDate:string;remark?:string;payableId?:string;receiptId?:string;clientRequestId?:string;attachmentDocumentId?:string}){return request<SupplierInvoice>({method:"POST",url:"/procurement/supplier-invoices",data:payload});}
export function reviewSupplierInvoice(id:string,payload:{decision:"APPROVED"|"REJECTED";reviewerName:string;comment?:string}){return request<SupplierInvoice>({method:"POST",url:`/procurement/supplier-invoices/${id}/review`,data:payload});}
export function resolveProcurementReturn(id:string,payload:{replacementQty?:number;creditAmount?:number;claimAmount?:number;correctiveAction?:string;supplierResponse?:string;handlerName:string}){return request<ProcurementReturnOrder>({method:"POST",url:`/procurement/returns/${id}/resolve`,data:payload});}

export function listProcurementContracts(){return request<ProcurementContract[]>({method:"GET",url:"/procurement/governance/contracts"});}
export function createProcurementContract(payload:Omit<ProcurementContract,"id"|"status"|"approvalStatus"|"versionNo">){return request<ProcurementContract>({method:"POST",url:"/procurement/governance/contracts",data:payload});}
export function submitProcurementContract(id:string){return request<ProcurementContract>({method:"POST",url:`/procurement/governance/contracts/${id}/submit`});}
export function reviewProcurementContract(id:string,payload:{decision:"APPROVED"|"REJECTED";comment?:string}){return request<ProcurementContract>({method:"POST",url:`/procurement/governance/contracts/${id}/review`,data:payload});}
export function amendProcurementContract(id:string,payload:{amount:number;startDate?:string;endDate?:string;paymentTerms?:string;changeReason:string;remark?:string}){return request<ProcurementContract>({method:"POST",url:`/procurement/governance/contracts/${id}/amend`,data:payload});}
export function listSupplierChanges(){return request<SupplierChangeRequest[]>({method:"GET",url:"/procurement/governance/supplier-changes"});}
export function createSupplierChange(payload:{supplierId:string;changeType:string;proposedAdmissionStatus?:string;proposedRiskStatus?:string;proposedBankName?:string;proposedBankAccount?:string;proposedSettlementTerms?:string;reason:string}){return request<SupplierChangeRequest>({method:"POST",url:"/procurement/governance/supplier-changes",data:payload});}
export function reviewSupplierChange(id:string,payload:{decision:"APPROVED"|"REJECTED";comment?:string}){return request<SupplierChangeRequest>({method:"POST",url:`/procurement/governance/supplier-changes/${id}/review`,data:payload});}
export function listSupplierPerformanceReviews(){return request<SupplierPerformanceReview[]>({method:"GET",url:"/procurement/governance/supplier-reviews"});}
export function calculateSupplierPerformance(payload:{supplierId:string;reviewPeriod:string;responseScore?:number;improvementAction?:string}){return request<SupplierPerformanceReview>({method:"POST",url:"/procurement/governance/supplier-reviews/calculate",data:payload});}
export function listProcurementCollaborationEvents(){return request<ProcurementCollaborationEvent[]>({method:"GET",url:"/procurement/governance/collaboration-events"});}
export function createProcurementCollaborationEvent(payload:{supplierId:string;orderId?:string;eventType:string;referenceNo?:string;eventDate:string;promisedDate?:string;quantity?:number;status?:string;content?:string}){return request<ProcurementCollaborationEvent>({method:"POST",url:"/procurement/governance/collaboration-events",data:payload});}
export function convertReplenishmentToRequest(payload:{partId:string;quantity:number;unitPrice?:number;expectedDate?:string;costType:ProcurementCostType;projectId?:string;departmentId?:string;reason?:string}){return request<PurchaseRequest>({method:"POST",url:"/procurement/governance/replenishment/convert",data:payload});}
