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

    var pendingMigrations = flyway.info().pending();
    assertThat(pendingMigrations).isNotEmpty();
    var expectedTargetSchemaVersion =
        pendingMigrations[pendingMigrations.length - 1].getVersion().getVersion();

    var result = flyway.migrate();
    assertThat(result.targetSchemaVersion).isEqualTo(expectedTargetSchemaVersion);

    var dataSource = new DriverManagerDataSource(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    var jdbc = new JdbcTemplate(dataSource);
    assertThat(jdbc.queryForObject(
        "select count(*) from information_schema.tables where table_schema='public' "
            + "and table_name in ('sys_users','project_projects','project_cost_entries',"
            + "'procurement_goods_receipts','biz_collaboration_task_controls','shedlock',"
            + "'work_order_attachments','work_order_mobile_operations','sys_wechat_bindings',"
            + "'biz_accounting_periods','biz_bank_statement_lines','biz_control_records',"
            + "'biz_governance_action_logs','fin_period_end_jobs','fin_partner_reconciliations',"
            + "'fin_cash_forecast_scenarios','fin_tax_filings','fin_consolidation_runs',"
            + "'fin_report_snapshots','fin_voucher_generation_requests')",
        Integer.class)).isEqualTo(20);
    assertThat(jdbc.queryForObject(
        "select count(*) from pg_indexes where schemaname='public' "
            + "and indexname='uk_proc_receipt_client_request'",
        Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject(
        "select count(*) from information_schema.table_constraints where constraint_schema='public' "
            + "and constraint_name in ('uk_period_job_key','uk_tax_filing_period',"
            + "'uk_report_snapshot_hash','uk_voucher_request_key')",
        Integer.class)).isEqualTo(4);
    assertThat(jdbc.queryForObject("select count(*) from sys_users", Integer.class)).isZero();
  }
}
