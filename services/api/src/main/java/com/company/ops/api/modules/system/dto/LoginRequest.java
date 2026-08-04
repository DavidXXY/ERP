package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password,
    @Size(max = 32) String mfaCode
) {
}
