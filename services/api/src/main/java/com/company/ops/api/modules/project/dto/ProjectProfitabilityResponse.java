package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectStage;
import java.math.BigDecimal;
import java.util.UUID;

public record ProjectProfitabilityResponse(
    UUID projectId,
    String projectCode,
    String projectName,
    String customerName,
    ProjectStage stage,
    ProjectApprovalStatus approvalStatus,
    BigDecimal contractAmount,
    BigDecimal budgetAmount,
    BigDecimal actualCost,
    BigDecimal grossMargin,
    BigDecimal grossMarginRate,
    BigDecimal budgetUsageRate,
    String riskLevel,
    String riskMessage
) {}
