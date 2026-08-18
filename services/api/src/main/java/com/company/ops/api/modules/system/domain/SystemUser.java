package com.company.ops.api.modules.system.domain;

import com.company.ops.api.common.domain.BaseEntity;
import com.company.ops.api.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
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

  @Column(name = "last_login_at")
  private OffsetDateTime lastLoginAt;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "auth_version", nullable = false)
  private long authVersion;

  @Column(name = "mfa_enabled", nullable = false)
  private boolean mfaEnabled;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "mfa_secret", length = 1024)
  private String mfaSecret;

  @Column(name = "mfa_recovery_codes", columnDefinition = "text")
  private String mfaRecoveryCodes;

  @ManyToMany(fetch = FetchType.EAGER)
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

  public OffsetDateTime getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(OffsetDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getAuthVersion() { return authVersion; }

  public void bumpAuthVersion() { authVersion++; }

  public boolean isMfaEnabled() { return mfaEnabled; }

  public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

  public String getMfaSecret() { return mfaSecret; }

  public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }

  public String getMfaRecoveryCodes() { return mfaRecoveryCodes; }

  public void setMfaRecoveryCodes(String mfaRecoveryCodes) { this.mfaRecoveryCodes = mfaRecoveryCodes; }

  public Set<SystemRole> getRoles() {
    return roles;
  }
}
