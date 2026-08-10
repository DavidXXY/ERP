package com.company.ops.api.modules.system.interceptor;

import com.company.ops.api.modules.system.service.AuditLogWriter;
import com.company.ops.api.modules.system.service.AuditLogWriter.AuditEvent;
import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.procurement.security.SupplierPortalPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.company.ops.api.common.security.ClientIpResolver;

@Component
public class AuditInterceptor implements HandlerInterceptor {
  private final AuditLogWriter auditLogWriter;
  private final ClientIpResolver clientIpResolver;

  public AuditInterceptor(AuditLogWriter auditLogWriter, ClientIpResolver clientIpResolver) {
    this.auditLogWriter = auditLogWriter;
    this.clientIpResolver = clientIpResolver;
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
      username = principalName(auth);
    }

    String clientIp = clientIpResolver.resolve(request);

    auditLogWriter.write(new AuditEvent(
        TenantContext.currentTenant(), username, request.getMethod(), request.getRequestURI(),
        truncate(request.getQueryString(), 1000), operationType(request.getMethod()),
        module(request.getRequestURI()), objectId(request.getRequestURI()), response.getStatus(),
        duration, clientIp));
  }

  private String principalName(Authentication auth) {
    Object principal = auth.getPrincipal();
    if (principal instanceof SupplierPortalPrincipal portal) {
      return truncate(portal.email(), 80);
    }
    if (principal instanceof UserDetails userDetails) {
      return truncate(userDetails.getUsername(), 80);
    }
    return truncate(auth.getName(), 80);
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
