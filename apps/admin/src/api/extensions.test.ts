import { beforeEach, describe, expect, it, vi } from "vitest";

const { requestMock, requestAllPagesMock } = vi.hoisted(() => ({
  requestMock: vi.fn(),
  requestAllPagesMock: vi.fn(),
}));
vi.mock("./http", () => ({
  request: requestMock,
  requestAllPages: requestAllPagesMock,
}));

import {
  applyAdvancePayment,
  applyAdvanceReceipt,
  approveSalesOrder,
  cancelSalesOrder,
  createAdvancePayment,
  createAdvanceReceipt,
  createIntercompanyTransaction,
  createLegalEntity,
  createRevenueMilestone,
  createSalesOrder,
  createSalesReturn,
  createSalesShipment,
  createStocktake,
  getCustomerCredit,
  listAdvancePayments,
  listAdvanceReceipts,
  listBatches,
  listFixedAssets,
  listIntercompanyTransactions,
  listLegalEntities,
  listPayrollRuns,
  listRevenueMilestones,
  listRevenueRecognitions,
  listSalesOrders,
  listSalesReturns,
  listSalesShipments,
  listSerials,
  listStocktakes,
  postStocktake,
  recognizeRevenueMilestone,
  setCustomerCredit,
  updateStocktakeLine,
} from "./extensions";

describe("extensions API", () => {
  beforeEach(() => {
    requestMock.mockReset().mockResolvedValue({});
    requestAllPagesMock.mockReset().mockResolvedValue([]);
  });

  it("covers sales order / shipment / return endpoints", async () => {
    await listSalesOrders();
    await createSalesOrder({
      customerId: "cust-1",
      orderDate: "2026-08-01",
      lines: [{ partId: "part-1", quantity: 2, unitPrice: 10 }],
    });
    await approveSalesOrder("order-1");
    await cancelSalesOrder("order-1");
    await listSalesShipments();
    await createSalesShipment({
      orderId: "order-1",
      shipmentDate: "2026-08-02",
      lines: [{ orderLineId: "line-1", quantity: 1 }],
    });
    await listSalesReturns();
    await createSalesReturn({
      orderId: "order-1",
      returnDate: "2026-08-03",
      lines: [{ orderLineId: "line-1", quantity: 1 }],
    });

    expect(requestAllPagesMock).toHaveBeenCalledTimes(3);
    expect(requestMock).toHaveBeenCalledWith({
      method: "POST",
      url: "/sales/orders/order-1/approve",
    });
    expect(requestMock).toHaveBeenCalledTimes(5);
  });

  it("covers stocktake and batch/serial endpoints", async () => {
    await listStocktakes();
    await createStocktake({ warehouse: "W1", partIds: ["part-1"] });
    await updateStocktakeLine("line-1", 5);
    await postStocktake("st-1");
    await listBatches();
    await listSerials();

    expect(requestAllPagesMock).toHaveBeenCalledTimes(3);
    expect(requestMock).toHaveBeenCalledWith({
      method: "PUT",
      url: "/inventory/stocktakes/lines/line-1",
      data: { actualQty: 5 },
    });
    expect(requestMock).toHaveBeenCalledTimes(3);
  });

  it("covers advance receipt / payment endpoints", async () => {
    await listAdvanceReceipts();
    await createAdvanceReceipt({
      customerId: "cust-1",
      amount: 100,
      receivedDate: "2026-08-01",
    });
    await applyAdvanceReceipt("rcpt-1", "ar-1", {
      amount: 50,
      applyDate: "2026-08-02",
    });
    await listAdvancePayments();
    await createAdvancePayment({
      supplierId: "sup-1",
      amount: 80,
      paidDate: "2026-08-03",
    });
    await applyAdvancePayment("pay-1", "ap-1", {
      amount: 40,
      applyDate: "2026-08-04",
    });

    expect(requestAllPagesMock).toHaveBeenCalledTimes(2);
    expect(requestMock).toHaveBeenCalledWith({
      method: "POST",
      url: "/finance/advance-receipts/rcpt-1/apply/ar-1",
      data: { amount: 50, applyDate: "2026-08-02" },
    });
    expect(requestMock).toHaveBeenCalledTimes(4);
  });

  it("covers revenue, credit, asset, payroll, entity and intercompany endpoints", async () => {
    await listRevenueMilestones("contract-1");
    await createRevenueMilestone({
      contractId: "contract-1",
      name: "里程碑",
      amount: 100,
    });
    await recognizeRevenueMilestone("ms-1", { recognizeDate: "2026-08-01" });
    await listRevenueRecognitions();
    await getCustomerCredit("cust-1");
    await setCustomerCredit("cust-1", {
      creditLimit: 1000,
      creditBlocked: false,
    });
    await listFixedAssets();
    await listPayrollRuns();
    await listLegalEntities();
    await createLegalEntity({ code: "HQ", name: "总部" });
    await listIntercompanyTransactions();
    await createIntercompanyTransaction({
      fromEntityId: "e1",
      toEntityId: "e2",
      amount: 500,
      direction: "DR",
      transactionDate: "2026-08-01",
    });

    expect(requestAllPagesMock).toHaveBeenCalledTimes(5);
    expect(requestMock).toHaveBeenCalledWith({
      method: "GET",
      url: "/crm/customers/cust-1/credit",
    });
    expect(requestMock).toHaveBeenCalledTimes(7);
  });
});
