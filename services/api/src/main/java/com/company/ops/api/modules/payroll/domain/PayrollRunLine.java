package com.company.ops.api.modules.payroll.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "payroll_run_lines")
public class PayrollRunLine {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false, length = 64)
  @TenantId
  private String tenantId;

  @Column(name = "run_id", nullable = false)
  private UUID runId;

  @Column(name = "employee_id", nullable = false)
  private UUID employeeId;

  @Column(name = "employee_name", length = 120)
  private String employeeName;

  @Column(name = "base_salary", nullable = false, precision = 18, scale = 2)
  private BigDecimal baseSalary = BigDecimal.ZERO;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal allowances = BigDecimal.ZERO;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal bonus = BigDecimal.ZERO;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal deductions = BigDecimal.ZERO;

  @Column(name = "social_insurance", nullable = false, precision = 18, scale = 2)
  private BigDecimal socialInsurance = BigDecimal.ZERO;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal tax = BigDecimal.ZERO;

  @Column(name = "net_pay", nullable = false, precision = 18, scale = 2)
  private BigDecimal netPay = BigDecimal.ZERO;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @PrePersist
  protected void prePersist() {
    createdAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public UUID getRunId() {
    return runId;
  }

  public void setRunId(UUID runId) {
    this.runId = runId;
  }

  public UUID getEmployeeId() {
    return employeeId;
  }

  public void setEmployeeId(UUID employeeId) {
    this.employeeId = employeeId;
  }

  public String getEmployeeName() {
    return employeeName;
  }

  public void setEmployeeName(String employeeName) {
    this.employeeName = employeeName;
  }

  public BigDecimal getBaseSalary() {
    return baseSalary;
  }

  public void setBaseSalary(BigDecimal baseSalary) {
    this.baseSalary = baseSalary;
  }

  public BigDecimal getAllowances() {
    return allowances;
  }

  public void setAllowances(BigDecimal allowances) {
    this.allowances = allowances;
  }

  public BigDecimal getBonus() {
    return bonus;
  }

  public void setBonus(BigDecimal bonus) {
    this.bonus = bonus;
  }

  public BigDecimal getDeductions() {
    return deductions;
  }

  public void setDeductions(BigDecimal deductions) {
    this.deductions = deductions;
  }

  public BigDecimal getSocialInsurance() {
    return socialInsurance;
  }

  public void setSocialInsurance(BigDecimal socialInsurance) {
    this.socialInsurance = socialInsurance;
  }

  public BigDecimal getTax() {
    return tax;
  }

  public void setTax(BigDecimal tax) {
    this.tax = tax;
  }

  public BigDecimal getNetPay() {
    return netPay;
  }

  public void setNetPay(BigDecimal netPay) {
    this.netPay = netPay;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }
}
