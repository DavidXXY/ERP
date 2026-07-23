package com.company.ops.api.modules.procurement.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.dto.ProcurementGovernanceDtos.*;
import com.company.ops.api.modules.procurement.dto.PurchaseRequestResponse;
import com.company.ops.api.modules.procurement.service.ProcurementGovernanceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/procurement/governance")
public class ProcurementGovernanceController {
  private final ProcurementGovernanceService service;

  public ProcurementGovernanceController(ProcurementGovernanceService service) {
    this.service = service;
  }

  @GetMapping("/contracts")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<ProcurementContract>> contracts() {
    return ApiResponse.ok(service.listContracts());
  }

  @PostMapping("/contracts")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<ProcurementContract> createContract(@Valid @RequestBody CreateContract request) {
    return ApiResponse.ok(service.createContract(request));
  }

  @PostMapping("/contracts/{id}/submit")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<ProcurementContract> submitContract(@PathVariable UUID id) {
    return ApiResponse.ok(service.submitContract(id));
  }

  @PostMapping("/contracts/{id}/review")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<ProcurementContract> reviewContract(
      @PathVariable UUID id, @Valid @RequestBody ReviewAction request) {
    return ApiResponse.ok(service.reviewContract(id, request));
  }

  @PostMapping("/contracts/{id}/amend")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<ProcurementContract> amendContract(
      @PathVariable UUID id, @Valid @RequestBody AmendContract request) {
    return ApiResponse.ok(service.amendContract(id, request));
  }

  @GetMapping("/supplier-changes")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<SupplierChangeRequest>> supplierChanges() {
    return ApiResponse.ok(service.listSupplierChanges());
  }

  @PostMapping("/supplier-changes")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<SupplierChangeRequest> createSupplierChange(
      @Valid @RequestBody CreateSupplierChange request) {
    return ApiResponse.ok(service.createSupplierChange(request));
  }

  @PostMapping("/supplier-changes/{id}/review")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<SupplierChangeRequest> reviewSupplierChange(
      @PathVariable UUID id, @Valid @RequestBody ReviewAction request) {
    return ApiResponse.ok(service.reviewSupplierChange(id, request));
  }

  @GetMapping("/supplier-reviews")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<SupplierPerformanceReview>> supplierReviews() {
    return ApiResponse.ok(service.listReviews());
  }

  @PostMapping("/supplier-reviews/calculate")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<SupplierPerformanceReview> calculateSupplierReview(
      @Valid @RequestBody CalculateSupplierReview request) {
    return ApiResponse.ok(service.calculateReview(request));
  }

  @GetMapping("/collaboration-events")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<ProcurementCollaborationEvent>> collaborationEvents() {
    return ApiResponse.ok(service.listCollaborationEvents());
  }

  @PostMapping("/collaboration-events")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<ProcurementCollaborationEvent> createCollaborationEvent(
      @Valid @RequestBody CreateCollaborationEvent request) {
    return ApiResponse.ok(service.createCollaborationEvent(request));
  }

  @PostMapping("/replenishment/convert")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<PurchaseRequestResponse> convertReplenishment(
      @Valid @RequestBody ConvertReplenishment request) {
    return ApiResponse.ok(service.convertReplenishment(request));
  }

  @GetMapping("/action-logs")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<ProcurementActionLog>> actionLogs(
      @RequestParam String sourceType, @RequestParam UUID sourceId) {
    return ApiResponse.ok(service.listActionLogs(sourceType, sourceId));
  }
}
