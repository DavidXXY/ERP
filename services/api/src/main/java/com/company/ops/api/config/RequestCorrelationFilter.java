package com.company.ops.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {
  static final String HEADER = "X-Request-ID";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String requestId = request.getHeader(HEADER);
    if (requestId == null || !requestId.matches("[A-Za-z0-9._-]{1,64}")) {
      requestId = UUID.randomUUID().toString();
    }
    try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
      response.setHeader(HEADER, requestId);
      filterChain.doFilter(request, response);
    }
  }
}
