package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import jakarta.validation.constraints.NotBlank;

public record CreateSupplierRequest(
    // auto-gen
    String code,
    @NotBlank String name,
    String category,
    String contactName,
    String phone,
    String settlementTerms,
    SupplierRiskStatus riskStatus
) {}
