package com.company.ops.api.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BackupStatusMetrics {
  static final String BACKUP_MARKER = "backup-last-success.epoch";
  static final String RESTORE_DRILL_MARKER = "restore-drill-last-success.epoch";

  private final Path statusDirectory;

  public BackupStatusMetrics(
      MeterRegistry meterRegistry,
      @Value("${ops.backup.status-directory:backups/.status}") String statusDirectory) {
    this.statusDirectory = Path.of(statusDirectory);
    registerGauge(
        meterRegistry,
        "ops.erp.backup.last.success.timestamp",
        "Unix timestamp of the latest fully successful encrypted backup",
        BACKUP_MARKER);
    registerGauge(
        meterRegistry,
        "ops.erp.restore.drill.last.success.timestamp",
        "Unix timestamp of the latest successful isolated restore drill",
        RESTORE_DRILL_MARKER);
  }

  private void registerGauge(
      MeterRegistry meterRegistry, String name, String description, String markerName) {
    Gauge.builder(name, () -> readTimestamp(markerName))
        .description(description)
        .baseUnit("seconds")
        .register(meterRegistry);
  }

  private double readTimestamp(String markerName) {
    try {
      long timestamp = Long.parseLong(
          Files.readString(statusDirectory.resolve(markerName)).trim());
      return timestamp > 0 ? timestamp : 0;
    } catch (IOException | NumberFormatException exception) {
      return 0;
    }
  }
}
