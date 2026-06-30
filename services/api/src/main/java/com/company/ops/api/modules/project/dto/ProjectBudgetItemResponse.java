package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import java.math.BigDecimal;
import java.util.UUID;

public record ProjectBudgetItemResponse(
    UUID id,
    ProjectCostCategory category,
    BigDecimal plannedAmount,
    BigDecimal actualAmount,
    BigDecimal variance,
    String remark
) {}
