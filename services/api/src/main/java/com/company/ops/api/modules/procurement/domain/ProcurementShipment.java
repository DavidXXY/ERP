package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_shipments")
public class ProcurementShipment extends BaseEntity {

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Column(name = "delivery_no", length = 80)
  private String deliveryNo;

  @Column(length = 80)
  private String carrier;

  @Column(name = "expected_arrival")
  private LocalDate expectedArrival;

  @Column(length = 500)
  private String remark;

  @Column(nullable = false, length = 32)
  private String status = "PENDING";

  @Column(name = "review_comment", length = 500)
  private String reviewComment;

  @Column(name = "reviewed_by", length = 64)
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private OffsetDateTime reviewedAt;

  public UUID getOrderId() { return orderId; } public void setOrderId(UUID v) { orderId = v; }
  public UUID getSupplierId() { return supplierId; } public void setSupplierId(UUID v) { supplierId = v; }
  public String getDeliveryNo() { return deliveryNo; } public void setDeliveryNo(String v) { deliveryNo = v; }
  public String getCarrier() { return carrier; } public void setCarrier(String v) { carrier = v; }
  public LocalDate getExpectedArrival() { return expectedArrival; } public void setExpectedArrival(LocalDate v) { expectedArrival = v; }
  public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
  public String getStatus() { return status; } public void setStatus(String v) { status = v; }
  public String getReviewComment() { return reviewComment; } public void setReviewComment(String v) { reviewComment = v; }
  public String getReviewedBy() { return reviewedBy; } public void setReviewedBy(String v) { reviewedBy = v; }
  public OffsetDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(OffsetDateTime v) { reviewedAt = v; }
}
