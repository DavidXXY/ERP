package com.company.ops.api.modules.maintenance.dto;

import com.company.ops.api.modules.maintenance.domain.WorkOrderPriority;
import com.company.ops.api.modules.maintenance.domain.WorkOrderSource;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.domain.WorkOrderType;
import com.company.ops.api.modules.maintenance.domain.EquipmentStatus;
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
      LocalDate plannedDate,
      OffsetDateTime startedAt,
      OffsetDateTime completedAt, OffsetDateTime acceptedAt,
      OffsetDateTime createdAt, OffsetDateTime updatedAt,
      String remarks,
      List<StatusLogResponse> statusLogs) {}

  public record CreateWorkOrderRequest(
      String title, String description, UUID customerId, UUID equipmentId,
      WorkOrderType workType, WorkOrderPriority priority, WorkOrderSource source) {}

  public record AssignWorkOrderRequest(UUID assigneeId, String assigneeName) {}

  public record CheckInRequest(OffsetDateTime checkInAt, String checkInLocation) {}

  public record CompleteWorkOrderRequest(
      BigDecimal laborHours, BigDecimal laborCost, BigDecimal materialCost,
      BigDecimal travelCost, BigDecimal outsourcingCost,
      BigDecimal costAmount, BigDecimal billableAmount,
      String serviceResult, String remarks) {}

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
      UUID customerId, UUID contractId, String code, String name,
      String category, String model, String serialNo,
      String siteAddress, LocalDate installedDate,
      LocalDate warrantyEndDate, Integer maintenanceCycleDays,
      LocalDate nextMaintenanceDate, String requiredCertificate,
      String notes) {}

  // -- Status Log --
  public record StatusLogResponse(
      UUID id, WorkOrderStatus fromStatus, WorkOrderStatus toStatus,
      String operatorName, String comment, OffsetDateTime createdAt) {}

  // -- Plan --
  public record PlanResponse(UUID id, String name, String description,
      WorkOrderType workType, WorkOrderPriority priority,
      Integer cycleDays, Boolean autoGenerate, LocalDate nextRunDate,
      Boolean enabled) {}
  public record CreatePlanRequest(String name, String description,
      WorkOrderType workType, WorkOrderPriority priority,
      Integer cycleDays, Boolean autoGenerate) {}
  public record GeneratePlanResponse(int generated) {}
  public record GeneratePlanRequest(UUID planId) {}

  // -- Certificate --
  public record CertificateResponse(UUID id, String certificateType, String certificateNo,
      LocalDate issueDate, LocalDate expiryDate, String issuingAuthority, String remark,
      long daysUntilExpiry) {}
  public record CreateCertificateRequest(UUID userId, String certificateType,
      String certificateNo, LocalDate issueDate, LocalDate expiryDate,
      String issuingAuthority, String remark) {}

  // -- Schedule --
  public record ScheduleResponse(UUID id, UUID orderId, String orderCode, String title,
      String engineerName, OffsetDateTime scheduledAt, OffsetDateTime checkInAt,
      String checkInLocation, OffsetDateTime startedAt, OffsetDateTime completedAt,
      WorkOrderStatus status) {}
  public record CreateScheduleRequest(UUID orderId, UUID engineerId,
      OffsetDateTime scheduledAt) {}

  // -- Attendance --
  public record AttendanceResponse(UUID id, UUID orderId, String orderCode,
      UUID engineerId, String engineerName, OffsetDateTime checkInAt,
      String checkInLocation, OffsetDateTime checkOutAt) {}
}
