package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.system.dto.CreateRoleRequest;
import com.company.ops.api.modules.system.dto.RoleResponse;
import com.company.ops.api.modules.system.dto.UpdateRoleRequest;
import com.company.ops.api.modules.system.service.SystemService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/roles")
public class RoleController {

  private final SystemService systemService;

  public RoleController(SystemService systemService) {
    this.systemService = systemService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('system:role:view')")
  public ApiResponse<Page<RoleResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(systemService.listRoles(pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('system:role:view')")
  public ApiResponse<RoleResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(systemService.getRole(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('system:role:create')")
  public ApiResponse<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
    return ApiResponse.ok(systemService.createRole(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('system:role:update')")
  public ApiResponse<RoleResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
    return ApiResponse.ok(systemService.updateRole(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('system:role:delete')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    systemService.deleteRole(id);
    return ApiResponse.ok();
  }
}
