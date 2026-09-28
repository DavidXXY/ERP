package com.company.ops.api.modules.sales.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.sales.dto.SalesDtos.*;
import com.company.ops.api.modules.sales.service.SalesService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/sales")
public class SalesController {

  private final SalesService salesService;

  public SalesController(SalesService salesService) {
    this.salesService = salesService;
  }

  @GetMapping("/orders")
  @PreAuthorize("hasAuthority('sales:order:manage')")
  public ApiResponse<PageResponse<SalesOrderResponse>> listOrders(@PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(salesService.listOrders(pageable)));
  }

  @GetMapping("/orders/{id}")
  @PreAuthorize("hasAuthority('sales:order:manage')")
  public ApiResponse<SalesOrderResponse> getOrder(@PathVariable UUID id) {
    return ApiResponse.ok(salesService.getOrder(id));
  }

  @PostMapping("/orders")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('sales:order:manage')")
  public ApiResponse<SalesOrderResponse> createOrder(@Valid @RequestBody CreateSalesOrderRequest request) {
    return ApiResponse.ok(salesService.createOrder(request));
  }

  @PostMapping("/orders/{id}/approve")
  @PreAuthorize("hasAuthority('sales:order:manage')")
  public ApiResponse<SalesOrderResponse> approveOrder(@PathVariable UUID id) {
    return ApiResponse.ok(salesService.approveOrder(id));
  }

  @PostMapping("/orders/{id}/cancel")
  @PreAuthorize("hasAuthority('sales:order:manage')")
  public ApiResponse<SalesOrderResponse> cancelOrder(@PathVariable UUID id) {
    return ApiResponse.ok(salesService.cancelOrder(id));
  }

  @GetMapping("/shipments")
  @PreAuthorize("hasAuthority('sales:shipment:manage')")
  public ApiResponse<PageResponse<ShipmentResponse>> listShipments(@PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(salesService.listShipments(pageable)));
  }

  @PostMapping("/shipments")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('sales:shipment:manage')")
  public ApiResponse<ShipmentResponse> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
    return ApiResponse.ok(salesService.createShipment(request));
  }

  @GetMapping("/returns")
  @PreAuthorize("hasAuthority('sales:return:manage')")
  public ApiResponse<PageResponse<ReturnResponse>> listReturns(@PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(salesService.listReturns(pageable)));
  }

  @PostMapping("/shipments/{shipmentId}/returns")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('sales:return:manage')")
  public ApiResponse<ReturnResponse> createReturn(
      @PathVariable UUID shipmentId,
      @Valid @RequestBody CreateReturnRequest request
  ) {
    return ApiResponse.ok(salesService.createReturn(request));
  }
}