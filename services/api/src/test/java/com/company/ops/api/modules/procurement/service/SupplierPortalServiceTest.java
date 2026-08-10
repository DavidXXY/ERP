package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiry;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiryInvitation;
import com.company.ops.api.modules.procurement.domain.ProcurementContract;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierPortalAccount;
import com.company.ops.api.modules.procurement.domain.SupplierQuotation;
import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import com.company.ops.api.modules.procurement.repository.ProcurementContractRepository;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.UpdateAccountStatusRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.OpenAccountRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.UpdateProfileRequest;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryInvitationRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRequestRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.InquiryClarificationRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementOrderDocumentRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementShipmentRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalAccountRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalDocumentRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalNotificationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuoteAttachmentRepository;
import com.company.ops.api.modules.procurement.repository.SupplierChangeRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationLineRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRevisionRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPerformanceReviewRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.procurement.security.SupplierPortalPrincipal;
import com.company.ops.api.modules.procurement.domain.SupplierChangeRequest;
import com.company.ops.api.modules.procurement.domain.SupplierPerformanceReview;
import com.company.ops.api.modules.procurement.domain.SupplierQuotationRevision;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.RegisterRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.PortalChangeRequest;
import com.company.ops.api.modules.system.security.JwtService;
import com.company.ops.api.modules.system.security.LoginAttemptService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
  @Mock private ProcurementContractRepository contracts;
  @Mock private ProcurementOrderDocumentRepository orderDocuments;
  @Mock private PurchaseOrderRepository orders;
  @Mock private SupplierPortalNotificationRepository notifications;
  @Mock private ProcurementShipmentRepository shipments;
  @Mock private SupplierChangeRequestRepository supplierChanges;
  @Mock private SupplierPerformanceReviewRepository performanceReviews;
  @Mock private SupplierPortalNotifier notifier;
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
  void supplierProfileUpdateCannotChangeProcurementCategory() {
    Fixture fixture = fixture();
    fixture.supplier.setName("原供应商");
    fixture.supplier.setUnifiedSocialCreditCode("91310000TEST000001");
    fixture.supplier.setCategory("采购指定分类");
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    when(suppliers.save(fixture.supplier)).thenReturn(fixture.supplier);
    when(accounts.save(fixture.account)).thenReturn(fixture.account);

    service.updateProfile(fixture.principal, new UpdateProfileRequest(
        "原供应商", "供应商自行修改的分类", "联系人", "13800000000", null,
        "91310000TEST000001", null, null, null, null, null, null, null, null,
        null));

    assertThat(fixture.supplier.getCategory()).isEqualTo("采购指定分类");
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

  @Test
  void awardedInquiryShowsAwardStatusAndContractInPortalList() {
    Fixture fixture = fixture();
    fixture.inquiry.setStatus("AWARDED");
    fixture.inquiry.setSelectedAt(OffsetDateTime.now().minusDays(1));
    SupplierQuotation quote = quote(fixture, "SUPPLIER_PORTAL", "SUBMITTED");
    fixture.inquiry.setSelectedQuoteId(quote.getId());
    quote.setSelected(true);
    ProcurementContract contract = new ProcurementContract();
    contract.setId(UUID.randomUUID());
    contract.setContractNo("HT-2026-001");
    contract.setName("设备采购合同");
    contract.setSupplierId(fixture.supplier.getId());
    contract.setInquiryId(fixture.inquiry.getId());
    contract.setSelectedQuoteId(quote.getId());
    contract.setAmount(java.math.BigDecimal.valueOf(200));
    contract.setStatus("ACTIVE");
    contract.setApprovalStatus("APPROVED");
    when(invitations.findBySupplierIdOrderByInvitedAtDesc(fixture.supplier.getId()))
        .thenReturn(List.of(fixture.invitation));
    when(inquiries.findById(fixture.inquiry.getId())).thenReturn(Optional.of(fixture.inquiry));
    when(inquiryRequests.findByInquiryIdOrderByCreatedAtAsc(fixture.inquiry.getId()))
        .thenReturn(List.of());
    when(purchaseRequests.findAllById(any())).thenReturn(List.of());
    when(quotes.findByInquiryIdAndSupplierId(fixture.inquiry.getId(), fixture.supplier.getId()))
        .thenReturn(Optional.of(quote));
    when(quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId())).thenReturn(List.of());
    when(contracts.findFirstByInquiryIdAndSupplierIdOrderByCreatedAtDesc(
        fixture.inquiry.getId(), fixture.supplier.getId())).thenReturn(Optional.of(contract));

    List<Map<String, Object>> views = service.listInquiries(fixture.principal);

    Map<String, Object> view = views.get(0);
    assertThat(view.get("awardStatus")).isEqualTo("AWARDED");
    assertThat(view.get("awardedAt")).isEqualTo(fixture.inquiry.getSelectedAt());
    @SuppressWarnings("unchecked")
    Map<String, Object> contractView = (Map<String, Object>) view.get("contract");
    assertThat(contractView).isNotNull();
    assertThat(contractView.get("contractNo")).isEqualTo("HT-2026-001");
    assertThat(contractView.get("status")).isEqualTo("ACTIVE");
  }

  @Test
  void nonWinningSupplierSeesNotAwardedStatusWithoutContract() {
    Fixture fixture = fixture();
    fixture.inquiry.setStatus("AWARDED");
    fixture.inquiry.setSelectedAt(OffsetDateTime.now());
    SupplierQuotation quote = quote(fixture, "SUPPLIER_PORTAL", "SUBMITTED");
    fixture.inquiry.setSelectedQuoteId(UUID.randomUUID());
    quote.setSelected(false);
    when(invitations.findBySupplierIdOrderByInvitedAtDesc(fixture.supplier.getId()))
        .thenReturn(List.of(fixture.invitation));
    when(inquiries.findById(fixture.inquiry.getId())).thenReturn(Optional.of(fixture.inquiry));
    when(inquiryRequests.findByInquiryIdOrderByCreatedAtAsc(fixture.inquiry.getId()))
        .thenReturn(List.of());
    when(purchaseRequests.findAllById(any())).thenReturn(List.of());
    when(quotes.findByInquiryIdAndSupplierId(fixture.inquiry.getId(), fixture.supplier.getId()))
        .thenReturn(Optional.of(quote));
    when(quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId())).thenReturn(List.of());

    List<Map<String, Object>> views = service.listInquiries(fixture.principal);

    Map<String, Object> view = views.get(0);
    assertThat(view.get("awardStatus")).isEqualTo("NOT_AWARDED");
    assertThat(view.get("awardedAt")).isNull();
    assertThat(view.get("contract")).isNull();
  }

  @Test
  void registrationRejectsHoneypotFieldBeforeCreatingAccount() {
    assertThatThrownBy(() -> service.register(new RegisterRequest(
        "机器人供应商", "91310000TEST000002", null, "联系人", "bot@example.com",
        "13800000000", "password123", null, null, null, "http://spam.example"), "1.2.3.4"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("稍后重试");
    verify(accounts, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void registrationCountsFailedAttemptWhenEmailAlreadyRegistered() {
    Fixture fixture = fixture();
    when(accounts.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

    assertThatThrownBy(() -> service.register(new RegisterRequest(
        "重复供应商", "91310000TEST000002", null, "联系人", "dup@example.com",
        "13800000000", "password123", null, null, null, null), "1.2.3.4"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已经注册");
    verify(loginAttempts).assertAllowed("supplier-reg|dup@example.com|1.2.3.4");
    verify(loginAttempts).failed("supplier-reg|dup@example.com|1.2.3.4");
  }

  @Test
  void supplierCanSubmitInformationChangeRequest() {
    Fixture fixture = fixture();
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    when(supplierChanges.existsBySupplierIdAndStatus(fixture.supplier.getId(), "PENDING"))
        .thenReturn(false);
    SupplierChangeRequest saved = new SupplierChangeRequest();
    saved.setId(UUID.randomUUID());
    saved.setSupplierId(fixture.supplier.getId());
    saved.setChangeType("BANK_INFO");
    saved.setProposedBankName("招商银行上海分行");
    saved.setReason("开户网点变更");
    saved.setRequestedByName("供应商联系人");
    saved.setRequestSource("PORTAL");
    saved.setStatus("PENDING");
    when(supplierChanges.save(any(SupplierChangeRequest.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.createChangeRequest(fixture.principal,
        new PortalChangeRequest("BANK_INFO", null, null, "招商银行上海分行",
            "6222000000000000", null, "开户网点变更"));

    assertThat(result.status()).isEqualTo("PENDING");
    assertThat(result.requestedByName()).isEqualTo("供应商联系人");
    assertThat(result.proposedBankName()).isEqualTo("招商银行上海分行");
  }

  @Test
  void pendingChangeRequestBlocksSecondSubmission() {
    Fixture fixture = fixture();
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    when(supplierChanges.existsBySupplierIdAndStatus(fixture.supplier.getId(), "PENDING"))
        .thenReturn(true);

    assertThatThrownBy(() -> service.createChangeRequest(fixture.principal,
        new PortalChangeRequest("NAME", "新企业名称", null, null, null, null, "企业改名")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("待审批");
  }

  @Test
  void portalListsOwnQuoteRevisionSnapshots() throws Exception {
    Fixture fixture = fixture();
    SupplierQuotation quote = quote(fixture, "SUPPLIER_PORTAL", "SUBMITTED");
    SupplierQuotationRevision revision = new SupplierQuotationRevision();
    revision.setId(UUID.randomUUID());
    revision.setQuoteId(quote.getId());
    revision.setVersionNo(1);
    revision.setSubmissionSource("SUPPLIER_PORTAL");
    revision.setSubmittedByName("供应商联系人");
    revision.setSubmittedAt(OffsetDateTime.now());
    revision.setSnapshotJson("{\"versionNo\":1}");
    when(invitations.findByInquiryIdAndSupplierId(fixture.inquiry.getId(), fixture.supplier.getId()))
        .thenReturn(Optional.of(fixture.invitation));
    when(quotes.findByInquiryIdAndSupplierId(fixture.inquiry.getId(), fixture.supplier.getId()))
        .thenReturn(Optional.of(quote));
    when(revisions.findByQuoteIdOrderByVersionNoDesc(quote.getId())).thenReturn(List.of(revision));
    org.mockito.Mockito.doReturn(Map.of("versionNo", 1))
        .when(objectMapper).readValue(anyString(), any(TypeReference.class));

    var result = service.listQuoteRevisions(fixture.principal, fixture.inquiry.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).versionNo()).isEqualTo(1);
    assertThat(result.get(0).snapshot().get("versionNo")).isEqualTo(1);
  }

  @Test
  void portalSeesOwnPerformanceReviews() {
    Fixture fixture = fixture();
    SupplierPerformanceReview review = new SupplierPerformanceReview();
    review.setId(UUID.randomUUID());
    review.setSupplierId(fixture.supplier.getId());
    review.setReviewPeriod("2026-Q2");
    review.setGrade("B");
    review.setTotalScore(java.math.BigDecimal.valueOf(88));
    review.setReviewerName("采购经理");
    review.setStatus("ACTIVE");
    when(performanceReviews.findBySupplierIdOrderByReviewPeriodDesc(fixture.supplier.getId()))
        .thenReturn(List.of(review));

    var result = service.listPerformanceReviews(fixture.principal);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).reviewPeriod()).isEqualTo("2026-Q2");
    assertThat(result.get(0).totalScore()).isEqualByComparingTo("88");
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
