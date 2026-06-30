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
    LocalDate expectedDeliveryDate
) {}
