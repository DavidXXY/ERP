package com.company.ops.api.modules.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record PrepareChildProjectRequest(
    @NotBlank @Size(max = 300) String siteAddress,
    @NotNull LocalDate plannedStartDate,
    @NotNull LocalDate plannedEndDate,
    LocalDate warrantyEndDate,
    @NotEmpty List<@Valid ProjectBudgetItemRequest> budgetItems
) {}
