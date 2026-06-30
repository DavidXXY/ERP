package com.company.ops.api.modules.system.dto;

import java.util.List;
import java.util.UUID;

public record RoleResponse(
    UUID id,
    String code,
    String name,
    String dataScope,
    List<PermissionSummary> permissions,
    List<OrganizationSummary> dataOrganizations,
    long userCount,
    boolean builtIn,
    String createdAt,
    String updatedAt
) {
  public record PermissionSummary(UUID id, String code, String name, String module) {}
  public record OrganizationSummary(UUID id, String code, String name) {}
}
