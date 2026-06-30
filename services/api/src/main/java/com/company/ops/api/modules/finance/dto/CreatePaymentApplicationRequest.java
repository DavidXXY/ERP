package com.company.ops.api.modules.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePaymentApplicationRequest(
    String code, // auto-generated if null
    @NotNull UUID payableId,
    @NotNull @Positive BigDecimal requestedAmount,
    @NotNull LocalDate requestedDate,
    @NotBlank @Size(max = 80) String applicantName,
    @NotBlank @Size(max = 300) String purpose
) {}
