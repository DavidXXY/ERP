package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiry;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiryInvitation;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierPortalAccount;
import com.company.ops.api.modules.procurement.domain.SupplierQuotation;
import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.UpdateAccountStatusRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.OpenAccountRequest;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryInvitationRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRequestRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.InquiryClarificationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalAccountRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalDocumentRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuoteAttachmentRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationLineRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRevisionRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.procurement.security.SupplierPortalPrincipal;
import com.company.ops.api.modules.system.security.JwtService;
import com.company.ops.api.modules.system.security.LoginAttemptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SupplierPortalServiceTest {
  @Mock private SupplierPortalAccountRepository accounts;
  @Mock private SupplierPortalDocumentRepository documents;
  @Mock private SupplierRepository suppliers;
  @Mock private ProcurementInquiryInvitationRepository invitations;
  @Mock private ProcurementInquiryRepository inquiries;
  @Mock private ProcurementInquiryRequestRepository inquiryRequests;
  @Mock private PurchaseRequestRepository purchaseRequests;
  @Mock private SupplierQuotationRepository quotes;
  @Mock private SupplierQuotationLineRepository quoteLines;
  @Mock private SupplierQuotationRevisionRepository revisions;
  @Mock private SupplierQuoteAttachmentRepository quoteAttachments;
  @Mock private InquiryClarificationRepository clarifications;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private LoginAttemptService loginAttempts;
  @Mock private CodeGenerator codeGenerator;
  @Mock private FileStorageService storage;
  @Mock private ObjectMapper objectMapper;
  @InjectMocks private SupplierPortalService service;

  @Test
  void viewingInquiryAdvancesInvitationStatus() {
    Fixture fixture = fixture();
    fixture.invitation.setStatus("INVITED");
    when(invitations.findBySupplierIdOrderByInvitedAtDesc(fixture.supplier.getId()))
        .thenReturn(List.of(fixture.invitation));
    when(inquiries.findById(fixture.inquiry.getId())).thenReturn(Optional.of(fixture.inquiry));
    when(inquiryRequests.findByInquiryIdOrderByCreatedAtAsc(fixture.inquiry.getId()))
        .thenReturn(List.of());
    when(purchaseRequests.findById(fixture.inquiry.getRequestId())).thenReturn(Optional.empty());
    when(purchaseRequests.findAllById(any())).thenReturn(List.of());
    when(quotes.findByInquiryIdAndSupplierId(fixture.inquiry.getId(), fixture.supplier.getId()))
        .thenReturn(Optional.empty());

    service.listInquiries(fixture.principal);

    assertThat(fixture.invitation.getStatus()).isEqualTo("VIEWED");
    assertThat(fixture.invitation.getViewedAt()).isNotNull();
    verify(invitations).saveAll(List.of(fixture.invitation));
  }

  @Test
  void withdrawingSupplierQuoteReopensInvitation() {
    Fixture fixture = fixture();
    stubActive(fixture);
    fixture.invitation.setStatus("RESPONDED");
    fixture.invitation.setViewedAt(OffsetDateTime.now().minusHours(1));
    fixture.invitation.setRespondedAt(OffsetDateTime.now());
    SupplierQuotation quote = quote(fixture, "SUPPLIER_PORTAL", "SUBMITTED");
    when(invitations.findByInquiryIdAndSupplierId(fixture.inquiry.getId(), fixture.supplier.getId()))
        .thenReturn(Optional.of(fixture.invitation));
    when(quotes.findByInquiryIdAndSupplierId(fixture.inquiry.getId(), fixture.supplier.getId()))
        .thenReturn(Optional.of(quote));
    when(quotes.save(quote)).thenReturn(quote);
    when(quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId())).thenReturn(List.of());

    service.withdrawQuote(fixture.principal, fixture.inquiry.getId());

    assertThat(quote.getSubmissionStatus()).isEqualTo("DRAFT");
    assertThat(quote.getVersionNo()).isEqualTo(2);
    assertThat(fixture.invitation.getStatus()).isEqualTo("VIEWED");
    assertThat(fixture.invitation.getRespondedAt()).isNull();
    verify(invitations).save(fixture.invitation);
  }

  @Test
  void confirmingInternalQuoteMarksInvitationResponded() {
    Fixture fixture = fixture();
    stubActive(fixture);
    fixture.invitation.setStatus("VIEWED");
    SupplierQuotation quote = quote(fixture, "INTERNAL_ENTRY", "SUBMITTED");
    when(invitations.findByInquiryIdAndSupplierId(fixture.inquiry.getId(), fixture.supplier.getId()))
        .thenReturn(Optional.of(fixture.invitation));
    when(quotes.findByInquiryIdAndSupplierId(fixture.inquiry.getId(), fixture.supplier.getId()))
        .thenReturn(Optional.of(quote));
    when(quotes.save(quote)).thenReturn(quote);
    when(quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId())).thenReturn(List.of());

    service.confirmInternalQuote(fixture.principal, fixture.inquiry.getId());

    assertThat(quote.getConfirmedByAccountId()).isEqualTo(fixture.account.getId());
    assertThat(quote.getConfirmedAt()).isNotNull();
    assertThat(fixture.invitation.getStatus()).isEqualTo("RESPONDED");
    assertThat(fixture.invitation.getRespondedAt()).isEqualTo(quote.getConfirmedAt());
    verify(invitations).save(fixture.invitation);
  }

  @Test
  void suspendingPortalAccountInvalidatesExistingTokens() {
    Fixture fixture = fixture();
    long previousAuthVersion = fixture.account.getAuthVersion();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    when(accounts.save(fixture.account)).thenReturn(fixture.account);

    service.updateAccountStatus(fixture.account.getId(),
        new UpdateAccountStatusRequest("SUSPENDED", "风险复核"));

    assertThat(fixture.account.getStatus()).isEqualTo("SUSPENDED");
    assertThat(fixture.account.getAuthVersion()).isEqualTo(previousAuthVersion + 1);
    verify(accounts).save(fixture.account);
  }

  @Test
  void procurementManagerCanOpenAccountForExistingSupplier() {
    Fixture fixture = fixture();
    fixture.supplier.setPhone("13800000000");
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    when(accounts.existsBySupplierId(fixture.supplier.getId())).thenReturn(false);
    when(accounts.existsByEmailIgnoreCase("buyer@example.com")).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("encoded-password");
    when(accounts.save(any(SupplierPortalAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.openAccount(fixture.supplier.getId(),
        new OpenAccountRequest(" Buyer@Example.com ", null, "供应商联系人"));

    assertThat(result.temporaryPassword()).startsWith("Tmp").endsWith("!");
    assertThat(result.account().email()).isEqualTo("buyer@example.com");
    assertThat(result.account().phone()).isEqualTo("13800000000");
    assertThat(result.account().status()).isEqualTo("ACTIVE");
    assertThat(result.account().mustChangePassword()).isTrue();
  }

  @Test
  void openingSecondAccountForSupplierIsRejected() {
    Fixture fixture = fixture();
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    when(accounts.existsBySupplierId(fixture.supplier.getId())).thenReturn(true);

    assertThatThrownBy(() -> service.openAccount(fixture.supplier.getId(),
        new OpenAccountRequest("buyer@example.com", null, "供应商联系人")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已经开通门户账号");
  }

  @Test
  void temporaryPasswordMustBeChangedBeforeSupplierCanConfirmQuote() {
    Fixture fixture = fixture();
    fixture.account.setMustChangePassword(true);
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));

    assertThatThrownBy(() -> service.confirmInternalQuote(fixture.principal, fixture.inquiry.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("请先修改临时密码");
  }

  private Fixture fixture() {
    SupplierPortalAccount account = new SupplierPortalAccount();
    account.setId(UUID.randomUUID());
    account.setStatus("ACTIVE");
    Supplier supplier = new Supplier();
    supplier.setId(UUID.randomUUID());
    supplier.setAdmissionStatus("APPROVED");
    supplier.setRiskStatus(SupplierRiskStatus.NORMAL);
    account.setSupplierId(supplier.getId());
    ProcurementInquiry inquiry = new ProcurementInquiry();
    inquiry.setId(UUID.randomUUID());
    inquiry.setRequestId(UUID.randomUUID());
    inquiry.setStatus("OPEN");
    inquiry.setDeadline(LocalDate.now().plusDays(1));
    ProcurementInquiryInvitation invitation = new ProcurementInquiryInvitation();
    invitation.setId(UUID.randomUUID());
    invitation.setInquiryId(inquiry.getId());
    invitation.setSupplierId(supplier.getId());
    invitation.setInvitedAt(OffsetDateTime.now().minusDays(1));
    SupplierPortalPrincipal principal = new SupplierPortalPrincipal(
        account.getId(), supplier.getId(), "default", "supplier@example.com", "供应商联系人", "ACTIVE", 0);
    return new Fixture(account, supplier, inquiry, invitation, principal);
  }

  private void stubActive(Fixture fixture) {
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    when(inquiries.findById(fixture.inquiry.getId())).thenReturn(Optional.of(fixture.inquiry));
  }

  private SupplierQuotation quote(Fixture fixture, String source, String status) {
    SupplierQuotation quote = new SupplierQuotation();
    quote.setId(UUID.randomUUID());
    quote.setInquiryId(fixture.inquiry.getId());
    quote.setSupplierId(fixture.supplier.getId());
    quote.setSubmissionSource(source);
    quote.setSubmissionStatus(status);
    quote.setVersionNo(1);
    return quote;
  }

  private record Fixture(
      SupplierPortalAccount account,
      Supplier supplier,
      ProcurementInquiry inquiry,
      ProcurementInquiryInvitation invitation,
      SupplierPortalPrincipal principal
  ) {}
}
