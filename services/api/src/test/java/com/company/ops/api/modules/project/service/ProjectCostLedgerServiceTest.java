package com.company.ops.api.modules.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import com.company.ops.api.modules.project.domain.ProjectExecutionStatus;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectCostLedgerServiceTest {
  @Mock private ProjectRepository projects;
  @Mock private ProjectCostEntryRepository entries;
  @InjectMocks private ProjectCostLedgerService service;
  private Project project;

  @BeforeEach
  void setUp() {
    project = new Project();
    project.setId(UUID.randomUUID());
    project.setBudgetAmount(new BigDecimal("1000.00"));
    when(projects.findByIdForUpdate(project.getId())).thenReturn(Optional.of(project));
  }

  @Test
  void recordRefreshesProjectionFromLedgerTotal() {
    when(entries.sumAmountByProjectId(project.getId()))
        .thenReturn(BigDecimal.ZERO, new BigDecimal("125.50"));

    ProjectCostEntry entry = service.record(project.getId(), ProjectCostCategory.MATERIAL,
        ProjectCostSource.INVENTORY, "ISSUE-1", "领料", new BigDecimal("125.50"), LocalDate.now());

    assertThat(entry.getAmount()).isEqualByComparingTo("125.50");
    assertThat(project.getActualCost()).isEqualByComparingTo("125.50");
    verify(entries).saveAndFlush(entry);
    verify(projects).save(project);
  }

  @Test
  void upsertReplacesAmountWithoutIncrementalAccumulation() {
    ProjectCostEntry existing = new ProjectCostEntry();
    existing.setId(UUID.randomUUID());
    existing.setProjectId(project.getId());
    existing.setAmount(BigDecimal.TEN);
    when(entries.findBySourceTypeAndSourceNo(ProjectCostSource.MANUAL, "STAFF-1")).thenReturn(Optional.of(existing));
    when(entries.sumAmountByProjectId(project.getId()))
        .thenReturn(BigDecimal.TEN, BigDecimal.valueOf(30));

    service.upsert(project.getId(), ProjectCostCategory.LABOR, ProjectCostSource.MANUAL,
        "STAFF-1", "工时", BigDecimal.valueOf(30), LocalDate.now());

    assertThat(existing.getAmount()).isEqualByComparingTo("30");
    assertThat(project.getActualCost()).isEqualByComparingTo("30");
  }

  @Test
  void rejectsSourceNumberOwnedByAnotherProject() {
    ProjectCostEntry existing = new ProjectCostEntry();
    existing.setId(UUID.randomUUID());
    existing.setProjectId(UUID.randomUUID());
    when(entries.findBySourceTypeAndSourceNo(ProjectCostSource.MANUAL, "STAFF-1")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.upsert(project.getId(), ProjectCostCategory.LABOR,
        ProjectCostSource.MANUAL, "STAFF-1", "工时", BigDecimal.ONE, LocalDate.now()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("其他项目");
    verify(entries, org.mockito.Mockito.never()).saveAndFlush(any());
  }

  @Test
  void rejectsProjectedCostAboveBudgetInsideProjectLock() {
    project.setBudgetAmount(new BigDecimal("100.00"));
    when(entries.sumAmountByProjectId(project.getId())).thenReturn(new BigDecimal("90.00"));

    assertThatThrownBy(() -> service.record(project.getId(), ProjectCostCategory.TRAVEL,
        ProjectCostSource.EXPENSE, "EXP-1", "差旅", new BigDecimal("10.01"), LocalDate.now()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("超过项目预算");
    verify(entries, org.mockito.Mockito.never()).saveAndFlush(any());
  }

  @Test
  void allowsNegativeReversalOnCancelledProject() {
    project.setExecutionStatus(ProjectExecutionStatus.CANCELLED);
    when(entries.sumAmountByProjectId(project.getId())).thenReturn(new BigDecimal("80.00"), new BigDecimal("60.00"));

    ProjectCostEntry reversal = service.record(project.getId(), ProjectCostCategory.MATERIAL,
        ProjectCostSource.INVENTORY, "RETURN-1", "退料冲销", new BigDecimal("-20.00"), LocalDate.now());

    assertThat(reversal.getAmount()).isEqualByComparingTo("-20.00");
    assertThat(project.getActualCost()).isEqualByComparingTo("60.00");
  }

  @Test
  void repeatedSourceUpdatesSameLedgerEntryIdempotently() {
    ProjectCostEntry existing = new ProjectCostEntry();
    existing.setId(UUID.randomUUID());
    existing.setProjectId(project.getId());
    existing.setAmount(new BigDecimal("20.00"));
    when(entries.findBySourceTypeAndSourceNo(ProjectCostSource.EXPENSE, "EXP-2"))
        .thenReturn(Optional.of(existing));
    when(entries.sumAmountByProjectId(project.getId())).thenReturn(new BigDecimal("20.00"), new BigDecimal("25.00"));

    ProjectCostEntry updated = service.record(project.getId(), ProjectCostCategory.TRAVEL,
        ProjectCostSource.EXPENSE, "EXP-2", "费用调整", new BigDecimal("25.00"), LocalDate.now());

    assertThat(updated).isSameAs(existing);
    assertThat(updated.getAmount()).isEqualByComparingTo("25.00");
    assertThat(project.getActualCost()).isEqualByComparingTo("25.00");
  }
}
