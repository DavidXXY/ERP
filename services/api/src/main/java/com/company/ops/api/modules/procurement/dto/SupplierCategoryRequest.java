package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierCategoryRequest(
    @NotBlank @Size(max = 80) String name,
    @Size(max = 240) String description,
    @Min(0) @Max(9999) Integer sortOrder,
    Boolean enabled
) {}
