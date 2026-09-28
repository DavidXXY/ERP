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
@Table(name = "fa_disposals", uniqueConstraints = @UniqueConstraint(
    name = "uk_fa_disposals_tenant_code", columnNames = {"tenant_id", "code"}))
public class FixedAssetDisposal extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "asset_id", nullable = false)
  private UUID assetId;

  @Column(name = "disposal_date", nullable = false)
  private LocalDate disposalDate;

  @Column(length = 24)
  private String method;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal proceeds = BigDecimal.ZERO;

  @Column(name = "net_book_value", nullable = false, precision = 18, scale = 2)
  private BigDecimal netBookValue = BigDecimal.ZERO;

  @Column(name = "gain_loss", nullable = false, precision = 18, scale = 2)
  private BigDecimal gainLoss = BigDecimal.ZERO;

  @Column(length = 500)
  private String remark;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public UUID getAssetId() { return assetId; }
  public void setAssetId(UUID assetId) { this.assetId = assetId; }

  public LocalDate getDisposalDate() { return disposalDate; }
  public void setDisposalDate(LocalDate disposalDate) { this.disposalDate = disposalDate; }

  public String getMethod() { return method; }
  public void setMethod(String method) { this.method = method; }

  public BigDecimal getProceeds() { return proceeds; }
  public void setProceeds(BigDecimal proceeds) { this.proceeds = proceeds; }

  public BigDecimal getNetBookValue() { return netBookValue; }
  public void setNetBookValue(BigDecimal netBookValue) { this.netBookValue = netBookValue; }

  public BigDecimal getGainLoss() { return gainLoss; }
  public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }

  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
