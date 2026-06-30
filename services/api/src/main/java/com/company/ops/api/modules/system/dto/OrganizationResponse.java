package com.company.ops.api.modules.system.dto;

import java.util.List;
import java.util.UUID;

public record OrganizationResponse(
    UUID id,
    String code,
    String name,
    String type,
    Integer sortOrder,
    UUID parentId,
    String parentName,
    String fullPath,
    String leaderName,
    String phone,
    boolean enabled,
    String description,
    long directUserCount,
    long totalUserCount,
    int childCount,
    List<OrganizationResponse> children,
    String createdAt,
    String updatedAt
) {}
