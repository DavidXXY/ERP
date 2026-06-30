package com.company.ops.api.modules.system.dto;

import java.util.UUID;

public record PermissionResponse(
    UUID id,
    String code,
    String name,
    String module,
    String createdAt
) {}