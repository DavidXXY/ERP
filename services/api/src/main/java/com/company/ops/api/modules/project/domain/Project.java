package com.company.ops.api.modules.project.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
  @Column(name = "project_type", nullable = false, length = 40)
  private ProjectType projectType = ProjectType.RENOVATION;

  @Column(name = "manager_name", nullable = false, length = 80)
  private String managerName;

  @Column(name = "site_address", nullable = false, length = 300)
  private String siteAddress;

  @Column(name = "contract_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal contractAmount = BigDecimal.ZERO;

  @Column(name = "planned_start_date")
  private LocalDate plannedStartDate;

  @Column(name = "planned_end_date")
  private LocalDate plannedEndDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "approval_status", nullable = false, length = 32)
  private ProjectApprovalStatus approvalStatus = ProjectApprovalStatus.PENDING;

  @Column(name = "approval_comment", length = 500)
  private String approvalComment;

  @Column(name = "approver_name", length = 80)
  private String approverName;

  @Column(name = "approved_at")
  private OffsetDateTime approvedAt;

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

  public ProjectType getProjectType() {
    return projectType;
  }

  public void setProjectType(ProjectType projectType) {
    this.projectType = projectType;
  }

  public String getManagerName() {
    return managerName;
  }

  public void setManagerName(String managerName) {
    this.managerName = managerName;
  }

  public String getSiteAddress() {
    return siteAddress;
  }

  public void setSiteAddress(String siteAddress) {
    this.siteAddress = siteAddress;
  }

  public BigDecimal getContractAmount() {
    return contractAmount;
  }

  public void setContractAmount(BigDecimal contractAmount) {
    this.contractAmount = contractAmount;
  }

  public LocalDate getPlannedStartDate() {
    return plannedStartDate;
  }

  public void setPlannedStartDate(LocalDate plannedStartDate) {
    this.plannedStartDate = plannedStartDate;
  }

  public LocalDate getPlannedEndDate() {
    return plannedEndDate;
  }

  public void setPlannedEndDate(LocalDate plannedEndDate) {
    this.plannedEndDate = plannedEndDate;
  }

  public ProjectApprovalStatus getApprovalStatus() {
    return approvalStatus;
  }

  public void setApprovalStatus(ProjectApprovalStatus approvalStatus) {
    this.approvalStatus = approvalStatus;
  }

  public String getApprovalComment() {
    return approvalComment;
  }

  public void setApprovalComment(String approvalComment) {
    this.approvalComment = approvalComment;
  }

  public String getApproverName() {
    return approverName;
  }

  public void setApproverName(String approverName) {
    this.approverName = approverName;
  }

  public OffsetDateTime getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(OffsetDateTime approvedAt) {
    this.approvedAt = approvedAt;
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
