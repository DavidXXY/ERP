package com.company.ops.api.modules.procurement.dto;

import java.util.List;

public record ProcurementCostTargetOptionsResponse(
    List<ProcurementCostTargetOptionResponse> projects,
    List<ProcurementCostTargetOptionResponse> departments
) {}
