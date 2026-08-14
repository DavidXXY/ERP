package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateReplenishmentRequestRequest(
    @NotNull ProcurementCostType costType,
    UUID projectId,
    UUID departmentId,
    String reason,
    LocalDate expectedDate,
    @NotEmpty List<@Valid Line> lines
) {
  public record Line(
      @NotNull UUID partId,
      @NotNull @DecimalMin("0.01") BigDecimal quantity,
      @DecimalMin("0") BigDecimal unitPrice,
      String reason,
      LocalDate expectedDate
  ) {}
}
