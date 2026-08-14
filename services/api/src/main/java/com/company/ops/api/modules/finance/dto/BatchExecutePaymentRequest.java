package com.company.ops.api.modules.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BatchExecutePaymentRequest(
    @NotEmpty List<@Valid Item> items
) {
  public record Item(
      @NotNull UUID applicationId,
      @Valid @NotNull ExecutePaymentRequest payment
  ) {}
}
