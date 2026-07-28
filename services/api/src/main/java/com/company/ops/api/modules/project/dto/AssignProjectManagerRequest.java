package com.company.ops.api.modules.project.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignProjectManagerRequest(
    @NotNull UUID managerUserId,
    @Size(max = 500) String comment
) {}
