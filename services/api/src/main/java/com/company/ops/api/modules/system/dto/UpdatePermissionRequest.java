package com.company.ops.api.modules.system.dto;

public record UpdatePermissionRequest(
    String name,
    String module
) {}
