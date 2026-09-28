package com.company.ops.api.modules.inventory.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.inventory.dto.StocktakeDtos.*;
import com.company.ops.api.modules.inventory.service.StocktakeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/stocktakes")
public class StocktakeController {

  private final StocktakeService stocktakeService;

  public StocktakeController(StocktakeService stocktakeService) {
    this.stocktakeService = stocktakeService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('inventory:stocktake:manage')")
  public ApiResponse<PageResponse<StocktakeResponse>> list(@PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(stocktakeService.list(pageable)));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('inventory:stocktake:manage')")
  public ApiResponse<StocktakeResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(stocktakeService.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('inventory:stocktake:manage')")
  public ApiResponse<StocktakeResponse> create(@Valid @RequestBody CreateStocktakeRequest request) {
    return ApiResponse.ok(stocktakeService.create(request));
  }

  @PutMapping("/{stocktakeId}/lines/{lineId}")
  @PreAuthorize("hasAuthority('inventory:stocktake:manage')")
  public ApiResponse<StocktakeResponse> updateLine(
      @PathVariable UUID stocktakeId,
      @PathVariable UUID lineId,
      @Valid @RequestBody UpdateStocktakeLineRequest request
  ) {
    return ApiResponse.ok(stocktakeService.updateLine(stocktakeId, lineId, request));
  }

  @PostMapping("/{id}/post")
  @PreAuthorize("hasAuthority('inventory:stocktake:manage')")
  public ApiResponse<StocktakeResponse> post(@PathVariable UUID id) {
    return ApiResponse.ok(stocktakeService.post(id));
  }
}