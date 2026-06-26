package com.company.ops.api.modules.inventory.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "inventory_parts")
public class InventoryPart extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 120)
  private String model;

  @Column(name = "stock_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal stockQty = BigDecimal.ZERO;

  @Column(name = "safety_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal safetyQty = BigDecimal.ZERO;

  @Column(length = 80)
  private String location;

  @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitCost = BigDecimal.ZERO;

  public boolean isLowStock() {
    return stockQty.compareTo(safetyQty) < 0;
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

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public BigDecimal getStockQty() {
    return stockQty;
  }

  public void setStockQty(BigDecimal stockQty) {
    this.stockQty = stockQty;
  }

  public BigDecimal getSafetyQty() {
    return safetyQty;
  }

  public void setSafetyQty(BigDecimal safetyQty) {
    this.safetyQty = safetyQty;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }

  public void setUnitCost(BigDecimal unitCost) {
    this.unitCost = unitCost;
  }
}
