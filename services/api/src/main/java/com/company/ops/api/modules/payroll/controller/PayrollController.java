package com.company.ops.api.modules.payroll.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.ConfigRequest;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.ConfigResponse;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.PayrollRunResponse;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.RunPayrollRequest;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.SalaryItemRequest;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.SalaryItemResponse;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.SocialTaxLedgerResponse;
import com.company.ops.api.modules.payroll.service.PayrollService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

  private final PayrollService payrollService;

  public PayrollController(PayrollService payrollService) {
    this.payrollService = payrollService;
  }

  // ==================== 薪资项 ====================

  @GetMapping("/items")
  @PreAuthorize("hasAuthority('payroll:view')")
  public ApiResponse<PageResponse<SalaryItemResponse>> listItems(
      @RequestParam(required = false) UUID employeeId,
      @PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(payrollService.listItems(employeeId, pageable)));
  }

  @PostMapping("/items")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('payroll:manage')")
  public ApiResponse<SalaryItemResponse> createItem(@Valid @RequestBody SalaryItemRequest request) {
    return ApiResponse.ok(payrollService.createItem(request));
  }

  @PutMapping("/items/{id}")
  @PreAuthorize("hasAuthority('payroll:manage')")
  public ApiResponse<SalaryItemResponse> updateItem(@PathVariable UUID id,
      @Valid @RequestBody SalaryItemRequest request) {
    return ApiResponse.ok(payrollService.updateItem(id, request));
  }

  @DeleteMapping("/items/{id}")
  @PreAuthorize("hasAuthority('payroll:manage')")
  public ApiResponse<Void> deleteItem(@PathVariable UUID id) {
    payrollService.deleteItem(id);
    return ApiResponse.ok();
  }

  // ==================== 工资核算 ====================

  @PostMapping("/runs")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('payroll:manage')")
  public ApiResponse<PayrollRunResponse> runPayroll(@Valid @RequestBody RunPayrollRequest request) {
    return ApiResponse.ok(payrollService.runPayroll(request.period()));
  }

  @GetMapping("/runs")
  @PreAuthorize("hasAuthority('payroll:view')")
  public ApiResponse<PageResponse<PayrollRunResponse>> listRuns(
      @PageableDefault(size = 50) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(payrollService.listRuns(pageable)));
  }

  @GetMapping("/runs/{id}")
  @PreAuthorize("hasAuthority('payroll:view')")
  public ApiResponse<PayrollRunResponse> getRun(@PathVariable UUID id) {
    return ApiResponse.ok(payrollService.getRun(id));
  }

  @PostMapping("/runs/{period}/pay")
  @PreAuthorize("hasAuthority('payroll:manage')")
  public ApiResponse<PayrollRunResponse> pay(@PathVariable String period) {
    return ApiResponse.ok(payrollService.pay(period));
  }

  // ==================== 参数 ====================

  @GetMapping("/configs")
  @PreAuthorize("hasAuthority('payroll:view')")
  public ApiResponse<List<ConfigResponse>> listConfigs() {
    return ApiResponse.ok(payrollService.listConfigs());
  }

  @PostMapping("/configs")
  @PreAuthorize("hasAuthority('payroll:manage')")
  public ApiResponse<ConfigResponse> saveConfig(@Valid @RequestBody ConfigRequest request) {
    return ApiResponse.ok(payrollService.saveConfig(request));
  }

  // ==================== 台账 ====================

  @GetMapping("/ledger/social-tax")
  @PreAuthorize("hasAuthority('payroll:view')")
  public ApiResponse<List<SocialTaxLedgerResponse>> socialTaxLedger() {
    return ApiResponse.ok(payrollService.socialTaxLedger());
  }
}
