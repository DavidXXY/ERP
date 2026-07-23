package com.company.ops.api.modules.procurement.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ImportPurchaseRequestBatchResponse(
    UUID batchId,
    String batchCode,
    String batchName,
    int totalLines,
    BigDecimal totalAmount,
    List<PurchaseRequestResponse> items
) {}

