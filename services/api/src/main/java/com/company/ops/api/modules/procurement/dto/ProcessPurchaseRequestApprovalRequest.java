package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessPurchaseRequestApprovalRequest(
    @NotNull ApprovalStatus decision,
    @NotBlank @Size(max = 500) String comment,
    @NotBlank @Size(max = 80) String approverName
) {
}
