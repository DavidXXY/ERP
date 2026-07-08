package com.company.ops.api.modules.crm.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.AdvanceOpportunityRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ContractResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ConvertQuoteRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateFollowUpRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateOpportunityRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateQuoteRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CustomerProfileResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.FollowUpResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.OpportunityResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ProcessQuoteApprovalRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ProcessQuoteCustomerResultRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteConversionResult;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteRevisionResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReceivableResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RecordReceiptRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RegisterInvoiceRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RenewalResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateQuoteRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateContractChangeRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ContractChangeResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ApprovalActionRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateContractRequest;

import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateReceivableRequest;
import com.company.ops.api.modules.crm.service.CrmOperationsService;
import com.company.ops.api.modules.crm.service.CrmExportService;
import com.company.ops.api.modules.crm.dto.CreateCustomerRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm")
public class CrmOperationsController {

  private final CrmOperationsService crmOperationsService;
  private final CrmExportService crmExportService;

  public CrmOperationsController(CrmOperationsService crmOperationsService, CrmExportService crmExportService) {
    this.crmOperationsService = crmOperationsService;
    this.crmExportService = crmExportService;
  }

  @GetMapping("/opportunities")
  @PreAuthorize("hasAuthority('crm:opportunity:view')")
  public ApiResponse<List<OpportunityResponse>> listOpportunities() {
    return ApiResponse.ok(crmOperationsService.listOpportunities());
  }

  @GetMapping("/opportunities/{id}")
  @PreAuthorize("hasAuthority('crm:opportunity:view')")
  public ApiResponse<OpportunityResponse> getOpportunity(@PathVariable UUID id) {
    return ApiResponse.ok(crmOperationsService.getOpportunity(id));
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

  @GetMapping("/quotes/{id}")
  @PreAuthorize("hasAuthority('crm:quote:view')")
  public ApiResponse<QuoteResponse> getQuote(@PathVariable UUID id) {
    return ApiResponse.ok(crmOperationsService.getQuote(id));
  }

  @PostMapping("/quotes")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('crm:quote:create')")
  public ApiResponse<QuoteResponse> createQuote(@Valid @RequestBody CreateQuoteRequest request) {
    return ApiResponse.ok(crmOperationsService.createQuote(request));
  }

  @PutMapping("/quotes/{id}")
  @PreAuthorize("hasAuthority('crm:quote:update')")
  public ApiResponse<QuoteResponse> updateQuote(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateQuoteRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.updateQuote(id, request));
  }

  @GetMapping("/quotes/{id}/revisions")
  @PreAuthorize("hasAuthority('crm:quote:view')")
  public ApiResponse<List<QuoteRevisionResponse>> listQuoteRevisions(@PathVariable UUID id) {
    return ApiResponse.ok(crmOperationsService.listQuoteRevisions(id));
  }

  @PostMapping("/quotes/{id}/submit")
  @PreAuthorize("hasAuthority('crm:quote:submit')")
  public ApiResponse<QuoteResponse> submitQuote(@PathVariable UUID id) {
    return ApiResponse.ok(crmOperationsService.submitQuote(id));
  }

  @PostMapping("/quotes/{id}/approval")
  @PreAuthorize("hasAuthority('crm:quote:approve')")
  public ApiResponse<QuoteResponse> processQuoteApproval(
      @PathVariable UUID id,
      @Valid @RequestBody ProcessQuoteApprovalRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.processQuoteApproval(id, request));
  }

  @PostMapping("/quotes/{id}/customer-result")
  @PreAuthorize("hasAuthority('crm:quote:customer-result')")
  public ApiResponse<QuoteResponse> processQuoteCustomerResult(
      @PathVariable UUID id,
      @Valid @RequestBody ProcessQuoteCustomerResultRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.processQuoteCustomerResult(id, request));
  }

  @PostMapping("/quotes/{id}/convert")
  @PreAuthorize("hasAuthority('crm:quote:convert')")
  public ApiResponse<QuoteConversionResult> convertQuote(
      @PathVariable UUID id,
      @Valid @RequestBody ConvertQuoteRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.convertQuote(id, request));
  }

  @GetMapping("/contracts")
  @PreAuthorize("hasAuthority('crm:contract:view')")
  public ApiResponse<List<ContractResponse>> listContracts() {
    return ApiResponse.ok(crmOperationsService.listContracts());
  }

  @GetMapping("/contracts/{id}")
  @PreAuthorize("hasAuthority('crm:contract:view')")
  public ApiResponse<ContractResponse> getContract(@PathVariable UUID id) {
    return ApiResponse.ok(crmOperationsService.getContract(id));
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


  @PutMapping("/receivables/{id}")
  @PreAuthorize("hasAuthority('crm:receivable:update')")
  public ApiResponse<ReceivableResponse> updateReceivable(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateReceivableRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.updateReceivable(id, request));
  }

  @PostMapping("/receivables/{id}/receipts")
  @PreAuthorize("hasAuthority('crm:receivable:settle')")
  public ApiResponse<ReceivableResponse> recordReceipt(
      @PathVariable UUID id,
      @Valid @RequestBody RecordReceiptRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.recordReceipt(id, request));
  }

  @DeleteMapping("/opportunities/{id}")
  @PreAuthorize("hasAuthority('crm:opportunity:delete')")
  public ApiResponse<Void> deleteOpportunity(@PathVariable UUID id) {
    crmOperationsService.deleteOpportunity(id);
    return ApiResponse.ok();
  }

  @DeleteMapping("/quotes/{id}")
  @PreAuthorize("hasAuthority('crm:quote:delete')")
  public ApiResponse<Void> deleteQuote(@PathVariable UUID id) {
    crmOperationsService.deleteQuote(id);
    return ApiResponse.ok();
  }

  @DeleteMapping("/contracts/{id}")
  @PreAuthorize("hasAuthority('crm:contract:delete')")
  public ApiResponse<Void> deleteContract(@PathVariable UUID id) {
    crmOperationsService.deleteContract(id);
    return ApiResponse.ok();
  }

  @DeleteMapping("/follow-ups/{id}")
  @PreAuthorize("hasAuthority('crm:followup:delete')")
  public ApiResponse<Void> deleteFollowUp(@PathVariable UUID id) {
    crmOperationsService.deleteFollowUp(id);
    return ApiResponse.ok();
  }

  @GetMapping("/profiles")
  @PreAuthorize("hasAuthority('crm:profile:view')")
  public ApiResponse<List<CustomerProfileResponse>> listCustomerProfiles() {
    return ApiResponse.ok(crmOperationsService.listCustomerProfiles());
  }

  @GetMapping("/contracts/{id}/changes")
  @PreAuthorize("hasAuthority('crm:contract:view')")
  public ApiResponse<List<ContractChangeResponse>> listContractChanges(@PathVariable UUID id) {
    return ApiResponse.ok(crmOperationsService.listContractChanges(id));
  }

  @PostMapping("/contracts/{id}/changes")
  @PreAuthorize("hasAuthority('crm:contract:update')")
  public ApiResponse<ContractChangeResponse> createContractChange(
      @PathVariable UUID id,
      @Valid @RequestBody CreateContractChangeRequest request
  ) {
    CreateContractChangeRequest full = new CreateContractChangeRequest(
        request.changeData(), request.reason(), request.requestedBy());
    return ApiResponse.ok(crmOperationsService.createContractChangeRequest(id, full));
  }

  @PostMapping("/contract-changes/{id}/approve")
  @PreAuthorize("hasAuthority('crm:contract:update')")
  public ApiResponse<ContractChangeResponse> approveContractChange(
      @PathVariable UUID id,
      @Valid @RequestBody ApprovalActionRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.approveContractChange(id, request.operatorName(), request.comment()));
  }

  @PutMapping("/opportunities/{id}")
  @PreAuthorize("hasAuthority('crm:opportunity:update')")
  public ApiResponse<OpportunityResponse> updateOpportunity(
      @PathVariable UUID id,
      @Valid @RequestBody CreateOpportunityRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.updateOpportunity(id, request));
  }

  @PutMapping("/contracts/{id}")
  @PreAuthorize("hasAuthority('crm:contract:update')")
  public ApiResponse<ContractResponse> updateContract(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateContractRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.updateContract(id, request));
  }


  @PostMapping("/contracts/{id}/renew")
  @PreAuthorize("hasAuthority('crm:contract:create')")
  public ApiResponse<ContractResponse> renewContract(@PathVariable UUID id) {
    return ApiResponse.ok(crmOperationsService.renewContract(id));
  }
  @PostMapping("/contract-changes/{id}/reject")
  @PreAuthorize("hasAuthority('crm:contract:update')")
  public ApiResponse<ContractChangeResponse> rejectContractChange(
      @PathVariable UUID id,
      @Valid @RequestBody ApprovalActionRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.rejectContractChange(id, request.operatorName(), request.comment()));
  }

  @GetMapping("/export/customers")
  @PreAuthorize("hasAuthority('crm:customer:view')")
  public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> exportCustomers() {
    var resource = crmExportService.exportCustomersExcel();
    return org.springframework.http.ResponseEntity.ok()
        .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"crm-customers.xlsx\"")
        .body(resource);
  }

  @GetMapping("/export/contracts")
  @PreAuthorize("hasAuthority('crm:contract:view')")
  public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> exportContracts() {
    var resource = crmExportService.exportContractsExcel();
    return org.springframework.http.ResponseEntity.ok()
        .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"crm-contracts.xlsx\"")
        .body(resource);
  }

}
