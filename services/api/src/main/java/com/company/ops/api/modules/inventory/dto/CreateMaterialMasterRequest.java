package com.company.ops.api.modules.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateMaterialMasterRequest(
    @Size(max = 64) String code,
    @NotBlank @Size(max = 160) String name,
    @Size(max = 120) String model,
    @NotBlank @Size(max = 64) String category,
    @PositiveOrZero BigDecimal safetyQty,
    @PositiveOrZero BigDecimal unitCost
) {}
