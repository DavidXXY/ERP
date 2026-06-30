package com.company.ops.api.modules.finance.dto;

import com.company.ops.api.modules.finance.domain.PaymentApplicationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentApplicationResponse(
    UUID id,
    String code,
    UUID payableId,
    String payableCode,
    UUID supplierId,
    String supplierName,
    BigDecimal requestedAmount,
    LocalDate requestedDate,
    String applicantName,
    String purpose,
    PaymentApplicationStatus status,
    String approvalComment,
    String approverName,
    OffsetDateTime approvedAt,
    UUID paymentId,
    String paymentCode
) {}
