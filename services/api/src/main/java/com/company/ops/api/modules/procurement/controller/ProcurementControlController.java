package com.company.ops.api.modules.procurement.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.procurement.domain.GoodsReceipt;
import com.company.ops.api.modules.procurement.domain.ProcurementReturnOrder;
import com.company.ops.api.modules.procurement.domain.SupplierInvoice;
import com.company.ops.api.modules.procurement.dto.ProcurementControlDtos.*;
import com.company.ops.api.modules.procurement.dto.CreateConsolidatedInquiryRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementPurchasePoolResponse;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.service.ProcurementControlService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/procurement")
public class ProcurementControlController {
  private final ProcurementControlService service;

  public ProcurementControlController(ProcurementControlService service) {
    this.service = service;
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
    return ApiResponse.ok(service.listReturns());
  }

  @PostMapping("/returns/{id}/resolve")
  @PreAuthorize("hasAuthority('procurement:order:receive')")
  public ApiResponse<ProcurementReturnOrder> resolveReturn(
      @PathVariable UUID id,
      @Valid @RequestBody ResolveReturn request
  ) {
    return ApiResponse.ok(service.resolveReturn(id, request));
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

  @GetMapping("/supplier-invoices")
  @PreAuthorize("hasAuthority('procurement:payable:view')")
  public ApiResponse<List<SupplierInvoice>> invoices() {
    return ApiResponse.ok(service.listInvoices());
  }
}
