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

@Entity @Table(name = "oa_outsource_orders")
public class OutsourceOrder extends BaseEntity {
  @Column(nullable = false, length = 64) private String code;
  @Column(name = "supplier_id", nullable = false) private UUID supplierId;
  @Column(name = "project_id") private UUID projectId;
  @Column(name = "work_order_id") private UUID workOrderId;
  @Column(name = "service_type", nullable = false, length = 100) private String serviceType;
  @Column(nullable = false, length = 800) private String description;
  @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
  @Column(name = "planned_date", nullable = false) private LocalDate plannedDate;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private OutsourceStatus status;
  @Column(name = "approval_request_id") private UUID approvalRequestId;
  @Column(name = "acceptance_note", length = 500) private String acceptanceNote;
  public String getCode() { return code; } public void setCode(String v) { code = v; }
  public UUID getSupplierId() { return supplierId; } public void setSupplierId(UUID v) { supplierId = v; }
  public UUID getProjectId() { return projectId; } public void setProjectId(UUID v) { projectId = v; }
  public UUID getWorkOrderId() { return workOrderId; } public void setWorkOrderId(UUID v) { workOrderId = v; }
  public String getServiceType() { return serviceType; } public void setServiceType(String v) { serviceType = v; }
  public String getDescription() { return description; } public void setDescription(String v) { description = v; }
  public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
  public LocalDate getPlannedDate() { return plannedDate; } public void setPlannedDate(LocalDate v) { plannedDate = v; }
  public OutsourceStatus getStatus() { return status; } public void setStatus(OutsourceStatus v) { status = v; }
  public UUID getApprovalRequestId() { return approvalRequestId; } public void setApprovalRequestId(UUID v) { approvalRequestId = v; }
  public String getAcceptanceNote() { return acceptanceNote; } public void setAcceptanceNote(String v) { acceptanceNote = v; }
}
