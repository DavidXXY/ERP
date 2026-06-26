package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectStage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateProjectRequest(
    UUID customerId,
    @NotBlank String code,
    @NotBlank String name,
    ProjectStage stage,
    @PositiveOrZero BigDecimal budgetAmount,
    @PositiveOrZero BigDecimal actualCost,
    @Min(0) @Max(100) Integer progress,
    LocalDate warrantyEndDate
) {}
