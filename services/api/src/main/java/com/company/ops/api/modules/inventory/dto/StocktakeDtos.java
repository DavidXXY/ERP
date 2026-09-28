package com.company.ops.api.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class StocktakeDtos {
  private StocktakeDtos() {}

  public record CreateStocktakeRequest(
      String code,
      @NotNull LocalDate countDate,
      List<UUID> partIds,
      @Size(max = 500) String remark
  ) {}

  public record UpdateStocktakeLineRequest(
      @NotNull @DecimalMin("0") BigDecimal actualQty,
      @Size(max = 300) String remark
  ) {}

  public record StocktakeLineResponse(
      UUID id, UUID partId, String partName, BigDecimal bookQty, BigDecimal actualQty,
      BigDecimal difference, BigDecimal unitCost, String remark
  ) {}

  public record StocktakeResponse(
      UUID id, String code, LocalDate countDate, String status, String remark,
      List<StocktakeLineResponse> lines
  ) {}
}