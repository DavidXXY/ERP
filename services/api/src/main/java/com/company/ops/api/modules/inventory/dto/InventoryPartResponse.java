package com.company.ops.api.modules.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryPartResponse(
    UUID id,
    String code,
    String name,
    String model,
    BigDecimal stockQty,
    BigDecimal safetyQty,
    String location,
    BigDecimal unitCost,
    boolean lowStock
) {}
