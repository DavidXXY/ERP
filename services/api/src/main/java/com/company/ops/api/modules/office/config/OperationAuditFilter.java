package com.company.ops.api.modules.office.config;

import com.company.ops.api.modules.office.domain.OperationAudit;
import com.company.ops.api.modules.office.repository.OperationAuditRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OperationAuditFilter extends OncePerRequestFilter {
  private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
  private final OperationAuditRepository repository;
  public OperationAuditFilter(OperationAuditRepository repository) { this.repository = repository; }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/") || !MUTATING_METHODS.contains(request.getMethod());
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    long started = System.nanoTime();
    try { chain.doFilter(request, response); }
    finally {
      try {
        Principal principal = request.getUserPrincipal();
        OperationAudit item = new OperationAudit(); item.setUsername(principal == null ? "anonymous" : principal.getName());
        item.setHttpMethod(request.getMethod()); item.setRequestPath(request.getRequestURI()); item.setResponseStatus(response.getStatus());
        item.setClientIp(request.getRemoteAddr()); item.setDurationMs((System.nanoTime() - started) / 1_000_000); repository.save(item);
      } catch (RuntimeException ignored) { }
    }
  }
}
