package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseRequestRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementGovernanceDtos.*;
import com.company.ops.api.modules.procurement.dto.PurchaseRequestResponse;
import com.company.ops.api.modules.procurement.repository.*;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcurementGovernanceService {
  private final ProcurementContractRepository contracts;
  private final SupplierChangeRequestRepository supplierChanges;
  private final SupplierPerformanceReviewRepository reviews;
  private final ProcurementCollaborationEventRepository collaborationEvents;
  private final ProcurementActionLogRepository actionLogs;
  private final SupplierRepository suppliers;
  private final PurchaseOrderRepository orders;
  private final GoodsReceiptRepository receipts;
  private final SupplierInvoiceRepository invoices;
  private final PurchaseRequestRepository purchaseRequests;
  private final InventoryPartRepository parts;
  private final ProcurementService procurementService;

  public ProcurementGovernanceService(
      ProcurementContractRepository contracts,
      SupplierChangeRequestRepository supplierChanges,
      SupplierPerformanceReviewRepository reviews,
      ProcurementCollaborationEventRepository collaborationEvents,
      ProcurementActionLogRepository actionLogs,
      SupplierRepository suppliers,
      PurchaseOrderRepository orders,
      GoodsReceiptRepository receipts,
      SupplierInvoiceRepository invoices,
      PurchaseRequestRepository purchaseRequests,
      InventoryPartRepository parts,
      ProcurementService procurementService
  ) {
    this.contracts = contracts;
    this.supplierChanges = supplierChanges;
    this.reviews = reviews;
    this.collaborationEvents = collaborationEvents;
    this.actionLogs = actionLogs;
    this.suppliers = suppliers;
    this.orders = orders;
    this.receipts = receipts;
    this.invoices = invoices;
    this.purchaseRequests = purchaseRequests;
    this.parts = parts;
    this.procurementService = procurementService;
  }

  @Transactional(readOnly = true)
  public List<ProcurementContract> listContracts() {
    return contracts.findAllByOrderByCreatedAtDesc();
  }

  @Transactional
  public ProcurementContract createContract(CreateContract request) {
    requireSupplier(request.supplierId());
    ProcurementContract existing = contracts.findFirstByContractNoOrderByVersionNoDesc(request.contractNo())
        .orElse(null);
    if (existing != null) {
      throw new BusinessException("合同编号已存在，如需变更请使用合同变更");
    }
    ProcurementContract contract = new ProcurementContract();
    contract.setContractNo(request.contractNo());
    contract.setName(request.name());
    contract.setSupplierId(request.supplierId());
    contract.setAmount(request.amount());
    contract.setCurrency(blank(request.currency()) ? "CNY" : request.currency());
    contract.setStartDate(request.startDate());
    contract.setEndDate(request.endDate());
    contract.setPaymentTerms(request.paymentTerms());
    contract.setRemark(request.remark());
    contract.setStatus("DRAFT");
    contract.setApprovalStatus("PENDING");
    ProcurementContract saved = contracts.save(contract);
    log("CONTRACT", saved.getId(), "CREATED", "创建采购合同 " + saved.getContractNo());
    return saved;
  }

  @Transactional
  public ProcurementContract submitContract(UUID id) {
    ProcurementContract contract = requireContract(id);
    if (!"DRAFT".equals(contract.getStatus())) {
      throw new BusinessException("只有草稿合同可以提交审批");
    }
    contract.setStatus("PENDING_APPROVAL");
    contract.setApprovalStatus("PENDING");
    contract.setSubmittedByName(currentName());
    contract.setSubmittedAt(OffsetDateTime.now());
    ProcurementContract saved = contracts.save(contract);
    log("CONTRACT", id, "SUBMITTED", "提交合同审批");
    return saved;
  }

  @Transactional
  public ProcurementContract reviewContract(UUID id, ReviewAction request) {
    ProcurementContract contract = requireContract(id);
    if (!"PENDING_APPROVAL".equals(contract.getStatus())) {
      throw new BusinessException("合同当前状态不可审批");
    }
    String decision = decision(request.decision());
    contract.setApprovalStatus(decision);
    contract.setStatus("APPROVED".equals(decision) ? "ACTIVE" : "REJECTED");
    contract.setApprovedByName(currentName());
    contract.setApprovalComment(request.comment());
    contract.setApprovedAt(OffsetDateTime.now());
    ProcurementContract saved = contracts.save(contract);
    log("CONTRACT", id, decision, request.comment());
    return saved;
  }

  @Transactional
  public ProcurementContract amendContract(UUID id, AmendContract request) {
    ProcurementContract parent = requireContract(id);
    if (!"ACTIVE".equals(parent.getStatus())) {
      throw new BusinessException("只有已生效合同可以发起变更");
    }
    parent.setStatus("SUPERSEDED");
    contracts.save(parent);
    ProcurementContract amendment = new ProcurementContract();
    amendment.setContractNo(parent.getContractNo());
    amendment.setName(parent.getName());
    amendment.setSupplierId(parent.getSupplierId());
    amendment.setAmount(request.amount());
    amendment.setCurrency(parent.getCurrency());
    amendment.setStartDate(request.startDate());
    amendment.setEndDate(request.endDate());
    amendment.setPaymentTerms(request.paymentTerms());
    amendment.setRemark(request.remark());
    amendment.setParentContractId(parent.getId());
    amendment.setVersionNo(parent.getVersionNo() + 1);
    amendment.setChangeReason(request.changeReason());
    amendment.setStatus("DRAFT");
    amendment.setApprovalStatus("PENDING");
    ProcurementContract saved = contracts.save(amendment);
    log("CONTRACT", saved.getId(), "AMENDED", request.changeReason());
    return saved;
  }

  @Transactional(readOnly = true)
  public List<SupplierChangeRequest> listSupplierChanges() {
    return supplierChanges.findAllByOrderByCreatedAtDesc();
  }

  @Transactional
  public SupplierChangeRequest createSupplierChange(CreateSupplierChange request) {
    requireSupplier(request.supplierId());
    if (supplierChanges.existsBySupplierIdAndStatus(request.supplierId(), "PENDING")) {
      throw new BusinessException("该供应商已有待审批变更");
    }
    SupplierChangeRequest change = new SupplierChangeRequest();
    change.setSupplierId(request.supplierId());
    change.setChangeType(request.changeType());
    change.setProposedAdmissionStatus(request.proposedAdmissionStatus());
    change.setProposedRiskStatus(request.proposedRiskStatus());
    change.setProposedBankName(request.proposedBankName());
    change.setProposedBankAccount(request.proposedBankAccount());
    change.setProposedSettlementTerms(request.proposedSettlementTerms());
    change.setReason(request.reason());
    change.setRequestedByName(currentName());
    change.setStatus("PENDING");
    SupplierChangeRequest saved = supplierChanges.save(change);
    log("SUPPLIER", change.getSupplierId(), "CHANGE_REQUESTED", request.changeType() + "：" + request.reason());
    return saved;
  }

  @Transactional
  public SupplierChangeRequest reviewSupplierChange(UUID id, ReviewAction request) {
    SupplierChangeRequest change = supplierChanges.findById(id)
        .orElseThrow(() -> new BusinessException("供应商变更申请不存在"));
    if (!"PENDING".equals(change.getStatus())) {
      throw new BusinessException("供应商变更申请已处理");
    }
    String decision = decision(request.decision());
    if ("APPROVED".equals(decision)) {
      Supplier supplier = requireSupplier(change.getSupplierId());
      if (!blank(change.getProposedAdmissionStatus())) {
        supplier.setAdmissionStatus(change.getProposedAdmissionStatus());
      }
      if (!blank(change.getProposedRiskStatus())) {
        try {
          supplier.setRiskStatus(SupplierRiskStatus.valueOf(change.getProposedRiskStatus()));
        } catch (IllegalArgumentException exception) {
          throw new BusinessException("供应商风险状态不合法");
        }
      }
      if (!blank(change.getProposedBankName())) supplier.setBankName(change.getProposedBankName());
      if (!blank(change.getProposedBankAccount())) supplier.setBankAccount(change.getProposedBankAccount());
      if (!blank(change.getProposedSettlementTerms())) {
        supplier.setSettlementTerms(change.getProposedSettlementTerms());
      }
      suppliers.save(supplier);
    }
    change.setStatus(decision);
    change.setReviewedByName(currentName());
    change.setReviewComment(request.comment());
    change.setReviewedAt(OffsetDateTime.now());
    SupplierChangeRequest saved = supplierChanges.save(change);
    log("SUPPLIER", change.getSupplierId(), decision, request.comment());
    return saved;
  }

  @Transactional(readOnly = true)
  public List<SupplierPerformanceReview> listReviews() {
    return reviews.findAllByOrderByReviewPeriodDescCreatedAtDesc();
  }

  @Transactional
  public SupplierPerformanceReview calculateReview(CalculateSupplierReview request) {
    requireSupplier(request.supplierId());
    YearMonth period;
    try {
      period = YearMonth.parse(request.reviewPeriod());
    } catch (Exception exception) {
      throw new BusinessException("考核期间格式应为 YYYY-MM");
    }
    LocalDate start = period.atDay(1);
    LocalDate end = period.atEndOfMonth();
    List<PurchaseOrder> supplierOrders = orders.findBySupplierId(request.supplierId()).stream()
        .filter(order -> order.getExpectedDeliveryDate() != null
            && !order.getExpectedDeliveryDate().isBefore(start)
            && !order.getExpectedDeliveryDate().isAfter(end))
        .toList();
    long onTime = supplierOrders.stream().filter(order -> {
      LocalDate completed = receipts.findByOrderId(order.getId()).stream()
          .filter(receipt -> !"PENDING".equals(receipt.getInspectionStatus()))
          .map(GoodsReceipt::getReceivedDate).max(LocalDate::compareTo).orElse(null);
      return completed != null && !completed.isAfter(order.getExpectedDeliveryDate());
    }).count();
    BigDecimal totalReceived = supplierOrders.stream()
        .flatMap(order -> receipts.findByOrderId(order.getId()).stream())
        .filter(receipt -> !"PENDING".equals(receipt.getInspectionStatus()))
        .map(GoodsReceipt::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal qualified = supplierOrders.stream()
        .flatMap(order -> receipts.findByOrderId(order.getId()).stream())
        .map(receipt -> value(receipt.getQualifiedQty())).reduce(BigDecimal.ZERO, BigDecimal::add);
    List<SupplierInvoice> supplierInvoices = invoices.findAllByOrderByInvoiceDateDesc().stream()
        .filter(invoice -> invoice.getSupplierId().equals(request.supplierId()))
        .filter(invoice -> !invoice.getInvoiceDate().isBefore(start) && !invoice.getInvoiceDate().isAfter(end))
        .toList();
    long matchedInvoices = supplierInvoices.stream()
        .filter(invoice -> "MATCHED".equals(invoice.getMatchStatus())).count();
    BigDecimal onTimeRate = rate(BigDecimal.valueOf(onTime), BigDecimal.valueOf(supplierOrders.size()));
    BigDecimal qualityRate = rate(qualified, totalReceived);
    BigDecimal invoiceRate = rate(BigDecimal.valueOf(matchedInvoices), BigDecimal.valueOf(supplierInvoices.size()));
    BigDecimal response = request.responseScore() == null ? BigDecimal.valueOf(100) : request.responseScore();
    BigDecimal total = onTimeRate.multiply(BigDecimal.valueOf(.35))
        .add(qualityRate.multiply(BigDecimal.valueOf(.35)))
        .add(invoiceRate.multiply(BigDecimal.valueOf(.2)))
        .add(response.multiply(BigDecimal.valueOf(.1))).setScale(2, RoundingMode.HALF_UP);
    SupplierPerformanceReview review = reviews.findBySupplierIdAndReviewPeriod(
        request.supplierId(), request.reviewPeriod()).orElse(new SupplierPerformanceReview());
    review.setSupplierId(request.supplierId());
    review.setReviewPeriod(request.reviewPeriod());
    review.setOnTimeRate(onTimeRate);
    review.setQualityRate(qualityRate);
    review.setInvoiceMatchRate(invoiceRate);
    review.setResponseScore(response);
    review.setTotalScore(total);
    review.setGrade(total.compareTo(BigDecimal.valueOf(90)) >= 0 ? "A"
        : total.compareTo(BigDecimal.valueOf(80)) >= 0 ? "B"
        : total.compareTo(BigDecimal.valueOf(70)) >= 0 ? "C" : "D");
    review.setReviewerName(currentName());
    review.setImprovementAction(request.improvementAction());
    SupplierPerformanceReview saved = reviews.save(review);
    log("SUPPLIER", request.supplierId(), "PERFORMANCE_REVIEW", request.reviewPeriod() + "：" + total);
    return saved;
  }

  @Transactional(readOnly = true)
  public List<ProcurementCollaborationEvent> listCollaborationEvents() {
    return collaborationEvents.findAllByOrderByEventDateDescCreatedAtDesc();
  }

  @Transactional
  public ProcurementCollaborationEvent createCollaborationEvent(CreateCollaborationEvent request) {
    requireSupplier(request.supplierId());
    if (request.orderId() != null) {
      PurchaseOrder order = orders.findById(request.orderId())
          .orElseThrow(() -> new BusinessException("采购订单不存在"));
      if (!order.getSupplierId().equals(request.supplierId())) {
        throw new BusinessException("协同事件供应商与订单不一致");
      }
    }
    ProcurementCollaborationEvent event = new ProcurementCollaborationEvent();
    event.setSupplierId(request.supplierId());
    event.setOrderId(request.orderId());
    event.setEventType(request.eventType());
    event.setReferenceNo(request.referenceNo());
    event.setEventDate(request.eventDate());
    event.setPromisedDate(request.promisedDate());
    event.setQuantity(request.quantity());
    event.setStatus(blank(request.status()) ? "OPEN" : request.status());
    event.setContent(request.content());
    event.setCreatedByName(currentName());
    ProcurementCollaborationEvent saved = collaborationEvents.save(event);
    log("SUPPLIER", request.supplierId(), request.eventType(), request.content());
    return saved;
  }

  @Transactional
  public PurchaseRequestResponse convertReplenishment(ConvertReplenishment request) {
    var part = parts.findById(request.partId()).orElseThrow(() -> new BusinessException("物料不存在"));
    PurchaseRequestResponse response = procurementService.createPurchaseRequest(new CreatePurchaseRequestRequest(
        null, currentName(), part.getId(), part.getName(), request.quantity(),
        request.unitPrice() == null ? part.getUnitCost() : request.unitPrice(),
        BigDecimal.valueOf(13), request.expectedDate(),
        blank(request.reason()) ? "库存补货建议自动转采购申请" : request.reason(),
        request.costType(), request.projectId(), request.departmentId()
    ));
    PurchaseRequest entity = purchaseRequests.findById(response.id()).orElseThrow();
    entity.setSourceType("REPLENISHMENT");
    entity.setSourceReference(part.getCode());
    purchaseRequests.save(entity);
    log("PURCHASE_REQUEST", entity.getId(), "REPLENISHMENT_CONVERTED", part.getCode());
    return response;
  }

  @Transactional(readOnly = true)
  public List<ProcurementActionLog> listActionLogs(String sourceType, UUID sourceId) {
    return actionLogs.findBySourceTypeAndSourceIdOrderByCreatedAtDesc(sourceType, sourceId);
  }

  private ProcurementContract requireContract(UUID id) {
    return contracts.findById(id).orElseThrow(() -> new BusinessException("采购合同不存在"));
  }

  private Supplier requireSupplier(UUID id) {
    return suppliers.findById(id).orElseThrow(() -> new BusinessException("供应商不存在"));
  }

  private String decision(String value) {
    String decision = value.toUpperCase();
    if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
      throw new BusinessException("审批结果只能为 APPROVED 或 REJECTED");
    }
    return decision;
  }

  private void log(String sourceType, UUID sourceId, String action, String details) {
    ProcurementActionLog log = new ProcurementActionLog();
    log.setSourceType(sourceType);
    log.setSourceId(sourceId);
    log.setActionType(action);
    log.setOperatorName(currentName());
    log.setDetails(details);
    actionLogs.save(log);
  }

  private static BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
    return denominator.signum() == 0 ? BigDecimal.ZERO
        : numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
  }

  private static BigDecimal value(BigDecimal number) {
    return number == null ? BigDecimal.ZERO : number;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }
}
