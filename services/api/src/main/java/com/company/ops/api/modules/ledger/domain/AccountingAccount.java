package com.company.ops.api.modules.ledger.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "fin_accounting_accounts", uniqueConstraints = @UniqueConstraint(
    name = "uk_accounting_account_tenant_code", columnNames = {"tenant_id", "code"}))
public class AccountingAccount extends BaseEntity {
  @Column(nullable = false, length = 32) private String code;
  @Column(nullable = false, length = 120) private String name;
  @Column(nullable = false, length = 24) private String category;
  @Column(name = "normal_direction", nullable = false, length = 12) private String normalDirection;
  @Column(name = "cash_account", nullable = false) private boolean cashAccount;
  @Column(nullable = false) private boolean active = true;
  @Column(name = "system_account", nullable = false) private boolean systemAccount;

  public String getCode() { return code; }
  public void setCode(String value) { code = value; }
  public String getName() { return name; }
  public void setName(String value) { name = value; }
  public String getCategory() { return category; }
  public void setCategory(String value) { category = value; }
  public String getNormalDirection() { return normalDirection; }
  public void setNormalDirection(String value) { normalDirection = value; }
  public boolean isCashAccount() { return cashAccount; }
  public void setCashAccount(boolean value) { cashAccount = value; }
  public boolean isActive() { return active; }
  public void setActive(boolean value) { active = value; }
  public boolean isSystemAccount() { return systemAccount; }
  public void setSystemAccount(boolean value) { systemAccount = value; }
}
