package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.dto.CreatePermissionRequest;
import com.company.ops.api.modules.system.dto.UpdatePermissionRequest;
import com.company.ops.api.modules.system.dto.PermissionResponse;
import com.company.ops.api.modules.system.repository.SystemPermissionRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

  private final SystemPermissionRepository permissionRepository;
  private final SystemRoleRepository roleRepository;

  private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public PermissionService(
      SystemPermissionRepository permissionRepository,
      SystemRoleRepository roleRepository) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
  }

  @Transactional(readOnly = true)
  public List<PermissionResponse> listPermissions() {
    return permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "module", "code"))
        .stream().map(this::toPermissionResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<PermissionResponse> listAllPermissions() {
    return permissionRepository.findAll().stream().map(this::toPermissionResponse).toList();
  }

  @Transactional(readOnly = true)
  public PermissionResponse getPermission(UUID id) {
    SystemPermission permission = permissionRepository.findById(id).orElseThrow();
    return toPermissionResponse(permission);
  }

  @Transactional(readOnly = true)
  public java.util.Map<String, List<PermissionResponse>> getGroupedPermissions() {
    return permissionRepository.findAll().stream()
        .map(this::toPermissionResponse)
        .collect(Collectors.groupingBy(PermissionResponse::module));
  }

  @Transactional
  public PermissionResponse createPermission(CreatePermissionRequest request) {
    if (permissionRepository.existsByCodeAndTenantId(request.code(), TenantContext.currentTenant())) {
      throw new BusinessException("权限代码已存在");
    }
    SystemPermission permission = new SystemPermission();
    permission.setCode(request.code());
    permission.setName(request.name());
    permission.setModule(request.module());
    permission.setBuiltIn(false);
    SystemPermission saved = permissionRepository.save(permission);
    return toPermissionResponse(saved);
  }

  @Transactional
  public PermissionResponse updatePermission(UUID id, UpdatePermissionRequest request) {
    SystemPermission permission = permissionRepository.findById(id).orElseThrow();
    if (permission.isBuiltIn()) {
      throw new BusinessException("内置权限不能修改");
    }
    if (request.name() != null) permission.setName(request.name());
    if (request.module() != null) permission.setModule(request.module());
    SystemPermission saved = permissionRepository.save(permission);
    return toPermissionResponse(saved);
  }

  @Transactional
  public void deletePermission(UUID id) {
    SystemPermission permission = permissionRepository.findById(id).orElseThrow();
    if (permission.isBuiltIn()) {
      throw new BusinessException("内置权限不能删除");
    }
    long roleCount = roleRepository.countByPermissions_Id(id);
    if (roleCount > 0) {
      throw new BusinessException("该权限仍分配给 " + roleCount + " 个角色，不能删除");
    }
    permissionRepository.delete(permission);
  }

  private PermissionResponse toPermissionResponse(SystemPermission permission) {
    return new PermissionResponse(
        permission.getId(), permission.getCode(), permission.getName(), permission.getModule(),
        roleRepository.countByPermissions_Id(permission.getId()),
        permission.isBuiltIn(),
        permission.getCreatedAt() != null ? permission.getCreatedAt().format(DT_FORMATTER) : null
    );
  }
}
