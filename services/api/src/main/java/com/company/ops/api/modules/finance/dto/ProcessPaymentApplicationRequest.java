package com.company.ops.api.modules.finance.dto;

import com.company.ops.api.modules.finance.domain.PaymentApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessPaymentApplicationRequest(
    @NotNull PaymentApplicationStatus decision,
    @NotBlank @Size(max = 500) String comment,
    @NotBlank @Size(max = 80) String approverName
) {}
