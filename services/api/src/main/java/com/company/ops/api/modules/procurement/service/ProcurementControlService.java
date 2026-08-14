package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.dto.ProcurementControlDtos.*;
import com.company.ops.api.modules.procurement.dto.CreateConsolidatedInquiryRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementPurchasePoolResponse;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.InvoiceSubmissionResponse;
import com.company.ops.api.modules.procurement.repository.*;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcurementControlService {
  private static final java.util.Collection<String> ACTIVE_INQUIRY_STATUSES = List.of("OPEN", "AWARDED");

  private final ProcurementInquiryRepository inquiries;
  private final ProcurementInquiryRequestRepository inquiryRequests;
  private final SupplierQuotationRepository quotes;
  private final ProcurementInquiryInvitationRepository invitations;
  private final SupplierQuotationLineRepository quoteLines;
  private final PurchaseRequestRepository requests;
  private final SupplierRepository suppliers;
  private final PurchaseOrderRepository orders;
  private final GoodsReceiptRepository receipts;
  private final InventoryPartRepository parts;
  private final StockMovementRepository movements;
  private final ProcurementPayableRepository payables;
  private final ProcurementCostAllocationRepository costs;
  private final ProcurementReturnOrderRepository returns;
  private final SupplierInvoiceRepository invoices;
  private final SupplierInvoicePayableRepository invoicePayables;
  private final PayableAdjustmentRepository adjustments;
  private final SupplierInvoiceSubmissionRepository invoiceSubmissions;
  private final FileStorageService storage;
  private final PurchaseRequestApprovalRecordRepository requestApprovals;
  private final ProjectRepository projects;
  private final ProcurementArrivalService arrivals;
  private final LedgerService ledgerService;
  private final DataScopeService dataScopeService;
  private final SupplierPortalNotifier portalNotifier;

  public ProcurementControlService(
      ProcurementInquiryRepository inquiries,
      ProcurementInquiryRequestRepository inquiryRequests,
      SupplierQuotationRepository quotes,
      ProcurementInquiryInvitationRepository invitations,
      SupplierQuotationLineRepository quoteLines,
      PurchaseRequestRepository requests,
      SupplierRepository suppliers,
      PurchaseOrderRepository orders,
      GoodsReceiptRepository receipts,
      InventoryPartRepository parts,
      StockMovementRepository movements,
      ProcurementPayableRepository payables,
      ProcurementCostAllocationRepository costs,
      ProcurementReturnOrderRepository returns,
      SupplierInvoiceRepository invoices,
      SupplierInvoicePayableRepository invoicePayables,
      PayableAdjustmentRepository adjustments,
      SupplierInvoiceSubmissionRepository invoiceSubmissions,
      FileStorageService storage,
      PurchaseRequestApprovalRecordRepository requestApprovals,
      ProjectRepository projects,
      ProcurementArrivalService arrivals,
      LedgerService ledgerService,
      DataScopeService dataScopeService,
      SupplierPortalNotifier portalNotifier
  ) {
    this.inquiries = inquiries;
    this.inquiryRequests = inquiryRequests;
    this.quotes = quotes;
    this.invitations = invitations;
    this.quoteLines = quoteLines;
    this.requests = requests;
    this.suppliers = suppliers;
    this.orders = orders;
    this.receipts = receipts;
    this.parts = parts;
    this.movements = movements;
    this.payables = payables;
    this.costs = costs;
    this.returns = returns;
    this.invoices = invoices;
    this.invoicePayables = invoicePayables;
    this.adjustments = adjustments;
    this.invoiceSubmissions = invoiceSubmissions;
    this.storage = storage;
    this.requestApprovals = requestApprovals;
    this.projects = projects;
    this.arrivals = arrivals;
    this.ledgerService = ledgerService;
    this.dataScopeService = dataScopeService;
    this.portalNotifier = portalNotifier;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listInquiries() {
    return inquiries.findAllByOrderByCreatedAtDesc().stream().map(this::inquiryView).toList();
  }

  @Transactional(readOnly = true)
  public ProcurementPurchasePoolResponse purchasePool() {
    List<PurchaseRequest> approved = requests.findByApprovalStatusAndStatusOrderByCreatedAtDesc(
        ApprovalStatus.APPROVED, PurchaseRequestStatus.APPROVED);
    Set<UUID> activeInquiryRequestIds = activeInquiryRequestIds();
    Map<UUID, BigDecimal> orderedByRequest = orders.findByRequestIdNotNullAndStatusNot(
        PurchaseOrderStatus.CANCELLED).stream()
        .collect(Collectors.groupingBy(
            PurchaseOrder::getRequestId,
            Collectors.reducing(BigDecimal.ZERO, PurchaseOrder::getOrderedQty, this::add)));
    Map<UUID, InventoryPart> partMap = parts.findAllById(
        approved.stream().map(PurchaseRequest::getPartId)
            .filter(java.util.Objects::nonNull).distinct().toList()
    ).stream().collect(Collectors.toMap(InventoryPart::getId, item -> item));

    Map<String, List<ProcurementPurchasePoolResponse.Item>> grouped = new LinkedHashMap<>();
    Map<String, PurchaseRequest> groupSources = new HashMap<>();
    for (PurchaseRequest request : approved) {
      if (activeInquiryRequestIds.contains(request.getId())) continue;
      BigDecimal ordered = valueOr(orderedByRequest.get(request.getId()), BigDecimal.ZERO);
      BigDecimal remaining = valueOr(request.getQuantity(), BigDecimal.ZERO).subtract(ordered);
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;
      InventoryPart part = request.getPartId() == null ? null : partMap.get(request.getPartId());
      BigDecimal estimatedUnitPrice = valueOr(request.getUnitPrice(), BigDecimal.ZERO);
      if (estimatedUnitPrice.compareTo(BigDecimal.ZERO) == 0 && part != null) {
        estimatedUnitPrice = valueOr(part.getUnitCost(), BigDecimal.ZERO);
      }
      String groupKey = sourcingGroupKey(request);
      groupSources.putIfAbsent(groupKey, request);
      PurchaseRequestApprovalRecord approval = requestApprovals
          .findFirstByRequestIdOrderByDecidedAtDesc(request.getId()).orElse(null);
      grouped.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(
          new ProcurementPurchasePoolResponse.Item(
              request.getId(), request.getCode(), request.getBatchId(), request.getBatchCode(),
              request.getBatchName(), request.getLineNo(), request.getRequesterName(),
              request.getQuantity(), ordered, remaining, estimatedUnitPrice,
              remaining.multiply(estimatedUnitPrice), request.getTaxRate(),
              request.getExpectedDate(), request.getCostType(),
              request.getCostType() == ProcurementCostType.PROJECT
                  ? request.getProjectId() : request.getDepartmentId(),
              request.getCostTargetCode(), request.getCostTargetName(), request.getReason(),
              approval == null ? request.getUpdatedAt() : approval.getDecidedAt()));
    }

    List<ProcurementPurchasePoolResponse.Group> groups = grouped.entrySet().stream()
        .map(entry -> {
          PurchaseRequest source = groupSources.get(entry.getKey());
          InventoryPart part = source.getPartId() == null ? null : partMap.get(source.getPartId());
          List<ProcurementPurchasePoolResponse.Item> items = entry.getValue().stream()
              .sorted(Comparator.comparing(
                  ProcurementPurchasePoolResponse.Item::expectedDate,
                  Comparator.nullsLast(Comparator.naturalOrder())))
              .toList();
          BigDecimal totalQty = items.stream()
              .map(ProcurementPurchasePoolResponse.Item::remainingQuantity)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal totalAmount = items.stream()
              .map(ProcurementPurchasePoolResponse.Item::estimatedAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          int targetCount = (int) items.stream()
              .map(item -> item.costTargetId() == null
                  ? item.costTargetCode() : item.costTargetId().toString())
              .distinct().count();
          LocalDate earliest = items.stream()
              .map(ProcurementPurchasePoolResponse.Item::expectedDate)
              .filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(null);
          return new ProcurementPurchasePoolResponse.Group(
              entry.getKey(), source.getPartId(), part == null ? null : part.getCode(),
              source.getPartName(), items.size(), targetCount, totalQty, totalAmount,
              earliest, items);
        })
        .sorted(Comparator.comparing(
            ProcurementPurchasePoolResponse.Group::earliestExpectedDate,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
    return new ProcurementPurchasePoolResponse(
        groups.size(),
        groups.stream().mapToInt(ProcurementPurchasePoolResponse.Group::requestCount).sum(),
        groups.stream().map(ProcurementPurchasePoolResponse.Group::totalRemainingQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        groups.stream().map(ProcurementPurchasePoolResponse.Group::totalEstimatedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        groups);
  }

  @Transactional
  public Map<String, Object> createConsolidatedInquiry(
      CreateConsolidatedInquiryRequest request
  ) {
    List<UUID> requestIds = request.requestIds().stream().distinct().toList();
    List<PurchaseRequest> selected = requests.findAllById(requestIds);
    if (selected.size() != requestIds.size()) {
      throw new BusinessException("部分采购申请不存在，请刷新待采购清单");
    }
    if (selected.stream().anyMatch(item ->
        item.getApprovalStatus() != ApprovalStatus.APPROVED
            || item.getStatus() != PurchaseRequestStatus.APPROVED)) {
      throw new BusinessException("只能选择已审批且尚未完成下单的采购申请");
    }
    Set<UUID> activeRequestIds = activeInquiryRequestIds();
    if (selected.stream().anyMatch(item -> activeRequestIds.contains(item.getId()))) {
      throw new BusinessException("部分采购申请已进入询价，请刷新待采购清单");
    }
    Map<UUID, BigDecimal> orderedByRequest = orders.findByRequestIdNotNullAndStatusNot(
        PurchaseOrderStatus.CANCELLED).stream()
        .collect(Collectors.groupingBy(
            PurchaseOrder::getRequestId,
            Collectors.reducing(BigDecimal.ZERO, PurchaseOrder::getOrderedQty, this::add)));
    Map<UUID, BigDecimal> remainingByRequest = new LinkedHashMap<>();
    for (PurchaseRequest item : selected) {
      BigDecimal remaining = valueOr(item.getQuantity(), BigDecimal.ZERO)
          .subtract(valueOr(orderedByRequest.get(item.getId()), BigDecimal.ZERO));
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException(item.getCode() + " 已无待采购数量");
      }
      remainingByRequest.put(item.getId(), remaining);
    }

    String method = defaultText(request.sourcingMethod(), "COMPETITIVE").toUpperCase();
    int minQuotes = request.minQuoteCount() == null
        ? ("SINGLE_SOURCE".equals(method) ? 1 : 3) : request.minQuoteCount();
    validateSourcingRules(method, minQuotes, request.exceptionReason());
    PurchaseRequest primary = selected.get(0);
    ProcurementInquiry inquiry = new ProcurementInquiry();
    inquiry.setCode("XJ-JC-" + LocalDate.now().toString().replace("-", "")
        + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    inquiry.setRequestId(primary.getId());
    inquiry.setTitle(request.title().trim());
    inquiry.setDeadline(request.deadline());
    inquiry.setCreatedByName(currentName());
    inquiry.setStatus("OPEN");
    inquiry.setSourcingMethod(method);
    inquiry.setMinQuoteCount(minQuotes);
    inquiry.setExceptionReason(request.exceptionReason());
    ProcurementInquiry saved = inquiries.save(inquiry);

    List<ProcurementInquiryRequest> links = selected.stream().map(item -> {
      ProcurementInquiryRequest link = new ProcurementInquiryRequest();
      link.setInquiryId(saved.getId());
      link.setRequestId(item.getId());
      link.setRequestedQty(remainingByRequest.get(item.getId()));
      return link;
    }).toList();
    inquiryRequests.saveAll(links);
    return inquiryView(saved);
  }

  @Transactional
  public Map<String, Object> createInquiry(CreateInquiry request) {
    PurchaseRequest purchaseRequest = requests.findById(request.requestId())
        .orElseThrow(() -> new BusinessException("采购申请不存在"));
    if (purchaseRequest.getApprovalStatus() != ApprovalStatus.APPROVED) {
      throw new BusinessException("采购申请审批通过后才能询价");
    }
    if (activeInquiryRequestIds().contains(purchaseRequest.getId())) {
      throw new BusinessException("该采购申请已进入询价");
    }
    String method = defaultText(request.sourcingMethod(), "COMPETITIVE").toUpperCase();
    int minQuotes = request.minQuoteCount() == null ? ("SINGLE_SOURCE".equals(method) ? 1 : 3)
        : request.minQuoteCount();
    validateSourcingRules(method, minQuotes, request.exceptionReason());
    ProcurementInquiry inquiry = new ProcurementInquiry();
    inquiry.setCode("XJ-" + System.currentTimeMillis());
    inquiry.setRequestId(purchaseRequest.getId());
    inquiry.setTitle(request.title());
    inquiry.setDeadline(request.deadline());
    inquiry.setCreatedByName(currentName());
    inquiry.setStatus("OPEN");
    inquiry.setSourcingMethod(method);
    inquiry.setMinQuoteCount(minQuotes);
    inquiry.setExceptionReason(request.exceptionReason());
    ProcurementInquiry saved = inquiries.save(inquiry);
    ProcurementInquiryRequest link = new ProcurementInquiryRequest();
    link.setInquiryId(saved.getId());
    link.setRequestId(purchaseRequest.getId());
    link.setRequestedQty(purchaseRequest.getQuantity());
    inquiryRequests.save(link);
    return inquiryView(saved);
  }

  @Transactional
  public Map<String, Object> addQuote(UUID inquiryId, CreateSupplierQuote request) {
    ProcurementInquiry inquiry = inquiries.findById(inquiryId)
        .orElseThrow(() -> new BusinessException("询价单不存在"));
    if (!"OPEN".equals(inquiry.getStatus())) {
      throw new BusinessException("询价已结束");
    }
    if (inquiry.getDeadline() != null && LocalDate.now().isAfter(inquiry.getDeadline())) {
      throw new BusinessException("询价已超过截止日期");
    }
    Supplier supplier = suppliers.findById(request.supplierId())
        .orElseThrow(() -> new BusinessException("供应商不存在"));
    requireEligibleSupplier(supplier);
    boolean duplicated = quotes.findByInquiryIdOrderByUnitPriceAsc(inquiryId).stream()
        .anyMatch(item -> item.getSupplierId().equals(request.supplierId()));
    if (duplicated) {
      throw new BusinessException("同一供应商只能提交一份报价");
    }
    List<ProcurementInquiryRequest> inquiryLinks =
        inquiryRequests.findByInquiryIdOrderByCreatedAtAsc(inquiryId);
    if (inquiryLinks.isEmpty()) {
      ProcurementInquiryRequest fallback = new ProcurementInquiryRequest();
      fallback.setInquiryId(inquiryId);
      fallback.setRequestId(inquiry.getRequestId());
      PurchaseRequest source = requests.findById(inquiry.getRequestId()).orElse(null);
      fallback.setRequestedQty(source == null ? BigDecimal.ZERO : source.getQuantity());
      inquiryLinks = List.of(fallback);
    }
    Map<UUID, ProcurementInquiryRequest> linkMap = inquiryLinks.stream()
        .collect(Collectors.toMap(ProcurementInquiryRequest::getRequestId, item -> item));
    List<CreateSupplierQuoteLine> submittedLines = request.lines() == null
        ? new ArrayList<>() : new ArrayList<>(request.lines());
    if (submittedLines.isEmpty()) {
      if (request.unitPrice() == null || request.taxRate() == null) {
        throw new BusinessException("请填写每项物料的报价");
      }
      for (ProcurementInquiryRequest link : inquiryLinks) {
        submittedLines.add(new CreateSupplierQuoteLine(
            link.getRequestId(), request.unitPrice(), request.taxRate(),
            request.deliveryDate(), request.remark()));
      }
    }
    Set<UUID> submittedRequestIds = submittedLines.stream()
        .map(CreateSupplierQuoteLine::requestId).collect(Collectors.toSet());
    if (submittedRequestIds.size() != submittedLines.size()
        || !submittedRequestIds.equals(linkMap.keySet())) {
      throw new BusinessException("报价分项必须完整覆盖询价包中的全部采购申请");
    }
    BigDecimal totalQuantity = inquiryLinks.stream()
        .map(ProcurementInquiryRequest::getRequestedQty)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal materialAmount = submittedLines.stream()
        .map(line -> linkMap.get(line.requestId()).getRequestedQty().multiply(line.unitPrice()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal averageUnitPrice = totalQuantity.compareTo(BigDecimal.ZERO) == 0
        ? BigDecimal.ZERO
        : materialAmount.divide(totalQuantity, 2, RoundingMode.HALF_UP);
    LocalDate latestDelivery = submittedLines.stream()
        .map(CreateSupplierQuoteLine::deliveryDate)
        .filter(java.util.Objects::nonNull)
        .max(LocalDate::compareTo)
        .orElse(request.deliveryDate());
    BigDecimal technical = BigDecimal.ZERO;
    BigDecimal commercial = BigDecimal.ZERO;
    SupplierQuotation quote = new SupplierQuotation();
    quote.setInquiryId(inquiryId);
    quote.setSupplierId(request.supplierId());
    quote.setUnitPrice(averageUnitPrice);
    quote.setTaxRate(submittedLines.get(0).taxRate());
    quote.setDeliveryDate(latestDelivery);
    quote.setPaymentTerms(request.paymentTerms());
    quote.setRemark(request.remark());
    quote.setCurrency(defaultText(request.currency(), "CNY"));
    quote.setFreightAmount(valueOr(request.freightAmount(), BigDecimal.ZERO));
    quote.setOtherCostAmount(valueOr(request.otherCostAmount(), BigDecimal.ZERO));
    quote.setTechnicalScore(technical);
    quote.setCommercialScore(commercial);
    quote.setTotalScore(technical.multiply(BigDecimal.valueOf(.4))
        .add(commercial.multiply(BigDecimal.valueOf(.6))).setScale(2, RoundingMode.HALF_UP));
    quote.setValidUntil(request.validUntil());
    quote.setSubmissionSource("INTERNAL_ENTRY");
    quote.setSubmissionStatus("SUBMITTED");
    quote.setVersionNo(1);
    quote.setSubmittedByType("INTERNAL_USER");
    quote.setSubmittedById(currentUserId());
    quote.setSubmittedByName(currentName());
    quote.setSubmittedAt(OffsetDateTime.now());
    SupplierQuotation saved = quotes.save(quote);
    List<SupplierQuotationLine> persistedLines = submittedLines.stream().map(line -> {
      SupplierQuotationLine entity = new SupplierQuotationLine();
      entity.setQuoteId(saved.getId());
      entity.setRequestId(line.requestId());
      entity.setQuantity(linkMap.get(line.requestId()).getRequestedQty());
      entity.setUnitPrice(line.unitPrice());
      entity.setTaxRate(line.taxRate());
      entity.setDeliveryDate(line.deliveryDate());
      entity.setRemark(line.remark());
      return entity;
    }).toList();
    quoteLines.saveAll(persistedLines);
    return quoteView(saved);
  }

  @Transactional
  public Map<String, Object> inviteSuppliers(UUID inquiryId, InviteSuppliers request) {
    ProcurementInquiry inquiry = inquiries.findById(inquiryId)
        .orElseThrow(() -> new BusinessException("询价单不存在"));
    if (!"OPEN".equals(inquiry.getStatus())) {
      throw new BusinessException("只有进行中的询价可以邀请供应商");
    }
    if (inquiry.getDeadline() != null && LocalDate.now().isAfter(inquiry.getDeadline())) {
      throw new BusinessException("询价已超过截止日期");
    }
    OffsetDateTime now = OffsetDateTime.now();
    Map<UUID, String> registrationCodes = new LinkedHashMap<>();
    Map<UUID, String> contactEmails = request.contactEmails() == null
        ? Map.of() : request.contactEmails();
    for (UUID supplierId : request.supplierIds().stream().distinct().toList()) {
      Supplier supplier = suppliers.findById(supplierId)
          .orElseThrow(() -> new BusinessException("供应商不存在"));
      requireEligibleSupplier(supplier);
      if (invitations.findByInquiryIdAndSupplierId(inquiryId, supplierId).isEmpty()) {
        String contactEmail = contactEmails.get(supplierId);
        if (contactEmail != null && !contactEmail.isBlank()
            && !contactEmail.contains("@")) {
          throw new BusinessException("供应商 " + supplier.getName() + " 的通知邮箱格式不正确");
        }
        ProcurementInquiryInvitation invitation = new ProcurementInquiryInvitation();
        invitation.setInquiryId(inquiryId);
        invitation.setSupplierId(supplierId);
        invitation.setStatus("INVITED");
        invitation.setInvitedByName(currentName());
        invitation.setInvitedAt(now);
        String registrationCode = "REG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        invitation.setRegistrationCodeHash(sha256Text(registrationCode));
        invitation.setRegistrationCodeExpiresAt(now.plusDays(7));
        invitation.setDeliveryStatus("PENDING");
        invitations.save(invitation);
        portalNotifier.notify(supplierId, "INQUIRY_INVITATION",
            "收到新的询价邀请",
            "询价单 " + inquiry.getCode() + " 邀请贵司参与报价，请在截止前响应。",
            "INQUIRY", inquiryId);
        registrationCodes.put(supplierId, registrationCode);
        if (contactEmail != null && !contactEmail.isBlank()) {
          Boolean delivered = portalNotifier.deliverInvitation(
              supplierId, contactEmail.trim(), registrationCode,
              inquiry.getCode(),
              inquiry.getDeadline() == null ? null : inquiry.getDeadline().toString());
          if (delivered == null) {
            invitation.setDeliveryStatus("PENDING");
          } else if (delivered) {
            invitation.setDeliveryStatus("DELIVERED");
            invitation.setDeliveryAttemptCount(invitation.getDeliveryAttemptCount() + 1);
            invitation.setLastDeliveryAt(OffsetDateTime.now());
          } else {
            invitation.setDeliveryStatus("FAILED");
            invitation.setDeliveryAttemptCount(invitation.getDeliveryAttemptCount() + 1);
            invitation.setLastDeliveryAt(OffsetDateTime.now());
            invitation.setDeliveryError("SMTP 发送失败或未启用");
          }
          invitations.save(invitation);
        }
      }
    }
    Map<String, Object> result = inquiryView(inquiry);
    result.put("registrationCodes", registrationCodes);
    return result;
  }

  @Transactional
  public Map<String, Object> updateInquiryDeadline(
      UUID inquiryId,
      UpdateInquiryDeadline request
  ) {
    ProcurementInquiry inquiry = inquiries.findById(inquiryId)
        .orElseThrow(() -> new BusinessException("询价单不存在"));
    if (!"OPEN".equals(inquiry.getStatus())) {
      throw new BusinessException("只有进行中的询价可以调整截止日期");
    }
    if (request.deadline().isBefore(LocalDate.now())) {
      throw new BusinessException("截止日期不能早于今天");
    }
    inquiry.setDeadline(request.deadline());
    return inquiryView(inquiries.save(inquiry));
  }

  @Transactional
  public Map<String, Object> updateInquiryMinQuotes(
      UUID inquiryId,
      UpdateInquiryMinQuotes request
  ) {
    ProcurementInquiry inquiry = inquiries.findById(inquiryId)
        .orElseThrow(() -> new BusinessException("询价单不存在"));
    if (!"OPEN".equals(inquiry.getStatus())) {
      throw new BusinessException("只有进行中的询价可以调整最低报价数");
    }
    inquiry.setMinQuoteCount(request.minQuoteCount());
    return inquiryView(inquiries.save(inquiry));
  }

  @Transactional
  public Map<String, Object> scoreQuote(
      UUID inquiryId,
      UUID quoteId,
      ScoreSupplierQuote request
  ) {
    SupplierQuotation quote = quotes.findById(quoteId)
        .orElseThrow(() -> new BusinessException("供应商报价不存在"));
    if (!quote.getInquiryId().equals(inquiryId)) {
      throw new BusinessException("报价不属于该询价单");
    }
    if (!"SUBMITTED".equals(quote.getSubmissionStatus())) {
      throw new BusinessException("只有已提交的报价可以评分");
    }
    quote.setTechnicalScore(request.technicalScore());
    quote.setCommercialScore(request.commercialScore());
    quote.setTotalScore(request.technicalScore().multiply(BigDecimal.valueOf(.4))
        .add(request.commercialScore().multiply(BigDecimal.valueOf(.6)))
        .setScale(2, RoundingMode.HALF_UP));
    return quoteView(quotes.save(quote));
  }

  @Transactional
  public Map<String, Object> selectQuote(UUID inquiryId, UUID quoteId, SelectSupplierQuote request) {
    ProcurementInquiry inquiry = inquiries.findById(inquiryId)
        .orElseThrow(() -> new BusinessException("询价单不存在"));
    if (!"OPEN".equals(inquiry.getStatus())) {
      throw new BusinessException("只有进行中的询价可以定标");
    }
    List<SupplierQuotation> allQuotes = quotes.findByInquiryIdOrderByUnitPriceAsc(inquiryId).stream()
        .filter(item -> "SUBMITTED".equals(item.getSubmissionStatus()))
        .toList();
    if (allQuotes.size() < inquiry.getMinQuoteCount()
        && !"SINGLE_SOURCE".equals(inquiry.getSourcingMethod())) {
      throw new BusinessException("有效报价不足 " + inquiry.getMinQuoteCount() + " 家，不能定标");
    }
    SupplierQuotation selected = quotes.findById(quoteId)
        .orElseThrow(() -> new BusinessException("供应商报价不存在"));
    if (!selected.getInquiryId().equals(inquiryId)) {
      throw new BusinessException("报价不属于该询价单");
    }
    if (!"SUBMITTED".equals(selected.getSubmissionStatus())) {
      throw new BusinessException("只有已提交的报价可以定标");
    }
    Supplier supplier = suppliers.findById(selected.getSupplierId())
        .orElseThrow(() -> new BusinessException("供应商不存在"));
    requireEligibleSupplier(supplier);
    if (selected.getValidUntil() != null && LocalDate.now().isAfter(selected.getValidUntil())) {
      throw new BusinessException("所选报价已过有效期");
    }
    allQuotes.forEach(item -> {
      item.setSelected(item.getId().equals(quoteId));
      quotes.save(item);
    });
    inquiry.setSelectedQuoteId(quoteId);
    inquiry.setSelectionReason(request.reason());
    inquiry.setSelectedByName(currentName());
    inquiry.setSelectedAt(OffsetDateTime.now());
    inquiry.setStatus("AWARDED");
    Map<String, Object> result = inquiryView(inquiries.save(inquiry));
    portalNotifier.notify(selected.getSupplierId(), "AWARD",
        "恭喜，贵司已中标",
        "询价单 " + inquiry.getCode() + " 已完成定标，采购方将据此下单签约。",
        "INQUIRY", inquiryId);
    allQuotes.stream().filter(item -> !item.getId().equals(quoteId)).forEach(item ->
        portalNotifier.notify(item.getSupplierId(), "NOT_AWARDED",
            "询价未中标通知",
            "询价单 " + inquiry.getCode() + " 已完成定标，感谢贵司参与报价。",
            "INQUIRY", inquiryId));
    return result;
  }

  @Transactional
  public GoodsReceipt registerArrival(UUID orderId, ReceivePurchaseOrderRequest request) {
    return arrivals.register(orderId, request);
  }

  @Transactional
  public Map<String, Object> inspect(UUID receiptId, InspectReceipt request) {
    GoodsReceipt receipt = receipts.findById(receiptId)
        .orElseThrow(() -> new BusinessException("到货记录不存在"));
    if (!"PENDING".equals(receipt.getInspectionStatus())) {
      throw new BusinessException("该到货记录已质检");
    }
    if (request.qualifiedQty().add(request.rejectedQty()).compareTo(receipt.getQuantity()) != 0) {
      throw new BusinessException("合格与不合格数量合计必须等于到货数量");
    }
    PurchaseOrder order = orders.findByIdForUpdate(receipt.getOrderId())
        .orElseThrow(() -> new BusinessException("订单不存在"));
    if (order.getApprovalStatus() != ApprovalStatus.APPROVED
        || (order.getStatus() != PurchaseOrderStatus.ORDERED
        && order.getStatus() != PurchaseOrderStatus.PARTIAL_RECEIVED)) {
      throw new BusinessException("采购订单已取消或关闭，不能继续质检入库");
    }
    String operator = currentName();
    receipt.setQualifiedQty(request.qualifiedQty());
    receipt.setRejectedQty(request.rejectedQty());
    receipt.setInspectorName(operator);
    receipt.setInspectionComment(request.comment());
    receipt.setInspectedAt(OffsetDateTime.now());
    receipt.setInspectionStatus(request.rejectedQty().signum() == 0 ? "PASSED"
        : request.qualifiedQty().signum() == 0 ? "REJECTED" : "PARTIAL");
    LocalDate resolvedDueDate = request.payableDueDate();
    if (resolvedDueDate == null) {
      Supplier supplier = suppliers.findById(order.getSupplierId()).orElse(null);
      int terms = supplier == null || supplier.getPaymentTermsDays() <= 0
          ? 30 : supplier.getPaymentTermsDays();
      resolvedDueDate = receipt.getReceivedDate().plusDays(terms);
    }
    receipt.setPayableDueDate(resolvedDueDate);
    receipts.save(receipt);

    if (request.qualifiedQty().signum() > 0) {
      postQualifiedReceipt(order, receipt, request);
    }
    ProcurementReturnOrder returnOrder = null;
    if (request.rejectedQty().signum() > 0) {
      returnOrder = new ProcurementReturnOrder();
      returnOrder.setCode("TH-" + receipt.getCode());
      returnOrder.setOrderId(order.getId());
      returnOrder.setReceiptId(receipt.getId());
      returnOrder.setSupplierId(order.getSupplierId());
      returnOrder.setQuantity(request.rejectedQty());
      returnOrder.setAmount(request.rejectedQty().multiply(order.getUnitPrice()));
      returnOrder.setReason(isBlank(request.comment()) ? "质检不合格" : request.comment());
      returnOrder.setReturnDate(receipt.getReceivedDate());
      returnOrder.setHandlerName(operator);
      returnOrder.setStatus("OPEN");
      returns.save(returnOrder);
    }
    String inspectionLabel = "PASSED".equals(receipt.getInspectionStatus()) ? "合格"
        : "REJECTED".equals(receipt.getInspectionStatus()) ? "不合格" : "部分合格";
    portalNotifier.notify(order.getSupplierId(), "INSPECTION",
        "质检结果：" + inspectionLabel,
        "到货单 " + receipt.getCode() + " 质检" + inspectionLabel
            + "（合格 " + plainQty(receipt.getQualifiedQty())
            + " / 不合格 " + plainQty(receipt.getRejectedQty()) + "），可在门户查看并发起申诉。",
        "ORDER", order.getId());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("receipt", receipt);
    result.put("order", order);
    result.put("returnOrder", returnOrder);
    return result;
  }

  @Transactional(readOnly = true)
  public List<ProcurementReturnOrder> listReturns() {
    return returns.findAllByOrderByReturnDateDesc();
  }

  @Transactional
  public ProcurementReturnOrder resolveReturn(UUID returnId, ResolveReturn request) {
    ProcurementReturnOrder item = returns.findById(returnId)
        .orElseThrow(() -> new BusinessException("退换货记录不存在"));
    if ("COMPLETED".equals(item.getStatus())) {
      throw new BusinessException("该退换货记录已结案");
    }
    PurchaseOrder order = orders.findByIdForUpdate(item.getOrderId())
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    GoodsReceipt originalReceipt = receipts.findById(item.getReceiptId())
        .orElseThrow(() -> new BusinessException("原到货记录不存在"));
    BigDecimal replacementQty = valueOr(request.replacementQty(), BigDecimal.ZERO);
    BigDecimal creditAmount = valueOr(request.creditAmount(), BigDecimal.ZERO);
    BigDecimal claimAmount = valueOr(request.claimAmount(), BigDecimal.ZERO);
    if (replacementQty.signum() == 0 && creditAmount.signum() == 0 && claimAmount.signum() == 0) {
      throw new BusinessException("请至少登记换货、折让或索赔中的一项处理结果");
    }
    if (replacementQty.compareTo(item.getQuantity()) > 0) {
      throw new BusinessException("换货数量不能超过不合格数量");
    }
    BigDecimal unresolvedAmount = item.getQuantity().subtract(replacementQty)
        .multiply(order.getUnitPrice());
    if (creditAmount.add(claimAmount).compareTo(unresolvedAmount) > 0) {
      throw new BusinessException("折让与索赔金额合计不能超过未换货部分金额");
    }
    item.setReplacementQty(replacementQty);
    item.setCreditAmount(creditAmount);
    item.setClaimAmount(claimAmount);
    item.setCorrectiveAction(request.correctiveAction());
    item.setSupplierResponse(request.supplierResponse());
    item.setHandlerName(currentName());
    item.setStatus("COMPLETED");
    item.setCompletedAt(OffsetDateTime.now());
    ProcurementReturnOrder saved = returns.save(item);
    if (replacementQty.signum() > 0) {
      GoodsReceipt replacement = new GoodsReceipt();
      replacement.setCode("DH-" + order.getCode() + "-R"
          + String.format("%02d", receipts.countByOrderId(order.getId()) + 1));
      replacement.setOrderId(order.getId());
      replacement.setPartId(originalReceipt.getPartId());
      replacement.setQuantity(replacementQty);
      replacement.setUnitPrice(originalReceipt.getUnitPrice());
      replacement.setTaxRate(originalReceipt.getTaxRate());
      replacement.setAmount(replacementQty.multiply(originalReceipt.getUnitPrice()));
      replacement.setReceivedDate(LocalDate.now());
      replacement.setDeliveryNo("换货-" + item.getCode());
      replacement.setReceiverName(currentName());
      replacement.setPayableDueDate(originalReceipt.getPayableDueDate());
      replacement.setInspectionStatus("PENDING");
      replacement.setClientRequestId("RETURN:" + item.getId());
      receipts.save(replacement);
    }
    if (creditAmount.signum() > 0) {
      applyReturnAdjustment(order, saved, PayableAdjustmentType.CREDIT, creditAmount);
    }
    if (claimAmount.signum() > 0) {
      applyReturnAdjustment(order, saved, PayableAdjustmentType.CLAIM, claimAmount);
    }
    if (creditAmount.signum() > 0 || claimAmount.signum() > 0) {
      portalNotifier.notify(order.getSupplierId(), "PAYABLE",
          "退货折让/索赔已冲减应付",
          "退换货单 " + saved.getCode() + " 已冲减应付 "
              + creditAmount.add(claimAmount).stripTrailingZeros().toPlainString()
              + " 元，可在门户对账页查看。",
          "ORDER", order.getId());
    }
    return saved;
  }

  private BigDecimal applyReturnAdjustment(
      PurchaseOrder order,
      ProcurementReturnOrder returnOrder,
      PayableAdjustmentType type,
      BigDecimal creditAmount
  ) {
    BigDecimal remaining = valueOr(creditAmount, BigDecimal.ZERO);
    if (remaining.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    List<ProcurementPayable> openPayables = payables
        .findByOrderIdAndStatusNotInOrderByDueDateAsc(
            order.getId(), List.of(PayableStatus.PAID, PayableStatus.CANCELLED));
    BigDecimal appliedTotal = BigDecimal.ZERO;
    for (ProcurementPayable payable : openPayables) {
      if (remaining.signum() <= 0) {
        break;
      }
      BigDecimal effective = valueOr(payable.getAmount(), BigDecimal.ZERO)
          .subtract(valueOr(payable.getAdjustedAmount(), BigDecimal.ZERO));
      BigDecimal outstanding = effective.subtract(valueOr(payable.getPaidAmount(), BigDecimal.ZERO));
      if (outstanding.signum() <= 0) {
        continue;
      }
      BigDecimal applied = remaining.min(outstanding);
      remaining = remaining.subtract(applied);
      appliedTotal = appliedTotal.add(applied);
      payable.setAdjustedAmount(valueOr(payable.getAdjustedAmount(), BigDecimal.ZERO).add(applied));
      BigDecimal newEffective = valueOr(payable.getAmount(), BigDecimal.ZERO)
          .subtract(valueOr(payable.getAdjustedAmount(), BigDecimal.ZERO));
      if (newEffective.signum() == 0 && valueOr(payable.getPaidAmount(), BigDecimal.ZERO).signum() == 0) {
        payable.setStatus(PayableStatus.CANCELLED);
        payable.setCancelReason("退货折让/索赔冲减清零");
        payable.setCancelledBy(currentName());
        payable.setCancelledAt(LocalDate.now());
      }
      payables.save(payable);

      PayableAdjustment adjustment = new PayableAdjustment();
      adjustment.setCode(returnAdjustmentCode());
      adjustment.setPayableId(payable.getId());
      adjustment.setOrderId(order.getId());
      adjustment.setSupplierId(order.getSupplierId());
      adjustment.setAdjustmentType(type);
      adjustment.setAmount(applied);
      adjustment.setReason("退换货单 " + returnOrder.getCode()
          + (type == PayableAdjustmentType.CLAIM ? " 索赔" : " 折让") + "冲减");
      adjustment.setOperatorName(currentName());
      adjustment.setAppliedAt(LocalDate.now());
      adjustment.setSource("RETURN");
      adjustment.setSourceId(returnOrder.getId());
      adjustments.save(adjustment);
      if (type == PayableAdjustmentType.CLAIM) {
        ledgerService.post("PAYABLE_ADJUSTMENT", adjustment.getCode(), adjustment.getAppliedAt(),
            "供应商索赔冲减应付 " + adjustment.getCode(), List.of(
                new PostingLine("2202", "应付账款", applied, BigDecimal.ZERO, payable.getCode()),
                new PostingLine("6111", "其他业务收入", BigDecimal.ZERO, applied, adjustment.getCode())));
      } else {
        ledgerService.post("PAYABLE_ADJUSTMENT", adjustment.getCode(), adjustment.getAppliedAt(),
            "退货折让冲减应付 " + adjustment.getCode(), List.of(
                new PostingLine("2202", "应付账款", applied, BigDecimal.ZERO, payable.getCode()),
                new PostingLine("1405", "库存商品", BigDecimal.ZERO, applied, adjustment.getCode())));
      }
    }
    return appliedTotal;
  }

  private String returnAdjustmentCode() {
    String code = "YFTZ-" + System.currentTimeMillis()
        + "-" + UUID.randomUUID().toString().substring(0, 6);
    while (adjustments.existsByCode(code)) {
      code = "YFTZ-" + System.currentTimeMillis()
          + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
    return code;
  }

  @Transactional
  public SupplierInvoice createInvoice(CreateInvoice request) {
    if (!isBlank(request.clientRequestId())) {
      Optional<SupplierInvoice> existing = invoices.findByClientRequestId(request.clientRequestId());
      if (existing.isPresent()) {
        return existing.get();
      }
    }
    if (invoices.existsByInvoiceNo(request.invoiceNo())) {
      throw new BusinessException("供应商发票号码已存在");
    }
    PurchaseOrder order = orders.findById(request.orderId())
        .orElseThrow(() -> new BusinessException("订单不存在"));
    List<ProcurementPayable> selectedPayables = resolvePayables(order, request);
    Set<UUID> payableIds = selectedPayables.stream()
        .map(ProcurementPayable::getId).collect(Collectors.toSet());
    BigDecimal eligible = payableIds.isEmpty()
        ? payables.findByOrderId(order.getId()).stream()
            .filter(item -> item.getStatus() != PayableStatus.CANCELLED)
            .map(item -> valueOr(item.getAmount(), BigDecimal.ZERO)
                .subtract(valueOr(item.getAdjustedAmount(), BigDecimal.ZERO)))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
        : selectedPayables.stream()
            .map(item -> valueOr(item.getAmount(), BigDecimal.ZERO)
                .subtract(valueOr(item.getAdjustedAmount(), BigDecimal.ZERO)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    List<SupplierInvoice> previousInvoices = new ArrayList<>();
    if (payableIds.isEmpty()) {
      previousInvoices.addAll(invoices.findByOrderId(order.getId()));
    } else {
      previousInvoices.addAll(invoices.findByPayableIdIn(payableIds));
      Set<UUID> linkedInvoiceIds = invoicePayables.findByPayableIdIn(payableIds).stream()
          .map(SupplierInvoicePayable::getInvoiceId).collect(Collectors.toSet());
      if (!linkedInvoiceIds.isEmpty()) {
        previousInvoices.addAll(invoices.findAllById(linkedInvoiceIds));
      }
    }
    BigDecimal previous = previousInvoices.stream()
        .filter(item -> !"REJECTED".equals(item.getApprovalStatus()))
        .map(SupplierInvoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal remaining = eligible.subtract(previous).max(BigDecimal.ZERO);
    BigDecimal matched = request.amount().min(remaining);
    BigDecimal difference = request.amount().subtract(matched);
    boolean matchedWithinTolerance = difference.abs().compareTo(matchTolerance(request.amount())) <= 0;

    SupplierInvoice invoice = new SupplierInvoice();
    invoice.setCode("CGFP-" + System.currentTimeMillis());
    invoice.setInvoiceNo(request.invoiceNo());
    invoice.setOrderId(order.getId());
    invoice.setSupplierId(order.getSupplierId());
    ProcurementPayable firstPayable = selectedPayables.stream().findFirst().orElse(null);
    invoice.setPayableId(firstPayable == null ? null : firstPayable.getId());
    invoice.setReceiptId(request.receiptId() != null ? request.receiptId()
        : firstPayable == null ? null : firstPayable.getReceiptId());
    invoice.setAmount(request.amount());
    invoice.setMatchedAmount(matched);
    invoice.setTaxRate(request.taxRate());
    invoice.setInvoiceDate(request.invoiceDate());
    invoice.setDifferenceAmount(difference);
    invoice.setMatchStatus(matchedWithinTolerance ? "MATCHED" : "MISMATCH");
    invoice.setApprovalStatus("PENDING");
    invoice.setHandlerName(currentName());
    invoice.setVerificationStatus("MATCHED".equals(invoice.getMatchStatus()) ? "VERIFIED" : "EXCEPTION");
    invoice.setClientRequestId(request.clientRequestId());
    invoice.setAttachmentDocumentId(request.attachmentDocumentId());
    invoice.setRemark(request.remark());
    SupplierInvoice saved = invoices.save(invoice);
    for (UUID payableId : payableIds) {
      SupplierInvoicePayable link = new SupplierInvoicePayable();
      link.setInvoiceId(saved.getId());
      link.setPayableId(payableId);
      invoicePayables.save(link);
    }
    portalNotifier.notify(order.getSupplierId(), "INVOICE",
        "采购方已登记发票",
        "订单 " + order.getCode() + " 已登记发票 " + saved.getInvoiceNo()
            + "（金额 " + saved.getAmount().stripTrailingZeros().toPlainString() + "），可在门户开票与对账页查看。",
        "ORDER", order.getId());
    return saved;
  }

  @Transactional
  public SupplierInvoice reviewInvoice(UUID invoiceId, ReviewInvoice request) {
    SupplierInvoice invoice = invoices.findById(invoiceId)
        .orElseThrow(() -> new BusinessException("供应商发票不存在"));
    if (!"PENDING".equals(invoice.getApprovalStatus())) {
      throw new BusinessException("该发票已审核");
    }
    String decision = request.decision().toUpperCase();
    if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
      throw new BusinessException("审核结果只能为 APPROVED 或 REJECTED");
    }
    boolean exceptionApproval = "APPROVED".equals(decision)
        && !"MATCHED".equals(invoice.getMatchStatus())
        && valueOr(invoice.getDifferenceAmount(), BigDecimal.ZERO).abs()
            .compareTo(matchTolerance(invoice.getAmount())) <= 0;
    if ("APPROVED".equals(decision) && !"MATCHED".equals(invoice.getMatchStatus())
        && !exceptionApproval) {
      throw new BusinessException("三单匹配异常的发票不能审核通过，超出容差的需先处理差异");
    }
    if (exceptionApproval && isBlank(request.comment())) {
      throw new BusinessException("容差内异常审核必须填写审核意见");
    }
    invoice.setApprovalStatus(decision);
    invoice.setApprovedByName(currentName());
    invoice.setApprovedAt(OffsetDateTime.now());
    if (!isBlank(request.comment())) {
      invoice.setRemark(defaultText(invoice.getRemark(), "") + " 审核：" + request.comment());
    }
    SupplierInvoice saved = invoices.save(invoice);
    if ("APPROVED".equals(decision)) {
      BigDecimal netAmount = netAmount(saved.getAmount(), saved.getTaxRate());
      BigDecimal taxAmount = saved.getAmount().subtract(netAmount);
      ledgerService.post("SUPPLIER_INVOICE", saved.getCode(), saved.getInvoiceDate(),
          "供应商发票 " + saved.getInvoiceNo(), List.of(
              new PostingLine("1405", "库存商品", netAmount, BigDecimal.ZERO, saved.getInvoiceNo()),
              new PostingLine("22210101", "应交增值税-进项税额", taxAmount, BigDecimal.ZERO, saved.getInvoiceNo()),
              new PostingLine("2202", "应付账款", BigDecimal.ZERO, saved.getAmount(), saved.getCode())
          ));
    }
    portalNotifier.notify(saved.getSupplierId(), "INVOICE",
        "发票审核" + ("APPROVED".equals(decision) ? "通过" : "驳回"),
        "发票 " + saved.getInvoiceNo() + " 已" + ("APPROVED".equals(decision) ? "审核通过并入账。" : "审核驳回，请在门户查看原因。"),
        "ORDER", saved.getOrderId() == null ? null : saved.getOrderId());
    return saved;
  }

  @Transactional
  public SupplierInvoice verifyInvoice(UUID invoiceId, VerifyInvoice request) {
    SupplierInvoice invoice = invoices.findById(invoiceId)
        .orElseThrow(() -> new BusinessException("供应商发票不存在"));
    if ("PENDING".equals(invoice.getApprovalStatus())) {
      throw new BusinessException("请先完成发票审核再进行验真");
    }
    String decision = request.decision().toUpperCase();
    if (!"VERIFIED".equals(decision) && !"EXCEPTION".equals(decision)) {
      throw new BusinessException("验真结果只能为 VERIFIED 或 EXCEPTION");
    }
    invoice.setVerificationStatus(decision);
    invoice.setVerifiedBy(currentName());
    invoice.setVerifiedAt(OffsetDateTime.now());
    invoice.setVerificationComment(request.comment());
    return invoices.save(invoice);
  }

  @Transactional(readOnly = true)
  public List<SupplierInvoice> listInvoices() {
    return invoices.findAllByOrderByInvoiceDateDesc();
  }


  @Transactional(readOnly = true)
  public List<InvoiceSubmissionResponse> listInvoiceSubmissions(String status) {
    List<SupplierInvoiceSubmission> items = isBlank(status)
        ? invoiceSubmissions.findAllByOrderByCreatedAtDesc()
        : invoiceSubmissions.findByStatusOrderByCreatedAtDesc(status);
    return items.stream()
        .map(item -> invoiceSubmissionView(item,
            orderCode(item.getOrderId()), supplierName(item.getSupplierId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public InvoiceSubmissionResponse invoiceSubmission(UUID id) {
    SupplierInvoiceSubmission submission = invoiceSubmissions.findById(id)
        .orElseThrow(() -> new BusinessException("开票资料不存在"));
    return invoiceSubmissionView(submission,
        orderCode(submission.getOrderId()), supplierName(submission.getSupplierId()));
  }

  @Transactional(readOnly = true)
  public Resource loadInvoiceSubmissionFile(UUID id) {
    SupplierInvoiceSubmission submission = invoiceSubmissions.findById(id)
        .orElseThrow(() -> new BusinessException("开票资料不存在"));
    return storage.loadInNamespace("supplier-invoices", submission.getObjectKey());
  }

  @Transactional
  public InvoiceSubmissionResponse reviewInvoiceSubmission(
      UUID id,
      ReviewInvoiceSubmissionRequest request
  ) {
    SupplierInvoiceSubmission submission = invoiceSubmissions.findById(id)
        .orElseThrow(() -> new BusinessException("开票资料不存在"));
    if (!"PENDING".equals(submission.getStatus())) {
      throw new BusinessException("该开票资料已审核");
    }
    String action = request.action().toUpperCase();
    if (!"APPROVED".equals(action) && !"REJECTED".equals(action)) {
      throw new BusinessException("审核结果只能为 APPROVED 或 REJECTED");
    }
    if ("APPROVED".equals(action)) {
      if (invoices.existsByInvoiceNo(submission.getInvoiceNo())) {
        throw new BusinessException("发票号码 " + submission.getInvoiceNo() + " 已登记，无法审核通过");
      }
      CreateInvoice createRequest = new CreateInvoice(
          submission.getOrderId(),
          submission.getInvoiceNo(),
          submission.getAmount(),
          submission.getTaxRate(),
          submission.getInvoiceDate(),
          submission.getRemark(),
          null,
          null,
          null,
          "SUBMISSION:" + submission.getId(),
          null);
      createInvoice(createRequest);
      submission.setStatus("APPROVED");
      submission.setReviewComment(request.comment());
      submission.setReviewedBy(currentName());
      submission.setReviewedAt(OffsetDateTime.now());
      invoiceSubmissions.save(submission);
      return invoiceSubmissionView(submission,
          orderCode(submission.getOrderId()), supplierName(submission.getSupplierId()));
    }
    submission.setStatus("REJECTED");
    submission.setReviewComment(request.comment());
    submission.setReviewedBy(currentName());
    submission.setReviewedAt(OffsetDateTime.now());
    invoiceSubmissions.save(submission);
    portalNotifier.notify(submission.getSupplierId(), "INVOICE",
        "开票资料被退回",
        "您提交的发票 " + submission.getInvoiceNo() + " 未通过审核："
            + (isBlank(request.comment()) ? "请登录门户查看详情" : request.comment()),
        "ORDER", submission.getOrderId());
    return invoiceSubmissionView(submission,
        orderCode(submission.getOrderId()), supplierName(submission.getSupplierId()));
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listAppeals(String status) {
    List<GoodsReceipt> items = isBlank(status)
        ? receipts.findAllByAppealStatusNotOrderByAppealedAtDesc("NONE")
        : receipts.findByAppealStatusOrderByAppealedAtDesc(status);
    return items.stream()
        .map(item -> {
          PurchaseOrder order = orders.findById(item.getOrderId()).orElse(null);
          return appealView(item, order,
              order == null || order.getSupplierId() == null ? null
                  : suppliers.findById(order.getSupplierId()).orElse(null));
        })
        .toList();
  }

  @Transactional
  public Map<String, Object> resolveAppeal(UUID receiptId, ResolveAppealRequest request) {
    GoodsReceipt receipt = receipts.findById(receiptId)
        .orElseThrow(() -> new BusinessException("到货记录不存在"));
    if (!"PENDING".equals(receipt.getAppealStatus())) {
      throw new BusinessException("该申诉已处理");
    }
    String action = request.action().toUpperCase();
    if (!"DISMISSED".equals(action) && !"REOPEN".equals(action)) {
      throw new BusinessException("处理结果只能为 DISMISSED 或 REOPEN");
    }
    PurchaseOrder order = orders.findById(receipt.getOrderId())
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if ("REOPEN".equals(action)) {
      receipt.setInspectionStatus("PENDING");
      receipt.setQualifiedQty(null);
      receipt.setRejectedQty(BigDecimal.ZERO);
      receipt.setInspectorName(null);
      receipt.setInspectionComment(null);
      receipt.setInspectedAt(null);
      receipt.setAppealStatus("REOPENED");
      receipt.setAppealResolution("REOPENED");
      receipt.setAppealReviewComment(request.comment());
      receipt.setAppealReviewedBy(currentName());
      receipt.setAppealReviewedAt(OffsetDateTime.now());
      receipts.save(receipt);
      portalNotifier.notify(order.getSupplierId(), "INSPECTION",
          "质检申诉已受理",
          "订单 " + order.getCode() + " 的质检申诉已受理，收货记录将重新质检，请在门户查看最新结果。"
              + (isBlank(request.comment()) ? "" : "采购方说明：" + request.comment()),
          "ORDER", order.getId());
      return appealView(receipt, order, suppliers.findById(order.getSupplierId()).orElse(null));
    }
    receipt.setAppealStatus("DISMISSED");
    receipt.setAppealResolution("DISMISSED");
    receipt.setAppealReviewComment(request.comment());
    receipt.setAppealReviewedBy(currentName());
    receipt.setAppealReviewedAt(OffsetDateTime.now());
    receipts.save(receipt);
    portalNotifier.notify(order.getSupplierId(), "INSPECTION",
        "质检申诉未成立",
        "订单 " + order.getCode() + " 的质检申诉未成立，维持原质检结果。"
            + (isBlank(request.comment()) ? "" : "采购方意见：" + request.comment()),
        "ORDER", order.getId());
    return appealView(receipt, order, suppliers.findById(order.getSupplierId()).orElse(null));
  }

  private Map<String, Object> appealView(GoodsReceipt receipt, PurchaseOrder order, Supplier supplier) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", receipt.getId());
    view.put("code", receipt.getCode());
    view.put("orderId", receipt.getOrderId());
    view.put("orderCode", order == null ? null : order.getCode());
    view.put("supplierId", order == null ? null : order.getSupplierId());
    view.put("supplierName", supplier == null ? null : supplier.getName());
    view.put("partName", order == null ? null : order.getPartName());
    view.put("quantity", receipt.getQuantity());
    view.put("qualifiedQty", receipt.getQualifiedQty());
    view.put("rejectedQty", receipt.getRejectedQty());
    view.put("inspectionStatus", receipt.getInspectionStatus());
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
    view.put("receivedDate", receipt.getReceivedDate());
    return view;
  }

  private String orderCode(UUID orderId) {
    return orderId == null ? null
        : orders.findById(orderId).map(PurchaseOrder::getCode).orElse(null);
  }

  private String supplierName(UUID supplierId) {
    return supplierId == null ? null
        : suppliers.findById(supplierId).map(Supplier::getName).orElse(null);
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

  private void postQualifiedReceipt(PurchaseOrder order, GoodsReceipt receipt, InspectReceipt request) {
    InventoryPart part = parts.findById(order.getPartId())
        .orElseThrow(() -> new BusinessException("物料不存在"));
    BigDecimal oldStock = part.getStockQty();
    BigDecimal newStock = oldStock.add(request.qualifiedQty());
    BigDecimal weightedCost = oldStock.multiply(part.getUnitCost())
        .add(request.qualifiedQty().multiply(order.getUnitPrice()));
    part.setStockQty(newStock);
    part.setUnitCost(newStock.signum() == 0 ? order.getUnitPrice()
        : weightedCost.divide(newStock, 2, RoundingMode.HALF_UP));
    parts.save(part);

    StockMovement movement = new StockMovement();
    movement.setPartId(part.getId());
    movement.setMovementType(StockMovementType.INBOUND);
    movement.setQuantity(request.qualifiedQty());
    movement.setSourceNo(order.getCode());
    movement.setRemark("采购质检合格入库 " + receipt.getCode());
    movement.setOperatorName(currentName());
    movements.save(movement);

    BigDecimal amount = request.qualifiedQty().multiply(order.getUnitPrice());
    ProcurementPayable payable = new ProcurementPayable();
    payable.setCode("YF-" + receipt.getCode());
    payable.setSupplierId(order.getSupplierId());
    payable.setOrderId(order.getId());
    UUID organizationId = order.getDepartmentId();
    if (organizationId == null && order.getProjectId() != null) {
      organizationId = projects.findById(order.getProjectId())
          .map(Project::getManagerUserId)
          .map(dataScopeService::organizationIdForUser)
          .orElse(null);
    }
    payable.setOrganizationId(organizationId == null
        ? dataScopeService.currentOrganizationId() : organizationId);
    payable.setReceiptId(receipt.getId());
    payable.setAmount(amount);
    payable.setTaxRate(order.getTaxRate());
    payable.setPaidAmount(BigDecimal.ZERO);
    payable.setDueDate(receipt.getPayableDueDate());
    payable.setStatus(PayableStatus.PENDING);
    payable.setHandlerName(currentName());
    payables.save(payable);
    portalNotifier.notify(order.getSupplierId(), "PAYABLE",
        "应付单已生成",
        "订单 " + order.getCode() + " 合格入库 " + receipt.getCode()
            + "，应付金额 " + amount.stripTrailingZeros().toPlainString()
            + "（到期日 " + (receipt.getPayableDueDate() == null ? "未设置" : receipt.getPayableDueDate()) + "），可在门户对账查看。",
        "ORDER", order.getId());

    ProcurementCostAllocation cost = new ProcurementCostAllocation();
    cost.setOrderId(order.getId());
    cost.setReceiptId(receipt.getId());
    cost.setCostType(order.getCostType());
    cost.setProjectId(order.getProjectId());
    cost.setDepartmentId(order.getDepartmentId());
    cost.setTargetCode(order.getCostTargetCode());
    cost.setTargetName(order.getCostTargetName());
    cost.setPartName(order.getPartName());
    cost.setAmount(amount);
    cost.setIncurredDate(receipt.getReceivedDate());
    costs.save(cost);

    order.setReceivedQty(order.getReceivedQty().add(request.qualifiedQty()));
    order.setStatus(order.getReceivedQty().compareTo(order.getOrderedQty()) >= 0
        ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIAL_RECEIVED);
    orders.save(order);
  }

  private List<ProcurementPayable> resolvePayables(PurchaseOrder order, CreateInvoice request) {
    if (request.payableIds() != null && !request.payableIds().isEmpty()) {
      List<ProcurementPayable> payables = this.payables.findAllById(request.payableIds());
      for (ProcurementPayable payable : payables) {
        if (!payable.getOrderId().equals(order.getId())) {
          throw new BusinessException("所选应付不属于该采购订单");
        }
      }
      return payables;
    }
    if (request.payableId() != null) {
      ProcurementPayable payable = payables.findById(request.payableId())
          .orElseThrow(() -> new BusinessException("采购应付不存在"));
      if (!payable.getOrderId().equals(order.getId())) {
        throw new BusinessException("所选应付不属于该采购订单");
      }
      return List.of(payable);
    }
    if (request.receiptId() != null) {
      ProcurementPayable payable = payables.findByReceiptId(request.receiptId())
          .orElseThrow(() -> new BusinessException("该收货记录尚未形成应付"));
      return List.of(payable);
    }
    return List.of();
  }

  private BigDecimal matchTolerance(BigDecimal amount) {
    return valueOr(amount, BigDecimal.ZERO).multiply(BigDecimal.valueOf(0.005))
        .max(BigDecimal.valueOf(0.01));
  }

  private void requireEligibleSupplier(Supplier supplier) {
    if (supplier.getRiskStatus() == SupplierRiskStatus.BLOCKED) {
      throw new BusinessException("冻结供应商不能参与采购");
    }
    if (!"APPROVED".equals(supplier.getAdmissionStatus())) {
      throw new BusinessException("供应商尚未完成准入审批");
    }
  }

  private Map<String, Object> inquiryView(ProcurementInquiry inquiry) {
    List<ProcurementInquiryRequest> links =
        inquiryRequests.findByInquiryIdOrderByCreatedAtAsc(inquiry.getId());
    if (links.isEmpty()) {
      ProcurementInquiryRequest fallback = new ProcurementInquiryRequest();
      fallback.setInquiryId(inquiry.getId());
      fallback.setRequestId(inquiry.getRequestId());
      PurchaseRequest source = requests.findById(inquiry.getRequestId()).orElse(null);
      fallback.setRequestedQty(source == null ? BigDecimal.ZERO : source.getQuantity());
      links = List.of(fallback);
    }
    Map<UUID, PurchaseRequest> requestMap = requests.findAllById(
        links.stream().map(ProcurementInquiryRequest::getRequestId).toList()
    ).stream().collect(Collectors.toMap(PurchaseRequest::getId, item -> item));
    List<Map<String, Object>> requestLines = links.stream().map(link -> {
      PurchaseRequest source = requestMap.get(link.getRequestId());
      Map<String, Object> line = new LinkedHashMap<>();
      line.put("requestId", link.getRequestId());
      line.put("requestCode", source == null ? null : source.getCode());
      line.put("batchCode", source == null ? null : source.getBatchCode());
      line.put("partId", source == null ? null : source.getPartId());
      line.put("partName", source == null ? null : source.getPartName());
      line.put("requestedQty", link.getRequestedQty());
      line.put("costTargetName", source == null ? null : source.getCostTargetName());
      line.put("expectedDate", source == null ? null : source.getExpectedDate());
      return line;
    }).toList();
    int materialCount = (int) requestMap.values().stream()
        .map(this::sourcingGroupKey).distinct().count();
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", inquiry.getId());
    view.put("code", inquiry.getCode());
    view.put("requestId", inquiry.getRequestId());
    view.put("requestIds", links.stream().map(ProcurementInquiryRequest::getRequestId).toList());
    view.put("requestCount", links.size());
    view.put("materialCount", materialCount);
    view.put("totalRequestedQty", links.stream().map(ProcurementInquiryRequest::getRequestedQty)
        .reduce(BigDecimal.ZERO, BigDecimal::add));
    view.put("requestLines", requestLines);
    view.put("partName", materialCount <= 1
        ? requestLines.isEmpty() ? null : requestLines.get(0).get("partName")
        : materialCount + "种物料");
    view.put("title", inquiry.getTitle());
    view.put("deadline", inquiry.getDeadline());
    view.put("status", inquiry.getStatus());
    view.put("createdByName", inquiry.getCreatedByName());
    view.put("sourcingMethod", inquiry.getSourcingMethod());
    view.put("minQuoteCount", inquiry.getMinQuoteCount());
    view.put("exceptionReason", inquiry.getExceptionReason());
    view.put("selectedQuoteId", inquiry.getSelectedQuoteId());
    view.put("selectionReason", inquiry.getSelectionReason());
    view.put("selectedByName", inquiry.getSelectedByName());
    view.put("selectedAt", inquiry.getSelectedAt());
    List<ProcurementInquiryInvitation> invitationList =
        invitations.findByInquiryIdOrderByInvitedAtAsc(inquiry.getId());
    Map<UUID, Supplier> invitedSuppliers = suppliers.findAllById(
        invitationList.stream().map(ProcurementInquiryInvitation::getSupplierId).toList()
    ).stream().collect(Collectors.toMap(Supplier::getId, item -> item));
    view.put("invitations", invitationList.stream().map(invitation -> {
      Supplier supplier = invitedSuppliers.get(invitation.getSupplierId());
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", invitation.getId());
      item.put("supplierId", invitation.getSupplierId());
      item.put("supplierName", supplier == null ? null : supplier.getName());
      item.put("status", invitation.getStatus());
      item.put("invitedByName", invitation.getInvitedByName());
      item.put("invitedAt", invitation.getInvitedAt());
      item.put("viewedAt", invitation.getViewedAt());
      item.put("respondedAt", invitation.getRespondedAt());
      item.put("deliveryStatus", invitation.getDeliveryStatus());
      item.put("deliveryAttemptCount", invitation.getDeliveryAttemptCount());
      item.put("lastDeliveryAt", invitation.getLastDeliveryAt());
      item.put("deliveryError", invitation.getDeliveryError());
      return item;
    }).toList());
    view.put("quotes", quotes.findByInquiryIdOrderByUnitPriceAsc(inquiry.getId()).stream()
        .filter(item -> !"DRAFT".equals(item.getSubmissionStatus()))
        .map(this::quoteView).toList());
    return view;
  }

  private Set<UUID> activeInquiryRequestIds() {
    return inquiryRequests.findByInquiryStatusIn(ACTIVE_INQUIRY_STATUSES).stream()
        .map(ProcurementInquiryRequest::getRequestId)
        .collect(Collectors.toSet());
  }

  private String sourcingGroupKey(PurchaseRequest request) {
    if (request.getPartId() != null) return "PART:" + request.getPartId();
    String normalized = defaultText(request.getPartName(), "未命名物料")
        .replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
    return "NAME:" + normalized;
  }

  private void validateSourcingRules(String method, int minQuotes, String exceptionReason) {
    if (minQuotes < 1) {
      throw new BusinessException("最低报价数量必须大于零");
    }
    if ("SINGLE_SOURCE".equals(method) && isBlank(exceptionReason)) {
      throw new BusinessException("单一来源采购必须填写例外原因");
    }
  }

  private BigDecimal add(BigDecimal left, BigDecimal right) {
    return valueOr(left, BigDecimal.ZERO).add(valueOr(right, BigDecimal.ZERO));
  }

  private BigDecimal netAmount(BigDecimal grossAmount, BigDecimal taxRate) {
    BigDecimal rate = valueOr(taxRate, BigDecimal.valueOf(13));
    return valueOr(grossAmount, BigDecimal.ZERO)
        .divide(BigDecimal.ONE.add(rate.movePointLeft(2)), 2, RoundingMode.HALF_UP);
  }

  private Map<String, Object> quoteView(SupplierQuotation quote) {
    Supplier supplier = suppliers.findById(quote.getSupplierId()).orElse(null);
    List<SupplierQuotationLine> persistedLines =
        quoteLines.findByQuoteIdOrderByCreatedAtAsc(quote.getId());
    List<ProcurementInquiryRequest> inquiryLinks =
        inquiryRequests.findByInquiryIdOrderByCreatedAtAsc(quote.getInquiryId());
    if (inquiryLinks.isEmpty()) {
      ProcurementInquiry inquiry = inquiries.findById(quote.getInquiryId()).orElse(null);
      if (inquiry != null) {
        ProcurementInquiryRequest fallback = new ProcurementInquiryRequest();
        fallback.setInquiryId(inquiry.getId());
        fallback.setRequestId(inquiry.getRequestId());
        PurchaseRequest source = requests.findById(inquiry.getRequestId()).orElse(null);
        fallback.setRequestedQty(source == null ? BigDecimal.ZERO : source.getQuantity());
        inquiryLinks = List.of(fallback);
      }
    }
    Map<UUID, PurchaseRequest> requestMap = requests.findAllById(
        inquiryLinks.stream().map(ProcurementInquiryRequest::getRequestId).toList()
    ).stream().collect(Collectors.toMap(PurchaseRequest::getId, item -> item));
    List<Map<String, Object>> lineViews;
    if (persistedLines.isEmpty()) {
      lineViews = inquiryLinks.stream().map(link -> quoteLineView(
          link.getRequestId(), requestMap.get(link.getRequestId()), link.getRequestedQty(),
          quote.getUnitPrice(), quote.getTaxRate(), quote.getDeliveryDate(), quote.getRemark()
      )).toList();
    } else {
      lineViews = persistedLines.stream().map(line -> quoteLineView(
          line.getRequestId(), requestMap.get(line.getRequestId()), line.getQuantity(),
          line.getUnitPrice(), line.getTaxRate(), line.getDeliveryDate(), line.getRemark()
      )).toList();
    }
    BigDecimal materialAmount = lineViews.stream()
        .map(line -> (BigDecimal) line.get("amount"))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", quote.getId());
    view.put("supplierId", quote.getSupplierId());
    view.put("supplierName", supplier == null ? null : supplier.getName());
    view.put("unitPrice", quote.getUnitPrice());
    view.put("taxRate", quote.getTaxRate());
    view.put("deliveryDate", quote.getDeliveryDate());
    view.put("paymentTerms", quote.getPaymentTerms());
    view.put("remark", quote.getRemark());
    view.put("selected", quote.isSelected());
    view.put("currency", quote.getCurrency());
    view.put("freightAmount", quote.getFreightAmount());
    view.put("otherCostAmount", quote.getOtherCostAmount());
    view.put("technicalScore", quote.getTechnicalScore());
    view.put("commercialScore", quote.getCommercialScore());
    view.put("totalScore", quote.getTotalScore());
    view.put("validUntil", quote.getValidUntil());
    view.put("submissionSource", quote.getSubmissionSource());
    view.put("submissionStatus", quote.getSubmissionStatus());
    view.put("versionNo", quote.getVersionNo());
    view.put("submittedByType", quote.getSubmittedByType());
    view.put("submittedById", quote.getSubmittedById());
    view.put("submittedByName", quote.getSubmittedByName());
    view.put("submittedAt", quote.getSubmittedAt());
    view.put("confirmed", quote.getConfirmedAt() != null);
    view.put("confirmedAt", quote.getConfirmedAt());
    view.put("declinedAt", quote.getDeclinedAt());
    view.put("declineReason", quote.getDeclineReason());
    view.put("lines", lineViews);
    view.put("materialAmount", materialAmount);
    view.put("totalAmount", materialAmount
        .add(valueOr(quote.getFreightAmount(), BigDecimal.ZERO))
        .add(valueOr(quote.getOtherCostAmount(), BigDecimal.ZERO)));
    return view;
  }

  private Map<String, Object> quoteLineView(
      UUID requestId,
      PurchaseRequest request,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal taxRate,
      LocalDate deliveryDate,
      String remark
  ) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("requestId", requestId);
    view.put("requestCode", request == null ? null : request.getCode());
    view.put("partName", request == null ? null : request.getPartName());
    view.put("quantity", quantity);
    view.put("unitPrice", unitPrice);
    view.put("taxRate", taxRate);
    view.put("deliveryDate", deliveryDate);
    view.put("remark", remark);
    view.put("amount", valueOr(quantity, BigDecimal.ZERO)
        .multiply(valueOr(unitPrice, BigDecimal.ZERO)));
    return view;
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }

  private UUID currentUserId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.id() : null;
  }

  private static BigDecimal valueOr(BigDecimal value, BigDecimal fallback) {
    return value == null ? fallback : value;
  }

  private static String plainQty(BigDecimal value) {
    return value == null ? "0" : value.stripTrailingZeros().toPlainString();
  }

  private static String defaultText(String value, String fallback) {
    return isBlank(value) ? fallback : value.trim();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String sha256Text(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new BusinessException("注册码生成失败");
    }
  }
}
