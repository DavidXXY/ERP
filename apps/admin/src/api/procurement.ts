import { request } from "./http";
import { type PageResponse } from "./system";

export type SupplierRiskStatus = "NORMAL" | "WATCHLIST" | "BLOCKED";
export type PurchaseRequestStatus = "DRAFT" | "SUBMITTED" | "APPROVED" | "ORDERED" | "RECEIVED" | "CANCELLED";
export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";
export type PurchaseOrderStatus = "DRAFT" | "ORDERED" | "PARTIAL_RECEIVED" | "RECEIVED" | "CLOSED" | "CANCELLED";
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
  riskStatus: SupplierRiskStatus;
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
  orderAmount: number;
  expectedDeliveryDate?: string;
  costType: ProcurementCostType;
  costTargetId: string;
  costTargetCode: string;
  costTargetName: string;
  status: PurchaseOrderStatus;
};

export type CreateSupplierPayload = {
  code?: string;
  name: string;
  category?: string;
  contactName?: string;
  phone?: string;
  settlementTerms?: string;
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
  expectedDeliveryDate?: string;
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
  amount: number;
  receivedDate: string;
  deliveryNo: string;
  receiverName: string;
  costType: ProcurementCostType;
  costTargetId: string;
  costTargetCode: string;
  costTargetName: string;
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

export type ReceivePurchaseOrderResult = {
  order: PurchaseOrder;
  receipt: GoodsReceipt;
  payable: ProcurementPayable;
  costAllocation: ProcurementCostAllocation;
  currentStockQty: number;
};


export function listSuppliers(page?: number, size?: number) {
  return request<PageResponse<Supplier>>({ method: "GET", url: "/procurement/suppliers", params: { page, size } });
}

export function createSupplier(payload: CreateSupplierPayload) {
  return request<Supplier>({ method: "POST", url: "/procurement/suppliers", data: payload });
}

export function listProcurementCostTargets() {
  return request<ProcurementCostTargetOptions>({ method: "GET", url: "/procurement/cost-targets" });
}

export function listProcurementCostAllocations() {
  return request<ProcurementCostAllocation[]>({ method: "GET", url: "/procurement/cost-allocations" });
}

export function listPurchaseRequests(params?: {
  status?: PurchaseRequestStatus;
  approvalStatus?: ApprovalStatus;
  costType?: ProcurementCostType;
  search?: string;
  page?: number;
  size?: number;
}) {
  return request<PageResponse<PurchaseRequest>>({ method: "GET", url: "/procurement/requests", params });
}

export function createPurchaseRequest(payload: CreatePurchaseRequestPayload) {
  return request<PurchaseRequest>({ method: "POST", url: "/procurement/requests", data: payload });
}

export function processPurchaseRequestApproval(id: string, payload: { decision: ApprovalStatus; comment: string; approverName: string }) {
  return request<PurchaseRequest>({ method: "POST", url: `/procurement/requests/${id}/approval`, data: payload });
}

export function updatePurchaseRequest(id: string, payload: CreatePurchaseRequestPayload) {
  return request<PurchaseRequest>({ method: "PUT", url: `/procurement/requests/${id}`, data: payload });
}

export function listPurchaseOrders(params?: {
  status?: PurchaseOrderStatus;
  costType?: ProcurementCostType;
  search?: string;
  page?: number;
  size?: number;
}) {
  return request<PageResponse<PurchaseOrder>>({ method: "GET", url: "/procurement/orders", params });
}

export function createPurchaseOrder(payload: CreatePurchaseOrderPayload) {
  return request<PurchaseOrder>({ method: "POST", url: "/procurement/orders", data: payload });
}

export function cancelPurchaseOrder(id: string) {
  return request<PurchaseOrder>({ method: "POST", url: `/procurement/orders/${id}/cancel` });
}

export function receivePurchaseOrder(id: string, payload: { quantity: number; receivedDate: string; deliveryNo: string; receiverName: string; payableDueDate: string }) {
  return request<ReceivePurchaseOrderResult>({ method: "POST", url: `/procurement/orders/${id}/receipts`, data: payload });
}

export function listGoodsReceipts() {
  return request<GoodsReceipt[]>({ method: "GET", url: "/procurement/receipts" });
}

export function listProcurementPayables() {
  return request<ProcurementPayable[]>({ method: "GET", url: "/procurement/payables" });
}
