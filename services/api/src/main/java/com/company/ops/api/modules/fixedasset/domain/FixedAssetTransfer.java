package com.company.ops.api.modules.fixedasset.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fa_transfers", uniqueConstraints = @UniqueConstraint(
    name = "uk_fa_transfers_tenant_code", columnNames = {"tenant_id", "code"}))
public class FixedAssetTransfer extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "asset_id", nullable = false)
  private UUID assetId;

  @Column(name = "from_location", length = 120)
  private String fromLocation;

  @Column(name = "to_location", length = 120)
  private String toLocation;

  @Column(name = "from_custodian", length = 80)
  private String fromCustodian;

  @Column(name = "to_custodian", length = 80)
  private String toCustodian;

  @Column(name = "transfer_date", nullable = false)
  private LocalDate transferDate;

  @Column(length = 500)
  private String remark;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public UUID getAssetId() { return assetId; }
  public void setAssetId(UUID assetId) { this.assetId = assetId; }

  public String getFromLocation() { return fromLocation; }
  public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }

  public String getToLocation() { return toLocation; }
  public void setToLocation(String toLocation) { this.toLocation = toLocation; }

  public String getFromCustodian() { return fromCustodian; }
  public void setFromCustodian(String fromCustodian) { this.fromCustodian = fromCustodian; }

  public String getToCustodian() { return toCustodian; }
  public void setToCustodian(String toCustodian) { this.toCustodian = toCustodian; }

  public LocalDate getTransferDate() { return transferDate; }
  public void setTransferDate(LocalDate transferDate) { this.transferDate = transferDate; }

  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
