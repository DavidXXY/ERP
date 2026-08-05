package com.company.ops.api.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

  @Mock private CustomerRepository customerRepo;
  @Mock private ServiceContractRepository contractRepo;
  @Mock private InventoryPartRepository partRepo;
  @Mock private ProjectRepository projectRepo;
  @Mock private QualificationEmployeeRepository empRepo;
  @Mock private DataScopeService dataScope;
  @Mock private UserPrincipal principal;

  @InjectMocks private SearchService service;

  @Test
  void searchDoesNotQueryModulesWithoutPermission() {
    when(principal.roleCodes()).thenReturn(List.of("EMPLOYEE"));
    when(principal.permissions()).thenReturn(List.of());

    assertThat(service.search("测试", principal)).isEmpty();

    verifyNoInteractions(customerRepo, contractRepo, partRepo, projectRepo, empRepo, dataScope);
  }

  @Test
  void customerSearchAppliesOwnerDataScope() {
    Customer visible = customer("C-001", "可见客户");
    Customer hidden = customer("C-002", "越权客户");
    when(principal.roleCodes()).thenReturn(List.of("SALES"));
    when(principal.permissions()).thenReturn(List.of("crm:customer:view"));
    when(customerRepo.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
        anyString(), anyString(), any())).thenReturn(List.of(visible, hidden));
    when(dataScope.canViewOwner(visible.getOwnerUserId())).thenReturn(true);
    when(dataScope.canViewOwner(hidden.getOwnerUserId())).thenReturn(false);

    var results = service.search("客户", principal);

    assertThat(results).extracting(SearchService.SearchResult::id)
        .containsExactly(visible.getId().toString());
    assertThat(results.get(0).subtitle()).isEqualTo("普通客户");
  }

  private Customer customer(String code, String name) {
    Customer customer = new Customer();
    customer.setId(UUID.randomUUID());
    customer.setCode(code);
    customer.setName(name);
    customer.setLevel(CustomerLevel.NORMAL);
    customer.setOwnerUserId(UUID.randomUUID());
    return customer;
  }
}
