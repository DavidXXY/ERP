package com.company.ops.api.modules.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class RevenueRecognitionDtos {
  private RevenueRecognitionDtos() {}

  public record CreateMilestoneRequest(
      @NotNull UUID contractId,
      @NotBlank @Size(max = 160) String name,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      LocalDate plannedDate,
      @Size(max = 500) String remark
  ) {}

  public record MilestoneResponse(
      UUID id, UUID contractId, String name, BigDecimal amount, LocalDate plannedDate,
      String status, LocalDate recognizedDate, String recognizedBy, String remark
  ) {}

  public record RecognizeMilestoneRequest(
      @NotNull LocalDate recognizeDate
  ) {}

  public record RecognitionResponse(
      UUID id, String code, UUID contractId, UUID milestoneId, BigDecimal amount,
      LocalDate recognizeDate, String recognizedBy, String remark
  ) {}
}