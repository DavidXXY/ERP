package com.company.ops.api.modules.inventory.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_stocktakes")
public class InventoryStocktake extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "count_date", nullable = false)
  private LocalDate countDate;

  @Column(nullable = false, length = 24)
  private String status = "DRAFT";

  @Column(length = 500)
  private String remark;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public LocalDate getCountDate() { return countDate; }
  public void setCountDate(LocalDate countDate) { this.countDate = countDate; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
