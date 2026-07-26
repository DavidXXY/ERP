package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "work_order_attachments")
public class WorkOrderAttachment extends BaseEntity {
  @Column(name = "work_order_id", nullable = false) private UUID workOrderId;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private WorkOrderAttachmentCategory category;
  @Column(name = "file_name", nullable = false, length = 240) private String fileName;
  @Column(name = "object_key", nullable = false, length = 500) private String objectKey;
  @Column(name = "content_type", length = 120) private String contentType;
  @Column(name = "file_size", nullable = false) private long fileSize;
  @Column(name = "uploaded_by", length = 80) private String uploadedBy;

  public UUID getWorkOrderId() { return workOrderId; }
  public void setWorkOrderId(UUID value) { workOrderId = value; }
  public WorkOrderAttachmentCategory getCategory() { return category; }
  public void setCategory(WorkOrderAttachmentCategory value) { category = value; }
  public String getFileName() { return fileName; }
  public void setFileName(String value) { fileName = value; }
  public String getObjectKey() { return objectKey; }
  public void setObjectKey(String value) { objectKey = value; }
  public String getContentType() { return contentType; }
  public void setContentType(String value) { contentType = value; }
  public long getFileSize() { return fileSize; }
  public void setFileSize(long value) { fileSize = value; }
  public String getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(String value) { uploadedBy = value; }
}
