package com.company.ops.api.modules.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class FinanceAnalyticsDtos {
  private FinanceAnalyticsDtos() {}

  public record FinanceAnalyticsResponse(
      LocalDate asOf,
      int fiscalYear,
      FinanceScopeInfo scope,
      List<MonthlyCashFlow> monthlyCashFlow,
      List<ForecastBucket> forecast,
      List<AgingBucket> aging,
      ReconciliationSummary reconciliation,
      TaxSummary tax,
      CashPlanSummary cashPlan,
      List<FinanceRisk> risks) {}

  public record FinanceScopeInfo(UUID organizationId, String organizationName,
      String organizationPath, boolean includeDescendants, int organizationCount,
      boolean unrestricted, boolean unallocatedExcluded) {}

  public record FinanceOrganizationNode(UUID id, String name, String type,
      String fullPath, List<FinanceOrganizationNode> children) {}

  public record MonthlyCashFlow(int month, BigDecimal receipt, BigDecimal payment, BigDecimal net) {}
  public record ForecastBucket(String key, String label, int horizonDays,
      BigDecimal receivable, BigDecimal payable, BigDecimal net) {}
  public record AgingBucket(String key, String label, BigDecimal receivable,
      BigDecimal payable, long receivableCount, long payableCount) {}
  public record ReconciliationItem(String key, BigDecimal businessAmount,
      BigDecimal ledgerAmount, BigDecimal difference) {}
  public record ReconciliationSummary(List<ReconciliationItem> ledger,
      long bankLineCount, long matchedBankLines, long suggestedBankLines,
      long unmatchedBankLines, BigDecimal unmatchedBankAmount) {}
  public record TaxSummary(BigDecimal outputGross, BigDecimal outputNet,
      BigDecimal outputTax, BigDecimal inputGross, BigDecimal inputNet,
      BigDecimal inputTax, BigDecimal netTaxPayable, long pendingOutputInvoices,
      long inputInvoiceExceptions, long adjustedInvoices) {}
  public record CashPlanSummary(BigDecimal baseline, BigDecimal committed,
      BigDecimal actual, BigDecimal forecast, BigDecimal variance, long activePlans) {}
  public record FinanceRisk(String key, String severity, String category,
      String title, String description, BigDecimal amount, long count) {}

  public record TaxInvoiceLine(UUID id, String side, String businessNo,
      String invoiceNo, String partnerName, LocalDate invoiceDate,
      BigDecimal grossAmount, BigDecimal netAmount, BigDecimal taxAmount,
      BigDecimal taxRate, String status, String verificationStatus,
      String adjustmentReason, OffsetDateTime adjustedAt, String adjustedBy) {}

  public record AdjustTaxInvoiceRequest(
      @NotBlank @Size(max = 24) String status,
      @NotNull LocalDate adjustmentDate,
      @NotBlank @Size(min = 5, max = 500) String reason) {}
}
