package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateOrganizationRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 40) String type,
    Integer sortOrder,
    UUID parentId
) {}