package com.company.ops.api.config;

import com.company.ops.api.modules.system.interceptor.AuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  private final AuditInterceptor auditInterceptor;

  public WebMvcConfig(AuditInterceptor auditInterceptor) {
    this.auditInterceptor = auditInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(auditInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/system/health", "/api/auth/login", "/api/auth/me");
  }
}
