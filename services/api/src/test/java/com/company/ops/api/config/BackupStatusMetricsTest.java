package com.company.ops.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackupStatusMetricsTest {
  @TempDir Path statusDirectory;

  @Test
  void reportsZeroWhenSuccessMarkersAreAbsent() {
    var registry = new SimpleMeterRegistry();
    new BackupStatusMetrics(registry, statusDirectory.toString());

    assertThat(gauge(registry, "ops.erp.backup.last.success.timestamp")).isZero();
    assertThat(gauge(registry, "ops.erp.restore.drill.last.success.timestamp")).isZero();
  }

  @Test
  void readsUpdatedSuccessMarkersWithoutRestartingApplication() throws Exception {
    var registry = new SimpleMeterRegistry();
    new BackupStatusMetrics(registry, statusDirectory.toString());

    Files.writeString(statusDirectory.resolve(BackupStatusMetrics.BACKUP_MARKER), "1700000000\n");
    Files.writeString(
        statusDirectory.resolve(BackupStatusMetrics.RESTORE_DRILL_MARKER), "1700000100\n");

    assertThat(gauge(registry, "ops.erp.backup.last.success.timestamp"))
        .isEqualTo(1700000000d);
    assertThat(gauge(registry, "ops.erp.restore.drill.last.success.timestamp"))
        .isEqualTo(1700000100d);
  }

  @Test
  void treatsMalformedOrNonPositiveMarkersAsUnknown() throws Exception {
    var registry = new SimpleMeterRegistry();
    new BackupStatusMetrics(registry, statusDirectory.toString());
    Files.writeString(statusDirectory.resolve(BackupStatusMetrics.BACKUP_MARKER), "not-a-time\n");
    Files.writeString(statusDirectory.resolve(BackupStatusMetrics.RESTORE_DRILL_MARKER), "-1\n");

    assertThat(gauge(registry, "ops.erp.backup.last.success.timestamp")).isZero();
    assertThat(gauge(registry, "ops.erp.restore.drill.last.success.timestamp")).isZero();
  }

  @Test
  void exportsNamesUsedByPrometheusAlertRules() {
    var registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    new BackupStatusMetrics(registry, statusDirectory.toString());

    assertThat(registry.scrape())
        .contains("ops_erp_backup_last_success_timestamp_seconds")
        .contains("ops_erp_restore_drill_last_success_timestamp_seconds");
  }

  private static double gauge(SimpleMeterRegistry registry, String name) {
    return registry.get(name).gauge().value();
  }
}
