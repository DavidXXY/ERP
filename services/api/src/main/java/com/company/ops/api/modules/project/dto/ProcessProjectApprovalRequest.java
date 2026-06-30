package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessProjectApprovalRequest(
    @NotNull ProjectApprovalStatus decision,
    @NotBlank @Size(max = 500) String comment,
    @NotBlank @Size(max = 80) String approverName
) {}
