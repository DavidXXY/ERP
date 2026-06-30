package com.company.ops.api.modules.system.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataScopeServiceTest {

  @Mock
  private SystemUserRepository userRepository;
  @Mock
  private SystemOrganizationRepository organizationRepository;
  @InjectMocks
  private DataScopeService dataScopeService;

  @Test
  void departmentAndSubScopeIncludesDescendantOrganizationUsers() {
    SystemOrganization department = organization("销售部", null);
    SystemOrganization team = organization("华东团队", department);
    SystemOrganization unrelated = organization("财务部", null);
    SystemUser current = user("销售经理", department, "DEPT_AND_SUB");
    SystemUser departmentUser = user("销售专员", department, "SELF");
    SystemUser teamUser = user("华东客户经理", team, "SELF");

    when(userRepository.findById(current.getId())).thenReturn(Optional.of(current));
    when(organizationRepository.findByTenantIdOrderBySortOrderAsc("default"))
        .thenReturn(List.of(department, team, unrelated));
    when(userRepository.findByOrganization_IdIn(anySet()))
        .thenReturn(List.of(departmentUser, teamUser));

    Set<UUID> visible = dataScopeService.visibleUserIds(new UserPrincipal(current));

    assertThat(visible).containsExactlyInAnyOrder(
        current.getId(),
        departmentUser.getId(),
        teamUser.getId()
    );
  }

  @Test
  void customScopeIncludesSelectedOrganizationUsers() {
    SystemOrganization ownDepartment = organization("项目部", null);
    SystemOrganization selectedDepartment = organization("采购部", null);
    SystemUser current = user("项目经理", ownDepartment, "CUSTOM");
    current.getRoles().iterator().next().getDataScopeOrganizations().add(selectedDepartment);
    SystemUser selectedUser = user("采购专员", selectedDepartment, "SELF");

    when(userRepository.findById(current.getId())).thenReturn(Optional.of(current));
    when(userRepository.findByOrganization_IdIn(Set.of(selectedDepartment.getId())))
        .thenReturn(List.of(selectedUser));

    Set<UUID> visible = dataScopeService.visibleUserIds(new UserPrincipal(current));

    assertThat(visible).containsExactlyInAnyOrder(current.getId(), selectedUser.getId());
  }

  private SystemOrganization organization(String name, SystemOrganization parent) {
    SystemOrganization organization = new SystemOrganization();
    organization.setId(UUID.randomUUID());
    organization.setCode(name);
    organization.setName(name);
    organization.setParent(parent);
    return organization;
  }

  private SystemUser user(String name, SystemOrganization organization, String dataScope) {
    SystemRole role = new SystemRole();
    role.setId(UUID.randomUUID());
    role.setCode(name + "_ROLE");
    role.setName(name + "角色");
    role.setDataScope(dataScope);
    SystemUser user = new SystemUser();
    user.setId(UUID.randomUUID());
    user.setUsername(name);
    user.setDisplayName(name);
    user.setPasswordHash("test");
    user.setOrganization(organization);
    user.getRoles().add(role);
    return user;
  }
}
