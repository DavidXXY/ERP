package com.company.ops.api.modules.system.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_audit_logs")
public class SystemAuditLog extends BaseEntity {
  @Column(length = 80)
  private String username;

  @Column(name = "http_method", nullable = false, length = 10)
  private String httpMethod;

  @Column(name = "request_path", nullable = false, length = 500)
  private String requestPath;

  @Column(name = "response_status", nullable = false)
  private int responseStatus;

  @Column(name = "duration_ms", nullable = false)
  private long durationMs;

  @Column(name = "client_ip", length = 50)
  private String clientIp;

  public String getUsername() { return username; }
  public void setUsername(String v) { username = v; }
  public String getHttpMethod() { return httpMethod; }
  public void setHttpMethod(String v) { httpMethod = v; }
  public String getRequestPath() { return requestPath; }
  public void setRequestPath(String v) { requestPath = v; }
  public int getResponseStatus() { return responseStatus; }
  public void setResponseStatus(int v) { responseStatus = v; }
  public long getDurationMs() { return durationMs; }
  public void setDurationMs(long v) { durationMs = v; }
  public String getClientIp() { return clientIp; }
  public void setClientIp(String v) { clientIp = v; }
}
