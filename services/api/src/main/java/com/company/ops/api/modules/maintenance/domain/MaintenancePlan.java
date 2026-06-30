package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "maintenance_plans")
public class MaintenancePlan extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "asset_id", nullable = false)
  private UUID assetId;

  @Column(name = "contract_id")
  private UUID contractId;

  @Column(name = "plan_name", nullable = false, length = 180)
  private String planName;

  @Column(name = "cycle_days", nullable = false)
  private Integer cycleDays;

  @Column(name = "next_due_date", nullable = false)
  private LocalDate nextDueDate;

  @Column(name = "last_generated_date")
  private LocalDate lastGeneratedDate;

  @Column(nullable = false)
  private boolean active = true;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public UUID getAssetId() { return assetId; }
  public void setAssetId(UUID assetId) { this.assetId = assetId; }
  public UUID getContractId() { return contractId; }
  public void setContractId(UUID contractId) { this.contractId = contractId; }
  public String getPlanName() { return planName; }
  public void setPlanName(String planName) { this.planName = planName; }
  public Integer getCycleDays() { return cycleDays; }
  public void setCycleDays(Integer cycleDays) { this.cycleDays = cycleDays; }
  public LocalDate getNextDueDate() { return nextDueDate; }
  public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }
  public LocalDate getLastGeneratedDate() { return lastGeneratedDate; }
  public void setLastGeneratedDate(LocalDate lastGeneratedDate) { this.lastGeneratedDate = lastGeneratedDate; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
}
