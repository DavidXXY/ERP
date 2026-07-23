package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "procurement_collaboration_events")
public class ProcurementCollaborationEvent extends BaseEntity {
  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;
  @Column(name = "order_id")
  private UUID orderId;
  @Column(name = "event_type", nullable = false, length = 40)
  private String eventType;
  @Column(name = "reference_no", length = 100)
  private String referenceNo;
  @Column(name = "event_date", nullable = false)
  private LocalDate eventDate;
  @Column(name = "promised_date")
  private LocalDate promisedDate;
  @Column(precision = 14, scale = 2)
  private BigDecimal quantity;
  @Column(nullable = false, length = 32)
  private String status = "OPEN";
  @Column(length = 1000)
  private String content;
  @Column(name = "created_by_name", nullable = false, length = 80)
  private String createdByName;

  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID value) { supplierId = value; }
  public UUID getOrderId() { return orderId; }
  public void setOrderId(UUID value) { orderId = value; }
  public String getEventType() { return eventType; }
  public void setEventType(String value) { eventType = value; }
  public String getReferenceNo() { return referenceNo; }
  public void setReferenceNo(String value) { referenceNo = value; }
  public LocalDate getEventDate() { return eventDate; }
  public void setEventDate(LocalDate value) { eventDate = value; }
  public LocalDate getPromisedDate() { return promisedDate; }
  public void setPromisedDate(LocalDate value) { promisedDate = value; }
  public BigDecimal getQuantity() { return quantity; }
  public void setQuantity(BigDecimal value) { quantity = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public String getContent() { return content; }
  public void setContent(String value) { content = value; }
  public String getCreatedByName() { return createdByName; }
  public void setCreatedByName(String value) { createdByName = value; }
}
