package com.company.ops.api.modules.procurement.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.procurement.domain.GoodsReceipt;
import com.company.ops.api.modules.procurement.domain.ProcurementReturnOrder;
import com.company.ops.api.modules.procurement.domain.SupplierInvoice;
import com.company.ops.api.modules.procurement.dto.ProcurementControlDtos.*;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.InvoiceSubmissionResponse;
import com.company.ops.api.modules.procurement.dto.CreateConsolidatedInquiryRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementPurchasePoolResponse;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.service.PortalCollaborationService;
import com.company.ops.api.modules.procurement.service.ProcurementControlService;
import com.company.ops.api.modules.procurement.service.ProcurementReturnService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/procurement")
public class ProcurementControlController {
  private final ProcurementControlService service;
  private final ProcurementReturnService returnService;
  private final PortalCollaborationService collaboration;

  public ProcurementControlController(
      ProcurementControlService service,
      ProcurementReturnService returnService,
      PortalCollaborationService collaboration
  ) {
    this.service = service;
    this.returnService = returnService;
    this.collaboration = collaboration;
  }

  @GetMapping("/inquiries")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<Map<String, Object>>> inquiries() {
    return ApiResponse.ok(service.listInquiries());
  }

  @GetMapping("/purchase-pool")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<ProcurementPurchasePoolResponse> purchasePool() {
    return ApiResponse.ok(service.purchasePool());
  }

  @PostMapping("/purchase-pool/inquiries")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<Map<String, Object>> createConsolidatedInquiry(
      @Valid @RequestBody CreateConsolidatedInquiryRequest request
  ) {
    return ApiResponse.ok(service.createConsolidatedInquiry(request));
  }

  @PostMapping("/inquiries")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<Map<String, Object>> createInquiry(@Valid @RequestBody CreateInquiry request) {
    return ApiResponse.ok(service.createInquiry(request));
  }

  @PostMapping("/inquiries/{id}/quotes")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<Map<String, Object>> addQuote(
      @PathVariable UUID id,
      @Valid @RequestBody CreateSupplierQuote request
  ) {
    return ApiResponse.ok(service.addQuote(id, request));
  }

  @PostMapping("/inquiries/{id}/invitations")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<Map<String, Object>> inviteSuppliers(
      @PathVariable UUID id,
      @Valid @RequestBody InviteSuppliers request
  ) {
    return ApiResponse.ok(service.inviteSuppliers(id, request));
  }

  @PostMapping("/inquiries/{id}/deadline")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<Map<String, Object>> updateInquiryDeadline(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateInquiryDeadline request
  ) {
    return ApiResponse.ok(service.updateInquiryDeadline(id, request));
  }

  @PostMapping("/inquiries/{id}/min-quotes")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<Map<String, Object>> updateInquiryMinQuotes(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateInquiryMinQuotes request
  ) {
    return ApiResponse.ok(service.updateInquiryMinQuotes(id, request));
  }

  @PostMapping("/inquiries/{id}/quotes/{quoteId}/score")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<Map<String, Object>> scoreQuote(
      @PathVariable UUID id,
      @PathVariable UUID quoteId,
      @Valid @RequestBody ScoreSupplierQuote request
  ) {
    return ApiResponse.ok(service.scoreQuote(id, quoteId, request));
  }

  @PostMapping("/inquiries/{id}/quotes/{quoteId}/select")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<Map<String, Object>> selectQuote(
      @PathVariable UUID id,
      @PathVariable UUID quoteId,
      @Valid @RequestBody SelectSupplierQuote request
  ) {
    return ApiResponse.ok(service.selectQuote(id, quoteId, request));
  }

  @PostMapping("/orders/{id}/arrivals")
  @PreAuthorize("hasAuthority('procurement:order:receive')")
  public ApiResponse<GoodsReceipt> arrival(
      @PathVariable UUID id,
      @Valid @RequestBody ReceivePurchaseOrderRequest request
  ) {
    return ApiResponse.ok(service.registerArrival(id, request));
  }

  @PostMapping("/receipts/{id}/inspection")
  @PreAuthorize("hasAuthority('procurement:receipt:inspect')")
  public ApiResponse<Map<String, Object>> inspect(
      @PathVariable UUID id,
      @Valid @RequestBody InspectReceipt request
  ) {
    return ApiResponse.ok(service.inspect(id, request));
  }

  @GetMapping("/returns")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<ProcurementReturnOrder>> returns() {
    return ApiResponse.ok(returnService.listReturns());
  }

  @PostMapping("/returns/{id}/resolve")
  @PreAuthorize("hasAuthority('procurement:order:receive')")
  public ApiResponse<ProcurementReturnOrder> resolveReturn(
      @PathVariable UUID id,
      @Valid @RequestBody ResolveReturn request
  ) {
    return ApiResponse.ok(returnService.resolveReturn(id, request));
  }

  @PostMapping("/supplier-invoices")
  @PreAuthorize("hasAuthority('procurement:payable:view')")
  public ApiResponse<SupplierInvoice> invoice(@Valid @RequestBody CreateInvoice request) {
    return ApiResponse.ok(service.createInvoice(request));
  }

  @PostMapping("/supplier-invoices/{id}/review")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<SupplierInvoice> reviewInvoice(
      @PathVariable UUID id,
      @Valid @RequestBody ReviewInvoice request
  ) {
    return ApiResponse.ok(service.reviewInvoice(id, request));
  }

  @PostMapping("/supplier-invoices/{id}/verify")
  @PreAuthorize("hasAuthority('procurement:payable:view')")
  public ApiResponse<SupplierInvoice> verifyInvoice(
      @PathVariable UUID id,
      @Valid @RequestBody VerifyInvoice request
  ) {
    return ApiResponse.ok(service.verifyInvoice(id, request));
  }

  @GetMapping("/supplier-invoices")
  @PreAuthorize("hasAuthority('procurement:payable:view')")
  public ApiResponse<List<SupplierInvoice>> invoices() {
    return ApiResponse.ok(service.listInvoices());
  }

  @GetMapping("/invoice-submissions")
  @PreAuthorize("hasAuthority('procurement:payable:view')")
  public ApiResponse<List<InvoiceSubmissionResponse>> invoiceSubmissions(
      @RequestParam(required = false) String status
  ) {
    return ApiResponse.ok(service.listInvoiceSubmissions(status));
  }

  @GetMapping("/invoice-submissions/{id}/download")
  @PreAuthorize("hasAuthority('procurement:payable:view')")
  public ResponseEntity<Resource> downloadInvoiceSubmission(@PathVariable UUID id) {
    InvoiceSubmissionResponse metadata = service.invoiceSubmission(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.contentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE : metadata.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(metadata.fileName(), StandardCharsets.UTF_8).build().toString())
        .body(service.loadInvoiceSubmissionFile(id));
  }

  @PostMapping("/invoice-submissions/{id}/review")
  @PreAuthorize("hasAuthority('procurement:request:approve')")
  public ApiResponse<InvoiceSubmissionResponse> reviewInvoiceSubmission(
      @PathVariable UUID id,
      @Valid @RequestBody ReviewInvoiceSubmissionRequest request
  ) {
    return ApiResponse.ok(service.reviewInvoiceSubmission(id, request));
  }

  @GetMapping("/appeals")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<Map<String, Object>>> appeals(
      @RequestParam(required = false) String status
  ) {
    return ApiResponse.ok(service.listAppeals(status));
  }


  @GetMapping("/portal-collaboration/summary")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<Map<String, Object>> portalCollaborationSummary() {
    return ApiResponse.ok(collaboration.summary());
  }

  @PostMapping("/appeals/{id}/resolve")
  @PreAuthorize("hasAuthority('procurement:order:receive')")
  public ApiResponse<Map<String, Object>> resolveAppeal(
      @PathVariable UUID id,
      @Valid @RequestBody ResolveAppealRequest request
  ) {
    return ApiResponse.ok(service.resolveAppeal(id, request));
  }
}
