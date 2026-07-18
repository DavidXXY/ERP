package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "oa_expense_claim_lines")
public class ExpenseClaimLine extends BaseEntity {
  @Column(name = "expense_id", nullable = false) private UUID expenseId;
  @Column(name = "line_no", nullable = false) private Integer lineNo;
  @Enumerated(EnumType.STRING) @Column(name = "expense_type", nullable = false, length = 40) private ExpenseType expenseType;
  @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
  @Column(name = "expense_date", nullable = false) private LocalDate expenseDate;
  @Column(nullable = false, length = 500) private String description;
  @Column(name = "invoice_file_name", length = 240) private String invoiceFileName;
  @Column(name = "invoice_content_type", length = 120) private String invoiceContentType;
  @Column(name = "invoice_size_bytes") private Long invoiceSizeBytes;

  public UUID getExpenseId() { return expenseId; } public void setExpenseId(UUID v) { expenseId = v; }
  public Integer getLineNo() { return lineNo; } public void setLineNo(Integer v) { lineNo = v; }
  public ExpenseType getExpenseType() { return expenseType; } public void setExpenseType(ExpenseType v) { expenseType = v; }
  public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
  public LocalDate getExpenseDate() { return expenseDate; } public void setExpenseDate(LocalDate v) { expenseDate = v; }
  public String getDescription() { return description; } public void setDescription(String v) { description = v; }
  public String getInvoiceFileName() { return invoiceFileName; } public void setInvoiceFileName(String v) { invoiceFileName = v; }
  public String getInvoiceContentType() { return invoiceContentType; } public void setInvoiceContentType(String v) { invoiceContentType = v; }
  public Long getInvoiceSizeBytes() { return invoiceSizeBytes; } public void setInvoiceSizeBytes(Long v) { invoiceSizeBytes = v; }
}
