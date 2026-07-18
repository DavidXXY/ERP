package com.company.ops.api.common.exception;

public class RateLimitException extends BusinessException {
  public RateLimitException(String message) {
    super(message);
  }
}
