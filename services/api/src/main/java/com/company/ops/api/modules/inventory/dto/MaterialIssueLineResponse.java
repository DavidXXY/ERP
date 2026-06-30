package com.company.ops.api.modules.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MaterialIssueLineResponse(
    UUID id,
    UUID partId,
    String partName,
    BigDecimal quantity,
    BigDecimal returnedQty,
    BigDecimal returnableQty,
    BigDecimal unitCost,
    BigDecimal amount
) {}
