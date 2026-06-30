package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseOrderResponse(
    UUID id,
    String code,
    UUID supplierId,
    String supplierName,
    UUID requestId,
    String requestCode,
    UUID partId,
    String partName,
    BigDecimal orderedQty,
    BigDecimal receivedQty,
    BigDecimal unitPrice,
    BigDecimal orderAmount,
    LocalDate expectedDeliveryDate,
    ProcurementCostType costType,
    UUID costTargetId,
    String costTargetCode,
    String costTargetName,
    PurchaseOrderStatus status
) {}
