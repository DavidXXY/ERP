package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmShipmentRequest(
    @NotBlank @Pattern(regexp = "CONFIRMED|REJECTED") String action,
    @Size(max = 500) String comment
) {}
