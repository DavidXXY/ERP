package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.RiskSeverity;
import com.company.ops.api.modules.project.domain.RiskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectRiskRequest(
    @NotBlank String title,
    String description,
    @NotNull RiskSeverity severity,
    @NotNull RiskStatus status,
    String ownerName,
    UUID ownerUserId,
    LocalDate dueDate,
    String resolution
) {}
