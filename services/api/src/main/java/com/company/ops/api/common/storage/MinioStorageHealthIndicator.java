package com.company.ops.api.common.storage;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ops.storage.type", havingValue = "minio")
public class MinioStorageHealthIndicator implements HealthIndicator {
  private final MinioFileStorageService storageService;

  public MinioStorageHealthIndicator(MinioFileStorageService storageService) {
    this.storageService = storageService;
  }

  @Override
  public Health health() {
    return storageService.isAvailable()
        ? Health.up().withDetail("storage", "minio").build()
        : Health.down().withDetail("storage", "minio").build();
  }
}
