package com.company.ops.api.modules.system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.dto.UpdateOrganizationRequest;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemPermissionRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

  @Mock
  private SystemUserRepository userRepository;
  @Mock
  private SystemRoleRepository roleRepository;
  @Mock
  private SystemPermissionRepository permissionRepository;
  @Mock
  private SystemOrganizationRepository organizationRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @InjectMocks
  private SystemService systemService;

  @Test
  void organizationCannotMoveBelowItsOwnDescendant() {
    SystemOrganization parent = organization("项目中心", null);
    SystemOrganization child = organization("项目一部", parent);
    when(organizationRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
    when(organizationRepository.findById(child.getId())).thenReturn(Optional.of(child));

    var request = new UpdateOrganizationRequest(
        "项目中心",
        "DEPARTMENT",
        0,
        child.getId(),
        "负责人",
        "021-10000000",
        true,
        "项目管理"
    );

    assertThatThrownBy(() -> systemService.updateOrganization(parent.getId(), request))
        .isInstanceOf(BusinessException.class)
        .hasMessage("不能将组织移动到自己的下级组织");
  }

  @Test
  void builtInRoleCannotBeDeleted() {
    UUID roleId = UUID.randomUUID();
    SystemRole role = new SystemRole();
    role.setId(roleId);
    role.setCode("ADMIN");
    role.setBuiltIn(true);
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

    assertThatThrownBy(() -> systemService.deleteRole(roleId))
        .isInstanceOf(BusinessException.class)
        .hasMessage("内置角色不能删除");
  }

  private SystemOrganization organization(String name, SystemOrganization parent) {
    SystemOrganization organization = new SystemOrganization();
    organization.setId(UUID.randomUUID());
    organization.setCode(name);
    organization.setName(name);
    organization.setParent(parent);
    return organization;
  }
}
