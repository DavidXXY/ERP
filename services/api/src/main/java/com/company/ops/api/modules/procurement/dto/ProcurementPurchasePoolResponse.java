package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProcurementPurchasePoolResponse(
    int totalGroups,
    int totalRequests,
    BigDecimal totalRemainingQuantity,
    BigDecimal totalEstimatedAmount,
    List<Group> groups
) {
  public record Group(
      String groupKey,
      UUID partId,
      String partCode,
      String partName,
      int requestCount,
      int costTargetCount,
      BigDecimal totalRemainingQuantity,
      BigDecimal totalEstimatedAmount,
      LocalDate earliestExpectedDate,
      List<Item> items
  ) {}

  public record Item(
      UUID requestId,
      String requestCode,
      UUID batchId,
      String batchCode,
      String batchName,
      Integer lineNo,
      String requesterName,
      BigDecimal requestedQuantity,
      BigDecimal orderedQuantity,
      BigDecimal remainingQuantity,
      BigDecimal estimatedUnitPrice,
      BigDecimal estimatedAmount,
      BigDecimal taxRate,
      LocalDate expectedDate,
      ProcurementCostType costType,
      UUID costTargetId,
      String costTargetCode,
      String costTargetName,
      String reason,
      OffsetDateTime approvedAt
  ) {}
}

