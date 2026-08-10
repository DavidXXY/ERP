package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "procurement_framework_agreement_items")
public class FrameworkAgreementItem extends BaseEntity {

  @Column(name = "agreement_id", nullable = false)
  private UUID agreementId;

  @Column(name = "part_id", nullable = false)
  private UUID partId;

  @Column(name = "part_name", nullable = false, length = 160)
  private String partName;

  @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal taxRate = BigDecimal.valueOf(13);

  public UUID getAgreementId() { return agreementId; }
  public void setAgreementId(UUID v) { agreementId = v; }
  public UUID getPartId() { return partId; }
  public void setPartId(UUID v) { partId = v; }
  public String getPartName() { return partName; }
  public void setPartName(String v) { partName = v; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal v) { unitPrice = v; }
  public BigDecimal getTaxRate() { return taxRate; }
  public void setTaxRate(BigDecimal v) { taxRate = v; }
}
