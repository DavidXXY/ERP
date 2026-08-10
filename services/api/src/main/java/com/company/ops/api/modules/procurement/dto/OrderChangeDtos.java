package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class OrderChangeDtos {

  private OrderChangeDtos() {}

  public record CreateOrderChangeRequest(
      @Size(max = 24) String changeType,
      BigDecimal quantityAfter,
      BigDecimal unitPriceAfter,
      LocalDate expectedDateAfter,
      @NotBlank @Size(max = 500) String reason
  ) {}

  public record DecideOrderChangeRequest(
      @NotBlank String decision,
      @Size(max = 500) String comment
  ) {}

  public record OrderChangeResponse(
      UUID id,
      UUID orderId,
      String orderCode,
      String changeNo,
      String changeType,
      BigDecimal quantityBefore,
      BigDecimal quantityAfter,
      BigDecimal unitPriceBefore,
      BigDecimal unitPriceAfter,
      LocalDate expectedDateBefore,
      LocalDate expectedDateAfter,
      String reason,
      String status,
      String createdByName,
      String decidedByName,
      String decisionComment,
      Integer orderVersionBefore,
      Integer orderVersionAfter,
      OffsetDateTime appliedAt,
      OffsetDateTime createdAt
  ) {}
}
