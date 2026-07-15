package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
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
    BigDecimal taxRate,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    LocalDate dueDate,
    ProcurementCostType costType,
    UUID costTargetId,
    String costTargetCode,
    String costTargetName,
    PayableStatus status
) {
}
