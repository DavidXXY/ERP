package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "procurement_supplier_quote_attachments")
public class SupplierQuoteAttachment extends BaseEntity {
  @Column(name = "quote_id", nullable = false) private UUID quoteId;
  @Column(name = "supplier_id", nullable = false) private UUID supplierId;
  @Column(name = "account_id", nullable = false) private UUID accountId;
  @Column(name = "attachment_type", nullable = false, length = 40) private String attachmentType;
  @Column(name = "file_name", nullable = false, length = 240) private String fileName;
  @Column(name = "object_key", nullable = false, length = 500) private String objectKey;
  @Column(name = "content_type", length = 160) private String contentType;
  @Column(name = "size_bytes", nullable = false) private long sizeBytes;
  @Column(nullable = false, length = 64) private String sha256;

  public UUID getQuoteId() { return quoteId; }
  public void setQuoteId(UUID value) { quoteId = value; }
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID value) { supplierId = value; }
  public UUID getAccountId() { return accountId; }
  public void setAccountId(UUID value) { accountId = value; }
  public String getAttachmentType() { return attachmentType; }
  public void setAttachmentType(String value) { attachmentType = value; }
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
}
