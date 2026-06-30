package com.company.ops.api.modules.system.dto;

import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    UUID orgId,
    String username,
    String displayName,
    String phone,
    String email,
    boolean enabled,
    List<RoleSummary> roles,
    String createdAt,
    String updatedAt
) {
  public record RoleSummary(UUID id, String code, String name) {}
}