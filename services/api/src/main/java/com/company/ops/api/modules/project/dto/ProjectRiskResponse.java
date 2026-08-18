package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.RiskSeverity;
import com.company.ops.api.modules.project.domain.RiskStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectRiskResponse(
    UUID id,
    UUID projectId,
    String title,
    String description,
    RiskSeverity severity,
    RiskStatus status,
    String ownerName,
    LocalDate dueDate,
    String resolution,
    OffsetDateTime createdAt,
    String createdBy
) {}
