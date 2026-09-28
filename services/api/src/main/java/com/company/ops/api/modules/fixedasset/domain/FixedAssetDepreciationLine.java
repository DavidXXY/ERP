package com.company.ops.api.modules.fixedasset.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "fa_depreciation_lines")
public class FixedAssetDepreciationLine extends BaseEntity {

  @Column(name = "run_id", nullable = false)
  private UUID runId;

  @Column(name = "asset_id", nullable = false)
  private UUID assetId;

  @Column(nullable = false, length = 7)
  private String period;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount = BigDecimal.ZERO;

  public UUID getRunId() { return runId; }
  public void setRunId(UUID runId) { this.runId = runId; }

  public UUID getAssetId() { return assetId; }
  public void setAssetId(UUID assetId) { this.assetId = assetId; }

  public String getPeriod() { return period; }
  public void setPeriod(String period) { this.period = period; }

  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
}
