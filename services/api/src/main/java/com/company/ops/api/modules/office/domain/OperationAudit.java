package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity @Table(name = "operation_audits")
public class OperationAudit extends BaseEntity {
  @Column(name = "username", length = 80) private String username;
  @Column(name = "http_method", nullable = false, length = 12) private String httpMethod;
  @Column(name = "request_path", nullable = false, length = 500) private String requestPath;
  @Column(name = "response_status", nullable = false) private Integer responseStatus;
  @Column(name = "client_ip", length = 80) private String clientIp;
  @Column(name = "duration_ms", nullable = false) private Long durationMs;
  public String getUsername() { return username; } public void setUsername(String v) { username = v; }
  public String getHttpMethod() { return httpMethod; } public void setHttpMethod(String v) { httpMethod = v; }
  public String getRequestPath() { return requestPath; } public void setRequestPath(String v) { requestPath = v; }
  public Integer getResponseStatus() { return responseStatus; } public void setResponseStatus(Integer v) { responseStatus = v; }
  public String getClientIp() { return clientIp; } public void setClientIp(String v) { clientIp = v; }
  public Long getDurationMs() { return durationMs; } public void setDurationMs(Long v) { durationMs = v; }
}
