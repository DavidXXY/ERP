package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectExecutionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeProjectExecutionStatusRequest(
    @NotNull ProjectExecutionStatus status,
    @NotBlank @Size(max = 500) String comment
) {}
