package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemService {

  private CodeGenerator codeGenerator;
  private final SystemUserRepository userRepository;
  private final SystemRoleRepository roleRepository;
  private final SystemPermissionRepository permissionRepository;
  private final SystemOrganizationRepository organizationRepository;
  private final QualificationEmployeeRepository employeeRepository;
  private final PasswordEncoder passwordEncoder;

  private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public SystemService(
      SystemUserRepository userRepository,
      SystemRoleRepository roleRepository,
      SystemPermissionRepository permissionRepository,
      SystemOrganizationRepository organizationRepository,
      QualificationEmployeeRepository employeeRepository,
      PasswordEncoder passwordEncoder) {
    this.codeGenerator = codeGenerator;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.organizationRepository = organizationRepository;
    this.employeeRepository = employeeRepository;
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
    if (userRepository.existsByUsername(request.username())) {
      throw new BusinessException("用户名已存在");
    }
    SystemUser user = new SystemUser();
    if (request.orgId() != null) {
      user.setOrganization(organizationRepository.findById(request.orgId())
          .orElseThrow(() -> new BusinessException("所属组织不存在")));
    }
    user.setUsername(request.username());
    user.setDisplayName(request.displayName());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setPhone(request.phone());
    user.setEmail(request.email());
    user.setEnabled(true);

    if (request.roleIds() != null && !request.roleIds().isEmpty()) {
      Set<SystemRole> roles = resolveRoles(request.roleIds());
      user.getRoles().addAll(roles);
    }

    SystemUser saved = userRepository.save(user);
    return toUserResponseWithDetails(saved);
  }

  @Transactional
  public UserResponse updateUser(UUID id, UpdateUserRequest request) {
    SystemUser user = userRepository.findById(id).orElseThrow();

    SystemOrganization selectedOrganization = null;
    if (request.orgId() != null) {
      selectedOrganization = organizationRepository.findById(request.orgId())
          .orElseThrow(() -> new BusinessException("所属组织不存在"));
      user.setOrganization(selectedOrganization);
    }
    if (request.displayName() != null) user.setDisplayName(request.displayName());
    if (request.phone() != null) user.setPhone(request.phone());
    if (request.email() != null) user.setEmail(request.email());
    if (request.enabled() != null) user.setEnabled(request.enabled());

    if (request.roleIds() != null) {
      user.getRoles().clear();
      if (!request.roleIds().isEmpty()) {
        Set<SystemRole> roles = resolveRoles(request.roleIds());
        user.getRoles().addAll(roles);
      }
    }

    SystemUser saved = userRepository.save(user);
    if (selectedOrganization != null) {
      SystemOrganization organization = selectedOrganization;
      employeeRepository.findBySystemUser_Id(id).ifPresent(employee -> {
        employee.setOrganization(organization);
        employee.setDepartment(organization.getName());
        employeeRepository.save(employee);
      });
    }
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
    if (roleRepository.existsByCodeAndTenantId(request.code(), "default")) {
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

  private RoleResponse toRoleResponse(SystemRole role) {
    return new RoleResponse(
        role.getId(),
        role.getCode(),
        role.getName(),
        role.getDataScope(),
        List.of(),
        List.of(),
        userRepository.countByRoles_Id(role.getId()),
        role.isBuiltIn(),
        role.getCreatedAt() != null ? role.getCreatedAt().format(DT_FORMATTER) : null,
        role.getUpdatedAt() != null ? role.getUpdatedAt().format(DT_FORMATTER) : null
    );
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
        role.getId(),
        role.getCode(),
        role.getName(),
        role.getDataScope(),
        permissions,
        dataOrganizations,
        userRepository.countByRoles_Id(role.getId()),
        role.isBuiltIn(),
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
    if (permissionRepository.existsByCodeAndTenantId(request.code(), "default")) {
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
    if (request.name() != null) permission.setName(request.name());
    if (request.module() != null) permission.setModule(request.module());
    SystemPermission saved = permissionRepository.save(permission);
    return toPermissionResponse(saved);
  }

  @Transactional
  public void deletePermission(UUID id) {
    SystemPermission permission = permissionRepository.findById(id)
        .orElseThrow(() -> new BusinessException("权限不存在"));
    if (permission.isBuiltIn()) {
      throw new BusinessException("系统内置权限不能删除");
    }
    long roleCount = roleRepository.countByPermissions_Id(id);
    if (roleCount > 0) {
      throw new BusinessException("该权限仍被 " + roleCount + " 个角色使用，不能删除");
    }
    permissionRepository.delete(permission);
  }

  private PermissionResponse toPermissionResponse(SystemPermission permission) {
    return new PermissionResponse(
        permission.getId(),
        permission.getCode(),
        permission.getName(),
        permission.getModule(),
        roleRepository.countByPermissions_Id(permission.getId()),
        permission.isBuiltIn(),
        permission.getCreatedAt() != null ? permission.getCreatedAt().format(DT_FORMATTER) : null
    );
  }

  // Organization management
  @Transactional(readOnly = true)
  public List<OrganizationResponse> listOrganizations() {
    List<SystemOrganization> all = organizationRepository.findByTenantIdOrderBySortOrderAsc("default");
    Map<UUID, List<SystemOrganization>> children = childOrganizations(all);
    Map<UUID, Long> directUsers = directUserCounts(all);
    Map<UUID, Long> directEmployees = directEmployeeCounts(all);
    return all.stream()
        .filter(item -> item.getParent() == null)
        .map(item -> toOrganizationResponse(item, children, directUsers, directEmployees, true))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<OrganizationResponse> listOrganizationsFlat() {
    List<SystemOrganization> all = organizationRepository.findByTenantIdOrderBySortOrderAsc("default");
    Map<UUID, List<SystemOrganization>> children = childOrganizations(all);
    Map<UUID, Long> directUsers = directUserCounts(all);
    Map<UUID, Long> directEmployees = directEmployeeCounts(all);
    return all.stream()
        .map(item -> toOrganizationResponse(item, children, directUsers, directEmployees, false))
        .toList();
  }

  @Transactional(readOnly = true)
  public OrganizationResponse getOrganization(UUID id) {
    List<SystemOrganization> all = organizationRepository.findByTenantIdOrderBySortOrderAsc("default");
    SystemOrganization org = all.stream().filter(item -> item.getId().equals(id)).findFirst()
        .orElseThrow(() -> new BusinessException("组织不存在"));
    return toOrganizationResponse(org, childOrganizations(all), directUserCounts(all), directEmployeeCounts(all), true);
  }

  @Transactional
  public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
    String orgCode = request.code() != null ? request.code() : codeGenerator.generate("ORGANIZATION");
    if (organizationRepository.existsByCodeAndTenantId(orgCode, "default")) {
      throw new BusinessException("组织代码已存在");
    }
    validateOrganizationType(request.type());
    SystemOrganization org = new SystemOrganization();
    org.setCode(orgCode);
    org.setName(request.name());
    org.setType(request.type() != null ? request.type() : "DEPARTMENT");
    org.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
    org.setLeaderName(request.leaderName());
    org.setPhone(request.phone());
    org.setEnabled(request.enabled() == null || request.enabled());
    org.setDescription(request.description());

    if (request.parentId() != null) {
      SystemOrganization parent = organizationRepository.findById(request.parentId())
          .orElseThrow(() -> new BusinessException("上级组织不存在"));
      org.setParent(parent);
    }

    SystemOrganization saved = organizationRepository.save(org);
    employeeRepository.findByOrganization_IdOrderByNameAsc(saved.getId()).forEach(employee -> employee.setDepartment(saved.getName()));
    return getOrganization(saved.getId());
  }

  @Transactional
  public OrganizationResponse updateOrganization(UUID id, UpdateOrganizationRequest request) {
    SystemOrganization org = organizationRepository.findById(id)
        .orElseThrow(() -> new BusinessException("组织不存在"));

    if (request.name() != null) org.setName(request.name());
    if (request.type() != null) {
      validateOrganizationType(request.type());
      org.setType(request.type());
    }
    if (request.sortOrder() != null) org.setSortOrder(request.sortOrder());
    if (request.leaderName() != null) org.setLeaderName(request.leaderName());
    if (request.phone() != null) org.setPhone(request.phone());
    if (request.enabled() != null) org.setEnabled(request.enabled());
    if (request.description() != null) org.setDescription(request.description());

    if (request.parentId() != null) {
      SystemOrganization parent = organizationRepository.findById(request.parentId())
          .orElseThrow(() -> new BusinessException("上级组织不存在"));
      validateOrganizationParent(org, parent);
      org.setParent(parent);
    } else {
      org.setParent(null);
    }

    SystemOrganization saved = organizationRepository.save(org);
    return getOrganization(saved.getId());
  }

  @Transactional
  public void deleteOrganization(UUID id) {
    SystemOrganization org = organizationRepository.findById(id)
        .orElseThrow(() -> new BusinessException("组织不存在"));
    if ("ROOT".equals(org.getCode())) {
      throw new BusinessException("根组织不能删除");
    }
    if (organizationRepository.countByParent_Id(id) > 0) {
      throw new BusinessException("该组织存在下级组织，请先调整下级组织");
    }
    long userCount = userRepository.countByOrganization_Id(id);
    if (userCount > 0) {
      throw new BusinessException("该组织仍有 " + userCount + " 个账号，请先转移成员");
    }
    long employeeCount = employeeRepository.countByOrganization_Id(id);
    if (employeeCount > 0) {
      throw new BusinessException("该组织仍有 " + employeeCount + " 名员工，请先在人事管理中调整部门");
    }
    if (roleRepository.countByDataScopeOrganizations_Id(id) > 0) {
      throw new BusinessException("该组织仍被角色数据范围引用，不能删除");
    }
    organizationRepository.delete(org);
  }

  private OrganizationResponse toOrganizationResponse(
      SystemOrganization org,
      Map<UUID, List<SystemOrganization>> children,
      Map<UUID, Long> directUsers,
      Map<UUID, Long> directEmployees,
      boolean includeChildren
  ) {
    String parentName = org.getParent() != null ? org.getParent().getName() : null;
    List<SystemOrganization> childEntities = children.getOrDefault(org.getId(), List.of());
    List<OrganizationResponse> childResponses = includeChildren
        ? childEntities.stream()
            .map(child -> toOrganizationResponse(child, children, directUsers, directEmployees, true))
            .toList()
        : List.of();
    return new OrganizationResponse(
        org.getId(),
        org.getCode(),
        org.getName(),
        org.getType(),
        org.getSortOrder(),
        org.getParent() != null ? org.getParent().getId() : null,
        parentName,
        organizationPath(org),
        org.getLeaderName(),
        org.getPhone(),
        org.isEnabled(),
        org.getDescription(),
        directUsers.getOrDefault(org.getId(), 0L),
        totalUserCount(org.getId(), children, directUsers),
        directEmployees.getOrDefault(org.getId(), 0L),
        totalEmployeeCount(org.getId(), children, directEmployees),
        childEntities.size(),
        childResponses,
        org.getCreatedAt() != null ? org.getCreatedAt().format(DT_FORMATTER) : null,
        org.getUpdatedAt() != null ? org.getUpdatedAt().format(DT_FORMATTER) : null
    );
  }

  private Map<UUID, List<SystemOrganization>> childOrganizations(List<SystemOrganization> organizations) {
    Map<UUID, List<SystemOrganization>> result = new HashMap<>();
    for (SystemOrganization organization : organizations) {
      if (organization.getParent() != null) {
        result.computeIfAbsent(organization.getParent().getId(), ignored -> new ArrayList<>())
            .add(organization);
      }
    }
    result.values().forEach(items -> items.sort((a, b) -> {
      int order = Integer.compare(a.getSortOrder() == null ? 0 : a.getSortOrder(), b.getSortOrder() == null ? 0 : b.getSortOrder());
      return order != 0 ? order : a.getName().compareTo(b.getName());
    }));
    return result;
  }

  private Map<UUID, Long> directUserCounts(List<SystemOrganization> organizations) {
    Map<UUID, Long> counts = new HashMap<>();
    organizations.forEach(org -> counts.put(org.getId(), userRepository.countByOrganization_Id(org.getId())));
    return counts;
  }

  private Map<UUID, Long> directEmployeeCounts(List<SystemOrganization> organizations) {
    Map<UUID, Long> counts = new HashMap<>();
    organizations.forEach(org -> counts.put(org.getId(), employeeRepository.countByOrganization_Id(org.getId())));
    return counts;
  }

  private long totalUserCount(
      UUID organizationId,
      Map<UUID, List<SystemOrganization>> children,
      Map<UUID, Long> directUsers
  ) {
    long total = directUsers.getOrDefault(organizationId, 0L);
    for (SystemOrganization child : children.getOrDefault(organizationId, List.of())) {
      total += totalUserCount(child.getId(), children, directUsers);
    }
    return total;
  }

  private long totalEmployeeCount(
      UUID organizationId,
      Map<UUID, List<SystemOrganization>> children,
      Map<UUID, Long> directEmployees
  ) {
    long total = directEmployees.getOrDefault(organizationId, 0L);
    for (SystemOrganization child : children.getOrDefault(organizationId, List.of())) {
      total += totalEmployeeCount(child.getId(), children, directEmployees);
    }
    return total;
  }

  private String organizationPath(SystemOrganization organization) {
    List<String> names = new ArrayList<>();
    Set<UUID> visited = new HashSet<>();
    SystemOrganization current = organization;
    while (current != null && visited.add(current.getId())) {
      names.add(current.getName());
      current = current.getParent();
    }
    Collections.reverse(names);
    return String.join(" / ", names);
  }

  private void validateOrganizationType(String type) {
    if (type != null && !Set.of("COMPANY", "DEPARTMENT", "TEAM").contains(type)) {
      throw new BusinessException("组织类型不正确");
    }
  }

  private void validateOrganizationParent(SystemOrganization organization, SystemOrganization parent) {
    if (organization.getId().equals(parent.getId())) {
      throw new BusinessException("上级组织不能选择自身");
    }
    Set<UUID> visited = new HashSet<>();
    SystemOrganization current = parent;
    while (current != null && visited.add(current.getId())) {
      if (organization.getId().equals(current.getId())) {
        throw new BusinessException("不能将组织移动到自己的下级组织");
      }
      current = current.getParent();
    }
  }

  private void validateDataScope(String dataScope, List<UUID> organizationIds) {
    if (!Set.of("SELF", "DEPT", "DEPARTMENT", "DEPT_AND_SUB", "CUSTOM", "ALL").contains(dataScope)) {
      throw new BusinessException("数据范围不正确");
    }
    if ("CUSTOM".equals(dataScope) && (organizationIds == null || organizationIds.isEmpty())) {
      throw new BusinessException("自定义数据范围至少选择一个组织");
    }
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

  private Set<SystemRole> resolveRoles(List<UUID> ids) {
    Set<UUID> requested = new HashSet<>(ids == null ? List.of() : ids);
    Set<SystemRole> roles = new HashSet<>(roleRepository.findAllById(requested));
    if (roles.size() != requested.size()) {
      throw new BusinessException("部分角色不存在，请刷新后重试");
    }
    return roles;
  }
}
