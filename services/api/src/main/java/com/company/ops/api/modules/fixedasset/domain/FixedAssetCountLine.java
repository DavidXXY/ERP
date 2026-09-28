package com.company.ops.api.modules.fixedasset.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "fa_count_lines")
public class FixedAssetCountLine extends BaseEntity {

  @Column(name = "count_id", nullable = false)
  private UUID countId;

  @Column(name = "asset_id", nullable = false)
  private UUID assetId;

  @Column(name = "book_quantity", nullable = false)
  private int bookQuantity = 1;

  @Column(name = "actual_quantity")
  private Integer actualQuantity;

  @Column(name = "difference")
  private Integer difference;

  @Column(length = 300)
  private String remark;

  public UUID getCountId() { return countId; }
  public void setCountId(UUID countId) { this.countId = countId; }

  public UUID getAssetId() { return assetId; }
  public void setAssetId(UUID assetId) { this.assetId = assetId; }

  public int getBookQuantity() { return bookQuantity; }
  public void setBookQuantity(int bookQuantity) { this.bookQuantity = bookQuantity; }

  public Integer getActualQuantity() { return actualQuantity; }
  public void setActualQuantity(Integer actualQuantity) { this.actualQuantity = actualQuantity; }

  public Integer getDifference() { return difference; }
  public void setDifference(Integer difference) { this.difference = difference; }

  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
