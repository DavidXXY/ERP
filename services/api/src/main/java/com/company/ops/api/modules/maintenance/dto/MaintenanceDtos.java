package com.company.ops.api.modules.maintenance.dto;

import com.company.ops.api.modules.maintenance.domain.EquipmentStatus;
import com.company.ops.api.modules.maintenance.domain.ScheduleStatus;
import com.company.ops.api.modules.maintenance.domain.WorkOrderPriority;
import com.company.ops.api.modules.maintenance.domain.WorkOrderSource;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.domain.WorkOrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class MaintenanceDtos {
  private MaintenanceDtos() {}

  public record DashboardResponse(long equipmentCount, long dueEquipmentCount, long openWorkOrders,
                                  long overdueWorkOrders, long expiringCertificates, BigDecimal monthCost,
                                  BigDecimal monthBillable) {}
  public record CustomerOption(UUID id, String code, String name) {}
  public record ContractOption(UUID id, UUID customerId, String code, String projectName) {}
  public record PartOption(UUID id, String code, String name, String model, BigDecimal stockQty, BigDecimal unitCost) {}
  public record UserOption(UUID id, String displayName, boolean enabled) {}
  public record ReferenceDataResponse(List<CustomerOption> customers, List<ContractOption> contracts,
                                      List<PartOption> parts, List<UserOption> users) {}

  public record CreateEquipmentRequest(
      @NotNull UUID customerId, UUID contractId, @NotBlank @Size(max = 64) String code,
      @NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 80) String category,
      @Size(max = 120) String model, @Size(max = 120) String serialNo,
      @NotBlank @Size(max = 300) String siteAddress, LocalDate installedDate,
      LocalDate warrantyEndDate, @NotNull @Min(1) Integer maintenanceCycleDays,
      @NotNull LocalDate nextMaintenanceDate, @Size(max = 120) String requiredCertificate,
      @Size(max = 500) String notes, boolean createPlan
  ) {}

  public record EquipmentResponse(UUID id, UUID customerId, String customerName, UUID contractId,
                                  String contractCode, String code, String name, String category,
                                  String model, String serialNo, String siteAddress, LocalDate installedDate,
                                  LocalDate warrantyEndDate, Integer maintenanceCycleDays,
                                  LocalDate lastMaintenanceDate, LocalDate nextMaintenanceDate,
                                  EquipmentStatus status, String requiredCertificate, String notes,
                                  long workOrderCount, long faultCount) {}

  public record CreatePlanRequest(@NotBlank @Size(max = 64) String code, @NotNull UUID assetId,
                                  @NotBlank @Size(max = 180) String planName,
                                  @NotNull @Min(1) Integer cycleDays, @NotNull LocalDate nextDueDate) {}

  public record PlanResponse(UUID id, String code, UUID assetId, String assetCode, String assetName,
                             UUID contractId, String contractCode, String planName, Integer cycleDays,
                             LocalDate nextDueDate, LocalDate lastGeneratedDate, boolean active,
                             boolean due) {}

  public record GeneratePlanRequest(@NotNull LocalDate throughDate, @NotBlank String operatorName) {}
  public record GeneratePlanResponse(int generatedCount, List<String> workOrderCodes) {}

  public record CreateWorkOrderRequest(
      @NotBlank @Size(max = 64) String code, @NotNull WorkOrderSource source,
      @NotNull WorkOrderType workType, @NotNull WorkOrderPriority priority,
      @NotBlank @Size(max = 180) String title, @NotNull UUID customerId,
      UUID contractId, UUID projectId, UUID equipmentId, @NotNull LocalDate plannedDate,
      @NotBlank @Size(max = 300) String siteAddress,
      @NotBlank @Size(max = 1000) String problemDescription,
      @DecimalMin("0.00") BigDecimal billableAmount, boolean freeWarranty,
      @NotBlank String operatorName
  ) {}

  public record AssignWorkOrderRequest(@NotNull UUID assigneeId, @NotBlank String operatorName) {}
  public record CheckInRequest(@NotBlank @Size(max = 300) String location, @NotBlank String operatorName) {}
  public record MaterialRequest(@NotNull UUID partId, @NotNull @Positive BigDecimal quantity) {}
  public record CompleteWorkOrderRequest(
      @NotBlank @Size(max = 1500) String serviceResult,
      @NotNull @DecimalMin("0.00") BigDecimal laborHours,
      @NotNull @DecimalMin("0.00") BigDecimal laborCost,
      @NotNull @DecimalMin("0.00") BigDecimal travelCost,
      @NotNull @DecimalMin("0.00") BigDecimal outsourcingCost,
      @NotNull @Valid List<MaterialRequest> materials,
      @NotBlank String operatorName
  ) {}
  public record AcceptWorkOrderRequest(@NotBlank @Size(max = 80) String customerSigner,
                                       @NotBlank @Size(max = 500) String acceptanceNote,
                                       @NotBlank String operatorName) {}

  public record MaterialResponse(UUID id, UUID partId, String partName, BigDecimal quantity,
                                 BigDecimal unitCost, BigDecimal amount) {}
  public record StatusLogResponse(UUID id, WorkOrderStatus fromStatus, WorkOrderStatus toStatus,
                                  String operatorName, String remark, OffsetDateTime createdAt) {}
  public record WorkOrderResponse(
      UUID id, String code, WorkOrderSource source, WorkOrderType workType, WorkOrderPriority priority,
      WorkOrderStatus status, String title, UUID customerId, String customerName,
      UUID contractId, String contractCode, UUID projectId, UUID equipmentId,
      String equipmentCode, String equipmentName, LocalDate plannedDate, String siteAddress,
      String problemDescription, UUID assigneeId, String engineerName, String requiredCertificate,
      OffsetDateTime checkInAt, String checkInLocation, OffsetDateTime startedAt,
      OffsetDateTime completedAt, OffsetDateTime acceptedAt, String customerSigner,
      String serviceResult, String acceptanceNote, BigDecimal laborHours, BigDecimal laborCost,
      BigDecimal materialCost, BigDecimal travelCost, BigDecimal outsourcingCost,
      BigDecimal costAmount, BigDecimal billableAmount, boolean freeWarranty,
      List<MaterialResponse> materials, List<StatusLogResponse> statusLogs
  ) {}

  public record CreateCertificateRequest(@NotNull UUID userId, @NotBlank @Size(max = 120) String certificateType,
                                         @NotBlank @Size(max = 120) String certificateNo,
                                         LocalDate issueDate, @NotNull LocalDate expiryDate,
                                         @Size(max = 180) String issuingAuthority) {}
  public record CertificateResponse(UUID id, UUID userId, String employeeName, String certificateType,
                                    String certificateNo, LocalDate issueDate, LocalDate expiryDate,
                                    String issuingAuthority, boolean expired, boolean expiringSoon) {}
  public record CreateScheduleRequest(@NotNull UUID userId, @NotNull LocalDate workDate,
                                      @NotBlank @Size(max = 80) String shiftName,
                                      @Size(max = 180) String siteName, @NotNull ScheduleStatus status) {}
  public record ScheduleResponse(UUID id, UUID userId, String employeeName, LocalDate workDate,
                                 String shiftName, String siteName, ScheduleStatus status) {}
  public record AttendanceResponse(UUID id, UUID userId, String employeeName, UUID workOrderId,
                                   String workOrderCode, OffsetDateTime checkInAt, OffsetDateTime checkOutAt,
                                   String checkInLocation, String checkOutLocation) {}
  public record EmployeeOption(UUID id, String displayName, List<String> validCertificates,
                               boolean availableOnPlannedDate) {}
}
