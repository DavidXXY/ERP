package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateProjectRequest(
    @NotNull UUID customerId,
    String code, // auto-generated if null
    @NotBlank @Size(max = 180) String name,
    @NotNull ProjectType projectType,
    @NotBlank @Size(max = 80) String managerName,
    @NotBlank @Size(max = 300) String siteAddress,
    @NotNull @PositiveOrZero BigDecimal contractAmount,
    @NotNull LocalDate plannedStartDate,
    @NotNull LocalDate plannedEndDate,
    @NotNull List<@Valid ProjectBudgetItemRequest> budgetItems,
    LocalDate warrantyEndDate,
    UUID contractId // null　则不关联合同
) {}
