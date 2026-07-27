package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMaterialCategoryRequest(
    @NotBlank @Size(max = 64) String name
) {}
