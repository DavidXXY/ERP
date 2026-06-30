package com.company.ops.api.modules.inventory.dto;

import com.company.ops.api.modules.inventory.domain.InventoryIssueStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MaterialIssueResponse(
    UUID id,
    String code,
    UUID projectId,
    String projectCode,
    String projectName,
    LocalDate issueDate,
    String receiverName,
    String purpose,
    BigDecimal totalAmount,
    InventoryIssueStatus status,
    List<MaterialIssueLineResponse> lines
) {}
