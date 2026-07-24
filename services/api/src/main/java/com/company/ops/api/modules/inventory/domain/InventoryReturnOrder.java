package com.company.ops.api.modules.inventory.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_return_orders")
public class InventoryReturnOrder extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "issue_id", nullable = false)
  private UUID issueId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "return_date", nullable = false)
  private LocalDate returnDate;

  @Column(name = "handler_name", nullable = false, length = 80)
  private String handlerName;

  @Column(nullable = false, length = 255)
  private String reason;

  @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public UUID getIssueId() {
    return issueId;
  }

  public void setIssueId(UUID issueId) {
    this.issueId = issueId;
  }

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public LocalDate getReturnDate() {
    return returnDate;
  }

  public void setReturnDate(LocalDate returnDate) {
    this.returnDate = returnDate;
  }

  public String getHandlerName() {
    return handlerName;
  }

  public void setHandlerName(String handlerName) {
    this.handlerName = handlerName;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }
}
