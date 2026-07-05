package com.company.ops.api.modules.maintenance.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.*;
import com.company.ops.api.modules.maintenance.service.MaintenanceService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

  private final MaintenanceService service;

  public MaintenanceController(MaintenanceService service) {
    this.service = service;
  }

  @GetMapping("/dashboard")
  @PreAuthorize("hasAuthority('maintenance:view')")
  public ApiResponse<DashboardResponse> dashboard() {
    return ApiResponse.ok(service.dashboard());
  }

  @GetMapping("/references")
  @PreAuthorize("hasAuthority('maintenance:view')")
  public ApiResponse<ReferenceDataResponse> references() {
    return ApiResponse.ok(service.references());
  }

  @GetMapping("/work-orders")
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ApiResponse<List<WorkOrderResponse>> workOrders() {
    return ApiResponse.ok(service.listWorkOrders());
  }

  @GetMapping("/work-orders/{id}")
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ApiResponse<WorkOrderResponse> workOrder(@PathVariable UUID id) {
    return ApiResponse.ok(service.getWorkOrder(id));
  }

  @PostMapping("/work-orders")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('maintenance:order:manage')")
  public ApiResponse<WorkOrderResponse> createWorkOrder(@Valid @RequestBody CreateWorkOrderRequest request) {
    return ApiResponse.ok(service.createWorkOrder(request));
  }

  @PutMapping("/work-orders/{id}/assign")
  @PreAuthorize("hasAuthority('maintenance:order:manage')")
  public ApiResponse<WorkOrderResponse> assign(@PathVariable UUID id, @Valid @RequestBody AssignWorkOrderRequest request) {
    return ApiResponse.ok(service.assign(id, request));
  }

  @PutMapping("/work-orders/{id}/check-in")
  @PreAuthorize("hasAuthority('maintenance:order:manage')")
  public ApiResponse<WorkOrderResponse> checkIn(@PathVariable UUID id, @Valid @RequestBody CheckInRequest request) {
    return ApiResponse.ok(service.checkIn(id, request));
  }

  @PutMapping("/work-orders/{id}/complete")
  @PreAuthorize("hasAuthority('maintenance:order:manage')")
  public ApiResponse<WorkOrderResponse> complete(@PathVariable UUID id, @Valid @RequestBody CompleteWorkOrderRequest request) {
    return ApiResponse.ok(service.complete(id, request));
  }

  @PutMapping("/work-orders/{id}/accept")
  @PreAuthorize("hasAuthority('maintenance:order:manage')")
  public ApiResponse<WorkOrderResponse> accept(@PathVariable UUID id, @Valid @RequestBody AcceptWorkOrderRequest request) {
    return ApiResponse.ok(service.accept(id, request));
  }

  @DeleteMapping("/work-orders/{id}")
  public ApiResponse<Void> deleteWorkOrder(@PathVariable UUID id,
      @AuthenticationPrincipal UserPrincipal principal) {
    if (!principal.roleCodes().contains("ADMIN")) {
      return ApiResponse.fail("删除需要管理员权限");
    }
    service.deleteWorkOrder(id);
    return ApiResponse.ok();
  }

  @GetMapping("/equipment")
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ApiResponse<List<EquipmentResponse>> equipment() {
    return ApiResponse.ok(service.listEquipment());
  }

  @GetMapping("/plans")
  @PreAuthorize("hasAuthority('maintenance:plan:manage')")
  public ApiResponse<List<PlanResponse>> plans() {
    return ApiResponse.ok(service.listPlans());
  }

  @GetMapping("/certificates")
  public ApiResponse<List<CertificateResponse>> certificates() {
    return ApiResponse.ok(service.listCertificates());
  }

  @GetMapping("/schedules")
  public ApiResponse<List<ScheduleResponse>> schedules() {
    return ApiResponse.ok(service.listSchedules());
  }
}
