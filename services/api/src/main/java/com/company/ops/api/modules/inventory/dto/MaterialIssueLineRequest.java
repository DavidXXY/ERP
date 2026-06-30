package com.company.ops.api.modules.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record MaterialIssueLineRequest(
    @NotNull UUID partId,
    @NotNull @DecimalMin("0.01") BigDecimal quantity
) {}
