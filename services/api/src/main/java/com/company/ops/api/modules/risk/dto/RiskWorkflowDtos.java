package com.company.ops.api.modules.risk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class RiskWorkflowDtos {
  private RiskWorkflowDtos() {}

  public record RiskWorkflowResponse(
      String riskKey,
      String status,
      String owner,
      String note,
      String reason,
      String updatedByName,
      OffsetDateTime processedAt,
      OffsetDateTime updatedAt
  ) {}

  public record RiskWorkflowActionResponse(
      String riskKey,
      String fromStatus,
      String toStatus,
      String operatorName,
      String owner,
      String note,
      String reason,
      OffsetDateTime createdAt
  ) {}

  public record RiskItemResponse(
      String key,
      String module,
      String moduleName,
      String title,
      String subject,
      String description,
      String severity,
      String status,
      BigDecimal amount,
      String date,
      String route,
      String ruleCode,
      Integer slaHours,
      OffsetDateTime dueAt,
      boolean slaOverdue,
      RiskWorkflowResponse workflow
  ) {}

  public record RiskRuleConfigResponse(
      UUID id,
      String ruleCode,
      String name,
      String module,
      boolean enabled,
      BigDecimal highThreshold,
      BigDecimal mediumThreshold,
      Integer warningDays,
      Integer slaHours,
      String defaultOwner,
      String escalationOwner,
      String remark
  ) {}

  public record UpdateRiskRuleConfigRequest(
      @NotBlank @Size(max = 80) String ruleCode,
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 40) String module,
      Boolean enabled,
      BigDecimal highThreshold,
      BigDecimal mediumThreshold,
      Integer warningDays,
      Integer slaHours,
      @Size(max = 80) String defaultOwner,
      @Size(max = 80) String escalationOwner,
      @Size(max = 500) String remark
  ) {}

  public record BatchUpdateRiskWorkflowRequest(
      @NotEmpty List<@NotBlank @Size(max = 180) String> riskKeys,
      @NotBlank @Size(max = 32) String status,
      @Size(max = 80) String owner,
      @Size(max = 1000) String note,
      @Size(max = 500) String reason
  ) {}

  public record RiskTrendPointResponse(
      LocalDate date,
      long totalCount,
      long highCount,
      long overdueCount,
      long closedCount,
      BigDecimal amount
  ) {}

  public record RiskModuleSummaryResponse(
      String module,
      String moduleName,
      long totalCount,
      long highCount,
      long overdueCount,
      long slaOverdueCount,
      BigDecimal amount
  ) {}

  public record RiskSummaryResponse(
      long totalCount,
      long highCount,
      long overdueCount,
      long slaOverdueCount,
      long closedCount,
      BigDecimal amount,
      List<RiskModuleSummaryResponse> modules,
      List<RiskTrendPointResponse> trends
  ) {}

  public record UpdateRiskWorkflowRequest(
      @NotBlank @Size(max = 180) String riskKey,
      @NotBlank @Size(max = 32) String status,
      @Size(max = 80) String owner,
      @Size(max = 1000) String note,
      @Size(max = 500) String reason
  ) {}
}
