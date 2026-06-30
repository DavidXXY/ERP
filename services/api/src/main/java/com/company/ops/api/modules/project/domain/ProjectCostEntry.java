package com.company.ops.api.modules.project.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "project_cost_entries")
public class ProjectCostEntry extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ProjectCostCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 40)
  private ProjectCostSource sourceType;

  @Column(name = "source_no", length = 80)
  private String sourceNo;

  @Column(nullable = false, length = 300)
  private String description;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "incurred_date", nullable = false)
  private LocalDate incurredDate;

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

  public ProjectCostSource getSourceType() {
    return sourceType;
  }

  public void setSourceType(ProjectCostSource sourceType) {
    this.sourceType = sourceType;
  }

  public String getSourceNo() {
    return sourceNo;
  }

  public void setSourceNo(String sourceNo) {
    this.sourceNo = sourceNo;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDate getIncurredDate() {
    return incurredDate;
  }

  public void setIncurredDate(LocalDate incurredDate) {
    this.incurredDate = incurredDate;
  }
}
