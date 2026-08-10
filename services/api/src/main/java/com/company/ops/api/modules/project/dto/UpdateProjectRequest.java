package com.company.ops.api.modules.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateProjectRequest(
    @NotBlank @Size(max = 180) String name,
    @NotBlank @Size(max = 300) String siteAddress,
    @NotNull @DecimalMin("0") BigDecimal contractAmount,
    @NotNull LocalDate plannedStartDate,
    @NotNull LocalDate plannedEndDate,
    LocalDate warrantyEndDate,
    @NotNull List<@Valid ProjectBudgetItemRequest> budgetItems
) {}
