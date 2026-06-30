package com.company.ops.api.modules.finance.dto;

import com.company.ops.api.modules.finance.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentRecordResponse(
    UUID id,
    String code,
    UUID applicationId,
    String applicationCode,
    UUID payableId,
    String payableCode,
    UUID supplierId,
    String supplierName,
    BigDecimal amount,
    LocalDate paidDate,
    PaymentMethod paymentMethod,
    String bankReference,
    String payerName
) {}
