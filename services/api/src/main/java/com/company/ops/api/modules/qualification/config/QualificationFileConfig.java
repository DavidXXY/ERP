package com.company.ops.api.modules.qualification.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class QualificationFileConfig implements WebMvcConfigurer {
  private final Path storageRoot;
  public QualificationFileConfig(@Value("${ops.storage.local-path:.local-data/uploads}") String storagePath) {
    storageRoot = Path.of(storagePath).toAbsolutePath().normalize().resolve("qualification");
  }
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/qualification-files/**").addResourceLocations(storageRoot.toUri().toString());
  }
}
