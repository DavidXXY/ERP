package com.company.ops.api.modules.inventory.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inventory_stocktake_lines")
public class InventoryStocktakeLine extends BaseEntity {

  @Column(name = "stocktake_id", nullable = false)
  private UUID stocktakeId;

  @Column(name = "part_id", nullable = false)
  private UUID partId;

  @Column(name = "part_name", nullable = false, length = 160)
  private String partName;

  @Column(name = "book_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal bookQty;

  @Column(name = "actual_qty", precision = 14, scale = 2)
  private BigDecimal actualQty;

  @Column(precision = 14, scale = 2)
  private BigDecimal difference;

  @Column(name = "unit_cost", nullable = false, precision = 14, scale = 4)
  private BigDecimal unitCost = BigDecimal.ZERO;

  @Column(length = 300)
  private String remark;

  public UUID getStocktakeId() { return stocktakeId; }
  public void setStocktakeId(UUID stocktakeId) { this.stocktakeId = stocktakeId; }
  public UUID getPartId() { return partId; }
  public void setPartId(UUID partId) { this.partId = partId; }
  public String getPartName() { return partName; }
  public void setPartName(String partName) { this.partName = partName; }
  public BigDecimal getBookQty() { return bookQty; }
  public void setBookQty(BigDecimal bookQty) { this.bookQty = bookQty; }
  public BigDecimal getActualQty() { return actualQty; }
  public void setActualQty(BigDecimal actualQty) { this.actualQty = actualQty; }
  public BigDecimal getDifference() { return difference; }
  public void setDifference(BigDecimal difference) { this.difference = difference; }
  public BigDecimal getUnitCost() { return unitCost; }
  public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
