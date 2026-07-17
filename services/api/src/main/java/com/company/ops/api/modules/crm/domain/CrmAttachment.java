package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "crm_attachment")
public class CrmAttachment extends BaseEntity {

  @Column(name = "entity_type", nullable = false, length = 20)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private UUID entityId;

  @Column(name = "attachment_type", length = 20)
  private String attachmentType;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "file_path", nullable = false, length = 500)
  private String filePath;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @Column(name = "mime_type", length = 100)
  private String mimeType;

  @Column(name = "uploaded_by", nullable = false, length = 80)
  private String uploadedBy;

  @Column(name = "uploaded_at", nullable = false)
  private OffsetDateTime uploadedAt;

  public String getEntityType() { return entityType; }
  public void setEntityType(String v) { this.entityType = v; }
  public UUID getEntityId() { return entityId; }
  public void setEntityId(UUID v) { this.entityId = v; }
  public String getAttachmentType() { return attachmentType; }
  public void setAttachmentType(String v) { this.attachmentType = v; }
  public String getFileName() { return fileName; }
  public void setFileName(String v) { this.fileName = v; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String v) { this.filePath = v; }
  public Long getFileSize() { return fileSize; }
  public void setFileSize(Long v) { this.fileSize = v; }
  public String getMimeType() { return mimeType; }
  public void setMimeType(String v) { this.mimeType = v; }
  public String getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(String v) { this.uploadedBy = v; }
  public OffsetDateTime getUploadedAt() { return uploadedAt; }
  public void setUploadedAt(OffsetDateTime v) { this.uploadedAt = v; }
}
