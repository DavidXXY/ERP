package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "procurement_supplier_categories",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_supplier_category_tenant_name",
        columnNames = {"tenant_id", "name"}
    )
)
public class SupplierCategory extends BaseEntity {
  @Column(nullable = false, length = 80) private String name;
  @Column(length = 240) private String description;
  @Column(name = "sort_order", nullable = false) private int sortOrder = 100;
  @Column(nullable = false) private boolean enabled = true;
  @Column(name = "built_in", nullable = false) private boolean builtIn;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public int getSortOrder() { return sortOrder; }
  public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public boolean isBuiltIn() { return builtIn; }
  public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }
}
