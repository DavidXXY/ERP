package com.company.ops.api.modules.governance.controller;

import static com.company.ops.api.modules.governance.dto.GovernanceDtos.*;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.governance.domain.ControlStatus;
import com.company.ops.api.modules.governance.domain.ControlType;
import com.company.ops.api.modules.governance.domain.ReconciliationStatus;
import com.company.ops.api.modules.governance.service.GovernanceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/governance")
public class GovernanceController {
  private final GovernanceService service;

  public GovernanceController(GovernanceService service) { this.service = service; }

  @GetMapping("/overview")
  @PreAuthorize("hasAuthority('governance:view')")
  public ApiResponse<GovernanceOverview> overview() { return ApiResponse.ok(service.overview()); }

  @GetMapping("/control-types")
  @PreAuthorize("hasAuthority('governance:view')")
  public ApiResponse<List<ControlTypeResponse>> controlTypes() { return ApiResponse.ok(service.controlTypes()); }

  @GetMapping("/controls")
  @PreAuthorize("hasAuthority('governance:view')")
  public ApiResponse<PageResponse<ControlResponse>> controls(
      @RequestParam(required = false) ControlType type,
      @RequestParam(required = false) ControlStatus status,
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(service.controls(type, status, keyword, pageable)));
  }

  @PostMapping("/controls")
  @PreAuthorize("hasAuthority('governance:manage')")
  public ApiResponse<ControlResponse> createControl(@Valid @RequestBody SaveControlRequest request) {
    return ApiResponse.ok(service.createControl(request));
  }

  @PutMapping("/controls/{id}")
  @PreAuthorize("hasAuthority('governance:manage')")
  public ApiResponse<ControlResponse> updateControl(@PathVariable UUID id, @Valid @RequestBody SaveControlRequest request) {
    return ApiResponse.ok(service.updateControl(id, request));
  }

  @PostMapping("/controls/{id}/transition")
  @PreAuthorize("hasAuthority('governance:manage')")
  public ApiResponse<ControlResponse> transition(@PathVariable UUID id, @Valid @RequestBody TransitionControlRequest request) {
    return ApiResponse.ok(service.transition(id, request));
  }

  @PostMapping("/controls/{id}/review")
  @PreAuthorize("hasAuthority('governance:manage')")
  public ApiResponse<ControlResponse> review(@PathVariable UUID id, @Valid @RequestBody ReviewControlRequest request) {
    return ApiResponse.ok(service.review(id, request));
  }

  @GetMapping("/exceptions")
  @PreAuthorize("hasAuthority('governance:view')")
  public ApiResponse<List<ControlExceptionResponse>> exceptions() { return ApiResponse.ok(service.exceptions()); }

  @GetMapping("/actions/{entityType}/{entityId}")
  @PreAuthorize("hasAuthority('governance:view')")
  public ApiResponse<List<GovernanceActionResponse>> actions(
      @PathVariable String entityType, @PathVariable UUID entityId) {
    return ApiResponse.ok(service.actions(entityType, entityId));
  }

  @GetMapping("/periods")
  @PreAuthorize("hasAuthority('governance:view')")
  public ApiResponse<List<PeriodResponse>> periods() { return ApiResponse.ok(service.periods()); }

  @PostMapping("/periods")
  @PreAuthorize("hasAuthority('governance:period:close')")
  public ApiResponse<PeriodResponse> openPeriod(@Valid @RequestBody OpenPeriodRequest request) {
    return ApiResponse.ok(service.openPeriod(request));
  }

  @GetMapping("/periods/{year}/{month}/readiness")
  @PreAuthorize("hasAuthority('governance:view')")
  public ApiResponse<CloseReadinessResponse> closeReadiness(@PathVariable int year, @PathVariable int month) {
    return ApiResponse.ok(service.closeReadiness(year, month));
  }

  @PostMapping("/periods/{year}/{month}/close")
  @PreAuthorize("hasAuthority('governance:period:close')")
  public ApiResponse<PeriodResponse> closePeriod(
      @PathVariable int year, @PathVariable int month, @Valid @RequestBody ClosePeriodRequest request) {
    return ApiResponse.ok(service.closePeriod(year, month, request));
  }

  @PostMapping("/periods/{year}/{month}/reopen")
  @PreAuthorize("hasAuthority('governance:period:close')")
  public ApiResponse<PeriodResponse> reopenPeriod(
      @PathVariable int year, @PathVariable int month, @Valid @RequestBody ReopenPeriodRequest request) {
    return ApiResponse.ok(service.reopenPeriod(year, month, request));
  }

  @GetMapping("/bank-lines")
  @PreAuthorize("hasAuthority('governance:view')")
  public ApiResponse<PageResponse<BankLineResponse>> bankLines(
      @RequestParam(required = false) ReconciliationStatus status,
      @PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(service.bankLines(status, pageable)));
  }

  @PostMapping("/bank-lines/import")
  @PreAuthorize("hasAuthority('governance:bank:reconcile')")
  public ApiResponse<BankImportResponse> importBankStatement(@Valid @RequestBody ImportBankStatementRequest request) {
    return ApiResponse.ok(service.importBankStatement(request));
  }

  @PostMapping("/bank-lines/{id}/reconcile")
  @PreAuthorize("hasAuthority('governance:bank:reconcile')")
  public ApiResponse<BankLineResponse> reconcile(@PathVariable UUID id, @Valid @RequestBody ReconcileBankLineRequest request) {
    return ApiResponse.ok(service.reconcile(id, request));
  }

  @PostMapping("/bank-lines/{id}/unreconcile")
  @PreAuthorize("hasAuthority('governance:bank:reconcile')")
  public ApiResponse<BankLineResponse> unreconcile(@PathVariable UUID id, @RequestParam String reason) {
    return ApiResponse.ok(service.unreconcile(id, reason));
  }
}
