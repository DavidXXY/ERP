package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_notification_reads")
public class SystemNotificationRead extends BaseEntity {
  @Column(name = "notification_id", nullable = false)
  private UUID notificationId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "read_at", nullable = false)
  private OffsetDateTime readAt;

  public UUID getNotificationId() { return notificationId; }
  public void setNotificationId(UUID value) { notificationId = value; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID value) { userId = value; }
  public OffsetDateTime getReadAt() { return readAt; }
  public void setReadAt(OffsetDateTime value) { readAt = value; }
}
