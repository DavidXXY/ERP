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
@Table(name = "procurement_goods_receipts")
public class GoodsReceipt extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "part_id", nullable = false)
  private UUID partId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal quantity;

  @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal taxRate = BigDecimal.valueOf(13);

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "received_date", nullable = false)
  private LocalDate receivedDate;

  @Column(name = "delivery_no", nullable = false, length = 80)
  private String deliveryNo;

  @Column(name = "receiver_name", nullable = false, length = 80)
  private String receiverName;
  @Column(name="inspection_status",nullable=false,length=32) private String inspectionStatus="PENDING";
  @Column(name="qualified_qty") private BigDecimal qualifiedQty;
  @Column(name="rejected_qty",nullable=false) private BigDecimal rejectedQty=BigDecimal.ZERO;
  @Column(name="inspector_name",length=80) private String inspectorName;
  @Column(name="inspection_comment",length=500) private String inspectionComment;
  @Column(name="inspected_at") private OffsetDateTime inspectedAt;
  @Column(name="payable_due_date") private LocalDate payableDueDate;
  @Column(name="client_request_id",length=80) private String clientRequestId;
  @Column(name="asn_no",length=80) private String asnNo;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public void setOrderId(UUID orderId) {
    this.orderId = orderId;
  }

  public UUID getPartId() {
    return partId;
  }

  public void setPartId(UUID partId) {
    this.partId = partId;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getTaxRate() {
    return taxRate;
  }

  public void setTaxRate(BigDecimal taxRate) {
    this.taxRate = taxRate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDate getReceivedDate() {
    return receivedDate;
  }

  public void setReceivedDate(LocalDate receivedDate) {
    this.receivedDate = receivedDate;
  }

  public String getDeliveryNo() {
    return deliveryNo;
  }

  public void setDeliveryNo(String deliveryNo) {
    this.deliveryNo = deliveryNo;
  }

  public String getReceiverName() {
    return receiverName;
  }

  public void setReceiverName(String receiverName) {
    this.receiverName = receiverName;
  }
  public String getInspectionStatus(){return inspectionStatus;} public void setInspectionStatus(String v){inspectionStatus=v;}
  public BigDecimal getQualifiedQty(){return qualifiedQty;} public void setQualifiedQty(BigDecimal v){qualifiedQty=v;}
  public BigDecimal getRejectedQty(){return rejectedQty;} public void setRejectedQty(BigDecimal v){rejectedQty=v;}
  public String getInspectorName(){return inspectorName;} public void setInspectorName(String v){inspectorName=v;}
  public String getInspectionComment(){return inspectionComment;} public void setInspectionComment(String v){inspectionComment=v;}
  public OffsetDateTime getInspectedAt(){return inspectedAt;} public void setInspectedAt(OffsetDateTime v){inspectedAt=v;}
  public LocalDate getPayableDueDate(){return payableDueDate;} public void setPayableDueDate(LocalDate v){payableDueDate=v;}
  public String getClientRequestId(){return clientRequestId;} public void setClientRequestId(String v){clientRequestId=v;}
  public String getAsnNo(){return asnNo;} public void setAsnNo(String v){asnNo=v;}
}
