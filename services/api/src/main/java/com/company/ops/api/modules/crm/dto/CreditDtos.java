package com.company.ops.api.modules.crm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public final class CreditDtos {
  private CreditDtos() {}

  public record CreditInfoResponse(
      UUID customerId, String customerName, BigDecimal creditLimit, boolean creditBlocked,
      BigDecimal outstandingReceivables, BigDecimal openOrderAmount, BigDecimal totalExposure
  ) {}

  public record SetCreditLimitRequest(
      @NotNull @DecimalMin("0") BigDecimal creditLimit,
      boolean creditBlocked
  ) {}
}