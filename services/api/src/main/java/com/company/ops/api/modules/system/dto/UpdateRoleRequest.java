package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateRoleRequest(
    @Size(max = 120) String name,
    @Size(max = 40) String dataScope,
    List<UUID> permissionIds,
    List<UUID> dataOrganizationIds
) {}
