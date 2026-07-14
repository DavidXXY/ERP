package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class ApprovalConfigDtos {
  private ApprovalConfigDtos() {}
  public record ApprovalConfigResponse(UUID id, String flowCode, String flowName, String assigneeType,
                                       UUID userId, UUID roleId, String assigneeName, int versionNo,
                                       String dynamicAssignee, String autoAction, Integer slaHours,
                                       UUID escalationRoleId, String escalationRoleName, String stepPolicy,
                                       String publishStatus,
                                       String approvalMode, int sequenceNo, String conditionType,
                                       BigDecimal minAmount, BigDecimal maxAmount, String departmentName,
                                       String businessType, String projectCode, String supplierRisk,
                                       String customerLevel, int priority, String remark, boolean enabled) {}
  public record CreateApprovalConfigRequest(@NotBlank @Size(max = 64) String flowCode,
                                            @NotBlank @Size(max = 120) String flowName,
                                            @NotBlank String assigneeType,
                                            UUID userId, UUID roleId, String dynamicAssignee,
                                            String autoAction, Integer slaHours, UUID escalationRoleId,
                                            String stepPolicy,
                                            @NotBlank String approvalMode,
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
                                            UUID userId, UUID roleId, String dynamicAssignee,
                                            String autoAction, Integer slaHours, UUID escalationRoleId,
                                            String stepPolicy,
                                            @NotBlank String approvalMode,
                                            int sequenceNo, String conditionType, BigDecimal minAmount,
                                            BigDecimal maxAmount, @Size(max = 120) String departmentName,
                                            @Size(max = 80) String businessType,
                                            @Size(max = 80) String projectCode,
                                            @Size(max = 40) String supplierRisk,
                                            @Size(max = 40) String customerLevel,
                                            Integer priority,
                                            @Size(max = 500) String remark,
                                            Boolean enabled) {}
  public record PreviewApprovalFlowRequest(@NotBlank @Size(max = 64) String flowCode,
                                           BigDecimal amount,
                                           @Size(max = 120) String departmentName,
                                           @Size(max = 80) String businessType,
                                           @Size(max = 80) String projectCode,
                                           @Size(max = 40) String supplierRisk,
                                           @Size(max = 40) String customerLevel) {}
  public record ApprovalFlowPreviewResponse(String flowCode, String flowName, String approvalMode,
                                            int totalSteps, int versionNo, String ruleText,
                                            java.util.List<ApprovalFlowPreviewStep> steps) {}
  public record ApprovalFlowPreviewStep(int stepNo, java.util.List<String> assignees,
                                        java.util.List<String> conditions, Integer slaHours,
                                        String escalationRoleName, boolean autoApproved) {}
  public record CopyApprovalFlowRequest(@NotBlank @Size(max = 64) String sourceFlowCode,
                                        @NotBlank @Size(max = 64) String targetFlowCode,
                                        @NotBlank @Size(max = 120) String targetFlowName,
                                        boolean overwrite) {}
  public record ApprovalFlowDiagnostic(String flowCode, String flowName, String severity, String message) {}
  public record BatchPreviewApprovalFlowRequest(@NotNull java.util.List<PreviewApprovalFlowRequest> items) {}
  public record ApprovalFlowVersionResponse(String flowCode, String flowName, int versionNo, long ruleCount,
                                            String publishStatus) {}
}
