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
    BigDecimal invoiceAmount,
    BigDecimal matchedInvoiceAmount,
    BigDecimal paidAmount,
    String matchStatus,
    String riskMessage
) {}
