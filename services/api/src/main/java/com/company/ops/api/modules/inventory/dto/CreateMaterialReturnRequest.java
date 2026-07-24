package com.company.ops.api.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateMaterialReturnRequest(
    // auto-gen
    String code,
    @NotNull LocalDate returnDate,
    @NotBlank @Size(max = 80) String handlerName,
    @NotBlank @Size(max = 255) String reason,
    @NotEmpty List<@Valid MaterialReturnLineRequest> lines
) {}
