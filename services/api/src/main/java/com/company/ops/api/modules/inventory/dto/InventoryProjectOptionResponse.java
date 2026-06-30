package com.company.ops.api.modules.inventory.dto;

import com.company.ops.api.modules.project.domain.ProjectStage;
import java.util.UUID;

public record InventoryProjectOptionResponse(
    UUID id,
    String code,
    String name,
    String managerName,
    ProjectStage stage
) {}
