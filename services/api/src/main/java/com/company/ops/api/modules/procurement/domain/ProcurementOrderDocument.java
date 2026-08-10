package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_order_documents")
public class ProcurementOrderDocument extends BaseEntity {
  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "file_name", nullable = false, length = 240)
  private String fileName;

  @Column(name = "object_key", nullable = false, length = 500)
  private String objectKey;

  @Column(name = "content_type", length = 160)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 64)
  private String sha256;

  @Column(name = "uploaded_by", length = 80)
  private String uploadedBy;

  @Column(name = "uploaded_at")
  private OffsetDateTime uploadedAt;

  @Column(name = "doc_type", nullable = false, length = 20)
  private String docType = "OTHER";

  public UUID getOrderId() { return orderId; }
  public void setOrderId(UUID value) { orderId = value; }
  public String getFileName() { return fileName; }
  public void setFileName(String value) { fileName = value; }
  public String getObjectKey() { return objectKey; }
  public void setObjectKey(String value) { objectKey = value; }
  public String getContentType() { return contentType; }
  public void setContentType(String value) { contentType = value; }
  public long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(long value) { sizeBytes = value; }
  public String getSha256() { return sha256; }
  public void setSha256(String value) { sha256 = value; }
  public String getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(String value) { uploadedBy = value; }
  public OffsetDateTime getUploadedAt() { return uploadedAt; }
  public void setUploadedAt(OffsetDateTime value) { uploadedAt = value; }
  public String getDocType() { return docType; }
  public void setDocType(String value) { docType = value; }
}
