package com.company.ops.api.modules.fixedasset.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fa_assets", uniqueConstraints = @UniqueConstraint(
    name = "uk_fa_assets_tenant_code", columnNames = {"tenant_id", "code"}))
public class FixedAsset extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 64)
  private String category;

  @Column(name = "acquisition_date")
  private LocalDate acquisitionDate;

  @Column(name = "original_value", nullable = false, precision = 18, scale = 2)
  private BigDecimal originalValue = BigDecimal.ZERO;

  @Column(name = "residual_value", nullable = false, precision = 18, scale = 2)
  private BigDecimal residualValue = BigDecimal.ZERO;

  @Column(name = "useful_life_months", nullable = false)
  private int usefulLifeMonths;

  @Column(name = "depreciation_method", nullable = false, length = 32)
  private String depreciationMethod = "STRAIGHT_LINE";

  @Column(name = "monthly_depreciation", nullable = false, precision = 18, scale = 2)
  private BigDecimal monthlyDepreciation = BigDecimal.ZERO;

  @Column(name = "accumulated_depreciation", nullable = false, precision = 18, scale = 2)
  private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

  @Column(name = "last_depreciated_period", length = 7)
  private String lastDepreciatedPeriod;

  @Column(nullable = false, length = 24)
  private String status = "IN_USE";

  @Column(length = 120)
  private String location;

  @Column(length = 80)
  private String custodian;

  @Column(name = "custodian_user_id")
  private UUID custodianUserId;

  @Column(name = "organization_id")
  private UUID organizationId;

  @Column(length = 500)
  private String remark;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }

  public LocalDate getAcquisitionDate() { return acquisitionDate; }
  public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

  public BigDecimal getOriginalValue() { return originalValue; }
  public void setOriginalValue(BigDecimal originalValue) { this.originalValue = originalValue; }

  public BigDecimal getResidualValue() { return residualValue; }
  public void setResidualValue(BigDecimal residualValue) { this.residualValue = residualValue; }

  public int getUsefulLifeMonths() { return usefulLifeMonths; }
  public void setUsefulLifeMonths(int usefulLifeMonths) { this.usefulLifeMonths = usefulLifeMonths; }

  public String getDepreciationMethod() { return depreciationMethod; }
  public void setDepreciationMethod(String depreciationMethod) { this.depreciationMethod = depreciationMethod; }

  public BigDecimal getMonthlyDepreciation() { return monthlyDepreciation; }
  public void setMonthlyDepreciation(BigDecimal monthlyDepreciation) { this.monthlyDepreciation = monthlyDepreciation; }

  public BigDecimal getAccumulatedDepreciation() { return accumulatedDepreciation; }
  public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciation) { this.accumulatedDepreciation = accumulatedDepreciation; }

  public String getLastDepreciatedPeriod() { return lastDepreciatedPeriod; }
  public void setLastDepreciatedPeriod(String lastDepreciatedPeriod) { this.lastDepreciatedPeriod = lastDepreciatedPeriod; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public String getLocation() { return location; }
  public void setLocation(String location) { this.location = location; }

  public String getCustodian() { return custodian; }
  public void setCustodian(String custodian) { this.custodian = custodian; }

  public UUID getCustodianUserId() { return custodianUserId; }
  public void setCustodianUserId(UUID custodianUserId) { this.custodianUserId = custodianUserId; }

  public UUID getOrganizationId() { return organizationId; }
  public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
