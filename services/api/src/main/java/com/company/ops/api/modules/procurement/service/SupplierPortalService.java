package com.company.ops.api.modules.procurement.service;

import static com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.*;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.storage.FileStorageService.FilePolicy;
import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.repository.*;
import com.company.ops.api.modules.procurement.security.SupplierPortalPrincipal;
import com.company.ops.api.modules.system.security.JwtService;
import com.company.ops.api.modules.system.security.LoginAttemptService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SupplierPortalService {
  private static final FilePolicy DOCUMENT_POLICY = new FilePolicy(
      20L * 1024 * 1024,
      Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf", ".doc", ".docx", ".xls", ".xlsx"),
      "供应商资料不能超过20MB",
      "仅支持图片、PDF、Word 和 Excel 文件",
      true);

  private final SupplierPortalAccountRepository accounts;
  private final SupplierPortalDocumentRepository documents;
  private final SupplierRepository suppliers;
  private final ProcurementInquiryInvitationRepository invitations;
  private final ProcurementInquiryRepository inquiries;
  private final ProcurementInquiryRequestRepository inquiryRequests;
  private final PurchaseRequestRepository purchaseRequests;
  private final SupplierQuotationRepository quotes;
  private final SupplierQuotationLineRepository quoteLines;
  private final SupplierQuotationRevisionRepository revisions;
  private final SupplierQuoteAttachmentRepository quoteAttachments;
  private final InquiryClarificationRepository clarifications;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final LoginAttemptService loginAttempts;
  private final CodeGenerator codeGenerator;
  private final FileStorageService storage;
  private final ObjectMapper objectMapper;

  public SupplierPortalService(
      SupplierPortalAccountRepository accounts,
      SupplierPortalDocumentRepository documents,
      SupplierRepository suppliers,
      ProcurementInquiryInvitationRepository invitations,
      ProcurementInquiryRepository inquiries,
      ProcurementInquiryRequestRepository inquiryRequests,
      PurchaseRequestRepository purchaseRequests,
      SupplierQuotationRepository quotes,
      SupplierQuotationLineRepository quoteLines,
      SupplierQuotationRevisionRepository revisions,
      SupplierQuoteAttachmentRepository quoteAttachments,
      InquiryClarificationRepository clarifications,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      LoginAttemptService loginAttempts,
      CodeGenerator codeGenerator,
      FileStorageService storage,
      ObjectMapper objectMapper
  ) {
    this.accounts = accounts;
    this.documents = documents;
    this.suppliers = suppliers;
    this.invitations = invitations;
    this.inquiries = inquiries;
    this.inquiryRequests = inquiryRequests;
    this.purchaseRequests = purchaseRequests;
    this.quotes = quotes;
    this.quoteLines = quoteLines;
    this.revisions = revisions;
    this.quoteAttachments = quoteAttachments;
    this.clarifications = clarifications;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.loginAttempts = loginAttempts;
    this.codeGenerator = codeGenerator;
    this.storage = storage;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public SessionResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (accounts.existsByEmailIgnoreCase(email)) throw new BusinessException("该邮箱已经注册");
    String creditCode = request.unifiedSocialCreditCode().trim().toUpperCase();
    Supplier supplier = suppliers.findFirstByUnifiedSocialCreditCodeIgnoreCase(creditCode).orElse(null);
    ProcurementInquiryInvitation registrationInvitation = null;
    if (supplier == null) {
      supplier = createPendingSupplier(request, creditCode);
    } else {
      if (accounts.existsBySupplierId(supplier.getId())) {
        throw new BusinessException("该供应商已经绑定门户账号，请联系采购管理员处理联系人交接");
      }
      registrationInvitation = validateRegistrationInvitation(supplier, request.registrationCode());
      if (!normalizeCompanyName(supplier.getName()).equals(normalizeCompanyName(request.companyName()))) {
        throw new BusinessException("企业名称与采购邀请中的供应商主档不一致");
      }
    }
    SupplierPortalAccount account = new SupplierPortalAccount();
    account.setTenantId(TenantContext.currentTenant());
    account.setSupplierId(supplier.getId());
    account.setEmail(email);
    account.setPhone(trim(request.phone()));
    account.setContactName(request.contactName().trim());
    account.setPasswordHash(passwordEncoder.encode(request.password()));
    account.setStatus("PENDING_REVIEW");
    SupplierPortalAccount saved = accounts.save(account);
    if (registrationInvitation != null) {
      registrationInvitation.setRegistrationCodeUsedAt(OffsetDateTime.now());
      invitations.save(registrationInvitation);
    }
    return session(saved, supplier);
  }

  @Transactional
  public SessionResponse login(LoginRequest request, String clientAddress) {
    String email = normalizeEmail(request.email());
    String accountKey = "supplier|" + email;
    String attemptKey = "supplier-ip|" + email + "|" + clientAddress;
    loginAttempts.assertAllowed(accountKey);
    loginAttempts.assertAllowed(attemptKey);
    SupplierPortalAccount account = accounts.findByEmailIgnoreCase(email).orElse(null);
    if (account == null || !passwordEncoder.matches(request.password(), account.getPasswordHash())
        || "REJECTED".equals(account.getStatus()) || "SUSPENDED".equals(account.getStatus())) {
      loginAttempts.failed(accountKey);
      loginAttempts.failed(attemptKey);
      throw new BadCredentialsException("邮箱或密码错误，或账号不可用");
    }
    loginAttempts.succeeded(accountKey);
    loginAttempts.succeeded(attemptKey);
    account.setLastLoginAt(OffsetDateTime.now());
    SupplierPortalAccount saved = accounts.save(account);
    return session(saved, requireSupplier(saved.getSupplierId()));
  }

  @Transactional(readOnly = true)
  public SessionResponse current(SupplierPortalPrincipal principal) {
    SupplierPortalAccount account = requireAccount(principal.accountId());
    return session(account, requireSupplier(principal.supplierId()));
  }

  @Transactional
  public SessionResponse changePassword(
      SupplierPortalPrincipal principal,
      ChangePasswordRequest request
  ) {
    SupplierPortalAccount account = requireAccount(principal.accountId());
    if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
      throw new BusinessException("当前密码不正确");
    }
    if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
      throw new BusinessException("新密码不能与当前密码相同");
    }
    account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    account.setMustChangePassword(false);
    account.setPasswordChangedAt(OffsetDateTime.now());
    account.bumpAuthVersion();
    SupplierPortalAccount saved = accounts.save(account);
    return session(saved, requireSupplier(saved.getSupplierId()));
  }

  @Transactional
  public SupplierProfileResponse updateProfile(
      SupplierPortalPrincipal principal,
      UpdateProfileRequest request
  ) {
    SupplierPortalAccount account = requireAccount(principal.accountId());
    Supplier supplier = requireSupplier(principal.supplierId());
    if ("APPROVED".equals(supplier.getAdmissionStatus())) {
      if (!Objects.equals(supplier.getName(), request.name().trim())
          || !Objects.equals(normalize(supplier.getUnifiedSocialCreditCode()),
              normalize(request.unifiedSocialCreditCode()))
          || request.bankName() != null
              && !Objects.equals(trim(supplier.getBankName()), trim(request.bankName()))
          || request.bankAccount() != null
              && !Objects.equals(trim(supplier.getBankAccount()), trim(request.bankAccount()))) {
        throw new BusinessException("已准入供应商的企业名称、信用代码和银行信息必须提交内部变更审批");
      }
    }
    if (!"ACTIVE".equals(account.getStatus())) {
      account.setProfileDraftJson(writeProfileDraft(request));
      accounts.save(account);
      return profile(supplier, request);
    }
    applyProfile(supplier, request);
    account.setProfileDraftJson(null);
    accounts.save(account);
    return profile(suppliers.save(supplier));
  }

  @Transactional
  public DocumentResponse uploadDocument(
      SupplierPortalPrincipal principal,
      String documentType,
      LocalDate validTo,
      MultipartFile file
  ) {
    requireAccount(principal.accountId());
    String type = normalizeDocumentType(documentType);
    FileStorageService.StoredFile stored = null;
    try {
      stored = storage.store(file, "supplier-portal", DOCUMENT_POLICY);
      SupplierPortalDocument document = new SupplierPortalDocument();
      document.setSupplierId(principal.supplierId());
      document.setAccountId(principal.accountId());
      document.setDocumentType(type);
      document.setDocumentName(stored.originalName());
      document.setObjectKey(stored.objectKey());
      document.setContentType(stored.contentType());
      document.setSizeBytes(stored.sizeBytes());
      document.setValidTo(validTo);
      return document(documents.save(document));
    } catch (RuntimeException exception) {
      if (stored != null) storage.delete(stored.relativePath());
      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public List<DocumentResponse> listDocuments(SupplierPortalPrincipal principal) {
    return documents.findBySupplierIdOrderByCreatedAtDesc(principal.supplierId()).stream()
        .map(this::document).toList();
  }

  @Transactional(readOnly = true)
  public Resource loadDocument(SupplierPortalPrincipal principal, UUID id) {
    SupplierPortalDocument document = requireDocument(id);
    if (!document.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权访问该供应商资料");
    }
    return storage.loadInNamespace("supplier-portal", document.getObjectKey());
  }

  @Transactional
  public void deleteDocument(SupplierPortalPrincipal principal, UUID id) {
    SupplierPortalDocument document = requireDocument(id);
    if (!document.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权删除该供应商资料");
    }
    if ("APPROVED".equals(document.getReviewStatus())) {
      throw new BusinessException("已审核资料不能直接删除，请联系采购人员");
    }
    documents.delete(document);
    storage.deleteInNamespace("supplier-portal", document.getObjectKey());
  }

  @Transactional
  public List<Map<String, Object>> listInquiries(SupplierPortalPrincipal principal) {
    List<ProcurementInquiryInvitation> items = invitations
        .findBySupplierIdOrderByInvitedAtDesc(principal.supplierId());
    OffsetDateTime now = OffsetDateTime.now();
    items.stream().filter(item -> item.getViewedAt() == null).forEach(item -> {
      item.setViewedAt(now);
      if ("INVITED".equals(item.getStatus())) item.setStatus("VIEWED");
    });
    invitations.saveAll(items);
    return items.stream().filter(item -> !"CANCELLED".equals(item.getStatus()))
        .map(item -> portalInquiry(item, principal.supplierId())).toList();
  }

  @Transactional
  public Map<String, Object> saveQuote(
      SupplierPortalPrincipal principal,
      UUID inquiryId,
      SaveQuoteRequest request,
      boolean submit
  ) {
    Supplier supplier = requireActiveSupplier(principal);
    ProcurementInquiry inquiry = requireOpenInquiry(inquiryId);
    ProcurementInquiryInvitation invitation = invitations
        .findByInquiryIdAndSupplierId(inquiryId, supplier.getId())
        .orElseThrow(() -> new BusinessException("该询价未邀请当前供应商"));
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, supplier.getId())
        .orElse(null);
    if (quote != null && "INTERNAL_ENTRY".equals(quote.getSubmissionSource())) {
      throw new BusinessException("采购员已经代录报价，请确认代录报价或联系采购员修订");
    }
    if (quote != null && !"DRAFT".equals(quote.getSubmissionStatus())) {
      throw new BusinessException("已提交报价不能直接覆盖，请先撤回");
    }
    QuoteInputs inputs = validateQuoteLines(inquiry, request);
    boolean isNew = quote == null;
    if (isNew) {
      quote = new SupplierQuotation();
      quote.setInquiryId(inquiryId);
      quote.setSupplierId(supplier.getId());
      quote.setVersionNo(1);
    }
    applyQuote(quote, request, inputs, principal, submit);
    SupplierQuotation saved = quotes.save(quote);
    List<SupplierQuotationLine> existingLines = quoteLines.findByQuoteIdOrderByCreatedAtAsc(saved.getId());
    if (!existingLines.isEmpty()) quoteLines.deleteAll(existingLines);
    List<SupplierQuotationLine> persisted = request.lines().stream().map(line -> {
      SupplierQuotationLine entity = new SupplierQuotationLine();
      entity.setQuoteId(saved.getId());
      entity.setRequestId(line.requestId());
      entity.setQuantity(inputs.linkMap().get(line.requestId()).getRequestedQty());
      entity.setUnitPrice(line.unitPrice());
      entity.setTaxRate(line.taxRate());
      entity.setDeliveryDate(line.deliveryDate());
      entity.setRemark(trim(line.remark()));
      return entity;
    }).toList();
    quoteLines.saveAll(persisted);
    if (submit) {
      invitation.setStatus("RESPONDED");
      invitation.setRespondedAt(OffsetDateTime.now());
      invitations.save(invitation);
      saveRevision(saved, persisted);
    }
    return portalQuote(saved, persisted);
  }

  @Transactional
  public Map<String, Object> withdrawQuote(SupplierPortalPrincipal principal, UUID inquiryId) {
    requireActiveSupplier(principal);
    requireOpenInquiry(inquiryId);
    ProcurementInquiryInvitation invitation = invitations
        .findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("该询价未邀请当前供应商"));
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("报价不存在"));
    if (!"SUPPLIER_PORTAL".equals(quote.getSubmissionSource())
        || !"SUBMITTED".equals(quote.getSubmissionStatus())) {
      throw new BusinessException("当前报价不能撤回");
    }
    quote.setSubmissionStatus("DRAFT");
    quote.setVersionNo(quote.getVersionNo() + 1);
    quote.setSubmittedAt(null);
    invitation.setStatus(invitation.getViewedAt() == null ? "INVITED" : "VIEWED");
    invitation.setRespondedAt(null);
    invitations.save(invitation);
    return portalQuote(quotes.save(quote), quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId()));
  }

  @Transactional
  public Map<String, Object> confirmInternalQuote(SupplierPortalPrincipal principal, UUID inquiryId) {
    requireActiveSupplier(principal);
    requireOpenInquiry(inquiryId);
    ProcurementInquiryInvitation invitation = invitations
        .findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("该询价未邀请当前供应商"));
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("代录报价不存在"));
    if (!"INTERNAL_ENTRY".equals(quote.getSubmissionSource())) {
      throw new BusinessException("只有采购员代录报价需要供应商确认");
    }
    if (!"SUBMITTED".equals(quote.getSubmissionStatus())) {
      throw new BusinessException("只有已提交的代录报价可以确认");
    }
    quote.setConfirmedByAccountId(principal.accountId());
    quote.setConfirmedAt(OffsetDateTime.now());
    invitation.setStatus("RESPONDED");
    invitation.setRespondedAt(quote.getConfirmedAt());
    invitations.save(invitation);
    return portalQuote(quotes.save(quote), quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId()));
  }

  @Transactional
  public Map<String, Object> declineInquiry(
      SupplierPortalPrincipal principal,
      UUID inquiryId,
      DeclineQuoteRequest request
  ) {
    requireActiveSupplier(principal);
    requireOpenInquiry(inquiryId);
    ProcurementInquiryInvitation invitation = invitations
        .findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("该询价未邀请当前供应商"));
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, principal.supplierId()).orElse(null);
    if (quote != null && "SUBMITTED".equals(quote.getSubmissionStatus())) {
      throw new BusinessException("已提交报价不能直接放弃，请先撤回报价");
    }
    invitation.setStatus("DECLINED");
    invitation.setDeclinedAt(OffsetDateTime.now());
    invitation.setDeclineReason(request.reason().trim());
    invitation.setRespondedAt(invitation.getDeclinedAt());
    invitations.save(invitation);
    return portalInquiry(invitation, principal.supplierId());
  }

  @Transactional
  public QuoteAttachmentResponse uploadQuoteAttachment(
      SupplierPortalPrincipal principal,
      UUID inquiryId,
      String attachmentType,
      MultipartFile file
  ) {
    requireActiveSupplier(principal);
    requireOpenInquiry(inquiryId);
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("请先保存报价草稿，再上传报价附件"));
    if (!"SUPPLIER_PORTAL".equals(quote.getSubmissionSource())
        || !"DRAFT".equals(quote.getSubmissionStatus())) {
      throw new BusinessException("只有供应商报价草稿可以增补附件");
    }
    FileStorageService.StoredFile stored = null;
    try {
      String checksum = sha256(file);
      stored = storage.store(file, "supplier-quotes", DOCUMENT_POLICY);
      SupplierQuoteAttachment attachment = new SupplierQuoteAttachment();
      attachment.setQuoteId(quote.getId());
      attachment.setSupplierId(principal.supplierId());
      attachment.setAccountId(principal.accountId());
      attachment.setAttachmentType(normalizeAttachmentType(attachmentType));
      attachment.setFileName(stored.originalName());
      attachment.setObjectKey(stored.objectKey());
      attachment.setContentType(stored.contentType());
      attachment.setSizeBytes(stored.sizeBytes());
      attachment.setSha256(checksum);
      return attachment(quoteAttachments.save(attachment));
    } catch (RuntimeException exception) {
      if (stored != null) storage.delete(stored.relativePath());
      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public List<QuoteAttachmentResponse> listQuoteAttachments(
      SupplierPortalPrincipal principal,
      UUID inquiryId
  ) {
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElse(null);
    return quote == null ? List.of() : quoteAttachments.findByQuoteIdOrderByCreatedAtDesc(quote.getId())
        .stream().map(this::attachment).toList();
  }

  @Transactional(readOnly = true)
  public Resource loadQuoteAttachment(SupplierPortalPrincipal principal, UUID id) {
    SupplierQuoteAttachment attachment = requireQuoteAttachment(id);
    if (!attachment.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权访问该报价附件");
    }
    return storage.loadInNamespace("supplier-quotes", attachment.getObjectKey());
  }

  @Transactional
  public void deleteQuoteAttachment(SupplierPortalPrincipal principal, UUID id) {
    SupplierQuoteAttachment attachment = requireQuoteAttachment(id);
    if (!attachment.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权删除该报价附件");
    }
    SupplierQuotation quote = quotes.findById(attachment.getQuoteId())
        .orElseThrow(() -> new BusinessException("报价不存在"));
    if (!"DRAFT".equals(quote.getSubmissionStatus())) {
      throw new BusinessException("已提交报价的附件不能删除");
    }
    quoteAttachments.delete(attachment);
    storage.deleteInNamespace("supplier-quotes", attachment.getObjectKey());
  }

  @Transactional(readOnly = true)
  public List<ClarificationResponse> listClarifications(
      SupplierPortalPrincipal principal,
      UUID inquiryId
  ) {
    requireInvitation(inquiryId, principal.supplierId());
    return clarifications.findByInquiryIdAndSupplierIdOrderByAskedAtAsc(inquiryId, principal.supplierId())
        .stream().map(this::clarification).toList();
  }

  @Transactional
  public ClarificationResponse askClarification(
      SupplierPortalPrincipal principal,
      UUID inquiryId,
      AskClarificationRequest request
  ) {
    requireActiveSupplier(principal);
    requireOpenInquiry(inquiryId);
    requireInvitation(inquiryId, principal.supplierId());
    InquiryClarification item = new InquiryClarification();
    item.setInquiryId(inquiryId);
    item.setSupplierId(principal.supplierId());
    item.setAccountId(principal.accountId());
    item.setQuestion(request.question().trim());
    item.setAskedAt(OffsetDateTime.now());
    item.setStatus("OPEN");
    return clarification(clarifications.save(item));
  }

  @Transactional(readOnly = true)
  public List<AccountResponse> listAccounts() {
    Map<UUID, Supplier> supplierMap = suppliers.findAllById(
        accounts.findAllByOrderByCreatedAtDesc().stream().map(SupplierPortalAccount::getSupplierId).toList()
    ).stream().collect(Collectors.toMap(Supplier::getId, item -> item));
    return accounts.findAllByOrderByCreatedAtDesc().stream()
        .map(item -> account(item, supplierMap.get(item.getSupplierId()))).toList();
  }

  @Transactional
  public AccountResponse reviewAccount(UUID id, ReviewAccountRequest request) {
    SupplierPortalAccount account = requireAccount(id);
    if (!"PENDING_REVIEW".equals(account.getStatus())) throw new BusinessException("门户账号已处理");
    String decision = request.decision().trim().toUpperCase();
    if (!decision.equals("ACTIVE") && !decision.equals("REJECTED")) {
      throw new BusinessException("账号审批结果只能为 ACTIVE 或 REJECTED");
    }
    if (decision.equals("REJECTED") && isBlank(request.comment())) {
      throw new BusinessException("驳回账号必须填写原因");
    }
    account.setStatus(decision);
    account.setReviewComment(trim(request.comment()));
    account.setReviewedByName(currentInternalName());
    account.setReviewedAt(OffsetDateTime.now());
    account.bumpAuthVersion();
    if (decision.equals("ACTIVE") && !isBlank(account.getProfileDraftJson())) {
      Supplier supplier = requireSupplier(account.getSupplierId());
      applyProfile(supplier, readProfileDraft(account.getProfileDraftJson()));
      suppliers.save(supplier);
      account.setProfileDraftJson(null);
    }
    SupplierPortalAccount saved = accounts.save(account);
    return account(saved, requireSupplier(saved.getSupplierId()));
  }

  @Transactional
  public AccountResponse updateAccountStatus(UUID id, UpdateAccountStatusRequest request) {
    SupplierPortalAccount account = requireAccount(id);
    String status = request.status().trim().toUpperCase();
    if (!status.equals("ACTIVE") && !status.equals("SUSPENDED")) {
      throw new BusinessException("账号状态只能设置为 ACTIVE 或 SUSPENDED");
    }
    if (status.equals(account.getStatus())) return account(account, requireSupplier(account.getSupplierId()));
    account.setStatus(status);
    account.setReviewComment(trim(request.comment()));
    account.setReviewedByName(currentInternalName());
    account.setReviewedAt(OffsetDateTime.now());
    account.bumpAuthVersion();
    SupplierPortalAccount saved = accounts.save(account);
    return account(saved, requireSupplier(saved.getSupplierId()));
  }

  @Transactional
  public ResetPasswordResponse resetPassword(UUID id) {
    SupplierPortalAccount account = requireAccount(id);
    String temporaryPassword = "Tmp" + UUID.randomUUID().toString().replace("-", "").substring(0, 9) + "!";
    account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
    account.setMustChangePassword(true);
    account.setPasswordChangedAt(OffsetDateTime.now());
    account.bumpAuthVersion();
    SupplierPortalAccount saved = accounts.save(account);
    return new ResetPasswordResponse(temporaryPassword, account(saved, requireSupplier(saved.getSupplierId())));
  }

  @Transactional(readOnly = true)
  public List<DocumentResponse> listSupplierDocuments(UUID supplierId) {
    requireSupplier(supplierId);
    return documents.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream()
        .map(this::document).toList();
  }

  @Transactional
  public DocumentResponse reviewDocument(UUID id, ReviewDocumentRequest request) {
    SupplierPortalDocument document = requireDocument(id);
    String decision = request.decision().trim().toUpperCase();
    if (!decision.equals("APPROVED") && !decision.equals("REJECTED")) {
      throw new BusinessException("资料审核结果只能为 APPROVED 或 REJECTED");
    }
    document.setReviewStatus(decision);
    document.setReviewComment(trim(request.comment()));
    document.setReviewedByName(currentInternalName());
    document.setReviewedAt(OffsetDateTime.now());
    return document(documents.save(document));
  }

  @Transactional(readOnly = true)
  public Resource loadDocumentForInternal(UUID id) {
    SupplierPortalDocument document = requireDocument(id);
    return storage.loadInNamespace("supplier-portal", document.getObjectKey());
  }

  @Transactional(readOnly = true)
  public List<QuoteAttachmentResponse> listQuoteAttachmentsForInternal(UUID quoteId) {
    if (!quotes.existsById(quoteId)) throw new BusinessException("报价不存在");
    return quoteAttachments.findByQuoteIdOrderByCreatedAtDesc(quoteId).stream()
        .map(this::attachment).toList();
  }

  @Transactional(readOnly = true)
  public Resource loadQuoteAttachmentForInternal(UUID id) {
    SupplierQuoteAttachment attachment = requireQuoteAttachment(id);
    return storage.loadInNamespace("supplier-quotes", attachment.getObjectKey());
  }

  @Transactional(readOnly = true)
  public List<ClarificationResponse> listClarificationsForInternal(UUID inquiryId) {
    if (!inquiries.existsById(inquiryId)) throw new BusinessException("询价单不存在");
    return clarifications.findByInquiryIdOrderByAskedAtAsc(inquiryId).stream()
        .map(this::clarification).toList();
  }

  @Transactional
  public ClarificationResponse answerClarification(UUID id, AnswerClarificationRequest request) {
    InquiryClarification item = clarifications.findById(id)
        .orElseThrow(() -> new BusinessException("澄清问题不存在"));
    item.setAnswer(request.answer().trim());
    item.setAnsweredByName(currentInternalName());
    item.setAnsweredAt(OffsetDateTime.now());
    item.setStatus("ANSWERED");
    return clarification(clarifications.save(item));
  }

  private Supplier createPendingSupplier(RegisterRequest request, String creditCode) {
    Supplier supplier = new Supplier();
    supplier.setCode(codeGenerator.generate("SUPPLIER"));
    supplier.setName(request.companyName().trim());
    supplier.setUnifiedSocialCreditCode(creditCode);
    supplier.setCategory(trim(request.category()));
    supplier.setContactName(request.contactName().trim());
    supplier.setPhone(trim(request.phone()));
    supplier.setLicenseValidTo(request.licenseValidTo());
    supplier.setQualificationValidTo(request.qualificationValidTo());
    supplier.setAdmissionStatus("PENDING");
    supplier.setAdmissionSubmittedAt(OffsetDateTime.now());
    supplier.setRiskStatus(SupplierRiskStatus.NORMAL);
    return suppliers.save(supplier);
  }

  private SessionResponse session(SupplierPortalAccount account, Supplier supplier) {
    SupplierProfileResponse profile = !isBlank(account.getProfileDraftJson()) && !"ACTIVE".equals(account.getStatus())
        ? profile(supplier, readProfileDraft(account.getProfileDraftJson())) : profile(supplier);
    return new SessionResponse(jwtService.createSupplierPortalToken(account), account(account, supplier), profile);
  }

  private Supplier requireActiveSupplier(SupplierPortalPrincipal principal) {
    SupplierPortalAccount account = requireAccount(principal.accountId());
    if (!"ACTIVE".equals(account.getStatus())) throw new BusinessException("门户账号仍在等待内部审核");
    if (account.isMustChangePassword()) throw new BusinessException("请先修改临时密码");
    Supplier supplier = requireSupplier(principal.supplierId());
    if (!"APPROVED".equals(supplier.getAdmissionStatus())) throw new BusinessException("供应商尚未通过准入审批");
    if (supplier.getRiskStatus() == SupplierRiskStatus.BLOCKED) throw new BusinessException("供应商已冻结，不能报价");
    return supplier;
  }

  private ProcurementInquiry requireOpenInquiry(UUID id) {
    ProcurementInquiry inquiry = inquiries.findById(id).orElseThrow(() -> new BusinessException("询价单不存在"));
    if (!"OPEN".equals(inquiry.getStatus())) throw new BusinessException("询价已经结束");
    if (inquiry.getDeadline() != null && LocalDate.now().isAfter(inquiry.getDeadline())) {
      throw new BusinessException("询价已经超过截止日期");
    }
    return inquiry;
  }

  private QuoteInputs validateQuoteLines(ProcurementInquiry inquiry, SaveQuoteRequest request) {
    List<ProcurementInquiryRequest> links = inquiryRequests.findByInquiryIdOrderByCreatedAtAsc(inquiry.getId());
    if (links.isEmpty()) {
      ProcurementInquiryRequest fallback = new ProcurementInquiryRequest();
      fallback.setInquiryId(inquiry.getId());
      fallback.setRequestId(inquiry.getRequestId());
      PurchaseRequest source = purchaseRequests.findById(inquiry.getRequestId()).orElse(null);
      fallback.setRequestedQty(source == null ? BigDecimal.ZERO : source.getQuantity());
      links = List.of(fallback);
    }
    Map<UUID, ProcurementInquiryRequest> linkMap = links.stream()
        .collect(Collectors.toMap(ProcurementInquiryRequest::getRequestId, item -> item));
    Set<UUID> requestIds = request.lines().stream().map(SaveQuoteLineRequest::requestId).collect(Collectors.toSet());
    if (requestIds.size() != request.lines().size() || !requestIds.equals(linkMap.keySet())) {
      throw new BusinessException("报价分项必须完整覆盖询价包中的全部采购申请");
    }
    BigDecimal totalQty = links.stream().map(ProcurementInquiryRequest::getRequestedQty)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal materialAmount = request.lines().stream()
        .map(line -> linkMap.get(line.requestId()).getRequestedQty().multiply(line.unitPrice()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal average = totalQty.signum() == 0 ? BigDecimal.ZERO
        : materialAmount.divide(totalQty, 2, RoundingMode.HALF_UP);
    LocalDate latestDelivery = request.lines().stream().map(SaveQuoteLineRequest::deliveryDate)
        .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
    return new QuoteInputs(linkMap, average, latestDelivery);
  }

  private void applyQuote(
      SupplierQuotation quote,
      SaveQuoteRequest request,
      QuoteInputs inputs,
      SupplierPortalPrincipal principal,
      boolean submit
  ) {
    quote.setUnitPrice(inputs.averageUnitPrice());
    quote.setTaxRate(request.lines().get(0).taxRate());
    quote.setDeliveryDate(inputs.latestDelivery());
    quote.setPaymentTerms(trim(request.paymentTerms()));
    quote.setRemark(trim(request.remark()));
    quote.setCurrency(isBlank(request.currency()) ? "CNY" : request.currency().trim().toUpperCase());
    quote.setFreightAmount(value(request.freightAmount()));
    quote.setOtherCostAmount(value(request.otherCostAmount()));
    quote.setTechnicalScore(BigDecimal.ZERO);
    quote.setCommercialScore(BigDecimal.ZERO);
    quote.setTotalScore(BigDecimal.ZERO);
    quote.setValidUntil(request.validUntil());
    quote.setSubmissionSource("SUPPLIER_PORTAL");
    quote.setSubmissionStatus(submit ? "SUBMITTED" : "DRAFT");
    quote.setSubmittedByType("SUPPLIER_ACCOUNT");
    quote.setSubmittedById(principal.accountId());
    quote.setSubmittedByName(principal.contactName());
    quote.setSubmittedAt(submit ? OffsetDateTime.now() : null);
    quote.setConfirmedByAccountId(submit ? principal.accountId() : null);
    quote.setConfirmedAt(submit ? OffsetDateTime.now() : null);
  }

  private void saveRevision(SupplierQuotation quote, List<SupplierQuotationLine> lines) {
    SupplierQuotationRevision revision = new SupplierQuotationRevision();
    revision.setQuoteId(quote.getId());
    revision.setVersionNo(quote.getVersionNo());
    revision.setSubmissionSource(quote.getSubmissionSource());
    revision.setSubmittedByType(quote.getSubmittedByType());
    revision.setSubmittedById(quote.getSubmittedById());
    revision.setSubmittedByName(quote.getSubmittedByName());
    revision.setSubmittedAt(quote.getSubmittedAt());
    try {
      revision.setSnapshotJson(objectMapper.writeValueAsString(portalQuote(quote, lines)));
    } catch (JsonProcessingException exception) {
      throw new BusinessException("报价版本快照保存失败");
    }
    revisions.save(revision);
  }

  private Map<String, Object> portalInquiry(ProcurementInquiryInvitation invitation, UUID supplierId) {
    ProcurementInquiry inquiry = inquiries.findById(invitation.getInquiryId()).orElseThrow();
    List<ProcurementInquiryRequest> links = inquiryRequests.findByInquiryIdOrderByCreatedAtAsc(inquiry.getId());
    if (links.isEmpty()) {
      ProcurementInquiryRequest fallback = new ProcurementInquiryRequest();
      fallback.setInquiryId(inquiry.getId()); fallback.setRequestId(inquiry.getRequestId());
      PurchaseRequest source = purchaseRequests.findById(inquiry.getRequestId()).orElse(null);
      fallback.setRequestedQty(source == null ? BigDecimal.ZERO : source.getQuantity());
      links = List.of(fallback);
    }
    Map<UUID, PurchaseRequest> requestMap = purchaseRequests.findAllById(
        links.stream().map(ProcurementInquiryRequest::getRequestId).toList()
    ).stream().collect(Collectors.toMap(PurchaseRequest::getId, item -> item));
    List<Map<String, Object>> lineViews = links.stream().map(link -> {
      PurchaseRequest source = requestMap.get(link.getRequestId());
      Map<String, Object> line = new LinkedHashMap<>();
      line.put("requestId", link.getRequestId());
      line.put("requestCode", source == null ? null : source.getCode());
      line.put("partName", source == null ? null : source.getPartName());
      line.put("quantity", link.getRequestedQty());
      line.put("expectedDate", source == null ? null : source.getExpectedDate());
      return line;
    }).toList();
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", inquiry.getId()); view.put("code", inquiry.getCode());
    view.put("title", inquiry.getTitle()); view.put("deadline", inquiry.getDeadline());
    view.put("status", inquiry.getStatus()); view.put("invitationStatus", invitation.getStatus());
    view.put("invitedAt", invitation.getInvitedAt()); view.put("lines", lineViews);
    view.put("declineReason", invitation.getDeclineReason()); view.put("declinedAt", invitation.getDeclinedAt());
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiry.getId(), supplierId).orElse(null);
    view.put("quote", quote == null ? null : portalQuote(quote, quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId())));
    view.put("attachments", quote == null ? List.of() : quoteAttachments.findByQuoteIdOrderByCreatedAtDesc(quote.getId())
        .stream().map(this::attachment).toList());
    view.put("clarifications", clarifications.findByInquiryIdAndSupplierIdOrderByAskedAtAsc(inquiry.getId(), supplierId)
        .stream().map(this::clarification).toList());
    return view;
  }

  private Map<String, Object> portalQuote(SupplierQuotation quote, List<SupplierQuotationLine> lines) {
    BigDecimal materialAmount = lines.stream()
        .map(line -> value(line.getQuantity()).multiply(value(line.getUnitPrice())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", quote.getId()); view.put("source", quote.getSubmissionSource());
    view.put("status", quote.getSubmissionStatus()); view.put("versionNo", quote.getVersionNo());
    view.put("currency", quote.getCurrency()); view.put("paymentTerms", quote.getPaymentTerms());
    view.put("remark", quote.getRemark()); view.put("freightAmount", quote.getFreightAmount());
    view.put("otherCostAmount", quote.getOtherCostAmount()); view.put("validUntil", quote.getValidUntil());
    view.put("submittedByName", quote.getSubmittedByName()); view.put("submittedAt", quote.getSubmittedAt());
    view.put("confirmed", quote.getConfirmedAt() != null); view.put("confirmedAt", quote.getConfirmedAt());
    view.put("declinedAt", quote.getDeclinedAt()); view.put("declineReason", quote.getDeclineReason());
    view.put("materialAmount", materialAmount);
    view.put("totalAmount", materialAmount.add(value(quote.getFreightAmount())).add(value(quote.getOtherCostAmount())));
    view.put("lines", lines.stream().map(line -> Map.of(
        "requestId", line.getRequestId(), "quantity", line.getQuantity(), "unitPrice", line.getUnitPrice(),
        "taxRate", line.getTaxRate(), "deliveryDate", line.getDeliveryDate() == null ? "" : line.getDeliveryDate().toString(),
        "remark", line.getRemark() == null ? "" : line.getRemark())).toList());
    return view;
  }

  private AccountResponse account(SupplierPortalAccount account, Supplier supplier) {
    return new AccountResponse(account.getId(), account.getSupplierId(), supplier == null ? null : supplier.getCode(),
        supplier == null ? null : supplier.getName(), supplier == null ? null : supplier.getAdmissionStatus(),
        account.getEmail(), account.getPhone(), account.getContactName(), account.getStatus(), account.isMustChangePassword(),
        account.getReviewComment(), account.getReviewedByName(), account.getReviewedAt(), account.getLastLoginAt(),
        account.getCreatedAt());
  }

  private SupplierProfileResponse profile(Supplier supplier) {
    return new SupplierProfileResponse(supplier.getId(), supplier.getCode(), supplier.getName(), supplier.getCategory(),
        supplier.getContactName(), supplier.getPhone(), supplier.getLegalRepresentative(),
        supplier.getUnifiedSocialCreditCode(), supplier.getRegisteredCapital(), supplier.getRegisteredAddress(),
        supplier.getBusinessScope(), supplier.getLicenseValidTo(), supplier.getQualificationValidTo(),
        supplier.getTaxpayerType(), supplier.getBankName(), maskBankAccount(supplier.getBankAccount()),
        supplier.getSettlementTerms(), supplier.getAdmissionStatus(), supplier.getAdmissionReviewComment(),
        supplier.getRiskStatus() == null ? null : supplier.getRiskStatus().name());
  }

  private SupplierProfileResponse profile(Supplier supplier, UpdateProfileRequest request) {
    Supplier copy = new Supplier();
    copy.setId(supplier.getId()); copy.setCode(supplier.getCode()); copy.setName(request.name());
    copy.setCategory(request.category()); copy.setContactName(request.contactName()); copy.setPhone(request.phone());
    copy.setLegalRepresentative(request.legalRepresentative()); copy.setUnifiedSocialCreditCode(request.unifiedSocialCreditCode());
    copy.setRegisteredCapital(request.registeredCapital()); copy.setRegisteredAddress(request.registeredAddress());
    copy.setBusinessScope(request.businessScope()); copy.setLicenseValidTo(request.licenseValidTo());
    copy.setQualificationValidTo(request.qualificationValidTo()); copy.setTaxpayerType(request.taxpayerType());
    copy.setBankName(request.bankName()); copy.setBankAccount(request.bankAccount());
    copy.setSettlementTerms(request.settlementTerms()); copy.setAdmissionStatus(supplier.getAdmissionStatus());
    copy.setAdmissionReviewComment(supplier.getAdmissionReviewComment()); copy.setRiskStatus(supplier.getRiskStatus());
    return profile(copy);
  }

  private DocumentResponse document(SupplierPortalDocument document) {
    return new DocumentResponse(document.getId(), document.getSupplierId(), document.getDocumentType(),
        document.getDocumentName(), document.getContentType(), document.getSizeBytes(), document.getValidTo(),
        document.getReviewStatus(), document.getReviewComment(), document.getReviewedByName(),
        document.getReviewedAt(), document.getCreatedAt());
  }

  private SupplierPortalAccount requireAccount(UUID id) {
    return accounts.findById(id).orElseThrow(() -> new BusinessException("供应商门户账号不存在"));
  }

  private Supplier requireSupplier(UUID id) {
    return suppliers.findById(id).orElseThrow(() -> new BusinessException("供应商不存在"));
  }

  private SupplierPortalDocument requireDocument(UUID id) {
    return documents.findById(id).orElseThrow(() -> new BusinessException("供应商资料不存在"));
  }

  private SupplierQuoteAttachment requireQuoteAttachment(UUID id) {
    return quoteAttachments.findById(id).orElseThrow(() -> new BusinessException("报价附件不存在"));
  }

  private ProcurementInquiryInvitation requireInvitation(UUID inquiryId, UUID supplierId) {
    return invitations.findByInquiryIdAndSupplierId(inquiryId, supplierId)
        .orElseThrow(() -> new BusinessException("该询价未邀请当前供应商"));
  }

  private ProcurementInquiryInvitation validateRegistrationInvitation(Supplier supplier, String code) {
    if (isBlank(code)) throw new BusinessException("已有供应商必须使用采购邀请注册码完成绑定");
    ProcurementInquiryInvitation match = invitations.findBySupplierIdOrderByInvitedAtDesc(supplier.getId()).stream()
        .filter(item -> item.getRegistrationCodeHash() != null)
        .filter(item -> item.getRegistrationCodeUsedAt() == null)
        .filter(item -> item.getRegistrationCodeExpiresAt() == null || item.getRegistrationCodeExpiresAt().isAfter(OffsetDateTime.now()))
        .filter(item -> sha256Text(code.trim()).equals(item.getRegistrationCodeHash()))
        .findFirst().orElse(null);
    if (match == null) throw new BusinessException("采购邀请注册码无效或已过期");
    return match;
  }

  private String writeProfileDraft(UpdateProfileRequest request) {
    try { return objectMapper.writeValueAsString(request); }
    catch (JsonProcessingException exception) { throw new BusinessException("资料暂存失败"); }
  }

  private UpdateProfileRequest readProfileDraft(String json) {
    try { return objectMapper.readValue(json, UpdateProfileRequest.class); }
    catch (JsonProcessingException exception) { throw new BusinessException("待审核资料格式错误"); }
  }

  private void applyProfile(Supplier supplier, UpdateProfileRequest request) {
    supplier.setName(request.name().trim()); supplier.setCategory(trim(request.category()));
    supplier.setContactName(trim(request.contactName())); supplier.setPhone(trim(request.phone()));
    supplier.setLegalRepresentative(trim(request.legalRepresentative()));
    supplier.setUnifiedSocialCreditCode(request.unifiedSocialCreditCode().trim().toUpperCase());
    supplier.setRegisteredCapital(trim(request.registeredCapital())); supplier.setRegisteredAddress(trim(request.registeredAddress()));
    supplier.setBusinessScope(trim(request.businessScope())); supplier.setLicenseValidTo(request.licenseValidTo());
    supplier.setQualificationValidTo(request.qualificationValidTo()); supplier.setTaxpayerType(trim(request.taxpayerType()));
    supplier.setBankName(trim(request.bankName())); supplier.setBankAccount(trim(request.bankAccount()));
    supplier.setSettlementTerms(trim(request.settlementTerms()));
  }

  private ClarificationResponse clarification(InquiryClarification item) {
    Supplier supplier = suppliers.findById(item.getSupplierId()).orElse(null);
    return new ClarificationResponse(item.getId(), item.getInquiryId(), item.getSupplierId(),
        supplier == null ? null : supplier.getName(), item.getQuestion(), item.getAskedAt(), item.getAnswer(),
        item.getAnsweredByName(), item.getAnsweredAt(), item.getStatus());
  }

  private QuoteAttachmentResponse attachment(SupplierQuoteAttachment item) {
    return new QuoteAttachmentResponse(item.getId(), item.getQuoteId(), item.getAttachmentType(),
        item.getFileName(), item.getContentType(), item.getSizeBytes(), item.getSha256(), item.getCreatedAt());
  }

  private static String normalizeCompanyName(String value) {
    return value == null ? "" : value.replaceAll("[\\s　]", "").toUpperCase();
  }

  private static String normalizeAttachmentType(String value) {
    String type = isBlank(value) ? "OTHER" : value.trim().toUpperCase();
    if (!Set.of("QUOTATION", "TECHNICAL", "OTHER").contains(type)) throw new BusinessException("报价附件类型不合法");
    return type;
  }

  private static String sha256(MultipartFile file) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(file.getBytes()));
    } catch (NoSuchAlgorithmException | java.io.IOException exception) {
      throw new BusinessException("报价附件校验失败");
    }
  }

  private static String sha256Text(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new BusinessException("注册码校验失败");
    }
  }

  private String currentInternalName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }

  private static String normalizeEmail(String email) { return email.trim().toLowerCase(); }
  private static String normalize(String value) { return value == null ? null : value.trim().toUpperCase(); }
  private static String trim(String value) { return isBlank(value) ? null : value.trim(); }
  private static boolean isBlank(String value) { return value == null || value.isBlank(); }
  private static BigDecimal value(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
  private static String normalizeDocumentType(String value) {
    String type = isBlank(value) ? "OTHER" : value.trim().toUpperCase();
    if (!Set.of("BUSINESS_LICENSE", "QUALIFICATION", "BANK_PROOF", "TAX_DOCUMENT", "OTHER").contains(type)) {
      throw new BusinessException("供应商资料类型不合法");
    }
    return type;
  }
  private static String maskBankAccount(String value) {
    if (isBlank(value)) return null;
    String trimmed = value.trim();
    return trimmed.length() <= 4 ? "****" : "**** **** " + trimmed.substring(trimmed.length() - 4);
  }

  private record QuoteInputs(
      Map<UUID, ProcurementInquiryRequest> linkMap,
      BigDecimal averageUnitPrice,
      LocalDate latestDelivery
  ) {}
}
