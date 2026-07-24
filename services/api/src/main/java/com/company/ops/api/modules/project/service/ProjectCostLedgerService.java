package com.company.ops.api.modules.project.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single write path for the project cost ledger and its rebuildable total projection.
 */
@Service
public class ProjectCostLedgerService {
  private final ProjectRepository projects;
  private final ProjectCostEntryRepository entries;

  public ProjectCostLedgerService(ProjectRepository projects, ProjectCostEntryRepository entries) {
    this.projects = projects;
    this.entries = entries;
  }

  @Transactional(readOnly = true)
  public BigDecimal total(UUID projectId) {
    return amount(entries.sumAmountByProjectId(projectId));
  }

  @Transactional
  public ProjectCostEntry record(
      UUID projectId,
      ProjectCostCategory category,
      ProjectCostSource sourceType,
      String sourceNo,
      String description,
      BigDecimal costAmount,
      LocalDate incurredDate
  ) {
    Project project = lockProject(projectId);
    ProjectCostEntry entry = new ProjectCostEntry();
    apply(entry, projectId, category, sourceType, sourceNo, description, costAmount, incurredDate);
    entries.saveAndFlush(entry);
    refreshLocked(project);
    return entry;
  }

  @Transactional
  public ProjectCostEntry upsert(
      UUID projectId,
      ProjectCostCategory category,
      ProjectCostSource sourceType,
      String sourceNo,
      String description,
      BigDecimal costAmount,
      LocalDate incurredDate
  ) {
    Project project = lockProject(projectId);
    ProjectCostEntry entry = entries.findBySourceNo(sourceNo).orElseGet(ProjectCostEntry::new);
    if (entry.getId() != null && !Objects.equals(entry.getProjectId(), projectId)) {
      throw new BusinessException("成本来源编号已被其他项目使用");
    }
    apply(entry, projectId, category, sourceType, sourceNo, description, costAmount, incurredDate);
    entries.saveAndFlush(entry);
    refreshLocked(project);
    return entry;
  }

  @Transactional
  public BigDecimal reconcile(UUID projectId) {
    Project project = lockProject(projectId);
    refreshLocked(project);
    return project.getActualCost();
  }

  private Project lockProject(UUID projectId) {
    return projects.findByIdForUpdate(projectId)
        .orElseThrow(() -> new BusinessException("项目不存在"));
  }

  private void refreshLocked(Project project) {
    project.setActualCost(amount(entries.sumAmountByProjectId(project.getId())));
    projects.save(project);
  }

  private void apply(
      ProjectCostEntry entry,
      UUID projectId,
      ProjectCostCategory category,
      ProjectCostSource sourceType,
      String sourceNo,
      String description,
      BigDecimal costAmount,
      LocalDate incurredDate
  ) {
    entry.setProjectId(projectId);
    entry.setCategory(category);
    entry.setSourceType(sourceType);
    entry.setSourceNo(sourceNo);
    entry.setDescription(description);
    entry.setAmount(amount(costAmount));
    entry.setIncurredDate(incurredDate);
  }
}
