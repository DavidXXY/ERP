package com.company.ops.api.modules.finance.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BatchPaymentExecutionResult(
    int successCount,
    int failedCount,
    List<BatchPaymentItemResult> items
) {
  public record BatchPaymentItemResult(
      UUID applicationId,
      boolean success,
      String paymentCode,
      BigDecimal totalAmount,
      String errorMessage
  ) {}
}
