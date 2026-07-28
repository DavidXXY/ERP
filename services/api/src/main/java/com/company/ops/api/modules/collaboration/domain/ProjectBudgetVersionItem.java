package com.company.ops.api.modules.collaboration.domain;

import com.company.ops.api.common.domain.BaseEntity;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "biz_project_budget_version_items")
public class ProjectBudgetVersionItem extends BaseEntity {
  @Column(name = "budget_version_id", nullable = false)
  private UUID budgetVersionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ProjectCostCategory category;

  @Column(name = "planned_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal plannedAmount;

  public UUID getBudgetVersionId() { return budgetVersionId; }
  public void setBudgetVersionId(UUID value) { budgetVersionId = value; }
  public ProjectCostCategory getCategory() { return category; }
  public void setCategory(ProjectCostCategory value) { category = value; }
  public BigDecimal getPlannedAmount() { return plannedAmount; }
  public void setPlannedAmount(BigDecimal value) { plannedAmount = value; }
}
