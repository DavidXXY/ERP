package com.company.ops.api.modules.finance.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReceivableResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RecordReceiptRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RegisterInvoiceRequest;
import com.company.ops.api.modules.crm.service.CrmOperationsService;
import com.company.ops.api.modules.finance.dto.CreatePaymentApplicationRequest;
import com.company.ops.api.modules.finance.dto.ExecutePaymentRequest;
import com.company.ops.api.modules.finance.dto.FinanceOverviewResponse;
import com.company.ops.api.modules.finance.dto.FinancePayableResponse;
import com.company.ops.api.modules.finance.dto.PaymentApplicationResponse;
import com.company.ops.api.modules.finance.dto.PaymentRecordResponse;
import com.company.ops.api.modules.finance.dto.ProcessPaymentApplicationRequest;
import com.company.ops.api.modules.finance.service.FinanceService;
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
@RequestMapping("/api/finance")
public class FinanceController {

  private final FinanceService financeService;
  private final CrmOperationsService crmOperationsService;

  public FinanceController(
      FinanceService financeService,
      CrmOperationsService crmOperationsService
  ) {
    this.financeService = financeService;
    this.crmOperationsService = crmOperationsService;
  }

  @GetMapping("/overview")
  @PreAuthorize("hasAuthority('finance:view')")
  public ApiResponse<FinanceOverviewResponse> overview() {
    return ApiResponse.ok(financeService.overview());
  }

  @GetMapping("/receivables")
  @PreAuthorize("hasAuthority('finance:receivable:view')")
  public ApiResponse<List<ReceivableResponse>> listReceivables() {
    return ApiResponse.ok(crmOperationsService.listReceivables());
  }

  @PostMapping("/receivables/{id}/invoice")
  @PreAuthorize("hasAuthority('finance:receivable:invoice')")
  public ApiResponse<ReceivableResponse> registerInvoice(
      @PathVariable UUID id,
      @Valid @RequestBody RegisterInvoiceRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.registerInvoice(id, request));
  }

  @PostMapping("/receivables/{id}/receipts")
  @PreAuthorize("hasAuthority('finance:receivable:collect')")
  public ApiResponse<ReceivableResponse> recordReceipt(
      @PathVariable UUID id,
      @Valid @RequestBody RecordReceiptRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.recordReceipt(id, request));
  }

  @GetMapping("/payables")
  @PreAuthorize("hasAuthority('finance:payable:view')")
  public ApiResponse<List<FinancePayableResponse>> listPayables() {
    return ApiResponse.ok(financeService.listPayables());
  }

  @GetMapping("/payment-applications")
  @PreAuthorize("hasAnyAuthority('finance:payable:view', 'finance:payment:approve', 'finance:payment:execute')")
  public ApiResponse<List<PaymentApplicationResponse>> listApplications() {
    return ApiResponse.ok(financeService.listApplications());
  }

  @PostMapping("/payment-applications")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('finance:payment:apply')")
  public ApiResponse<PaymentApplicationResponse> createApplication(
      @Valid @RequestBody CreatePaymentApplicationRequest request
  ) {
    return ApiResponse.ok(financeService.createApplication(request));
  }

  @PostMapping("/payment-applications/{id}/approval")
  @PreAuthorize("hasAuthority('finance:payment:approve')")
  public ApiResponse<PaymentApplicationResponse> processApplication(
      @PathVariable UUID id,
      @Valid @RequestBody ProcessPaymentApplicationRequest request
  ) {
    return ApiResponse.ok(financeService.processApplication(id, request));
  }

  @PostMapping("/payment-applications/{id}/payment")
  @PreAuthorize("hasAuthority('finance:payment:execute')")
  public ApiResponse<PaymentRecordResponse> executePayment(
      @PathVariable UUID id,
      @Valid @RequestBody ExecutePaymentRequest request
  ) {
    return ApiResponse.ok(financeService.executePayment(id, request));
  }

  @GetMapping("/payments")
  @PreAuthorize("hasAnyAuthority('finance:payable:view', 'finance:payment:approve', 'finance:payment:execute')")
  public ApiResponse<List<PaymentRecordResponse>> listPayments() {
    return ApiResponse.ok(financeService.listPayments());
  }
}
