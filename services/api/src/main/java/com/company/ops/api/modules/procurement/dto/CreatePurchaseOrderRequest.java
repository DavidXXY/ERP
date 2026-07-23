package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePurchaseOrderRequest(
    // auto-gen
    String code,
    @NotNull UUID supplierId,
    @NotNull UUID requestId,
    @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
    @DecimalMin("0") BigDecimal taxRate,
    LocalDate expectedDeliveryDate,
    @DecimalMin("0.01") BigDecimal orderedQty,
    UUID inquiryId,
    UUID contractId,
    String currency,
    @DecimalMin("0") BigDecimal freightAmount,
    String sourceReason
) {
  public CreatePurchaseOrderRequest(
      String code,
      UUID supplierId,
      UUID requestId,
      BigDecimal unitPrice,
      BigDecimal taxRate,
      LocalDate expectedDeliveryDate
  ) {
    this(code, supplierId, requestId, unitPrice, taxRate, expectedDeliveryDate,
        null, null, null, null, null, null);
  }
}
