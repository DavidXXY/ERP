package com.company.ops.api.modules.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ReportingDtos {
  private ReportingDtos() {}

  public record ReportCreateRequest(
      @NotBlank @Size(max = 16) String reportType,
      @Size(max = 24) String reportCategory,
      UUID projectId,
      UUID additionalProjectId,
      BigDecimal hours,
      @NotNull LocalDate reportDate,
      @NotBlank @Size(max = 20000) String content,
      @Size(max = 1000) String progressSummary,
      @Size(max = 2000) String planNext,
      @Size(max = 1000) String issue,
      List<UUID> ccUserIds
  ) {}

  public record ReportResponse(
      UUID id, String reportType, String reportCategory, UUID projectId, String projectName,
      UUID additionalProjectId, String additionalProjectName,
      BigDecimal hours, String reportStatus, UUID approvalId,
      LocalDate reportDate, String content,
      String progressSummary, String planNext, String issue,
      UUID employeeId, String employeeName, String departmentName,
      List<UUID> ccUserIds, List<String> ccNames,
      OffsetDateTime createdAt, OffsetDateTime updatedAt
  ) {}

  public record ReportUserOption(UUID id, String name) {}

  public record ReportProjectOption(UUID id, String code, String name) {}
}
