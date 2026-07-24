package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.dto.CreateRoleRequest;
import com.company.ops.api.modules.system.dto.UpdateRoleRequest;
import com.company.ops.api.modules.system.dto.RoleResponse;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemPermissionRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

  private final SystemRoleRepository roleRepository;
  private final SystemPermissionRepository permissionRepository;
  private final SystemOrganizationRepository organizationRepository;
  private final SystemUserRepository userRepository;

  private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public RoleService(
      SystemRoleRepository roleRepository,
      SystemPermissionRepository permissionRepository,
      SystemOrganizationRepository organizationRepository,
      SystemUserRepository userRepository) {
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.organizationRepository = organizationRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public Page<RoleResponse> listRoles(Pageable pageable) {
    return roleRepository.findAll(pageable).map(this::toRoleResponseWithDetails);
  }

  @Transactional(readOnly = true)
  public RoleResponse getRole(UUID id) {
    SystemRole role = roleRepository.findById(id).orElseThrow();
    return toRoleResponseWithDetails(role);
  }

  @Transactional
  public RoleResponse createRole(CreateRoleRequest request) {
    if (roleRepository.existsByCodeAndTenantId(request.code(), TenantContext.currentTenant())) {
      throw new BusinessException("角色代码已存在");
    }
    validateDataScope(request.dataScope(), request.dataOrganizationIds());
    SystemRole role = new SystemRole();
    role.setCode(request.code());
    role.setName(request.name());
    role.setDataScope(request.dataScope());
    role.setBuiltIn(false);

    if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
      role.getPermissions().addAll(resolvePermissions(request.permissionIds()));
    }
    if ("CUSTOM".equals(request.dataScope())) {
      role.getDataScopeOrganizations().addAll(resolveOrganizations(request.dataOrganizationIds()));
    }

    SystemRole saved = roleRepository.save(role);
    return toRoleResponseWithDetails(saved);
  }

  @Transactional
  public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
    SystemRole role = roleRepository.findById(id).orElseThrow();

    if (request.name() != null) role.setName(request.name());
    if (request.dataScope() != null) {
      validateDataScope(request.dataScope(), request.dataOrganizationIds());
      role.setDataScope(request.dataScope());
      role.getDataScopeOrganizations().clear();
      if ("CUSTOM".equals(request.dataScope())) {
        role.getDataScopeOrganizations().addAll(resolveOrganizations(request.dataOrganizationIds()));
      }
    }

    if (request.permissionIds() != null) {
      role.getPermissions().clear();
      if (!request.permissionIds().isEmpty()) {
        role.getPermissions().addAll(resolvePermissions(request.permissionIds()));
      }
    }

    SystemRole saved = roleRepository.save(role);
    return toRoleResponseWithDetails(saved);
  }

  @Transactional
  public void deleteRole(UUID id) {
    SystemRole role = roleRepository.findById(id)
        .orElseThrow(() -> new BusinessException("角色不存在"));
    if (role.isBuiltIn()) {
      throw new BusinessException("内置角色不能删除");
    }
    long userCount = userRepository.countByRoles_Id(id);
    if (userCount > 0) {
      throw new BusinessException("该角色仍分配给 " + userCount + " 个账号，不能删除");
    }
    roleRepository.delete(role);
  }

  private RoleResponse toRoleResponseWithDetails(SystemRole role) {
    List<RoleResponse.PermissionSummary> permissions = role.getPermissions().stream()
        .map(p -> new RoleResponse.PermissionSummary(p.getId(), p.getCode(), p.getName(), p.getModule()))
        .sorted((a, b) -> a.code().compareTo(b.code()))
        .toList();
    List<RoleResponse.OrganizationSummary> dataOrganizations = role.getDataScopeOrganizations().stream()
        .map(org -> new RoleResponse.OrganizationSummary(org.getId(), org.getCode(), org.getName()))
        .sorted((a, b) -> a.name().compareTo(b.name()))
        .toList();
    return new RoleResponse(
        role.getId(), role.getCode(), role.getName(), role.getDataScope(),
        permissions, dataOrganizations,
        userRepository.countByRoles_Id(role.getId()),
        role.isBuiltIn(),
        role.getCreatedAt() != null ? role.getCreatedAt().format(DT_FORMATTER) : null,
        role.getUpdatedAt() != null ? role.getUpdatedAt().format(DT_FORMATTER) : null
    );
  }

  private Set<SystemPermission> resolvePermissions(List<UUID> ids) {
    Set<UUID> requested = new HashSet<>(ids == null ? List.of() : ids);
    Set<SystemPermission> permissions = new HashSet<>(permissionRepository.findAllById(requested));
    if (permissions.size() != requested.size()) {
      throw new BusinessException("部分权限不存在，请刷新后重试");
    }
    return permissions;
  }

  private Set<SystemOrganization> resolveOrganizations(List<UUID> ids) {
    Set<UUID> requested = new HashSet<>(ids == null ? List.of() : ids);
    Set<SystemOrganization> organizations = new HashSet<>(organizationRepository.findAllById(requested));
    if (organizations.size() != requested.size()) {
      throw new BusinessException("部分组织不存在，请刷新后重试");
    }
    return organizations;
  }

  protected void validateDataScope(String dataScope, List<UUID> organizationIds) {
    if (!Set.of("SELF", "DEPT", "DEPARTMENT", "DEPT_AND_SUB", "CUSTOM", "ALL").contains(dataScope)) {
      throw new BusinessException("数据范围不正确");
    }
    if ("CUSTOM".equals(dataScope) && (organizationIds == null || organizationIds.isEmpty())) {
      throw new BusinessException("自定义数据范围至少选择一个组织");
    }
  }
}
