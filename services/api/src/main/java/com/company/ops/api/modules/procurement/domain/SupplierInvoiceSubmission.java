package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "supplier_invoice_submissions")
public class SupplierInvoiceSubmission extends BaseEntity {
  @Column(name = "supplier_id", nullable = false) private UUID supplierId;
  @Column(name = "account_id", nullable = false) private UUID accountId;
  @Column(name = "order_id", nullable = false) private UUID orderId;
  @Column(name = "invoice_no", nullable = false, length = 100) private String invoiceNo;
  @Column(nullable = false) private BigDecimal amount;
  @Column(name = "tax_rate", nullable = false) private BigDecimal taxRate;
  @Column(name = "invoice_date", nullable = false) private LocalDate invoiceDate;
  @Column(length = 500) private String remark;
  @Column(name = "file_name", nullable = false, length = 255) private String fileName;
  @Column(name = "object_key", nullable = false, length = 500) private String objectKey;
  @Column(name = "content_type", length = 120) private String contentType;
  @Column(name = "size_bytes", nullable = false) private long sizeBytes;
  @Column(nullable = false, length = 64) private String sha256;
  @Column(nullable = false, length = 32) private String status = "PENDING";
  @Column(name = "review_comment", length = 500) private String reviewComment;
  @Column(name = "reviewed_by", length = 64) private String reviewedBy;
  @Column(name = "reviewed_at") private OffsetDateTime reviewedAt;

  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID v) { supplierId = v; }
  public UUID getAccountId() { return accountId; }
  public void setAccountId(UUID v) { accountId = v; }
  public UUID getOrderId() { return orderId; }
  public void setOrderId(UUID v) { orderId = v; }
  public String getInvoiceNo() { return invoiceNo; }
  public void setInvoiceNo(String v) { invoiceNo = v; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal v) { amount = v; }
  public BigDecimal getTaxRate() { return taxRate; }
  public void setTaxRate(BigDecimal v) { taxRate = v; }
  public LocalDate getInvoiceDate() { return invoiceDate; }
  public void setInvoiceDate(LocalDate v) { invoiceDate = v; }
  public String getRemark() { return remark; }
  public void setRemark(String v) { remark = v; }
  public String getFileName() { return fileName; }
  public void setFileName(String v) { fileName = v; }
  public String getObjectKey() { return objectKey; }
  public void setObjectKey(String v) { objectKey = v; }
  public String getContentType() { return contentType; }
  public void setContentType(String v) { contentType = v; }
  public long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(long v) { sizeBytes = v; }
  public String getSha256() { return sha256; }
  public void setSha256(String v) { sha256 = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { status = v; }
  public String getReviewComment() { return reviewComment; }
  public void setReviewComment(String v) { reviewComment = v; }
  public String getReviewedBy() { return reviewedBy; }
  public void setReviewedBy(String v) { reviewedBy = v; }
  public OffsetDateTime getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(OffsetDateTime v) { reviewedAt = v; }
}
