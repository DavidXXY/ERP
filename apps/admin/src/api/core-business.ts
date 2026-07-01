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

export type ProjectType = "NEW_CONSTRUCTION" | "RENOVATION" | "O_M_RENOVATION";
export type ProjectApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ProjectCostCategory = "LABOR" | "MATERIAL" | "SUBCONTRACT" | "TRAVEL" | "OTHER";
export type ProjectCostSource = "MANUAL" | "INVENTORY" | "PROCUREMENT" | "EXPENSE" | "SUBCONTRACT";

export type ContractSummary = { id: string; code: string; projectName: string; amount: number; startDate: string; endDate: string; status: string; };
export type Project = {
  id: string;
  customerId?: string;
  customerName?: string;
  code?: string;
  name: string;
  projectType: ProjectType;
  managerName: string;
  siteAddress: string;
  contractId?: string;
  contractCode?: string;
  contractProjectName?: string;
  contractStatus?: string;
  contractAmount: number;
  plannedStartDate?: string;
  plannedEndDate?: string;
  stage: ProjectStage;
  approvalStatus: ProjectApprovalStatus;
  approvalComment?: string;
  approverName?: string;
  approvedAt?: string;
  budgetAmount: number;
  actualCost: number;
  grossMargin: number;
  budgetVariance: number;
  progress: number;
  warrantyEndDate?: string;
};

export type ProjectBudgetInput = {
  category: ProjectCostCategory;
  plannedAmount: number;
  remark?: string;
};

export type CreateProjectPayload = {
  customerId: string;
  code?: string;
  name: string;
  projectType: ProjectType;
  managerName: string;
  siteAddress: string;
  contractAmount: number;
  plannedStartDate: string;
  plannedEndDate: string;
  budgetItems: ProjectBudgetInput[];
  warrantyEndDate?: string;
};

export type ProjectBudgetItem = ProjectBudgetInput & {
  id: string;
  actualAmount: number;
  variance: number;
};

export type ProjectCostEntry = {
  id: string;
  category: ProjectCostCategory;
  sourceType: ProjectCostSource;
  sourceNo?: string;
  description: string;
  amount: number;
  incurredDate: string;
};

export type ProjectStageRecord = {
  id: string;
  fromStage: ProjectStage;
  toStage: ProjectStage;
  progress: number;
  comment: string;
  operatorName: string;
  changedAt: string;
};

export type ProjectDetail = {
  project: Project;
  budgetItems: ProjectBudgetItem[];
  costEntries: ProjectCostEntry[];
  stageRecords: ProjectStageRecord[];
};

export type StockMovementType = "INBOUND" | "OUTBOUND" | "RETURN" | "SCRAP" | "ADJUSTMENT";

export type InventoryPart = {
  id: string;
  code?: string;
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
  code?: string;
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

export type InventoryIssueStatus = "POSTED" | "PARTIAL_RETURNED" | "FULLY_RETURNED";

export type MaterialIssueLine = {
  id: string;
  partId: string;
  partName: string;
  quantity: number;
  returnedQty: number;
  returnableQty: number;
  unitCost: number;
  amount: number;
};

export type MaterialIssue = {
  id: string;
  code?: string;
  projectId: string;
  projectCode: string;
  projectName: string;
  issueDate: string;
  receiverName: string;
  purpose: string;
  totalAmount: number;
  status: InventoryIssueStatus;
  lines: MaterialIssueLine[];
};

export type InventoryProjectOption = {
  id: string;
  code?: string;
  name: string;
  managerName: string;
  stage: ProjectStage;
};

export type CreateMaterialIssuePayload = {
  code?: string;
  projectId: string;
  issueDate: string;
  receiverName: string;
  purpose: string;
  lines: Array<{ partId: string; quantity: number }>;
};

export type MaterialReturnLine = {
  id: string;
  issueLineId: string;
  partId: string;
  partName: string;
  quantity: number;
  unitCost: number;
  amount: number;
};

export type MaterialReturn = {
  id: string;
  code?: string;
  issueId: string;
  issueCode: string;
  projectId: string;
  projectCode: string;
  projectName: string;
  returnDate: string;
  handlerName: string;
  totalAmount: number;
  lines: MaterialReturnLine[];
};

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
  partId?: string;
  partName: string;
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
  requesterName: string;
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

export function listProjects() {
  return request<Project[]>({ method: "GET", url: "/projects" });
}

export function createProject(payload: CreateProjectPayload) {
  return request<ProjectDetail>({ method: "POST", url: "/projects", data: payload });
}

export function getProject(id: string) {
  return request<ProjectDetail>({ method: "GET", url: `/projects/${id}` });
}

export function processProjectApproval(id: string, payload: { decision: ProjectApprovalStatus; comment: string; approverName: string }) {
  return request<ProjectDetail>({ method: "POST", url: `/projects/${id}/approval`, data: payload });
}

export function advanceProjectStage(id: string, payload: { targetStage: ProjectStage; progress: number; comment: string; operatorName: string }) {
  return request<ProjectDetail>({ method: "POST", url: `/projects/${id}/stage`, data: payload });
}

export function createProjectCost(id: string, payload: { category: ProjectCostCategory; sourceType: ProjectCostSource; sourceNo?: string; description: string; amount: number; incurredDate: string }) {
  return request<ProjectDetail>({ method: "POST", url: `/projects/${id}/costs`, data: payload });
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

export function listMaterialIssues() {
  return request<MaterialIssue[]>({ method: "GET", url: "/inventory/issues" });
}

export function listInventoryProjectOptions() {
  return request<InventoryProjectOption[]>({ method: "GET", url: "/inventory/eligible-projects" });
}

export function createMaterialIssue(payload: CreateMaterialIssuePayload) {
  return request<MaterialIssue>({ method: "POST", url: "/inventory/issues", data: payload });
}

export function listMaterialReturns() {
  return request<MaterialReturn[]>({ method: "GET", url: "/inventory/returns" });
}

export function createMaterialReturn(issueId: string, payload: { code: string; returnDate: string; handlerName: string; lines: Array<{ issueLineId: string; quantity: number }> }) {
  return request<MaterialReturn>({ method: "POST", url: `/inventory/issues/${issueId}/returns`, data: payload });
}

export function listSuppliers() {
  return request<Supplier[]>({ method: "GET", url: "/procurement/suppliers" });
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

export function listPurchaseRequests() {
  return request<PurchaseRequest[]>({ method: "GET", url: "/procurement/requests" });
}

export function createPurchaseRequest(payload: CreatePurchaseRequestPayload) {
  return request<PurchaseRequest>({ method: "POST", url: "/procurement/requests", data: payload });
}

export function processPurchaseRequestApproval(id: string, payload: { decision: ApprovalStatus; comment: string; approverName: string }) {
  return request<PurchaseRequest>({ method: "POST", url: `/procurement/requests/${id}/approval`, data: payload });
}

export function listPurchaseOrders() {
  return request<PurchaseOrder[]>({ method: "GET", url: "/procurement/orders" });
}

export function createPurchaseOrder(payload: CreatePurchaseOrderPayload) {
  return request<PurchaseOrder>({ method: "POST", url: "/procurement/orders", data: payload });
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
