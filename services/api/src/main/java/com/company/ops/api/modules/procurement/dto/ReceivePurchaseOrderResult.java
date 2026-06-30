package com.company.ops.api.modules.procurement.dto;

import java.math.BigDecimal;

public record ReceivePurchaseOrderResult(
    PurchaseOrderResponse order,
    GoodsReceiptResponse receipt,
    ProcurementPayableResponse payable,
    ProcurementCostAllocationResponse costAllocation,
    BigDecimal currentStockQty
) {
}
