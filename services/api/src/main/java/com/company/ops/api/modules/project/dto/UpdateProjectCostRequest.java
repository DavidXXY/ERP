package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProjectCostRequest(
    @NotNull ProjectCostCategory category,
    @NotBlank @Size(max = 300) String description,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull LocalDate incurredDate
) {}
