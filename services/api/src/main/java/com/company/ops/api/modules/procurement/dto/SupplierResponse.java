package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierResponse(
    UUID id,
    String code,
    String name,
    String category,
    String contactName,
    String phone,
    String settlementTerms,
    String legalRepresentative,
    String unifiedSocialCreditCode,
    String registeredCapital,
    String registeredAddress,
    String businessScope,
    LocalDate licenseValidTo,
    LocalDate qualificationValidTo,
    String taxpayerType,
    String bankName,
    String bankAccount,
    String admissionStatus,
    String remark,
    SupplierRiskStatus riskStatus
) {}
