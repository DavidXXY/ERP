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
@Table(name = "project_projects")
public class Project extends BaseEntity {

  @Column(name = "customer_id")
  private UUID customerId;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 180)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ProjectStage stage = ProjectStage.INITIATED;

  @Column(name = "budget_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal budgetAmount = BigDecimal.ZERO;

  @Column(name = "actual_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal actualCost = BigDecimal.ZERO;

  @Column(nullable = false)
  private int progress;

  @Column(name = "warranty_end_date")
  private LocalDate warrantyEndDate;

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ProjectStage getStage() {
    return stage;
  }

  public void setStage(ProjectStage stage) {
    this.stage = stage;
  }

  public BigDecimal getBudgetAmount() {
    return budgetAmount;
  }

  public void setBudgetAmount(BigDecimal budgetAmount) {
    this.budgetAmount = budgetAmount;
  }

  public BigDecimal getActualCost() {
    return actualCost;
  }

  public void setActualCost(BigDecimal actualCost) {
    this.actualCost = actualCost;
  }

  public int getProgress() {
    return progress;
  }

  public void setProgress(int progress) {
    this.progress = progress;
  }

  public LocalDate getWarrantyEndDate() {
    return warrantyEndDate;
  }

  public void setWarrantyEndDate(LocalDate warrantyEndDate) {
    this.warrantyEndDate = warrantyEndDate;
  }
}
