package com.company.ops.api.modules.procurement.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FrameworkAgreementQuoteResponse(
    UUID agreementId,
    String agreementCode,
    String agreementTitle,
    BigDecimal unitPrice,
    BigDecimal taxRate
) {}
