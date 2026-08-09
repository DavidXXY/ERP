package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "procurement_central_plans")
public class CentralPlan extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(name = "period_year", nullable = false)
  private Integer periodYear;

  @Column(nullable = false, length = 24)
  private String status = "DRAFT";

  @Column(length = 1000)
  private String remark;

  @Column(name = "created_by_name", length = 80)
  private String createdByName;

  public String getCode() { return code; }
  public void setCode(String v) { code = v; }
  public String getName() { return name; }
  public void setName(String v) { name = v; }
  public Integer getPeriodYear() { return periodYear; }
  public void setPeriodYear(Integer v) { periodYear = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { status = v; }
  public String getRemark() { return remark; }
  public void setRemark(String v) { remark = v; }
  public String getCreatedByName() { return createdByName; }
  public void setCreatedByName(String v) { createdByName = v; }
}
