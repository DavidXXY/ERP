package com.company.ops.api.modules.procurement.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.inventory.dto.CreateMaterialMasterRequest;
import com.company.ops.api.modules.inventory.dto.InventoryPartResponse;
import com.company.ops.api.modules.inventory.dto.UpdateMaterialMasterRequest;
import com.company.ops.api.modules.procurement.dto.CreateMaterialCategoryRequest;
import com.company.ops.api.modules.procurement.dto.MaterialCategoryResponse;
import com.company.ops.api.modules.procurement.dto.MaterialDeletionResponse;
import com.company.ops.api.modules.procurement.service.ProcurementMaterialService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procurement/materials")
public class ProcurementMaterialController {

  private final ProcurementMaterialService materialService;

  public ProcurementMaterialController(ProcurementMaterialService materialService) {
    this.materialService = materialService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<PageResponse<InventoryPartResponse>> listMaterials(
      @PageableDefault(size = 100) Pageable pageable
  ) {
    return ApiResponse.ok(PageResponse.from(materialService.listMaterials(pageable)));
  }

  @GetMapping("/categories")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<MaterialCategoryResponse>> listCategories() {
    return ApiResponse.ok(materialService.listCategories());
  }

  @PostMapping("/categories")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('procurement:material:manage')")
  public ApiResponse<MaterialCategoryResponse> createCategory(
      @Valid @RequestBody CreateMaterialCategoryRequest request
  ) {
    return ApiResponse.ok(materialService.createCategory(request));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('procurement:material:manage')")
  public ApiResponse<InventoryPartResponse> createMaterial(
      @Valid @RequestBody CreateMaterialMasterRequest request
  ) {
    return ApiResponse.ok(materialService.createMaterial(request));
  }

  @PutMapping("/{materialId}")
  @PreAuthorize("hasAuthority('procurement:material:manage')")
  public ApiResponse<InventoryPartResponse> updateMaterial(
      @PathVariable UUID materialId,
      @Valid @RequestBody UpdateMaterialMasterRequest request
  ) {
    return ApiResponse.ok(materialService.updateMaterial(materialId, request));
  }

  @DeleteMapping("/{materialId}")
  @PreAuthorize("hasAuthority('procurement:material:manage')")
  public ApiResponse<MaterialDeletionResponse> deleteMaterial(@PathVariable UUID materialId) {
    return ApiResponse.ok(materialService.deleteMaterial(materialId));
  }
}
