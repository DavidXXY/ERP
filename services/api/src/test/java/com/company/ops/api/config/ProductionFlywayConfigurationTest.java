package com.company.ops.api.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

class ProductionFlywayConfigurationTest {
  @Test
  void permitsConsolidatedHistoryWhenEveryCurrentMigrationSucceeded() {
    List<MigrationInfo> migrations = currentMigrations();
    migrations.add(migration(12, MigrationState.MISSING_SUCCESS));
    migrations.add(migration(77, MigrationState.BASELINE));

    assertThatCode(() -> ProductionFlywayConfiguration.validateCurrentMigrationHistory(
        migrations.toArray(MigrationInfo[]::new))).doesNotThrowAnyException();
  }

  @Test
  void rejectsMissingCurrentMigration() {
    List<MigrationInfo> migrations = currentMigrations();
    migrations.removeIf(migration -> migration.getVersion().getVersion().equals("93"));

    assertThatThrownBy(() -> ProductionFlywayConfiguration.validateCurrentMigrationHistory(
        migrations.toArray(MigrationInfo[]::new)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("V93")
        .hasMessageContaining("absent");
  }

  @Test
  void rejectsMissingOrFailedResolvedState() {
    List<MigrationInfo> migrations = currentMigrations();
    migrations.set(15, migration(93, MigrationState.MISSING_SUCCESS));

    assertThatThrownBy(() -> ProductionFlywayConfiguration.validateCurrentMigrationHistory(
        migrations.toArray(MigrationInfo[]::new)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("V93")
        .hasMessageContaining("Missing");
  }

  @Test
  void rejectsDatabaseMigrationThatIsNewerThanApplicationCode() {
    List<MigrationInfo> migrations = currentMigrations();
    migrations.add(migration(101, MigrationState.FUTURE_SUCCESS));

    assertThatThrownBy(() -> ProductionFlywayConfiguration.validateCurrentMigrationHistory(
        migrations.toArray(MigrationInfo[]::new)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("V101")
        .hasMessageContaining("Future");
  }

  private static List<MigrationInfo> currentMigrations() {
    List<MigrationInfo> migrations = new ArrayList<>();
    for (int version = 78; version <= 100; version++) {
      migrations.add(migration(version, MigrationState.SUCCESS));
    }
    return migrations;
  }

  private static MigrationInfo migration(int version, MigrationState state) {
    MigrationInfo migration = mock(MigrationInfo.class);
    when(migration.getVersion()).thenReturn(MigrationVersion.fromVersion(String.valueOf(version)));
    when(migration.getState()).thenReturn(state);
    return migration;
  }
}
