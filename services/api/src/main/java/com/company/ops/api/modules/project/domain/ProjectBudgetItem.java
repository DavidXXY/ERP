package com.company.ops.api.modules.project.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "project_budget_items")
public class ProjectBudgetItem extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ProjectCostCategory category;

  @Column(name = "planned_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal plannedAmount;

  @Column(length = 300)
  private String remark;

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public ProjectCostCategory getCategory() {
    return category;
  }

  public void setCategory(ProjectCostCategory category) {
    this.category = category;
  }

  public BigDecimal getPlannedAmount() {
    return plannedAmount;
  }

  public void setPlannedAmount(BigDecimal plannedAmount) {
    this.plannedAmount = plannedAmount;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }
}
