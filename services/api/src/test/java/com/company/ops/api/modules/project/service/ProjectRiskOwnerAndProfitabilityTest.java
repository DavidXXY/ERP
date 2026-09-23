package com.company.ops.api.modules.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.domain.RiskSeverity;
import com.company.ops.api.modules.project.domain.RiskStatus;
import com.company.ops.api.modules.project.dto.ProjectProfitabilityResponse;
import com.company.ops.api.modules.project.dto.ProjectRiskRequest;
import com.company.ops.api.modules.project.dto.ProjectRiskResponse;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.repository.ProjectRiskRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProjectRiskOwnerAndProfitabilityTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectRiskRepository riskRepository;
  @Mock private DataScopeService dataScopeService;
  @Mock private DeleteGovernanceService deleteGovernanceService;

  @InjectMocks private ProjectService service;

  @Test
  void createRiskResolvesOwnerUserIdToDisplayName() {
    UUID projectId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Project project = new Project();
    project.setId(projectId);
    when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    when(deleteGovernanceService.isHidden("PROJECT", projectId)).thenReturn(false);
    when(dataScopeService.hasAllDataScope()).thenReturn(true);
    when(dataScopeService.requireVisibleOwnerName(ownerId)).thenReturn("张三");
    when(riskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProjectRiskResponse response = service.createRisk(projectId, new ProjectRiskRequest(
        "风险标题", null, RiskSeverity.HIGH, RiskStatus.OPEN, null, ownerId, null, null));

    assertThat(response.ownerUserId()).isEqualTo(ownerId);
    assertThat(response.ownerName()).isEqualTo("张三");
  }

  @Test
  void createRiskRejectsInvisibleOwner() {
    UUID projectId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Project project = new Project();
    project.setId(projectId);
    when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    when(deleteGovernanceService.isHidden("PROJECT", projectId)).thenReturn(false);
    when(dataScopeService.hasAllDataScope()).thenReturn(true);
    when(dataScopeService.requireVisibleOwnerName(ownerId))
        .thenThrow(new BusinessException("无权分配给该负责人"));

    assertThatThrownBy(() -> service.createRisk(projectId, new ProjectRiskRequest(
        "风险标题", null, RiskSeverity.HIGH, RiskStatus.OPEN, null, ownerId, null, null)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void profitabilityOnlyAtRiskKeepsConsistentTotal() {
    Project low = project(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE);
    Project high = project(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.valueOf(20));
    when(deleteGovernanceService.hiddenIds("PROJECT")).thenReturn(Set.of());
    when(dataScopeService.visibleUserIds()).thenReturn(Set.of());
    when(dataScopeService.visibleOwnerNames()).thenReturn(Set.of());
    when(dataScopeService.hasAllDataScope()).thenReturn(true);
    when(dataScopeService.hasAuthority("project:approve")).thenReturn(false);
    when(projectRepository.findAll(any(Specification.class), any(Sort.class)))
        .thenReturn(List.of(high, low));

    Page<ProjectProfitabilityResponse> page = service.profitability(
        null, null, null, null, true, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent())
        .extracting(ProjectProfitabilityResponse::riskLevel)
        .containsExactly("HIGH");
  }

  private Project project(BigDecimal contract, BigDecimal budget, BigDecimal actual) {
    Project p = new Project();
    p.setName("项目");
    p.setContractAmount(contract);
    p.setBudgetAmount(budget);
    p.setActualCost(actual);
    p.setProgress(0);
    p.setStage(ProjectStage.CONSTRUCTION);
    p.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    return p;
  }
}
