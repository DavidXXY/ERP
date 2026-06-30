package com.company.ops.api.modules.system.dto;

import java.util.UUID;

public record PermissionResponse(
    UUID id,
    String code,
    String name,
    String module,
    long roleCount,
    boolean builtIn,
    String createdAt
) {}
