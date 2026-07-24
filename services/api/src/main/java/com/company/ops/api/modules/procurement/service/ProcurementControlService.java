package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.dto.ProcurementControlDtos.*;
import com.company.ops.api.modules.procurement.dto.CreateConsolidatedInquiryRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementPurchasePoolResponse;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.repository.*;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
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
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcurementControlService {
  private final ProcurementInquiryRepository inquiries;
  private final ProcurementInquiryRequestRepository inquiryRequests;
  private final SupplierQuotationRepository quotes;
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
  private final PurchaseRequestApprovalRecordRepository requestApprovals;
  private final ProjectRepository projects;
  private final ProcurementArrivalService arrivals;

  public ProcurementControlService(
      ProcurementInquiryRepository inquiries,
      ProcurementInquiryRequestRepository inquiryRequests,
      SupplierQuotationRepository quotes,
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
      PurchaseRequestApprovalRecordRepository requestApprovals,
      ProjectRepository projects,
      ProcurementArrivalService arrivals
  ) {
    this.inquiries = inquiries;
    this.inquiryRequests = inquiryRequests;
    this.quotes = quotes;
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
    this.requestApprovals = requestApprovals;
    this.projects = projects;
    this.arrivals = arrivals;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listInquiries() {
    return inquiries.findAllByOrderByCreatedAtDesc().stream().map(this::inquiryView).toList();
  }

  @Transactional(readOnly = true)
  public ProcurementPurchasePoolResponse purchasePool() {
    List<PurchaseRequest> approved = requests.findAllByOrderByCreatedAtDesc().stream()
        .filter(item -> item.getApprovalStatus() == ApprovalStatus.APPROVED)
        .filter(item -> item.getStatus() == PurchaseRequestStatus.APPROVED)
        .toList();
    Map<UUID, ProcurementInquiry> inquiryMap = inquiries.findAll().stream()
        .collect(Collectors.toMap(ProcurementInquiry::getId, item -> item));
    Set<UUID> activeInquiryRequestIds = inquiryRequests.findAll().stream()
        .filter(link -> {
          ProcurementInquiry inquiry = inquiryMap.get(link.getInquiryId());
          return inquiry != null
              && ("OPEN".equals(inquiry.getStatus()) || "AWARDED".equals(inquiry.getStatus()));
        })
        .map(ProcurementInquiryRequest::getRequestId)
        .collect(Collectors.toSet());
    Map<UUID, BigDecimal> orderedByRequest = orders.findAll().stream()
        .filter(order -> order.getRequestId() != null)
        .filter(order -> order.getStatus() != PurchaseOrderStatus.CANCELLED)
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
    Map<UUID, BigDecimal> orderedByRequest = orders.findAll().stream()
        .filter(order -> order.getRequestId() != null)
        .filter(order -> order.getStatus() != PurchaseOrderStatus.CANCELLED)
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
    BigDecimal technical = valueOr(request.technicalScore(), BigDecimal.valueOf(100));
    BigDecimal commercial = valueOr(request.commercialScore(), BigDecimal.valueOf(100));
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
  public Map<String, Object> selectQuote(UUID inquiryId, UUID quoteId, SelectSupplierQuote request) {
    ProcurementInquiry inquiry = inquiries.findById(inquiryId)
        .orElseThrow(() -> new BusinessException("询价单不存在"));
    if (!"OPEN".equals(inquiry.getStatus())) {
      throw new BusinessException("只有进行中的询价可以定标");
    }
    List<SupplierQuotation> allQuotes = quotes.findByInquiryIdOrderByUnitPriceAsc(inquiryId);
    if (allQuotes.size() < inquiry.getMinQuoteCount()
        && !"SINGLE_SOURCE".equals(inquiry.getSourcingMethod())) {
      throw new BusinessException("有效报价不足 " + inquiry.getMinQuoteCount() + " 家，不能定标");
    }
    SupplierQuotation selected = quotes.findById(quoteId)
        .orElseThrow(() -> new BusinessException("供应商报价不存在"));
    if (!selected.getInquiryId().equals(inquiryId)) {
      throw new BusinessException("报价不属于该询价单");
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
    return inquiryView(inquiries.save(inquiry));
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
    String operator = currentName();
    receipt.setQualifiedQty(request.qualifiedQty());
    receipt.setRejectedQty(request.rejectedQty());
    receipt.setInspectorName(operator);
    receipt.setInspectionComment(request.comment());
    receipt.setInspectedAt(OffsetDateTime.now());
    receipt.setInspectionStatus(request.rejectedQty().signum() == 0 ? "PASSED"
        : request.qualifiedQty().signum() == 0 ? "REJECTED" : "PARTIAL");
    receipt.setPayableDueDate(request.payableDueDate());
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
    item.setReplacementQty(valueOr(request.replacementQty(), BigDecimal.ZERO));
    item.setCreditAmount(valueOr(request.creditAmount(), BigDecimal.ZERO));
    item.setClaimAmount(valueOr(request.claimAmount(), BigDecimal.ZERO));
    item.setCorrectiveAction(request.correctiveAction());
    item.setSupplierResponse(request.supplierResponse());
    item.setHandlerName(currentName());
    item.setStatus("COMPLETED");
    item.setCompletedAt(OffsetDateTime.now());
    return returns.save(item);
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
    ProcurementPayable payable = resolvePayable(order, request);
    BigDecimal eligible = payable == null
        ? payables.findByOrderId(order.getId()).stream()
            .filter(item -> item.getStatus() != PayableStatus.CANCELLED)
            .map(ProcurementPayable::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
        : payable.getAmount();
    BigDecimal previous = (payable == null ? invoices.findByOrderId(order.getId())
        : invoices.findByPayableId(payable.getId())).stream()
        .filter(item -> !"REJECTED".equals(item.getApprovalStatus()))
        .map(SupplierInvoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal remaining = eligible.subtract(previous).max(BigDecimal.ZERO);
    BigDecimal matched = request.amount().min(remaining);

    SupplierInvoice invoice = new SupplierInvoice();
    invoice.setCode("CGFP-" + System.currentTimeMillis());
    invoice.setInvoiceNo(request.invoiceNo());
    invoice.setOrderId(order.getId());
    invoice.setSupplierId(order.getSupplierId());
    invoice.setPayableId(payable == null ? null : payable.getId());
    invoice.setReceiptId(request.receiptId() != null ? request.receiptId()
        : payable == null ? null : payable.getReceiptId());
    invoice.setAmount(request.amount());
    invoice.setMatchedAmount(matched);
    invoice.setTaxRate(request.taxRate());
    invoice.setInvoiceDate(request.invoiceDate());
    invoice.setDifferenceAmount(request.amount().subtract(matched));
    invoice.setMatchStatus(request.amount().compareTo(matched) == 0 ? "MATCHED" : "MISMATCH");
    invoice.setApprovalStatus("PENDING");
    invoice.setVerificationStatus("MATCHED".equals(invoice.getMatchStatus()) ? "VERIFIED" : "EXCEPTION");
    invoice.setClientRequestId(request.clientRequestId());
    invoice.setAttachmentDocumentId(request.attachmentDocumentId());
    invoice.setRemark(request.remark());
    return invoices.save(invoice);
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
    if ("APPROVED".equals(decision) && !"MATCHED".equals(invoice.getMatchStatus())) {
      throw new BusinessException("三单匹配异常的发票不能直接审核通过");
    }
    invoice.setApprovalStatus(decision);
    invoice.setApprovedByName(currentName());
    invoice.setApprovedAt(OffsetDateTime.now());
    if (!isBlank(request.comment())) {
      invoice.setRemark(defaultText(invoice.getRemark(), "") + " 审核：" + request.comment());
    }
    return invoices.save(invoice);
  }

  @Transactional(readOnly = true)
  public List<SupplierInvoice> listInvoices() {
    return invoices.findAllByOrderByInvoiceDateDesc();
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
    movements.save(movement);

    BigDecimal amount = request.qualifiedQty().multiply(order.getUnitPrice());
    ProcurementPayable payable = new ProcurementPayable();
    payable.setCode("YF-" + receipt.getCode());
    payable.setSupplierId(order.getSupplierId());
    payable.setOrderId(order.getId());
    payable.setReceiptId(receipt.getId());
    payable.setAmount(amount);
    payable.setTaxRate(order.getTaxRate());
    payable.setPaidAmount(BigDecimal.ZERO);
    payable.setDueDate(request.payableDueDate());
    payable.setStatus(PayableStatus.PENDING);
    payables.save(payable);

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

  private ProcurementPayable resolvePayable(PurchaseOrder order, CreateInvoice request) {
    if (request.payableId() != null) {
      ProcurementPayable payable = payables.findById(request.payableId())
          .orElseThrow(() -> new BusinessException("采购应付不存在"));
      if (!payable.getOrderId().equals(order.getId())) {
        throw new BusinessException("所选应付不属于该采购订单");
      }
      return payable;
    }
    if (request.receiptId() != null) {
      return payables.findByReceiptId(request.receiptId())
          .orElseThrow(() -> new BusinessException("该收货记录尚未形成应付"));
    }
    return null;
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
    view.put("quotes", quotes.findByInquiryIdOrderByUnitPriceAsc(inquiry.getId()).stream()
        .map(this::quoteView).toList());
    return view;
  }

  private Set<UUID> activeInquiryRequestIds() {
    Map<UUID, ProcurementInquiry> inquiryMap = inquiries.findAll().stream()
        .collect(Collectors.toMap(ProcurementInquiry::getId, item -> item));
    return inquiryRequests.findAll().stream()
        .filter(link -> {
          ProcurementInquiry inquiry = inquiryMap.get(link.getInquiryId());
          return inquiry != null
              && ("OPEN".equals(inquiry.getStatus()) || "AWARDED".equals(inquiry.getStatus()));
        })
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

  private static BigDecimal valueOr(BigDecimal value, BigDecimal fallback) {
    return value == null ? fallback : value;
  }

  private static String defaultText(String value, String fallback) {
    return isBlank(value) ? fallback : value.trim();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
