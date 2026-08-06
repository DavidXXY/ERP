package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_inquiry_invitations")
public class ProcurementInquiryInvitation extends BaseEntity {
  @Column(name = "inquiry_id", nullable = false) private UUID inquiryId;
  @Column(name = "supplier_id", nullable = false) private UUID supplierId;
  @Column(nullable = false, length = 32) private String status = "INVITED";
  @Column(name = "invited_by_name", length = 80) private String invitedByName;
  @Column(name = "invited_at", nullable = false) private OffsetDateTime invitedAt;
  @Column(name = "viewed_at") private OffsetDateTime viewedAt;
  @Column(name = "responded_at") private OffsetDateTime respondedAt;
  @Column(name = "registration_code_hash", length = 255) private String registrationCodeHash;
  @Column(name = "registration_code_expires_at") private OffsetDateTime registrationCodeExpiresAt;
  @Column(name = "registration_code_used_at") private OffsetDateTime registrationCodeUsedAt;
  @Column(name = "delivery_status", nullable = false, length = 32) private String deliveryStatus = "PENDING";
  @Column(name = "delivery_attempt_count", nullable = false) private int deliveryAttemptCount;
  @Column(name = "last_delivery_at") private OffsetDateTime lastDeliveryAt;
  @Column(name = "delivery_error", length = 500) private String deliveryError;
  @Column(name = "declined_at") private OffsetDateTime declinedAt;
  @Column(name = "decline_reason", length = 500) private String declineReason;

  public UUID getInquiryId() { return inquiryId; }
  public void setInquiryId(UUID inquiryId) { this.inquiryId = inquiryId; }
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getInvitedByName() { return invitedByName; }
  public void setInvitedByName(String invitedByName) { this.invitedByName = invitedByName; }
  public OffsetDateTime getInvitedAt() { return invitedAt; }
  public void setInvitedAt(OffsetDateTime invitedAt) { this.invitedAt = invitedAt; }
  public OffsetDateTime getViewedAt() { return viewedAt; }
  public void setViewedAt(OffsetDateTime viewedAt) { this.viewedAt = viewedAt; }
  public OffsetDateTime getRespondedAt() { return respondedAt; }
  public void setRespondedAt(OffsetDateTime respondedAt) { this.respondedAt = respondedAt; }
  public String getRegistrationCodeHash() { return registrationCodeHash; }
  public void setRegistrationCodeHash(String value) { registrationCodeHash = value; }
  public OffsetDateTime getRegistrationCodeExpiresAt() { return registrationCodeExpiresAt; }
  public void setRegistrationCodeExpiresAt(OffsetDateTime value) { registrationCodeExpiresAt = value; }
  public OffsetDateTime getRegistrationCodeUsedAt() { return registrationCodeUsedAt; }
  public void setRegistrationCodeUsedAt(OffsetDateTime value) { registrationCodeUsedAt = value; }
  public String getDeliveryStatus() { return deliveryStatus; }
  public void setDeliveryStatus(String value) { deliveryStatus = value; }
  public int getDeliveryAttemptCount() { return deliveryAttemptCount; }
  public void setDeliveryAttemptCount(int value) { deliveryAttemptCount = value; }
  public OffsetDateTime getLastDeliveryAt() { return lastDeliveryAt; }
  public void setLastDeliveryAt(OffsetDateTime value) { lastDeliveryAt = value; }
  public String getDeliveryError() { return deliveryError; }
  public void setDeliveryError(String value) { deliveryError = value; }
  public OffsetDateTime getDeclinedAt() { return declinedAt; }
  public void setDeclinedAt(OffsetDateTime value) { declinedAt = value; }
  public String getDeclineReason() { return declineReason; }
  public void setDeclineReason(String value) { declineReason = value; }
}
