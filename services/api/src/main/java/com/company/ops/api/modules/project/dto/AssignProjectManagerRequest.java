package com.company.ops.api.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignProjectManagerRequest(
    @NotBlank @Size(max = 80) String managerName,
    @NotBlank @Size(max = 80) String operatorName,
    @Size(max = 500) String comment
) {}
