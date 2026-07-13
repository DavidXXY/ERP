package com.company.ops.api.modules.system.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sys_roles")
public class SystemRole extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "data_scope", nullable = false, length = 40)
  private String dataScope = "SELF";

  @Column(name = "built_in", nullable = false)
  private boolean builtIn;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "sys_role_permissions",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id")
  )
  private Set<SystemPermission> permissions = new HashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "sys_role_data_organizations",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "organization_id")
  )
  private Set<SystemOrganization> dataScopeOrganizations = new HashSet<>();

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

  public String getDataScope() {
    return dataScope;
  }

  public void setDataScope(String dataScope) {
    this.dataScope = dataScope;
  }

  public boolean isBuiltIn() {
    return builtIn;
  }

  public void setBuiltIn(boolean builtIn) {
    this.builtIn = builtIn;
  }

  public Set<SystemPermission> getPermissions() {
    return permissions;
  }

  public Set<SystemOrganization> getDataScopeOrganizations() {
    return dataScopeOrganizations;
  }
}
