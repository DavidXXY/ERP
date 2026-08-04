package com.company.ops.api.modules.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class FinanceOperationsDtos {
  private FinanceOperationsDtos() {}

  public record OperationsOverview(
      long pendingPeriodJobs, long failedVoucherRequests, long unreconciledPartners,
      long unlockedTaxPeriods, long draftConsolidations, long snapshots,
      BigDecimal budgetVariance, BigDecimal forecastLiquidity) {}

  public record SavePeriodJobRequest(
      @Min(2000) @Max(2200) int fiscalYear, @Min(1) @Max(12) int periodNo,
      @NotBlank @Size(max = 32) String processType,
      @NotBlank @Size(max = 300) String description,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotBlank @Size(max = 32) String debitAccountCode,
      @NotBlank @Size(max = 32) String creditAccountCode,
      boolean autoReverse, LocalDate reversalDate,
      @NotBlank @Size(max = 100) String idempotencyKey) {}

  public record PeriodJobResponse(
      UUID id, int fiscalYear, int periodNo, String processType, String description,
      BigDecimal amount, String debitAccountCode, String creditAccountCode,
      boolean autoReverse, LocalDate reversalDate, String status, UUID voucherId,
      UUID reversalVoucherId, String idempotencyKey, OffsetDateTime executedAt, String executedBy) {}

  public record OpeningValidationLine(String key, String severity, String message, BigDecimal difference) {}
  public record OpeningValidationResponse(int fiscalYear, boolean valid, BigDecimal totalDebit,
      BigDecimal totalCredit, BigDecimal difference, List<OpeningValidationLine> issues) {}

  public record BudgetVarianceResponse(UUID controlId, String controlCode, String name, String owner,
      BigDecimal budget, BigDecimal committed, BigDecimal actual, BigDecimal forecast,
      BigDecimal variance, BigDecimal usageRate, String status) {}

  public record PartnerStatementResponse(String partnerType, UUID partnerId, String partnerCode,
      String partnerName, LocalDate periodEnd, BigDecimal ledgerBalance, BigDecimal confirmedBalance,
      BigDecimal difference, String status, UUID reconciliationId, String confirmationNote,
      OffsetDateTime confirmedAt, String confirmedBy) {}

  public record ConfirmPartnerRequest(@NotNull BigDecimal statementBalance,
      @NotBlank @Size(max = 24) String status, @NotBlank @Size(max = 1000) String note) {}

  public record SaveCashScenarioRequest(@NotBlank @Size(max = 120) String name,
      @NotNull LocalDate asOfDate, @Min(1) @Max(3660) int horizonDays,
      @NotNull BigDecimal openingCash, @NotNull BigDecimal receiptAdjustment,
      @NotNull BigDecimal paymentAdjustment, @Size(max = 2000) String assumptions) {}

  public record CashScenarioResponse(UUID id, String name, LocalDate asOfDate, int horizonDays,
      BigDecimal openingCash, BigDecimal expectedReceipts, BigDecimal expectedPayments,
      BigDecimal receiptAdjustment, BigDecimal paymentAdjustment, BigDecimal forecastCash,
      String status, String assumptions, OffsetDateTime createdAt) {}

  public record TaxFilingResponse(UUID id, int fiscalYear, int periodNo, BigDecimal outputTax,
      BigDecimal inputTax, BigDecimal taxPayable, BigDecimal ledgerTax, BigDecimal difference,
      String status, String filingReference, OffsetDateTime lockedAt, String lockedBy, UUID snapshotId) {}

  public record LockTaxFilingRequest(@NotBlank @Size(max = 100) String filingReference) {}

  public record ConsolidationEntityInput(@NotBlank @Size(max = 120) String entityCode,
      @NotBlank @Size(max = 180) String entityName, @NotNull BigDecimal revenue,
      @NotNull BigDecimal expense) {}

  public record SaveConsolidationRequest(@Min(2000) @Max(2200) int fiscalYear,
      @Min(1) @Max(12) int periodNo, @NotBlank @Size(max = 160) String name,
      @NotEmpty @Size(min = 2, max = 100) List<@Valid ConsolidationEntityInput> entities,
      @NotNull @DecimalMin("0") BigDecimal intercompanyRevenue,
      @NotNull @DecimalMin("0") BigDecimal intercompanyExpense) {}

  public record ConsolidationResponse(UUID id, int fiscalYear, int periodNo, String name,
      int entityCount, BigDecimal combinedRevenue, BigDecimal combinedExpense,
      BigDecimal intercompanyRevenue, BigDecimal intercompanyExpense,
      BigDecimal consolidatedProfit, String status, UUID snapshotId,
      OffsetDateTime completedAt, String completedBy) {}

  public record CaptureSnapshotRequest(@NotBlank @Size(max = 48) String reportType,
      @NotBlank @Size(max = 120) String scopeKey, Integer fiscalYear, Integer periodNo,
      @NotBlank @Size(max = 20000) String payload, @Size(max = 1000) String evidenceNote) {}

  public record ReportSnapshotResponse(UUID id, String reportType, String scopeKey,
      Integer fiscalYear, Integer periodNo, String contentHash, String evidenceNote,
      OffsetDateTime capturedAt, String capturedBy) {}

  public record VoucherRequestResponse(UUID id, String idempotencyKey, String sourceType,
      String businessNo, String status, int attemptCount, UUID voucherId, String lastError,
      OffsetDateTime lastAttemptAt, OffsetDateTime completedAt) {}
}
