package com.company.ops.api.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AddProjectStaffRequest(
    @NotNull UUID userId,
    @NotBlank @Size(max = 80) String roleName,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    BigDecimal allocationPercent,
    BigDecimal plannedHours
) {}
