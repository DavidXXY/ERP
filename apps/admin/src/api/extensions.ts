import { request, requestAllPages } from "./http";

// ============================================================
// 销售订单 / 发货 / 退货 (sales)
// ============================================================
export type SalesOrderStatus =
  | "DRAFT"
  | "APPROVED"
  | "PARTIAL_SHIPPED"
  | "SHIPPED"
  | "CANCELLED";

export type SalesOrderLine = {
  id: string;
  partId: string;
  partName?: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  shippedQty: number;
  returnableQty: number;
};

export type SalesOrder = {
  id: string;
  code: string;
  customerId: string;
  customerName?: string;
  status: SalesOrderStatus;
  totalAmount: number;
  orderDate: string;
  remark?: string;
  lines: SalesOrderLine[];
};

export type SalesShipmentLine = {
  id: string;
  partId: string;
  partName?: string;
  quantity: number;
  unitCost: number;
  amount: number;
};

export type SalesShipment = {
  id: string;
  code: string;
  orderId: string;
  orderCode?: string;
  customerName?: string;
  shipmentDate: string;
  totalCost: number;
  remark?: string;
  lines: SalesShipmentLine[];
};

export type SalesReturn = {
  id: string;
  code: string;
  orderId: string;
  orderCode?: string;
  customerName?: string;
  returnDate: string;
  totalAmount: number;
  reason?: string;
  lines: Array<{
    id: string;
    partId: string;
    partName?: string;
    quantity: number;
    unitCost: number;
    amount: number;
  }>;
};

export type CreateSalesOrderPayload = {
  code?: string;
  customerId: string;
  orderDate: string;
  remark?: string;
  lines: Array<{ partId: string; quantity: number; unitPrice: number }>;
};

export type CreateSalesShipmentPayload = {
  code?: string;
  orderId: string;
  shipmentDate: string;
  remark?: string;
  lines: Array<{ orderLineId: string; quantity: number }>;
};

export type CreateSalesReturnPayload = {
  code?: string;
  orderId: string;
  returnDate: string;
  reason?: string;
  lines: Array<{ orderLineId: string; quantity: number }>;
};

export function listSalesOrders() {
  return requestAllPages<SalesOrder>({ method: "GET", url: "/sales/orders" });
}
export function createSalesOrder(payload: CreateSalesOrderPayload) {
  return request<SalesOrder>({
    method: "POST",
    url: "/sales/orders",
    data: payload,
  });
}
export function approveSalesOrder(id: string) {
  return request<SalesOrder>({
    method: "POST",
    url: `/sales/orders/${id}/approve`,
  });
}
export function cancelSalesOrder(id: string) {
  return request<SalesOrder>({
    method: "POST",
    url: `/sales/orders/${id}/cancel`,
  });
}
export function listSalesShipments() {
  return requestAllPages<SalesShipment>({
    method: "GET",
    url: "/sales/shipments",
  });
}
export function createSalesShipment(payload: CreateSalesShipmentPayload) {
  return request<SalesShipment>({
    method: "POST",
    url: "/sales/shipments",
    data: payload,
  });
}
export function listSalesReturns() {
  return requestAllPages<SalesReturn>({ method: "GET", url: "/sales/returns" });
}
export function createSalesReturn(payload: CreateSalesReturnPayload) {
  return request<SalesReturn>({
    method: "POST",
    url: "/sales/returns",
    data: payload,
  });
}

// ============================================================
// 库存盘点 (stocktake)
// ============================================================
export type StocktakeStatus = "DRAFT" | "POSTED";

export type StocktakeLine = {
  id: string;
  partId: string;
  partName?: string;
  bookQty: number;
  actualQty: number;
  difference: number;
};

export type Stocktake = {
  id: string;
  code: string;
  warehouse?: string;
  status: StocktakeStatus;
  totalDifference?: number;
  lines: StocktakeLine[];
  remark?: string;
};

export type CreateStocktakePayload = {
  code?: string;
  warehouse?: string;
  remark?: string;
  partIds?: string[];
};

export function listStocktakes() {
  return requestAllPages<Stocktake>({
    method: "GET",
    url: "/inventory/stocktakes",
  });
}
export function createStocktake(payload: CreateStocktakePayload) {
  return request<Stocktake>({
    method: "POST",
    url: "/inventory/stocktakes",
    data: payload,
  });
}
export function updateStocktakeLine(lineId: string, actualQty: number) {
  return request<StocktakeLine>({
    method: "PUT",
    url: `/inventory/stocktakes/lines/${lineId}`,
    data: { actualQty },
  });
}
export function postStocktake(id: string) {
  return request<Stocktake>({
    method: "POST",
    url: `/inventory/stocktakes/${id}/post`,
  });
}

// ============================================================
// 批次 / 序列号 (batch/serial)
// ============================================================
export type InventoryBatch = {
  id: string;
  partId: string;
  partName?: string;
  batchNo: string;
  quantity: number;
  unitCost: number;
  receivedDate?: string;
  expiryDate?: string;
  status: string;
};

export type InventorySerial = {
  id: string;
  partId: string;
  partName?: string;
  serialNo: string;
  status: string;
};

export function listBatches() {
  return requestAllPages<InventoryBatch>({
    method: "GET",
    url: "/inventory/batches",
  });
}
export function listSerials() {
  return requestAllPages<InventorySerial>({
    method: "GET",
    url: "/inventory/serials",
  });
}

// ============================================================
// 预收 / 预付 (advance)
// ============================================================
export type AdvanceReceipt = {
  id: string;
  code: string;
  customerId: string;
  customerName?: string;
  amount: number;
  settledAmount: number;
  available: number;
  receivedDate: string;
  referenceNo?: string;
  status: string;
  remark?: string;
};

export type AdvancePayment = {
  id: string;
  code: string;
  supplierId: string;
  supplierName?: string;
  amount: number;
  settledAmount: number;
  available: number;
  paidDate: string;
  referenceNo?: string;
  status: string;
  remark?: string;
};

export function listAdvanceReceipts() {
  return requestAllPages<AdvanceReceipt>({
    method: "GET",
    url: "/finance/advance-receipts",
  });
}
export function createAdvanceReceipt(payload: {
  code?: string;
  customerId: string;
  amount: number;
  receivedDate: string;
  referenceNo?: string;
  remark?: string;
}) {
  return request<AdvanceReceipt>({
    method: "POST",
    url: "/finance/advance-receipts",
    data: payload,
  });
}
export function applyAdvanceReceipt(
  receiptId: string,
  receivableId: string,
  payload: { amount: number; applyDate: string },
) {
  return request<AdvanceReceipt>({
    method: "POST",
    url: `/finance/advance-receipts/${receiptId}/apply/${receivableId}`,
    data: payload,
  });
}
export function listAdvancePayments() {
  return requestAllPages<AdvancePayment>({
    method: "GET",
    url: "/finance/advance-payments",
  });
}
export function createAdvancePayment(payload: {
  code?: string;
  supplierId: string;
  amount: number;
  paidDate: string;
  referenceNo?: string;
  remark?: string;
}) {
  return request<AdvancePayment>({
    method: "POST",
    url: "/finance/advance-payments",
    data: payload,
  });
}
export function applyAdvancePayment(
  paymentId: string,
  payableId: string,
  payload: { amount: number; applyDate: string },
) {
  return request<AdvancePayment>({
    method: "POST",
    url: `/finance/advance-payments/${paymentId}/apply/${payableId}`,
    data: payload,
  });
}

// ============================================================
// 收入确认 (revenue recognition)
// ============================================================
export type RevenueMilestone = {
  id: string;
  contractId: string;
  name: string;
  amount: number;
  plannedDate?: string;
  status: "PENDING" | "RECOGNIZED";
  recognizedDate?: string;
  recognizedBy?: string;
  remark?: string;
};

export type RevenueRecognition = {
  id: string;
  code: string;
  contractId: string;
  milestoneId?: string;
  amount: number;
  recognizeDate: string;
  recognizedBy?: string;
  remark?: string;
};

export function listRevenueMilestones(contractId: string) {
  return request<RevenueMilestone[]>({
    method: "GET",
    url: "/finance/revenue-milestones",
    params: { contractId },
  });
}
export function createRevenueMilestone(payload: {
  contractId: string;
  name: string;
  amount: number;
  plannedDate?: string;
  remark?: string;
}) {
  return request<RevenueMilestone>({
    method: "POST",
    url: "/finance/revenue-milestones",
    data: payload,
  });
}
export function recognizeRevenueMilestone(
  id: string,
  payload: { recognizeDate: string },
) {
  return request<RevenueMilestone>({
    method: "POST",
    url: `/finance/revenue-milestones/${id}/recognize`,
    data: payload,
  });
}
export function listRevenueRecognitions() {
  return requestAllPages<RevenueRecognition>({
    method: "GET",
    url: "/finance/revenue-recognitions",
  });
}

// ============================================================
// 客户信用 (credit)
// ============================================================
export type CreditInfo = {
  customerId: string;
  customerName: string;
  creditLimit: number | null;
  creditBlocked: boolean;
  outstandingReceivables: number;
  openOrderAmount: number;
  totalExposure: number;
};

export function getCustomerCredit(id: string) {
  return request<CreditInfo>({
    method: "GET",
    url: `/crm/customers/${id}/credit`,
  });
}
export function setCustomerCredit(
  id: string,
  payload: { creditLimit: number; creditBlocked: boolean },
) {
  return request<CreditInfo>({
    method: "PUT",
    url: `/crm/customers/${id}/credit`,
    data: payload,
  });
}

// ============================================================
// 固定资产 (fixed asset)
// ============================================================
export type FixedAsset = {
  id: string;
  code: string;
  name: string;
  category: string;
  originalValue: number;
  accumulatedDepreciation: number;
  netValue: number;
  status: string;
  acquisitionDate?: string;
};

export function listFixedAssets() {
  return requestAllPages<FixedAsset>({
    method: "GET",
    url: "/fixedassets/assets",
  });
}

// ============================================================
// 薪酬 (payroll)
// ============================================================
export type PayrollRun = {
  id: string;
  code: string;
  period: string;
  grossAmount: number;
  netAmount: number;
  socialAmount: number;
  taxAmount: number;
  status: string;
};

export function listPayrollRuns() {
  return requestAllPages<PayrollRun>({ method: "GET", url: "/payroll/runs" });
}

// ============================================================
// 法人 / 账套 (legal entity) 与内部交易 (intercompany)
// ============================================================
export type LegalEntity = {
  id: string;
  code: string;
  name: string;
  entityType: string;
  currency: string;
  active: boolean;
  remark?: string;
};

export type IntercompanyTransaction = {
  id: string;
  code: string;
  fromEntityId: string;
  fromEntityName?: string;
  toEntityId: string;
  toEntityName?: string;
  amount: number;
  direction: string;
  transactionDate: string;
  reason?: string;
  status: string;
};

export function listLegalEntities() {
  return requestAllPages<LegalEntity>({
    method: "GET",
    url: "/system/legal-entities",
  });
}
export function createLegalEntity(payload: {
  code: string;
  name: string;
  entityType?: string;
  currency?: string;
  active?: boolean;
  remark?: string;
}) {
  return request<LegalEntity>({
    method: "POST",
    url: "/system/legal-entities",
    data: payload,
  });
}
export function listIntercompanyTransactions() {
  return requestAllPages<IntercompanyTransaction>({
    method: "GET",
    url: "/finance/intercompany-transactions",
  });
}
export function createIntercompanyTransaction(payload: {
  code?: string;
  fromEntityId: string;
  toEntityId: string;
  amount: number;
  direction: string;
  transactionDate: string;
  reason?: string;
}) {
  return request<IntercompanyTransaction>({
    method: "POST",
    url: "/finance/intercompany-transactions",
    data: payload,
  });
}
