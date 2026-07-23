package com.company.ops.api.modules.procurement.domain;
import com.company.ops.api.common.domain.BaseEntity; import jakarta.persistence.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
@Entity @Table(name="procurement_supplier_invoices")
public class SupplierInvoice extends BaseEntity {
  @Column(nullable=false,length=64) private String code; @Column(name="invoice_no",nullable=false,length=100) private String invoiceNo;
  @Column(name="order_id",nullable=false) private UUID orderId; @Column(name="supplier_id",nullable=false) private UUID supplierId;
  @Column(nullable=false) private BigDecimal amount; @Column(name="tax_rate",nullable=false) private BigDecimal taxRate;
  @Column(name="invoice_date",nullable=false) private LocalDate invoiceDate; @Column(nullable=false,length=32) private String status="REGISTERED";
  @Column(name="match_status",nullable=false,length=32) private String matchStatus; @Column(name="difference_amount",nullable=false) private BigDecimal differenceAmount;
  @Column(length=500) private String remark;
  public String getCode(){return code;} public void setCode(String v){code=v;} public String getInvoiceNo(){return invoiceNo;} public void setInvoiceNo(String v){invoiceNo=v;}
  public UUID getOrderId(){return orderId;} public void setOrderId(UUID v){orderId=v;} public UUID getSupplierId(){return supplierId;} public void setSupplierId(UUID v){supplierId=v;}
  public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public BigDecimal getTaxRate(){return taxRate;} public void setTaxRate(BigDecimal v){taxRate=v;}
  public LocalDate getInvoiceDate(){return invoiceDate;} public void setInvoiceDate(LocalDate v){invoiceDate=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public String getMatchStatus(){return matchStatus;} public void setMatchStatus(String v){matchStatus=v;} public BigDecimal getDifferenceAmount(){return differenceAmount;} public void setDifferenceAmount(BigDecimal v){differenceAmount=v;}
  public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
}
