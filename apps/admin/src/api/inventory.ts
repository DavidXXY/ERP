import { request } from "./http";
import { type ProjectStage } from "./project";

export type StockMovementType =
  | "INBOUND"
  | "OUTBOUND"
  | "RETURN"
  | "SCRAP"
  | "ADJUSTMENT";

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

export type InventoryIssueStatus =
  | "POSTED"
  | "PARTIAL_RETURNED"
  | "FULLY_RETURNED";

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
  reason: string;
  totalAmount: number;
  lines: MaterialReturnLine[];
};

export type ReplenishmentSuggestion = {
  partId: string;
  partCode?: string;
  partName: string;
  model?: string;
  stockQty: number;
  safetyQty: number;
  recentOutboundQty: number;
  suggestedQty: number;
  reason: string;
  priority: string;
};

export function listInventoryParts() {
  return request<InventoryPart[]>({ method: "GET", url: "/inventory/parts" });
}

export function listReplenishmentSuggestions() {
  return request<ReplenishmentSuggestion[]>({
    method: "GET",
    url: "/inventory/replenishment-suggestions",
  });
}

export function createInventoryPart(payload: CreateInventoryPartPayload) {
  return request<InventoryPart>({
    method: "POST",
    url: "/inventory/parts",
    data: payload,
  });
}

export function listStockMovements(partId: string) {
  return request<StockMovement[]>({
    method: "GET",
    url: `/inventory/parts/${partId}/movements`,
  });
}

export function createStockMovement(
  partId: string,
  payload: CreateStockMovementPayload,
) {
  return request<InventoryPart>({
    method: "POST",
    url: `/inventory/parts/${partId}/movements`,
    data: payload,
  });
}

export function listMaterialIssues() {
  return request<MaterialIssue[]>({ method: "GET", url: "/inventory/issues" });
}

export function listInventoryProjectOptions() {
  return request<InventoryProjectOption[]>({
    method: "GET",
    url: "/inventory/eligible-projects",
  });
}

export function createMaterialIssue(payload: CreateMaterialIssuePayload) {
  return request<MaterialIssue>({
    method: "POST",
    url: "/inventory/issues",
    data: payload,
  });
}

export function listMaterialReturns() {
  return request<MaterialReturn[]>({
    method: "GET",
    url: "/inventory/returns",
  });
}

export function createMaterialReturn(
  issueId: string,
  payload: {
    code: string;
    returnDate: string;
    handlerName: string;
    reason: string;
    lines: Array<{ issueLineId: string; quantity: number }>;
  },
) {
  return request<MaterialReturn>({
    method: "POST",
    url: `/inventory/issues/${issueId}/returns`,
    data: payload,
  });
}
