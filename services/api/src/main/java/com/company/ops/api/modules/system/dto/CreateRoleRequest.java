package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateRoleRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 40) String dataScope,
    List<UUID> permissionIds,
    List<UUID> dataOrganizationIds
) {}
