package com.company.ops.api.modules.maintenance.dto;

import com.company.ops.api.modules.maintenance.domain.WorkOrderPriority;
import com.company.ops.api.modules.maintenance.domain.WorkOrderSource;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.domain.WorkOrderType;
import com.company.ops.api.modules.maintenance.domain.EquipmentStatus;
import com.company.ops.api.modules.maintenance.domain.WorkOrderAttachmentCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class MaintenanceDtos {

  private MaintenanceDtos() {}

  // -- Dashboard --
  public record DashboardResponse(long open, long closed, long urgent, long equipmentCount) {}

  // -- Reference Data --
  public record ReferenceDataResponse(
      List<CustomerOption> customers, List<EquipmentOption> equipment, List<ContractOption> contracts) {}
  public record CustomerOption(UUID id, String name) {}
  public record EquipmentOption(UUID id, String code, String name) {}
  public record ContractOption(UUID id, String name) {}
  public record AssigneeOption(UUID id, String displayName) {}

  // -- Work Order --
  public record WorkOrderResponse(
      UUID id, String code, String title, String description,
      UUID customerId, String customerName,
      UUID equipmentId, String equipmentCode, String equipmentName,
      WorkOrderType workType, WorkOrderPriority priority,
      WorkOrderSource source, WorkOrderStatus status,
      UUID assigneeId, String assigneeName,
      BigDecimal laborHours, BigDecimal laborCost, BigDecimal costAmount,
      BigDecimal billableAmount, BigDecimal actualCost,
      LocalDate plannedDate, String siteAddress,
      OffsetDateTime assignmentAcceptedAt,
      OffsetDateTime checkInAt, String checkInLocation,
      BigDecimal checkInLatitude, BigDecimal checkInLongitude, BigDecimal checkInAccuracy,
      OffsetDateTime startedAt,
      OffsetDateTime completedAt, OffsetDateTime acceptedAt,
      OffsetDateTime createdAt, OffsetDateTime updatedAt,
      String serviceResult, String customerSigner, String remarks,
      List<AttachmentResponse> attachments,
      List<MaterialResponse> materials,
      List<StatusLogResponse> statusLogs) {}

  public record CreateWorkOrderRequest(
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 180) String title,
      @jakarta.validation.constraints.Size(max = 1000) String description,
      UUID customerId, UUID equipmentId,
      @jakarta.validation.constraints.NotNull WorkOrderType workType,
      @jakarta.validation.constraints.NotNull WorkOrderPriority priority,
      @jakarta.validation.constraints.NotNull WorkOrderSource source) {}

  public record AssignWorkOrderRequest(@jakarta.validation.constraints.NotNull UUID assigneeId,
      @jakarta.validation.constraints.NotBlank String assigneeName) {}

  public record CheckInRequest(
      @jakarta.validation.constraints.NotNull OffsetDateTime checkInAt,
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 300) String checkInLocation) {}

  public record MobileOperationRequest(
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String operationId) {}

  public record MobileCheckInRequest(
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String operationId,
      @jakarta.validation.constraints.NotNull OffsetDateTime checkInAt,
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 300) String checkInLocation,
      @jakarta.validation.constraints.NotNull BigDecimal latitude,
      @jakarta.validation.constraints.NotNull BigDecimal longitude,
      BigDecimal accuracy) {}

  public record CompleteWorkOrderRequest(
      BigDecimal laborHours, BigDecimal laborCost, BigDecimal materialCost,
      BigDecimal travelCost, BigDecimal outsourcingCost,
      BigDecimal costAmount, BigDecimal billableAmount,
      String serviceResult, String remarks) {}

  public record MobileCompleteWorkOrderRequest(
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String operationId,
      BigDecimal laborHours, BigDecimal laborCost, BigDecimal materialCost,
      BigDecimal travelCost, BigDecimal outsourcingCost,
      BigDecimal costAmount, BigDecimal billableAmount,
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 1500) String serviceResult,
      @jakarta.validation.constraints.Size(max = 500) String remarks,
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 80) String customerSigner,
      List<@jakarta.validation.Valid MaterialRequest> materials) {}

  public record MaterialRequest(
      UUID partId,
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 160) String partName,
      @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0.01") BigDecimal quantity,
      @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0.00") BigDecimal unitCost,
      @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0.00") BigDecimal amount) {}

  public record MaterialResponse(
      UUID id, UUID partId, String partName, BigDecimal quantity, BigDecimal unitCost, BigDecimal amount) {}

  public record AttachmentResponse(
      UUID id, WorkOrderAttachmentCategory category, String fileName, String contentType,
      long fileSize, String uploadedBy, OffsetDateTime createdAt, String previewUrl) {}

  public record AcceptWorkOrderRequest(BigDecimal actualCost, String remarks) {}

  public record CloseWorkOrderRequest(String remarks) {}

  // -- Equipment --
  public record EquipmentResponse(
      UUID id, String code, String name,
      UUID customerId, String customerName,
      String category, String model, String serialNo,
      String siteAddress, LocalDate installedDate,
      LocalDate warrantyEndDate, Integer maintenanceCycleDays,
      LocalDate lastMaintenanceDate, LocalDate nextMaintenanceDate,
      EquipmentStatus status, long orderCount) {}

  public record CreateEquipmentRequest(
      @jakarta.validation.constraints.NotNull UUID customerId, UUID contractId,
      @jakarta.validation.constraints.Size(max = 64) String code,
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 160) String name,
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 80) String category,
      String model, String serialNo,
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 300) String siteAddress,
      LocalDate installedDate,
      LocalDate warrantyEndDate, Integer maintenanceCycleDays,
      LocalDate nextMaintenanceDate, String requiredCertificate,
      String notes) {}

  // -- Status Log --
  public record StatusLogResponse(
      UUID id, WorkOrderStatus fromStatus, WorkOrderStatus toStatus,
      String operatorName, String comment, OffsetDateTime createdAt) {}

  // -- Plan --
  public record PlanResponse(UUID id, String code, UUID assetId, String assetName, String name, String description,
      WorkOrderType workType, WorkOrderPriority priority,
      Integer cycleDays, Boolean autoGenerate, LocalDate nextRunDate,
      Boolean enabled) {}
  public record CreatePlanRequest(@jakarta.validation.constraints.NotNull UUID assetId,
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 180) String name,
      @jakarta.validation.constraints.Size(max = 1000) String description,
      WorkOrderType workType, WorkOrderPriority priority,
      @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(1) Integer cycleDays,
      Boolean autoGenerate, @jakarta.validation.constraints.NotNull LocalDate nextRunDate) {}
  public record GeneratePlanResponse(int generated) {}
  public record GeneratePlanRequest(UUID planId) {}

  // -- Certificate --
  public record CertificateResponse(UUID id, UUID userId, String employeeName, String certificateType, String certificateNo,
      LocalDate issueDate, LocalDate expiryDate, String issuingAuthority, String remark,
      long daysUntilExpiry) {}
  public record CreateCertificateRequest(@jakarta.validation.constraints.NotNull UUID userId,
      @jakarta.validation.constraints.NotBlank String certificateType,
      @jakarta.validation.constraints.NotBlank String certificateNo,
      LocalDate issueDate, @jakarta.validation.constraints.NotNull LocalDate expiryDate,
      String issuingAuthority, String remark) {}

  // -- Schedule --
  public record ScheduleResponse(UUID id, UUID orderId, String orderCode, String title,
      String engineerName, OffsetDateTime scheduledAt, OffsetDateTime checkInAt,
      String checkInLocation, OffsetDateTime startedAt, OffsetDateTime completedAt,
      WorkOrderStatus status) {}
  public record CreateScheduleRequest(@jakarta.validation.constraints.NotNull UUID orderId,
      @jakarta.validation.constraints.NotNull UUID engineerId,
      @jakarta.validation.constraints.NotNull OffsetDateTime scheduledAt) {}

  // -- Attendance --
  public record AttendanceResponse(UUID id, UUID orderId, String orderCode,
      UUID engineerId, String engineerName, OffsetDateTime checkInAt,
      String checkInLocation, OffsetDateTime checkOutAt) {}
}
