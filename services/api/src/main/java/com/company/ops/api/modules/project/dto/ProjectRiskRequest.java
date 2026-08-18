package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.RiskSeverity;
import com.company.ops.api.modules.project.domain.RiskStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ProjectRiskRequest(
    @NotBlank String title,
    String description,
    RiskSeverity severity,
    RiskStatus status,
    String ownerName,
    LocalDate dueDate,
    String resolution
) {}
