package com.company.ops.api.modules.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReplenishmentSuggestionResponse(
    UUID partId,
    String partCode,
    String partName,
    String model,
    BigDecimal stockQty,
    BigDecimal safetyQty,
    BigDecimal recentOutboundQty,
    BigDecimal suggestedQty,
    String reason,
    String priority
) {}
