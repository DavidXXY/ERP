package com.company.ops.api.modules.finance.dto;

import com.company.ops.api.modules.finance.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ExecutePaymentRequest(
    String paymentCode, // auto-generated if null
    @NotNull LocalDate paidDate,
    @NotNull PaymentMethod paymentMethod,
    @NotBlank @Size(max = 100) String bankReference,
    @NotBlank @Size(max = 80) String payerName
) {}
