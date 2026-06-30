package com.company.ops.api.modules.inventory.domain;

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
@Table(name = "inventory_issue_orders")
public class InventoryIssueOrder extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Column(name = "receiver_name", nullable = false, length = 80)
  private String receiverName;

  @Column(nullable = false, length = 300)
  private String purpose;

  @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private InventoryIssueStatus status = InventoryIssueStatus.POSTED;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(LocalDate issueDate) {
    this.issueDate = issueDate;
  }

  public String getReceiverName() {
    return receiverName;
  }

  public void setReceiverName(String receiverName) {
    this.receiverName = receiverName;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public InventoryIssueStatus getStatus() {
    return status;
  }

  public void setStatus(InventoryIssueStatus status) {
    this.status = status;
  }
}
