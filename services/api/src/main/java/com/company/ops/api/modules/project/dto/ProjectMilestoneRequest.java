package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.MilestoneStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ProjectMilestoneRequest(
    @NotBlank String name,
    LocalDate plannedDate,
    LocalDate actualDate,
    @NotNull MilestoneStatus status,
    int sortOrder,
    String remark
) {}
