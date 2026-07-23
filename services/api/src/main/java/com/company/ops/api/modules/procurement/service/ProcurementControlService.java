package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.dto.ProcurementControlDtos.*;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.repository.*;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcurementControlService {
  private final ProcurementInquiryRepository inquiries;
  private final SupplierQuotationRepository quotes;
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

  public ProcurementControlService(
      ProcurementInquiryRepository inquiries,
      SupplierQuotationRepository quotes,
      PurchaseRequestRepository requests,
      SupplierRepository suppliers,
      PurchaseOrderRepository orders,
      GoodsReceiptRepository receipts,
      InventoryPartRepository parts,
      StockMovementRepository movements,
      ProcurementPayableRepository payables,
      ProcurementCostAllocationRepository costs,
      ProcurementReturnOrderRepository returns,
      SupplierInvoiceRepository invoices
  ) {
    this.inquiries = inquiries;
    this.quotes = quotes;
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
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listInquiries() {
    return inquiries.findAllByOrderByCreatedAtDesc().stream().map(this::inquiryView).toList();
  }

  @Transactional
  public Map<String, Object> createInquiry(CreateInquiry request) {
    PurchaseRequest purchaseRequest = requests.findById(request.requestId())
        .orElseThrow(() -> new BusinessException("采购申请不存在"));
    if (purchaseRequest.getApprovalStatus() != ApprovalStatus.APPROVED) {
      throw new BusinessException("采购申请审批通过后才能询价");
    }
    String method = defaultText(request.sourcingMethod(), "COMPETITIVE").toUpperCase();
    int minQuotes = request.minQuoteCount() == null ? ("SINGLE_SOURCE".equals(method) ? 1 : 3)
        : request.minQuoteCount();
    if (minQuotes < 1) {
      throw new BusinessException("最低报价数量必须大于零");
    }
    if ("SINGLE_SOURCE".equals(method) && isBlank(request.exceptionReason())) {
      throw new BusinessException("单一来源采购必须填写例外原因");
    }
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
    return inquiryView(inquiries.save(inquiry));
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
    BigDecimal technical = valueOr(request.technicalScore(), BigDecimal.valueOf(100));
    BigDecimal commercial = valueOr(request.commercialScore(), BigDecimal.valueOf(100));
    SupplierQuotation quote = new SupplierQuotation();
    quote.setInquiryId(inquiryId);
    quote.setSupplierId(request.supplierId());
    quote.setUnitPrice(request.unitPrice());
    quote.setTaxRate(request.taxRate());
    quote.setDeliveryDate(request.deliveryDate());
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
    return quoteView(quotes.save(quote));
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
    if (!isBlank(request.clientRequestId())) {
      Optional<GoodsReceipt> existing = receipts.findByClientRequestId(request.clientRequestId());
      if (existing.isPresent()) {
        return existing.get();
      }
    }
    PurchaseOrder order = orders.findByIdForUpdate(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getApprovalStatus() != ApprovalStatus.APPROVED
        || (order.getStatus() != PurchaseOrderStatus.ORDERED
        && order.getStatus() != PurchaseOrderStatus.PARTIAL_RECEIVED)) {
      throw new BusinessException("只有审批通过且未关闭的订单可以登记到货");
    }
    BigDecimal pending = receipts.findByOrderId(orderId).stream()
        .filter(item -> "PENDING".equals(item.getInspectionStatus()))
        .map(GoodsReceipt::getQuantity)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (order.getReceivedQty().add(pending).add(request.quantity()).compareTo(order.getOrderedQty()) > 0) {
      throw new BusinessException("到货数量超过订单剩余数量");
    }
    GoodsReceipt receipt = new GoodsReceipt();
    receipt.setCode("DH-" + order.getCode() + "-" + String.format("%02d", receipts.countByOrderId(orderId) + 1));
    receipt.setOrderId(orderId);
    receipt.setPartId(order.getPartId());
    receipt.setQuantity(request.quantity());
    receipt.setUnitPrice(order.getUnitPrice());
    receipt.setTaxRate(order.getTaxRate());
    receipt.setAmount(request.quantity().multiply(order.getUnitPrice()));
    receipt.setReceivedDate(request.receivedDate());
    receipt.setDeliveryNo(request.deliveryNo());
    receipt.setReceiverName(currentName());
    receipt.setPayableDueDate(request.payableDueDate());
    receipt.setInspectionStatus("PENDING");
    receipt.setClientRequestId(request.clientRequestId());
    receipt.setAsnNo(request.asnNo());
    return receipts.save(receipt);
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
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", inquiry.getId());
    view.put("code", inquiry.getCode());
    view.put("requestId", inquiry.getRequestId());
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

  private Map<String, Object> quoteView(SupplierQuotation quote) {
    Supplier supplier = suppliers.findById(quote.getSupplierId()).orElse(null);
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
