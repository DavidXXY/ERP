package com.company.ops.api.modules.crm.dto;

import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateCustomerRequest(
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String industry,
    @NotNull CustomerLevel level,
    @NotBlank String ownerName,
    String paymentHabit,
    RiskStatus riskStatus,
    String riskNote,
    @Valid InvoiceRequest invoice,
    @Valid List<ContactRequest> contacts,
    @Valid List<SiteRequest> sites
) {

  public record InvoiceRequest(
      String title,
      String taxNo,
      String bankName,
      String bankAccount,
      String registeredAddress,
      String registeredPhone
  ) {
  }

  public record ContactRequest(
      @NotBlank String name,
      String title,
      String phone,
      String email,
      boolean primaryContact
  ) {
  }

  public record SiteRequest(
      @NotBlank String name,
      @NotBlank String address
  ) {
  }
}

