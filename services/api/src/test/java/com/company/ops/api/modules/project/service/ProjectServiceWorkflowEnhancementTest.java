package com.company.ops.api.modules.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.collaboration.domain.ProjectHandover;
import com.company.ops.api.modules.collaboration.repository.ProjectHandoverRepository;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.QuoteCostRequest;
import com.company.ops.api.modules.crm.domain.QuoteCostStatus;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.QuoteCostRequestRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.project.domain.CloseoutReviewStatus;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectCloseoutReview;
import com.company.ops.api.modules.project.domain.ProjectBudgetItem;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectExecutionStatus;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.domain.ProjectStageRecord;
import com.company.ops.api.modules.project.domain.ProjectType;
import com.company.ops.api.modules.project.dto.CloseoutReviewRequest;
import com.company.ops.api.modules.project.dto.CreateProjectRequest;
import com.company.ops.api.modules.project.dto.ProcessCloseoutReviewRequest;
import com.company.ops.api.modules.project.dto.ProjectBudgetItemRequest;
import com.company.ops.api.modules.project.dto.RollbackProjectStageRequest;
import com.company.ops.api.modules.project.dto.UpdateProjectRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectServiceWorkflowEnhancementTest {
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
  @InjectMocks private ProjectService service;

  private Project project;

  @BeforeEach
  void setUp() {
    project = new Project();
    project.setId(UUID.randomUUID());
    project.setCustomerId(UUID.randomUUID());
    project.setCode("XM-001");
    project.setName("治理项目");
    project.setProjectType(ProjectType.RENOVATION);
    project.setManagerName("待项目管理部门分配");
    project.setSiteAddress("上海");
    project.setContractAmount(new BigDecimal("1000"));
    project.setBudgetAmount(new BigDecimal("800"));
    project.setActualCost(BigDecimal.ZERO);
    project.setPlannedStartDate(LocalDate.now());
    project.setPlannedEndDate(LocalDate.now().plusMonths(1));
    project.setStage(ProjectStage.ENTRY);
    project.setApprovalStatus(ProjectApprovalStatus.PENDING);
    project.setExecutionStatus(ProjectExecutionStatus.ACTIVE);
    when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
    when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(dataScopeService.hasAllDataScope()).thenReturn(true);
    when(customerRepository.findById(project.getCustomerId()))
        .thenReturn(Optional.of(customer("测试客户")));
    when(budgetRepository.findByProjectIdOrderByCategoryAsc(project.getId())).thenReturn(List.of());
    when(costRepository.findByProjectIdOrderByIncurredDateDescCreatedAtDesc(project.getId())).thenReturn(List.of());
    when(stageRecordRepository.findByProjectIdOrderByChangedAtDesc(project.getId())).thenReturn(List.of());
  }

  @Test
  void rejectedTopLevelProjectCanBeEditedAndResubmitted() {
    project.setApprovalStatus(ProjectApprovalStatus.REJECTED);
    when(budgetRepository.findByProjectIdOrderByCategoryAsc(project.getId())).thenReturn(List.of());

    service.updateProject(project.getId(), new UpdateProjectRequest(
        "重新提交的项目",
        "上海 · 新址",
        new BigDecimal("1200"),
        LocalDate.now(),
        LocalDate.now().plusMonths(2),
        LocalDate.now().plusMonths(14),
        List.of(new ProjectBudgetItemRequest(ProjectCostCategory.MATERIAL, new BigDecimal("1200"), "材料预算"))
    ));

    assertThat(project.getName()).isEqualTo("重新提交的项目");
    assertThat(project.getBudgetAmount()).isEqualByComparingTo("1200");
    assertThat(project.getApprovalStatus()).isEqualTo(ProjectApprovalStatus.PENDING);
    assertThat(project.getApproverName()).isNull();
    verify(budgetRepository).deleteByProjectId(project.getId());
  }

  @Test
  void approvedProjectCannotBeEditedDirectly() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);

    assertThatThrownBy(() -> service.updateProject(project.getId(), new UpdateProjectRequest(
        "改名", "上海", new BigDecimal("1000"), LocalDate.now(), LocalDate.now().plusMonths(1),
        null, List.of(new ProjectBudgetItemRequest(ProjectCostCategory.LABOR, BigDecimal.ONE, "人工")))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已审批项目不能直接修改");
    verify(budgetRepository, never()).deleteByProjectId(any());
  }

  @Test
  void rollbackStageMovesToPreviousStageAndRecordsHistory() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    project.setManagerUserId(UUID.randomUUID());
    project.setManagerName("项目经理");
    project.setStage(ProjectStage.CONSTRUCTION);
    project.setProgress(20);
    when(stageRecordRepository.save(any(ProjectStageRecord.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.rollbackStage(project.getId(), new RollbackProjectStageRequest("验收资料需要补充"));

    assertThat(project.getStage()).isEqualTo(ProjectStage.ENTRY);
    assertThat(project.getProgress()).isZero();
    ArgumentCaptor<ProjectStageRecord> captor = ArgumentCaptor.forClass(ProjectStageRecord.class);
    verify(stageRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getFromStage()).isEqualTo(ProjectStage.CONSTRUCTION);
    assertThat(captor.getValue().getToStage()).isEqualTo(ProjectStage.ENTRY);
    assertThat(captor.getValue().getComment()).contains("阶段回退");
  }

  @Test
  void entryStageCannotRollBack() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    project.setManagerUserId(UUID.randomUUID());
    project.setManagerName("项目经理");
    project.setStage(ProjectStage.ENTRY);

    assertThatThrownBy(() -> service.rollbackStage(project.getId(),
        new RollbackProjectStageRequest("尝试回退")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("当前阶段不能回退");
  }

  @Test
  void closingProjectRequiresApprovedCloseoutReview() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    project.setManagerUserId(UUID.randomUUID());
    project.setManagerName("项目经理");
    project.setStage(ProjectStage.WARRANTY);
    when(closeoutReviewRepository.findFirstByProjectIdOrderByCreatedAtDesc(project.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.advanceStage(project.getId(),
        new com.company.ops.api.modules.project.dto.AdvanceProjectStageRequest(ProjectStage.CLOSED, "关闭")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("结项申请");
  }

  @Test
  void closeoutRequestBlockedWhenHandoverNotAccepted() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    project.setManagerUserId(UUID.randomUUID());
    project.setManagerName("项目经理");
    project.setStage(ProjectStage.WARRANTY);
    ProjectHandover handover = new ProjectHandover();
    handover.setStatus("PENDING");
    when(handoverRepository.existsByProjectId(project.getId())).thenReturn(true);
    when(handoverRepository.findByProjectId(project.getId())).thenReturn(Optional.of(handover));

    assertThatThrownBy(() -> service.requestCloseout(project.getId(),
        new CloseoutReviewRequest("申请结项")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("项目交接尚未完成");
  }

  @Test
  void closeoutReviewRejectsWhenNoApplication() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    when(closeoutReviewRepository.findFirstByProjectIdOrderByCreatedAtDesc(project.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.reviewCloseout(project.getId(),
        new ProcessCloseoutReviewRequest(CloseoutReviewStatus.APPROVED, "同意")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("尚未提交结项申请");
  }

  @Test
  void closeoutReviewRejectionRecordsDecision() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    ProjectCloseoutReview review = new ProjectCloseoutReview();
    review.setId(UUID.randomUUID());
    review.setProjectId(project.getId());
    review.setStatus(CloseoutReviewStatus.PENDING);
    when(closeoutReviewRepository.findFirstByProjectIdOrderByCreatedAtDesc(project.getId()))
        .thenReturn(Optional.of(review));
    when(closeoutReviewRepository.save(any(ProjectCloseoutReview.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.reviewCloseout(project.getId(),
        new ProcessCloseoutReviewRequest(CloseoutReviewStatus.REJECTED, "资料不全"));

    assertThat(review.getStatus()).isEqualTo(CloseoutReviewStatus.REJECTED);
    assertThat(review.getReviewComment()).isEqualTo("资料不全");
    verify(closeoutReviewRepository).save(review);
  }

  @Test
  void createProjectPrefillsBudgetFromApprovedQuote() {
    UUID customerId = UUID.randomUUID();
    UUID quoteId = UUID.randomUUID();
    QuoteCostRequest quote = new QuoteCostRequest();
    quote.setId(quoteId);
    quote.setCustomerId(customerId);
    quote.setStatus(QuoteCostStatus.APPROVED);
    quote.setLaborCost(new BigDecimal("30000"));
    quote.setMaterialCost(new BigDecimal("40000"));
    quote.setSubcontractCost(new BigDecimal("20000"));
    quote.setTravelCost(new BigDecimal("5000"));
    quote.setEquipmentCost(new BigDecimal("3000"));
    quote.setRiskReserve(new BigDecimal("1000"));
    quote.setOtherCost(new BigDecimal("1000"));
    when(quoteCostRepository.findById(quoteId)).thenReturn(Optional.of(quote));
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer("报价客户")));
    when(codeGenerator.generate("PROJECT")).thenReturn("XM-QUOTE-001");
    when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
      Project p = invocation.getArgument(0);
      p.setId(UUID.randomUUID());
      return p;
    });
    when(budgetRepository.findByProjectIdOrderByCategoryAsc(any())).thenReturn(List.of());

    service.createProject(new CreateProjectRequest(
        customerId, null, "报价项目", ProjectType.RENOVATION, null, "项目经理", "上海",
        new BigDecimal("100000"), LocalDate.now(), LocalDate.now().plusMonths(3),
        List.of(), null, null, null, quoteId));

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(budgetRepository).saveAll(captor.capture());
    List<ProjectBudgetItem> items = captor.getValue();
    assertThat(items).hasSize(5);
    assertThat(items.stream()
        .filter(item -> item.getCategory() == ProjectCostCategory.MATERIAL)
        .findFirst().orElseThrow().getPlannedAmount())
        .isEqualByComparingTo("40000");
    assertThat(items.stream()
        .filter(item -> item.getCategory() == ProjectCostCategory.OTHER)
        .findFirst().orElseThrow().getPlannedAmount())
        .isEqualByComparingTo("5000");
  }

  private Customer customer(String name) {
    Customer customer = new Customer();
    customer.setId(UUID.randomUUID());
    customer.setName(name);
    return customer;
  }
}
