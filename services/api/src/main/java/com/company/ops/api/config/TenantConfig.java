package com.company.ops.api.config;

import com.company.ops.api.common.tenant.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantConfig {
  @Bean
  CurrentTenantIdentifierResolver<String> tenantIdentifierResolver() {
    return new CurrentTenantIdentifierResolver<>() {
      @Override public String resolveCurrentTenantIdentifier() {
        return TenantContext.currentTenant();
      }
      @Override public boolean validateExistingCurrentSessions() {
        return true;
      }
    };
  }

  @Bean
  HibernatePropertiesCustomizer tenantHibernateCustomizer(
      CurrentTenantIdentifierResolver<String> resolver) {
    return properties -> properties.put("hibernate.tenant_identifier_resolver", resolver);
  }
}
