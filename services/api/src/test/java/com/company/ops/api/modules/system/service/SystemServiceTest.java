package com.company.ops.api.modules.system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.dto.UpdateOrganizationRequest;
import com.company.ops.api.modules.system.dto.UpdateUserRequest;
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
  private QualificationEmployeeRepository employeeRepository;
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

  @Test
  void accountOrganizationChangeSynchronizesEmployeeDepartment() {
    UUID userId = UUID.randomUUID();
    SystemOrganization organization = organization("财务部", null);
    SystemUser user = new SystemUser();
    user.setId(userId);
    user.setUsername("employee");
    user.setDisplayName("测试员工");
    QualificationEmployee employee = new QualificationEmployee();
    employee.setId(UUID.randomUUID());
    employee.setName("测试员工");

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
    when(userRepository.save(user)).thenReturn(user);
    when(employeeRepository.findBySystemUser_Id(userId)).thenReturn(Optional.of(employee));

    systemService.updateUser(userId, new UpdateUserRequest(
        organization.getId(), null, null, null, null, null
    ));

    assertThat(employee.getOrganization()).isSameAs(organization);
    assertThat(employee.getDepartment()).isEqualTo("财务部");
  }

  @Test
  void organizationWithEmployeesCannotBeDeleted() {
    SystemOrganization organization = organization("技术服务部", null);
    when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
    when(organizationRepository.countByParent_Id(organization.getId())).thenReturn(0L);
    when(userRepository.countByOrganization_Id(organization.getId())).thenReturn(0L);
    when(employeeRepository.countByOrganization_Id(organization.getId())).thenReturn(2L);

    assertThatThrownBy(() -> systemService.deleteOrganization(organization.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("该组织仍有 2 名员工，请先在人事管理中调整部门");
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
