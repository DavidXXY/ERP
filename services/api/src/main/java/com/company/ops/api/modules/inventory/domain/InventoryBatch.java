package com.company.ops.api.modules.inventory.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_batches")
public class InventoryBatch extends BaseEntity {

  @Column(name = "part_id", nullable = false)
  private UUID partId;

  @Column(name = "batch_no", nullable = false, length = 64)
  private String batchNo;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal quantity;

  @Column(name = "unit_cost", nullable = false, precision = 14, scale = 4)
  private BigDecimal unitCost;

  @Column(name = "received_date")
  private LocalDate receivedDate;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  @Column(nullable = false, length = 16)
  private String status = "ACTIVE";

  @Column(length = 300)
  private String remark;

  public UUID getPartId() { return partId; }
  public void setPartId(UUID partId) { this.partId = partId; }
  public String getBatchNo() { return batchNo; }
  public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
  public BigDecimal getQuantity() { return quantity; }
  public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
  public BigDecimal getUnitCost() { return unitCost; }
  public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
  public LocalDate getReceivedDate() { return receivedDate; }
  public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
  public LocalDate getExpiryDate() { return expiryDate; }
  public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
