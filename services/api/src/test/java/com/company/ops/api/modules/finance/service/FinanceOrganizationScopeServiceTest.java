package com.company.ops.api.modules.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class FinanceOrganizationScopeServiceTest {
  @Mock private DataScopeService dataScopeService;
  @Mock private SystemOrganizationRepository organizationRepository;
  @Mock private SystemUserRepository userRepository;
  private FinanceOrganizationScopeService service;
  private SystemOrganization root;
  private SystemOrganization sales;
  private SystemOrganization salesEast;
  private SystemOrganization delivery;

  @BeforeEach
  void setUp() {
    service = new FinanceOrganizationScopeService(
        dataScopeService, organizationRepository, userRepository);
    root = organization("总部", null);
    sales = organization("销售中心", root);
    salesEast = organization("华东销售部", sales);
    delivery = organization("交付中心", root);
    when(organizationRepository.findByTenantIdOrderBySortOrderAsc(anyString()))
        .thenReturn(List.of(root, sales, salesEast, delivery));
  }

  @Test
  void visibleTreeContainsOnlyAuthorizedOrganizations() {
    when(dataScopeService.visibleOrganizationIds()).thenReturn(Set.of(sales.getId(), salesEast.getId()));

    var tree = service.visibleOrganizations();

    assertThat(tree).singleElement().satisfies(node -> {
      assertThat(node.id()).isEqualTo(sales.getId());
      assertThat(node.fullPath()).isEqualTo("总部 / 销售中心");
      assertThat(node.children()).singleElement()
          .satisfies(child -> assertThat(child.id()).isEqualTo(salesEast.getId()));
    });
  }

  @Test
  void descendantScopeIsIntersectedWithRolePermissions() {
    when(dataScopeService.visibleOrganizationIds()).thenReturn(Set.of(sales.getId(), salesEast.getId()));
    when(dataScopeService.canViewOrganization(sales.getId())).thenReturn(true);

    var scope = service.resolve(sales.getId(), true);

    assertThat(scope.organizationIds()).containsExactlyInAnyOrder(sales.getId(), salesEast.getId());
    assertThat(scope.info().organizationPath()).isEqualTo("总部 / 销售中心");
    assertThat(scope.info().organizationCount()).isEqualTo(2);
    assertThat(scope.info().unallocatedExcluded()).isTrue();
  }

  @Test
  void rejectsAnOrganizationOutsideTheRoleScope() {
    when(dataScopeService.visibleOrganizationIds()).thenReturn(Set.of(sales.getId()));
    when(dataScopeService.canViewOrganization(delivery.getId())).thenReturn(false);

    assertThatThrownBy(() -> service.resolve(delivery.getId(), false))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("无权查看");
  }

  @Test
  void allDataScopeKeepsUnallocatedCompanyData() {
    when(dataScopeService.visibleOrganizationIds())
        .thenReturn(Set.of(root.getId(), sales.getId(), salesEast.getId(), delivery.getId()));
    when(dataScopeService.hasAllDataScope()).thenReturn(true);

    var scope = service.resolve(null, true);

    assertThat(scope.unrestricted()).isTrue();
    assertThat(scope.info().organizationName()).isEqualTo("全公司");
    assertThat(scope.info().unallocatedExcluded()).isFalse();
  }

  private SystemOrganization organization(String name, SystemOrganization parent) {
    SystemOrganization organization = new SystemOrganization();
    organization.setId(UUID.randomUUID());
    organization.setName(name);
    organization.setCode(UUID.randomUUID().toString());
    organization.setParent(parent);
    return organization;
  }
}
