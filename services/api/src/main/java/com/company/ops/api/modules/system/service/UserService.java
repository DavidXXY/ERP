package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.dto.CreateUserRequest;
import com.company.ops.api.modules.system.dto.UpdateUserRequest;
import com.company.ops.api.modules.system.dto.UserResponse;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
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
public class UserService {

  private final SystemUserRepository userRepository;
  private final SystemRoleRepository roleRepository;
  private final SystemOrganizationRepository organizationRepository;
  private final QualificationEmployeeRepository employeeRepository;
  private final PasswordEncoder passwordEncoder;

  private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public UserService(
      SystemUserRepository userRepository,
      SystemRoleRepository roleRepository,
      SystemOrganizationRepository organizationRepository,
      QualificationEmployeeRepository employeeRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.organizationRepository = organizationRepository;
    this.employeeRepository = employeeRepository;
    this.passwordEncoder = passwordEncoder;
  }

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

  private Set<SystemRole> resolveRoles(List<UUID> ids) {
    Set<UUID> requested = new HashSet<>(ids == null ? List.of() : ids);
    Set<SystemRole> roles = new HashSet<>(roleRepository.findAllById(requested));
    if (roles.size() != requested.size()) {
      throw new BusinessException("部分角色不存在，请刷新后重试");
    }
    return roles;
  }
}
