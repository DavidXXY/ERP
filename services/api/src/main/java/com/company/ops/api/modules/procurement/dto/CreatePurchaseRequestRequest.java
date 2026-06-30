package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePurchaseRequestRequest(
    // auto-gen
    String code,
    @NotBlank String requesterName,
    UUID partId,
    String partName,
    @NotNull @DecimalMin("0.01") BigDecimal quantity,
    LocalDate expectedDate,
    String reason,
    @NotNull ProcurementCostType costType,
    UUID projectId,
    UUID departmentId
) {}
