package com.company.ops.api.modules.procurement.controller;
import com.company.ops.api.common.api.ApiResponse; import com.company.ops.api.modules.procurement.domain.*; import com.company.ops.api.modules.procurement.dto.ProcurementControlDtos.*; import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest; import com.company.ops.api.modules.procurement.service.ProcurementControlService;
import jakarta.validation.Valid; import java.util.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/procurement")
public class ProcurementControlController {
 private final ProcurementControlService service; public ProcurementControlController(ProcurementControlService s){service=s;}
 @GetMapping("/inquiries") @PreAuthorize("hasAuthority('procurement:view')") public ApiResponse<List<Map<String,Object>>> inquiries(){return ApiResponse.ok(service.listInquiries());}
 @PostMapping("/inquiries") @PreAuthorize("hasAuthority('procurement:purchase:create')") public ApiResponse<Map<String,Object>> createInquiry(@Valid @RequestBody CreateInquiry x){return ApiResponse.ok(service.createInquiry(x));}
 @PostMapping("/inquiries/{id}/quotes") @PreAuthorize("hasAuthority('procurement:purchase:create')") public ApiResponse<Map<String,Object>> addQuote(@PathVariable UUID id,@Valid @RequestBody CreateSupplierQuote x){return ApiResponse.ok(service.addQuote(id,x));}
 @PostMapping("/inquiries/{id}/quotes/{quoteId}/select") @PreAuthorize("hasAuthority('procurement:request:approve')") public ApiResponse<Map<String,Object>> selectQuote(@PathVariable UUID id,@PathVariable UUID quoteId,@Valid @RequestBody SelectSupplierQuote x){return ApiResponse.ok(service.selectQuote(id,quoteId,x));}
 @PostMapping("/orders/{id}/arrivals") @PreAuthorize("hasAuthority('procurement:order:receive')") public ApiResponse<GoodsReceipt> arrival(@PathVariable UUID id,@Valid @RequestBody ReceivePurchaseOrderRequest x){return ApiResponse.ok(service.registerArrival(id,x));}
 @PostMapping("/receipts/{id}/inspection") @PreAuthorize("hasAuthority('procurement:order:receive')") public ApiResponse<Map<String,Object>> inspect(@PathVariable UUID id,@Valid @RequestBody InspectReceipt x){return ApiResponse.ok(service.inspect(id,x));}
 @GetMapping("/returns") @PreAuthorize("hasAuthority('procurement:view')") public ApiResponse<List<ProcurementReturnOrder>> returns(){return ApiResponse.ok(service.listReturns());}
 @PostMapping("/supplier-invoices") @PreAuthorize("hasAuthority('procurement:payable:view')") public ApiResponse<SupplierInvoice> invoice(@Valid @RequestBody CreateInvoice x){return ApiResponse.ok(service.createInvoice(x));}
 @GetMapping("/supplier-invoices") @PreAuthorize("hasAuthority('procurement:payable:view')") public ApiResponse<List<SupplierInvoice>> invoices(){return ApiResponse.ok(service.listInvoices());}
}
