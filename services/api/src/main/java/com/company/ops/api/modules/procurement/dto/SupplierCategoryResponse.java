package com.company.ops.api.modules.procurement.dto;

import java.util.UUID;

public record SupplierCategoryResponse(
    UUID id,
    String name,
    String description,
    int sortOrder,
    boolean enabled,
    boolean builtIn,
    long supplierCount
) {}
