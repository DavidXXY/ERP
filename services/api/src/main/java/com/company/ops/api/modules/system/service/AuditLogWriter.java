package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.system.domain.SystemAuditLog;
import com.company.ops.api.modules.system.repository.SystemAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogWriter {
  private static final Logger log = LoggerFactory.getLogger(AuditLogWriter.class);
  private final SystemAuditLogRepository repository;

  public AuditLogWriter(SystemAuditLogRepository repository) { this.repository = repository; }

  @Async("auditExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void write(AuditEvent event) {
    try (TenantContext.Scope ignored = TenantContext.use(event.tenantId())) {
      SystemAuditLog audit = new SystemAuditLog();
      audit.setUsername(event.username()); audit.setHttpMethod(event.httpMethod());
      audit.setRequestPath(event.requestPath()); audit.setQueryString(event.queryString());
      audit.setOperationType(event.operationType()); audit.setBizModule(event.bizModule());
      audit.setBizObject(event.bizObject()); audit.setResponseStatus(event.responseStatus());
      audit.setDurationMs(event.durationMs()); audit.setClientIp(event.clientIp());
      repository.save(audit);
    } catch (RuntimeException exception) {
      log.error("Failed to persist audit event for {} {}", event.httpMethod(), event.requestPath(), exception);
    }
  }

  public record AuditEvent(String tenantId, String username, String httpMethod, String requestPath,
                           String queryString, String operationType, String bizModule, String bizObject,
                           int responseStatus, long durationMs, String clientIp) {}
}
