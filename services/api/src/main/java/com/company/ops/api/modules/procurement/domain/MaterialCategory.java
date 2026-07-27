package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "procurement_material_categories",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_material_category_tenant_name",
        columnNames = {"tenant_id", "name"}
    )
)
public class MaterialCategory extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String name;

  @Column(name = "built_in", nullable = false)
  private boolean builtIn;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isBuiltIn() {
    return builtIn;
  }

  public void setBuiltIn(boolean builtIn) {
    this.builtIn = builtIn;
  }
}
