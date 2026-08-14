package com.company.ops.api.modules.procurement.security;

import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.procurement.domain.SupplierPortalAccount;
import com.company.ops.api.modules.procurement.repository.SupplierPortalAccountRepository;
import com.company.ops.api.modules.system.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SupplierPortalAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final SupplierPortalAccountRepository accounts;

  public SupplierPortalAuthenticationFilter(
      JwtService jwtService,
      SupplierPortalAccountRepository accounts
  ) {
    this.jwtService = jwtService;
    this.accounts = accounts;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/supplier-portal/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    String token = null;
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      // 大文件下载使用 <a> 链接，无法携带请求头；仅允许 GET 下载端点通过查询参数携带令牌。
      if ("GET".equalsIgnoreCase(request.getMethod())
          && (request.getRequestURI().endsWith("/download")
              || request.getRequestURI().endsWith("/excel")
              || request.getRequestURI().endsWith("/pdf"))) {
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
          token = queryToken;
        }
      }
      if (token == null) {
        filterChain.doFilter(request, response);
        return;
      }
    } else {
      token = authorization.substring(7);
    }
    String tenantId;
    try {
      tenantId = jwtService.extractTenant(token);
    } catch (RuntimeException exception) {
      SecurityContextHolder.clearContext();
      filterChain.doFilter(request, response);
      return;
    }

    try (TenantContext.Scope ignored = TenantContext.use(tenantId)) {
      try {
        if (jwtService.isSupplierPortalToken(token)) {
          SupplierPortalAccount account = accounts.findById(jwtService.extractPortalAccountId(token))
              .orElse(null);
          if (account != null && jwtService.isValidSupplierPortalToken(token, account)) {
            SupplierPortalPrincipal principal = new SupplierPortalPrincipal(
                account.getId(), account.getSupplierId(), account.getTenantId(), account.getEmail(),
                account.getContactName(), account.getStatus(), account.getAuthVersion());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("supplier-portal:access")));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
          }
        }
      } catch (RuntimeException exception) {
        SecurityContextHolder.clearContext();
      }
      filterChain.doFilter(request, response);
    }
  }
}
