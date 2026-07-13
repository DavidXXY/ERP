package com.company.ops.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.domain.SystemRole;
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
  @Mock private PasswordEncoder passwordEncoder;

  @Test
  void existingAdminEnvironmentSeedsMissingPermissionsAndRefreshesAdminRole() {
    SystemRole admin = new SystemRole();
    admin.setCode("ADMIN");
    SystemPermission deletePermission = permission("maintenance:order:delete");
    when(userRepository.existsByUsername("admin")).thenReturn(true);
    when(permissionRepository.existsByCodeAndTenantId(any(), eq("default"))).thenReturn(true);
    when(permissionRepository.existsByCodeAndTenantId(eq("maintenance:order:delete"), eq("default"))).thenReturn(false);
    when(permissionRepository.findAll()).thenReturn(List.of(deletePermission));
    when(roleRepository.findByCodeAndTenantIdWithPermissions("ADMIN", "default")).thenReturn(Optional.of(admin));

    new DataInitializer(userRepository, roleRepository, permissionRepository, organizationRepository, passwordEncoder).run();

    ArgumentCaptor<SystemPermission> captor = ArgumentCaptor.forClass(SystemPermission.class);
    verify(permissionRepository, atLeastOnce()).save(captor.capture());
    assertThat(captor.getAllValues()).extracting(SystemPermission::getCode).contains("maintenance:order:delete");
    assertThat(admin.getPermissions()).contains(deletePermission);
    verify(roleRepository).save(admin);
  }

  private SystemPermission permission(String code) {
    SystemPermission permission = new SystemPermission();
    permission.setCode(code);
    permission.setName(code);
    permission.setModule("maintenance");
    return permission;
  }
}
