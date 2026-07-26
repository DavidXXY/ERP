package com.company.ops.api.modules.system.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sys_wechat_bindings")
public class SystemWechatBinding extends BaseEntity {
  @Column(name = "user_id", nullable = false) private UUID userId;
  @Column(name = "app_id", nullable = false, length = 64) private String appId;
  @Column(name = "open_id", nullable = false, length = 128) private String openId;
  @Column(name = "union_id", length = 128) private String unionId;
  @Column(name = "last_login_at") private OffsetDateTime lastLoginAt;
  public UUID getUserId() { return userId; }
  public void setUserId(UUID value) { userId = value; }
  public String getAppId() { return appId; }
  public void setAppId(String value) { appId = value; }
  public String getOpenId() { return openId; }
  public void setOpenId(String value) { openId = value; }
  public String getUnionId() { return unionId; }
  public void setUnionId(String value) { unionId = value; }
  public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
  public void setLastLoginAt(OffsetDateTime value) { lastLoginAt = value; }
}
