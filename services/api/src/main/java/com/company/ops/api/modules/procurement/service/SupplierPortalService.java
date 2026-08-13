package com.company.ops.api.modules.procurement.service;

import static com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.*;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.storage.FileStorageService.FilePolicy;
import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.dto.ProcurementShipmentResponse;
import com.company.ops.api.modules.procurement.dto.SupplierPortalNotificationResponse;
import com.company.ops.api.modules.procurement.repository.*;
import com.company.ops.api.modules.procurement.security.SupplierPortalPrincipal;
import com.company.ops.api.modules.system.security.JwtService;
import com.company.ops.api.modules.system.security.LoginAttemptService;
import com.company.ops.api.modules.system.security.TotpService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;

@Service
public class SupplierPortalService {
  private static final int MAX_SHIPMENTS_PER_ORDER = 50;

  private static final FilePolicy DOCUMENT_POLICY = new FilePolicy(
      20L * 1024 * 1024,
      Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf", ".doc", ".docx", ".xls", ".xlsx"),
      "供应商资料不能超过20MB",
      "仅支持图片、PDF、Word 和 Excel 文件",
      true);

  private volatile BaseFont pdfCjkFont;

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
  private final ProcurementContractRepository contracts;
  private final ProcurementOrderDocumentRepository orderDocuments;
  private final PurchaseOrderRepository orders;
  private final GoodsReceiptRepository receipts;
  private final PurchaseOrderChangeRepository orderChanges;
  private final SupplierPortalNotificationRepository notifications;
  private final SupplierInvoiceSubmissionRepository invoiceSubmissions;
  private final ProcurementShipmentRepository shipments;
  private final SupplierShipmentAttachmentRepository shipmentAttachments;
  private final SupplierChangeRequestRepository supplierChanges;
  private final SupplierPerformanceReviewRepository performanceReviews;
  private final SupplierInvoiceRepository invoices;
  private final ProcurementPayableRepository payables;
  private final SupplierPortalNotifier notifier;
  private final ProcurementInternalNotifier internalNotifier;
  private final SupplierPortalEmailService emails;
  private final SupplierPortalActivityRepository activities;
  private final TotpService totpService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final LoginAttemptService loginAttempts;
  private final CodeGenerator codeGenerator;
  private final FileStorageService storage;
  private final ObjectMapper objectMapper;

  /** 开发联调：邮件通道未启用时把密码重置验证码直接回传，生产环境必须保持关闭。 */
  @Value("${ops.supplier-portal.dev-password-reset-code:false}")
  private boolean devPasswordResetCodeReturn;

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
      ProcurementContractRepository contracts,
      ProcurementOrderDocumentRepository orderDocuments,
      PurchaseOrderRepository orders,
      GoodsReceiptRepository receipts,
      PurchaseOrderChangeRepository orderChanges,
      SupplierPortalNotificationRepository notifications,
      SupplierInvoiceSubmissionRepository invoiceSubmissions,
      ProcurementShipmentRepository shipments,
      SupplierShipmentAttachmentRepository shipmentAttachments,
      SupplierChangeRequestRepository supplierChanges,
      SupplierPerformanceReviewRepository performanceReviews,
      SupplierInvoiceRepository invoices,
      ProcurementPayableRepository payables,
      SupplierPortalNotifier notifier,
      ProcurementInternalNotifier internalNotifier,
      SupplierPortalEmailService emails,
      SupplierPortalActivityRepository activities,
      TotpService totpService,
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
    this.contracts = contracts;
    this.orderDocuments = orderDocuments;
    this.orders = orders;
    this.receipts = receipts;
    this.orderChanges = orderChanges;
    this.notifications = notifications;
    this.invoiceSubmissions = invoiceSubmissions;
    this.shipments = shipments;
    this.shipmentAttachments = shipmentAttachments;
    this.supplierChanges = supplierChanges;
    this.performanceReviews = performanceReviews;
    this.invoices = invoices;
    this.payables = payables;
    this.notifier = notifier;
    this.internalNotifier = internalNotifier;
    this.emails = emails;
    this.activities = activities;
    this.totpService = totpService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.loginAttempts = loginAttempts;
    this.codeGenerator = codeGenerator;
    this.storage = storage;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public SessionResponse register(RegisterRequest request, String clientAddress) {
    String email = normalizeEmail(request.email());
    String registerKey = "supplier-reg|" + email + "|" + clientAddress;
    String registerIpKey = "supplier-reg-ip|" + clientAddress;
    loginAttempts.assertAllowed(registerKey);
    loginAttempts.assertAllowed(registerIpKey);
    if (!isBlank(request.website())) {
      throw new BusinessException("注册未完成，请稍后重试");
    }
    if (accounts.existsByEmailIgnoreCase(email)) {
      loginAttempts.failed(registerKey);
      loginAttempts.failed(registerIpKey);
      throw new BusinessException("该邮箱已经注册，请直接登录；如忘记密码请联系采购管理员重置");
    }
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
    if (account.isMfaEnabled()) {
      boolean verified = totpService.verify(account.getMfaSecret(), request.mfaCode());
      if (!verified) verified = verifyRecoveryCode(account, request.mfaCode());
      if (!verified) {
        loginAttempts.failed(accountKey);
        throw new BadCredentialsException("该账号已开启双重验证，动态验证码不正确");
      }
    } else if (request.mfaCode() != null && !request.mfaCode().isBlank()) {
      throw new BadCredentialsException("该账号未开启双重验证，无需填写动态验证码");
    }
    loginAttempts.succeeded(accountKey);
    loginAttempts.succeeded(attemptKey);
    OffsetDateTime lastLoginAt = account.getLastLoginAt();
    String lastLoginIp = activities
        .findFirstByAccountIdAndActionOrderByCreatedAtDesc(account.getId(), "LOGIN")
        .map(SupplierPortalActivity::getIp).orElse(null);
    account.setLastLoginAt(OffsetDateTime.now());
    SupplierPortalAccount saved = accounts.save(account);
    recordActivity(saved, "LOGIN", "门户登录", clientAddress);
    return session(saved, requireSupplier(saved.getSupplierId()), lastLoginAt, lastLoginIp);
  }

  @Transactional(readOnly = true)
  public MfaStatusResponse mfaStatus(SupplierPortalPrincipal principal) {
    SupplierPortalAccount account = requireAccount(principal.accountId());
    return new MfaStatusResponse(account.isMfaEnabled(),
        isBlank(account.getMfaRecoveryCodes()) ? 0 : account.getMfaRecoveryCodes().split("\n").length);
  }

  @Transactional
  public MfaSetupResponse beginMfaSetup(SupplierPortalPrincipal principal, MfaSetupRequest request) {
    SupplierPortalAccount account = requireAccount(principal.accountId());
    if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
      throw new BusinessException("当前密码不正确");
    }
    if (account.isMfaEnabled()) throw new BusinessException("双重验证已启用");
    String secret = totpService.generateSecret();
    account.setMfaSecret(secret);
    account.setMfaRecoveryCodes(null);
    accounts.save(account);
    return new MfaSetupResponse(secret, totpService.provisioningUri(account.getEmail(), secret));
  }

  @Transactional
  public List<String> enableMfa(SupplierPortalPrincipal principal, MfaEnableRequest request) {
    SupplierPortalAccount account = requireAccount(principal.accountId());
    if (account.isMfaEnabled()) throw new BusinessException("双重验证已启用");
    if (isBlank(account.getMfaSecret())) throw new BusinessException("请先开始双重验证设置");
    if (!totpService.verify(account.getMfaSecret(), request.code())) {
      throw new BusinessException("动态验证码不正确");
    }
    List<String> recoveryCodes = generateRecoveryCodes();
    account.setMfaRecoveryCodes(recoveryCodes.stream()
        .map(code -> sha256Text("recovery:" + code + ":" + account.getId()))
        .collect(Collectors.joining("\n")));
    account.setMfaEnabled(true);
    accounts.save(account);
    recordActivity(account, "MFA_ENABLE", "开启双重验证", null);
    return recoveryCodes;
  }

  @Transactional
  public void disableMfa(SupplierPortalPrincipal principal, MfaDisableRequest request) {
    SupplierPortalAccount account = requireAccount(principal.accountId());
    if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
      throw new BusinessException("当前密码不正确");
    }
    account.setMfaEnabled(false);
    account.setMfaSecret(null);
    account.setMfaRecoveryCodes(null);
    accounts.save(account);
    recordActivity(account, "MFA_DISABLE", "关闭双重验证", null);
  }

  private boolean verifyRecoveryCode(SupplierPortalAccount account, String code) {
    if (isBlank(code) || isBlank(account.getMfaRecoveryCodes())) return false;
    String expected = sha256Text("recovery:" + code.trim() + ":" + account.getId());
    List<String> hashes = Arrays.stream(account.getMfaRecoveryCodes().split("\n"))
        .filter(hash -> !hash.isBlank()).collect(Collectors.toList());
    for (String hash : hashes) {
      if (MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
          hash.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
        List<String> remaining = hashes.stream().filter(item -> !item.equals(hash)).collect(Collectors.toList());
        account.setMfaRecoveryCodes(remaining.isEmpty() ? null : String.join("\n", remaining));
        accounts.save(account);
        return true;
      }
    }
    return false;
  }

  private List<String> generateRecoveryCodes() {
    List<String> codes = new ArrayList<>();
    for (int index = 0; index < 10; index++) {
      codes.add(UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
    }
    return codes;
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
    recordActivity(saved, "CHANGE_PASSWORD", "修改登录密码", null);
    return session(saved, requireSupplier(saved.getSupplierId()));
  }

  @Transactional
  public String requestPasswordReset(String email, String clientAddress) {
    String normalized = normalizeEmail(email);
    String requestKey = "supplier-reset|" + normalized;
    String ipKey = "supplier-reset-ip|" + normalized + "|" + (clientAddress == null ? "" : clientAddress);
    loginAttempts.assertAllowed(requestKey, "密码重置请求过于频繁，请稍后重试");
    loginAttempts.assertAllowed(ipKey, "密码重置请求过于频繁，请稍后重试");
    SupplierPortalAccount account = accounts.findByEmailIgnoreCase(normalized).orElse(null);
    if (account == null) {
      // 统一提示，避免暴露邮箱是否已注册
      return null;
    }
    String code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
    account.setResetTokenHash(sha256Text(code + ":" + account.getId()));
    account.setResetTokenExpiresAt(OffsetDateTime.now().plusMinutes(15));
    account.setResetTokenUsedAt(null);
    accounts.save(account);
    loginAttempts.failed(requestKey);
    loginAttempts.failed(ipKey);
    recordActivity(account, "RESET_REQUEST", "申请密码重置验证码", clientAddress);
    Boolean sent = emails.send(account.getEmail(), "供应商门户密码重置验证码",
        "您的密码重置验证码是：" + code + "，15 分钟内有效。如非本人操作请忽略本邮件。");
    // 邮件通道完全未启用且明确允许开发回传时，才把验证码返回给调用方，便于本地联调；
    // SMTP 已启用但发送失败（sent=false）或生产环境一律不回传。
    return sent == null && devPasswordResetCodeReturn ? code : null;
  }

  @Transactional
  public void resetPassword(String email, String code, String newPassword) {
    String normalized = normalizeEmail(email);
    String attemptKey = "supplier-reset-code|" + normalized;
    loginAttempts.assertAllowed(attemptKey, "验证码尝试次数过多，请重新获取验证码");
    SupplierPortalAccount account = accounts.findByEmailIgnoreCase(normalized)
        .orElseThrow(() -> new BusinessException("验证码无效或已过期，请重新获取"));
    if (account.getResetTokenHash() == null
        || account.getResetTokenExpiresAt() == null
        || account.getResetTokenExpiresAt().isBefore(OffsetDateTime.now())
        || account.getResetTokenUsedAt() != null) {
      throw new BusinessException("验证码无效或已过期，请重新获取");
    }
    String expected = sha256Text(code + ":" + account.getId());
    if (!MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        account.getResetTokenHash().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
      loginAttempts.failed(attemptKey);
      throw new BusinessException("验证码不正确");
    }
    account.setPasswordHash(passwordEncoder.encode(newPassword));
    account.setResetTokenUsedAt(OffsetDateTime.now());
    account.setMustChangePassword(false);
    account.setPasswordChangedAt(OffsetDateTime.now());
    account.bumpAuthVersion();
    accounts.save(account);
    loginAttempts.succeeded(attemptKey);
    recordActivity(account, "RESET_PASSWORD", "通过验证码重置密码", null);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listAccountActivities(SupplierPortalPrincipal principal) {
    return activities.findByAccountIdOrderByCreatedAtDesc(principal.accountId()).stream()
        .map(item -> {
          Map<String, Object> view = new LinkedHashMap<>();
          view.put("id", item.getId());
          view.put("action", item.getAction());
          view.put("detail", item.getDetail());
          view.put("ip", item.getIp());
          view.put("createdAt", item.getCreatedAt());
          return view;
        }).toList();
  }

  private void recordActivity(
      SupplierPortalAccount account,
      String action,
      String detail,
      String ip
  ) {
    SupplierPortalActivity activity = new SupplierPortalActivity();
    activity.setTenantId(TenantContext.currentTenant());
    activity.setAccountId(account.getId());
    activity.setSupplierId(account.getSupplierId());
    activity.setAction(action);
    activity.setDetail(detail);
    activity.setIp(ip);
    activities.save(activity);
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
    assertUploadAllowed(file,
        documents.countBySupplierId(principal.supplierId()), 30,
        documents.sumSizeBySupplierId(principal.supplierId()), 200L * 1024 * 1024,
        "资质文件最多 30 份、总大小不超过 200MB，请先删除旧文件");
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
    if (submit) {
      boolean incomplete = request.lines().stream()
          .anyMatch(line -> line.unitPrice() == null
              || line.unitPrice().signum() <= 0
              || line.taxRate() == null);
      if (incomplete) throw new BusinessException("请完整填写所有分项的单价与税率后再提交");
    }
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
    if (!existingLines.isEmpty()) quoteLines.deleteAllInBatch(existingLines);
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
      recordActivity(requireAccount(principal.accountId()), "QUOTE_SUBMIT",
          "提交报价 " + inquiry.getCode(), null);
      internalNotifier.notifyProcurementStaff(
          "PROCUREMENT",
          "供应商提交报价",
          supplier.getName() + " 已提交报价（询价单 " + inquiry.getCode() + "），请及时评审。",
          "QUOTE", saved.getId(),
          "PORTAL_QUOTE_SUBMITTED:" + saved.getId());
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
    Supplier supplier = requireActiveSupplier(principal);
    ProcurementInquiry inquiry = requireOpenInquiry(inquiryId);
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
    internalNotifier.notifyProcurementStaff(
        "PROCUREMENT",
        "供应商确认代录报价",
        supplier.getName() + " 已确认代录报价（询价单 " + inquiry.getCode() + "）。",
        "QUOTE", quote.getId(),
        "PORTAL_QUOTE_CONFIRMED:" + quote.getId());
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
    assertUploadAllowed(file,
        quoteAttachments.countByQuoteId(quote.getId()), 10,
        quoteAttachments.sumSizeByQuoteId(quote.getId()), 100L * 1024 * 1024,
        "单个报价最多 10 个附件、总大小不超过 100MB，请先删除旧附件");
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
  public Resource loadContractDocument(SupplierPortalPrincipal principal, UUID id) {
    ProcurementOrderDocument document = requireContractDocument(principal, id);
    return storage.loadInNamespace("procurement-orders", document.getObjectKey());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> contractDocumentMetadata(SupplierPortalPrincipal principal, UUID id) {
    ProcurementOrderDocument document = requireContractDocument(principal, id);
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", document.getId());
    view.put("fileName", document.getFileName());
    view.put("contentType", document.getContentType());
    return view;
  }

  private ProcurementOrderDocument requireContractDocument(
      SupplierPortalPrincipal principal,
      UUID id
  ) {
    ProcurementOrderDocument document = orderDocuments.findById(id)
        .orElseThrow(() -> new BusinessException("采购合同附件不存在"));
    PurchaseOrder order = orders.findById(document.getOrderId())
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (!order.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权访问该合同附件");
    }
    return document;
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
  public List<QuoteRevisionResponse> listQuoteRevisions(
      SupplierPortalPrincipal principal,
      UUID inquiryId
  ) {
    requireInvitation(inquiryId, principal.supplierId());
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("报价不存在"));
    return revisions.findByQuoteIdOrderByVersionNoDesc(quote.getId()).stream()
        .map(revision -> {
          Map<String, Object> snapshot;
          try {
            snapshot = objectMapper.readValue(revision.getSnapshotJson(),
                new TypeReference<Map<String, Object>>() {});
          } catch (JsonProcessingException exception) {
            throw new BusinessException("报价版本快照解析失败");
          }
          return new QuoteRevisionResponse(revision.getId(), revision.getVersionNo(),
              revision.getSubmissionSource(), revision.getSubmittedByName(),
              revision.getSubmittedAt(), snapshot);
        }).toList();
  }

  @Transactional(readOnly = true)
  public List<PortalChangeRequestResponse> listChangeRequests(SupplierPortalPrincipal principal) {
    return supplierChanges.findBySupplierIdOrderByCreatedAtDesc(principal.supplierId()).stream()
        .map(this::changeView).toList();
  }

  @Transactional
  public PortalChangeRequestResponse createChangeRequest(
      SupplierPortalPrincipal principal,
      PortalChangeRequest request
  ) {
    Supplier supplier = requireSupplier(principal.supplierId());
    if (!"APPROVED".equals(supplier.getAdmissionStatus())) {
      throw new BusinessException("供应商准入通过后才能提交信息变更申请");
    }
    if (supplierChanges.existsBySupplierIdAndStatus(supplier.getId(), "PENDING")) {
      throw new BusinessException("已有待审批的变更申请，请等待处理完成");
    }
    String changeType = request.changeType().trim().toUpperCase();
    if (!Set.of("NAME", "CREDIT_CODE", "BANK_INFO", "SETTLEMENT_TERMS").contains(changeType)) {
      throw new BusinessException("不支持的变更类型");
    }
    if ("NAME".equals(changeType) && isBlank(request.proposedName())) {
      throw new BusinessException("企业名称变更必须填写新的企业名称");
    }
    if ("CREDIT_CODE".equals(changeType) && isBlank(request.proposedCreditCode())) {
      throw new BusinessException("信用代码变更必须填写新的统一社会信用代码");
    }
    if ("BANK_INFO".equals(changeType)
        && isBlank(request.proposedBankName()) && isBlank(request.proposedBankAccount())) {
      throw new BusinessException("银行信息变更必须填写开户银行或银行账号");
    }
    if ("SETTLEMENT_TERMS".equals(changeType) && isBlank(request.proposedSettlementTerms())) {
      throw new BusinessException("结算条款变更必须填写新的结算条款");
    }
    SupplierChangeRequest change = new SupplierChangeRequest();
    change.setSupplierId(supplier.getId());
    change.setChangeType(changeType);
    change.setProposedName(trim(request.proposedName()));
    change.setProposedCreditCode(trim(request.proposedCreditCode()));
    change.setProposedBankName(trim(request.proposedBankName()));
    change.setProposedBankAccount(trim(request.proposedBankAccount()));
    change.setProposedSettlementTerms(trim(request.proposedSettlementTerms()));
    change.setReason(request.reason().trim());
    change.setRequestedByName(principal.contactName());
    change.setRequestSource("PORTAL");
    change.setStatus("PENDING");
    return changeView(supplierChanges.save(change));
  }

  @Transactional(readOnly = true)
  public List<PerformanceReviewResponse> listPerformanceReviews(SupplierPortalPrincipal principal) {
    return performanceReviews.findBySupplierIdOrderByReviewPeriodDesc(principal.supplierId()).stream()
        .map(item -> new PerformanceReviewResponse(item.getId(), item.getReviewPeriod(),
            item.getOnTimeRate(), item.getQualityRate(), item.getInvoiceMatchRate(),
            item.getResponseScore(), item.getTotalScore(), item.getGrade(),
            item.getReviewerName(), item.getImprovementAction(), item.getStatus(),
            item.getAppealStatus(), item.getAppealReason(), item.getAppealedAt(),
            item.getAppealResolution(), item.getAppealReviewComment(),
            item.getAppealReviewedBy(), item.getAppealReviewedAt(),
            item.getCreatedAt()))
        .toList();
  }

  @Transactional
  public PerformanceReviewResponse appealPerformanceReview(
      SupplierPortalPrincipal principal,
      UUID reviewId,
      String reason
  ) {
    Supplier supplier = requireActiveSupplier(principal);
    SupplierPerformanceReview review = performanceReviews.findById(reviewId)
        .orElseThrow(() -> new BusinessException("绩效评价不存在"));
    if (!review.getSupplierId().equals(supplier.getId())) {
      throw new BusinessException("无权对该绩效评价发起申诉");
    }
    if (!"NONE".equals(review.getAppealStatus())) {
      throw new BusinessException("该绩效评价已发起申诉，请等待采购方处理");
    }
    if (reason == null || reason.isBlank()) {
      throw new BusinessException("请填写申诉理由");
    }
    review.setAppealStatus("PENDING");
    review.setAppealReason(reason.trim());
    review.setAppealedAt(OffsetDateTime.now());
    SupplierPerformanceReview saved = performanceReviews.save(review);
    Supplier supplierInfo = suppliers.findById(saved.getSupplierId()).orElse(supplier);
    internalNotifier.notifyProcurementStaff(
        "PROCUREMENT",
        "供应商发起绩效申诉",
        supplierInfo.getName() + " 对 " + saved.getReviewPeriod()
            + " 期绩效评价发起申诉（得分 " + saved.getTotalScore() + "），请及时处理。",
        "PERFORMANCE_APPEAL", saved.getId(),
        "PORTAL_PERFORMANCE_APPEAL:" + saved.getId());
    return new PerformanceReviewResponse(saved.getId(), saved.getReviewPeriod(),
        saved.getOnTimeRate(), saved.getQualityRate(), saved.getInvoiceMatchRate(),
        saved.getResponseScore(), saved.getTotalScore(), saved.getGrade(),
        saved.getReviewerName(), saved.getImprovementAction(), saved.getStatus(),
        saved.getAppealStatus(), saved.getAppealReason(), saved.getAppealedAt(),
        saved.getAppealResolution(), saved.getAppealReviewComment(),
        saved.getAppealReviewedBy(), saved.getAppealReviewedAt(),
        saved.getCreatedAt());
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
  public OpenAccountResponse openAccount(UUID supplierId, OpenAccountRequest request) {
    Supplier supplier = requireSupplier(supplierId);
    if (accounts.existsBySupplierId(supplierId)) {
      throw new BusinessException("该供应商已经开通门户账号，请使用重置密码或停用账号功能");
    }
    String email = normalizeEmail(request.email());
    if (accounts.existsByEmailIgnoreCase(email)) {
      throw new BusinessException("该邮箱已经绑定其他供应商门户账号");
    }
    String temporaryPassword = temporaryPassword();
    SupplierPortalAccount account = new SupplierPortalAccount();
    account.setTenantId(TenantContext.currentTenant());
    account.setSupplierId(supplierId);
    account.setEmail(email);
    account.setPhone(trim(request.phone()) == null ? supplier.getPhone() : trim(request.phone()));
    account.setContactName(request.contactName().trim());
    account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
    account.setStatus("ACTIVE");
    account.setMustChangePassword(true);
    account.setReviewedByName(currentInternalName());
    account.setReviewedAt(OffsetDateTime.now());
    account.setReviewComment("采购管理员为已有供应商开通门户账号");
    SupplierPortalAccount saved = accounts.save(account);
    return new OpenAccountResponse(temporaryPassword, account(saved, supplier));
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
    String temporaryPassword = temporaryPassword();
    account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
    account.setMustChangePassword(true);
    account.setPasswordChangedAt(OffsetDateTime.now());
    account.bumpAuthVersion();
    SupplierPortalAccount saved = accounts.save(account);
    return new ResetPasswordResponse(temporaryPassword, account(saved, requireSupplier(saved.getSupplierId())));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listNotifications(SupplierPortalPrincipal principal, OffsetDateTime before) {
    List<SupplierPortalNotification> page = before == null
        ? notifications.findTop100ByAccountIdOrderByCreatedAtDesc(principal.accountId())
        : notifications.findTop100ByAccountIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            principal.accountId(), before);
    List<SupplierPortalNotificationResponse> items = page.stream()
        .limit(100)
        .map(this::notificationView)
        .toList();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", items);
    result.put("hasMore", page.size() > items.size());
    return result;
  }

  @Transactional(readOnly = true)
  public long unreadNotificationCount(SupplierPortalPrincipal principal) {
    return notifications.countByAccountIdAndReadFalse(principal.accountId());
  }

  @Transactional
  public void markNotificationRead(SupplierPortalPrincipal principal, UUID id) {
    SupplierPortalNotification notification = notifications.findById(id)
        .orElseThrow(() -> new BusinessException("通知不存在"));
    if (!notification.getAccountId().equals(principal.accountId())) {
      throw new BusinessException("无权操作该通知");
    }
    notification.setRead(true);
    notification.setReadAt(OffsetDateTime.now());
    notifications.save(notification);
  }

  @Transactional
  public void markAllNotificationsRead(SupplierPortalPrincipal principal) {
    notifications.markAllRead(principal.accountId(), OffsetDateTime.now());
  }

  private SupplierPortalNotificationResponse notificationView(SupplierPortalNotification item) {
    return new SupplierPortalNotificationResponse(
        item.getId(), item.getType(), item.getTitle(), item.getContent(),
        item.getRelatedType(), item.getRelatedId(), item.isRead(),
        item.getReadAt(), item.getCreatedAt());
  }

  @Transactional(readOnly = true)
  public List<ProcurementShipmentResponse> listMyShipments(SupplierPortalPrincipal principal) {
    return shipments.findBySupplierIdOrderByCreatedAtDesc(principal.supplierId()).stream()
        .map(item -> shipmentView(item, null)).toList();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listMyInvoices(SupplierPortalPrincipal principal) {
    return invoices.findBySupplierIdOrderByCreatedAtDesc(principal.supplierId()).stream()
        .map(this::invoiceView).toList();
  }

  @Transactional
  public InvoiceSubmissionResponse submitInvoice(
      SupplierPortalPrincipal principal,
      SubmitInvoiceRequest request,
      MultipartFile file
  ) {
    Supplier supplier = requireActiveSupplier(principal);
    PurchaseOrder order = orders.findById(request.orderId())
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (!order.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权为该订单提交开票资料");
    }
    String invoiceNo = trim(request.invoiceNo());
    if (isBlank(invoiceNo)) {
      throw new BusinessException("请填写发票号码");
    }
    if (invoices.existsByInvoiceNo(invoiceNo)) {
      throw new BusinessException("发票号码 " + invoiceNo + " 已登记，请勿重复提交");
    }
    boolean duplicated = invoiceSubmissions
        .findByInvoiceNoIgnoreCaseAndStatus(invoiceNo, "PENDING").stream()
        .anyMatch(item -> item.getSupplierId().equals(principal.supplierId()));
    if (duplicated) {
      throw new BusinessException("该发票号码已有待审核的开票资料，请勿重复提交");
    }
    if (invoiceSubmissions.countBySupplierId(principal.supplierId()) >= 50) {
      throw new BusinessException("开票资料提交数量已达上限，请联系采购方处理");
    }
    FileStorageService.StoredFile stored = null;
    try {
      String checksum = sha256(file);
      stored = storage.store(file, "supplier-invoices", DOCUMENT_POLICY);
      SupplierInvoiceSubmission submission = new SupplierInvoiceSubmission();
      submission.setSupplierId(principal.supplierId());
      submission.setAccountId(principal.accountId());
      submission.setOrderId(order.getId());
      submission.setInvoiceNo(invoiceNo);
      submission.setAmount(request.amount());
      submission.setTaxRate(request.taxRate());
      submission.setInvoiceDate(request.invoiceDate());
      submission.setRemark(trim(request.remark()));
      submission.setFileName(stored.originalName());
      submission.setObjectKey(stored.objectKey());
      submission.setContentType(stored.contentType());
      submission.setSizeBytes(stored.sizeBytes());
      submission.setSha256(checksum);
      SupplierInvoiceSubmission saved = invoiceSubmissions.save(submission);
      recordActivity(requireAccount(principal.accountId()), "INVOICE_SUBMIT",
          "提交开票资料 " + order.getCode() + "，发票号 " + invoiceNo, null);
      internalNotifier.notifyProcurementStaff(
          "PROCUREMENT",
          "供应商提交开票资料",
          supplier.getName() + " 提交开票资料（订单 " + order.getCode()
              + "，发票号 " + invoiceNo + "），请及时审核。",
          "INVOICE_SUBMISSION", saved.getId(),
          "PORTAL_INVOICE_SUBMISSION:" + saved.getId());
      return invoiceSubmissionView(saved, order.getCode(), null);
    } catch (RuntimeException exception) {
      if (stored != null) storage.delete(stored.relativePath());
      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public List<InvoiceSubmissionResponse> listMyInvoiceSubmissions(
      SupplierPortalPrincipal principal
  ) {
    return invoiceSubmissions.findBySupplierIdOrderByCreatedAtDesc(principal.supplierId()).stream()
        .map(item -> invoiceSubmissionView(item, orderCode(item.getOrderId()), null))
        .toList();
  }

  @Transactional(readOnly = true)
  public Resource loadInvoiceSubmission(SupplierPortalPrincipal principal, UUID id) {
    SupplierInvoiceSubmission submission = requireInvoiceSubmission(id);
    if (!submission.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权访问该开票资料");
    }
    return storage.loadInNamespace("supplier-invoices", submission.getObjectKey());
  }

  @Transactional
  public void deleteInvoiceSubmission(SupplierPortalPrincipal principal, UUID id) {
    SupplierInvoiceSubmission submission = requireInvoiceSubmission(id);
    if (!submission.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权删除该开票资料");
    }
    if (!"PENDING".equals(submission.getStatus())) {
      throw new BusinessException("只有待审核的开票资料可以删除");
    }
    invoiceSubmissions.delete(submission);
    storage.deleteInNamespace("supplier-invoices", submission.getObjectKey());
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listMyPayables(SupplierPortalPrincipal principal) {
    return payables.findBySupplierIdOrderByCreatedAtDesc(principal.supplierId()).stream()
        .map(this::payableView).toList();
  }

  @Transactional(readOnly = true)
  public Resource loadPaymentReceipt(SupplierPortalPrincipal principal, UUID payableId) {
    requireActiveSupplier(principal);
    ProcurementPayable payable = payables.findById(payableId)
        .orElseThrow(() -> new BusinessException("应付单不存在"));
    if (!payable.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权访问该付款回单");
    }
    if (payable.getPaymentReceiptObjectKey() == null) {
      throw new BusinessException("该应付单尚未上传付款回单");
    }
    return storage.loadInNamespace("payment-receipts", payable.getPaymentReceiptObjectKey());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> financeSummary(SupplierPortalPrincipal principal) {
    UUID supplierId = principal.supplierId();
    SupplierInvoiceRepository.InvoiceSupplierTotals invoiceTotals =
        invoices.aggregateBySupplier(supplierId);
    ProcurementPayableRepository.PayableSupplierTotals payableTotals =
        payables.aggregateBySupplier(supplierId, LocalDate.now(),
            PayableStatus.PAID, PayableStatus.CANCELLED);
    BigDecimal payableAmount = payableTotals.getPayableAmount();
    BigDecimal paidAmount = payableTotals.getPaidAmount();
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("invoiceCount", invoiceTotals.getInvoiceCount());
    view.put("invoiceAmount", invoiceTotals.getInvoiceAmount());
    view.put("payableCount", payableTotals.getPayableCount());
    view.put("payableAmount", payableAmount);
    view.put("paidAmount", paidAmount);
    view.put("outstandingAmount", payableAmount.subtract(paidAmount));
    view.put("overdueAmount", payableTotals.getOverdueAmount());
    return view;
  }

  @Transactional(readOnly = true)
  public byte[] exportQuotePdf(SupplierPortalPrincipal principal, UUID inquiryId) {
    requireActiveSupplier(principal);
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("报价不存在"));
    ProcurementInquiry inquiry = inquiries.findById(inquiryId)
        .orElseThrow(() -> new BusinessException("询价不存在"));
    Supplier supplier = suppliers.findById(principal.supplierId()).orElse(null);
    List<SupplierQuotationLine> lines = quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId());
    return buildQuotePdf(inquiry, quote, lines, supplier);
  }

  @Transactional(readOnly = true)
  public byte[] exportOrderPdf(SupplierPortalPrincipal principal, UUID orderId) {
    requireActiveSupplier(principal);
    PurchaseOrder order = orders.findById(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (!order.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权导出该订单");
    }
    ProcurementContract contract = order.getContractId() == null
        ? null : contracts.findById(order.getContractId()).orElse(null);
    Supplier supplier = suppliers.findById(principal.supplierId()).orElse(null);
    return buildOrderPdf(order, contract, supplier);
  }

  private byte[] buildQuotePdf(
      ProcurementInquiry inquiry,
      SupplierQuotation quote,
      List<SupplierQuotationLine> lines,
      Supplier supplier
  ) {
    Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      PdfWriter.getInstance(doc, out);
      doc.open();
      Font title = pdfFont(18, Font.BOLD);
      Font head = pdfFont(12, Font.NORMAL);
      Font small = pdfFont(10, Font.NORMAL);
      Map<UUID, PurchaseRequest> requestMap = purchaseRequests.findAllById(
          lines.stream().map(SupplierQuotationLine::getRequestId).toList()
      ).stream().collect(Collectors.toMap(PurchaseRequest::getId, item -> item, (a, b) -> a));
      Paragraph p = new Paragraph("报价单 " + inquiry.getCode(), title);
      p.setAlignment(Element.ALIGN_CENTER);
      doc.add(p);
      doc.add(new Paragraph("供应商：" + (supplier == null ? "" : supplier.getName()), head));
      doc.add(new Paragraph("询价主题：" + inquiry.getTitle(), head));
      doc.add(new Paragraph("报价有效期：" + (quote.getValidUntil() == null ? "—" : quote.getValidUntil()), head));
      doc.add(new Paragraph("币种：" + quote.getCurrency() + "　提交时间："
          + (quote.getSubmittedAt() == null ? "—" : quote.getSubmittedAt().toLocalDate()), head));
      doc.add(new Paragraph(" "));
      PdfPTable table = new PdfPTable(7);
      table.setWidthPercentage(100);
      table.setWidths(new float[]{1f, 2.4f, 1.2f, 1.4f, 1f, 1.6f, 2f});
      String[] headers = {"序号", "物料", "数量", "含税单价", "税率", "交付日期", "备注"};
      for (String header : headers) {
        PdfPCell cell = new PdfPCell(new Phrase(header, head));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
      }
      int index = 1;
      for (SupplierQuotationLine line : lines) {
        PurchaseRequest source = requestMap.get(line.getRequestId());
        table.addCell(new Phrase(String.valueOf(index++), small));
        table.addCell(new Phrase(source == null ? String.valueOf(line.getRequestId())
            : source.getPartName() == null ? source.getCode() : source.getPartName(), small));
        table.addCell(new Phrase(plain(line.getQuantity()), small));
        table.addCell(new Phrase(plain(line.getUnitPrice()), small));
        table.addCell(new Phrase(plain(line.getTaxRate()) + "%", small));
        table.addCell(new Phrase(line.getDeliveryDate() == null ? "—" : line.getDeliveryDate().toString(), small));
        table.addCell(new Phrase(line.getRemark() == null ? "" : line.getRemark(), small));
      }
      doc.add(table);
      doc.add(new Paragraph(" "));
      BigDecimal material = lines.stream()
          .map(l -> value(l.getQuantity()).multiply(value(l.getUnitPrice())))
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      doc.add(new Paragraph("物料金额：" + plain(material), head));
      doc.add(new Paragraph("运费：" + plain(quote.getFreightAmount()), head));
      doc.add(new Paragraph("其他费用：" + plain(quote.getOtherCostAmount()), head));
      doc.add(new Paragraph("报价总额：" + plain(material.add(value(quote.getFreightAmount())).add(value(quote.getOtherCostAmount()))), title));
      if (isNotBlank(quote.getRemark())) {
        doc.add(new Paragraph("报价说明：" + quote.getRemark(), small));
      }
      doc.close();
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("生成报价 PDF 失败");
    }
    return out.toByteArray();
  }

  private byte[] buildOrderPdf(
      PurchaseOrder order,
      ProcurementContract contract,
      Supplier supplier
  ) {
    Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      PdfWriter.getInstance(doc, out);
      doc.open();
      Font title = pdfFont(18, Font.BOLD);
      Font head = pdfFont(12, Font.NORMAL);
      Paragraph p = new Paragraph("采购订单 " + order.getCode(), title);
      p.setAlignment(Element.ALIGN_CENTER);
      doc.add(p);
      doc.add(new Paragraph("供应商：" + (supplier == null ? "" : supplier.getName()), head));
      doc.add(new Paragraph("物料：" + order.getPartName(), head));
      doc.add(new Paragraph("订购数量：" + plain(order.getOrderedQty())
          + "　单价：" + plain(order.getUnitPrice())
          + "　税率：" + plain(order.getTaxRate()) + "%", head));
      doc.add(new Paragraph("订单总额：" + plain(order.getOrderAmount()), title));
      doc.add(new Paragraph("预计交货：" + (order.getExpectedDeliveryDate() == null
          ? "未设置" : order.getExpectedDeliveryDate()), head));
      if (contract != null) {
        doc.add(new Paragraph("合同编号：" + contract.getContractNo() + "　合同名称：" + contract.getName(), head));
        doc.add(new Paragraph("付款条款：" + (contract.getPaymentTerms() == null ? "—" : contract.getPaymentTerms()), head));
      }
      if (isNotBlank(order.getSourceReason())) {
        doc.add(new Paragraph("采购说明：" + order.getSourceReason(), head));
      }
      doc.close();
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("生成订单 PDF 失败");
    }
    return out.toByteArray();
  }

  @Transactional(readOnly = true)
  public byte[] exportQuoteExcel(SupplierPortalPrincipal principal, UUID inquiryId) {
    requireActiveSupplier(principal);
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, principal.supplierId())
        .orElseThrow(() -> new BusinessException("报价不存在"));
    ProcurementInquiry inquiry = inquiries.findById(inquiryId)
        .orElseThrow(() -> new BusinessException("询价不存在"));
    List<SupplierQuotationLine> lines = quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId());
    Map<UUID, PurchaseRequest> requestMap = purchaseRequests.findAllById(
        lines.stream().map(SupplierQuotationLine::getRequestId).toList()
    ).stream().collect(Collectors.toMap(PurchaseRequest::getId, item -> item, (a, b) -> a));
    BigDecimal material = lines.stream()
        .map(l -> value(l.getQuantity()).multiply(value(l.getUnitPrice())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal total = material
        .add(value(quote.getFreightAmount()))
        .add(value(quote.getOtherCostAmount()));
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("报价单");
      String[] headers = {"询价单", "主题", "供应商", "币种", "报价有效期", "提交时间", "总额"};
      headerRow(sheet, headers);
      Row meta = sheet.createRow(1);
      cell(meta, 0, inquiry.getCode());
      cell(meta, 1, inquiry.getTitle());
      cell(meta, 2, suppliers.findById(principal.supplierId()).map(Supplier::getName).orElse(""));
      cell(meta, 3, quote.getCurrency());
      cell(meta, 4, quote.getValidUntil());
      cell(meta, 5, quote.getSubmittedAt());
      cell(meta, 6, total);
      Sheet linesSheet = workbook.createSheet("分项报价");
      String[] lineHeaders = {"序号", "物料", "数量", "含税单价", "税率%", "交付日期", "备注"};
      headerRow(linesSheet, lineHeaders);
      int rowIndex = 1;
      int index = 1;
      for (SupplierQuotationLine line : lines) {
        PurchaseRequest source = requestMap.get(line.getRequestId());
        Row row = linesSheet.createRow(rowIndex++);
        int col = 0;
        cell(row, col++, index++);
        cell(row, col++, source == null ? String.valueOf(line.getRequestId())
            : source.getPartName() == null ? source.getCode() : source.getPartName());
        cell(row, col++, line.getQuantity());
        cell(row, col++, line.getUnitPrice());
        cell(row, col++, line.getTaxRate());
        cell(row, col++, line.getDeliveryDate());
        cell(row, col++, line.getRemark());
      }
      autoSize(linesSheet, lineHeaders.length);
      return toBytes(workbook);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("生成报价 Excel 失败");
    }
  }

  @Transactional(readOnly = true)
  public byte[] exportOrderExcel(SupplierPortalPrincipal principal, UUID orderId) {
    requireActiveSupplier(principal);
    PurchaseOrder order = orders.findById(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (!order.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权导出该订单");
    }
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("采购订单");
      String[] headers = {"订单号", "物料", "数量", "已收", "单价", "税率%", "总额", "预计交货", "状态", "创建时间"};
      headerRow(sheet, headers);
      Row row = sheet.createRow(1);
      int col = 0;
      cell(row, col++, order.getCode());
      cell(row, col++, order.getPartName());
      cell(row, col++, order.getOrderedQty());
      cell(row, col++, order.getReceivedQty());
      cell(row, col++, order.getUnitPrice());
      cell(row, col++, order.getTaxRate());
      cell(row, col++, order.getOrderAmount());
      cell(row, col++, order.getExpectedDeliveryDate());
      cell(row, col++, order.getStatus() == null ? "" : order.getStatus().name());
      cell(row, col++, order.getCreatedAt());
      Sheet records = workbook.createSheet("收货与发货");
      String[] recordHeaders = {"类型", "订单号", "单号", "送货单号/数量", "承运方/收货人", "时间", "说明"};
      headerRow(records, recordHeaders);
      int rowIndex = 1;
      for (ProcurementShipment shipment : shipments.findByOrderIdOrderByCreatedAtDesc(orderId)) {
        Row record = records.createRow(rowIndex++);
        int c = 0;
        cell(record, c++, "发货");
        cell(record, c++, order.getCode());
        cell(record, c++, "");
        cell(record, c++, shipment.getDeliveryNo());
        cell(record, c++, shipment.getCarrier());
        cell(record, c++, shipment.getCreatedAt());
        cell(record, c++, shipment.getRemark());
      }
      for (GoodsReceipt receipt : receipts.findByOrderId(orderId)) {
        Row record = records.createRow(rowIndex++);
        int c = 0;
        cell(record, c++, "收货");
        cell(record, c++, order.getCode());
        cell(record, c++, receipt.getCode());
        cell(record, c++, receipt.getQuantity());
        cell(record, c++, receipt.getReceiverName());
        cell(record, c++, receipt.getReceivedDate());
        cell(record, c++, receipt.getInspectionStatus());
      }
      autoSize(records, recordHeaders.length);
      return toBytes(workbook);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("生成订单 Excel 失败");
    }
  }

  @Transactional(readOnly = true)
  public byte[] exportFinanceExcel(SupplierPortalPrincipal principal) {
    UUID supplierId = principal.supplierId();
    try (Workbook workbook = new XSSFWorkbook()) {
      Map<String, Object> summary = financeSummary(principal);
      Sheet summarySheet = workbook.createSheet("对账汇总");
      String[] summaryHeaders = {"累计开票", "应付总额", "已付金额", "待付金额", "逾期未付"};
      headerRow(summarySheet, summaryHeaders);
      Row summaryRow = summarySheet.createRow(1);
      int sc = 0;
      cell(summaryRow, sc++, summary.get("invoiceAmount"));
      cell(summaryRow, sc++, summary.get("payableAmount"));
      cell(summaryRow, sc++, summary.get("paidAmount"));
      cell(summaryRow, sc++, summary.get("outstandingAmount"));
      cell(summaryRow, sc++, summary.get("overdueAmount"));
      autoSize(summarySheet, summaryHeaders.length);

      Sheet invoiceSheet = workbook.createSheet("发票记录");
      String[] invoiceHeaders = {"发票号", "订单", "开票日期", "金额", "状态", "匹配", "创建时间"};
      headerRow(invoiceSheet, invoiceHeaders);
      int rowIndex = 1;
      for (Map<String, Object> invoice : listMyInvoices(principal)) {
        Row row = invoiceSheet.createRow(rowIndex++);
        int c = 0;
        cell(row, c++, invoice.get("invoiceNo"));
        cell(row, c++, invoice.get("orderCode"));
        cell(row, c++, invoice.get("invoiceDate"));
        cell(row, c++, invoice.get("amount"));
        cell(row, c++, invoice.get("status"));
        cell(row, c++, invoice.get("matchStatus"));
        cell(row, c++, invoice.get("createdAt"));
      }
      autoSize(invoiceSheet, invoiceHeaders.length);

      Sheet payableSheet = workbook.createSheet("应付与付款");
      String[] payableHeaders = {"应付单号", "订单", "应付金额", "已付金额", "待付金额", "到期日", "状态", "创建时间"};
      headerRow(payableSheet, payableHeaders);
      rowIndex = 1;
      for (Map<String, Object> payable : listMyPayables(principal)) {
        Row row = payableSheet.createRow(rowIndex++);
        int c = 0;
        cell(row, c++, payable.get("code"));
        cell(row, c++, payable.get("orderCode"));
        cell(row, c++, payable.get("amount"));
        cell(row, c++, payable.get("paidAmount"));
        cell(row, c++, payable.get("outstandingAmount"));
        cell(row, c++, payable.get("dueDate"));
        cell(row, c++, payable.get("status"));
        cell(row, c++, payable.get("createdAt"));
      }
      autoSize(payableSheet, payableHeaders.length);
      return toBytes(workbook);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("生成对账 Excel 失败");
    }
  }

  private void headerRow(Sheet sheet, String[] headers) {
    CellStyle style = sheet.getWorkbook().createCellStyle();
    org.apache.poi.ss.usermodel.Font font = sheet.getWorkbook().createFont();
    font.setBold(true);
    style.setFont(font);
    Row row = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = row.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(style);
    }
  }

  private void autoSize(Sheet sheet, int columns) {
    for (int i = 0; i < columns; i++) {
      sheet.autoSizeColumn(i);
    }
  }

  private void cell(Row row, int col, Object value) {
    Cell cell = row.createCell(col);
    if (value == null) {
      cell.setCellValue("");
    } else if (value instanceof BigDecimal number) {
      cell.setCellValue(number.doubleValue());
    } else if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
    } else if (value instanceof LocalDate date) {
      cell.setCellValue(date.toString());
    } else if (value instanceof OffsetDateTime dateTime) {
      cell.setCellValue(dateTime.toLocalDateTime().toString().replace("T", " "));
    } else {
      cell.setCellValue(value.toString());
    }
  }

  private byte[] toBytes(Workbook workbook) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.write(out);
    return out.toByteArray();
  }

  private BaseFont pdfFont() {
    BaseFont current = pdfCjkFont;
    if (current != null) return current;
    synchronized (this) {
      if (pdfCjkFont != null) return pdfCjkFont;
      // 优先使用服务器本机中文字体（可嵌入 PDF）；macOS 与常见 Linux 发行版路径都尝试。
      String[] candidates = {
          "/System/Library/Fonts/STHeiti Light.ttc,0",
          "/System/Library/Fonts/PingFang.ttc,0",
          "/System/Library/Fonts/Hiragino Sans GB.ttc,0",
          "/System/Library/Fonts/Supplemental/Songti.ttc,0",
          "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
          "/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc",
          "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
          "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
          "/usr/share/fonts/truetype/arphic/uming.ttc",
      };
      for (String candidate : candidates) {
        try {
          pdfCjkFont = BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
          return pdfCjkFont;
        } catch (Exception ignored) {
          // 尝试下一个候选字体
        }
      }
      // openpdf 自带 Adobe 中文（简体）字体包，不依赖系统字体，可保证任何平台都能导出。
      try {
        pdfCjkFont = BaseFont.createFont(
            "STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        return pdfCjkFont;
      } catch (Exception fallbackFailure) {
        throw new BusinessException("服务器缺少中文字体，无法导出 PDF");
      }
    }
  }

  private Font pdfFont(float size, int style) {
    return new Font(pdfFont(), size, style);
  }

  private static String plain(BigDecimal value) {
    return value == null ? "0" : value.stripTrailingZeros().toPlainString();
  }

  private Map<String, Object> invoiceView(SupplierInvoice invoice) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", invoice.getId());
    view.put("code", invoice.getCode());
    view.put("invoiceNo", invoice.getInvoiceNo());
    view.put("orderCode", orderCode(invoice.getOrderId()));
    view.put("amount", invoice.getAmount());
    view.put("taxRate", invoice.getTaxRate());
    view.put("invoiceDate", invoice.getInvoiceDate());
    view.put("status", invoice.getStatus());
    view.put("approvalStatus", invoice.getApprovalStatus());
    view.put("verificationStatus", invoice.getVerificationStatus());
    view.put("matchStatus", invoice.getMatchStatus());
    view.put("matchedAmount", invoice.getMatchedAmount());
    view.put("differenceAmount", invoice.getDifferenceAmount());
    view.put("remark", invoice.getRemark());
    view.put("createdAt", invoice.getCreatedAt());
    return view;
  }

  private Map<String, Object> payableView(ProcurementPayable payable) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", payable.getId());
    view.put("code", payable.getCode());
    view.put("orderCode", orderCode(payable.getOrderId()));
    view.put("amount", payable.getAmount());
    view.put("taxRate", payable.getTaxRate());
    view.put("paidAmount", payable.getPaidAmount());
    view.put("outstandingAmount", payable.getAmount().subtract(payable.getPaidAmount()));
    view.put("dueDate", payable.getDueDate());
    view.put("status", payable.getStatus() == null ? null : payable.getStatus().name());
    view.put("paidAt", payable.getPaidAt());
    view.put("paymentNote", payable.getPaymentNote());
    view.put("paymentReceiptFileName", payable.getPaymentReceiptFileName());
    view.put("paymentReceiptContentType", payable.getPaymentReceiptContentType());
    view.put("paymentReceiptSizeBytes", payable.getPaymentReceiptSizeBytes());
    view.put("paymentReceiptUploadedBy", payable.getPaymentReceiptUploadedBy());
    view.put("paymentReceiptUploadedAt", payable.getPaymentReceiptUploadedAt());
    view.put("createdAt", payable.getCreatedAt());
    return view;
  }

  private String orderCode(UUID orderId) {
    if (orderId == null) return null;
    return orders.findById(orderId).map(PurchaseOrder::getCode).orElse(null);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listMyOrders(SupplierPortalPrincipal principal) {
    UUID supplierId = principal.supplierId();
    List<ProcurementContract> supplierContracts = contracts.findBySupplierIdOrderByCreatedAtDesc(supplierId);
    Map<UUID, ProcurementContract> contractsById = supplierContracts.stream()
        .collect(Collectors.toMap(ProcurementContract::getId, item -> item, (a, b) -> a));
    Map<UUID, ProcurementContract> contractByOrderId = supplierContracts.stream()
        .filter(item -> item.getOrderId() != null)
        .collect(Collectors.toMap(ProcurementContract::getOrderId, item -> item, (a, b) -> a));
    List<PurchaseOrder> orderList = orders.findBySupplierId(supplierId);
    List<UUID> orderIds = orderList.stream().map(PurchaseOrder::getId).toList();
    Map<UUID, List<ProcurementShipment>> shipmentsByOrder = shipments
        .findByOrderIdInOrderByCreatedAtDesc(orderIds).stream()
        .collect(Collectors.groupingBy(ProcurementShipment::getOrderId));
    Map<UUID, List<GoodsReceipt>> receiptsByOrder = receipts.findByOrderIdIn(orderIds).stream()
        .collect(Collectors.groupingBy(GoodsReceipt::getOrderId));
    Map<UUID, List<PurchaseOrderChange>> changesByOrder = orderChanges
        .findByOrderIdInOrderByCreatedAtDesc(orderIds).stream()
        .collect(Collectors.groupingBy(PurchaseOrderChange::getOrderId));
    Map<UUID, List<ProcurementOrderDocument>> documentsByOrder = orderDocuments
        .findByOrderIdInOrderByCreatedAtDesc(orderIds).stream()
        .collect(Collectors.groupingBy(ProcurementOrderDocument::getOrderId));
    List<Map<String, Object>> entries = new ArrayList<>();
    Set<UUID> seenOrderIds = new HashSet<>();
    for (PurchaseOrder order : orderList) {
      if (!seenOrderIds.add(order.getId())) continue;
      ProcurementContract contract = order.getContractId() != null
          ? contractsById.get(order.getContractId())
          : contractByOrderId.get(order.getId());
      entries.add(orderEntry(order, contract,
          shipmentsByOrder.getOrDefault(order.getId(), List.of()),
          receiptsByOrder.getOrDefault(order.getId(), List.of()),
          changesByOrder.getOrDefault(order.getId(), List.of()),
          documentsByOrder.getOrDefault(order.getId(), List.of())));
    }
    for (ProcurementContract contract : supplierContracts) {
      if (contract.getOrderId() != null && seenOrderIds.contains(contract.getOrderId())) continue;
      if ("REJECTED".equals(contract.getStatus()) || "SUPERSEDED".equals(contract.getStatus())) continue;
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("order", null);
      entry.put("contract", contractView(contract));
      entry.put("inquiry", inquiryRef(contract.getInquiryId()));
      entry.put("quote", contract.getInquiryId() == null
          ? null : selectedQuoteView(contract.getInquiryId(), supplierId));
      entry.put("quoteAttachments", contract.getInquiryId() == null
          ? List.of() : quoteAttachmentsView(contract.getInquiryId(), supplierId));
      entry.put("shipments", List.of());
      entry.put("receipts", List.of());
      entry.put("changes", List.of());
      entry.put("documents", List.of());
      entries.add(entry);
    }
    return entries;
  }

  private Map<String, Object> orderEntry(
      PurchaseOrder order,
      ProcurementContract contract,
      List<ProcurementShipment> orderShipments,
      List<GoodsReceipt> orderReceipts,
      List<PurchaseOrderChange> orderChanges,
      List<ProcurementOrderDocument> orderDocuments
  ) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("order", orderView(order));
    entry.put("contract", contract == null ? null : contractView(contract));
    entry.put("inquiry", inquiryRef(order.getInquiryId()));
    entry.put("quote", order.getInquiryId() == null
        ? null : selectedQuoteView(order.getInquiryId(), order.getSupplierId()));
    entry.put("quoteAttachments", order.getInquiryId() == null
        ? List.of() : quoteAttachmentsView(order.getInquiryId(), order.getSupplierId()));
    entry.put("shipments", orderShipments.stream()
        .map(item -> shipmentView(item, order.getCode())).toList());
    entry.put("receipts", orderReceipts.stream().map(this::receiptView).toList());
    entry.put("changes", orderChanges.stream().map(this::orderChangeView).toList());
    entry.put("documents", orderDocuments.stream().map(this::orderDocumentView).toList());
    return entry;
  }

  private Map<String, Object> orderView(PurchaseOrder order) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", order.getId());
    view.put("code", order.getCode());
    view.put("partName", order.getPartName());
    view.put("orderedQty", order.getOrderedQty());
    view.put("receivedQty", order.getReceivedQty());
    view.put("unitPrice", order.getUnitPrice());
    view.put("taxRate", order.getTaxRate());
    view.put("orderAmount", order.getOrderAmount());
    BigDecimal materialAmount = value(order.getOrderedQty()).multiply(value(order.getUnitPrice()));
    BigDecimal remainingQty = value(order.getOrderedQty()).subtract(value(order.getReceivedQty()));
    view.put("materialAmount", materialAmount);
    view.put("remainingQty", remainingQty);
    view.put("remainingAmount", remainingQty.multiply(value(order.getUnitPrice())));
    view.put("currency", order.getCurrency());
    view.put("freightAmount", order.getFreightAmount());
    view.put("status", order.getStatus() == null ? null : order.getStatus().name());
    view.put("approvalStatus", order.getApprovalStatus() == null ? null : order.getApprovalStatus().name());
    view.put("approvalComment", order.getApprovalComment());
    view.put("approverName", order.getApproverName());
    view.put("approvedAt", order.getApprovedAt());
    view.put("expectedDeliveryDate",
        order.getExpectedDeliveryDate() == null ? "" : order.getExpectedDeliveryDate().toString());
    view.put("costTargetName", order.getCostTargetName());
    view.put("sourceReason", order.getSourceReason());
    view.put("responsibleName", order.getResponsibleName());
    view.put("submittedAt", order.getSubmittedAt());
    view.put("closedAt", order.getClosedAt());
    view.put("createdAt", order.getCreatedAt());
    return view;
  }

  private Map<String, Object> selectedQuoteView(UUID inquiryId, UUID supplierId) {
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, supplierId).orElse(null);
    if (quote == null) return null;
    return portalQuote(quote, quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId()));
  }

  private List<QuoteAttachmentResponse> quoteAttachmentsView(UUID inquiryId, UUID supplierId) {
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiryId, supplierId).orElse(null);
    if (quote == null) return List.of();
    return quoteAttachments.findByQuoteIdOrderByCreatedAtDesc(quote.getId()).stream()
        .map(this::attachment).toList();
  }

  private Map<String, Object> receiptView(GoodsReceipt receipt) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", receipt.getId());
    view.put("code", receipt.getCode());
    view.put("quantity", receipt.getQuantity());
    view.put("unitPrice", receipt.getUnitPrice());
    view.put("taxRate", receipt.getTaxRate());
    view.put("amount", receipt.getAmount());
    view.put("receivedDate", receipt.getReceivedDate());
    view.put("deliveryNo", receipt.getDeliveryNo());
    view.put("carrier", receipt.getCarrier());
    view.put("receiverName", receipt.getReceiverName());
    view.put("inspectionStatus", receipt.getInspectionStatus());
    view.put("qualifiedQty", receipt.getQualifiedQty());
    view.put("rejectedQty", receipt.getRejectedQty());
    view.put("inspectorName", receipt.getInspectorName());
    view.put("inspectionComment", receipt.getInspectionComment());
    view.put("inspectedAt", receipt.getInspectedAt());
    view.put("appealStatus", receipt.getAppealStatus());
    view.put("appealReason", receipt.getAppealReason());
    view.put("appealedAt", receipt.getAppealedAt());
    view.put("appealResolution", receipt.getAppealResolution());
    view.put("appealReviewComment", receipt.getAppealReviewComment());
    view.put("appealReviewedBy", receipt.getAppealReviewedBy());
    view.put("appealReviewedAt", receipt.getAppealReviewedAt());
    return view;
  }

  private Map<String, Object> orderChangeView(PurchaseOrderChange change) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", change.getId());
    view.put("changeNo", change.getChangeNo());
    view.put("changeType", change.getChangeType());
    view.put("quantityBefore", change.getQuantityBefore());
    view.put("quantityAfter", change.getQuantityAfter());
    view.put("unitPriceBefore", change.getUnitPriceBefore());
    view.put("unitPriceAfter", change.getUnitPriceAfter());
    view.put("expectedDateBefore", change.getExpectedDateBefore());
    view.put("expectedDateAfter", change.getExpectedDateAfter());
    view.put("reason", change.getReason());
    view.put("status", change.getStatus());
    view.put("createdByName", change.getCreatedByName());
    view.put("decidedByName", change.getDecidedByName());
    view.put("decisionComment", change.getDecisionComment());
    view.put("supplierResponse", change.getSupplierResponse());
    view.put("supplierComment", change.getSupplierComment());
    view.put("supplierRespondedAt", change.getSupplierRespondedAt());
    view.put("createdAt", change.getCreatedAt());
    return view;
  }

  @Transactional
  public Map<String, Object> respondOrderChange(
      SupplierPortalPrincipal principal,
      UUID orderId,
      UUID changeId,
      RespondOrderChangeRequest request
  ) {
    requireActiveSupplier(principal);
    PurchaseOrder order = orders.findById(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (!order.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权处理该订单变更");
    }
    PurchaseOrderChange change = orderChanges.findById(changeId)
        .orElseThrow(() -> new BusinessException("订单变更不存在"));
    if (!change.getOrderId().equals(order.getId())) {
      throw new BusinessException("订单变更不属于该订单");
    }
    if (change.getSupplierResponse() != null) {
      throw new BusinessException("该变更已回应，不能重复操作");
    }
    String response = isBlank(request.response()) ? "" : request.response().trim().toUpperCase();
    if (!Set.of("AGREE", "OBJECT").contains(response)) {
      throw new BusinessException("变更回应类型不合法");
    }
    if ("OBJECT".equals(response) && isBlank(request.comment())) {
      throw new BusinessException("提出异议时请填写说明");
    }
    change.setSupplierResponse(response);
    change.setSupplierComment(trim(request.comment()));
    change.setSupplierRespondedAt(OffsetDateTime.now());
    return orderChangeView(orderChanges.save(change));
  }

  @Transactional
  public Map<String, Object> appealReceipt(
      SupplierPortalPrincipal principal,
      UUID receiptId,
      ReceiptAppealRequest request
  ) {
    Supplier supplier = requireActiveSupplier(principal);
    GoodsReceipt receipt = receipts.findById(receiptId)
        .orElseThrow(() -> new BusinessException("收货记录不存在"));
    PurchaseOrder order = orders.findById(receipt.getOrderId())
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (!order.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权对该收货记录发起申诉");
    }
    if (!"NONE".equals(receipt.getAppealStatus())) {
      throw new BusinessException("该收货记录已发起申诉");
    }
    String inspection = receipt.getInspectionStatus();
    if (!Set.of("REJECTED", "PARTIAL").contains(inspection)) {
      throw new BusinessException("只有质检不合格或部分合格时才能发起申诉");
    }
    receipt.setAppealStatus("PENDING");
    receipt.setAppealReason(trim(request.reason()));
    receipt.setAppealedAt(OffsetDateTime.now());
    internalNotifier.notifyProcurementStaff(
        "PROCUREMENT",
        "供应商发起质检申诉",
        supplier.getName() + " 对订单 " + order.getCode()
            + " 的收货记录发起质检申诉，请及时处理。",
        "APPEAL", receipt.getId(),
        "PORTAL_RECEIPT_APPEAL:" + receipt.getId());
    return receiptView(receipts.save(receipt));
  }

  private Map<String, Object> inquiryRef(UUID inquiryId) {
    if (inquiryId == null) return null;
    ProcurementInquiry inquiry = inquiries.findById(inquiryId).orElse(null);
    if (inquiry == null) return null;
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", inquiry.getId());
    view.put("code", inquiry.getCode());
    view.put("title", inquiry.getTitle());
    view.put("status", inquiry.getStatus());
    view.put("awardedAt", inquiry.getSelectedAt());
    view.put("selectionReason", inquiry.getSelectionReason());
    view.put("selectedByName", inquiry.getSelectedByName());
    return view;
  }

  @Transactional
  public ProcurementShipmentResponse createShipment(
      SupplierPortalPrincipal principal,
      UUID orderId,
      CreateShipmentRequest request
  ) {
    PurchaseOrder order = orders.findById(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (!order.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权为该订单回传发货信息");
    }
    String status = order.getStatus() == null ? "" : order.getStatus().name();
    if (!"ORDERED".equals(status) && !"PARTIAL_RECEIVED".equals(status)) {
      throw new BusinessException("只有已下单的订单可以回传发货信息");
    }
    String deliveryNo = trim(request.deliveryNo());
    if (isBlank(deliveryNo)) {
      throw new BusinessException("请填写送货单号");
    }
    if (!shipments.findByOrderIdAndDeliveryNo(order.getId(), deliveryNo).isEmpty()) {
      throw new BusinessException("该订单已存在相同的送货单号，请核对后重试");
    }
    if (shipments.countByOrderId(order.getId()) >= MAX_SHIPMENTS_PER_ORDER) {
      throw new BusinessException("该订单发货记录数量已达上限，请联系采购方处理");
    }
    ProcurementShipment shipment = new ProcurementShipment();
    shipment.setOrderId(order.getId());
    shipment.setSupplierId(principal.supplierId());
    shipment.setDeliveryNo(deliveryNo);
    shipment.setCarrier(trim(request.carrier()));
    shipment.setExpectedArrival(request.expectedArrival());
    shipment.setRemark(trim(request.remark()));
    shipment.setStatus("PENDING");
    ProcurementShipment saved = shipments.save(shipment);
    recordActivity(requireAccount(principal.accountId()), "SHIPMENT_CREATE",
        "回传发货信息 " + order.getCode() + "，送货单号 " + (saved.getDeliveryNo() == null ? "—" : saved.getDeliveryNo()), null);
    return shipmentView(saved, order.getCode());
  }

  @Transactional
  public ProcurementShipmentResponse updateShipment(
      SupplierPortalPrincipal principal,
      UUID shipmentId,
      CreateShipmentRequest request
  ) {
    requireActiveSupplier(principal);
    ProcurementShipment shipment = requireShipmentOwnership(principal, shipmentId);
    if (!"PENDING".equals(shipment.getStatus())) {
      throw new BusinessException("只有待确认的发货记录可以修改");
    }
    String deliveryNo = trim(request.deliveryNo());
    if (isBlank(deliveryNo)) {
      throw new BusinessException("请填写送货单号");
    }
    boolean duplicated = shipments.findByOrderIdAndDeliveryNo(shipment.getOrderId(), deliveryNo).stream()
        .anyMatch(item -> !item.getId().equals(shipmentId));
    if (duplicated) {
      throw new BusinessException("该订单已存在相同的送货单号，请核对后重试");
    }
    shipment.setDeliveryNo(deliveryNo);
    shipment.setCarrier(trim(request.carrier()));
    shipment.setExpectedArrival(request.expectedArrival());
    shipment.setRemark(trim(request.remark()));
    ProcurementShipment saved = shipments.save(shipment);
    PurchaseOrder order = orders.findById(saved.getOrderId()).orElse(null);
    recordActivity(requireAccount(principal.accountId()), "SHIPMENT_UPDATE",
        "修改发货信息 " + (order == null ? "" : order.getCode() + "，") + "送货单号 " + deliveryNo, null);
    return shipmentView(saved, order == null ? null : order.getCode());
  }

  @Transactional
  public void deleteShipment(SupplierPortalPrincipal principal, UUID shipmentId) {
    requireActiveSupplier(principal);
    ProcurementShipment shipment = requireShipmentOwnership(principal, shipmentId);
    if (!"PENDING".equals(shipment.getStatus())) {
      throw new BusinessException("只有待确认的发货记录可以删除");
    }
    for (SupplierShipmentAttachment attachment
        : shipmentAttachments.findByShipmentIdOrderByCreatedAtDesc(shipmentId)) {
      storage.deleteInNamespace("supplier-shipments", attachment.getObjectKey());
      shipmentAttachments.delete(attachment);
    }
    PurchaseOrder order = orders.findById(shipment.getOrderId()).orElse(null);
    recordActivity(requireAccount(principal.accountId()), "SHIPMENT_DELETE",
        "删除发货信息 " + (order == null ? "" : order.getCode() + "，")
            + "送货单号 " + (shipment.getDeliveryNo() == null ? "—" : shipment.getDeliveryNo()), null);
    shipments.delete(shipment);
  }

  @Transactional
  public ShipmentAttachmentResponse uploadShipmentAttachment(
      SupplierPortalPrincipal principal,
      UUID shipmentId,
      MultipartFile file
  ) {
    requireActiveSupplier(principal);
    requireShipmentOwnership(principal, shipmentId);
    assertUploadAllowed(file,
        shipmentAttachments.countByShipmentId(shipmentId), 10,
        shipmentAttachments.sumSizeByShipmentId(shipmentId), 100L * 1024 * 1024,
        "单次发货最多 10 个附件、总大小不超过 100MB，请先删除旧附件");
    FileStorageService.StoredFile stored = null;
    try {
      String checksum = sha256(file);
      stored = storage.store(file, "supplier-shipments", DOCUMENT_POLICY);
      SupplierShipmentAttachment attachment = new SupplierShipmentAttachment();
      attachment.setShipmentId(shipmentId);
      attachment.setSupplierId(principal.supplierId());
      attachment.setFileName(stored.originalName());
      attachment.setObjectKey(stored.objectKey());
      attachment.setContentType(stored.contentType());
      attachment.setSizeBytes(stored.sizeBytes());
      attachment.setSha256(checksum);
      return shipmentAttachment(shipmentAttachments.save(attachment));
    } catch (RuntimeException exception) {
      if (stored != null) storage.delete(stored.relativePath());
      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public List<ShipmentAttachmentResponse> listShipmentAttachments(
      SupplierPortalPrincipal principal,
      UUID shipmentId
  ) {
    requireShipmentOwnership(principal, shipmentId);
    return shipmentAttachments.findByShipmentIdOrderByCreatedAtDesc(shipmentId).stream()
        .map(this::shipmentAttachment).toList();
  }

  @Transactional(readOnly = true)
  public Resource loadShipmentAttachment(SupplierPortalPrincipal principal, UUID id) {
    SupplierShipmentAttachment attachment = requireShipmentAttachment(id);
    if (!attachment.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权访问该发货附件");
    }
    return storage.loadInNamespace("supplier-shipments", attachment.getObjectKey());
  }

  @Transactional
  public void deleteShipmentAttachment(SupplierPortalPrincipal principal, UUID id) {
    SupplierShipmentAttachment attachment = requireShipmentAttachment(id);
    if (!attachment.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权删除该发货附件");
    }
    shipmentAttachments.delete(attachment);
    storage.deleteInNamespace("supplier-shipments", attachment.getObjectKey());
  }

  private SupplierInvoiceSubmission requireInvoiceSubmission(UUID id) {
    return invoiceSubmissions.findById(id)
        .orElseThrow(() -> new BusinessException("开票资料不存在"));
  }

  private InvoiceSubmissionResponse invoiceSubmissionView(
      SupplierInvoiceSubmission item,
      String orderCode,
      String supplierName
  ) {
    return new InvoiceSubmissionResponse(
        item.getId(), item.getOrderId(), orderCode, supplierName,
        item.getInvoiceNo(), item.getAmount(), item.getTaxRate(),
        item.getInvoiceDate(), item.getRemark(), item.getFileName(),
        item.getContentType(), item.getSizeBytes(), item.getStatus(),
        item.getReviewComment(), item.getReviewedBy(), item.getReviewedAt(),
        item.getCreatedAt());
  }

  private void assertUploadAllowed(
      MultipartFile file,
      long count,
      long maxCount,
      long usedBytes,
      long maxBytes,
      String message
  ) {
    if (count >= maxCount || usedBytes + file.getSize() > maxBytes) {
      throw new BusinessException(message);
    }
  }

  private ProcurementShipment requireShipmentOwnership(
      SupplierPortalPrincipal principal,
      UUID shipmentId
  ) {
    ProcurementShipment shipment = shipments.findById(shipmentId)
        .orElseThrow(() -> new BusinessException("发货记录不存在"));
    if (!shipment.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权访问该发货记录");
    }
    return shipment;
  }

  private SupplierShipmentAttachment requireShipmentAttachment(UUID id) {
    return shipmentAttachments.findById(id)
        .orElseThrow(() -> new BusinessException("发货附件不存在"));
  }

  private ShipmentAttachmentResponse shipmentAttachment(SupplierShipmentAttachment item) {
    return new ShipmentAttachmentResponse(item.getId(), item.getShipmentId(),
        item.getFileName(), item.getContentType(), item.getSizeBytes(),
        item.getSha256(), item.getCreatedAt());
  }

  private ProcurementShipmentResponse shipmentView(ProcurementShipment item, String orderCode) {
    return new ProcurementShipmentResponse(
        item.getId(), item.getOrderId(), orderCode, item.getSupplierId(), null,
        item.getDeliveryNo(), item.getCarrier(), item.getExpectedArrival(),
        item.getRemark(), item.getStatus(), null, item.getCreatedAt(),
        item.getReviewComment(), item.getReviewedBy(), item.getReviewedAt());
  }

  @Transactional
  public Map<String, Object> acknowledgeContract(
      SupplierPortalPrincipal principal,
      UUID contractId
  ) {
    ProcurementContract contract = contracts.findById(contractId)
        .orElseThrow(() -> new BusinessException("采购合同不存在"));
    if (!contract.getSupplierId().equals(principal.supplierId())) {
      throw new BusinessException("无权确认该合同");
    }
    if (contract.getAcknowledgedAt() != null) {
      throw new BusinessException("该合同已确认，无需重复操作");
    }
    contract.setAcknowledgedAt(OffsetDateTime.now());
    contract.setAcknowledgedByName(principal.contactName());
    contracts.save(contract);
    recordActivity(requireAccount(principal.accountId()), "CONTRACT_ACK",
        "确认中标合同 " + contract.getContractNo(), null);
    return contractView(contract);
  }

  private String temporaryPassword() {
    return "Tmp" + UUID.randomUUID().toString().replace("-", "").substring(0, 9) + "!";
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
    InquiryClarification saved = clarifications.save(item);
    notifier.notify(saved.getSupplierId(), "CLARIFICATION_ANSWER",
        "询价澄清已回复",
        "您在询价中的问题已由采购方回复，请查看。",
        "INQUIRY", saved.getInquiryId());
    return clarification(saved);
  }

  private Supplier createPendingSupplier(RegisterRequest request, String creditCode) {
    Supplier supplier = new Supplier();
    supplier.setCode(codeGenerator.generate("SUPPLIER"));
    supplier.setName(request.companyName().trim());
    supplier.setUnifiedSocialCreditCode(creditCode);
    supplier.setContactName(request.contactName().trim());
    supplier.setPhone(request.phone().trim());
    supplier.setLicenseValidTo(request.licenseValidTo());
    supplier.setQualificationValidTo(request.qualificationValidTo());
    supplier.setAdmissionStatus("PENDING");
    supplier.setAdmissionSubmittedAt(OffsetDateTime.now());
    supplier.setRiskStatus(SupplierRiskStatus.NORMAL);
    return suppliers.save(supplier);
  }

  private SessionResponse session(SupplierPortalAccount account, Supplier supplier) {
    return session(account, supplier, null, null);
  }

  private SessionResponse session(
      SupplierPortalAccount account,
      Supplier supplier,
      OffsetDateTime lastLoginAt,
      String lastLoginIp
  ) {
    SupplierProfileResponse profile = !isBlank(account.getProfileDraftJson()) && !"ACTIVE".equals(account.getStatus())
        ? profile(supplier, readProfileDraft(account.getProfileDraftJson())) : profile(supplier);
    return new SessionResponse(jwtService.createSupplierPortalToken(account),
        account(account, supplier), profile, lastLoginAt, lastLoginIp);
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
        .map(line -> linkMap.get(line.requestId()).getRequestedQty().multiply(value(line.unitPrice())))
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
    List<UUID> partIds = requestMap.values().stream()
        .map(PurchaseRequest::getPartId).filter(Objects::nonNull).distinct().toList();
    Map<UUID, BigDecimal> historicalPriceByPart = orders.findByPartIdIn(partIds).stream()
        .filter(order -> order.getUnitPrice() != null)
        .collect(Collectors.groupingBy(PurchaseOrder::getPartId,
            Collectors.mapping(PurchaseOrder::getUnitPrice, Collectors.toList())))
        .entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry ->
            entry.getValue().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP)));
    List<Map<String, Object>> lineViews = links.stream().map(link -> {
      PurchaseRequest source = requestMap.get(link.getRequestId());
      Map<String, Object> line = new LinkedHashMap<>();
      line.put("requestId", link.getRequestId());
      line.put("requestCode", source == null ? null : source.getCode());
      line.put("partName", source == null ? null : source.getPartName());
      line.put("quantity", link.getRequestedQty());
      line.put("expectedDate", source == null ? null : source.getExpectedDate());
      line.put("historicalPrice", source == null || source.getPartId() == null
          ? null : historicalPriceByPart.get(source.getPartId()));
      return line;
    }).toList();
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", inquiry.getId()); view.put("code", inquiry.getCode());
    view.put("title", inquiry.getTitle()); view.put("deadline", inquiry.getDeadline());
    view.put("status", inquiry.getStatus()); view.put("invitationStatus", invitation.getStatus());
    view.put("invitedAt", invitation.getInvitedAt()); view.put("lines", lineViews);
    view.put("declineReason", invitation.getDeclineReason()); view.put("declinedAt", invitation.getDeclinedAt());
    SupplierQuotation quote = quotes.findByInquiryIdAndSupplierId(inquiry.getId(), supplierId).orElse(null);
    boolean awarded = "AWARDED".equals(inquiry.getStatus()) && quote != null
        && quote.getId().equals(inquiry.getSelectedQuoteId());
    view.put("awardStatus", !"AWARDED".equals(inquiry.getStatus()) ? "PENDING" : awarded ? "AWARDED" : "NOT_AWARDED");
    view.put("awardedAt", awarded ? inquiry.getSelectedAt() : null);
    view.put("quote", quote == null ? null : portalQuote(quote, quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId())));
    ProcurementContract contract = awarded
        ? contracts.findFirstByInquiryIdAndSupplierIdOrderByCreatedAtDesc(inquiry.getId(), supplierId).orElse(null)
        : null;
    view.put("contract", contract == null ? null : contractView(contract));
    view.put("attachments", quote == null ? List.of() : quoteAttachments.findByQuoteIdOrderByCreatedAtDesc(quote.getId())
        .stream().map(this::attachment).toList());
    view.put("clarifications", clarifications.findByInquiryIdAndSupplierIdOrderByAskedAtAsc(inquiry.getId(), supplierId)
        .stream().map(this::clarification).toList());
    return view;
  }

  private Map<String, Object> contractView(ProcurementContract contract) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", contract.getId());
    view.put("contractNo", contract.getContractNo());
    view.put("name", contract.getName());
    view.put("amount", contract.getAmount());
    view.put("currency", contract.getCurrency());
    view.put("status", contract.getStatus());
    view.put("approvalStatus", contract.getApprovalStatus());
    view.put("startDate", contract.getStartDate() == null ? "" : contract.getStartDate().toString());
    view.put("endDate", contract.getEndDate() == null ? "" : contract.getEndDate().toString());
    view.put("paymentTerms", contract.getPaymentTerms());
    view.put("sourceType", contract.getSourceType());
    view.put("remark", contract.getRemark());
    view.put("orderId", contract.getOrderId());
    view.put("acknowledged", contract.getAcknowledgedAt() != null);
    view.put("acknowledgedAt", contract.getAcknowledgedAt());
    view.put("acknowledgedByName", contract.getAcknowledgedByName());
    view.put("documents", contract.getOrderId() == null ? List.of()
        : orderDocuments.findByOrderIdOrderByCreatedAtDesc(contract.getOrderId()).stream()
            .map(this::orderDocumentView).toList());
    return view;
  }

  private Map<String, Object> orderDocumentView(ProcurementOrderDocument document) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", document.getId());
    view.put("fileName", document.getFileName());
    view.put("contentType", document.getContentType());
    view.put("sizeBytes", document.getSizeBytes());
    view.put("uploadedBy", document.getUploadedBy());
    view.put("uploadedAt", document.getUploadedAt());
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
    copy.setCategory(supplier.getCategory()); copy.setContactName(request.contactName()); copy.setPhone(request.phone());
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
    supplier.setName(request.name().trim());
    supplier.setContactName(request.contactName().trim()); supplier.setPhone(request.phone().trim());
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

  private PortalChangeRequestResponse changeView(SupplierChangeRequest item) {
    return new PortalChangeRequestResponse(item.getId(), item.getChangeType(),
        item.getProposedName(), item.getProposedCreditCode(), item.getProposedBankName(),
        item.getProposedBankAccount(), item.getProposedSettlementTerms(), item.getReason(),
        item.getStatus(), item.getRequestedByName(), item.getReviewedByName(),
        item.getReviewComment(), item.getReviewedAt(), item.getCreatedAt());
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
  private static boolean isNotBlank(String value) { return !isBlank(value); }
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
