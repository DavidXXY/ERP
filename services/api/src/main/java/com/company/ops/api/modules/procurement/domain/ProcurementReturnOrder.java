package com.company.ops.api.modules.procurement.domain;
import com.company.ops.api.common.domain.BaseEntity; import jakarta.persistence.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.OffsetDateTime; import java.util.UUID;
@Entity @Table(name="procurement_return_orders")
public class ProcurementReturnOrder extends BaseEntity {
  @Column(nullable=false,length=64) private String code; @Column(name="order_id",nullable=false) private UUID orderId;
  @Column(name="receipt_id",nullable=false) private UUID receiptId; @Column(name="supplier_id",nullable=false) private UUID supplierId;
  @Column(nullable=false) private BigDecimal quantity; @Column(nullable=false) private BigDecimal amount;
  @Column(nullable=false,length=500) private String reason; @Column(name="return_date",nullable=false) private LocalDate returnDate;
  @Column(name="handler_name",nullable=false,length=80) private String handlerName; @Column(nullable=false,length=32) private String status="COMPLETED";
  @Column(name="replacement_qty",nullable=false) private BigDecimal replacementQty=BigDecimal.ZERO;
  @Column(name="credit_amount",nullable=false) private BigDecimal creditAmount=BigDecimal.ZERO;
  @Column(name="claim_amount",nullable=false) private BigDecimal claimAmount=BigDecimal.ZERO;
  @Column(name="corrective_action",length=1000) private String correctiveAction;
  @Column(name="supplier_response",length=1000) private String supplierResponse;
  @Column(name="completed_at") private OffsetDateTime completedAt;
  public String getCode(){return code;} public void setCode(String v){code=v;} public UUID getOrderId(){return orderId;} public void setOrderId(UUID v){orderId=v;}
  public UUID getReceiptId(){return receiptId;} public void setReceiptId(UUID v){receiptId=v;} public UUID getSupplierId(){return supplierId;} public void setSupplierId(UUID v){supplierId=v;}
  public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
  public String getReason(){return reason;} public void setReason(String v){reason=v;} public LocalDate getReturnDate(){return returnDate;} public void setReturnDate(LocalDate v){returnDate=v;}
  public String getHandlerName(){return handlerName;} public void setHandlerName(String v){handlerName=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public BigDecimal getReplacementQty(){return replacementQty;} public void setReplacementQty(BigDecimal v){replacementQty=v;}
  public BigDecimal getCreditAmount(){return creditAmount;} public void setCreditAmount(BigDecimal v){creditAmount=v;}
  public BigDecimal getClaimAmount(){return claimAmount;} public void setClaimAmount(BigDecimal v){claimAmount=v;}
  public String getCorrectiveAction(){return correctiveAction;} public void setCorrectiveAction(String v){correctiveAction=v;}
  public String getSupplierResponse(){return supplierResponse;} public void setSupplierResponse(String v){supplierResponse=v;}
  public OffsetDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(OffsetDateTime v){completedAt=v;}
}
