package com.company.ops.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.ApprovalAssigneeConfigRepository;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemPermissionRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {
  @Mock private SystemUserRepository userRepository;
  @Mock private SystemRoleRepository roleRepository;
  @Mock private SystemPermissionRepository permissionRepository;
  @Mock private SystemOrganizationRepository organizationRepository;
  @Mock private ApprovalAssigneeConfigRepository approvalConfigRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @Test
  void existingAdminEnvironmentSeedsMissingPermissionsAndRefreshesAdminRole() {
    SystemRole admin = new SystemRole();
    admin.setCode("ADMIN");
    SystemPermission deletePermission = permission("maintenance:order:delete");
    when(permissionRepository.existsByCodeAndTenantId(any(), eq("default"))).thenReturn(true);
    when(permissionRepository.existsByCodeAndTenantId(eq("maintenance:order:delete"), eq("default"))).thenReturn(false);
    when(permissionRepository.findAll()).thenReturn(List.of(deletePermission));
    when(roleRepository.findByCodeAndTenantIdWithPermissions("ADMIN", "default")).thenReturn(Optional.of(admin));

    new DataInitializer(userRepository, roleRepository, permissionRepository, organizationRepository, approvalConfigRepository, passwordEncoder).run();

    ArgumentCaptor<SystemPermission> captor = ArgumentCaptor.forClass(SystemPermission.class);
    verify(permissionRepository, atLeastOnce()).save(captor.capture());
    assertThat(captor.getAllValues()).extracting(SystemPermission::getCode).contains("maintenance:order:delete");
    assertThat(admin.getPermissions()).extracting(SystemPermission::getCode)
        .contains("maintenance:order:delete");
    verify(roleRepository).save(admin);
  }

  @Test
  void createsBootstrapAdministratorWhenExplicitPasswordIsConfigured() {
    SystemRole admin = new SystemRole();
    admin.setCode("ADMIN");
    SystemOrganization root = new SystemOrganization();
    root.setCode("ROOT");
    when(permissionRepository.findAll()).thenReturn(List.of());
    when(permissionRepository.existsByCodeAndTenantId(any(), eq("default"))).thenReturn(true);
    when(roleRepository.findByCodeAndTenantIdWithPermissions("ADMIN", "default"))
        .thenReturn(Optional.of(admin));
    when(userRepository.existsByUsername("first.admin")).thenReturn(false);
    when(organizationRepository.findByCodeAndTenantId("ROOT", "default")).thenReturn(root);
    when(passwordEncoder.encode("StrongPassword#2026")).thenReturn("{bcrypt}encoded");

    new DataInitializer(
        userRepository,
        roleRepository,
        permissionRepository,
        organizationRepository,
        passwordEncoder,
        "first.admin",
        "StrongPassword#2026",
        "首位管理员"
    ).run();

    ArgumentCaptor<SystemUser> userCaptor = ArgumentCaptor.forClass(SystemUser.class);
    verify(userRepository).save(userCaptor.capture());
    SystemUser created = userCaptor.getValue();
    assertThat(created.getUsername()).isEqualTo("first.admin");
    assertThat(created.getDisplayName()).isEqualTo("首位管理员");
    assertThat(created.getPasswordHash()).isEqualTo("{bcrypt}encoded");
    assertThat(created.getOrganization()).isSameAs(root);
    assertThat(created.getRoles()).containsExactly(admin);
    assertThat(created.isEnabled()).isTrue();
  }

  @Test
  void rejectsWeakBootstrapAdministratorPassword() {
    when(permissionRepository.findAll()).thenReturn(List.of());
    when(permissionRepository.existsByCodeAndTenantId(any(), eq("default"))).thenReturn(true);

    DataInitializer initializer = new DataInitializer(
        userRepository,
        roleRepository,
        permissionRepository,
        organizationRepository,
        passwordEncoder,
        "admin",
        "too-short",
        "系统管理员"
    );

    assertThatThrownBy(initializer::run)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 12 characters");
  }

  private SystemPermission permission(String code) {
    SystemPermission permission = new SystemPermission();
    permission.setCode(code);
    permission.setName(code);
    permission.setModule("maintenance");
    permission.setTenantId("default");
    return permission;
  }
}
