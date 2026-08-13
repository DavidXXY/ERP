package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fin_procurement_payables")
public class ProcurementPayable extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "organization_id")
  private UUID organizationId;

  @Column(name = "receipt_id", nullable = false)
  private UUID receiptId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal taxRate = BigDecimal.valueOf(13);

  @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal paidAmount = BigDecimal.ZERO;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "paid_at")
  private LocalDate paidAt;

  @Column(name = "payment_note", length = 500)
  private String paymentNote;

  @Column(name = "payment_receipt_object_key", length = 255)
  private String paymentReceiptObjectKey;

  @Column(name = "payment_receipt_file_name", length = 255)
  private String paymentReceiptFileName;

  @Column(name = "payment_receipt_content_type", length = 120)
  private String paymentReceiptContentType;

  @Column(name = "payment_receipt_size_bytes")
  private Long paymentReceiptSizeBytes;

  @Column(name = "payment_receipt_uploaded_by", length = 80)
  private String paymentReceiptUploadedBy;

  @Column(name = "payment_receipt_uploaded_at")
  private OffsetDateTime paymentReceiptUploadedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PayableStatus status = PayableStatus.PENDING;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public UUID getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(UUID supplierId) {
    this.supplierId = supplierId;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public void setOrderId(UUID orderId) {
    this.orderId = orderId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(UUID organizationId) {
    this.organizationId = organizationId;
  }

  public UUID getReceiptId() {
    return receiptId;
  }

  public void setReceiptId(UUID receiptId) {
    this.receiptId = receiptId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getTaxRate() {
    return taxRate;
  }

  public void setTaxRate(BigDecimal taxRate) {
    this.taxRate = taxRate;
  }

  public BigDecimal getPaidAmount() {
    return paidAmount;
  }

  public void setPaidAmount(BigDecimal paidAmount) {
    this.paidAmount = paidAmount;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public PayableStatus getStatus() {
    return status;
  }

  public void setStatus(PayableStatus status) {
    this.status = status;
  }

  public LocalDate getPaidAt() { return paidAt; }
  public void setPaidAt(LocalDate paidAt) { this.paidAt = paidAt; }
  public String getPaymentNote() { return paymentNote; }
  public void setPaymentNote(String paymentNote) { this.paymentNote = paymentNote; }
  public String getPaymentReceiptObjectKey() { return paymentReceiptObjectKey; }
  public void setPaymentReceiptObjectKey(String key) { paymentReceiptObjectKey = key; }
  public String getPaymentReceiptFileName() { return paymentReceiptFileName; }
  public void setPaymentReceiptFileName(String name) { paymentReceiptFileName = name; }
  public String getPaymentReceiptContentType() { return paymentReceiptContentType; }
  public void setPaymentReceiptContentType(String type) { paymentReceiptContentType = type; }
  public Long getPaymentReceiptSizeBytes() { return paymentReceiptSizeBytes; }
  public void setPaymentReceiptSizeBytes(Long size) { paymentReceiptSizeBytes = size; }
  public String getPaymentReceiptUploadedBy() { return paymentReceiptUploadedBy; }
  public void setPaymentReceiptUploadedBy(String name) { paymentReceiptUploadedBy = name; }
  public OffsetDateTime getPaymentReceiptUploadedAt() { return paymentReceiptUploadedAt; }
  public void setPaymentReceiptUploadedAt(OffsetDateTime time) { paymentReceiptUploadedAt = time; }
}
