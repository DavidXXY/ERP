package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.PayableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProcurementPayableResponse(
    UUID id,
    String code,
    UUID supplierId,
    String supplierName,
    UUID orderId,
    String orderCode,
    UUID receiptId,
    BigDecimal amount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    LocalDate dueDate,
    PayableStatus status
) {
}
