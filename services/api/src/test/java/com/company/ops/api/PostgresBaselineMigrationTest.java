package com.company.ops.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgresBaselineMigrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("erp_baseline")
      .withUsername("erp")
      .withPassword("erp");

  @Test
  void productionBaselineBuildsCompletePostgresSchema() {
    Flyway flyway = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .load();

    var result = flyway.migrate();
    assertThat(result.targetSchemaVersion).isEqualTo("78");

    var dataSource = new DriverManagerDataSource(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    var jdbc = new JdbcTemplate(dataSource);
    assertThat(jdbc.queryForObject(
        "select count(*) from information_schema.tables where table_schema='public' "
            + "and table_name in ('sys_users','project_projects','project_cost_entries',"
            + "'procurement_goods_receipts','biz_collaboration_task_controls','shedlock')",
        Integer.class)).isEqualTo(6);
    assertThat(jdbc.queryForObject(
        "select count(*) from pg_indexes where schemaname='public' "
            + "and indexname='uk_proc_receipt_client_request'",
        Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from sys_users", Integer.class)).isZero();
  }
}
