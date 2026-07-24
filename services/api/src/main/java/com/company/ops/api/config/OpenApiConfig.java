package com.company.ops.api.config;

import com.company.ops.api.common.version.ApplicationVersion;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private final ApplicationVersion applicationVersion;

  public OpenApiConfig(ApplicationVersion applicationVersion) {
    this.applicationVersion = applicationVersion;
  }

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("企业管理系统 API")
            .version(applicationVersion.getDisplayVersion())
            .description("CRM、项目、工单、仓储、财务、OA 一体化接口"));
  }
}
