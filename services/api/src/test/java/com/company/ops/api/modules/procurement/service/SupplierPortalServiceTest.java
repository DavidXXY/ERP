package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiry;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiryInvitation;
import com.company.ops.api.modules.procurement.domain.ProcurementContract;
import com.company.ops.api.modules.procurement.domain.ProcurementShipment;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierPortalAccount;
import com.company.ops.api.modules.procurement.domain.SupplierQuotation;
import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import com.company.ops.api.modules.procurement.repository.ProcurementContractRepository;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.CreateShipmentRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.UpdateAccountStatusRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.OpenAccountRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.UpdateProfileRequest;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryInvitationRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRequestRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.InquiryClarificationRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementOrderDocumentRepository;
import com.company.ops.api.modules.procurement.repository.GoodsReceiptRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderChangeRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementShipmentRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalAccountRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalDocumentRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalNotificationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoiceRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoiceSubmissionRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalActivityRepository;
import com.company.ops.api.modules.procurement.repository.SupplierShipmentAttachmentRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuoteAttachmentRepository;
import com.company.ops.api.modules.procurement.repository.SupplierChangeRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationLineRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRevisionRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPerformanceReviewRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.procurement.domain.SupplierShipmentAttachment;
import com.company.ops.api.modules.procurement.security.SupplierPortalPrincipal;
import com.company.ops.api.modules.procurement.domain.SupplierChangeRequest;
import com.company.ops.api.modules.procurement.domain.SupplierPerformanceReview;
import com.company.ops.api.modules.procurement.domain.SupplierQuotationRevision;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.RegisterRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.SubmitInvoiceRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.InvoiceSubmissionResponse;
import com.company.ops.api.modules.procurement.domain.SupplierInvoiceSubmission;
import com.company.ops.api.modules.procurement.domain.SupplierInvoice;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.PortalChangeRequest;
import com.company.ops.api.modules.system.security.JwtService;
import com.company.ops.api.modules.system.security.LoginAttemptService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
  @Mock private GoodsReceiptRepository receipts;
  @Mock private PurchaseOrderChangeRepository orderChanges;
  @Mock private SupplierPortalNotificationRepository notifications;
  @Mock private ProcurementShipmentRepository shipments;
  @Mock private SupplierChangeRequestRepository supplierChanges;
  @Mock private SupplierPerformanceReviewRepository performanceReviews;
  @Mock private SupplierInvoiceRepository invoices;
  @Mock private SupplierInvoiceSubmissionRepository invoiceSubmissions;
  @Mock private ProcurementPayableRepository payables;
  @Mock private SupplierPortalActivityRepository activities;
  @Mock private SupplierShipmentAttachmentRepository shipmentAttachments;
  @Mock private SupplierPortalNotifier notifier;
  @Mock private ProcurementInternalNotifier internalNotifier;
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

  @Test
  void portalListsOwnPurchaseOrdersWithContractAndShipments() {
    Fixture fixture = fixture();
    fixture.inquiry.setStatus("AWARDED");
    fixture.inquiry.setSelectedAt(OffsetDateTime.now().minusDays(2));
    fixture.inquiry.setCode("XJ-2026-0001");
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-2026-0001");
    order.setSupplierId(fixture.supplier.getId());
    order.setInquiryId(fixture.inquiry.getId());
    order.setPartName("减速电机");
    order.setOrderedQty(BigDecimal.TEN);
    order.setUnitPrice(BigDecimal.valueOf(120));
    order.setOrderAmount(BigDecimal.valueOf(1200));
    order.setStatus(PurchaseOrderStatus.ORDERED);
    order.setApprovalStatus(ApprovalStatus.APPROVED);
    order.setCreatedAt(OffsetDateTime.now().minusDays(1));
    ProcurementContract contract = new ProcurementContract();
    contract.setId(UUID.randomUUID());
    contract.setContractNo("HT-2026-0001");
    contract.setName("减速电机采购合同");
    contract.setSupplierId(fixture.supplier.getId());
    contract.setInquiryId(fixture.inquiry.getId());
    contract.setAmount(BigDecimal.valueOf(1200));
    contract.setStatus("ACTIVE");
    contract.setApprovalStatus("APPROVED");
    contract.setOrderId(order.getId());
    order.setContractId(contract.getId());
    ProcurementShipment shipment = new ProcurementShipment();
    shipment.setId(UUID.randomUUID());
    shipment.setOrderId(order.getId());
    shipment.setSupplierId(fixture.supplier.getId());
    shipment.setDeliveryNo("SF1234567890");
    shipment.setStatus("PENDING");
    shipment.setCreatedAt(OffsetDateTime.now().minusHours(2));
    when(orders.findBySupplierId(fixture.supplier.getId())).thenReturn(List.of(order));
    when(contracts.findBySupplierIdOrderByCreatedAtDesc(fixture.supplier.getId()))
        .thenReturn(List.of(contract));
    when(inquiries.findById(fixture.inquiry.getId())).thenReturn(Optional.of(fixture.inquiry));
    when(orderDocuments.findByOrderIdInOrderByCreatedAtDesc(List.of(order.getId())))
        .thenReturn(List.of());
    when(shipments.findByOrderIdInOrderByCreatedAtDesc(List.of(order.getId())))
        .thenReturn(List.of(shipment));
    when(receipts.findByOrderIdIn(List.of(order.getId()))).thenReturn(List.of());
    when(orderChanges.findByOrderIdInOrderByCreatedAtDesc(List.of(order.getId())))
        .thenReturn(List.of());

    List<Map<String, Object>> entries = service.listMyOrders(fixture.principal);

    assertThat(entries).hasSize(1);
    @SuppressWarnings("unchecked")
    Map<String, Object> orderView = (Map<String, Object>) entries.get(0).get("order");
    assertThat(orderView.get("code")).isEqualTo("PO-2026-0001");
    assertThat(orderView.get("status")).isEqualTo("ORDERED");
    @SuppressWarnings("unchecked")
    Map<String, Object> contractView = (Map<String, Object>) entries.get(0).get("contract");
    assertThat(contractView.get("contractNo")).isEqualTo("HT-2026-0001");
    @SuppressWarnings("unchecked")
    Map<String, Object> inquiryView = (Map<String, Object>) entries.get(0).get("inquiry");
    assertThat(inquiryView.get("code")).isEqualTo("XJ-2026-0001");
    assertThat(entries.get(0).get("shipments")).asList().hasSize(1);
  }

  @Test
  void portalSeesAwardedProjectBeforePurchaseOrderIsPlaced() {
    Fixture fixture = fixture();
    fixture.inquiry.setStatus("AWARDED");
    fixture.inquiry.setSelectedAt(OffsetDateTime.now().minusDays(2));
    ProcurementContract contract = new ProcurementContract();
    contract.setId(UUID.randomUUID());
    contract.setContractNo("HT-2026-0002");
    contract.setName("轴承采购合同");
    contract.setSupplierId(fixture.supplier.getId());
    contract.setInquiryId(fixture.inquiry.getId());
    contract.setAmount(BigDecimal.valueOf(800));
    contract.setStatus("ACTIVE");
    contract.setApprovalStatus("APPROVED");
    when(orders.findBySupplierId(fixture.supplier.getId())).thenReturn(List.of());
    when(contracts.findBySupplierIdOrderByCreatedAtDesc(fixture.supplier.getId()))
        .thenReturn(List.of(contract));
    when(inquiries.findById(fixture.inquiry.getId())).thenReturn(Optional.of(fixture.inquiry));

    List<Map<String, Object>> entries = service.listMyOrders(fixture.principal);

    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).get("order")).isNull();
    @SuppressWarnings("unchecked")
    Map<String, Object> contractView = (Map<String, Object>) entries.get(0).get("contract");
    assertThat(contractView.get("contractNo")).isEqualTo("HT-2026-0002");
    assertThat(entries.get(0).get("shipments")).asList().isEmpty();
  }


  @Test
  void createShipmentRejectsBlankDeliveryNo() {
    Fixture fixture = fixture();
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-2026-0002");
    order.setSupplierId(fixture.supplier.getId());
    order.setStatus(PurchaseOrderStatus.ORDERED);
    when(orders.findById(order.getId())).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> service.createShipment(fixture.principal, order.getId(),
        new CreateShipmentRequest("", "顺丰", null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("送货单号");
  }

  @Test
  void createShipmentRejectsDuplicateDeliveryNo() {
    Fixture fixture = fixture();
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-2026-0003");
    order.setSupplierId(fixture.supplier.getId());
    order.setStatus(PurchaseOrderStatus.ORDERED);
    ProcurementShipment existing = new ProcurementShipment();
    existing.setId(UUID.randomUUID());
    existing.setOrderId(order.getId());
    existing.setDeliveryNo("SF-DUP-001");
    when(orders.findById(order.getId())).thenReturn(Optional.of(order));
    when(shipments.findByOrderIdAndDeliveryNo(order.getId(), "SF-DUP-001"))
        .thenReturn(List.of(existing));

    assertThatThrownBy(() -> service.createShipment(fixture.principal, order.getId(),
        new CreateShipmentRequest("SF-DUP-001", null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("相同的送货单号");
  }

  @Test
  void createShipmentEnforcesPerOrderCap() {
    Fixture fixture = fixture();
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-2026-0004");
    order.setSupplierId(fixture.supplier.getId());
    order.setStatus(PurchaseOrderStatus.ORDERED);
    when(orders.findById(order.getId())).thenReturn(Optional.of(order));
    when(shipments.findByOrderIdAndDeliveryNo(order.getId(), "SF-CAP-001"))
        .thenReturn(List.of());
    when(shipments.countByOrderId(order.getId())).thenReturn(50L);

    assertThatThrownBy(() -> service.createShipment(fixture.principal, order.getId(),
        new CreateShipmentRequest("SF-CAP-001", null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("上限");
  }

  @Test
  void updateShipmentRejectsAfterConfirmed() {
    Fixture fixture = fixture();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-2026-0005");
    order.setSupplierId(fixture.supplier.getId());
    ProcurementShipment shipment = new ProcurementShipment();
    shipment.setId(UUID.randomUUID());
    shipment.setOrderId(order.getId());
    shipment.setSupplierId(fixture.supplier.getId());
    shipment.setStatus("CONFIRMED");
    when(shipments.findById(shipment.getId())).thenReturn(Optional.of(shipment));

    assertThatThrownBy(() -> service.updateShipment(fixture.principal, shipment.getId(),
        new CreateShipmentRequest("SF-EDIT-001", null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("待确认");
  }

  @Test
  void deleteShipmentRemovesAttachmentsAndStorage() {
    Fixture fixture = fixture();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-2026-0006");
    order.setSupplierId(fixture.supplier.getId());
    ProcurementShipment shipment = new ProcurementShipment();
    shipment.setId(UUID.randomUUID());
    shipment.setOrderId(order.getId());
    shipment.setSupplierId(fixture.supplier.getId());
    shipment.setStatus("PENDING");
    SupplierShipmentAttachment attachment = new SupplierShipmentAttachment();
    attachment.setId(UUID.randomUUID());
    attachment.setShipmentId(shipment.getId());
    attachment.setObjectKey("supplier-shipments/obj-1");
    when(shipments.findById(shipment.getId())).thenReturn(Optional.of(shipment));
    when(shipmentAttachments.findByShipmentIdOrderByCreatedAtDesc(shipment.getId()))
        .thenReturn(List.of(attachment));

    service.deleteShipment(fixture.principal, shipment.getId());

    verify(storage).deleteInNamespace("supplier-shipments", "supplier-shipments/obj-1");
    verify(shipmentAttachments).delete(attachment);
    verify(shipments).delete(shipment);
  }

  @Test
  void listNotificationsPaginatesWithBeforeCursor() {
    Fixture fixture = fixture();
    List<com.company.ops.api.modules.procurement.domain.SupplierPortalNotification> items = new ArrayList<>();
    for (int i = 0; i < 101; i++) {
      com.company.ops.api.modules.procurement.domain.SupplierPortalNotification n =
          new com.company.ops.api.modules.procurement.domain.SupplierPortalNotification();
      n.setId(UUID.randomUUID());
      n.setAccountId(fixture.account.getId());
      n.setRead(false);
      items.add(n);
    }
    when(notifications.findTop100ByAccountIdOrderByCreatedAtDesc(fixture.account.getId()))
        .thenReturn(items);

    Map<String, Object> page = service.listNotifications(fixture.principal, null);

    assertThat(((List<?>) page.get("items"))).hasSize(100);
    assertThat(page.get("hasMore")).isEqualTo(true);

    OffsetDateTime before = OffsetDateTime.now();
    com.company.ops.api.modules.procurement.domain.SupplierPortalNotification last =
        new com.company.ops.api.modules.procurement.domain.SupplierPortalNotification();
    last.setId(UUID.randomUUID());
    last.setAccountId(fixture.account.getId());
    when(notifications.findTop100ByAccountIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        fixture.account.getId(), before)).thenReturn(List.of(last));

    Map<String, Object> older = service.listNotifications(fixture.principal, before);

    assertThat(((List<?>) older.get("items"))).hasSize(1);
    assertThat(older.get("hasMore")).isEqualTo(false);
  }

  @Test
  void requestPasswordResetDoesNotRevealUnknownEmail() {
    when(accounts.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

    String result = service.requestPasswordReset("nobody@example.com", "127.0.0.1");

    assertThat(result).isNull();
    verify(accounts, never()).save(any());
  }

  @Test
  void uploadDocumentRejectsOverQuota() {
    Fixture fixture = fixture();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(documents.countBySupplierId(fixture.supplier.getId())).thenReturn(30L);

    assertThatThrownBy(() -> service.uploadDocument(fixture.principal, "BUSINESS_LICENSE",
        null, new org.springframework.mock.web.MockMultipartFile("file", "a.pdf",
            "application/pdf", new byte[]{1, 2, 3})))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("最多");

    verify(storage, never()).store(any(), anyString(), any());
  }

  @Test
  void markAllNotificationsReadUsesBulkUpdate() {
    Fixture fixture = fixture();
    service.markAllNotificationsRead(fixture.principal);
    verify(notifications).markAllRead(
        org.mockito.ArgumentMatchers.eq(fixture.account.getId()), any(OffsetDateTime.class));
  }


  @Test
  void submitInvoiceRejectsAlreadyRegisteredInvoiceNo() {
    Fixture fixture = fixture();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-2026-0099");
    order.setSupplierId(fixture.supplier.getId());
    when(orders.findById(order.getId())).thenReturn(Optional.of(order));
    when(invoices.existsByInvoiceNo("FP-DUP-001")).thenReturn(true);

    assertThatThrownBy(() -> service.submitInvoice(fixture.principal,
        new SubmitInvoiceRequest(order.getId(), "FP-DUP-001", BigDecimal.valueOf(1000),
            new BigDecimal("13"), LocalDate.now(), null),
        new org.springframework.mock.web.MockMultipartFile("file", "a.pdf",
            "application/pdf", new byte[]{1})))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已登记");

    verify(invoiceSubmissions, never()).save(any());
    verify(storage, never()).store(any(), anyString(), any());
  }

  @Test
  void submitInvoiceRejectsDuplicatePendingSubmission() {
    Fixture fixture = fixture();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-2026-0101");
    order.setSupplierId(fixture.supplier.getId());
    when(orders.findById(order.getId())).thenReturn(Optional.of(order));
    when(invoices.existsByInvoiceNo("FP-DUP-002")).thenReturn(false);
    SupplierInvoiceSubmission existing = new SupplierInvoiceSubmission();
    existing.setId(UUID.randomUUID());
    existing.setSupplierId(fixture.supplier.getId());
    existing.setStatus("PENDING");
    when(invoiceSubmissions.findByInvoiceNoIgnoreCaseAndStatus("FP-DUP-002", "PENDING"))
        .thenReturn(List.of(existing));

    assertThatThrownBy(() -> service.submitInvoice(fixture.principal,
        new SubmitInvoiceRequest(order.getId(), "FP-DUP-002", BigDecimal.valueOf(1000),
            new BigDecimal("13"), LocalDate.now(), null),
        new org.springframework.mock.web.MockMultipartFile("file", "a.pdf",
            "application/pdf", new byte[]{1})))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("待审核");

    verify(invoiceSubmissions, never()).save(any());
  }

  @Test
  void submitInvoiceEnforcesSubmissionCap() {
    Fixture fixture = fixture();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-2026-0102");
    order.setSupplierId(fixture.supplier.getId());
    when(orders.findById(order.getId())).thenReturn(Optional.of(order));
    when(invoices.existsByInvoiceNo("FP-CAP-001")).thenReturn(false);
    when(invoiceSubmissions.findByInvoiceNoIgnoreCaseAndStatus("FP-CAP-001", "PENDING"))
        .thenReturn(List.of());
    when(invoiceSubmissions.countBySupplierId(fixture.supplier.getId())).thenReturn(50L);

    assertThatThrownBy(() -> service.submitInvoice(fixture.principal,
        new SubmitInvoiceRequest(order.getId(), "FP-CAP-001", BigDecimal.valueOf(1000),
            new BigDecimal("13"), LocalDate.now(), null),
        new org.springframework.mock.web.MockMultipartFile("file", "a.pdf",
            "application/pdf", new byte[]{1})))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("上限");

    verify(storage, never()).store(any(), anyString(), any());
  }

  @Test
  void submitInvoiceStoresFileAndRecordsActivity() {
    Fixture fixture = fixture();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-2026-0103");
    order.setSupplierId(fixture.supplier.getId());
    when(orders.findById(order.getId())).thenReturn(Optional.of(order));
    when(invoices.existsByInvoiceNo("FP-OK-001")).thenReturn(false);
    when(invoiceSubmissions.findByInvoiceNoIgnoreCaseAndStatus("FP-OK-001", "PENDING"))
        .thenReturn(List.of());
    when(invoiceSubmissions.countBySupplierId(fixture.supplier.getId())).thenReturn(0L);
    when(storage.store(any(), anyString(), any())).thenReturn(
        new FileStorageService.StoredFile("a.pdf", "key-1", "supplier-invoices/key-1",
            "pdf", "application/pdf", 1, java.nio.file.Path.of("/tmp/a.pdf")));
    when(invoiceSubmissions.save(any(SupplierInvoiceSubmission.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    InvoiceSubmissionResponse result = service.submitInvoice(fixture.principal,
        new SubmitInvoiceRequest(order.getId(), "FP-OK-001", BigDecimal.valueOf(1000),
            new BigDecimal("13"), LocalDate.now(), "测试备注"),
        new org.springframework.mock.web.MockMultipartFile("file", "a.pdf",
            "application/pdf", new byte[]{1}));

    assertThat(result.invoiceNo()).isEqualTo("FP-OK-001");
    assertThat(result.status()).isEqualTo("PENDING");
    assertThat(result.orderCode()).isEqualTo("PO-2026-0103");
    verify(invoiceSubmissions).save(any(SupplierInvoiceSubmission.class));
    verify(activities).save(any());
    verify(internalNotifier).notifyProcurementStaff(
        eq("PROCUREMENT"), eq("供应商提交开票资料"),
        org.mockito.ArgumentMatchers.contains("FP-OK-001"),
        eq("INVOICE_SUBMISSION"), org.mockito.ArgumentMatchers.nullable(UUID.class),
        anyString());
  }

  @Test
  void appealPerformanceReviewRequiresOwnedPendingReview() {
    Fixture fixture = fixture();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    SupplierPerformanceReview review = new SupplierPerformanceReview();
    review.setId(UUID.randomUUID());
    review.setSupplierId(UUID.randomUUID());
    review.setAppealStatus("NONE");
    when(performanceReviews.findById(review.getId())).thenReturn(Optional.of(review));

    assertThatThrownBy(() -> service.appealPerformanceReview(
        fixture.principal, review.getId(), "评分依据有误"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("无权");
  }

  @Test
  void appealPerformanceReviewSubmitsAndNotifiesBuyers() {
    Fixture fixture = fixture();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    SupplierPerformanceReview review = new SupplierPerformanceReview();
    review.setId(UUID.randomUUID());
    review.setSupplierId(fixture.supplier.getId());
    review.setReviewPeriod("2026-06");
    review.setTotalScore(BigDecimal.valueOf(88));
    review.setGrade("B");
    review.setAppealStatus("NONE");
    when(performanceReviews.findById(review.getId())).thenReturn(Optional.of(review));
    when(performanceReviews.save(any(SupplierPerformanceReview.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.appealPerformanceReview(
        fixture.principal, review.getId(), "部分交货数据缺失导致评分偏低");

    assertThat(result.appealStatus()).isEqualTo("PENDING");
    assertThat(result.appealReason()).contains("数据缺失");
    verify(internalNotifier).notifyProcurementStaff(
        eq("PROCUREMENT"), eq("供应商发起绩效申诉"), anyString(),
        eq("PERFORMANCE_APPEAL"), any(UUID.class),
        org.mockito.ArgumentMatchers.startsWith("PORTAL_PERFORMANCE_APPEAL:"));
  }

  @Test
  void appealPerformanceReviewRejectsDuplicateAppeal() {
    Fixture fixture = fixture();
    when(accounts.findById(fixture.account.getId())).thenReturn(Optional.of(fixture.account));
    when(suppliers.findById(fixture.supplier.getId())).thenReturn(Optional.of(fixture.supplier));
    SupplierPerformanceReview review = new SupplierPerformanceReview();
    review.setId(UUID.randomUUID());
    review.setSupplierId(fixture.supplier.getId());
    review.setAppealStatus("PENDING");
    when(performanceReviews.findById(review.getId())).thenReturn(Optional.of(review));

    assertThatThrownBy(() -> service.appealPerformanceReview(
        fixture.principal, review.getId(), "再次申诉"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已发起申诉");
  }

  @Test
  void financeSummaryExposesReconciliationMetrics() {
    Fixture fixture = fixture();
    SupplierInvoice approved = invoice("FP-A", "APPROVED", "MATCHED",
        BigDecimal.valueOf(1000), BigDecimal.ZERO);
    SupplierInvoice mismatched = invoice("FP-B", "APPROVED", "MISMATCH",
        BigDecimal.valueOf(500), BigDecimal.valueOf(20));
    SupplierInvoice pending = invoice("FP-C", "PENDING", "UNMATCHED",
        BigDecimal.valueOf(300), null);
    when(invoices.findBySupplierIdOrderByCreatedAtDesc(fixture.supplier.getId()))
        .thenReturn(List.of(pending, mismatched, approved));
    when(invoices.aggregateBySupplier(fixture.supplier.getId()))
        .thenReturn(new SupplierInvoiceRepository.InvoiceSupplierTotals() {
          @Override public Long getInvoiceCount() { return 3L; }
          @Override public BigDecimal getInvoiceAmount() { return BigDecimal.valueOf(1800); }
        });
    when(payables.aggregateBySupplier(any(), any(), any(), any()))
        .thenReturn(new ProcurementPayableRepository.PayableSupplierTotals() {
          @Override public Long getPayableCount() { return 2L; }
          @Override public BigDecimal getPayableAmount() { return BigDecimal.valueOf(1000); }
          @Override public BigDecimal getPaidAmount() { return BigDecimal.valueOf(400); }
          @Override public BigDecimal getOverdueAmount() { return BigDecimal.valueOf(100); }
        });

    Map<String, Object> view = service.financeSummary(fixture.principal);

    assertThat((BigDecimal) view.get("invoiceApprovedAmount")).isEqualByComparingTo("1500");
    assertThat((BigDecimal) view.get("invoiceDifferenceAmount")).isEqualByComparingTo("20");
    assertThat(view.get("pendingInvoiceApprovals")).isEqualTo(1L);
    assertThat(view.get("matchedInvoiceCount")).isEqualTo(1L);
    assertThat((BigDecimal) view.get("outstandingAmount")).isEqualByComparingTo("600");
  }

  @Test
  void deletePendingInvoiceSubmissionRemovesFile() {
    Fixture fixture = fixture();
    SupplierInvoiceSubmission submission = new SupplierInvoiceSubmission();
    submission.setId(UUID.randomUUID());
    submission.setSupplierId(fixture.supplier.getId());
    submission.setStatus("PENDING");
    submission.setObjectKey("key-1");
    when(invoiceSubmissions.findById(submission.getId())).thenReturn(Optional.of(submission));

    service.deleteInvoiceSubmission(fixture.principal, submission.getId());

    verify(invoiceSubmissions).delete(submission);
    verify(storage).deleteInNamespace("supplier-invoices", "key-1");
  }

  @Test
  void deleteReviewedInvoiceSubmissionIsRejected() {
    Fixture fixture = fixture();
    SupplierInvoiceSubmission submission = new SupplierInvoiceSubmission();
    submission.setId(UUID.randomUUID());
    submission.setSupplierId(fixture.supplier.getId());
    submission.setStatus("REJECTED");
    when(invoiceSubmissions.findById(submission.getId())).thenReturn(Optional.of(submission));

    assertThatThrownBy(() -> service.deleteInvoiceSubmission(fixture.principal, submission.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("待审核");

    verify(invoiceSubmissions, never()).delete(any());
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

  private SupplierInvoice invoice(
      String invoiceNo,
      String approvalStatus,
      String matchStatus,
      BigDecimal amount,
      BigDecimal differenceAmount
  ) {
    SupplierInvoice invoice = new SupplierInvoice();
    invoice.setId(UUID.randomUUID());
    invoice.setInvoiceNo(invoiceNo);
    invoice.setApprovalStatus(approvalStatus);
    invoice.setMatchStatus(matchStatus);
    invoice.setAmount(amount);
    invoice.setDifferenceAmount(differenceAmount);
    return invoice;
  }

  private record Fixture(
      SupplierPortalAccount account,
      Supplier supplier,
      ProcurementInquiry inquiry,
      ProcurementInquiryInvitation invitation,
      SupplierPortalPrincipal principal
  ) {}
}
