package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewSupplierAdmissionRequest(
    @NotBlank @Size(max = 20) String decision,
    @Size(max = 500) String comment
) {}
