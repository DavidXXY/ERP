package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "crm_service_contracts")
public class ServiceContract extends BaseEntity {

  @Column(name = "quote_id")
  private UUID quoteId;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "project_name", nullable = false, length = 160)
  private String projectName;

  @Column(name = "contract_type", nullable = false, length = 80)
  private String contractType;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal taxRate = BigDecimal.valueOf(13);

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "service_cycle", length = 120)
  private String serviceCycle;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ContractStatus status = ContractStatus.ACTIVE;

  public UUID getQuoteId() {
    return quoteId;
  }

  public void setQuoteId(UUID quoteId) {
    this.quoteId = quoteId;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getContractType() {
    return contractType;
  }

  public void setContractType(String contractType) {
    this.contractType = contractType;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getTaxRate() {
    return taxRate;
  }

  public void setTaxRate(BigDecimal taxRate) {
    this.taxRate = taxRate;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public String getServiceCycle() {
    return serviceCycle;
  }

  public void setServiceCycle(String serviceCycle) {
    this.serviceCycle = serviceCycle;
  }

  public ContractStatus getStatus() {
    return status;
  }

  public void setStatus(ContractStatus status) {
    this.status = status;
  }
}
