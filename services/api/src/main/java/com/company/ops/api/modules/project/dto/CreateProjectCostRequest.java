package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProjectCostRequest(
    @NotNull ProjectCostCategory category,
    @NotNull ProjectCostSource sourceType,
    @Size(max = 80) String sourceNo,
    @NotBlank @Size(max = 300) String description,
    @NotNull @Positive BigDecimal amount,
    @NotNull LocalDate incurredDate
) {}
