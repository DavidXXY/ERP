package com.company.ops.api.modules.procurement.domain;
import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
@Entity @Table(name="procurement_supplier_quotes")
public class SupplierQuotation extends BaseEntity {
  @Column(name="inquiry_id",nullable=false) private UUID inquiryId;
  @Column(name="supplier_id",nullable=false) private UUID supplierId;
  @Column(name="unit_price",nullable=false) private BigDecimal unitPrice;
  @Column(name="tax_rate",nullable=false) private BigDecimal taxRate;
  @Column(name="delivery_date") private LocalDate deliveryDate;
  @Column(name="payment_terms",length=180) private String paymentTerms;
  @Column(length=500) private String remark;
  @Column(nullable=false) private boolean selected;
  public UUID getInquiryId(){return inquiryId;} public void setInquiryId(UUID v){inquiryId=v;}
  public UUID getSupplierId(){return supplierId;} public void setSupplierId(UUID v){supplierId=v;}
  public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal v){unitPrice=v;}
  public BigDecimal getTaxRate(){return taxRate;} public void setTaxRate(BigDecimal v){taxRate=v;}
  public LocalDate getDeliveryDate(){return deliveryDate;} public void setDeliveryDate(LocalDate v){deliveryDate=v;}
  public String getPaymentTerms(){return paymentTerms;} public void setPaymentTerms(String v){paymentTerms=v;}
  public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
  public boolean isSelected(){return selected;} public void setSelected(boolean v){selected=v;}
}
