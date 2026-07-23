package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class ProcurementGovernanceDtos {
  private ProcurementGovernanceDtos() {}

  public record CreateContract(
      @NotBlank String contractNo,
      @NotBlank String name,
      @NotNull UUID supplierId,
      @NotNull @Positive BigDecimal amount,
      String currency,
      LocalDate startDate,
      LocalDate endDate,
      String paymentTerms,
      String remark
  ) {}

  public record ReviewAction(@NotBlank String decision, String comment) {}

  public record AmendContract(
      @NotNull @Positive BigDecimal amount,
      LocalDate startDate,
      LocalDate endDate,
      String paymentTerms,
      @NotBlank String changeReason,
      String remark
  ) {}

  public record CreateSupplierChange(
      @NotNull UUID supplierId,
      @NotBlank String changeType,
      String proposedAdmissionStatus,
      String proposedRiskStatus,
      String proposedBankName,
      String proposedBankAccount,
      String proposedSettlementTerms,
      @NotBlank String reason
  ) {}

  public record CalculateSupplierReview(
      @NotNull UUID supplierId,
      @NotBlank String reviewPeriod,
      @PositiveOrZero BigDecimal responseScore,
      String improvementAction
  ) {}

  public record CreateCollaborationEvent(
      @NotNull UUID supplierId,
      UUID orderId,
      @NotBlank String eventType,
      String referenceNo,
      @NotNull LocalDate eventDate,
      LocalDate promisedDate,
      @PositiveOrZero BigDecimal quantity,
      String status,
      String content
  ) {}

  public record ConvertReplenishment(
      @NotNull UUID partId,
      @NotNull @Positive BigDecimal quantity,
      BigDecimal unitPrice,
      LocalDate expectedDate,
      @NotNull ProcurementCostType costType,
      UUID projectId,
      UUID departmentId,
      String reason
  ) {}
}
