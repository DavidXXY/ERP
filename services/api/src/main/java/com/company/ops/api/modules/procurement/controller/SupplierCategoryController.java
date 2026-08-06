package com.company.ops.api.modules.procurement.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.procurement.dto.SupplierCategoryRequest;
import com.company.ops.api.modules.procurement.dto.SupplierCategoryResponse;
import com.company.ops.api.modules.procurement.service.SupplierCategoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/procurement/supplier-categories")
public class SupplierCategoryController {
  private final SupplierCategoryService service;

  public SupplierCategoryController(SupplierCategoryService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<SupplierCategoryResponse>> list() {
    return ApiResponse.ok(service.list());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('procurement:supplier:create')")
  public ApiResponse<SupplierCategoryResponse> create(
      @Valid @RequestBody SupplierCategoryRequest request
  ) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('procurement:supplier:create')")
  public ApiResponse<SupplierCategoryResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody SupplierCategoryRequest request
  ) {
    return ApiResponse.ok(service.update(id, request));
  }
}
