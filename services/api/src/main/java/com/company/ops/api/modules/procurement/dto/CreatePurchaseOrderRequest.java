package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePurchaseOrderRequest(
    @NotBlank String code,
    @NotNull UUID supplierId,
    UUID requestId,
    @PositiveOrZero BigDecimal orderAmount,
    LocalDate expectedDeliveryDate
) {}
