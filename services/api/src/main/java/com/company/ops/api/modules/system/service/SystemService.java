package com.company.ops.api.modules.system.service;

import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.dto.CreateOrganizationRequest;
import com.company.ops.api.modules.system.dto.CreatePermissionRequest;
import com.company.ops.api.modules.system.dto.CreateRoleRequest;
import com.company.ops.api.modules.system.dto.CreateUserRequest;
import com.company.ops.api.modules.system.dto.OrganizationResponse;
import com.company.ops.api.modules.system.dto.PermissionResponse;
import com.company.ops.api.modules.system.dto.RoleResponse;
import com.company.ops.api.modules.system.dto.UpdateOrganizationRequest;
import com.company.ops.api.modules.system.dto.UpdatePermissionRequest;
import com.company.ops.api.modules.system.dto.UpdateRoleRequest;
import com.company.ops.api.modules.system.dto.UpdateUserRequest;
import com.company.ops.api.modules.system.dto.UserResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemService {

  private final SystemUserRepository userRepository;
  private final SystemRoleRepository roleRepository;
  private final SystemPermissionRepository permissionRepository;
  private final SystemOrganizationRepository organizationRepository;
  private final PasswordEncoder passwordEncoder;

  private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public SystemService(
      SystemUserRepository userRepository,
      SystemRoleRepository roleRepository,
      SystemPermissionRepository permissionRepository,
      SystemOrganizationRepository organizationRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.organizationRepository = organizationRepository;
    this.passwordEncoder = passwordEncoder;
  }

  // User management
  @Transactional(readOnly = true)
  public Page<UserResponse> listUsers(Pageable pageable) {
    return userRepository.findAll(pageable).map(this::toUserResponseWithDetails);
  }

  @Transactional(readOnly = true)
  public UserResponse getUser(UUID id) {
    SystemUser user = userRepository.findDetailById(id).orElseThrow();
    return toUserResponseWithDetails(user);
  }

  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    SystemUser user = new SystemUser();
    if (request.orgId() != null) {
      user.setOrganization(organizationRepository.findById(request.orgId()).orElse(null));
    }
    user.setUsername(request.username());
    user.setDisplayName(request.displayName());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setPhone(request.phone());
    user.setEmail(request.email());
    user.setEnabled(true);

    if (request.roleIds() != null && !request.roleIds().isEmpty()) {
      Set<SystemRole> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
      user.getRoles().addAll(roles);
    }

    SystemUser saved = userRepository.save(user);
    return toUserResponseWithDetails(saved);
  }

  @Transactional
  public UserResponse updateUser(UUID id, UpdateUserRequest request) {
    SystemUser user = userRepository.findById(id).orElseThrow();

    if (request.orgId() != null) {
      user.setOrganization(organizationRepository.findById(request.orgId()).orElse(null));
    }
    if (request.displayName() != null) user.setDisplayName(request.displayName());
    if (request.phone() != null) user.setPhone(request.phone());
    if (request.email() != null) user.setEmail(request.email());
    if (request.enabled() != null) user.setEnabled(request.enabled());

    if (request.roleIds() != null) {
      user.getRoles().clear();
      if (!request.roleIds().isEmpty()) {
        Set<SystemRole> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
        user.getRoles().addAll(roles);
      }
    }

    SystemUser saved = userRepository.save(user);
    return toUserResponseWithDetails(saved);
  }

  @Transactional
  public void deleteUser(UUID id) {
    userRepository.deleteById(id);
  }

  @Transactional
  public void resetPassword(UUID id, String newPassword) {
    SystemUser user = userRepository.findById(id).orElseThrow();
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  private UserResponse toUserResponse(SystemUser user) {
    return new UserResponse(
        user.getId(),
        user.getOrganization() != null ? user.getOrganization().getId() : null,
        user.getUsername(),
        user.getDisplayName(),
        user.getPhone(),
        user.getEmail(),
        user.isEnabled(),
        List.of(),
        user.getCreatedAt() != null ? user.getCreatedAt().format(DT_FORMATTER) : null,
        user.getUpdatedAt() != null ? user.getUpdatedAt().format(DT_FORMATTER) : null
    );
  }

  private UserResponse toUserResponseWithDetails(SystemUser user) {
    List<UserResponse.RoleSummary> roles = user.getRoles().stream()
        .map(r -> new UserResponse.RoleSummary(r.getId(), r.getCode(), r.getName()))
        .toList();
    return new UserResponse(
        user.getId(),
        user.getOrganization() != null ? user.getOrganization().getId() : null,
        user.getUsername(),
        user.getDisplayName(),
        user.getPhone(),
        user.getEmail(),
        user.isEnabled(),
        roles,
        user.getCreatedAt() != null ? user.getCreatedAt().format(DT_FORMATTER) : null,
        user.getUpdatedAt() != null ? user.getUpdatedAt().format(DT_FORMATTER) : null
    );
  }

  // Role management
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
    SystemRole role = new SystemRole();
    role.setCode(request.code());
    role.setName(request.name());
    role.setDataScope(request.dataScope());

    if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
      Set<SystemPermission> permissions = new HashSet<>(permissionRepository.findAllById(request.permissionIds()));
      role.getPermissions().addAll(permissions);
    }

    SystemRole saved = roleRepository.save(role);
    return toRoleResponseWithDetails(saved);
  }

  @Transactional
  public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
    SystemRole role = roleRepository.findById(id).orElseThrow();

    if (request.name() != null) role.setName(request.name());
    if (request.dataScope() != null) role.setDataScope(request.dataScope());

    if (request.permissionIds() != null) {
      role.getPermissions().clear();
      if (!request.permissionIds().isEmpty()) {
        Set<SystemPermission> permissions = new HashSet<>(permissionRepository.findAllById(request.permissionIds()));
        role.getPermissions().addAll(permissions);
      }
    }

    SystemRole saved = roleRepository.save(role);
    return toRoleResponseWithDetails(saved);
  }

  @Transactional
  public void deleteRole(UUID id) {
    roleRepository.deleteById(id);
  }

  private RoleResponse toRoleResponse(SystemRole role) {
    return new RoleResponse(
        role.getId(),
        role.getCode(),
        role.getName(),
        role.getDataScope(),
        List.of(),
        role.getCreatedAt() != null ? role.getCreatedAt().format(DT_FORMATTER) : null,
        role.getUpdatedAt() != null ? role.getUpdatedAt().format(DT_FORMATTER) : null
    );
  }

  private RoleResponse toRoleResponseWithDetails(SystemRole role) {
    List<RoleResponse.PermissionSummary> permissions = role.getPermissions().stream()
        .map(p -> new RoleResponse.PermissionSummary(p.getId(), p.getCode(), p.getName(), p.getModule()))
        .toList();
    return new RoleResponse(
        role.getId(),
        role.getCode(),
        role.getName(),
        role.getDataScope(),
        permissions,
        role.getCreatedAt() != null ? role.getCreatedAt().format(DT_FORMATTER) : null,
        role.getUpdatedAt() != null ? role.getUpdatedAt().format(DT_FORMATTER) : null
    );
  }

  // Permission management
  @Transactional(readOnly = true)
  public List<PermissionResponse> listPermissions() {
    return permissionRepository.findAll().stream()
        .map(this::toPermissionResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public PermissionResponse getPermission(UUID id) {
    SystemPermission permission = permissionRepository.findById(id).orElseThrow();
    return toPermissionResponse(permission);
  }

  @Transactional
  public PermissionResponse createPermission(CreatePermissionRequest request) {
    SystemPermission permission = new SystemPermission();
    permission.setCode(request.code());
    permission.setName(request.name());
    permission.setModule(request.module());
    SystemPermission saved = permissionRepository.save(permission);
    return toPermissionResponse(saved);
  }

  @Transactional
  public PermissionResponse updatePermission(UUID id, UpdatePermissionRequest request) {
    SystemPermission permission = permissionRepository.findById(id).orElseThrow();
    if (request.name() != null) permission.setName(request.name());
    if (request.module() != null) permission.setModule(request.module());
    SystemPermission saved = permissionRepository.save(permission);
    return toPermissionResponse(saved);
  }

  @Transactional
  public void deletePermission(UUID id) {
    permissionRepository.deleteById(id);
  }

  private PermissionResponse toPermissionResponse(SystemPermission permission) {
    return new PermissionResponse(
        permission.getId(),
        permission.getCode(),
        permission.getName(),
        permission.getModule(),
        permission.getCreatedAt() != null ? permission.getCreatedAt().format(DT_FORMATTER) : null
    );
  }

  // Organization management
  @Transactional(readOnly = true)
  public List<OrganizationResponse> listOrganizations() {
    List<SystemOrganization> roots = organizationRepository.findTreeByTenantId("default");
    return roots.stream().map(this::toOrganizationResponseWithChildren).toList();
  }

  @Transactional(readOnly = true)
  public List<OrganizationResponse> listOrganizationsFlat() {
    return organizationRepository.findByTenantIdOrderBySortOrderAsc("default").stream()
        .map(this::toOrganizationResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public OrganizationResponse getOrganization(UUID id) {
    SystemOrganization org = organizationRepository.findById(id).orElseThrow();
    return toOrganizationResponseWithChildren(org);
  }

  @Transactional
  public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
    SystemOrganization org = new SystemOrganization();
    org.setCode(request.code());
    org.setName(request.name());
    org.setType(request.type() != null ? request.type() : "DEPARTMENT");
    org.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);

    if (request.parentId() != null) {
      SystemOrganization parent = organizationRepository.findById(request.parentId()).orElseThrow();
      org.setParent(parent);
    }

    SystemOrganization saved = organizationRepository.save(org);
    return toOrganizationResponse(saved);
  }

  @Transactional
  public OrganizationResponse updateOrganization(UUID id, UpdateOrganizationRequest request) {
    SystemOrganization org = organizationRepository.findById(id).orElseThrow();

    if (request.name() != null) org.setName(request.name());
    if (request.type() != null) org.setType(request.type());
    if (request.sortOrder() != null) org.setSortOrder(request.sortOrder());

    if (request.parentId() != null) {
      SystemOrganization parent = organizationRepository.findById(request.parentId()).orElseThrow();
      org.setParent(parent);
    } else if (request.parentId() == null) {
      org.setParent(null);
    }

    SystemOrganization saved = organizationRepository.save(org);
    return toOrganizationResponse(saved);
  }

  @Transactional
  public void deleteOrganization(UUID id) {
    organizationRepository.deleteById(id);
  }

  private OrganizationResponse toOrganizationResponse(SystemOrganization org) {
    String parentName = org.getParent() != null ? org.getParent().getName() : null;
    return new OrganizationResponse(
        org.getId(),
        org.getCode(),
        org.getName(),
        org.getType(),
        org.getSortOrder(),
        org.getParent() != null ? org.getParent().getId() : null,
        parentName,
        List.of(),
        org.getCreatedAt() != null ? org.getCreatedAt().format(DT_FORMATTER) : null,
        org.getUpdatedAt() != null ? org.getUpdatedAt().format(DT_FORMATTER) : null
    );
  }

  private OrganizationResponse toOrganizationResponseWithChildren(SystemOrganization org) {
    String parentName = org.getParent() != null ? org.getParent().getName() : null;
    List<OrganizationResponse> children = org.getChildren().stream()
        .sorted((a, b) -> Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0, b.getSortOrder() != null ? b.getSortOrder() : 0))
        .map(this::toOrganizationResponseWithChildren)
        .toList();
    return new OrganizationResponse(
        org.getId(),
        org.getCode(),
        org.getName(),
        org.getType(),
        org.getSortOrder(),
        org.getParent() != null ? org.getParent().getId() : null,
        parentName,
        children,
        org.getCreatedAt() != null ? org.getCreatedAt().format(DT_FORMATTER) : null,
        org.getUpdatedAt() != null ? org.getUpdatedAt().format(DT_FORMATTER) : null
    );
  }
}
