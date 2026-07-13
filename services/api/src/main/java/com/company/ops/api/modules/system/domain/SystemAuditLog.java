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
  @Column(name = "query_string", length = 1000)
  private String queryString;
  @Column(name = "operation_type", length = 40)
  private String operationType;
  @Column(name = "biz_module", length = 80)
  private String bizModule;
  @Column(name = "biz_object", length = 120)
  private String bizObject;

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
  public String getQueryString() { return queryString; }
  public void setQueryString(String v) { queryString = v; }
  public String getOperationType() { return operationType; }
  public void setOperationType(String v) { operationType = v; }
  public String getBizModule() { return bizModule; }
  public void setBizModule(String v) { bizModule = v; }
  public String getBizObject() { return bizObject; }
  public void setBizObject(String v) { bizObject = v; }
}
