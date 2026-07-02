package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplierRequest(
    // auto-gen
    String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 60) String category,
    @Size(max = 60) String contactName,
    @Size(max = 30) String phone,
    @Size(max = 80) String settlementTerms,
    SupplierRiskStatus riskStatus
) {}
