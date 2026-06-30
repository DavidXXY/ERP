package com.company.ops.api.modules.inventory.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.inventory.dto.CreateInventoryPartRequest;
import com.company.ops.api.modules.inventory.dto.CreateMaterialIssueRequest;
import com.company.ops.api.modules.inventory.dto.CreateMaterialReturnRequest;
import com.company.ops.api.modules.inventory.dto.CreateStockMovementRequest;
import com.company.ops.api.modules.inventory.dto.InventoryPartResponse;
import com.company.ops.api.modules.inventory.dto.InventoryProjectOptionResponse;
import com.company.ops.api.modules.inventory.dto.MaterialIssueResponse;
import com.company.ops.api.modules.inventory.dto.MaterialReturnResponse;
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
@RequestMapping("/api/inventory")
public class InventoryController {

  private final InventoryService inventoryService;

  public InventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @GetMapping("/parts")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ApiResponse<List<InventoryPartResponse>> listParts() {
    return ApiResponse.ok(inventoryService.listParts());
  }

  @GetMapping("/eligible-projects")
  @PreAuthorize("hasAnyAuthority('inventory:view', 'inventory:issue:create')")
  public ApiResponse<List<InventoryProjectOptionResponse>> listEligibleProjects() {
    return ApiResponse.ok(inventoryService.listEligibleProjects());
  }

  @PostMapping("/parts")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('inventory:part:create')")
  public ApiResponse<InventoryPartResponse> createPart(@Valid @RequestBody CreateInventoryPartRequest request) {
    return ApiResponse.ok(inventoryService.createPart(request));
  }

  @GetMapping("/parts/{partId}/movements")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ApiResponse<List<StockMovementResponse>> listMovements(@PathVariable UUID partId) {
    return ApiResponse.ok(inventoryService.listMovements(partId));
  }

  @PostMapping("/parts/{partId}/movements")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('inventory:movement:create')")
  public ApiResponse<InventoryPartResponse> createMovement(
      @PathVariable UUID partId,
      @Valid @RequestBody CreateStockMovementRequest request
  ) {
    return ApiResponse.ok(inventoryService.createMovement(partId, request));
  }

  @GetMapping("/issues")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ApiResponse<List<MaterialIssueResponse>> listIssues() {
    return ApiResponse.ok(inventoryService.listIssues());
  }

  @PostMapping("/issues")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('inventory:issue:create')")
  public ApiResponse<MaterialIssueResponse> createIssue(
      @Valid @RequestBody CreateMaterialIssueRequest request
  ) {
    return ApiResponse.ok(inventoryService.createIssue(request));
  }

  @GetMapping("/returns")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ApiResponse<List<MaterialReturnResponse>> listReturns() {
    return ApiResponse.ok(inventoryService.listReturns());
  }

  @PostMapping("/issues/{issueId}/returns")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('inventory:return:create')")
  public ApiResponse<MaterialReturnResponse> createReturn(
      @PathVariable UUID issueId,
      @Valid @RequestBody CreateMaterialReturnRequest request
  ) {
    return ApiResponse.ok(inventoryService.createReturn(issueId, request));
  }
}
