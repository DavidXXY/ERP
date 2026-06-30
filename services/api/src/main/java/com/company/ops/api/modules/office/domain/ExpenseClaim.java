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

@Entity @Table(name = "oa_expense_claims")
public class ExpenseClaim extends BaseEntity {
  @Column(nullable = false, length = 64) private String code;
  @Column(name = "claimant_id") private UUID claimantId;
  @Column(name = "claimant_name", nullable = false, length = 80) private String claimantName;
  @Column(name = "project_id") private UUID projectId;
  @Column(name = "work_order_id") private UUID workOrderId;
  @Enumerated(EnumType.STRING) @Column(name = "expense_type", nullable = false, length = 40) private ExpenseType expenseType;
  @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
  @Column(name = "expense_date", nullable = false) private LocalDate expenseDate;
  @Column(nullable = false, length = 500) private String description;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ExpenseStatus status;
  @Column(name = "approval_request_id") private UUID approvalRequestId;
  public String getCode() { return code; } public void setCode(String v) { code = v; }
  public UUID getClaimantId() { return claimantId; } public void setClaimantId(UUID v) { claimantId = v; }
  public String getClaimantName() { return claimantName; } public void setClaimantName(String v) { claimantName = v; }
  public UUID getProjectId() { return projectId; } public void setProjectId(UUID v) { projectId = v; }
  public UUID getWorkOrderId() { return workOrderId; } public void setWorkOrderId(UUID v) { workOrderId = v; }
  public ExpenseType getExpenseType() { return expenseType; } public void setExpenseType(ExpenseType v) { expenseType = v; }
  public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
  public LocalDate getExpenseDate() { return expenseDate; } public void setExpenseDate(LocalDate v) { expenseDate = v; }
  public String getDescription() { return description; } public void setDescription(String v) { description = v; }
  public ExpenseStatus getStatus() { return status; } public void setStatus(ExpenseStatus v) { status = v; }
  public UUID getApprovalRequestId() { return approvalRequestId; } public void setApprovalRequestId(UUID v) { approvalRequestId = v; }
}
