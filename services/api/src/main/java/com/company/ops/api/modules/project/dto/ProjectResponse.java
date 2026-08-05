package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.domain.ProjectType;
import com.company.ops.api.modules.project.domain.ProjectExecutionStatus;
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
    @Nullable UUID parentProjectId,
    @Nullable String parentProjectCode,
    @Nullable String parentProjectName,
    int childProjectCount,
    String code,
    String name,
    ProjectType projectType,
    @Nullable UUID managerUserId,
    String managerName,
    @Nullable UUID managerAssignedByUserId,
    @Nullable String managerAssignedByName,
    @Nullable OffsetDateTime managerAssignedAt,
    @Nullable String managerAssignmentComment,
    String siteAddress,
    BigDecimal contractAmount,
    LocalDate plannedStartDate,
    LocalDate plannedEndDate,
    ProjectStage stage,
    ProjectApprovalStatus approvalStatus,
    String approvalComment,
    String approverName,
    OffsetDateTime approvedAt,
    @Nullable UUID approverUserId,
    ProjectExecutionStatus executionStatus,
    @Nullable String statusComment,
    @Nullable OffsetDateTime statusChangedAt,
    BigDecimal budgetAmount,
    BigDecimal actualCost,
    BigDecimal grossMargin,
    BigDecimal budgetVariance,
    int progress,
    LocalDate warrantyEndDate
) {}
