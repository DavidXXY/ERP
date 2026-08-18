package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.MilestoneStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectMilestoneResponse(
    UUID id,
    UUID projectId,
    String name,
    LocalDate plannedDate,
    LocalDate actualDate,
    MilestoneStatus status,
    int sortOrder,
    String remark
) {}
