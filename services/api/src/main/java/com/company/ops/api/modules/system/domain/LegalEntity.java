package com.company.ops.api.modules.system.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_legal_entities")
public class LegalEntity extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(name = "entity_type", nullable = false, length = 24)
  private String entityType = "LEGAL_ENTITY";

  @Column(nullable = false, length = 8)
  private String currency = "CNY";

  @Column(nullable = false)
  private boolean active = true;

  @Column(length = 500)
  private String remark;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getEntityType() { return entityType; }
  public void setEntityType(String entityType) { this.entityType = entityType; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}