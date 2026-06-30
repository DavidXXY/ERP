package com.company.ops.api.modules.procurement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoodsReceiptResponse(
    UUID id,
    String code,
    UUID orderId,
    String orderCode,
    UUID partId,
    String partName,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal amount,
    LocalDate receivedDate,
    String deliveryNo,
    String receiverName
) {
}
