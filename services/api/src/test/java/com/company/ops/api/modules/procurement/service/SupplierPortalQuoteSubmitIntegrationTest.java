package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.config.TenantConfig;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiry;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiryInvitation;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiryRequest;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierPortalAccount;
import com.company.ops.api.modules.procurement.domain.SupplierQuotation;
import com.company.ops.api.modules.procurement.domain.SupplierQuotationRevision;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.SaveQuoteLineRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.SaveQuoteRequest;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryInvitationRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalAccountRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationLineRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRevisionRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.procurement.security.SupplierPortalPrincipal;
import com.company.ops.api.modules.system.security.JwtService;
import com.company.ops.api.modules.system.security.LoginAttemptService;
import com.company.ops.api.modules.system.security.TotpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:quote_flow;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@Sql(statements =
    "alter table procurement_supplier_quote_lines "
        + "add constraint uk_proc_quote_request_line unique (tenant_id, quote_id, request_id)")
@Import({SupplierPortalService.class, TenantConfig.class,
    SupplierPortalQuoteSubmitIntegrationTest.JacksonConfig.class})
class SupplierPortalQuoteSubmitIntegrationTest {
  @TestConfiguration
  static class JacksonConfig {
    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackages = {"com.company.ops.api.modules.procurement.domain"})
  @EnableJpaRepositories(basePackages = {"com.company.ops.api.modules.procurement.repository"})
  static class TestApplication {}

  @MockBean private SupplierPortalNotifier notifier;
  @MockBean private ProcurementInternalNotifier internalNotifier;
  @MockBean private SupplierPortalEmailService emails;
  @MockBean private TotpService totpService;
  @MockBean private PasswordEncoder passwordEncoder;
  @MockBean private JwtService jwtService;
  @MockBean private LoginAttemptService loginAttempts;
  @MockBean private CodeGenerator codeGenerator;
  @MockBean private FileStorageService storage;

  @Autowired private SupplierPortalService service;
  @Autowired private SupplierPortalAccountRepository accounts;
  @Autowired private SupplierRepository suppliers;
  @Autowired private ProcurementInquiryRepository inquiries;
  @Autowired private ProcurementInquiryInvitationRepository invitations;
  @Autowired private ProcurementInquiryRequestRepository inquiryRequests;
  @Autowired private SupplierQuotationRepository quotes;
  @Autowired private SupplierQuotationLineRepository quoteLines;
  @Autowired private SupplierQuotationRevisionRepository revisions;

  @Test
  void submittingQuoteAfterDraftReplacesLinesAndStoresRevisionSnapshot() {
    UUID requestId = UUID.randomUUID();
    Supplier supplier = new Supplier();
    supplier.setCode("SUP-FLOW");
    supplier.setName("链路测试供应商");
    supplier.setAdmissionStatus("APPROVED");
    supplier = suppliers.save(supplier);

    SupplierPortalAccount account = new SupplierPortalAccount();
    account.setSupplierId(supplier.getId());
    account.setEmail("flow@example.com");
    account.setContactName("流程测试联系人");
    account.setPasswordHash("x");
    account.setStatus("ACTIVE");
    account.setMustChangePassword(false);
    account = accounts.save(account);

    ProcurementInquiry inquiry = new ProcurementInquiry();
    inquiry.setCode("XJ-FLOW");
    inquiry.setRequestId(requestId);
    inquiry.setTitle("链路测试询价");
    inquiry.setDeadline(LocalDate.now().plusDays(7));
    inquiry = inquiries.save(inquiry);

    ProcurementInquiryInvitation invitation = new ProcurementInquiryInvitation();
    invitation.setInquiryId(inquiry.getId());
    invitation.setSupplierId(supplier.getId());
    invitation.setStatus("VIEWED");
    invitation.setInvitedAt(OffsetDateTime.now());
    invitations.save(invitation);

    ProcurementInquiryRequest link = new ProcurementInquiryRequest();
    link.setInquiryId(inquiry.getId());
    link.setRequestId(requestId);
    link.setRequestedQty(new BigDecimal("2"));
    inquiryRequests.save(link);

    SaveQuoteRequest request = new SaveQuoteRequest(
        List.of(new SaveQuoteLineRequest(
            requestId, new BigDecimal("13600"), new BigDecimal("13"),
            LocalDate.now().plusDays(30), "含税含运")),
        "货到验收后30天付款", "链路测试报价", "CNY",
        BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusMonths(2));

    SupplierPortalPrincipal principal = new SupplierPortalPrincipal(
        account.getId(), supplier.getId(), "default", account.getEmail(),
        account.getContactName(), account.getStatus(), account.getAuthVersion());

    var draft = service.saveQuote(principal, inquiry.getId(), request, false);
    assertThat(draft.get("status")).isEqualTo("DRAFT");

    var submitted = service.saveQuote(principal, inquiry.getId(), request, true);
    assertThat(submitted.get("status")).isEqualTo("SUBMITTED");
    assertThat(submitted.get("versionNo")).isEqualTo(1);

    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiry.getId(), supplier.getId()).orElseThrow();
    assertThat(quote.getSubmissionStatus()).isEqualTo("SUBMITTED");
    assertThat(quote.getSubmittedAt()).isNotNull();
    assertThat(quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId())).hasSize(1);

    List<SupplierQuotationRevision> revisionsList = revisions.findByQuoteIdOrderByVersionNoDesc(quote.getId());
    assertThat(revisionsList).hasSize(1);
    assertThat(revisionsList.get(0).getSnapshotJson()).contains("\"status\":\"SUBMITTED\"");

    assertThat(invitations.findByInquiryIdAndSupplierId(inquiry.getId(), supplier.getId()).orElseThrow()
        .getStatus()).isEqualTo("RESPONDED");
  }
}
