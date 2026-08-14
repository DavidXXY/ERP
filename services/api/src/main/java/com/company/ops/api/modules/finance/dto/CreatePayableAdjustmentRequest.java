package com.company.ops.api.modules.finance.dto;

import com.company.ops.api.modules.procurement.domain.PayableAdjustmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePayableAdjustmentRequest(
    @NotNull PayableAdjustmentType adjustmentType,
    @NotNull @Positive BigDecimal amount,
    @Size(max = 500) String reason,
    LocalDate appliedAt
) {}
