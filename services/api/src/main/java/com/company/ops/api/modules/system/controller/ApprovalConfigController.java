package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalConfigResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.CreateApprovalConfigRequest;
import com.company.ops.api.modules.system.service.ApprovalConfigService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  @PostMapping
  @PreAuthorize("hasAuthority('system:role:update')")
  public ApiResponse<ApprovalConfigResponse> create(@Valid @RequestBody CreateApprovalConfigRequest request) { return ApiResponse.ok(service.create(request)); }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('system:role:update')")
  public ApiResponse<Void> delete(@PathVariable UUID id) { service.delete(id); return ApiResponse.ok(); }
}
