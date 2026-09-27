package com.company.ops.api.modules.system.domain;

import java.io.Serializable;
import java.util.Objects;

public class CodeSequenceId implements Serializable {

  private String entityType;
  private String tenantId;

  public CodeSequenceId() {}

  public CodeSequenceId(String entityType, String tenantId) {
    this.entityType = entityType;
    this.tenantId = tenantId;
  }

  public String getEntityType() { return entityType; }
  public void setEntityType(String entityType) { this.entityType = entityType; }

  public String getTenantId() { return tenantId; }
  public void setTenantId(String tenantId) { this.tenantId = tenantId; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CodeSequenceId that)) return false;
    return Objects.equals(entityType, that.entityType) && Objects.equals(tenantId, that.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType, tenantId);
  }
}