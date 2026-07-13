package com.company.ops.api.modules.procurement.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcurementMatchingResponse(
    UUID orderId,
    String orderCode,
    String supplierName,
    String partName,
    BigDecimal orderedQty,
    BigDecimal receivedQty,
    BigDecimal orderAmount,
    BigDecimal receiptAmount,
    BigDecimal payableAmount,
    BigDecimal paidAmount,
    String matchStatus,
    String riskMessage
) {}
