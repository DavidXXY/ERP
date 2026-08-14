package com.company.ops.api.modules.finance.dto;

import com.company.ops.api.modules.procurement.domain.PayableAdjustmentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayableAdjustmentResponse(
    UUID id,
    String code,
    UUID payableId,
    UUID orderId,
    UUID supplierId,
    PayableAdjustmentType adjustmentType,
    BigDecimal amount,
    String reason,
    String operatorName,
    LocalDate appliedAt,
    String status,
    String source,
    UUID sourceId
) {}
