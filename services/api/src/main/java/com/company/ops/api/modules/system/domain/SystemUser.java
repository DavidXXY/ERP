package com.company.ops.api.modules.system.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "sys_users")
public class SystemUser extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "org_id")
  private SystemOrganization organization;

  @Column(nullable = false, length = 80)
  private String username;

  @Column(name = "display_name", nullable = false, length = 80)
  private String displayName;

  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Column(length = 40)
  private String phone;

  @Column(length = 120)
  private String email;

  @Column(nullable = false)
  private boolean enabled = true;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "sys_user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private Set<SystemRole> roles = new HashSet<>();

  public SystemOrganization getOrganization() {
    return organization;
  }

  public void setOrganization(SystemOrganization organization) {
    this.organization = organization;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Set<SystemRole> getRoles() {
    return roles;
  }
}

