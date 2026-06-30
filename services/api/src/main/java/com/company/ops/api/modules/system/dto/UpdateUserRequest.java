package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateUserRequest(
    UUID orgId,
    @Size(max = 80) String displayName,
    @Size(max = 40) String phone,
    @Size(max = 120) String email,
    Boolean enabled,
    List<UUID> roleIds
) {}