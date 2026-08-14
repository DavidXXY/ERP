package com.company.ops.api.modules.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ExecutePaymentRequest(
    String paymentCode, // auto-generated if null
    @NotEmpty List<@Valid PaymentSplit> payments
) {}
