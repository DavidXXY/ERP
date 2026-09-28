package com.company.ops.api.modules.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class AdvanceDtos {
  private AdvanceDtos() {}

  public record CreateAdvanceReceiptRequest(
      String code,
      @NotNull UUID customerId,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotNull LocalDate receivedDate,
      @Size(max = 80) String referenceNo,
      @Size(max = 500) String remark
  ) {}

  public record CreateAdvancePaymentRequest(
      String code,
      @NotNull UUID supplierId,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotNull LocalDate paidDate,
      @Size(max = 80) String referenceNo,
      @Size(max = 500) String remark
  ) {}

  public record AdvanceReceiptResponse(
      UUID id, String code, UUID customerId, String customerName, BigDecimal amount,
      BigDecimal settledAmount, BigDecimal available, LocalDate receivedDate,
      String referenceNo, String status, String remark
  ) {}

  public record AdvancePaymentResponse(
      UUID id, String code, UUID supplierId, String supplierName, BigDecimal amount,
      BigDecimal settledAmount, BigDecimal available, LocalDate paidDate,
      String referenceNo, String status, String remark
  ) {}

  public record ApplyAdvanceRequest(
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotNull LocalDate applyDate
  ) {}
}