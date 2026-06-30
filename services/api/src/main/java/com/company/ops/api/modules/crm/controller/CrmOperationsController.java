package com.company.ops.api.modules.crm.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.AdvanceOpportunityRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ContractResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateFollowUpRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateOpportunityRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateQuoteRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CustomerProfileResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.FollowUpResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.OpportunityResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ProcessQuoteApprovalRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteApprovalResult;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReceivableResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RecordReceiptRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RegisterInvoiceRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RenewalResponse;
import com.company.ops.api.modules.crm.service.CrmOperationsService;
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
@RequestMapping("/api/crm")
public class CrmOperationsController {

  private final CrmOperationsService crmOperationsService;

  public CrmOperationsController(CrmOperationsService crmOperationsService) {
    this.crmOperationsService = crmOperationsService;
  }

  @GetMapping("/opportunities")
  @PreAuthorize("hasAuthority('crm:opportunity:view')")
  public ApiResponse<List<OpportunityResponse>> listOpportunities() {
    return ApiResponse.ok(crmOperationsService.listOpportunities());
  }

  @PostMapping("/opportunities")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('crm:opportunity:create')")
  public ApiResponse<OpportunityResponse> createOpportunity(
      @Valid @RequestBody CreateOpportunityRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.createOpportunity(request));
  }

  @PostMapping("/opportunities/{id}/advance")
  @PreAuthorize("hasAuthority('crm:opportunity:update')")
  public ApiResponse<OpportunityResponse> advanceOpportunity(
      @PathVariable UUID id,
      @Valid @RequestBody AdvanceOpportunityRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.advanceOpportunity(id, request));
  }

  @GetMapping("/quotes")
  @PreAuthorize("hasAuthority('crm:quote:view')")
  public ApiResponse<List<QuoteResponse>> listQuotes() {
    return ApiResponse.ok(crmOperationsService.listQuotes());
  }

  @PostMapping("/quotes")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('crm:quote:create')")
  public ApiResponse<QuoteResponse> createQuote(@Valid @RequestBody CreateQuoteRequest request) {
    return ApiResponse.ok(crmOperationsService.createQuote(request));
  }

  @PostMapping("/quotes/{id}/submit")
  @PreAuthorize("hasAuthority('crm:quote:submit')")
  public ApiResponse<QuoteResponse> submitQuote(@PathVariable UUID id) {
    return ApiResponse.ok(crmOperationsService.submitQuote(id));
  }

  @PostMapping("/quotes/{id}/approval")
  @PreAuthorize("hasAuthority('crm:quote:approve')")
  public ApiResponse<QuoteApprovalResult> processQuoteApproval(
      @PathVariable UUID id,
      @Valid @RequestBody ProcessQuoteApprovalRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.processQuoteApproval(id, request));
  }

  @GetMapping("/contracts")
  @PreAuthorize("hasAuthority('crm:contract:view')")
  public ApiResponse<List<ContractResponse>> listContracts() {
    return ApiResponse.ok(crmOperationsService.listContracts());
  }

  @GetMapping("/follow-ups")
  @PreAuthorize("hasAuthority('crm:followup:view')")
  public ApiResponse<List<FollowUpResponse>> listFollowUps() {
    return ApiResponse.ok(crmOperationsService.listFollowUps());
  }

  @PostMapping("/follow-ups")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('crm:followup:create')")
  public ApiResponse<FollowUpResponse> createFollowUp(
      @Valid @RequestBody CreateFollowUpRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.createFollowUp(request));
  }

  @GetMapping("/renewals")
  @PreAuthorize("hasAuthority('crm:renewal:view')")
  public ApiResponse<List<RenewalResponse>> listRenewals() {
    return ApiResponse.ok(crmOperationsService.listRenewals());
  }

  @GetMapping("/receivables")
  @PreAuthorize("hasAuthority('crm:receivable:view')")
  public ApiResponse<List<ReceivableResponse>> listReceivables() {
    return ApiResponse.ok(crmOperationsService.listReceivables());
  }

  @PostMapping("/receivables/{id}/invoice")
  @PreAuthorize("hasAuthority('crm:receivable:invoice')")
  public ApiResponse<ReceivableResponse> registerInvoice(
      @PathVariable UUID id,
      @Valid @RequestBody RegisterInvoiceRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.registerInvoice(id, request));
  }

  @PostMapping("/receivables/{id}/receipts")
  @PreAuthorize("hasAuthority('crm:receivable:settle')")
  public ApiResponse<ReceivableResponse> recordReceipt(
      @PathVariable UUID id,
      @Valid @RequestBody RecordReceiptRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.recordReceipt(id, request));
  }

  @GetMapping("/profiles")
  @PreAuthorize("hasAuthority('crm:profile:view')")
  public ApiResponse<List<CustomerProfileResponse>> listCustomerProfiles() {
    return ApiResponse.ok(crmOperationsService.listCustomerProfiles());
  }
}
