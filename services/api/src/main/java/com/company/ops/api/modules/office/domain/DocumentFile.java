package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity @Table(name = "doc_files")
public class DocumentFile extends BaseEntity {
  @Column(name = "biz_type", nullable = false, length = 80) private String bizType;
  @Column(name = "biz_id") private UUID bizId;
  @Column(name = "file_name", nullable = false, length = 240) private String fileName;
  @Column(name = "object_key", nullable = false, length = 500) private String objectKey;
  @Column(name = "content_type", length = 120) private String contentType;
  @Column(name = "size_bytes") private Long sizeBytes;
  public String getBizType() { return bizType; } public void setBizType(String v) { bizType = v; }
  public UUID getBizId() { return bizId; } public void setBizId(UUID v) { bizId = v; }
  public String getFileName() { return fileName; } public void setFileName(String v) { fileName = v; }
  public String getObjectKey() { return objectKey; } public void setObjectKey(String v) { objectKey = v; }
  public String getContentType() { return contentType; } public void setContentType(String v) { contentType = v; }
  public Long getSizeBytes() { return sizeBytes; } public void setSizeBytes(Long v) { sizeBytes = v; }
}
