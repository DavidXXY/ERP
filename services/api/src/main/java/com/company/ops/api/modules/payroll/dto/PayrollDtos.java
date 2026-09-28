package com.company.ops.api.modules.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class PayrollDtos {
  private PayrollDtos() {}

  public record SalaryItemRequest(
      @NotNull UUID employeeId,
      @NotBlank @Size(max = 80) String name,
      @NotNull @DecimalMin("0") BigDecimal amount,
      @NotBlank @Size(max = 24) String itemType,
      boolean active,
      @Size(max = 300) String remark
  ) {}

  public record SalaryItemResponse(
      UUID id,
      UUID employeeId,
      String name,
      BigDecimal amount,
      String itemType,
      boolean active,
      String remark,
      OffsetDateTime createdAt
  ) {}

  public record RunPayrollRequest(@NotBlank @Size(max = 7) String period) {}

  public record ConfigRequest(
      @NotBlank @Size(max = 64) String configKey,
      @NotNull @DecimalMin("0") BigDecimal configValue,
      @Size(max = 300) String remark
  ) {}

  public record ConfigResponse(UUID id, String configKey, BigDecimal configValue, String remark) {}

  public record RunLineResponse(
      UUID id,
      UUID runId,
      UUID employeeId,
      String employeeName,
      BigDecimal baseSalary,
      BigDecimal allowances,
      BigDecimal bonus,
      BigDecimal deductions,
      BigDecimal socialInsurance,
      BigDecimal tax,
      BigDecimal netPay
  ) {}

  public record PayrollRunResponse(
      UUID id,
      String code,
      String period,
      LocalDate runDate,
      BigDecimal totalGross,
      BigDecimal totalSocial,
      BigDecimal totalTax,
      BigDecimal totalNet,
      String status,
      List<RunLineResponse> lines,
      OffsetDateTime createdAt
  ) {}

  public record SocialTaxLedgerResponse(
      String period,
      String code,
      BigDecimal totalSocial,
      BigDecimal totalTax,
      BigDecimal totalNet,
      String status
  ) {}
}
