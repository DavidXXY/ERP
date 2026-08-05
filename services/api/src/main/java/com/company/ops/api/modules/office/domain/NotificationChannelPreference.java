package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "notification_channel_preferences")
public class NotificationChannelPreference extends BaseEntity {
  @Column(name = "user_id", nullable = false) private UUID userId;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private NotificationChannel channel;
  @Column(nullable = false) private boolean enabled;
  public UUID getUserId() { return userId; }
  public void setUserId(UUID value) { userId = value; }
  public NotificationChannel getChannel() { return channel; }
  public void setChannel(NotificationChannel value) { channel = value; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean value) { enabled = value; }
}
