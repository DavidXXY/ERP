package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateSupplierRequest(
    // auto-gen
    String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 60) String category,
    @Size(max = 60) String contactName,
    @Size(max = 30) String phone,
    @Size(max = 80) String settlementTerms,
    @Size(max = 80) String legalRepresentative,
    @Size(max = 80) String unifiedSocialCreditCode,
    @Size(max = 80) String registeredCapital,
    @Size(max = 240) String registeredAddress,
    @Size(max = 800) String businessScope,
    LocalDate licenseValidTo,
    LocalDate qualificationValidTo,
    @Size(max = 80) String taxpayerType,
    @Size(max = 120) String bankName,
    @Size(max = 120) String bankAccount,
    @Size(max = 40) String admissionStatus,
    @Size(max = 1000) String remark,
    SupplierRiskStatus riskStatus
) {}
