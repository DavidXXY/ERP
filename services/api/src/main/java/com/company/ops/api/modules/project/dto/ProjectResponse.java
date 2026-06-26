package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectStage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    UUID customerId,
    String customerName,
    String code,
    String name,
    ProjectStage stage,
    BigDecimal budgetAmount,
    BigDecimal actualCost,
    int progress,
    LocalDate warrantyEndDate
) {}
