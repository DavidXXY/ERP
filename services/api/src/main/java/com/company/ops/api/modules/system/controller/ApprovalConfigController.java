package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalFlowPreviewResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalFlowDiagnostic;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalFlowVersionResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.BatchPreviewApprovalFlowRequest;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.CopyApprovalFlowRequest;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalConfigResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.CreateApprovalConfigRequest;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.PreviewApprovalFlowRequest;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.UpdateApprovalConfigRequest;
import com.company.ops.api.modules.system.service.ApprovalConfigService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/approval-configs")
public class ApprovalConfigController {
  private final ApprovalConfigService service;
  public ApprovalConfigController(ApprovalConfigService service) { this.service = service; }

  @GetMapping
  @PreAuthorize("hasAuthority('system:role:view')")
  public ApiResponse<List<ApprovalConfigResponse>> list() { return ApiResponse.ok(service.list()); }

  @PostMapping("/preview")
  @PreAuthorize("hasAuthority('system:role:view')")
  public ApiResponse<ApprovalFlowPreviewResponse> preview(@Valid @RequestBody PreviewApprovalFlowRequest request) { return ApiResponse.ok(service.preview(request)); }

  @PostMapping("/batch-preview")
  @PreAuthorize("hasAuthority('system:role:view')")
  public ApiResponse<List<ApprovalFlowPreviewResponse>> batchPreview(@Valid @RequestBody BatchPreviewApprovalFlowRequest request) { return ApiResponse.ok(service.batchPreview(request)); }

  @GetMapping("/diagnostics")
  @PreAuthorize("hasAuthority('system:role:view')")
  public ApiResponse<List<ApprovalFlowDiagnostic>> diagnostics() { return ApiResponse.ok(service.diagnostics()); }

  @PostMapping("/copy")
  @PreAuthorize("hasAuthority('system:role:update')")
  public ApiResponse<List<ApprovalConfigResponse>> copy(@Valid @RequestBody CopyApprovalFlowRequest request) { return ApiResponse.ok(service.copyFlow(request)); }

  @GetMapping("/{flowCode}/versions")
  @PreAuthorize("hasAuthority('system:role:view')")
  public ApiResponse<List<ApprovalFlowVersionResponse>> versions(@PathVariable String flowCode) { return ApiResponse.ok(service.versions(flowCode)); }

  @PostMapping("/{flowCode}/publish")
  @PreAuthorize("hasAuthority('system:role:update')")
  public ApiResponse<List<ApprovalConfigResponse>> publish(@PathVariable String flowCode) { return ApiResponse.ok(service.publish(flowCode)); }

  @PostMapping("/{flowCode}/rollback/{versionNo}")
  @PreAuthorize("hasAuthority('system:role:update')")
  public ApiResponse<List<ApprovalConfigResponse>> rollback(@PathVariable String flowCode, @PathVariable int versionNo) { return ApiResponse.ok(service.rollback(flowCode, versionNo)); }

  @PostMapping
  @PreAuthorize("hasAuthority('system:role:update')")
  public ApiResponse<ApprovalConfigResponse> create(@Valid @RequestBody CreateApprovalConfigRequest request) { return ApiResponse.ok(service.create(request)); }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('system:role:update')")
  public ApiResponse<ApprovalConfigResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateApprovalConfigRequest request) { return ApiResponse.ok(service.update(id, request)); }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('system:role:update')")
  public ApiResponse<Void> delete(@PathVariable UUID id) { service.delete(id); return ApiResponse.ok(); }
}
