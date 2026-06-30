package com.company.ops.api.modules.inventory.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inventory_issue_lines")
public class InventoryIssueLine extends BaseEntity {

  @Column(name = "issue_id", nullable = false)
  private UUID issueId;

  @Column(name = "part_id", nullable = false)
  private UUID partId;

  @Column(name = "part_name", nullable = false, length = 160)
  private String partName;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal quantity;

  @Column(name = "returned_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal returnedQty = BigDecimal.ZERO;

  @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitCost;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  public UUID getIssueId() {
    return issueId;
  }

  public void setIssueId(UUID issueId) {
    this.issueId = issueId;
  }

  public UUID getPartId() {
    return partId;
  }

  public void setPartId(UUID partId) {
    this.partId = partId;
  }

  public String getPartName() {
    return partName;
  }

  public void setPartName(String partName) {
    this.partName = partName;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getReturnedQty() {
    return returnedQty;
  }

  public void setReturnedQty(BigDecimal returnedQty) {
    this.returnedQty = returnedQty;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }

  public void setUnitCost(BigDecimal unitCost) {
    this.unitCost = unitCost;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
