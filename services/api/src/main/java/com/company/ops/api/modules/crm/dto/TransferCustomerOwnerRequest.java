package com.company.ops.api.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TransferCustomerOwnerRequest(
    @NotBlank @Size(max = 80) String ownerName
) {
}
