package com.company.ops.api.modules.fixedasset.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fa_depreciation_runs", uniqueConstraints = @UniqueConstraint(
    name = "uk_fa_depreciation_runs_tenant_period", columnNames = {"tenant_id", "period"}))
public class FixedAssetDepreciationRun extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 7)
  private String period;

  @Column(name = "run_date", nullable = false)
  private LocalDate runDate;

  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(nullable = false, length = 16)
  private String status = "POSTED";

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public String getPeriod() { return period; }
  public void setPeriod(String period) { this.period = period; }

  public LocalDate getRunDate() { return runDate; }
  public void setRunDate(LocalDate runDate) { this.runDate = runDate; }

  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
