package com.company.ops.api.modules.procurement.dto;

import java.util.UUID;

public record MaterialCategoryResponse(
    UUID id,
    String name,
    boolean builtIn
) {}
