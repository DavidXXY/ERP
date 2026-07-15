package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PurchaseRequestResponse(
    UUID id,
    String code,
    String requesterName,
    UUID partId,
    String partName,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal taxRate,
    BigDecimal totalAmount,
    LocalDate expectedDate,
    String reason,
    ProcurementCostType costType,
    UUID costTargetId,
    String costTargetCode,
    String costTargetName,
    PurchaseRequestStatus status,
    ApprovalStatus approvalStatus,
    String lastApprovalComment,
    String lastApproverName,
    OffsetDateTime lastApprovalAt
) {}
