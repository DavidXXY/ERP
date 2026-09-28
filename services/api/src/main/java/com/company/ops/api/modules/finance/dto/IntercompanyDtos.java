package com.company.ops.api.modules.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class IntercompanyDtos {
  private IntercompanyDtos() {}

  public record CreateIntercompanyRequest(
      String code,
      @NotNull UUID fromEntityId,
      @NotNull UUID toEntityId,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotBlank @Size(max = 24) String direction,
      @NotNull LocalDate transactionDate,
      @Size(max = 500) String reason
  ) {}

  public record IntercompanyResponse(
      UUID id, String code, UUID fromEntityId, String fromEntityName, UUID toEntityId, String toEntityName,
      BigDecimal amount, String direction, LocalDate transactionDate, String reason, String status
  ) {}
}