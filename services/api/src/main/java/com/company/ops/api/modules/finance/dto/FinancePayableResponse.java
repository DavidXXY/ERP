package com.company.ops.api.modules.finance.dto;

import com.company.ops.api.modules.procurement.domain.PayableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FinancePayableResponse(
    UUID id,
    String code,
    UUID supplierId,
    String supplierName,
    UUID orderId,
    String orderCode,
    BigDecimal amount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    BigDecimal reservedAmount,
    BigDecimal availableAmount,
    LocalDate dueDate,
    PayableStatus status,
    boolean overdue
) {}
