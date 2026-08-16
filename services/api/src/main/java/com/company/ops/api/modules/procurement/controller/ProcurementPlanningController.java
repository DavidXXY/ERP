package com.company.ops.api.modules.procurement.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.CentralPlanResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.FrameworkAgreementResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.CentralPlanSuggestionsResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.SaveCentralPlanRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.SaveFrameworkAgreementRequest;
import com.company.ops.api.modules.procurement.dto.PurchaseRequestResponse;
import com.company.ops.api.modules.procurement.service.ProcurementPlanningService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procurement")
public class ProcurementPlanningController {

  private final ProcurementPlanningService service;

  public ProcurementPlanningController(ProcurementPlanningService service) {
    this.service = service;
  }

  // ---------- 框架协议 ----------

  @GetMapping("/framework-agreements")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<FrameworkAgreementResponse>> frameworkAgreements() {
    return ApiResponse.ok(service.listFrameworkAgreements());
  }

  @GetMapping("/framework-agreements/{id}")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<FrameworkAgreementResponse> frameworkAgreement(@PathVariable UUID id) {
    return ApiResponse.ok(service.getFrameworkAgreement(id));
  }

  @PostMapping("/framework-agreements")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<FrameworkAgreementResponse> createFrameworkAgreement(
      @Valid @RequestBody SaveFrameworkAgreementRequest request
  ) {
    return ApiResponse.ok(service.saveFrameworkAgreement(null, request));
  }

  @PutMapping("/framework-agreements/{id}")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<FrameworkAgreementResponse> updateFrameworkAgreement(
      @PathVariable UUID id,
      @Valid @RequestBody SaveFrameworkAgreementRequest request
  ) {
    return ApiResponse.ok(service.saveFrameworkAgreement(id, request));
  }

  @PostMapping("/framework-agreements/{id}/close")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<FrameworkAgreementResponse> closeFrameworkAgreement(@PathVariable UUID id) {
    return ApiResponse.ok(service.closeFrameworkAgreement(id));
  }

  // ---------- 集采计划 ----------

  @GetMapping("/central-plans")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<CentralPlanResponse>> centralPlans() {
    return ApiResponse.ok(service.listCentralPlans());
  }

  @PostMapping("/central-plans/generate-suggestions")
  @PreAuthorize("hasAnyAuthority('procurement:view', 'procurement:request:approve')")
  public ApiResponse<CentralPlanSuggestionsResponse> generateCentralPlanSuggestions(
      @RequestParam(required = false) Integer periodYear
  ) {
    return ApiResponse.ok(service.generateCentralPlanSuggestions(periodYear));
  }

  @PostMapping("/central-plans")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<CentralPlanResponse> createCentralPlan(
      @Valid @RequestBody SaveCentralPlanRequest request
  ) {
    return ApiResponse.ok(service.saveCentralPlan(null, request));
  }

  @PutMapping("/central-plans/{id}")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<CentralPlanResponse> updateCentralPlan(
      @PathVariable UUID id,
      @Valid @RequestBody SaveCentralPlanRequest request
  ) {
    return ApiResponse.ok(service.saveCentralPlan(id, request));
  }

  @PostMapping("/central-plans/{id}/status")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<CentralPlanResponse> updateCentralPlanStatus(
      @PathVariable UUID id,
      @RequestParam String status
  ) {
    return ApiResponse.ok(service.updateCentralPlanStatus(id, status));
  }

  @PostMapping("/central-plans/{planId}/items/{itemId}/convert")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<PurchaseRequestResponse> convertPlanItem(
      @PathVariable UUID planId,
      @PathVariable UUID itemId,
      @RequestParam(required = false) UUID departmentId,
      @RequestParam(required = false) UUID projectId
  ) {
    return ApiResponse.ok(service.convertPlanItemToRequest(planId, itemId, departmentId, projectId));
  }
}
