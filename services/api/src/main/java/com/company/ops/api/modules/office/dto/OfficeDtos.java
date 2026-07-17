package com.company.ops.api.modules.office.dto;

import com.company.ops.api.modules.office.domain.ApprovalStatus;
import com.company.ops.api.modules.office.domain.ApprovalType;
import com.company.ops.api.modules.office.domain.ExpenseStatus;
import com.company.ops.api.modules.office.domain.ExpenseType;
import com.company.ops.api.modules.office.domain.OutsourceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class OfficeDtos {
  private OfficeDtos() {}
  public record OfficeOverview(long pendingApprovals, BigDecimal pendingExpenseAmount, BigDecimal approvedExpenseAmount,
                               long activeOutsourceOrders, BigDecimal outsourceAmount, long unreadNotifications,
                               long documentCount) {}
  public record CreateApprovalRequest(// auto-gen
    String code, @NotNull ApprovalType approvalType,
                                      @NotBlank @Size(max=180) String title, @Size(max=64) String sourceNo,
                                      @DecimalMin("0.00") BigDecimal amount, @NotBlank @Size(max=80) String applicantName,
                                      @NotBlank @Size(max=1000) String content,
                                      @Size(max=120) String departmentName, @Size(max=80) String businessType,
                                      @Size(max=80) String projectCode, @Size(max=40) String supplierRisk,
                                      @Size(max=40) String customerLevel) {}
  public record ProcessApprovalRequest(@NotNull ApprovalStatus decision, @NotBlank @Size(max=500) String comment,
                                       @NotBlank @Size(max=80) String approverName) {}
  public record ApprovalTransferRequest(@NotNull UUID targetUserId, @NotBlank @Size(max=500) String comment,
                                        @NotBlank @Size(max=80) String operatorName) {}
  public record ApprovalAddSignRequest(@NotNull UUID targetUserId, @NotBlank @Size(max=500) String comment,
                                       @NotBlank @Size(max=80) String operatorName) {}
  public record ApprovalWithdrawRequest(@NotBlank @Size(max=500) String comment,
                                        @NotBlank @Size(max=80) String operatorName) {}
  public record ApprovalReturnRequest(@NotBlank @Size(max=500) String comment,
                                      @NotBlank @Size(max=80) String operatorName) {}
  public record ApprovalResubmitRequest(@NotBlank @Size(max=500) String comment,
                                        @NotBlank @Size(max=80) String applicantName) {}
  public record ApprovalActionResponse(UUID id, ApprovalStatus decision, String operatorName, String comment,
                                       String actionType, Integer stepNo, OffsetDateTime createdAt) {}
  public record ApprovalRuntimeNodeResponse(UUID id, Integer stepNo, String nodeStatus, String approvalMode,
                                            String stepPolicy, String assigneeType, UUID assigneeId,
                                            String assigneeName, String sourceType, String sourceValue,
                                            String conditionText, Integer slaHours, OffsetDateTime dueAt,
                                            OffsetDateTime remindedAt, OffsetDateTime escalatedAt,
                                            OffsetDateTime completedAt, String approverName,
                                            String approvalComment) {}
  public record ApprovalResponse(UUID id, String code, ApprovalType approvalType, String title, String sourceNo,
                                 BigDecimal amount, ApprovalStatus status, String applicantName, String content,
                                 String approverName, String approvalComment, OffsetDateTime processedAt,
                                 OffsetDateTime createdAt, String departmentName, String businessType, String projectCode,
                                 String supplierRisk, String customerLevel, String approvalMode, Integer currentStep,
                                 Integer totalSteps, String currentApproverName, String matchedRuleText,
                                 Integer approvalConfigVersion, String approvalPlanSnapshot,
                                 Object sourceDetail,
                                 List<ApprovalRuntimeNodeResponse> nodes,
                                 List<ApprovalActionResponse> actions) {}
  public record CreateExpenseRequest(// auto-gen
    String code, UUID claimantId,
                                     @NotBlank @Size(max=80) String claimantName, UUID projectId, UUID workOrderId,
                                     @NotNull ExpenseType expenseType, @NotNull @DecimalMin("0.01") BigDecimal amount,
                                     @NotNull LocalDate expenseDate, @NotBlank @Size(max=500) String description) {}
  public record ExpenseResponse(UUID id, String code, UUID claimantId, String claimantName, UUID projectId,
                                String projectCode, UUID workOrderId, String workOrderCode, ExpenseType expenseType,
                                BigDecimal amount, LocalDate expenseDate, String description, ExpenseStatus status,
                                UUID approvalRequestId) {}
  public record CreateOutsourceRequest(// auto-gen
    String code, @NotNull UUID supplierId,
                                       UUID projectId, UUID workOrderId, @NotBlank @Size(max=100) String serviceType,
                                       @NotBlank @Size(max=800) String description,
                                       @NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull LocalDate plannedDate,
                                       @NotBlank @Size(max=80) String applicantName) {}
  public record CompleteOutsourceRequest(@NotBlank @Size(max=500) String acceptanceNote) {}
  public record OutsourceResponse(UUID id, String code, UUID supplierId, String supplierName, UUID projectId,
                                  String projectCode, UUID workOrderId, String workOrderCode, String serviceType,
                                  String description, BigDecimal amount, LocalDate plannedDate, OutsourceStatus status,
                                  UUID approvalRequestId, String acceptanceNote) {}
  public record DocumentResponse(UUID id, String bizType, UUID bizId, String fileName, String contentType,
                                 Long sizeBytes, OffsetDateTime createdAt) {}
  public record NotificationResponse(UUID id, String type, String title, String content, String relatedType,
                                     UUID relatedId, boolean read, OffsetDateTime readAt, OffsetDateTime createdAt) {}
  public record AuditResponse(UUID id, String username, String httpMethod, String requestPath,
                              Integer responseStatus, String clientIp, Long durationMs, String queryString,
                              String operationType, String bizModule, String bizObject, OffsetDateTime createdAt) {}
  public record SupplierOption(UUID id, String code, String name) {}
  public record ProjectOption(UUID id, String code, String name) {}
  public record WorkOrderOption(UUID id, String code, String title) {}
  public record UserOption(UUID id, String displayName, boolean enabled) {}
  public record OfficeReferenceResponse(List<SupplierOption> suppliers, List<ProjectOption> projects,
                                        List<WorkOrderOption> workOrders, List<UserOption> users) {}
  public record TodoItemResponse(String type, UUID id, String title, String subtitle, BigDecimal amount,
                                 String priority, String route, OffsetDateTime createdAt) {}
  public record WarningItemResponse(String type, UUID id, String title, String content, String severity,
                                    String route, OffsetDateTime createdAt) {}
  public record WorkbenchResponse(List<TodoItemResponse> todos, List<WarningItemResponse> warnings,
                                  long pendingTodoCount, long highSeverityWarningCount) {}
}
