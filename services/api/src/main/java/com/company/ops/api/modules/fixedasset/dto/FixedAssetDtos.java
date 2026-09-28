package com.company.ops.api.modules.fixedasset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class FixedAssetDtos {
  private FixedAssetDtos() {}

  public record CreateAssetRequest(
      @NotBlank @Size(max = 160) String name,
      @Size(max = 64) String category,
      LocalDate acquisitionDate,
      @NotNull @DecimalMin("0") BigDecimal originalValue,
      @NotNull @DecimalMin("0") BigDecimal residualValue,
      @NotNull @Min(1) int usefulLifeMonths,
      @Size(max = 120) String location,
      @Size(max = 80) String custodian,
      UUID custodianUserId,
      UUID organizationId,
      @Size(max = 500) String remark) {}

  public record RunDepreciationRequest(@NotBlank @Size(max = 7) String period) {}

  public record CreateTransferRequest(
      @NotNull UUID assetId,
      @Size(max = 120) String toLocation,
      @Size(max = 80) String toCustodian,
      @NotNull LocalDate transferDate,
      @Size(max = 500) String remark) {}

  public record CreateDisposalRequest(
      @NotNull UUID assetId,
      @NotNull LocalDate disposalDate,
      @Size(max = 24) String method,
      @NotNull @DecimalMin("0") BigDecimal proceeds,
      @Size(max = 500) String remark) {}

  public record CreateCountRequest(
      @NotNull LocalDate countDate,
      @Size(max = 500) String remark) {}

  public record RecordCountLineRequest(
      @NotNull @Min(0) Integer actualQuantity,
      @Size(max = 300) String remark) {}

  public record FixedAssetResponse(
      UUID id, String code, String name, String category, LocalDate acquisitionDate,
      BigDecimal originalValue, BigDecimal residualValue, int usefulLifeMonths,
      String depreciationMethod, BigDecimal monthlyDepreciation, BigDecimal accumulatedDepreciation,
      String lastDepreciatedPeriod, String status, String location, String custodian,
      UUID custodianUserId, UUID organizationId, String remark,
      OffsetDateTime createdAt, OffsetDateTime updatedAt, String createdBy, String updatedBy, long version) {}

  public record DepreciationRunResponse(
      UUID id, String code, String period, LocalDate runDate, BigDecimal totalAmount,
      String status, OffsetDateTime createdAt, List<DepreciationLineResponse> lines) {}

  public record DepreciationLineResponse(
      UUID id, UUID runId, UUID assetId, String period, BigDecimal amount) {}

  public record TransferResponse(
      UUID id, String code, UUID assetId, String fromLocation, String toLocation,
      String fromCustodian, String toCustodian, LocalDate transferDate, String remark,
      OffsetDateTime createdAt) {}

  public record DisposalResponse(
      UUID id, String code, UUID assetId, LocalDate disposalDate, String method,
      BigDecimal proceeds, BigDecimal netBookValue, BigDecimal gainLoss, String remark,
      OffsetDateTime createdAt) {}

  public record CountResponse(
      UUID id, String code, LocalDate countDate, String status, String remark,
      OffsetDateTime createdAt, List<CountLineResponse> lines) {}

  public record CountLineResponse(
      UUID id, UUID countId, UUID assetId, String assetCode, String assetName,
      int bookQuantity, Integer actualQuantity, Integer difference, String remark) {}
}
