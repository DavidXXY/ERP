package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries")
public class NotificationDelivery extends BaseEntity {
  @Column(name = "notification_id", nullable = false) private UUID notificationId;
  @Column(name = "user_id", nullable = false) private UUID userId;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private NotificationChannel channel;
  @Column(nullable = false, length = 24) private String status;
  @Column(name = "attempt_count", nullable = false) private int attemptCount;
  @Column(name = "last_attempt_at") private OffsetDateTime lastAttemptAt;
  @Column(name = "delivered_at") private OffsetDateTime deliveredAt;
  @Column(name = "last_error", length = 500) private String lastError;
  public UUID getNotificationId() { return notificationId; }
  public void setNotificationId(UUID value) { notificationId = value; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID value) { userId = value; }
  public NotificationChannel getChannel() { return channel; }
  public void setChannel(NotificationChannel value) { channel = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public int getAttemptCount() { return attemptCount; }
  public void setAttemptCount(int value) { attemptCount = value; }
  public OffsetDateTime getLastAttemptAt() { return lastAttemptAt; }
  public void setLastAttemptAt(OffsetDateTime value) { lastAttemptAt = value; }
  public OffsetDateTime getDeliveredAt() { return deliveredAt; }
  public void setDeliveredAt(OffsetDateTime value) { deliveredAt = value; }
  public String getLastError() { return lastError; }
  public void setLastError(String value) { lastError = value; }
}
