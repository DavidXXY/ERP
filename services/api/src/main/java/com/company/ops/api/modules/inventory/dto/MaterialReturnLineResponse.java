package com.company.ops.api.modules.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MaterialReturnLineResponse(
    UUID id,
    UUID issueLineId,
    UUID partId,
    String partName,
    BigDecimal quantity,
    BigDecimal unitCost,
    BigDecimal amount
) {}
