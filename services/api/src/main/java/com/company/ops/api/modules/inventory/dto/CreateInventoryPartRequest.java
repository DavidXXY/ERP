package com.company.ops.api.modules.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateInventoryPartRequest(
    @NotBlank String code,
    @NotBlank String name,
    String model,
    @PositiveOrZero BigDecimal stockQty,
    @PositiveOrZero BigDecimal safetyQty,
    String location,
    @PositiveOrZero BigDecimal unitCost
) {}
