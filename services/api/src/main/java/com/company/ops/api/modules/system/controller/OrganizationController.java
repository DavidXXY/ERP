package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.system.dto.CreateOrganizationRequest;
import com.company.ops.api.modules.system.dto.OrganizationResponse;
import com.company.ops.api.modules.system.dto.UpdateOrganizationRequest;
import com.company.ops.api.modules.system.service.OrganizationService;
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

  private final OrganizationService organizationService;

  public OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('system:organization:view')")
  public ApiResponse<List<OrganizationResponse>> list() {
    return ApiResponse.ok(organizationService.listOrganizations());
  }

  @GetMapping("/flat")
  @PreAuthorize("hasAuthority('system:organization:view')")
  public ApiResponse<List<OrganizationResponse>> listFlat() {
    return ApiResponse.ok(organizationService.listOrganizationsFlat());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('system:organization:view')")
  public ApiResponse<OrganizationResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(organizationService.getOrganization(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('system:organization:create')")
  public ApiResponse<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
    return ApiResponse.ok(organizationService.createOrganization(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('system:organization:update')")
  public ApiResponse<OrganizationResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateOrganizationRequest request) {
    return ApiResponse.ok(organizationService.updateOrganization(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('system:organization:delete')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    organizationService.deleteOrganization(id);
    return ApiResponse.ok();
  }
}
