package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.system.dto.CreatePermissionRequest;
import com.company.ops.api.modules.system.dto.PermissionResponse;
import com.company.ops.api.modules.system.dto.UpdatePermissionRequest;
import com.company.ops.api.modules.system.service.SystemService;
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
@RequestMapping("/api/permissions")
public class PermissionController {

  private final SystemService systemService;

  public PermissionController(SystemService systemService) {
    this.systemService = systemService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('system:permission:view')")
  public ApiResponse<List<PermissionResponse>> list() {
    return ApiResponse.ok(systemService.listPermissions());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('system:permission:view')")
  public ApiResponse<PermissionResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(systemService.getPermission(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('system:permission:create')")
  public ApiResponse<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest request) {
    return ApiResponse.ok(systemService.createPermission(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('system:permission:update')")
  public ApiResponse<PermissionResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdatePermissionRequest request) {
    return ApiResponse.ok(systemService.updatePermission(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('system:permission:delete')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    systemService.deletePermission(id);
    return ApiResponse.ok();
  }
}
