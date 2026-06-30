package com.company.ops.api.modules.procurement.dto;

import java.util.UUID;

public record ProcurementCostTargetOptionResponse(
    UUID id,
    String code,
    String name
) {}
