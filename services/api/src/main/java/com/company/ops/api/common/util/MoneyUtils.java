package com.company.ops.api.common.util;

import java.math.BigDecimal;

public final class MoneyUtils {

  private MoneyUtils() {}

  /**
   * 空安全 BigDecimal，null 视为 0
   */
  public static BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
