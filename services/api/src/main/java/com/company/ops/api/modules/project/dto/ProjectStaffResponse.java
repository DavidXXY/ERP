package com.company.ops.api.modules.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** 项目成员（复用协作模块的 ProjectStaffAssignment，带显示名）。 */
public record ProjectStaffResponse(
    UUID id,
    UUID userId,
    String displayName,
    String roleName,
    BigDecimal plannedHours,
    BigDecimal actualHours,
    BigDecimal allocationPercent,
    LocalDate startDate,
    LocalDate endDate,
    String certificateStatus,
    String status
) {}
