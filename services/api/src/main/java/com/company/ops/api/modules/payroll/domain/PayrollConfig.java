package com.company.ops.api.modules.payroll.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "payroll_configs", uniqueConstraints = @UniqueConstraint(
    name = "uk_payroll_configs_tenant_key", columnNames = {"tenant_id", "config_key"}))
public class PayrollConfig extends BaseEntity {

  @Column(name = "config_key", nullable = false, length = 64)
  private String configKey;

  @Column(name = "config_value", nullable = false, precision = 18, scale = 4)
  private BigDecimal configValue = BigDecimal.ZERO;

  @Column(length = 300)
  private String remark;

  public String getConfigKey() {
    return configKey;
  }

  public void setConfigKey(String configKey) {
    this.configKey = configKey;
  }

  public BigDecimal getConfigValue() {
    return configValue;
  }

  public void setConfigValue(BigDecimal configValue) {
    this.configValue = configValue;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }
}
