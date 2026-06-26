package com.company.ops.api.modules.procurement.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseRequestRequest;
import com.company.ops.api.modules.procurement.dto.CreateSupplierRequest;
import com.company.ops.api.modules.procurement.dto.PurchaseOrderResponse;
import com.company.ops.api.modules.procurement.dto.PurchaseRequestResponse;
import com.company.ops.api.modules.procurement.dto.SupplierResponse;
import com.company.ops.api.modules.procurement.service.ProcurementService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procurement")
public class ProcurementController {

  private final ProcurementService procurementService;

  public ProcurementController(ProcurementService procurementService) {
    this.procurementService = procurementService;
  }

  @GetMapping("/suppliers")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<SupplierResponse>> listSuppliers() {
    return ApiResponse.ok(procurementService.listSuppliers());
  }

  @PostMapping("/suppliers")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('procurement:supplier:create')")
  public ApiResponse<SupplierResponse> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
    return ApiResponse.ok(procurementService.createSupplier(request));
  }

  @GetMapping("/requests")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<PurchaseRequestResponse>> listPurchaseRequests() {
    return ApiResponse.ok(procurementService.listPurchaseRequests());
  }

  @PostMapping("/requests")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<PurchaseRequestResponse> createPurchaseRequest(
      @Valid @RequestBody CreatePurchaseRequestRequest request
  ) {
    return ApiResponse.ok(procurementService.createPurchaseRequest(request));
  }

  @GetMapping("/orders")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<PurchaseOrderResponse>> listPurchaseOrders() {
    return ApiResponse.ok(procurementService.listPurchaseOrders());
  }

  @PostMapping("/orders")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<PurchaseOrderResponse> createPurchaseOrder(
      @Valid @RequestBody CreatePurchaseOrderRequest request
  ) {
    return ApiResponse.ok(procurementService.createPurchaseOrder(request));
  }
}
