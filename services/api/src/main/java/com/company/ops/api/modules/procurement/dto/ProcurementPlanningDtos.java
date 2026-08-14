package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProcurementPlanningDtos {

  private ProcurementPlanningDtos() {}

  public record SaveApprovalRuleRequest(
      @NotBlank @Size(max = 80) String ruleName,
      BigDecimal minAmount,
      BigDecimal maxAmount,
      @NotBlank @Size(max = 24) String approvalLevel,
      @Size(max = 64) String requiredRoleCode,
      @NotNull Boolean enabled,
      @NotNull Integer sortOrder
  ) {}

  public record ApprovalRuleResponse(
      UUID id,
      String ruleName,
      BigDecimal minAmount,
      BigDecimal maxAmount,
      String approvalLevel,
      String requiredRoleCode,
      boolean enabled,
      Integer sortOrder
  ) {}

  public record FrameworkItemRequest(
      @NotNull UUID partId,
      @NotBlank @Size(max = 160) String partName,
      @NotNull @DecimalMin("0.0001") BigDecimal unitPrice,
      BigDecimal taxRate
  ) {}

  public record SaveFrameworkAgreementRequest(
      @NotBlank @Size(max = 180) String title,
      @NotNull UUID supplierId,
      @NotNull LocalDate validFrom,
      @NotNull LocalDate validTo,
      @Size(max = 1000) String remark,
      List<FrameworkItemRequest> items
  ) {}

  public record FrameworkItemResponse(
      UUID id,
      UUID partId,
      String partName,
      BigDecimal unitPrice,
      BigDecimal taxRate
  ) {}

  public record FrameworkAgreementResponse(
      UUID id,
      String code,
      String title,
      UUID supplierId,
      String supplierName,
      LocalDate validFrom,
      LocalDate validTo,
      String status,
      String remark,
      String createdByName,
      List<FrameworkItemResponse> items
  ) {}

  public record CentralPlanItemRequest(
      @NotNull UUID partId,
      @NotBlank @Size(max = 160) String partName,
      @NotNull @DecimalMin("0.01") BigDecimal plannedQty,
      BigDecimal unitPrice,
      LocalDate expectedDate
  ) {}

  public record SaveCentralPlanRequest(
      @NotBlank @Size(max = 180) String name,
      @NotNull Integer periodYear,
      @Size(max = 1000) String remark,
      List<CentralPlanItemRequest> items
  ) {}

  public record CentralPlanItemResponse(
      UUID id,
      UUID partId,
      String partName,
      BigDecimal plannedQty,
      BigDecimal unitPrice,
      LocalDate expectedDate,
      UUID requestId,
      String requestCode,
      String status
  ) {}

  public record CentralPlanResponse(
      UUID id,
      String code,
      String name,
      Integer periodYear,
      String status,
      String remark,
      String createdByName,
      List<CentralPlanItemResponse> items
  ) {}

  public record CentralPlanSuggestionItem(
      UUID partId,
      String partName,
      BigDecimal plannedQty,
      BigDecimal unitPrice,
      BigDecimal estimatedAmount,
      int requestCount
  ) {}

  public record CentralPlanSuggestionsResponse(
      Integer periodYear,
      int itemCount,
      List<CentralPlanSuggestionItem> items
  ) {}
}
