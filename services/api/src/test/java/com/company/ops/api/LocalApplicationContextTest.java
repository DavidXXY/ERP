package com.company.ops.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.repository.SystemPermissionRepository;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ApplyInvoiceRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RegisterInvoiceRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReviewInvoiceRequest;
import com.company.ops.api.modules.crm.service.CrmOperationsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.time.LocalDate;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:erp_context;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.task.scheduling.enabled=false",
        "management.health.redis.enabled=false"
    }
)
@ActiveProfiles("local")
class LocalApplicationContextTest {
  @Autowired private DataSource dataSource;
  @Autowired private TestRestTemplate rest;
  @Autowired private SystemPermissionRepository permissionRepository;
  @Autowired private TransactionTemplate transactions;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CrmOperationsService crmOperationsService;
  @LocalServerPort private int port;

  @Test
  void migratesEmptyDatabaseThroughLatestVersion() {
    var jdbc = new JdbcTemplate(dataSource);
    Integer version = jdbc.queryForObject(
        "select max(cast(version as integer)) from flyway_schema_history where success = true",
        Integer.class
    );
    assertThat(version).isEqualTo(67);
    assertThat(jdbc.queryForObject("select count(*) from shedlock", Integer.class)).isZero();
  }

  @Test
  void enforcesAuthenticationAndCompletesLoginFlow() throws Exception {
    var anonymous = rest.getForEntity("http://localhost:" + port + "/api/auth/me", String.class);
    assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    var login = rest.postForEntity(
        "http://localhost:" + port + "/api/auth/login",
        Map.of("username", "admin", "password", "Admin@123"),
        String.class
    );
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(login.getBody()).contains("\"success\":true", "\"token\"");
    assertThat(login.getHeaders().getFirst("X-Request-ID")).isNotBlank();

    String token = objectMapper.readTree(login.getBody()).path("data").path("token").asText();
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    var currentUser = rest.exchange(
        "http://localhost:" + port + "/api/auth/me",
        org.springframework.http.HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class
    );
    assertThat(currentUser.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(currentUser.getBody()).contains("\"username\":\"admin\"");
  }

  @Test
  void isolatesJpaQueriesAndWritesByTenant() {
    savePermission("tenant-a", "test:tenant:a");
    savePermission("tenant-b", "test:tenant:b");

    try (var ignored = TenantContext.use("tenant-a")) {
      var codes = transactions.execute(status -> permissionRepository.findAll().stream()
          .map(SystemPermission::getCode)
          .toList());
      assertThat(codes).contains("test:tenant:a").doesNotContain("test:tenant:b");
    }
    try (var ignored = TenantContext.use("tenant-b")) {
      var codes = transactions.execute(status -> permissionRepository.findAll().stream()
          .map(SystemPermission::getCode)
          .toList());
      assertThat(codes).contains("test:tenant:b").doesNotContain("test:tenant:a");
    }
  }

  @Test
  void completesInvoiceApprovalStateMachine() {
    UUID approvedId = insertReceivable("TEST-INVOICE-APPROVED");
    var applied = crmOperationsService.applyInvoice(
        approvedId,
        new ApplyInvoiceRequest("测试业务员", "合同节点开票")
    );
    assertThat(applied.invoiceRequestStatus()).isEqualTo("PENDING_APPROVAL");

    var approved = crmOperationsService.reviewInvoice(
        approvedId,
        new ReviewInvoiceRequest("APPROVED", "测试财务", "资料完整")
    );
    assertThat(approved.invoiceRequestStatus()).isEqualTo("APPROVED");

    var invoiced = crmOperationsService.registerInvoice(
        approvedId,
        new RegisterInvoiceRequest("INV-TEST-001", LocalDate.now())
    );
    assertThat(invoiced.invoiceRequestStatus()).isEqualTo("INVOICED");
    assertThat(invoiced.invoiceNo()).isEqualTo("INV-TEST-001");

    UUID rejectedId = insertReceivable("TEST-INVOICE-REJECTED");
    crmOperationsService.applyInvoice(rejectedId, new ApplyInvoiceRequest("测试业务员", null));
    var rejected = crmOperationsService.reviewInvoice(
        rejectedId,
        new ReviewInvoiceRequest("REJECTED", "测试财务", "缺少开票资料")
    );
    assertThat(rejected.invoiceRequestStatus()).isEqualTo("REJECTED");
    var reapplied = crmOperationsService.applyInvoice(
        rejectedId,
        new ApplyInvoiceRequest("测试业务员", "资料已补齐")
    );
    assertThat(reapplied.invoiceRequestStatus()).isEqualTo("PENDING_APPROVAL");
    assertThat(reapplied.invoiceReviewComment()).isNull();
  }

  private UUID insertReceivable(String code) {
    var jdbc = new JdbcTemplate(dataSource);
    UUID id = UUID.randomUUID();
    UUID customerId = jdbc.queryForObject("select id from crm_customers limit 1", UUID.class);
    jdbc.update(
        "insert into fin_receivables "
            + "(id, tenant_id, customer_id, code, source_no, amount, due_date, status, "
            + "invoice_requested, invoice_request_status, settled_amount, version) "
            + "values (?, 'default', ?, ?, ?, 1000, ?, 'INVOICE_PENDING', false, 'NOT_REQUESTED', 0, 0)",
        id, customerId, code, code, LocalDate.now().plusDays(30)
    );
    return id;
  }

  private void savePermission(String tenantId, String code) {
    try (var ignored = TenantContext.use(tenantId)) {
      transactions.executeWithoutResult(status -> {
        var permission = new SystemPermission();
        permission.setCode(code);
        permission.setName(code);
        permission.setModule("test");
        permissionRepository.saveAndFlush(permission);
      });
    }
  }
}
