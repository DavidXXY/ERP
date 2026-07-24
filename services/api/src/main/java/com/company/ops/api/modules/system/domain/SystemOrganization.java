package com.company.ops.api.modules.system.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sys_organizations", uniqueConstraints = @UniqueConstraint(
    name = "uk_sys_organization_tenant_code", columnNames = {"tenant_id", "code"}))
public class SystemOrganization extends BaseEntity {

  @Column(name = "code", length = 64, nullable = false)
  private String code;

  @Column(name = "name", length = 120, nullable = false)
  private String name;

  @Column(name = "type", length = 40)
  private String type = "DEPARTMENT";

  @Column(name = "sort_order")
  private Integer sortOrder = 0;

  @Column(name = "leader_name", length = 80)
  private String leaderName;

  @Column(length = 40)
  private String phone;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(length = 500)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private SystemOrganization parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
  private Set<SystemOrganization> children = new HashSet<>();

  @OneToMany(mappedBy = "organization", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
  private Set<SystemUser> users = new HashSet<>();

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public String getLeaderName() {
    return leaderName;
  }

  public void setLeaderName(String leaderName) {
    this.leaderName = leaderName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public SystemOrganization getParent() {
    return parent;
  }

  public void setParent(SystemOrganization parent) {
    this.parent = parent;
  }

  public Set<SystemOrganization> getChildren() {
    return children;
  }

  public Set<SystemUser> getUsers() {
    return users;
  }
}
