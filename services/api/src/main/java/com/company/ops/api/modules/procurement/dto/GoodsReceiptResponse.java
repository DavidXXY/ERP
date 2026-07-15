package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
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
    BigDecimal taxRate,
    BigDecimal amount,
    LocalDate receivedDate,
    String deliveryNo,
    String receiverName,
    ProcurementCostType costType,
    UUID costTargetId,
    String costTargetCode,
    String costTargetName
) {
}
