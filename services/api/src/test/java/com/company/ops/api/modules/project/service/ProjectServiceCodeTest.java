package com.company.ops.api.modules.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.domain.ContractKind;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.QuoteCostRequestRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.collaboration.repository.ProjectHandoverRepository;
import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectExecutionStatus;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectType;
import com.company.ops.api.modules.project.dto.CreateProjectRequest;
import com.company.ops.api.modules.project.dto.ProjectBudgetItemRequest;
import com.company.ops.api.modules.project.repository.ProjectBudgetItemRepository;
import com.company.ops.api.modules.project.repository.ProjectCloseoutReviewRepository;
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
  @Mock private ReceivableRepository receivableRepository;
  @Mock private QuoteCostRequestRepository quoteCostRepository;
  @Mock private ProjectHandoverRepository handoverRepository;
  @Mock private ProjectCloseoutReviewRepository closeoutReviewRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectBudgetItemRepository budgetRepository;
  @Mock private ProjectCostEntryRepository costRepository;
  @Mock private ProjectCostLedgerService costLedger;
  @Mock private ProjectStageRecordRepository stageRecordRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private DataScopeService dataScopeService;
  @Mock private DeleteGovernanceService deleteGovernanceService;
  @Mock private SystemUserRepository userRepository;
  @Mock private SystemNotificationRepository notificationRepository;
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

  @Test
  void childProjectKeepsParentProjectRelationship() {
    UUID customerId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    Project parent = new Project();
    parent.setId(parentId);
    parent.setCustomerId(customerId);
    parent.setCode("XM-PARENT");
    parent.setName("总项目");
    parent.setExecutionStatus(com.company.ops.api.modules.project.domain.ProjectExecutionStatus.ACTIVE);
    when(projectRepository.findById(parentId)).thenReturn(Optional.of(parent));
    when(dataScopeService.hasAllDataScope()).thenReturn(true);
    when(codeGenerator.generate("PROJECT")).thenReturn("XM-CHILD");
    stubCreate(customerId, null);

    CreateProjectRequest base = request(customerId, null);
    projectService.createProject(new CreateProjectRequest(
        base.customerId(), base.code(), "一期子项目", base.projectType(), base.managerUserId(),
        base.managerName(), base.siteAddress(), base.contractAmount(), base.plannedStartDate(),
        base.plannedEndDate(), base.budgetItems(), base.warrantyEndDate(), null, parentId, null));

    ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
    verify(projectRepository).save(captor.capture());
    assertThat(captor.getValue().getParentProjectId()).isEqualTo(parentId);
  }

  @Test
  void frameworkOrderCreatesTopLevelProject() {
    UUID customerId = UUID.randomUUID();
    UUID contractId = UUID.randomUUID();
    ServiceContract framework = new ServiceContract();
    framework.setId(contractId);
    framework.setCustomerId(customerId);
    framework.setCode("HT-KJ-001");
    framework.setContractKind(ContractKind.FRAMEWORK);
    framework.setStatus(ContractStatus.ACTIVE);
    framework.setAmount(null);
    stubCreate(customerId, framework);

    projectService.createProject(request(customerId, contractId));

    ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
    verify(projectRepository).save(captor.capture());
    assertThat(captor.getValue().getContractId()).isEqualTo(contractId);
    assertThat(captor.getValue().getParentProjectId()).isNull();
  }

  @Test
  void childOrderCreatesSubprojectAndNotifiesParentManager() {
    UUID customerId = UUID.randomUUID();
    UUID frameworkId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();

    Project parent = new Project();
    parent.setId(parentId);
    parent.setContractId(frameworkId);
    parent.setCustomerId(customerId);
    parent.setCode("XM-KJ-001");
    parent.setName("年度框架项目");
    parent.setProjectType(ProjectType.RENOVATION);
    parent.setManagerUserId(managerId);
    parent.setManagerName("项目经理甲");
    parent.setSiteAddress("上海市");
    parent.setExecutionStatus(ProjectExecutionStatus.ACTIVE);

    ServiceContract order = new ServiceContract();
    order.setId(orderId);
    order.setParentContractId(frameworkId);
    order.setContractKind(ContractKind.CHILD_ORDER);
    order.setCustomerId(customerId);
    order.setCode("HT-ZDD-001");
    order.setProjectName("一期子项目");
    order.setAmount(new BigDecimal("120000"));
    order.setStartDate(LocalDate.of(2026, 8, 1));
    order.setEndDate(LocalDate.of(2026, 10, 31));

    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setName("测试客户");
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(projectRepository.findLatestByContractId(orderId)).thenReturn(Optional.empty());
    when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
      Project project = invocation.getArgument(0);
      if (project.getId() == null) project.setId(UUID.randomUUID());
      return project;
    });

    Project saved = projectService.createChildProjectFromOrder(
        order, parent, new BigDecimal("120000"));

    assertThat(saved.getParentProjectId()).isEqualTo(parentId);
    assertThat(saved.getContractId()).isEqualTo(orderId);
    assertThat(saved.getManagerUserId()).isEqualTo(managerId);
    assertThat(saved.getApprovalStatus()).isEqualTo(
        com.company.ops.api.modules.project.domain.ProjectApprovalStatus.PENDING);
    assertThat(parent.getContractAmount()).isEqualByComparingTo("120000");
    verify(projectRepository, times(2)).save(any(Project.class));
    ArgumentCaptor<SystemNotification> notificationCaptor = ArgumentCaptor.forClass(SystemNotification.class);
    verify(notificationRepository).save(notificationCaptor.capture());
    assertThat(notificationCaptor.getValue().getTargetUserId()).isEqualTo(managerId);
    assertThat(notificationCaptor.getValue().getRelatedId()).isEqualTo(saved.getId());
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
        contractId,
        null,
        null
    );
  }
}
