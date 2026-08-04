package com.company.ops.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.modules.bi.service.BiService;
import com.company.ops.api.modules.finance.service.FinanceService;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemPermissionRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.domain.SystemNotificationRead;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.office.repository.SystemNotificationReadRepository;
import com.company.ops.api.modules.qualification.domain.CompanyQualification;
import com.company.ops.api.modules.qualification.repository.CompanyQualificationRepository;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ApplyInvoiceRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RegisterInvoiceRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReviewInvoiceRequest;
import com.company.ops.api.modules.crm.service.CrmOperationsService;
import com.company.ops.api.modules.system.dto.CreateOrganizationRequest;
import com.company.ops.api.modules.system.dto.CreatePermissionRequest;
import com.company.ops.api.modules.system.dto.CreateRoleRequest;
import com.company.ops.api.modules.system.service.OrganizationService;
import com.company.ops.api.modules.system.service.PermissionService;
import com.company.ops.api.modules.system.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.PageRequest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:erp_context;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.locations=classpath:db/migration-h2",
        "spring.task.scheduling.enabled=false",
        "management.health.redis.enabled=false"
    }
)
class LocalApplicationContextTest {
  @Autowired private DataSource dataSource;
  @Autowired private TestRestTemplate rest;
  @Autowired private SystemPermissionRepository permissionRepository;
  @Autowired private SystemUserRepository userRepository;
  @Autowired private SystemNotificationRepository notificationRepository;
  @Autowired private SystemNotificationReadRepository notificationReadRepository;
  @Autowired private CompanyQualificationRepository companyQualificationRepository;
  @Autowired private DeleteGovernanceService deleteGovernanceService;
  @Autowired private TransactionTemplate transactions;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CrmOperationsService crmOperationsService;
  @Autowired private BiService biService;
  @Autowired private FinanceService financeService;
  @Autowired private LedgerService ledgerService;
  @Autowired private OrganizationService organizationService;
  @Autowired private PermissionService permissionService;
  @Autowired private RoleService roleService;
  @LocalServerPort private int port;

  @Test
  void migratesEmptyDatabaseThroughLatestVersion() {
    var jdbc = new JdbcTemplate(dataSource);
    Integer version = jdbc.queryForObject(
        "select max(cast(version as integer)) from flyway_schema_history where success = true",
        Integer.class
    );
    assertThat(version).isEqualTo(92);
    assertThat(jdbc.queryForObject("select count(*) from shedlock", Integer.class)).isZero();
  }

  @Test
  void loadsFinanceAndLedgerOverviewThroughTypedAggregateProjections() {
    var finance = financeService.overview();
    var ledger = ledgerService.overview();

    assertThat(finance.receivableAmount()).isNotNull();
    assertThat(finance.payableAmount()).isNotNull();
    assertThat(ledger.totalDebit()).isNotNull();
    assertThat(ledger.totalCredit()).isNotNull();
  }

  @Test
  void filtersQualificationStatusAndHiddenIdsBeforePagination() {
    LocalDate today = LocalDate.now();
    String marker = "page-filter-" + UUID.randomUUID().toString().substring(0, 8);
    List<CompanyQualification> fixtures = transactions.execute(status -> companyQualificationRepository.saveAll(List.of(
        companyQualification(marker, "voided", "VOIDED", false, null),
        companyQualification(marker, "locked", "NORMAL", true, null),
        companyQualification(marker, "unverified", "NORMAL", false, null),
        companyQualification(marker, "expired", "NORMAL", false, today.minusDays(1)),
        companyQualification(marker, "expiring", "NORMAL", false, today.plusDays(30)),
        companyQualification(marker, "valid-visible", "NORMAL", false, today.plusDays(120)),
        companyQualification(marker, "valid-hidden", "NORMAL", false, today.plusDays(120))
    )));

    UUID hiddenId = fixtures.get(6).getId();
    transactions.executeWithoutResult(status -> {
      assertThat(companyQualificationRepository.search(
          marker, "", "VOIDED", today, today.plusDays(90), List.of(hiddenId), PageRequest.of(0, 20)))
          .hasSize(1).allMatch(item -> item.getName().endsWith("voided"));
      assertThat(companyQualificationRepository.search(
          marker, "", "LOCKED", today, today.plusDays(90), List.of(hiddenId), PageRequest.of(0, 20)))
          .hasSize(1).allMatch(item -> item.getName().endsWith("locked"));
      assertThat(companyQualificationRepository.search(
          marker, "", "UNVERIFIED", today, today.plusDays(90), List.of(hiddenId), PageRequest.of(0, 20)))
          .hasSize(1).allMatch(item -> item.getName().endsWith("unverified"));
      assertThat(companyQualificationRepository.search(
          marker, "", "EXPIRED", today, today.plusDays(90), List.of(hiddenId), PageRequest.of(0, 20)))
          .hasSize(1).allMatch(item -> item.getName().endsWith("expired"));
      assertThat(companyQualificationRepository.search(
          marker, "", "EXPIRING", today, today.plusDays(90), List.of(hiddenId), PageRequest.of(0, 20)))
          .hasSize(1).allMatch(item -> item.getName().endsWith("expiring"));
      var validPage = companyQualificationRepository.search(
          marker, "", "VALID", today, today.plusDays(90), List.of(hiddenId), PageRequest.of(0, 20));
      assertThat(validPage.getTotalElements()).isEqualTo(1);
      assertThat(validPage.getContent()).extracting(CompanyQualification::getName)
          .containsExactly(marker + "-valid-visible");
    });
  }

  @Test
  void isolatesSoftDeleteGovernanceByTenant() {
    UUID tenantAEntity = UUID.randomUUID();
    UUID tenantBEntity = UUID.randomUUID();

    try (var ignored = TenantContext.use("delete-tenant-a")) {
      transactions.executeWithoutResult(status ->
          assertThat(deleteGovernanceService.allowPhysicalDelete("TEST_ENTITY", tenantAEntity, "Tenant A"))
              .isFalse());
      assertThat(deleteGovernanceService.hiddenIds("TEST_ENTITY"))
          .contains(tenantAEntity)
          .doesNotContain(tenantBEntity);
    }
    try (var ignored = TenantContext.use("delete-tenant-b")) {
      transactions.executeWithoutResult(status ->
          assertThat(deleteGovernanceService.allowPhysicalDelete("TEST_ENTITY", tenantBEntity, "Tenant B"))
              .isFalse());
      assertThat(deleteGovernanceService.hiddenIds("TEST_ENTITY"))
          .contains(tenantBEntity)
          .doesNotContain(tenantAEntity);
    }
  }

  private CompanyQualification companyQualification(
      String marker, String suffix, String manualStatus, boolean locked, LocalDate validTo) {
    CompanyQualification item = new CompanyQualification();
    item.setExternalId(UUID.randomUUID().toString());
    item.setSubjectCompany("Test Company");
    item.setName(marker + "-" + suffix);
    item.setCategory("Test");
    item.setManualStatus(manualStatus);
    item.setLocked(locked);
    item.setValidTo(validTo);
    return item;
  }

  @Test
  void keepsGlobalNotificationReadStateIndependentPerUser() {
    record Fixture(UUID firstUserId, UUID secondUserId, UUID notificationId,
                   long firstUnreadBefore, long secondUnreadBefore) {}

    Fixture fixture = transactions.execute(status -> {
      String suffix = UUID.randomUUID().toString().substring(0, 8);
      SystemUser first = testUser("notify-a-" + suffix);
      SystemUser second = testUser("notify-b-" + suffix);
      userRepository.saveAll(List.of(first, second));

      long firstBefore = notificationReadRepository.countUnreadForUser(first.getId());
      long secondBefore = notificationReadRepository.countUnreadForUser(second.getId());
      SystemNotification notification = new SystemNotification();
      notification.setType("TEST");
      notification.setTitle("Global notification");
      notification.setContent("Read receipts must be isolated");
      notification.setRead(false);
      notificationRepository.save(notification);
      return new Fixture(first.getId(), second.getId(), notification.getId(), firstBefore, secondBefore);
    });

    transactions.executeWithoutResult(status -> {
      assertThat(notificationReadRepository.countUnreadForUser(fixture.firstUserId()))
          .isEqualTo(fixture.firstUnreadBefore() + 1);
      assertThat(notificationReadRepository.countUnreadForUser(fixture.secondUserId()))
          .isEqualTo(fixture.secondUnreadBefore() + 1);

      SystemNotificationRead receipt = new SystemNotificationRead();
      receipt.setNotificationId(fixture.notificationId());
      receipt.setUserId(fixture.firstUserId());
      receipt.setReadAt(java.time.OffsetDateTime.now());
      notificationReadRepository.save(receipt);
    });

    transactions.executeWithoutResult(status -> {
      assertThat(notificationReadRepository.countUnreadForUser(fixture.firstUserId()))
          .isEqualTo(fixture.firstUnreadBefore());
      assertThat(notificationReadRepository.countUnreadForUser(fixture.secondUserId()))
          .isEqualTo(fixture.secondUnreadBefore() + 1);
    });
  }

  private SystemUser testUser(String username) {
    SystemUser user = new SystemUser();
    user.setUsername(username);
    user.setDisplayName(username);
    user.setPasswordHash("not-used");
    user.setEnabled(true);
    return user;
  }

  @Test
  void enforcesAuthenticationAndCompletesLoginFlow() throws Exception {
    var apiDocs = rest.getForEntity("http://localhost:" + port + "/api-docs", String.class);
    assertThat(apiDocs.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(apiDocs.getBody()).contains("\"openapi\"", "\"paths\"");

    var anonymous = rest.getForEntity("http://localhost:" + port + "/api/auth/me", String.class);
    assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    var anonymousWorkbench = rest.getForEntity(
        "http://localhost:" + port + "/api/mobile/workbench",
        String.class
    );
    assertThat(anonymousWorkbench.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    var loginHeaders = new HttpHeaders();
    loginHeaders.setOrigin("http://localhost:5180");
    var login = rest.postForEntity(
        "http://localhost:" + port + "/api/auth/login",
        new HttpEntity<>(Map.of("username", "admin", "password", "Admin@123"), loginHeaders),
        String.class
    );
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(login.getBody()).contains("\"success\":true", "\"token\"");
    assertThat(login.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5180");
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

    var workbench = rest.exchange(
        "http://localhost:" + port + "/api/mobile/workbench",
        org.springframework.http.HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class
    );
    assertThat(workbench.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(workbench.getBody()).contains(
        "\"success\":true",
        "\"pendingApprovals\"",
        "\"unreadNotifications\"",
        "\"activeWorkOrders\""
    );

    var mobileWorkOrders = rest.exchange(
        "http://localhost:" + port + "/api/maintenance/mobile/work-orders",
        org.springframework.http.HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class
    );
    assertThat(mobileWorkOrders.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(mobileWorkOrders.getBody()).contains("\"success\":true", "\"data\"");

    var mobileApprovals = rest.exchange(
        "http://localhost:" + port + "/api/mobile/approvals?size=1000",
        org.springframework.http.HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class
    );
    assertThat(mobileApprovals.getStatusCode()).isEqualTo(HttpStatus.OK);
    var approvalsPage = objectMapper.readTree(mobileApprovals.getBody()).path("data");
    assertThat(approvalsPage.path("size").asInt()).isEqualTo(200);
    assertThat(approvalsPage.path("content").isArray()).isTrue();
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

  @Test
  void executesOptimizedBiQueriesAndValidatesDateRange() {
    var dashboard = biService.dashboard();
    assertThat(dashboard.summary()).isNotNull();
    assertThat(dashboard.monthlyTrends()).hasSize(6);

    LocalDate end = LocalDate.now();
    var companyDashboard = biService.companyDashboard(end.minusMonths(1), end);
    assertThat(companyDashboard.startDate()).isEqualTo(end.minusMonths(1));
    assertThat(companyDashboard.endDate()).isEqualTo(end);
    assertThat(companyDashboard.trends()).hasSize(6);
    assertThatThrownBy(() -> biService.companyDashboard(end, end.minusDays(1)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("开始日期");
  }

  @Test
  void isolatesOrganizationPermissionAndRoleServiceFlowsByTenant() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    UUID tenantAPermission;
    UUID tenantAOrganization;
    try (var ignored = TenantContext.use("service-tenant-a")) {
      tenantAOrganization = organizationService.createOrganization(new CreateOrganizationRequest(
          "ORG-" + suffix, "租户A组织", "DEPARTMENT", 1, null, null, null, true, null)).id();
      tenantAPermission = permissionService.createPermission(new CreatePermissionRequest(
          "tenant:test:" + suffix, "租户A权限", "test")).id();
      var role = roleService.createRole(new CreateRoleRequest(
          "ROLE-" + suffix, "租户A角色", "CUSTOM", List.of(tenantAPermission), List.of(tenantAOrganization)));
      assertThat(role.permissions()).extracting(item -> item.id()).containsExactly(tenantAPermission);
      assertThat(role.dataOrganizations()).extracting(item -> item.id()).containsExactly(tenantAOrganization);
    }

    try (var ignored = TenantContext.use("service-tenant-b")) {
      var tenantBOrganization = organizationService.createOrganization(new CreateOrganizationRequest(
          "ORG-" + suffix, "租户B组织", "DEPARTMENT", 1, null, null, null, true, null));
      var tenantBPermission = permissionService.createPermission(new CreatePermissionRequest(
          "tenant:test:" + suffix, "租户B权限", "test"));
      assertThat(tenantBOrganization.id()).isNotEqualTo(tenantAOrganization);
      assertThat(tenantBPermission.id()).isNotEqualTo(tenantAPermission);
      assertThatThrownBy(() -> roleService.createRole(new CreateRoleRequest(
          "CROSS-" + suffix, "跨租户角色", "ALL", List.of(tenantAPermission), List.of())))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("权限不存在");
    }
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
