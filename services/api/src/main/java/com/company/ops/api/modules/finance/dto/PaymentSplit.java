package com.company.ops.api.modules.finance.dto;

import com.company.ops.api.modules.finance.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentSplit(
    UUID payableId, // 空时取申请/应付单主应付
    @NotNull @Positive BigDecimal amount,
    @NotNull LocalDate paidDate,
    @NotNull PaymentMethod paymentMethod,
    @NotBlank @Size(max = 100) String bankReference,
    @Size(max = 500) String note
) {}
