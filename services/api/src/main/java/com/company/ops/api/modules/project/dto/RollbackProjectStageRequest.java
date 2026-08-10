package com.company.ops.api.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RollbackProjectStageRequest(
    @NotBlank @Size(max = 500) String comment
) {}
