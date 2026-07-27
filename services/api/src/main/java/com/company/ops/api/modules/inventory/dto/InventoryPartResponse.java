package com.company.ops.api.modules.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryPartResponse(
    UUID id,
    String code,
    String name,
    String model,
    String category,
    BigDecimal stockQty,
    BigDecimal safetyQty,
    BigDecimal unitCost,
    boolean lowStock
) {}
