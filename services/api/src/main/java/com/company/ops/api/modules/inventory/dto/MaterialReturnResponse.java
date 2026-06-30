package com.company.ops.api.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MaterialReturnResponse(
    UUID id,
    String code,
    UUID issueId,
    String issueCode,
    UUID projectId,
    String projectCode,
    String projectName,
    LocalDate returnDate,
    String handlerName,
    BigDecimal totalAmount,
    List<MaterialReturnLineResponse> lines
) {}
