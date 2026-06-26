package com.company.ops.api.modules.inventory.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.inventory.dto.CreateInventoryPartRequest;
import com.company.ops.api.modules.inventory.dto.CreateStockMovementRequest;
import com.company.ops.api.modules.inventory.dto.InventoryPartResponse;
import com.company.ops.api.modules.inventory.dto.StockMovementResponse;
import com.company.ops.api.modules.inventory.service.InventoryService;
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
@RequestMapping("/api/inventory/parts")
public class InventoryController {

  private final InventoryService inventoryService;

  public InventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('inventory:view')")
  public ApiResponse<List<InventoryPartResponse>> listParts() {
    return ApiResponse.ok(inventoryService.listParts());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('inventory:part:create')")
  public ApiResponse<InventoryPartResponse> createPart(@Valid @RequestBody CreateInventoryPartRequest request) {
    return ApiResponse.ok(inventoryService.createPart(request));
  }

  @GetMapping("/{partId}/movements")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ApiResponse<List<StockMovementResponse>> listMovements(@PathVariable UUID partId) {
    return ApiResponse.ok(inventoryService.listMovements(partId));
  }

  @PostMapping("/{partId}/movements")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('inventory:movement:create')")
  public ApiResponse<InventoryPartResponse> createMovement(
      @PathVariable UUID partId,
      @Valid @RequestBody CreateStockMovementRequest request
  ) {
    return ApiResponse.ok(inventoryService.createMovement(partId, request));
  }
}
