package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_supplier_portal_documents")
public class SupplierPortalDocument extends BaseEntity {
  @Column(name = "supplier_id", nullable = false) private UUID supplierId;
  @Column(name = "account_id", nullable = false) private UUID accountId;
  @Column(name = "document_type", nullable = false, length = 40) private String documentType;
  @Column(name = "document_name", nullable = false, length = 240) private String documentName;
  @Column(name = "object_key", nullable = false, length = 500) private String objectKey;
  @Column(name = "content_type", length = 160) private String contentType;
  @Column(name = "size_bytes", nullable = false) private long sizeBytes;
  @Column(name = "valid_to") private LocalDate validTo;
  @Column(name = "review_status", nullable = false, length = 32) private String reviewStatus = "PENDING";
  @Column(name = "review_comment", length = 500) private String reviewComment;
  @Column(name = "reviewed_by_name", length = 80) private String reviewedByName;
  @Column(name = "reviewed_at") private OffsetDateTime reviewedAt;

  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
  public UUID getAccountId() { return accountId; }
  public void setAccountId(UUID accountId) { this.accountId = accountId; }
  public String getDocumentType() { return documentType; }
  public void setDocumentType(String documentType) { this.documentType = documentType; }
  public String getDocumentName() { return documentName; }
  public void setDocumentName(String documentName) { this.documentName = documentName; }
  public String getObjectKey() { return objectKey; }
  public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
  public LocalDate getValidTo() { return validTo; }
  public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
  public String getReviewStatus() { return reviewStatus; }
  public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
  public String getReviewComment() { return reviewComment; }
  public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
  public String getReviewedByName() { return reviewedByName; }
  public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }
  public OffsetDateTime getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
