package com.company.ops.api.modules.qualification.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "qual_attachment_files", uniqueConstraints = {
    @UniqueConstraint(name = "uk_qual_attachment_tenant_object", columnNames = {"tenant_id", "object_key"})
})
public class QualificationAttachmentRecord extends BaseEntity {
  @Column(name = "object_key", nullable = false, length = 255)
  private String objectKey;

  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerUserId;

  @Column(name = "original_name", nullable = false, length = 240)
  private String originalName;

  @Column(name = "content_type", length = 120)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  public String getObjectKey() { return objectKey; }
  public void setObjectKey(String value) { objectKey = value; }
  public UUID getOwnerUserId() { return ownerUserId; }
  public void setOwnerUserId(UUID value) { ownerUserId = value; }
  public String getOriginalName() { return originalName; }
  public void setOriginalName(String value) { originalName = value; }
  public String getContentType() { return contentType; }
  public void setContentType(String value) { contentType = value; }
  public long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(long value) { sizeBytes = value; }
}
