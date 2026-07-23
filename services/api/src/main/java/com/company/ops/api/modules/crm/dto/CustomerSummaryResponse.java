package com.company.ops.api.modules.crm.dto;

import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record CustomerSummaryResponse(
    UUID id,
    String code,
    String name,
    String industry,
    CustomerLevel level,
    String ownerName,
    String paymentHabit,
    RiskStatus riskStatus,
    int contactCount,
    int siteCount,
    String primaryContact,
    BigDecimal signedOrderAmount,
    BigDecimal paidAmount,
    BigDecimal pendingAmount
) {
}
