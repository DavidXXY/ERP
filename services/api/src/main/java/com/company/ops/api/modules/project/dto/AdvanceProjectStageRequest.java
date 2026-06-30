package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectStage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdvanceProjectStageRequest(
    @NotNull ProjectStage targetStage,
    @Min(0) @Max(100) int progress,
    @NotBlank @Size(max = 500) String comment,
    @NotBlank @Size(max = 80) String operatorName
) {}
