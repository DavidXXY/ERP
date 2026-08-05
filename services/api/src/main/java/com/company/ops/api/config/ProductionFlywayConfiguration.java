package com.company.ops.api.config;

import java.util.NavigableMap;
import java.util.TreeMap;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionFlywayConfiguration {
  static final int CONSOLIDATED_HISTORY_VERSION = 77;
  static final int MINIMUM_CURRENT_SCHEMA_VERSION = 100;

  @Bean
  FlywayMigrationStrategy productionFlywayMigrationStrategy() {
    return flyway -> {
      flyway.migrate();
      validateCurrentMigrationHistory(flyway.info().all());
    };
  }

  static void validateCurrentMigrationHistory(MigrationInfo[] migrations) {
    NavigableMap<Integer, MigrationState> currentHistory = new TreeMap<>();
    for (MigrationInfo migration : migrations) {
      if (migration.getVersion() == null) {
        continue;
      }
      String version = migration.getVersion().getVersion();
      if (!version.matches("[0-9]+")) {
        throw new IllegalStateException("Production migrations must use integer versions: " + version);
      }
      int numericVersion = Integer.parseInt(version);
      if (numericVersion > CONSOLIDATED_HISTORY_VERSION) {
        currentHistory.put(numericVersion, migration.getState());
      }
    }

    if (currentHistory.isEmpty()
        || currentHistory.lastKey() < MINIMUM_CURRENT_SCHEMA_VERSION) {
      throw new IllegalStateException(
          "Production schema must include migrations through V" + MINIMUM_CURRENT_SCHEMA_VERSION);
    }

    for (int version = CONSOLIDATED_HISTORY_VERSION + 1;
        version <= currentHistory.lastKey();
        version++) {
      MigrationState state = currentHistory.get(version);
      if (state != MigrationState.SUCCESS) {
        throw new IllegalStateException(
            "Production migration V" + version + " is not successfully resolved: "
                + (state == null ? "absent" : state.getDisplayName()));
      }
    }
  }
}
