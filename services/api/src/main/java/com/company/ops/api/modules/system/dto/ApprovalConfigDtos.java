package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class ApprovalConfigDtos {
  private ApprovalConfigDtos() {}
  public record ApprovalConfigResponse(UUID id, String flowCode, String flowName, String assigneeType,
                                       UUID userId, UUID roleId, String assigneeName,
                                       String approvalMode, int sequenceNo, String conditionType,
                                       BigDecimal minAmount, BigDecimal maxAmount, String departmentName,
                                       String businessType, String projectCode, String supplierRisk,
                                       String customerLevel, int priority, String remark, boolean enabled) {}
  public record CreateApprovalConfigRequest(@NotBlank @Size(max = 64) String flowCode,
                                            @NotBlank @Size(max = 120) String flowName,
                                            @NotBlank String assigneeType,
                                            UUID userId, UUID roleId, @NotBlank String approvalMode,
                                            int sequenceNo, String conditionType, BigDecimal minAmount,
                                            BigDecimal maxAmount, @Size(max = 120) String departmentName,
                                            @Size(max = 80) String businessType,
                                            @Size(max = 80) String projectCode,
                                            @Size(max = 40) String supplierRisk,
                                            @Size(max = 40) String customerLevel,
                                            Integer priority,
                                            @Size(max = 500) String remark) {}
  public record UpdateApprovalConfigRequest(@NotBlank @Size(max = 64) String flowCode,
                                            @NotBlank @Size(max = 120) String flowName,
                                            @NotBlank String assigneeType,
                                            UUID userId, UUID roleId, @NotBlank String approvalMode,
                                            int sequenceNo, String conditionType, BigDecimal minAmount,
                                            BigDecimal maxAmount, @Size(max = 120) String departmentName,
                                            @Size(max = 80) String businessType,
                                            @Size(max = 80) String projectCode,
                                            @Size(max = 40) String supplierRisk,
                                            @Size(max = 40) String customerLevel,
                                            Integer priority,
                                            @Size(max = 500) String remark,
                                            Boolean enabled) {}
}
