package com.company.ops.api.modules.finance.dto;

import java.math.BigDecimal;

public record FinanceOverviewResponse(
    BigDecimal receivableAmount,
    BigDecimal receivedAmount,
    BigDecimal receivableOutstanding,
    BigDecimal receivableOverdue,
    BigDecimal payableAmount,
    BigDecimal paidAmount,
    BigDecimal payableOutstanding,
    BigDecimal payableOverdue,
    BigDecimal netCashInflow,
    long pendingPaymentApplications
) {}
