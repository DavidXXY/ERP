package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.domain.ProjectType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.annotation.Nullable;
import com.company.ops.api.modules.crm.domain.ContractStatus;

public record ProjectResponse(
    UUID id,
    UUID customerId,
    String customerName,
    @Nullable UUID contractId,
    @Nullable String contractCode,
    @Nullable String contractProjectName,
    @Nullable ContractStatus contractStatus,
    String code,
    String name,
    ProjectType projectType,
    String managerName,
    String siteAddress,
    BigDecimal contractAmount,
    LocalDate plannedStartDate,
    LocalDate plannedEndDate,
    ProjectStage stage,
    ProjectApprovalStatus approvalStatus,
    String approvalComment,
    String approverName,
    OffsetDateTime approvedAt,
    BigDecimal budgetAmount,
    BigDecimal actualCost,
    BigDecimal grossMargin,
    BigDecimal budgetVariance,
    int progress,
    LocalDate warrantyEndDate
) {}
