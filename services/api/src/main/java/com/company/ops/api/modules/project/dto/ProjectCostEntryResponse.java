package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectCostEntryResponse(
    UUID id,
    ProjectCostCategory category,
    ProjectCostSource sourceType,
    String sourceNo,
    String description,
    BigDecimal amount,
    LocalDate incurredDate
) {}
