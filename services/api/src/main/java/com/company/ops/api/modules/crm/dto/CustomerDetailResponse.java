package com.company.ops.api.modules.crm.dto;

import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CustomerDetailResponse(
    UUID id,
    String code,
    String name,
    String industry,
    CustomerLevel level,
    String ownerName,
    String paymentHabit,
    RiskStatus riskStatus,
    String riskNote,
    Invoice invoice,
    List<Contact> contacts,
    List<Site> sites,
    List<Contract> contracts,
    List<Receivable> receivables,
    CustomerMetrics metrics
) {

  public record Invoice(
      String title,
      String taxNo,
      String bankName,
      String bankAccount,
      String registeredAddress,
      String registeredPhone
  ) {
  }

  public record Contact(
      UUID id,
      String name,
      String title,
      String phone,
      String email,
      boolean primaryContact
  ) {
  }

  public record Site(
      UUID id,
      String name,
      String address
  ) {
  }

  public record Contract(
      UUID id,
      String code,
      String projectName,
      String contractType,
      BigDecimal amount,
      LocalDate startDate,
      LocalDate endDate,
      String serviceCycle,
      ContractStatus status
  ) {
  }

  public record Receivable(
      UUID id,
      String code,
      String sourceNo,
      BigDecimal amount,
      LocalDate dueDate,
      ReceivableStatus status
  ) {
  }

  public record CustomerMetrics(
      int contractCount,
      BigDecimal contractAmount,
      BigDecimal outstandingAmount,
      BigDecimal settledAmount
  ) {
  }
}

