package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseRequestResponse(
    UUID id,
    String code,
    String requesterName,
    UUID partId,
    String partName,
    BigDecimal quantity,
    LocalDate expectedDate,
    String reason,
    PurchaseRequestStatus status,
    ApprovalStatus approvalStatus
) {}
