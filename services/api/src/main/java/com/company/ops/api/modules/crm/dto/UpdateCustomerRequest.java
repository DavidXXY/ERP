package com.company.ops.api.modules.crm.dto;

import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateCustomerRequest(
    @NotBlank String name,
    @NotBlank String industry,
    @NotNull CustomerLevel level,
    @NotBlank String ownerName,
    String paymentHabit,
    RiskStatus riskStatus,
    String riskNote,
    @Valid CreateCustomerRequest.InvoiceRequest invoice,
    @Valid List<CreateCustomerRequest.ContactRequest> contacts,
    @Valid List<CreateCustomerRequest.SiteRequest> sites
) {
}
