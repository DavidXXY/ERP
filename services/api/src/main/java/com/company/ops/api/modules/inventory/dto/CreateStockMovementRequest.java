package com.company.ops.api.modules.inventory.dto;

import com.company.ops.api.modules.inventory.domain.StockMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateStockMovementRequest(
    @NotNull StockMovementType movementType,
    @NotNull @DecimalMin("0.01") BigDecimal quantity,
    @DecimalMin("0") BigDecimal unitCost,
    String sourceNo,
    String remark
) {}
