package com.company.ops.api.modules.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelPayableRequest(
    @NotBlank @Size(max = 500) String reason
) {}
