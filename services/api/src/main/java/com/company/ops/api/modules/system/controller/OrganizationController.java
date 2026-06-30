package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.system.dto.CreateOrganizationRequest;
import com.company.ops.api.modules.system.dto.OrganizationResponse;
import com.company.ops.api.modules.system.dto.UpdateOrganizationRequest;
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
@RequestMapping("/api/organizations")
public class OrganizationController {

  private final SystemService systemService;

  public OrganizationController(SystemService systemService) {
    this.systemService = systemService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('system:organization:view')")
  public ApiResponse<List<OrganizationResponse>> list() {
    return ApiResponse.ok(systemService.listOrganizations());
  }

  @GetMapping("/flat")
  @PreAuthorize("hasAuthority('system:organization:view')")
  public ApiResponse<List<OrganizationResponse>> listFlat() {
    return ApiResponse.ok(systemService.listOrganizationsFlat());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('system:organization:view')")
  public ApiResponse<OrganizationResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(systemService.getOrganization(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('system:organization:create')")
  public ApiResponse<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
    return ApiResponse.ok(systemService.createOrganization(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('system:organization:update')")
  public ApiResponse<OrganizationResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateOrganizationRequest request) {
    return ApiResponse.ok(systemService.updateOrganization(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('system:organization:delete')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    systemService.deleteOrganization(id);
    return ApiResponse.ok();
  }
}
