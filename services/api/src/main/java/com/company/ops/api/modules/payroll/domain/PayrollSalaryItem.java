package com.company.ops.api.modules.payroll.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_salary_items")
public class PayrollSalaryItem extends BaseEntity {

  @Column(name = "employee_id", nullable = false)
  private UUID employeeId;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "item_type", nullable = false, length = 24)
  private String itemType;

  @Column(nullable = false)
  private boolean active = true;

  @Column(length = 300)
  private String remark;

  public UUID getEmployeeId() {
    return employeeId;
  }

  public void setEmployeeId(UUID employeeId) {
    this.employeeId = employeeId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getItemType() {
    return itemType;
  }

  public void setItemType(String itemType) {
    this.itemType = itemType;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }
}
