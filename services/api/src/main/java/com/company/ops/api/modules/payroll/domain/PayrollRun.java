package com.company.ops.api.modules.payroll.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payroll_runs", uniqueConstraints = @UniqueConstraint(
    name = "uk_payroll_runs_tenant_period", columnNames = {"tenant_id", "period"}))
public class PayrollRun extends BaseEntity {

  @Column(nullable = false, length = 80)
  private String code;

  @Column(nullable = false, length = 7)
  private String period;

  @Column(name = "run_date", nullable = false)
  private LocalDate runDate;

  @Column(name = "total_gross", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalGross = BigDecimal.ZERO;

  @Column(name = "total_social", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalSocial = BigDecimal.ZERO;

  @Column(name = "total_tax", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalTax = BigDecimal.ZERO;

  @Column(name = "total_net", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalNet = BigDecimal.ZERO;

  @Column(nullable = false, length = 16)
  private String status = "DRAFT";

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public LocalDate getRunDate() {
    return runDate;
  }

  public void setRunDate(LocalDate runDate) {
    this.runDate = runDate;
  }

  public BigDecimal getTotalGross() {
    return totalGross;
  }

  public void setTotalGross(BigDecimal totalGross) {
    this.totalGross = totalGross;
  }

  public BigDecimal getTotalSocial() {
    return totalSocial;
  }

  public void setTotalSocial(BigDecimal totalSocial) {
    this.totalSocial = totalSocial;
  }

  public BigDecimal getTotalTax() {
    return totalTax;
  }

  public void setTotalTax(BigDecimal totalTax) {
    this.totalTax = totalTax;
  }

  public BigDecimal getTotalNet() {
    return totalNet;
  }

  public void setTotalNet(BigDecimal totalNet) {
    this.totalNet = totalNet;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
