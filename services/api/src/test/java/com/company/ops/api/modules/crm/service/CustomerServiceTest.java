package com.company.ops.api.modules.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import com.company.ops.api.modules.crm.dto.CreateCustomerRequest;
import com.company.ops.api.modules.crm.dto.UpdateCustomerRequest;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.FollowUpRepository;
import com.company.ops.api.modules.crm.repository.OpportunityRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

  @Mock
  private CustomerRepository customerRepository;
  @Mock
  private OpportunityRepository opportunityRepository;
  @Mock
  private ServiceContractRepository contractRepository;
  @Mock
  private ReceivableRepository receivableRepository;
  @Mock
  private FollowUpRepository followUpRepository;
  @Mock
  private DataScopeService dataScopeService;
  @Mock
  private SystemUserRepository userRepository;
  @Mock
  private DeleteGovernanceService deleteGovernanceService;

  @InjectMocks
  private CustomerService customerService;

  @Test
  void updateCustomerReplacesProfileContactsAndSites() {
    UUID customerId = UUID.randomUUID();
    Customer customer = customer(customerId);
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(customerRepository.save(customer)).thenReturn(customer);
    when(dataScopeService.canViewOwner("客户经理A")).thenReturn(true);
    when(userRepository.findByDisplayNameAndEnabledTrue("客户经理A"))
        .thenReturn(List.of(new SystemUser()));

    var request = new UpdateCustomerRequest(
        "华东轨交能源集团",
        "轨道交通",
        CustomerLevel.STRATEGIC,
        "客户经理A",
        "月结30天",
        RiskStatus.NORMAL,
        "年度预算已确认",
        new CreateCustomerRequest.InvoiceRequest(
            "华东轨交能源集团",
            "91310000TEST",
            "中国银行上海分行",
            "622200001",
            "上海市虹桥路1号",
            "021-60000000"
        ),
        List.of(new CreateCustomerRequest.ContactRequest(
            "王经理", "设备处", "13800000000", "wang@example.com", true
        )),
        List.of(new CreateCustomerRequest.SiteRequest("虹桥基地", "上海市闵行区虹桥基地"))
    );

    var response = customerService.updateCustomer(customerId, request);

    assertThat(response.name()).isEqualTo("华东轨交能源集团");
    assertThat(response.level()).isEqualTo(CustomerLevel.STRATEGIC);
    assertThat(response.invoice().taxNo()).isEqualTo("91310000TEST");
    assertThat(response.contacts()).singleElement().satisfies(contact -> {
      assertThat(contact.name()).isEqualTo("王经理");
      assertThat(contact.primaryContact()).isTrue();
    });
    assertThat(response.sites()).singleElement().satisfies(site ->
        assertThat(site.name()).isEqualTo("虹桥基地"));
    verify(customerRepository).save(customer);
  }

  @Test
  void updateCustomerRejectsMultiplePrimaryContacts() {
    UUID customerId = UUID.randomUUID();
    Customer customer = customer(customerId);
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(dataScopeService.canViewOwner("客户经理A")).thenReturn(true);
    when(userRepository.findByDisplayNameAndEnabledTrue("客户经理A"))
        .thenReturn(List.of(new SystemUser()));

    var request = new UpdateCustomerRequest(
        "华东轨交能源中心",
        "轨道交通",
        CustomerLevel.KEY,
        "客户经理A",
        "",
        RiskStatus.NORMAL,
        "",
        null,
        List.of(
            new CreateCustomerRequest.ContactRequest("联系人一", "", "", "", true),
            new CreateCustomerRequest.ContactRequest("联系人二", "", "", "", true)
        ),
        List.of()
    );

    assertThatThrownBy(() -> customerService.updateCustomer(customerId, request))
        .isInstanceOf(BusinessException.class)
        .hasMessage("一个客户只能设置一位主要联系人");
  }

  private Customer customer(UUID id) {
    Customer customer = new Customer();
    customer.setId(id);
    customer.setCode("KH-001");
    customer.setName("华东轨交能源中心");
    customer.setIndustry("轨道交通");
    customer.setLevel(CustomerLevel.KEY);
    customer.setOwnerName("客户经理A");
    customer.setRiskStatus(RiskStatus.NORMAL);
    return customer;
  }
}
