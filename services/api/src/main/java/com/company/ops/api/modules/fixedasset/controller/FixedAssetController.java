package com.company.ops.api.modules.fixedasset.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.CountLineResponse;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.CountResponse;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.CreateAssetRequest;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.CreateCountRequest;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.CreateDisposalRequest;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.CreateTransferRequest;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.DepreciationRunResponse;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.DisposalResponse;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.FixedAssetResponse;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.RecordCountLineRequest;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.RunDepreciationRequest;
import com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.TransferResponse;
import com.company.ops.api.modules.fixedasset.service.FixedAssetService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fixedassets")
public class FixedAssetController {

  private final FixedAssetService fixedAssetService;

  public FixedAssetController(FixedAssetService fixedAssetService) {
    this.fixedAssetService = fixedAssetService;
  }

  @GetMapping("/assets")
  @PreAuthorize("hasAuthority('fixedasset:view')")
  public ApiResponse<PageResponse<FixedAssetResponse>> listAssets(
      @PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(fixedAssetService.listAssets(pageable)));
  }

  @GetMapping("/assets/{id}")
  @PreAuthorize("hasAuthority('fixedasset:view')")
  public ApiResponse<FixedAssetResponse> getAsset(@PathVariable UUID id) {
    return ApiResponse.ok(fixedAssetService.getAsset(id));
  }

  @PostMapping("/assets")
  @PreAuthorize("hasAuthority('fixedasset:manage')")
  public ApiResponse<FixedAssetResponse> createAsset(@Valid @RequestBody CreateAssetRequest request) {
    return ApiResponse.ok(fixedAssetService.createAsset(request));
  }

  @PostMapping("/depreciations")
  @PreAuthorize("hasAuthority('fixedasset:manage')")
  public ApiResponse<DepreciationRunResponse> runDepreciation(
      @Valid @RequestBody RunDepreciationRequest request) {
    return ApiResponse.ok(fixedAssetService.runDepreciation(request));
  }

  @GetMapping("/depreciations")
  @PreAuthorize("hasAuthority('fixedasset:view')")
  public ApiResponse<PageResponse<DepreciationRunResponse>> listDepreciationRuns(
      @PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(fixedAssetService.listDepreciationRuns(pageable)));
  }

  @GetMapping("/depreciations/{id}")
  @PreAuthorize("hasAuthority('fixedasset:view')")
  public ApiResponse<DepreciationRunResponse> getDepreciationRun(@PathVariable UUID id) {
    return ApiResponse.ok(fixedAssetService.getDepreciationRun(id));
  }

  @PostMapping("/transfers")
  @PreAuthorize("hasAuthority('fixedasset:manage')")
  public ApiResponse<TransferResponse> transfer(@Valid @RequestBody CreateTransferRequest request) {
    return ApiResponse.ok(fixedAssetService.transfer(request));
  }

  @GetMapping("/transfers")
  @PreAuthorize("hasAuthority('fixedasset:view')")
  public ApiResponse<PageResponse<TransferResponse>> listTransfers(
      @PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(fixedAssetService.listTransfers(pageable)));
  }

  @PostMapping("/disposals")
  @PreAuthorize("hasAuthority('fixedasset:manage')")
  public ApiResponse<DisposalResponse> dispose(@Valid @RequestBody CreateDisposalRequest request) {
    return ApiResponse.ok(fixedAssetService.dispose(request));
  }

  @GetMapping("/disposals")
  @PreAuthorize("hasAuthority('fixedasset:view')")
  public ApiResponse<PageResponse<DisposalResponse>> listDisposals(
      @PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(fixedAssetService.listDisposals(pageable)));
  }

  @PostMapping("/counts")
  @PreAuthorize("hasAuthority('fixedasset:manage')")
  public ApiResponse<CountResponse> createCount(@Valid @RequestBody CreateCountRequest request) {
    return ApiResponse.ok(fixedAssetService.createCount(request));
  }

  @GetMapping("/counts")
  @PreAuthorize("hasAuthority('fixedasset:view')")
  public ApiResponse<PageResponse<CountResponse>> listCounts(
      @PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(fixedAssetService.listCounts(pageable)));
  }

  @GetMapping("/counts/{id}")
  @PreAuthorize("hasAuthority('fixedasset:view')")
  public ApiResponse<CountResponse> getCount(@PathVariable UUID id) {
    return ApiResponse.ok(fixedAssetService.getCount(id));
  }

  @PutMapping("/counts/{countId}/lines/{lineId}")
  @PreAuthorize("hasAuthority('fixedasset:manage')")
  public ApiResponse<CountLineResponse> recordCountLine(
      @PathVariable UUID countId, @PathVariable UUID lineId,
      @Valid @RequestBody RecordCountLineRequest request) {
    return ApiResponse.ok(fixedAssetService.recordCountLine(countId, lineId, request));
  }

  @PostMapping("/counts/{id}/post")
  @PreAuthorize("hasAuthority('fixedasset:manage')")
  public ApiResponse<CountResponse> postCount(@PathVariable UUID id) {
    return ApiResponse.ok(fixedAssetService.postCount(id));
  }
}
