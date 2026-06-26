package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "crm_customers")
public class Customer extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false, length = 80)
  private String industry;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private CustomerLevel level = CustomerLevel.NORMAL;

  @Column(name = "owner_name", nullable = false, length = 80)
  private String ownerName;

  @Column(name = "payment_habit", length = 200)
  private String paymentHabit;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_status", nullable = false, length = 32)
  private RiskStatus riskStatus = RiskStatus.NORMAL;

  @Column(name = "risk_note", length = 500)
  private String riskNote;

  @Column(name = "invoice_title", length = 180)
  private String invoiceTitle;

  @Column(name = "tax_no", length = 64)
  private String taxNo;

  @Column(name = "bank_name", length = 160)
  private String bankName;

  @Column(name = "bank_account", length = 80)
  private String bankAccount;

  @Column(name = "registered_address", length = 240)
  private String registeredAddress;

  @Column(name = "registered_phone", length = 40)
  private String registeredPhone;

  @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CustomerContact> contacts = new ArrayList<>();

  @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CustomerSite> sites = new ArrayList<>();

  public void addContact(CustomerContact contact) {
    contact.setCustomer(this);
    contacts.add(contact);
  }

  public void addSite(CustomerSite site) {
    site.setCustomer(this);
    sites.add(site);
  }

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

  public String getIndustry() {
    return industry;
  }

  public void setIndustry(String industry) {
    this.industry = industry;
  }

  public CustomerLevel getLevel() {
    return level;
  }

  public void setLevel(CustomerLevel level) {
    this.level = level;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public String getPaymentHabit() {
    return paymentHabit;
  }

  public void setPaymentHabit(String paymentHabit) {
    this.paymentHabit = paymentHabit;
  }

  public RiskStatus getRiskStatus() {
    return riskStatus;
  }

  public void setRiskStatus(RiskStatus riskStatus) {
    this.riskStatus = riskStatus;
  }

  public String getRiskNote() {
    return riskNote;
  }

  public void setRiskNote(String riskNote) {
    this.riskNote = riskNote;
  }

  public String getInvoiceTitle() {
    return invoiceTitle;
  }

  public void setInvoiceTitle(String invoiceTitle) {
    this.invoiceTitle = invoiceTitle;
  }

  public String getTaxNo() {
    return taxNo;
  }

  public void setTaxNo(String taxNo) {
    this.taxNo = taxNo;
  }

  public String getBankName() {
    return bankName;
  }

  public void setBankName(String bankName) {
    this.bankName = bankName;
  }

  public String getBankAccount() {
    return bankAccount;
  }

  public void setBankAccount(String bankAccount) {
    this.bankAccount = bankAccount;
  }

  public String getRegisteredAddress() {
    return registeredAddress;
  }

  public void setRegisteredAddress(String registeredAddress) {
    this.registeredAddress = registeredAddress;
  }

  public String getRegisteredPhone() {
    return registeredPhone;
  }

  public void setRegisteredPhone(String registeredPhone) {
    this.registeredPhone = registeredPhone;
  }

  public List<CustomerContact> getContacts() {
    return contacts;
  }

  public List<CustomerSite> getSites() {
    return sites;
  }
}

