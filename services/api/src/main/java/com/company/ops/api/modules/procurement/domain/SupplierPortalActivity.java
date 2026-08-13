package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "supplier_portal_account_activities")
public class SupplierPortalActivity extends BaseEntity {
  @Column(name = "account_id", nullable = false) private UUID accountId;
  @Column(name = "supplier_id", nullable = false) private UUID supplierId;
  @Column(nullable = false, length = 40) private String action;
  @Column(length = 500) private String detail;
  @Column(length = 64) private String ip;

  public UUID getAccountId() { return accountId; }
  public void setAccountId(UUID v) { accountId = v; }
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID v) { supplierId = v; }
  public String getAction() { return action; }
  public void setAction(String v) { action = v; }
  public String getDetail() { return detail; }
  public void setDetail(String v) { detail = v; }
  public String getIp() { return ip; }
  public void setIp(String v) { ip = v; }
}
