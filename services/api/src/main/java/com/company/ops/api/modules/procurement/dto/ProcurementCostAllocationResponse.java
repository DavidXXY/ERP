package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProcurementCostAllocationResponse(
    UUID id,
    UUID orderId,
    String orderCode,
    UUID receiptId,
    String receiptCode,
    ProcurementCostType costType,
    UUID costTargetId,
    String costTargetCode,
    String costTargetName,
    String partName,
    BigDecimal amount,
    LocalDate incurredDate
) {}
