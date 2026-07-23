package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import com.company.ops.api.common.security.EncryptedStringConverter;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

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

  @Column(name = "legal_representative", length = 80)
  private String legalRepresentative;

  @Column(name = "unified_social_credit_code", length = 80)
  private String unifiedSocialCreditCode;

  @Column(name = "registered_capital", length = 80)
  private String registeredCapital;

  @Column(name = "registered_address", length = 240)
  private String registeredAddress;

  @Column(name = "business_scope", length = 800)
  private String businessScope;

  @Column(name = "license_valid_to")
  private LocalDate licenseValidTo;

  @Column(name = "qualification_valid_to")
  private LocalDate qualificationValidTo;

  @Column(name = "taxpayer_type", length = 80)
  private String taxpayerType;

  @Column(name = "bank_name", length = 120)
  private String bankName;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "bank_account", length = 512)
  private String bankAccount;

  @Column(name = "admission_status", length = 40)
  private String admissionStatus;

  @Column(name = "admission_submitted_at")
  private OffsetDateTime admissionSubmittedAt;

  @Column(name = "admission_reviewed_at")
  private OffsetDateTime admissionReviewedAt;

  @Column(name = "admission_reviewer_name", length = 80)
  private String admissionReviewerName;

  @Column(name = "admission_review_comment", length = 500)
  private String admissionReviewComment;

  @Column(name = "remark", length = 1000)
  private String remark;

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

  public String getLegalRepresentative() { return legalRepresentative; }

  public void setLegalRepresentative(String legalRepresentative) { this.legalRepresentative = legalRepresentative; }

  public String getUnifiedSocialCreditCode() { return unifiedSocialCreditCode; }

  public void setUnifiedSocialCreditCode(String unifiedSocialCreditCode) { this.unifiedSocialCreditCode = unifiedSocialCreditCode; }

  public String getRegisteredCapital() { return registeredCapital; }

  public void setRegisteredCapital(String registeredCapital) { this.registeredCapital = registeredCapital; }

  public String getRegisteredAddress() { return registeredAddress; }

  public void setRegisteredAddress(String registeredAddress) { this.registeredAddress = registeredAddress; }

  public String getBusinessScope() { return businessScope; }

  public void setBusinessScope(String businessScope) { this.businessScope = businessScope; }

  public LocalDate getLicenseValidTo() { return licenseValidTo; }

  public void setLicenseValidTo(LocalDate licenseValidTo) { this.licenseValidTo = licenseValidTo; }

  public LocalDate getQualificationValidTo() { return qualificationValidTo; }

  public void setQualificationValidTo(LocalDate qualificationValidTo) { this.qualificationValidTo = qualificationValidTo; }

  public String getTaxpayerType() { return taxpayerType; }

  public void setTaxpayerType(String taxpayerType) { this.taxpayerType = taxpayerType; }

  public String getBankName() { return bankName; }

  public void setBankName(String bankName) { this.bankName = bankName; }

  public String getBankAccount() { return bankAccount; }

  public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

  public String getAdmissionStatus() { return admissionStatus; }

  public void setAdmissionStatus(String admissionStatus) { this.admissionStatus = admissionStatus; }

  public OffsetDateTime getAdmissionSubmittedAt() { return admissionSubmittedAt; }

  public void setAdmissionSubmittedAt(OffsetDateTime admissionSubmittedAt) {
    this.admissionSubmittedAt = admissionSubmittedAt;
  }

  public OffsetDateTime getAdmissionReviewedAt() { return admissionReviewedAt; }

  public void setAdmissionReviewedAt(OffsetDateTime admissionReviewedAt) {
    this.admissionReviewedAt = admissionReviewedAt;
  }

  public String getAdmissionReviewerName() { return admissionReviewerName; }

  public void setAdmissionReviewerName(String admissionReviewerName) {
    this.admissionReviewerName = admissionReviewerName;
  }

  public String getAdmissionReviewComment() { return admissionReviewComment; }

  public void setAdmissionReviewComment(String admissionReviewComment) {
    this.admissionReviewComment = admissionReviewComment;
  }

  public String getRemark() { return remark; }

  public void setRemark(String remark) { this.remark = remark; }

  public SupplierRiskStatus getRiskStatus() {
    return riskStatus;
  }

  public void setRiskStatus(SupplierRiskStatus riskStatus) {
    this.riskStatus = riskStatus;
  }
}
