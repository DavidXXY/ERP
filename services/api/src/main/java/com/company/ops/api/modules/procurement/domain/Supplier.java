package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "procurement_suppliers")
public class Supplier extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 80)
  private String category;

  @Column(name = "contact_name", length = 80)
  private String contactName;

  @Column(length = 40)
  private String phone;

  @Column(name = "settlement_terms", length = 160)
  private String settlementTerms;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_status", nullable = false, length = 32)
  private SupplierRiskStatus riskStatus = SupplierRiskStatus.NORMAL;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getSettlementTerms() {
    return settlementTerms;
  }

  public void setSettlementTerms(String settlementTerms) {
    this.settlementTerms = settlementTerms;
  }

  public SupplierRiskStatus getRiskStatus() {
    return riskStatus;
  }

  public void setRiskStatus(SupplierRiskStatus riskStatus) {
    this.riskStatus = riskStatus;
  }
}
