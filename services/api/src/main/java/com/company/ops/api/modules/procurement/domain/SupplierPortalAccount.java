package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_supplier_portal_accounts")
public class SupplierPortalAccount extends BaseEntity {
  @Column(name = "supplier_id", nullable = false) private UUID supplierId;
  @Column(nullable = false, length = 160) private String email;
  @Column(length = 40) private String phone;
  @Column(name = "contact_name", nullable = false, length = 80) private String contactName;
  @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
  @Column(nullable = false, length = 32) private String status = "PENDING_REVIEW";
  @Column(name = "review_comment", length = 500) private String reviewComment;
  @Column(name = "reviewed_by_name", length = 80) private String reviewedByName;
  @Column(name = "reviewed_at") private OffsetDateTime reviewedAt;
  @Column(name = "last_login_at") private OffsetDateTime lastLoginAt;
  @Column(name = "auth_version", nullable = false) private long authVersion;
  @Column(name = "profile_draft_json", columnDefinition = "text") private String profileDraftJson;
  @Column(name = "must_change_password", nullable = false) private boolean mustChangePassword;
  @Column(name = "password_changed_at") private OffsetDateTime passwordChangedAt;
  @Column(name = "reset_token_hash", length = 255) private String resetTokenHash;
  @Column(name = "reset_token_expires_at") private OffsetDateTime resetTokenExpiresAt;
  @Column(name = "reset_token_used_at") private OffsetDateTime resetTokenUsedAt;
  @Column(name = "mfa_enabled", nullable = false) private boolean mfaEnabled;
  @Column(name = "mfa_secret", length = 1024) private String mfaSecret;
  @Column(name = "mfa_recovery_codes", columnDefinition = "text") private String mfaRecoveryCodes;

  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getContactName() { return contactName; }
  public void setContactName(String contactName) { this.contactName = contactName; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getReviewComment() { return reviewComment; }
  public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
  public String getReviewedByName() { return reviewedByName; }
  public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }
  public OffsetDateTime getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
  public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
  public void setLastLoginAt(OffsetDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
  public long getAuthVersion() { return authVersion; }
  public void bumpAuthVersion() { authVersion++; }
  public String getProfileDraftJson() { return profileDraftJson; }
  public void setProfileDraftJson(String profileDraftJson) { this.profileDraftJson = profileDraftJson; }
  public boolean isMustChangePassword() { return mustChangePassword; }
  public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
  public OffsetDateTime getPasswordChangedAt() { return passwordChangedAt; }
  public void setPasswordChangedAt(OffsetDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
  public String getResetTokenHash() { return resetTokenHash; }
  public void setResetTokenHash(String resetTokenHash) { this.resetTokenHash = resetTokenHash; }
  public OffsetDateTime getResetTokenExpiresAt() { return resetTokenExpiresAt; }
  public void setResetTokenExpiresAt(OffsetDateTime resetTokenExpiresAt) { this.resetTokenExpiresAt = resetTokenExpiresAt; }
  public OffsetDateTime getResetTokenUsedAt() { return resetTokenUsedAt; }
  public void setResetTokenUsedAt(OffsetDateTime resetTokenUsedAt) { this.resetTokenUsedAt = resetTokenUsedAt; }
  public boolean isMfaEnabled() { return mfaEnabled; }
  public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
  public String getMfaSecret() { return mfaSecret; }
  public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }
  public String getMfaRecoveryCodes() { return mfaRecoveryCodes; }
  public void setMfaRecoveryCodes(String mfaRecoveryCodes) { this.mfaRecoveryCodes = mfaRecoveryCodes; }
}
