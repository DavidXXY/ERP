package com.company.ops.api.modules.finance.controller;

import static com.company.ops.api.modules.finance.dto.FinanceOperationsDtos.*;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.finance.service.FinanceOperationsService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/operations")
@PreAuthorize("hasAnyAuthority('finance:operations:view','finance:operations:manage')")
public class FinanceOperationsController {
  private final FinanceOperationsService service;
  public FinanceOperationsController(FinanceOperationsService service) { this.service = service; }

  @GetMapping("/overview") public ApiResponse<OperationsOverview> overview() { return ApiResponse.ok(service.overview()); }

  @GetMapping("/period-jobs") public ApiResponse<List<PeriodJobResponse>> periodJobs(
      @RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month) {
    return ApiResponse.ok(service.periodJobs(year, month));
  }
  @PostMapping("/period-jobs") @PreAuthorize("hasAuthority('finance:operations:manage')")
  public ApiResponse<PeriodJobResponse> createPeriodJob(@Valid @RequestBody SavePeriodJobRequest request) { return ApiResponse.ok(service.createPeriodJob(request)); }
  @PostMapping("/period-jobs/{id}/execute") @PreAuthorize("hasAuthority('finance:operations:manage')")
  public ApiResponse<PeriodJobResponse> executePeriodJob(@PathVariable UUID id) { return ApiResponse.ok(service.executePeriodJob(id)); }
  @PostMapping("/period-jobs/reverse-due") @PreAuthorize("hasAuthority('finance:operations:manage')")
  public ApiResponse<List<PeriodJobResponse>> reverseDue(@RequestParam(required = false) LocalDate asOf) { return ApiResponse.ok(service.reverseDueJobs(asOf)); }

  @GetMapping("/opening-validation/{year}") public ApiResponse<OpeningValidationResponse> openingValidation(@PathVariable int year) { return ApiResponse.ok(service.validateOpening(year)); }
  @GetMapping("/budget-variance") public ApiResponse<List<BudgetVarianceResponse>> budgetVariance() { return ApiResponse.ok(service.budgetVariance()); }

  @GetMapping("/partner-statements") public ApiResponse<List<PartnerStatementResponse>> partnerStatements(
      @RequestParam String type, @RequestParam(required = false) LocalDate periodEnd) { return ApiResponse.ok(service.partnerStatements(type, periodEnd)); }
  @PostMapping("/partner-statements/{type}/{partnerId}/confirm") @PreAuthorize("hasAuthority('finance:operations:manage')")
  public ApiResponse<PartnerStatementResponse> confirmPartner(@PathVariable String type, @PathVariable UUID partnerId,
      @RequestParam LocalDate periodEnd, @Valid @RequestBody ConfirmPartnerRequest request) {
    return ApiResponse.ok(service.confirmPartner(type, partnerId, periodEnd, request));
  }

  @GetMapping("/cash-scenarios") public ApiResponse<List<CashScenarioResponse>> cashScenarios() { return ApiResponse.ok(service.cashScenarios()); }
  @PostMapping("/cash-scenarios") @PreAuthorize("hasAuthority('finance:operations:manage')")
  public ApiResponse<CashScenarioResponse> createCashScenario(@Valid @RequestBody SaveCashScenarioRequest request) { return ApiResponse.ok(service.createCashScenario(request)); }

  @GetMapping("/tax-filings") public ApiResponse<List<TaxFilingResponse>> taxFilings() { return ApiResponse.ok(service.taxFilings()); }
  @PostMapping("/tax-filings/{year}/{month}/reconcile") @PreAuthorize("hasAuthority('finance:operations:manage')")
  public ApiResponse<TaxFilingResponse> reconcileTax(@PathVariable int year, @PathVariable int month) { return ApiResponse.ok(service.reconcileTax(year, month)); }
  @PostMapping("/tax-filings/{year}/{month}/lock") @PreAuthorize("hasAuthority('finance:operations:manage')")
  public ApiResponse<TaxFilingResponse> lockTax(@PathVariable int year, @PathVariable int month, @Valid @RequestBody LockTaxFilingRequest request) { return ApiResponse.ok(service.lockTax(year, month, request)); }

  @GetMapping("/consolidations") public ApiResponse<List<ConsolidationResponse>> consolidations() { return ApiResponse.ok(service.consolidations()); }
  @PostMapping("/consolidations") @PreAuthorize("hasAuthority('finance:operations:manage')")
  public ApiResponse<ConsolidationResponse> createConsolidation(@Valid @RequestBody SaveConsolidationRequest request) { return ApiResponse.ok(service.createConsolidation(request)); }
  @PostMapping("/consolidations/{id}/complete") @PreAuthorize("hasAuthority('finance:operations:manage')")
  public ApiResponse<ConsolidationResponse> completeConsolidation(@PathVariable UUID id) { return ApiResponse.ok(service.completeConsolidation(id)); }

  @GetMapping("/snapshots") public ApiResponse<List<ReportSnapshotResponse>> snapshots() { return ApiResponse.ok(service.snapshots()); }
  @PostMapping("/snapshots") @PreAuthorize("hasAuthority('finance:operations:manage')")
  public ApiResponse<ReportSnapshotResponse> captureSnapshot(@Valid @RequestBody CaptureSnapshotRequest request) { return ApiResponse.ok(service.captureSnapshot(request)); }
  @GetMapping("/voucher-requests") public ApiResponse<List<VoucherRequestResponse>> voucherRequests() { return ApiResponse.ok(service.voucherRequests()); }
}
