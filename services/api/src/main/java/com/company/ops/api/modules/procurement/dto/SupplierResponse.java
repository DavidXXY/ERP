package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import java.util.UUID;

public record SupplierResponse(
    UUID id,
    String code,
    String name,
    String category,
    String contactName,
    String phone,
    String settlementTerms,
    SupplierRiskStatus riskStatus
) {}
