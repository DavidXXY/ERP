package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "system_notifications")
public class SystemNotification extends BaseEntity {
  @Column(nullable = false, length = 40) private String type;
  @Column(nullable = false, length = 180) private String title;
  @Column(nullable = false, length = 1000) private String content;
  @Column(name = "target_user_id") private UUID targetUserId;
  @Column(name = "related_type", length = 80) private String relatedType;
  @Column(name = "related_id") private UUID relatedId;
  @Column(name = "dedup_key", length = 180) private String dedupKey;
  @Column(name = "is_read", nullable = false) private boolean read;
  @Column(name = "read_at") private OffsetDateTime readAt;
  public String getType() { return type; } public void setType(String v) { type = v; }
  public String getTitle() { return title; } public void setTitle(String v) { title = v; }
  public String getContent() { return content; } public void setContent(String v) { content = v; }
  public UUID getTargetUserId() { return targetUserId; } public void setTargetUserId(UUID v) { targetUserId = v; }
  public String getRelatedType() { return relatedType; } public void setRelatedType(String v) { relatedType = v; }
  public UUID getRelatedId() { return relatedId; } public void setRelatedId(UUID v) { relatedId = v; }
  public String getDedupKey() { return dedupKey; } public void setDedupKey(String v) { dedupKey = v; }
  public boolean isRead() { return read; } public void setRead(boolean v) { read = v; }
  public OffsetDateTime getReadAt() { return readAt; } public void setReadAt(OffsetDateTime v) { readAt = v; }
}
