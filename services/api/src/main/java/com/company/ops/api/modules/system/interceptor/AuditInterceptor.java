package com.company.ops.api.modules.system.interceptor;

import com.company.ops.api.modules.system.domain.SystemAuditLog;
import com.company.ops.api.modules.system.repository.SystemAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.OffsetDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuditInterceptor implements HandlerInterceptor {
  private final SystemAuditLogRepository auditLogRepository;

  public AuditInterceptor(SystemAuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    request.setAttribute("_audit_start", System.currentTimeMillis());
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    Long startTime = (Long) request.getAttribute("_audit_start");
    if (startTime == null) return;

    long duration = System.currentTimeMillis() - startTime;
    String username = null;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
      username = auth.getName();
    }

    String clientIp = request.getHeader("X-Forwarded-For");
    if (clientIp == null || clientIp.isEmpty()) {
      clientIp = request.getRemoteAddr();
    }

    SystemAuditLog log = new SystemAuditLog();
    log.setUsername(username);
    log.setHttpMethod(request.getMethod());
    log.setRequestPath(request.getRequestURI());
    log.setQueryString(truncate(request.getQueryString(), 1000));
    log.setOperationType(operationType(request.getMethod()));
    log.setBizModule(module(request.getRequestURI()));
    log.setBizObject(objectId(request.getRequestURI()));
    log.setResponseStatus(response.getStatus());
    log.setDurationMs(duration);
    log.setClientIp(clientIp);
    auditLogRepository.save(log);
  }

  private String operationType(String method) {
    return switch (method) {
      case "POST" -> "CREATE_OR_ACTION";
      case "PUT", "PATCH" -> "UPDATE";
      case "DELETE" -> "DELETE";
      case "GET" -> "READ";
      default -> method;
    };
  }

  private String module(String path) {
    if (path == null) return null;
    String[] parts = path.split("/");
    return parts.length > 2 ? parts[2] : null;
  }

  private String objectId(String path) {
    if (path == null) return null;
    String[] parts = path.split("/");
    for (int i = parts.length - 1; i >= 0; i--) {
      if (parts[i].matches("[0-9a-fA-F-]{32,36}")) return parts[i];
    }
    return null;
  }

  private String truncate(String value, int max) {
    if (value == null || value.length() <= max) return value;
    return value.substring(0, max);
  }
}
