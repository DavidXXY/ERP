package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "maintenance_plans")
public class MaintenancePlan extends BaseEntity {
  @Column(nullable = false, length = 64) private String code;
  @Column(name = "asset_id", nullable = false) private UUID assetId;
  @Column(name = "contract_id") private UUID contractId;
  @Column(name = "plan_name", nullable = false, length = 180) private String planName;
  @Column(length = 1000) private String description;
  @Enumerated(EnumType.STRING) @Column(name = "work_type", nullable = false, length = 80)
  private WorkOrderType workType = WorkOrderType.INSPECTION;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
  private WorkOrderPriority priority = WorkOrderPriority.NORMAL;
  @Column(name = "cycle_days", nullable = false) private Integer cycleDays;
  @Column(name = "next_due_date", nullable = false) private LocalDate nextDueDate;
  @Column(name = "last_generated_date") private LocalDate lastGeneratedDate;
  @Column(name = "auto_generate", nullable = false) private boolean autoGenerate = true;
  @Column(name = "active", nullable = false) private boolean enabled = true;

  public String getCode() { return code; }
  public void setCode(String value) { code = value; }
  public UUID getAssetId() { return assetId; }
  public void setAssetId(UUID value) { assetId = value; }
  public UUID getContractId() { return contractId; }
  public void setContractId(UUID value) { contractId = value; }
  public String getPlanName() { return planName; }
  public void setPlanName(String value) { planName = value; }
  public String getDescription() { return description; }
  public void setDescription(String value) { description = value; }
  public WorkOrderType getWorkType() { return workType; }
  public void setWorkType(WorkOrderType value) { workType = value; }
  public WorkOrderPriority getPriority() { return priority; }
  public void setPriority(WorkOrderPriority value) { priority = value; }
  public Integer getCycleDays() { return cycleDays; }
  public void setCycleDays(Integer value) { cycleDays = value; }
  public LocalDate getNextDueDate() { return nextDueDate; }
  public void setNextDueDate(LocalDate value) { nextDueDate = value; }
  public LocalDate getLastGeneratedDate() { return lastGeneratedDate; }
  public void setLastGeneratedDate(LocalDate value) { lastGeneratedDate = value; }
  public boolean isAutoGenerate() { return autoGenerate; }
  public void setAutoGenerate(boolean value) { autoGenerate = value; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean value) { enabled = value; }
}
