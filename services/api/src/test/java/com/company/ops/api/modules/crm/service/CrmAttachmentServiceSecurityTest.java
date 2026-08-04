package com.company.ops.api.modules.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.repository.CrmAttachmentRepository;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.OpportunityRepository;
import com.company.ops.api.modules.crm.repository.QuotePlanRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CrmAttachmentServiceSecurityTest {
  @Mock private FileStorageService storageService;
  @Mock private CrmAttachmentRepository attachmentRepository;
  @Mock private DeleteGovernanceService deleteGovernanceService;
  @Mock private CustomerRepository customerRepository;
  @Mock private OpportunityRepository opportunityRepository;
  @Mock private QuotePlanRepository quotePlanRepository;
  @Mock private ServiceContractRepository contractRepository;
  @Mock private ReceivableRepository receivableRepository;
  @Mock private DataScopeService dataScopeService;
  private CrmAttachmentService service;

  @BeforeEach
  void setUp() {
    service = new CrmAttachmentService(storageService, attachmentRepository, deleteGovernanceService,
        customerRepository, opportunityRepository, quotePlanRepository, contractRepository,
        receivableRepository, dataScopeService);
  }

  @Test
  void deniesCustomerAttachmentOutsideCurrentDataScope() {
    UUID customerId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Customer customer = new Customer();
    customer.setOwnerUserId(ownerId);
    when(dataScopeService.hasAuthority("crm:customer:view")).thenReturn(true);
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(dataScopeService.canViewOwner(ownerId)).thenReturn(false);

    assertThatThrownBy(() -> service.listByEntity("CUSTOMER", customerId))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("无权访问该客户附件");
  }

  @Test
  void listsAttachmentsOnlyAfterPermissionAndDataScopeChecksPass() {
    UUID customerId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Customer customer = new Customer();
    customer.setOwnerUserId(ownerId);
    when(dataScopeService.hasAuthority("crm:customer:view")).thenReturn(true);
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(dataScopeService.canViewOwner(ownerId)).thenReturn(true);
    when(attachmentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("CUSTOMER", customerId))
        .thenReturn(List.of());

    assertThat(service.listByEntity("customer", customerId)).isEmpty();
  }
}
