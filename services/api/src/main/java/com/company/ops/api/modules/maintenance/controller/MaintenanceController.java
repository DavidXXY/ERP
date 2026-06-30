package com.company.ops.api.modules.maintenance.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.AcceptWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.AssignWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.AttendanceResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CertificateResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CheckInRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CompleteWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateCertificateRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateEquipmentRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreatePlanRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateScheduleRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.DashboardResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.EmployeeOption;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.EquipmentResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.GeneratePlanRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.GeneratePlanResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.PlanResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.ReferenceDataResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.ScheduleResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.WorkOrderResponse;
import com.company.ops.api.modules.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {
  private final MaintenanceService service;
  public MaintenanceController(MaintenanceService service) { this.service = service; }

  @GetMapping("/references") @PreAuthorize("hasAnyAuthority('maintenance:view', 'maintenance:equipment:create', 'maintenance:workorder:create', 'workforce:view')")
  public ApiResponse<ReferenceDataResponse> references() { return ApiResponse.ok(service.referenceData()); }
  @GetMapping("/dashboard") @PreAuthorize("hasAuthority('maintenance:view')")
  public ApiResponse<DashboardResponse> dashboard() { return ApiResponse.ok(service.dashboard()); }
  @GetMapping("/equipment") @PreAuthorize("hasAuthority('maintenance:equipment:view')")
  public ApiResponse<List<EquipmentResponse>> equipment() { return ApiResponse.ok(service.listEquipment()); }
  @PostMapping("/equipment") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('maintenance:equipment:create')")
  public ApiResponse<EquipmentResponse> createEquipment(@Valid @RequestBody CreateEquipmentRequest request) { return ApiResponse.ok(service.createEquipment(request)); }
  @GetMapping("/plans") @PreAuthorize("hasAuthority('maintenance:plan:view')")
  public ApiResponse<List<PlanResponse>> plans() { return ApiResponse.ok(service.listPlans()); }
  @PostMapping("/plans") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('maintenance:plan:create')")
  public ApiResponse<PlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest request) { return ApiResponse.ok(service.createPlan(request)); }
  @PostMapping("/plans/generate") @PreAuthorize("hasAuthority('maintenance:plan:generate')")
  public ApiResponse<GeneratePlanResponse> generate(@Valid @RequestBody GeneratePlanRequest request) { return ApiResponse.ok(service.generateDueWorkOrders(request)); }
  @GetMapping("/work-orders") @PreAuthorize("hasAuthority('maintenance:workorder:view')")
  public ApiResponse<List<WorkOrderResponse>> workOrders() { return ApiResponse.ok(service.listWorkOrders()); }
  @GetMapping("/work-orders/{id}") @PreAuthorize("hasAuthority('maintenance:workorder:view')")
  public ApiResponse<WorkOrderResponse> workOrder(@PathVariable UUID id) { return ApiResponse.ok(service.getWorkOrder(id)); }
  @PostMapping("/work-orders") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('maintenance:workorder:create')")
  public ApiResponse<WorkOrderResponse> createWorkOrder(@Valid @RequestBody CreateWorkOrderRequest request) { return ApiResponse.ok(service.createWorkOrder(request)); }
  @GetMapping("/work-orders/{id}/eligible-employees") @PreAuthorize("hasAuthority('maintenance:workorder:assign')")
  public ApiResponse<List<EmployeeOption>> eligibleEmployees(@PathVariable UUID id) { return ApiResponse.ok(service.eligibleEmployees(id)); }
  @PostMapping("/work-orders/{id}/assign") @PreAuthorize("hasAuthority('maintenance:workorder:assign')")
  public ApiResponse<WorkOrderResponse> assign(@PathVariable UUID id, @Valid @RequestBody AssignWorkOrderRequest request) { return ApiResponse.ok(service.assignWorkOrder(id, request)); }
  @PostMapping("/work-orders/{id}/check-in") @PreAuthorize("hasAuthority('maintenance:workorder:execute')")
  public ApiResponse<WorkOrderResponse> checkIn(@PathVariable UUID id, @Valid @RequestBody CheckInRequest request) { return ApiResponse.ok(service.checkIn(id, request)); }
  @PostMapping("/work-orders/{id}/complete") @PreAuthorize("hasAuthority('maintenance:workorder:execute')")
  public ApiResponse<WorkOrderResponse> complete(@PathVariable UUID id, @Valid @RequestBody CompleteWorkOrderRequest request) { return ApiResponse.ok(service.completeWorkOrder(id, request)); }
  @PostMapping("/work-orders/{id}/accept") @PreAuthorize("hasAuthority('maintenance:workorder:accept')")
  public ApiResponse<WorkOrderResponse> accept(@PathVariable UUID id, @Valid @RequestBody AcceptWorkOrderRequest request) { return ApiResponse.ok(service.acceptWorkOrder(id, request)); }
  @GetMapping("/certificates") @PreAuthorize("hasAuthority('workforce:view')")
  public ApiResponse<List<CertificateResponse>> certificates() { return ApiResponse.ok(service.listCertificates()); }
  @PostMapping("/certificates") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('workforce:certificate:create')")
  public ApiResponse<CertificateResponse> createCertificate(@Valid @RequestBody CreateCertificateRequest request) { return ApiResponse.ok(service.createCertificate(request)); }
  @GetMapping("/schedules") @PreAuthorize("hasAuthority('workforce:view')")
  public ApiResponse<List<ScheduleResponse>> schedules() { return ApiResponse.ok(service.listSchedules()); }
  @PostMapping("/schedules") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('workforce:schedule:create')")
  public ApiResponse<ScheduleResponse> createSchedule(@Valid @RequestBody CreateScheduleRequest request) { return ApiResponse.ok(service.createSchedule(request)); }
  @GetMapping("/attendance") @PreAuthorize("hasAuthority('workforce:view')")
  public ApiResponse<List<AttendanceResponse>> attendance() { return ApiResponse.ok(service.listAttendance()); }
}
