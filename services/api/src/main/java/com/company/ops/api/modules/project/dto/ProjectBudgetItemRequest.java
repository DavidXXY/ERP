package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProjectBudgetItemRequest(
    @NotNull ProjectCostCategory category,
    @NotNull @PositiveOrZero BigDecimal plannedAmount,
    @Size(max = 300) String remark
) {}
