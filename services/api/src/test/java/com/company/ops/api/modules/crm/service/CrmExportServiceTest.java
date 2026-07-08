package com.company.ops.api.modules.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.crm.domain.*;
import com.company.ops.api.modules.crm.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class CrmExportServiceTest {
  @Mock private CustomerRepository customerRepo;
  @Mock private ServiceContractRepository contractRepo;
  @InjectMocks private CrmExportService exportService;

  @Test
  void exportCustomersExcel() {
    Customer c = new Customer();
    c.setId(UUID.randomUUID()); c.setCode("KH-001"); c.setName("Test");
    c.setIndustry("IT"); c.setLevel(CustomerLevel.KEY); c.setOwnerName("Zhang"); c.setRiskStatus(RiskStatus.NORMAL);
    when(customerRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(c));
    Resource r = exportService.exportCustomersExcel();
    assertThat(r).isNotNull(); try { assertThat(r.contentLength()).isGreaterThan(0); } catch (java.io.IOException e) { throw new RuntimeException(e); }
  }

  @Test
  void exportContractsExcel() {
    ServiceContract c = new ServiceContract();
    c.setId(UUID.randomUUID()); c.setCode("HT-001"); c.setProjectName("Project");
    c.setAmount(BigDecimal.valueOf(100000)); c.setContractType("Maintenance");
    c.setStartDate(LocalDate.now()); c.setEndDate(LocalDate.now().plusYears(1));
    c.setStatus(ContractStatus.ACTIVE);
    when(contractRepo.findAllByOrderByEndDateAsc()).thenReturn(List.of(c));
    Resource r = exportService.exportContractsExcel();
    assertThat(r).isNotNull(); try { assertThat(r.contentLength()).isGreaterThan(0); } catch (java.io.IOException e) { throw new RuntimeException(e); }
  }
}
