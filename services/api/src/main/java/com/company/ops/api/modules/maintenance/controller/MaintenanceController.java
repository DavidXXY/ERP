package com.company.ops.api.modules.maintenance.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.*;
import com.company.ops.api.modules.maintenance.domain.WorkOrderAttachmentCategory;
import com.company.ops.api.modules.maintenance.domain.WorkOrderAttachment;
import com.company.ops.api.modules.maintenance.service.MaintenanceService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

  @GetMapping("/mobile/work-orders")
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ApiResponse<List<WorkOrderResponse>> mobileWorkOrders(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(service.listMobileWorkOrders(principal));
  }

  @GetMapping("/mobile/assignees")
  @PreAuthorize("hasAuthority('maintenance:order:manage')")
  public ApiResponse<List<AssigneeOption>> mobileAssignees() {
    return ApiResponse.ok(service.mobileAssignees());
  }

  @GetMapping("/mobile/work-orders/{id}")
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ApiResponse<WorkOrderResponse> mobileWorkOrder(@PathVariable UUID id,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(service.getMobileWorkOrder(id, principal));
  }

  @PutMapping("/mobile/work-orders/{id}/accept-assignment")
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ApiResponse<WorkOrderResponse> acceptAssignment(@PathVariable UUID id,
      @Valid @RequestBody MobileOperationRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(service.acceptAssignment(id, request, principal));
  }

  @PutMapping("/mobile/work-orders/{id}/check-in")
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ApiResponse<WorkOrderResponse> mobileCheckIn(@PathVariable UUID id,
      @Valid @RequestBody MobileCheckInRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(service.mobileCheckIn(id, request, principal));
  }

  @PutMapping("/mobile/work-orders/{id}/complete")
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ApiResponse<WorkOrderResponse> mobileComplete(@PathVariable UUID id,
      @Valid @RequestBody MobileCompleteWorkOrderRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(service.mobileComplete(id, request, principal));
  }

  @PostMapping(value = "/mobile/work-orders/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ApiResponse<AttachmentResponse> uploadMobileAttachment(@PathVariable UUID id,
      @RequestParam WorkOrderAttachmentCategory category,
      @RequestPart("file") MultipartFile file,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(service.uploadMobileAttachment(id, category, file, principal));
  }

  @GetMapping("/mobile/attachments/{attachmentId}/content")
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ResponseEntity<org.springframework.core.io.Resource> mobileAttachmentContent(
      @PathVariable UUID attachmentId, @AuthenticationPrincipal UserPrincipal principal) {
    WorkOrderAttachment item = service.requireMobileAttachment(attachmentId, principal);
    MediaType contentType;
    try { contentType = MediaType.parseMediaType(item.getContentType()); }
    catch (Exception ignored) { contentType = MediaType.APPLICATION_OCTET_STREAM; }
    return ResponseEntity.ok()
        .contentType(contentType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + item.getFileName().replace("\"", "") + "\"")
        .body(service.loadMobileAttachment(item));
  }

  @GetMapping("/mobile/work-orders/{id}/materials")
  @PreAuthorize("hasAnyAuthority('maintenance:order:manage', 'maintenance:view')")
  public ApiResponse<List<MaterialResponse>> mobileMaterials(@PathVariable UUID id,
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(service.listMobileMaterials(id, principal));
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
  @PreAuthorize("hasAuthority('maintenance:order:delete')")
  public ApiResponse<Void> deleteWorkOrder(@PathVariable UUID id) {
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
  @PreAuthorize("hasAnyAuthority('maintenance:certificate:view', 'maintenance:view')")
  public ApiResponse<List<CertificateResponse>> certificates() {
    return ApiResponse.ok(service.listCertificates());
  }

  @GetMapping("/schedules")
  @PreAuthorize("hasAnyAuthority('maintenance:schedule:view', 'maintenance:view')")
  public ApiResponse<List<ScheduleResponse>> schedules() {
    return ApiResponse.ok(service.listSchedules());
  }
}
