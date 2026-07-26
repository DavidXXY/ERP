package com.company.ops.api.modules.governance.dto;

import com.company.ops.api.modules.governance.domain.AccountingPeriodStatus;
import com.company.ops.api.modules.governance.domain.ControlStatus;
import com.company.ops.api.modules.governance.domain.ControlType;
import com.company.ops.api.modules.governance.domain.ReconciliationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
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
import java.util.Map;
import java.util.UUID;

public final class GovernanceDtos {
  private GovernanceDtos() {}

  public record ControlTypeResponse(ControlType type, String domain, String label) {}

  public record SaveControlRequest(
      @NotNull ControlType controlType,
      UUID businessId,
      @Size(max = 100) String businessNo,
      @NotBlank @Size(max = 180) String name,
      @NotBlank @Size(max = 80) String owner,
      @Size(max = 16) String riskLevel,
      LocalDate plannedStart,
      LocalDate plannedEnd,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      @DecimalMin("0") BigDecimal budgetAmount,
      @DecimalMin("0") BigDecimal committedAmount,
      @DecimalMin("0") BigDecimal actualAmount,
      @DecimalMin("0") BigDecimal forecastAmount,
      @DecimalMin("0") @DecimalMax("100") BigDecimal progressPercent,
      @Min(1) @Max(3660) Integer reviewFrequencyDays,
      Map<String, Object> details) {}

  public record TransitionControlRequest(@NotNull ControlStatus status, @Size(max = 1000) String note) {}
  public record ReviewControlRequest(@NotNull LocalDate reviewedOn, @Size(max = 1000) String note) {}

  public record ControlResponse(
      UUID id, String controlCode, ControlType controlType, String typeLabel, String businessDomain,
      UUID businessId, String businessNo, String name, String owner, ControlStatus status, String riskLevel,
      LocalDate plannedStart, LocalDate plannedEnd, LocalDate effectiveFrom, LocalDate effectiveTo,
      BigDecimal budgetAmount, BigDecimal committedAmount, BigDecimal actualAmount, BigDecimal forecastAmount,
      BigDecimal progressPercent, Integer reviewFrequencyDays, LocalDate lastReviewedOn, LocalDate nextReviewOn,
      Map<String, Object> details, OffsetDateTime activatedAt, OffsetDateTime completedAt,
      String completedBy, String completionNote, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

  public record ControlExceptionResponse(
      String key, UUID controlId, String controlCode, ControlType controlType, String domain, String name,
      String owner, String exceptionType, String severity, String message, LocalDate dueDate,
      BigDecimal exposureAmount) {}
  public record GovernanceActionResponse(
      UUID id, String entityType, UUID entityId, String entityNo, String actionType,
      String fromStatus, String toStatus, String operatorName, String note, OffsetDateTime createdAt) {}

  public record GovernanceOverview(
      long totalControls, long activeControls, long blockedControls, long overdueControls,
      long highRiskControls, BigDecimal budgetAmount, BigDecimal committedAmount,
      BigDecimal actualAmount, BigDecimal forecastAmount, BigDecimal forecastVariance,
      long unmatchedBankLines, long matchedBankLines, long closedPeriods,
      List<DomainSummary> domains) {}
  public record DomainSummary(String domain, long total, long active, long exceptionCount, BigDecimal exposureAmount) {}

  public record OpenPeriodRequest(@Min(2000) @Max(2200) int fiscalYear, @Min(1) @Max(12) int periodNo) {}
  public record ClosePeriodRequest(boolean force, @Size(max = 500) String reason) {}
  public record ReopenPeriodRequest(@NotBlank @Size(min = 5, max = 500) String reason) {}
  public record PeriodResponse(
      UUID id, int fiscalYear, int periodNo, AccountingPeriodStatus status, OffsetDateTime openedAt,
      OffsetDateTime closingStartedAt, OffsetDateTime closedAt, String closedBy, String closeReason,
      OffsetDateTime reopenedAt, String reopenedBy, String reopenReason) {}
  public record CloseReadinessResponse(boolean ready, List<String> blockers) {}

  public record ImportBankLine(
      @NotBlank @Size(max = 80) String accountNoMasked,
      @NotNull LocalDate transactionDate,
      @NotBlank @Size(max = 12) String direction,
      @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
      @Size(max = 180) String counterparty,
      @NotBlank @Size(max = 120) String bankReference,
      @Size(max = 500) String summary) {}
  public record ImportBankStatementRequest(@NotEmpty @Size(max = 2000) List<@Valid ImportBankLine> lines) {}
  public record BankImportResponse(int imported, int duplicates, int suggested) {}
  public record ReconcileBankLineRequest(
      @NotBlank @Size(max = 60) String businessType,
      @NotNull UUID businessId,
      @NotBlank @Size(max = 100) String businessNo,
      @Size(max = 500) String note) {}
  public record BankLineResponse(
      UUID id, String accountNoMasked, LocalDate transactionDate, String direction, BigDecimal amount,
      String counterparty, String bankReference, String summary, ReconciliationStatus reconciliationStatus,
      String matchedBizType, UUID matchedBizId, String matchedBizNo, OffsetDateTime matchedAt,
      String matchedBy, String matchNote) {}
}
