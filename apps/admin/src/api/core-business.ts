import { request } from "./http";

export type ProjectStage =
  | "INITIATED"
  | "BIDDING"
  | "ENTRY"
  | "CONSTRUCTION"
  | "COMMISSIONING"
  | "INITIAL_ACCEPTANCE"
  | "FINAL_ACCEPTANCE"
  | "WARRANTY"
  | "CLOSED";

export type Project = {
  id: string;
  customerId?: string;
  customerName?: string;
  code: string;
  name: string;
  stage: ProjectStage;
  budgetAmount: number;
  actualCost: number;
  progress: number;
  warrantyEndDate?: string;
};

export type CreateProjectPayload = {
  customerId?: string;
  code: string;
  name: string;
  stage?: ProjectStage;
  budgetAmount?: number;
  actualCost?: number;
  progress?: number;
  warrantyEndDate?: string;
};

export type StockMovementType = "INBOUND" | "OUTBOUND" | "RETURN" | "SCRAP" | "ADJUSTMENT";

export type InventoryPart = {
  id: string;
  code: string;
  name: string;
  model?: string;
  stockQty: number;
  safetyQty: number;
  location?: string;
  unitCost: number;
  lowStock: boolean;
};

export type StockMovement = {
  id: string;
  partId: string;
  movementType: StockMovementType;
  quantity: number;
  sourceNo?: string;
  remark?: string;
  createdAt: string;
};

export type CreateInventoryPartPayload = {
  code: string;
  name: string;
  model?: string;
  stockQty?: number;
  safetyQty?: number;
  location?: string;
  unitCost?: number;
};

export type CreateStockMovementPayload = {
  movementType: StockMovementType;
  quantity: number;
  sourceNo?: string;
  remark?: string;
};

export type SupplierRiskStatus = "NORMAL" | "WATCHLIST" | "BLOCKED";
export type PurchaseRequestStatus = "DRAFT" | "SUBMITTED" | "APPROVED" | "ORDERED" | "RECEIVED" | "CANCELLED";
export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";
export type PurchaseOrderStatus = "DRAFT" | "ORDERED" | "PARTIAL_RECEIVED" | "RECEIVED" | "CLOSED" | "CANCELLED";

export type Supplier = {
  id: string;
  code: string;
  name: string;
  category?: string;
  contactName?: string;
  phone?: string;
  settlementTerms?: string;
  riskStatus: SupplierRiskStatus;
};

export type PurchaseRequest = {
  id: string;
  code: string;
  requesterName: string;
  partId?: string;
  partName: string;
  quantity: number;
  expectedDate?: string;
  reason?: string;
  status: PurchaseRequestStatus;
  approvalStatus: ApprovalStatus;
};

export type PurchaseOrder = {
  id: string;
  code: string;
  supplierId: string;
  supplierName?: string;
  requestId?: string;
  requestCode?: string;
  orderAmount: number;
  expectedDeliveryDate?: string;
  status: PurchaseOrderStatus;
};

export type CreateSupplierPayload = {
  code: string;
  name: string;
  category?: string;
  contactName?: string;
  phone?: string;
  settlementTerms?: string;
  riskStatus?: SupplierRiskStatus;
};

export type CreatePurchaseRequestPayload = {
  code: string;
  requesterName: string;
  partId?: string;
  partName?: string;
  quantity: number;
  expectedDate?: string;
  reason?: string;
};

export type CreatePurchaseOrderPayload = {
  code: string;
  supplierId: string;
  requestId?: string;
  orderAmount?: number;
  expectedDeliveryDate?: string;
};

export function listProjects() {
  return request<Project[]>({ method: "GET", url: "/projects" });
}

export function createProject(payload: CreateProjectPayload) {
  return request<Project>({ method: "POST", url: "/projects", data: payload });
}

export function listInventoryParts() {
  return request<InventoryPart[]>({ method: "GET", url: "/inventory/parts" });
}

export function createInventoryPart(payload: CreateInventoryPartPayload) {
  return request<InventoryPart>({ method: "POST", url: "/inventory/parts", data: payload });
}

export function listStockMovements(partId: string) {
  return request<StockMovement[]>({ method: "GET", url: `/inventory/parts/${partId}/movements` });
}

export function createStockMovement(partId: string, payload: CreateStockMovementPayload) {
  return request<InventoryPart>({ method: "POST", url: `/inventory/parts/${partId}/movements`, data: payload });
}

export function listSuppliers() {
  return request<Supplier[]>({ method: "GET", url: "/procurement/suppliers" });
}

export function createSupplier(payload: CreateSupplierPayload) {
  return request<Supplier>({ method: "POST", url: "/procurement/suppliers", data: payload });
}

export function listPurchaseRequests() {
  return request<PurchaseRequest[]>({ method: "GET", url: "/procurement/requests" });
}

export function createPurchaseRequest(payload: CreatePurchaseRequestPayload) {
  return request<PurchaseRequest>({ method: "POST", url: "/procurement/requests", data: payload });
}

export function listPurchaseOrders() {
  return request<PurchaseOrder[]>({ method: "GET", url: "/procurement/orders" });
}

export function createPurchaseOrder(payload: CreatePurchaseOrderPayload) {
  return request<PurchaseOrder>({ method: "POST", url: "/procurement/orders", data: payload });
}
