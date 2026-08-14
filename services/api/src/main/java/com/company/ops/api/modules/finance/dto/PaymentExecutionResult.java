package com.company.ops.api.modules.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record PaymentExecutionResult(
    String paymentCode,
    BigDecimal totalAmount,
    List<PaymentRecordResponse> records
) {}
