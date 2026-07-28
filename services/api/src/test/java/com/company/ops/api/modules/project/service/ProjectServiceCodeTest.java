package com.company.ops.api.modules.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectType;
import com.company.ops.api.modules.project.dto.CreateProjectRequest;
import com.company.ops.api.modules.project.dto.ProjectBudgetItemRequest;
import com.company.ops.api.modules.project.repository.ProjectBudgetItemRepository;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.repository.ProjectStageRecordRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceCodeTest {

  @Mock private ServiceContractRepository contractRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectBudgetItemRepository budgetRepository;
  @Mock private ProjectCostEntryRepository costRepository;
  @Mock private ProjectCostLedgerService costLedger;
  @Mock private ProjectStageRecordRepository stageRecordRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private DataScopeService dataScopeService;
  @Mock private DeleteGovernanceService deleteGovernanceService;
  @Mock private SystemUserRepository userRepository;
  @Mock private CodeGenerator codeGenerator;

  @InjectMocks private ProjectService projectService;

  @Test
  void linkedProjectKeepsContractBusinessIdentifier() {
    UUID customerId = UUID.randomUUID();
    UUID contractId = UUID.randomUUID();
    ServiceContract contract = new ServiceContract();
    contract.setId(contractId);
    contract.setCode("HT-20260725-0042");
    contract.setCustomerId(customerId);
    contract.setAmount(new BigDecimal("100000"));
    contract.setStatus(ContractStatus.ACTIVE);
    stubCreate(customerId, contract);

    projectService.createProject(request(customerId, contractId));

    ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
    verify(projectRepository).save(projectCaptor.capture());
    assertThat(projectCaptor.getValue().getCode()).isEqualTo("XM-20260725-0042");
    verify(projectRepository).existsByCode("XM-20260725-0042");
    verify(codeGenerator, never()).generate("PROJECT");
  }

  @Test
  void standaloneProjectUsesUnifiedCodeGenerator() {
    UUID customerId = UUID.randomUUID();
    when(codeGenerator.generate("PROJECT")).thenReturn("XM-20260725-0043");
    stubCreate(customerId, null);

    projectService.createProject(request(customerId, null));

    ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
    verify(projectRepository).save(projectCaptor.capture());
    assertThat(projectCaptor.getValue().getCode()).isEqualTo("XM-20260725-0043");
    verify(projectRepository).existsByCode("XM-20260725-0043");
  }

  private void stubCreate(UUID customerId, ServiceContract contract) {
    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setName("测试客户");
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    if (contract != null) {
      when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
    }
    when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
      Project project = invocation.getArgument(0);
      project.setId(UUID.randomUUID());
      return project;
    });
  }

  private CreateProjectRequest request(UUID customerId, UUID contractId) {
    return new CreateProjectRequest(
        customerId,
        null,
        "测试项目",
        ProjectType.RENOVATION,
        null,
        "项目经理",
        "上海市",
        new BigDecimal("100000"),
        LocalDate.of(2026, 7, 25),
        LocalDate.of(2026, 8, 25),
        List.of(new ProjectBudgetItemRequest(ProjectCostCategory.LABOR, new BigDecimal("50000"), "人工预算")),
        null,
        contractId
    );
  }
}
