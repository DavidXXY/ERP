package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "procurement_framework_agreements")
public class FrameworkAgreement extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Column(name = "valid_from", nullable = false)
  private LocalDate validFrom;

  @Column(name = "valid_to", nullable = false)
  private LocalDate validTo;

  @Column(nullable = false, length = 24)
  private String status = "ACTIVE";

  @Column(length = 1000)
  private String remark;

  @Column(name = "created_by_name", length = 80)
  private String createdByName;

  public String getCode() { return code; }
  public void setCode(String v) { code = v; }
  public String getTitle() { return title; }
  public void setTitle(String v) { title = v; }
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID v) { supplierId = v; }
  public LocalDate getValidFrom() { return validFrom; }
  public void setValidFrom(LocalDate v) { validFrom = v; }
  public LocalDate getValidTo() { return validTo; }
  public void setValidTo(LocalDate v) { validTo = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { status = v; }
  public String getRemark() { return remark; }
  public void setRemark(String v) { remark = v; }
  public String getCreatedByName() { return createdByName; }
  public void setCreatedByName(String v) { createdByName = v; }
}
