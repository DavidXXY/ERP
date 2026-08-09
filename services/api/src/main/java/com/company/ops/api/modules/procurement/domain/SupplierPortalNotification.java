package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "supplier_portal_notifications")
public class SupplierPortalNotification extends BaseEntity {

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Column(nullable = false, length = 40)
  private String type;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(nullable = false, length = 1000)
  private String content;

  @Column(name = "related_type", length = 80)
  private String relatedType;

  @Column(name = "related_id")
  private UUID relatedId;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "read_at")
  private OffsetDateTime readAt;

  public UUID getAccountId() { return accountId; } public void setAccountId(UUID v) { accountId = v; }
  public UUID getSupplierId() { return supplierId; } public void setSupplierId(UUID v) { supplierId = v; }
  public String getType() { return type; } public void setType(String v) { type = v; }
  public String getTitle() { return title; } public void setTitle(String v) { title = v; }
  public String getContent() { return content; } public void setContent(String v) { content = v; }
  public String getRelatedType() { return relatedType; } public void setRelatedType(String v) { relatedType = v; }
  public UUID getRelatedId() { return relatedId; } public void setRelatedId(UUID v) { relatedId = v; }
  public boolean isRead() { return read; } public void setRead(boolean v) { read = v; }
  public OffsetDateTime getReadAt() { return readAt; } public void setReadAt(OffsetDateTime v) { readAt = v; }
}
