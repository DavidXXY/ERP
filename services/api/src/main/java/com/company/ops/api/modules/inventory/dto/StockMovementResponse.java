package com.company.ops.api.modules.inventory.dto;

import com.company.ops.api.modules.inventory.domain.StockMovementType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockMovementResponse(
    UUID id,
    UUID partId,
    StockMovementType movementType,
    BigDecimal quantity,
    String sourceNo,
    String remark,
    OffsetDateTime createdAt
) {}
