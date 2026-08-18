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
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.QuoteCostRequestRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.collaboration.repository.ProjectHandoverRepository;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectExecutionStatus;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.domain.ProjectType;
import com.company.ops.api.modules.project.dto.AssignProjectManagerRequest;
import com.company.ops.api.modules.project.dto.ChangeProjectExecutionStatusRequest;
import com.company.ops.api.modules.project.dto.ProcessProjectApprovalRequest;
import com.company.ops.api.modules.project.repository.ProjectBudgetItemRepository;
import com.company.ops.api.modules.project.repository.ProjectCloseoutReviewRepository;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.repository.ProjectStageRecordRepository;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectServiceWorkflowTest {
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
    when(budgetRepository.findByProjectIdOrderByCategoryAsc(project.getId())).thenReturn(List.of());
    when(costRepository.findByProjectIdOrderByIncurredDateDescCreatedAtDesc(project.getId())).thenReturn(List.of());
    when(stageRecordRepository.findByProjectIdOrderByChangedAtDesc(project.getId())).thenReturn(List.of());
  }

  @Test
  void approvalUsesAuthenticatedPrincipalAndDoesNotAssignManager() {
    SystemUser approver = user("real-approver", "真实审批人");
    when(dataScopeService.currentPrincipal()).thenReturn(new UserPrincipal(approver));

    service.processApproval(project.getId(),
        new ProcessProjectApprovalRequest(ProjectApprovalStatus.APPROVED, "资料齐全"));

    assertThat(project.getApprovalStatus()).isEqualTo(ProjectApprovalStatus.APPROVED);
    assertThat(project.getApproverUserId()).isEqualTo(approver.getId());
    assertThat(project.getApproverName()).isEqualTo("真实审批人");
    assertThat(project.getManagerUserId()).isNull();
  }

  @Test
  void managerAssignmentRequiresApprovalAndStoresStableUserId() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    SystemUser manager = user("pm-01", "同名也不受影响");
    SystemRole role = new SystemRole();
    role.setCode("PROJECT_MANAGER");
    manager.getRoles().add(role);
    when(userRepository.findDetailById(manager.getId())).thenReturn(Optional.of(manager));
    SystemUser assigner = user("assigner", "分配负责人");
    when(dataScopeService.currentPrincipal()).thenReturn(new UserPrincipal(assigner));

    service.assignManager(project.getId(), new AssignProjectManagerRequest(manager.getId(), "正式分配", false));

    assertThat(project.getManagerUserId()).isEqualTo(manager.getId());
    assertThat(project.getManagerName()).isEqualTo(manager.getDisplayName());
    assertThat(project.getManagerAssignedByUserId()).isEqualTo(assigner.getId());
    assertThat(project.getManagerAssignedByName()).isEqualTo("分配负责人");
    assertThat(project.getManagerAssignmentComment()).isEqualTo("正式分配");
    assertThat(project.getApprovalStatus()).isEqualTo(ProjectApprovalStatus.APPROVED);
  }

  @Test
  void pendingProjectCanAssignManagerWithoutSkippingApproval() {
    SystemUser manager = user("pm-pending", "待审批项目经理");
    SystemRole role = new SystemRole();
    role.setCode("PROJECT_MANAGER");
    manager.getRoles().add(role);
    when(userRepository.findDetailById(manager.getId())).thenReturn(Optional.of(manager));

    service.assignManager(project.getId(),
        new AssignProjectManagerRequest(manager.getId(), "审批前准备", false));

    assertThat(project.getManagerUserId()).isEqualTo(manager.getId());
    assertThat(project.getApprovalStatus()).isEqualTo(ProjectApprovalStatus.PENDING);
  }

  @Test
  void deleteAlwaysArchivesAndNeverPhysicallyDeletesProject() {
    service.deleteProject(project.getId());

    verify(deleteGovernanceService).requestSoftDelete("PROJECT", project.getId(), "XM-001 · 治理项目");
    verify(projectRepository, never()).deleteById(any());
  }

  @Test
  void cancelledProjectCannotResume() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    project.setManagerUserId(UUID.randomUUID());
    project.setManagerName("项目经理");
    project.setExecutionStatus(ProjectExecutionStatus.CANCELLED);

    assertThatThrownBy(() -> service.changeExecutionStatus(project.getId(),
        new ChangeProjectExecutionStatusRequest(ProjectExecutionStatus.ACTIVE, "尝试恢复")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不允许");
  }

  @Test
  void frameworkProjectCannotCancelWhileChildProjectIsActive() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    project.setManagerUserId(UUID.randomUUID());
    project.setManagerName("项目经理");
    Project child = new Project();
    child.setId(UUID.randomUUID());
    child.setParentProjectId(project.getId());
    child.setExecutionStatus(ProjectExecutionStatus.ACTIVE);
    when(projectRepository.findByParentProjectId(project.getId())).thenReturn(List.of(child));

    assertThatThrownBy(() -> service.changeExecutionStatus(project.getId(),
        new ChangeProjectExecutionStatusRequest(ProjectExecutionStatus.CANCELLED, "取消框架")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("仍有未结项或未取消的子项目");
  }

  @Test
  void parentManagerChangeSynchronizesActiveChildren() {
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    Project child = new Project();
    child.setId(UUID.randomUUID());
    child.setParentProjectId(project.getId());
    child.setCode("XM-001-01");
    child.setName("一期子项目");
    child.setExecutionStatus(ProjectExecutionStatus.ACTIVE);
    when(projectRepository.findByParentProjectId(project.getId())).thenReturn(List.of(child));
    SystemUser manager = user("pm-sync", "同步项目经理");
    SystemRole role = new SystemRole();
    role.setCode("PROJECT_MANAGER");
    manager.getRoles().add(role);
    when(userRepository.findDetailById(manager.getId())).thenReturn(Optional.of(manager));

    service.assignManager(project.getId(),
        new AssignProjectManagerRequest(manager.getId(), "统一调整", true));

    assertThat(child.getManagerUserId()).isEqualTo(manager.getId());
    assertThat(child.getManagerName()).isEqualTo("同步项目经理");
    assertThat(child.getManagerAssignmentComment()).contains("随框架项目");
  }

  @Test
  void childProjectApprovalRequiresAssignedManager() {
    project.setParentProjectId(UUID.randomUUID());

    assertThatThrownBy(() -> service.processApproval(project.getId(),
        new ProcessProjectApprovalRequest(ProjectApprovalStatus.APPROVED, "尝试通过")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("请先分配项目经理");
  }

  private SystemUser user(String username, String displayName) {
    SystemUser user = new SystemUser();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setDisplayName(displayName);
    user.setPasswordHash("-");
    user.setEnabled(true);
    return user;
  }
}
