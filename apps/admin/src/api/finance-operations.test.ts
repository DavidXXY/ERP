import { beforeEach, describe, expect, it, vi } from "vitest";

const { requestMock } = vi.hoisted(() => ({ requestMock: vi.fn() }));
vi.mock("./http", () => ({ request: requestMock }));

import {
  captureReportSnapshot,
  completeConsolidation,
  confirmPartnerStatement,
  createCashScenario,
  createConsolidation,
  createPeriodJob,
  executePeriodJob,
  getOperationsOverview,
  listBudgetVariance,
  listCashScenarios,
  listConsolidations,
  listPartnerStatements,
  listPeriodJobs,
  listReportSnapshots,
  listTaxFilings,
  listVoucherRequests,
  lockTaxFiling,
  reconcileTaxFiling,
  reverseDuePeriodJobs,
  validateOpening,
} from "./finance-operations";

describe("finance operations API", () => {
  beforeEach(() => requestMock.mockReset().mockResolvedValue({}));

  it("maps period-end commands to stable endpoints and payloads", async () => {
    const job = {
      fiscalYear: 2026,
      periodNo: 8,
      processType: "ACCRUAL",
      description: "月末计提",
      amount: 100,
      debitAccountCode: "6602",
      creditAccountCode: "2201",
      autoReverse: false,
      idempotencyKey: "period-2026-08",
    };
    await getOperationsOverview();
    await listPeriodJobs({ year: 2026, month: 8 });
    await createPeriodJob(job);
    await executePeriodJob("job-1");
    await reverseDuePeriodJobs("2026-09-01");
    await validateOpening(2026);

    expect(requestMock.mock.calls.map(([config]) => config)).toEqual([
      { method: "GET", url: "/finance/operations/overview" },
      {
        method: "GET",
        url: "/finance/operations/period-jobs",
        params: { year: 2026, month: 8 },
      },
      { method: "POST", url: "/finance/operations/period-jobs", data: job },
      { method: "POST", url: "/finance/operations/period-jobs/job-1/execute" },
      {
        method: "POST",
        url: "/finance/operations/period-jobs/reverse-due",
        params: { asOf: "2026-09-01" },
      },
      { method: "GET", url: "/finance/operations/opening-validation/2026" },
    ]);
  });

  it("maps reconciliation, tax, consolidation, and evidence operations", async () => {
    const partner = {
      statementBalance: 88,
      status: "CONFIRMED",
      note: "已核对",
    };
    const cash = {
      name: "基准情景",
      asOfDate: "2026-08-05",
      horizonDays: 30,
      openingCash: 1000,
      receiptAdjustment: 0,
      paymentAdjustment: 0,
    };
    const consolidation = {
      fiscalYear: 2026,
      periodNo: 8,
      name: "集团月结",
      entities: [
        { entityCode: "HQ", entityName: "总部", revenue: 100, expense: 60 },
        { entityCode: "SUB", entityName: "子公司", revenue: 40, expense: 20 },
      ],
      intercompanyRevenue: 10,
      intercompanyExpense: 10,
    };
    const snapshot = {
      reportType: "PERIOD_CLOSE",
      scopeKey: "2026-08",
      fiscalYear: 2026,
      periodNo: 8,
      payload: "{}",
      evidenceNote: "月结证据",
    };

    await listBudgetVariance();
    await listPartnerStatements("CUSTOMER", "2026-08-31");
    await confirmPartnerStatement(
      "CUSTOMER",
      "partner-1",
      "2026-08-31",
      partner,
    );
    await listCashScenarios();
    await createCashScenario(cash);
    await listTaxFilings();
    await reconcileTaxFiling(2026, 8);
    await lockTaxFiling(2026, 8, "TAX-2026-08");
    await listConsolidations();
    await createConsolidation(consolidation);
    await completeConsolidation("run-1");
    await listReportSnapshots();
    await captureReportSnapshot(snapshot);
    await listVoucherRequests();

    expect(requestMock).toHaveBeenCalledWith({
      method: "POST",
      url: "/finance/operations/tax-filings/2026/8/lock",
      data: { filingReference: "TAX-2026-08" },
    });
    expect(requestMock).toHaveBeenCalledWith({
      method: "POST",
      url: "/finance/operations/partner-statements/CUSTOMER/partner-1/confirm",
      params: { periodEnd: "2026-08-31" },
      data: partner,
    });
    expect(requestMock).toHaveBeenCalledTimes(14);
  });
});
