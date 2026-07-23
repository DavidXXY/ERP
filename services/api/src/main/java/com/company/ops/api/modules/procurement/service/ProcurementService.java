package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.GoodsReceipt;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementCostAllocation;
import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.domain.PurchaseRequest;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestApprovalRecord;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestStatus;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseRequestRequest;
import com.company.ops.api.modules.procurement.dto.CreateSupplierRequest;
import com.company.ops.api.modules.procurement.dto.GoodsReceiptResponse;
import com.company.ops.api.modules.procurement.dto.ProcessPurchaseRequestApprovalRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementCostAllocationResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementCostTargetOptionResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementCostTargetOptionsResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementMatchingResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPayableResponse;
import com.company.ops.api.modules.procurement.dto.PurchaseOrderResponse;
import com.company.ops.api.modules.procurement.dto.PurchaseRequestResponse;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderResult;
import com.company.ops.api.modules.procurement.dto.SupplierResponse;
import com.company.ops.api.modules.procurement.repository.GoodsReceiptRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementCostAllocationRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestApprovalRecordRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import com.company.ops.api.modules.system.security.UserPrincipal;
import static com.company.ops.api.common.util.MoneyUtils.amount;

@Service
public class ProcurementService {

  private final CodeGenerator codeGenerator;
  private final SupplierRepository supplierRepository;
  private final PurchaseRequestRepository requestRepository;
  private final PurchaseRequestApprovalRecordRepository requestApprovalRepository;
  private final PurchaseOrderRepository orderRepository;
  private final GoodsReceiptRepository receiptRepository;
  private final ProcurementPayableRepository payableRepository;
  private final ProcurementCostAllocationRepository costAllocationRepository;
  private final InventoryPartRepository partRepository;
  private final StockMovementRepository movementRepository;
  private final ProjectRepository projectRepository;
  private final ProjectCostEntryRepository projectCostRepository;
  private final SystemOrganizationRepository organizationRepository;

  public ProcurementService(
      CodeGenerator codeGenerator,
      SupplierRepository supplierRepository,
      PurchaseRequestRepository requestRepository,
      PurchaseRequestApprovalRecordRepository requestApprovalRepository,
      PurchaseOrderRepository orderRepository,
      GoodsReceiptRepository receiptRepository,
      ProcurementPayableRepository payableRepository,
      ProcurementCostAllocationRepository costAllocationRepository,
      InventoryPartRepository partRepository,
      StockMovementRepository movementRepository,
      ProjectRepository projectRepository,
      ProjectCostEntryRepository projectCostRepository,
      SystemOrganizationRepository organizationRepository
  ) {
    this.codeGenerator = codeGenerator;
    this.supplierRepository = supplierRepository;
    this.requestRepository = requestRepository;
    this.requestApprovalRepository = requestApprovalRepository;
    this.orderRepository = orderRepository;
    this.receiptRepository = receiptRepository;
    this.payableRepository = payableRepository;
    this.costAllocationRepository = costAllocationRepository;
    this.partRepository = partRepository;
    this.movementRepository = movementRepository;
    this.projectRepository = projectRepository;
    this.projectCostRepository = projectCostRepository;
    this.organizationRepository = organizationRepository;
  }

  @Transactional(readOnly = true)
  public List<SupplierResponse> listSuppliers() {
    Map<UUID, SupplierFinancialSummary> summaries = supplierFinancialSummaries();
    return supplierRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(supplier -> toSupplierResponse(supplier, summaries.get(supplier.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<SupplierResponse> listSuppliers(Pageable pageable) {
    Map<UUID, SupplierFinancialSummary> summaries = supplierFinancialSummaries();
    return supplierRepository.findAll(pageable)
        .map(supplier -> toSupplierResponse(supplier, summaries.get(supplier.getId())));
  }

  @Transactional
  public SupplierResponse createSupplier(CreateSupplierRequest request) {
    String supplierCode = request.code() != null ? request.code() : codeGenerator.generate("SUPPLIER");
    if (supplierRepository.existsByCode(supplierCode)) {
      throw new BusinessException("供应商编码已存在");
    }
    Supplier supplier = new Supplier();
    supplier.setCode(supplierCode);
    applySupplierRequest(supplier, request);
    return toSupplierResponse(supplierRepository.save(supplier), null);
  }

  @Transactional
  public SupplierResponse updateSupplier(UUID id, CreateSupplierRequest request) {
    Supplier supplier = supplierRepository.findById(id)
        .orElseThrow(() -> new BusinessException("供应商不存在"));
    applySupplierRequest(supplier, request);
    Supplier saved = supplierRepository.save(supplier);
    return toSupplierResponse(saved, supplierFinancialSummaries().get(saved.getId()));
  }

  private void applySupplierRequest(Supplier supplier, CreateSupplierRequest request) {
    supplier.setName(request.name());
    supplier.setCategory(request.category());
    supplier.setContactName(request.contactName());
    supplier.setPhone(request.phone());
    supplier.setSettlementTerms(request.settlementTerms());
    supplier.setLegalRepresentative(request.legalRepresentative());
    supplier.setUnifiedSocialCreditCode(request.unifiedSocialCreditCode());
    supplier.setRegisteredCapital(request.registeredCapital());
    supplier.setRegisteredAddress(request.registeredAddress());
    supplier.setBusinessScope(request.businessScope());
    supplier.setLicenseValidTo(request.licenseValidTo());
    supplier.setQualificationValidTo(request.qualificationValidTo());
    supplier.setTaxpayerType(request.taxpayerType());
    supplier.setBankName(request.bankName());
    supplier.setBankAccount(request.bankAccount());
    supplier.setAdmissionStatus(request.admissionStatus());
    supplier.setRemark(request.remark());
    supplier.setRiskStatus(request.riskStatus() == null ? SupplierRiskStatus.NORMAL : request.riskStatus());
  }

  @Transactional(readOnly = true)
  public ProcurementCostTargetOptionsResponse listCostTargets() {
    List<ProcurementCostTargetOptionResponse> projects = projectRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(project -> project.getApprovalStatus() == ProjectApprovalStatus.APPROVED)
        .filter(project -> project.getStage() != ProjectStage.CLOSED)
        .map(project -> new ProcurementCostTargetOptionResponse(
            project.getId(), project.getCode(), project.getName()
        ))
        .toList();
    List<ProcurementCostTargetOptionResponse> departments = organizationRepository
        .findByTenantIdOrderBySortOrderAsc("default").stream()
        .filter(SystemOrganization::isEnabled)
        .filter(organization -> "DEPARTMENT".equals(organization.getType()))
        .map(organization -> new ProcurementCostTargetOptionResponse(
            organization.getId(), organization.getCode(), organization.getName()
        ))
        .toList();
    return new ProcurementCostTargetOptionsResponse(projects, departments);
  }

  @Transactional(readOnly = true)
  public List<ProcurementCostAllocationResponse> listCostAllocations() {
    List<ProcurementCostAllocation> allocations = costAllocationRepository
        .findAllByOrderByIncurredDateDescCreatedAtDesc();
    Map<UUID, PurchaseOrder> orders = orderRepository.findAllById(
        allocations.stream().map(ProcurementCostAllocation::getOrderId).distinct().toList()
    ).stream().collect(Collectors.toMap(PurchaseOrder::getId, Function.identity()));
    Map<UUID, GoodsReceipt> receipts = receiptRepository.findAllById(
        allocations.stream().map(ProcurementCostAllocation::getReceiptId).distinct().toList()
    ).stream().collect(Collectors.toMap(GoodsReceipt::getId, Function.identity()));
    return allocations.stream()
        .map(item -> toCostAllocationResponse(
            item,
            orders.get(item.getOrderId()),
            receipts.get(item.getReceiptId())
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PurchaseRequestResponse> listPurchaseRequests() {
    return requestRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toPurchaseRequestResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<PurchaseRequestResponse> listPurchaseRequests(
      PurchaseRequestStatus status, ApprovalStatus approvalStatus,
      ProcurementCostType costType, String search, Pageable pageable) {
    return requestRepository.findByFilters(status, approvalStatus, costType, search, pageable)
        .map(this::toPurchaseRequestResponse);
  }

  @Transactional
  public PurchaseRequestResponse createPurchaseRequest(CreatePurchaseRequestRequest request) {
    String prCode = request.code() != null ? request.code() : codeGenerator.generate("PURCHASE_REQUEST");
    if (requestRepository.existsByCode(prCode)) {
      throw new BusinessException("采购申请编码已存在");
    }

    String partName = request.partName();
    if (request.partId() != null) {
      InventoryPart part = partRepository.findById(request.partId())
          .orElseThrow(() -> new BusinessException("物料不存在"));
      partName = part.getName();
    }
    if (!StringUtils.hasText(partName)) {
      throw new BusinessException("请选择物料或填写采购物料名称");
    }
    CostTarget costTarget = resolveCostTarget(
        request.costType(), request.projectId(), request.departmentId()
    );
    BigDecimal requestAmount = request.quantity().multiply(amount(request.unitPrice()));
    validateProjectBudget(request.costType(), request.projectId(), requestAmount, null);

    PurchaseRequest purchaseRequest = new PurchaseRequest();
    purchaseRequest.setCode(prCode);
    purchaseRequest.setRequesterName(currentName());
    purchaseRequest.setPartId(request.partId());
    purchaseRequest.setPartName(partName);
    purchaseRequest.setQuantity(request.quantity());
    purchaseRequest.setUnitPrice(amount(request.unitPrice()));
    purchaseRequest.setTaxRate(defaultTaxRate(request.taxRate()));
    purchaseRequest.setTotalAmount(requestAmount);
    purchaseRequest.setExpectedDate(request.expectedDate());
    purchaseRequest.setReason(request.reason());
    purchaseRequest.setCostType(request.costType());
    purchaseRequest.setProjectId(request.projectId());
    purchaseRequest.setDepartmentId(request.departmentId());
    purchaseRequest.setCostTargetCode(costTarget.code());
    purchaseRequest.setCostTargetName(costTarget.name());
    purchaseRequest.setStatus(PurchaseRequestStatus.SUBMITTED);
    purchaseRequest.setApprovalStatus(ApprovalStatus.PENDING);
    return toPurchaseRequestResponse(requestRepository.save(purchaseRequest));
  }

  @Transactional
  public PurchaseRequestResponse processRequestApproval(
      UUID id,
      ProcessPurchaseRequestApprovalRequest request
  ) {
    PurchaseRequest purchaseRequest = requestRepository.findById(id)
        .orElseThrow(() -> new BusinessException("采购申请不存在"));
    if (purchaseRequest.getApprovalStatus() != ApprovalStatus.PENDING) {
      throw new BusinessException("该采购申请已处理");
    }
    if (request.decision() == ApprovalStatus.PENDING) {
      throw new BusinessException("请选择通过或驳回");
    }

    purchaseRequest.setApprovalStatus(request.decision());
    purchaseRequest.setStatus(request.decision() == ApprovalStatus.APPROVED
        ? PurchaseRequestStatus.APPROVED
        : PurchaseRequestStatus.SUBMITTED);
    requestRepository.save(purchaseRequest);

    PurchaseRequestApprovalRecord record = new PurchaseRequestApprovalRecord();
    record.setRequestId(purchaseRequest.getId());
    record.setDecision(request.decision());
    record.setComment(request.comment());
    record.setApproverName(currentName());
    record.setDecidedAt(OffsetDateTime.now());
    requestApprovalRepository.save(record);
    return toPurchaseRequestResponse(purchaseRequest);
  }

  @Transactional
  public PurchaseRequestResponse updatePurchaseRequest(
      UUID id, CreatePurchaseRequestRequest request) {
    PurchaseRequest purchaseRequest = requestRepository.findById(id)
        .orElseThrow(() -> new BusinessException("采购申请不存在"));
    if (purchaseRequest.getApprovalStatus() == ApprovalStatus.APPROVED
        || purchaseRequest.getStatus() == PurchaseRequestStatus.RECEIVED
        || purchaseRequest.getStatus() == PurchaseRequestStatus.CANCELLED) {
      throw new BusinessException("该申请当前状态不可编辑");
    }
    purchaseRequest.setRequesterName(currentName());
    purchaseRequest.setPartId(request.partId());
    if (request.partId() != null) {
      InventoryPart part = partRepository.findById(request.partId())
          .orElseThrow(() -> new BusinessException("物料不存在"));
      purchaseRequest.setPartName(part.getName());
    } else {
      purchaseRequest.setPartName(request.partName());
    }
    purchaseRequest.setQuantity(request.quantity());
    purchaseRequest.setUnitPrice(amount(request.unitPrice()));
    purchaseRequest.setTaxRate(defaultTaxRate(request.taxRate()));
    purchaseRequest.setTotalAmount(request.quantity().multiply(amount(request.unitPrice())));
    purchaseRequest.setExpectedDate(request.expectedDate());
    purchaseRequest.setReason(request.reason());
    purchaseRequest.setCostType(request.costType());
    purchaseRequest.setProjectId(request.projectId());
    purchaseRequest.setDepartmentId(request.departmentId());
    CostTarget costTarget = resolveCostTarget(
        request.costType(), request.projectId(), request.departmentId());
    validateProjectBudget(
        request.costType(), request.projectId(), purchaseRequest.getTotalAmount(), purchaseRequest.getId());
    purchaseRequest.setCostTargetCode(costTarget.code());
    purchaseRequest.setCostTargetName(costTarget.name());
    // If was rejected, reset to PENDING for re-approval
    if (purchaseRequest.getApprovalStatus() == ApprovalStatus.REJECTED) {
      purchaseRequest.setApprovalStatus(ApprovalStatus.PENDING);
    }
    return toPurchaseRequestResponse(requestRepository.save(purchaseRequest));
  }

  @Transactional(readOnly = true)
  public List<PurchaseOrderResponse> listPurchaseOrders() {
    List<PurchaseOrder> orders = orderRepository.findAllByOrderByCreatedAtDesc();
    return mapOrders(orders);
  }

  @Transactional(readOnly = true)
  public Page<PurchaseOrderResponse> listPurchaseOrders(
      PurchaseOrderStatus status, ProcurementCostType costType,
      String search, Pageable pageable) {
    Page<PurchaseOrder> page = orderRepository.findByFilters(status, costType, search, pageable);
    // Batch-load suppliers
    Map<UUID, Supplier> supplierMap = supplierRepository.findAllById(
        page.getContent().stream().map(PurchaseOrder::getSupplierId).distinct().toList()
    ).stream().collect(Collectors.toMap(Supplier::getId, Function.identity()));
    Map<UUID, PurchaseRequest> requestMap = requestRepository.findAllById(
        page.getContent().stream().map(PurchaseOrder::getRequestId).filter(java.util.Objects::nonNull).distinct().toList()
    ).stream().collect(Collectors.toMap(PurchaseRequest::getId, Function.identity()));
    return page.map(order -> toPurchaseOrderResponse(order, supplierMap.get(order.getSupplierId()), requestMap.get(order.getRequestId())));
  }

  @Transactional
  public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request) {
    if (orderRepository.existsByCode(request.code())) {
      throw new BusinessException("采购订单编码已存在");
    }
    if (orderRepository.existsByRequestId(request.requestId())) {
      throw new BusinessException("该采购申请已生成订单");
    }
    Supplier supplier = supplierRepository.findById(request.supplierId())
        .orElseThrow(() -> new BusinessException("供应商不存在"));
    if (supplier.getRiskStatus() == SupplierRiskStatus.BLOCKED) {
      throw new BusinessException("该供应商已停用，不能下单");
    }

    PurchaseRequest purchaseRequest = requestRepository.findById(request.requestId())
        .orElseThrow(() -> new BusinessException("采购申请不存在"));
    if (purchaseRequest.getApprovalStatus() != ApprovalStatus.APPROVED
        || purchaseRequest.getStatus() != PurchaseRequestStatus.APPROVED) {
      throw new BusinessException("采购申请审批通过后才能下单");
    }
    if (purchaseRequest.getPartId() == null) {
      throw new BusinessException("采购申请未关联物料，请先建立物料档案");
    }
    partRepository.findById(purchaseRequest.getPartId())
        .orElseThrow(() -> new BusinessException("关联物料不存在"));

    PurchaseOrder order = new PurchaseOrder();
    order.setCode(request.code());
    order.setSupplierId(request.supplierId());
    order.setRequestId(request.requestId());
    order.setPartId(purchaseRequest.getPartId());
    order.setPartName(purchaseRequest.getPartName());
    order.setOrderedQty(purchaseRequest.getQuantity());
    order.setReceivedQty(BigDecimal.ZERO);
    order.setUnitPrice(request.unitPrice());
    order.setTaxRate(defaultTaxRate(request.taxRate() == null ? purchaseRequest.getTaxRate() : request.taxRate()));
    order.setOrderAmount(purchaseRequest.getQuantity().multiply(request.unitPrice()));
    order.setExpectedDeliveryDate(request.expectedDeliveryDate() == null
        ? purchaseRequest.getExpectedDate()
        : request.expectedDeliveryDate());
    order.setCostType(purchaseRequest.getCostType());
    order.setProjectId(purchaseRequest.getProjectId());
    order.setDepartmentId(purchaseRequest.getDepartmentId());
    order.setCostTargetCode(purchaseRequest.getCostTargetCode());
    order.setCostTargetName(purchaseRequest.getCostTargetName());
    order.setStatus(PurchaseOrderStatus.DRAFT);
    order.setApprovalStatus(ApprovalStatus.PENDING);

    PurchaseOrder saved = orderRepository.save(order);
    return toPurchaseOrderResponse(saved, supplier, purchaseRequest);
  }

  @Transactional
  public PurchaseOrderResponse cancelPurchaseOrder(UUID id) {
    PurchaseOrder order = orderRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() != PurchaseOrderStatus.ORDERED) {
      throw new BusinessException("只有已下单的订单才能取消");
    }
    order.setStatus(PurchaseOrderStatus.CANCELLED);
    PurchaseOrder saved = orderRepository.save(order);
    // Revert the associated purchase request back to APPROVED
    if (order.getRequestId() != null) {
      PurchaseRequest pr = requestRepository.findById(order.getRequestId()).orElse(null);
      if (pr != null && pr.getStatus() == PurchaseRequestStatus.ORDERED) {
        pr.setStatus(PurchaseRequestStatus.APPROVED);
        requestRepository.save(pr);
      }
    }
    Supplier supplier = supplierRepository.findById(order.getSupplierId()).orElse(null);
    PurchaseRequest purchaseRequest = order.getRequestId() != null
        ? requestRepository.findById(order.getRequestId()).orElse(null) : null;
    return toPurchaseOrderResponse(saved, supplier, purchaseRequest);
  }

  @Transactional
  public PurchaseOrderResponse submitPurchaseOrder(UUID id) {
    PurchaseOrder order = orderRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() != PurchaseOrderStatus.DRAFT) throw new BusinessException("只有草稿订单可以提交审批");
    order.setApprovalStatus(ApprovalStatus.PENDING);
    Supplier supplier = supplierRepository.findById(order.getSupplierId()).orElse(null);
    PurchaseRequest purchaseRequest = requestRepository.findById(order.getRequestId()).orElse(null);
    return toPurchaseOrderResponse(orderRepository.save(order), supplier, purchaseRequest);
  }

  @Transactional
  public PurchaseOrderResponse approvePurchaseOrder(UUID id, ApprovalStatus decision, String approverName, String comment) {
    PurchaseOrder order = orderRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() != PurchaseOrderStatus.DRAFT || order.getApprovalStatus() != ApprovalStatus.PENDING)
      throw new BusinessException("该订单当前不可审批");
    if (decision == ApprovalStatus.PENDING) throw new BusinessException("请选择通过或驳回");
    order.setApprovalStatus(decision); order.setApproverName(currentName()); order.setApprovalComment(comment); order.setApprovedAt(OffsetDateTime.now());
    if (decision == ApprovalStatus.APPROVED) {
      order.setStatus(PurchaseOrderStatus.ORDERED);
      requestRepository.findById(order.getRequestId()).ifPresent(pr -> {pr.setStatus(PurchaseRequestStatus.ORDERED); requestRepository.save(pr);});
    }
    Supplier supplier = supplierRepository.findById(order.getSupplierId()).orElse(null);
    PurchaseRequest purchaseRequest = requestRepository.findById(order.getRequestId()).orElse(null);
    return toPurchaseOrderResponse(orderRepository.save(order), supplier, purchaseRequest);
  }

  @Transactional
  public ReceivePurchaseOrderResult receiveOrder(UUID id, ReceivePurchaseOrderRequest request) {
    PurchaseOrder order = orderRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() != PurchaseOrderStatus.ORDERED
        && order.getStatus() != PurchaseOrderStatus.PARTIAL_RECEIVED) {
      throw new BusinessException("当前订单状态不能收货");
    }
    if (order.getPartId() == null) {
      throw new BusinessException("订单未关联物料，不能入库");
    }
    validateOrderCostTarget(order);
    BigDecimal currentReceived = amount(order.getReceivedQty());
    BigDecimal nextReceived = currentReceived.add(request.quantity());
    if (nextReceived.compareTo(amount(order.getOrderedQty())) > 0) {
      throw new BusinessException("本次收货数量超过订单剩余数量");
    }

    InventoryPart part = partRepository.findById(order.getPartId())
        .orElseThrow(() -> new BusinessException("关联物料不存在"));
    BigDecimal stockBefore = amount(part.getStockQty());
    BigDecimal stockAfter = stockBefore.add(request.quantity());
    BigDecimal inventoryValue = stockBefore.multiply(amount(part.getUnitCost()))
        .add(request.quantity().multiply(amount(order.getUnitPrice())));
    part.setStockQty(stockAfter);
    if (stockAfter.compareTo(BigDecimal.ZERO) > 0) {
      part.setUnitCost(inventoryValue.divide(stockAfter, 2, RoundingMode.HALF_UP));
    }
    partRepository.save(part);

    long sequence = receiptRepository.countByOrderId(order.getId()) + 1;
    String receiptCode = "RK-" + order.getCode() + "-" + String.format("%02d", sequence);
    BigDecimal receiptAmount = request.quantity().multiply(amount(order.getUnitPrice()));
    GoodsReceipt receipt = new GoodsReceipt();
    receipt.setCode(receiptCode);
    receipt.setOrderId(order.getId());
    receipt.setPartId(part.getId());
    receipt.setQuantity(request.quantity());
    receipt.setUnitPrice(order.getUnitPrice());
    receipt.setTaxRate(defaultTaxRate(order.getTaxRate()));
    receipt.setAmount(receiptAmount);
    receipt.setReceivedDate(request.receivedDate());
    receipt.setDeliveryNo(request.deliveryNo());
    receipt.setReceiverName(currentName());
    GoodsReceipt savedReceipt = receiptRepository.save(receipt);

    StockMovement movement = new StockMovement();
    movement.setPartId(part.getId());
    movement.setMovementType(StockMovementType.INBOUND);
    movement.setQuantity(request.quantity());
    movement.setSourceNo(order.getCode());
    movement.setRemark("采购收货 " + receiptCode + "，送货单 " + request.deliveryNo());
    movementRepository.save(movement);

    order.setReceivedQty(nextReceived);
    boolean fullyReceived = nextReceived.compareTo(amount(order.getOrderedQty())) == 0;
    order.setStatus(fullyReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIAL_RECEIVED);
    PurchaseOrder savedOrder = orderRepository.save(order);

    PurchaseRequest purchaseRequest = requestRepository.findById(order.getRequestId()).orElse(null);
    if (purchaseRequest != null && fullyReceived) {
      purchaseRequest.setStatus(PurchaseRequestStatus.RECEIVED);
      requestRepository.save(purchaseRequest);
    }

    Supplier supplier = supplierRepository.findById(order.getSupplierId())
        .orElseThrow(() -> new BusinessException("供应商不存在"));
    ProcurementPayable payable = new ProcurementPayable();
    payable.setCode("YF-" + order.getCode() + "-" + String.format("%02d", sequence));
    payable.setSupplierId(supplier.getId());
    payable.setOrderId(order.getId());
    payable.setReceiptId(savedReceipt.getId());
    payable.setAmount(receiptAmount);
    payable.setTaxRate(defaultTaxRate(order.getTaxRate()));
    payable.setPaidAmount(BigDecimal.ZERO);
    payable.setDueDate(request.payableDueDate());
    payable.setStatus(PayableStatus.PENDING);
    ProcurementPayable savedPayable = payableRepository.save(payable);
    ProcurementCostAllocation savedAllocation = postCostAllocation(
        savedOrder, savedReceipt, receiptAmount, request.receivedDate()
    );

    return new ReceivePurchaseOrderResult(
        toPurchaseOrderResponse(savedOrder, supplier, purchaseRequest),
        toGoodsReceiptResponse(savedReceipt, savedOrder, part),
        toPayableResponse(savedPayable, supplier, savedOrder),
        toCostAllocationResponse(savedAllocation, savedOrder, savedReceipt),
        stockAfter
    );
  }

  @Transactional(readOnly = true)
  public List<GoodsReceiptResponse> listGoodsReceipts() {
    List<GoodsReceipt> receipts = receiptRepository.findAllByOrderByReceivedDateDesc();
    Map<UUID, PurchaseOrder> orders = orderRepository.findAllById(
        receipts.stream().map(GoodsReceipt::getOrderId).distinct().toList()
    ).stream().collect(Collectors.toMap(PurchaseOrder::getId, Function.identity()));
    Map<UUID, InventoryPart> parts = partRepository.findAllById(
        receipts.stream().map(GoodsReceipt::getPartId).distinct().toList()
    ).stream().collect(Collectors.toMap(InventoryPart::getId, Function.identity()));
    return receipts.stream()
        .map(item -> toGoodsReceiptResponse(item, orders.get(item.getOrderId()), parts.get(item.getPartId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ProcurementPayableResponse> listPayables() {
    List<ProcurementPayable> payables = payableRepository.findAllByOrderByDueDateAsc();
    Map<UUID, Supplier> suppliers = supplierRepository.findAllById(
        payables.stream().map(ProcurementPayable::getSupplierId).distinct().toList()
    ).stream().collect(Collectors.toMap(Supplier::getId, Function.identity()));
    Map<UUID, PurchaseOrder> orders = orderRepository.findAllById(
        payables.stream().map(ProcurementPayable::getOrderId).distinct().toList()
    ).stream().collect(Collectors.toMap(PurchaseOrder::getId, Function.identity()));
    return payables.stream()
        .map(item -> toPayableResponse(item, suppliers.get(item.getSupplierId()), orders.get(item.getOrderId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ProcurementMatchingResponse> matching() {
    List<PurchaseOrder> orders = orderRepository.findAllByOrderByCreatedAtDesc();
    Map<UUID, Supplier> suppliers = supplierRepository.findAllById(
        orders.stream().map(PurchaseOrder::getSupplierId).distinct().toList()
    ).stream().collect(Collectors.toMap(Supplier::getId, Function.identity()));
    Map<UUID, List<GoodsReceipt>> receiptsByOrder = receiptRepository.findAll().stream()
        .collect(Collectors.groupingBy(GoodsReceipt::getOrderId));
    Map<UUID, List<ProcurementPayable>> payablesByOrder = payableRepository.findAll().stream()
        .collect(Collectors.groupingBy(ProcurementPayable::getOrderId));
    return orders.stream()
        .map(order -> toMatchingResponse(
            order,
            suppliers.get(order.getSupplierId()),
            receiptsByOrder.getOrDefault(order.getId(), List.of()),
            payablesByOrder.getOrDefault(order.getId(), List.of())
        ))
        .toList();
  }

  private List<PurchaseOrderResponse> mapOrders(List<PurchaseOrder> orders) {
    Map<UUID, Supplier> suppliers = supplierRepository.findAllById(
        orders.stream().map(PurchaseOrder::getSupplierId).distinct().toList()
    ).stream().collect(Collectors.toMap(Supplier::getId, Function.identity()));
    Map<UUID, PurchaseRequest> requests = requestRepository.findAllById(
        orders.stream().map(PurchaseOrder::getRequestId).filter(id -> id != null).distinct().toList()
    ).stream().collect(Collectors.toMap(PurchaseRequest::getId, Function.identity()));
    return orders.stream()
        .map(order -> toPurchaseOrderResponse(
            order,
            suppliers.get(order.getSupplierId()),
            order.getRequestId() == null ? null : requests.get(order.getRequestId())
        ))
        .toList();
  }

  private SupplierResponse toSupplierResponse(Supplier supplier, SupplierFinancialSummary summary) {
    SupplierFinancialSummary financials = summary == null ? SupplierFinancialSummary.ZERO : summary;
    return new SupplierResponse(
        supplier.getId(),
        supplier.getCode(),
        supplier.getName(),
        supplier.getCategory(),
        supplier.getContactName(),
        supplier.getPhone(),
        supplier.getSettlementTerms(),
        supplier.getLegalRepresentative(),
        supplier.getUnifiedSocialCreditCode(),
        supplier.getRegisteredCapital(),
        supplier.getRegisteredAddress(),
        supplier.getBusinessScope(),
        supplier.getLicenseValidTo(),
        supplier.getQualificationValidTo(),
        supplier.getTaxpayerType(),
        supplier.getBankName(),
        supplier.getBankAccount(),
        supplier.getAdmissionStatus(),
        supplier.getRemark(),
        supplier.getRiskStatus(),
        financials.contractedAmount(),
        financials.payableAmount(),
        financials.paidAmount(),
        financials.outstandingAmount()
    );
  }

  private Map<UUID, SupplierFinancialSummary> supplierFinancialSummaries() {
    Map<UUID, BigDecimal> contractedAmounts = orderRepository.findAll().stream()
        .filter(order -> order.getStatus() != PurchaseOrderStatus.CANCELLED)
        .collect(Collectors.groupingBy(
            PurchaseOrder::getSupplierId,
            Collectors.reducing(BigDecimal.ZERO, order -> amount(order.getOrderAmount()), BigDecimal::add)
        ));
    Map<UUID, List<ProcurementPayable>> payablesBySupplier = payableRepository.findAll().stream()
        .filter(payable -> payable.getStatus() != PayableStatus.CANCELLED)
        .collect(Collectors.groupingBy(ProcurementPayable::getSupplierId));
    return supplierRepository.findAll().stream().collect(Collectors.toMap(
        Supplier::getId,
        supplier -> {
          List<ProcurementPayable> payables = payablesBySupplier.getOrDefault(supplier.getId(), List.of());
          BigDecimal payableAmount = payables.stream()
              .map(ProcurementPayable::getAmount).map(this::amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal paidAmount = payables.stream()
              .map(ProcurementPayable::getPaidAmount).map(this::amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          return new SupplierFinancialSummary(
              contractedAmounts.getOrDefault(supplier.getId(), BigDecimal.ZERO),
              payableAmount,
              paidAmount,
              payableAmount.subtract(paidAmount)
          );
        }
    ));
  }

  private record SupplierFinancialSummary(
      BigDecimal contractedAmount,
      BigDecimal payableAmount,
      BigDecimal paidAmount,
      BigDecimal outstandingAmount
  ) {
    private static final SupplierFinancialSummary ZERO = new SupplierFinancialSummary(
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
    );
  }

  private ProcurementMatchingResponse toMatchingResponse(
      PurchaseOrder order,
      Supplier supplier,
      List<GoodsReceipt> receipts,
      List<ProcurementPayable> payables
  ) {
    BigDecimal receiptAmount = receipts.stream().map(GoodsReceipt::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal payableAmount = payables.stream().map(ProcurementPayable::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal paidAmount = payables.stream().map(ProcurementPayable::getPaidAmount).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal orderedQty = amount(order.getOrderedQty());
    BigDecimal receivedQty = amount(order.getReceivedQty());
    BigDecimal orderAmount = amount(order.getOrderAmount());
    String status;
    String risk;
    if (order.getStatus() == PurchaseOrderStatus.CANCELLED) {
      status = "CANCELLED"; risk = "订单已取消";
    } else if (receivedQty.compareTo(orderedQty) < 0) {
      status = "RECEIVING"; risk = "尚未收齐";
    } else if (payableAmount.compareTo(receiptAmount) < 0) {
      status = "PAYABLE_MISSING"; risk = "应付金额少于入库金额";
    } else if (payableAmount.compareTo(receiptAmount) > 0) {
      status = "AMOUNT_MISMATCH"; risk = "应付金额超过入库金额";
    } else if (receiptAmount.compareTo(orderAmount) != 0) {
      status = "AMOUNT_MISMATCH"; risk = "入库金额与订单金额不一致";
    } else {
      status = "MATCHED"; risk = "三单一致";
    }
    return new ProcurementMatchingResponse(
        order.getId(), order.getCode(), supplier == null ? null : supplier.getName(), order.getPartName(),
        orderedQty, receivedQty, orderAmount, receiptAmount, payableAmount, paidAmount, status, risk
    );
  }

  private PurchaseRequestResponse toPurchaseRequestResponse(PurchaseRequest request) {
    PurchaseRequestApprovalRecord approval = requestApprovalRepository
        .findFirstByRequestIdOrderByDecidedAtDesc(request.getId())
        .orElse(null);
    return new PurchaseRequestResponse(
        request.getId(),
        request.getCode(),
        request.getRequesterName(),
        request.getPartId(),
        request.getPartName(),
        request.getQuantity(),
        amount(request.getUnitPrice()),
        defaultTaxRate(request.getTaxRate()),
        amount(request.getTotalAmount()),
        request.getExpectedDate(),
        request.getReason(),
        request.getCostType(),
        costTargetId(request.getCostType(), request.getProjectId(), request.getDepartmentId()),
        request.getCostTargetCode(),
        request.getCostTargetName(),
        request.getStatus(),
        request.getApprovalStatus(),
        approval == null ? null : approval.getComment(),
        approval == null ? null : approval.getApproverName(),
        approval == null ? null : approval.getDecidedAt()
    );
  }

  private PurchaseOrderResponse toPurchaseOrderResponse(
      PurchaseOrder order,
      Supplier supplier,
      PurchaseRequest request
  ) {
    return new PurchaseOrderResponse(
        order.getId(),
        order.getCode(),
        order.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        order.getRequestId(),
        request == null ? null : request.getCode(),
        order.getPartId(),
        order.getPartName(),
        amount(order.getOrderedQty()),
        amount(order.getReceivedQty()),
        amount(order.getUnitPrice()),
        defaultTaxRate(order.getTaxRate()),
        amount(order.getOrderAmount()),
        order.getExpectedDeliveryDate(),
        order.getCostType(),
        costTargetId(order.getCostType(), order.getProjectId(), order.getDepartmentId()),
        order.getCostTargetCode(),
        order.getCostTargetName(),
        order.getStatus(), order.getApprovalStatus(), order.getApprovalComment(), order.getApproverName(), order.getApprovedAt()
    );
  }

  private GoodsReceiptResponse toGoodsReceiptResponse(
      GoodsReceipt receipt,
      PurchaseOrder order,
      InventoryPart part
  ) {
    return new GoodsReceiptResponse(
        receipt.getId(),
        receipt.getCode(),
        receipt.getOrderId(),
        order == null ? null : order.getCode(),
        receipt.getPartId(),
        part == null ? null : part.getName(),
        receipt.getQuantity(),
        receipt.getUnitPrice(),
        defaultTaxRate(receipt.getTaxRate()),
        receipt.getAmount(),
        receipt.getReceivedDate(),
        receipt.getDeliveryNo(),
        receipt.getReceiverName(),
        order == null ? null : order.getCostType(),
        order == null ? null : costTargetId(
            order.getCostType(), order.getProjectId(), order.getDepartmentId()
        ),
        order == null ? null : order.getCostTargetCode(),
        order == null ? null : order.getCostTargetName(),
        receipt.getInspectionStatus(), receipt.getQualifiedQty(), receipt.getRejectedQty(), receipt.getInspectorName(), receipt.getInspectionComment(), receipt.getInspectedAt()
    );
  }

  private ProcurementPayableResponse toPayableResponse(
      ProcurementPayable payable,
      Supplier supplier,
      PurchaseOrder order
  ) {
    BigDecimal outstanding = amount(payable.getAmount()).subtract(amount(payable.getPaidAmount()));
    return new ProcurementPayableResponse(
        payable.getId(),
        payable.getCode(),
        payable.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        payable.getOrderId(),
        order == null ? null : order.getCode(),
        payable.getReceiptId(),
        amount(payable.getAmount()),
        defaultTaxRate(payable.getTaxRate()),
        amount(payable.getPaidAmount()),
        outstanding,
        payable.getDueDate(),
        order == null ? null : order.getCostType(),
        order == null ? null : costTargetId(
            order.getCostType(), order.getProjectId(), order.getDepartmentId()
        ),
        order == null ? null : order.getCostTargetCode(),
        order == null ? null : order.getCostTargetName(),
        payable.getStatus()
    );
  }

  private ProcurementCostAllocationResponse toCostAllocationResponse(
      ProcurementCostAllocation allocation,
      PurchaseOrder order,
      GoodsReceipt receipt
  ) {
    return new ProcurementCostAllocationResponse(
        allocation.getId(),
        allocation.getOrderId(),
        order == null ? null : order.getCode(),
        allocation.getReceiptId(),
        receipt == null ? null : receipt.getCode(),
        allocation.getCostType(),
        costTargetId(
            allocation.getCostType(), allocation.getProjectId(), allocation.getDepartmentId()
        ),
        allocation.getTargetCode(),
        allocation.getTargetName(),
        allocation.getPartName(),
        amount(allocation.getAmount()),
        allocation.getIncurredDate()
    );
  }

  private CostTarget resolveCostTarget(
      ProcurementCostType costType,
      UUID projectId,
      UUID departmentId
  ) {
    if (costType == null) {
      throw new BusinessException("请选择采购成本归属类型");
    }
    if (costType == ProcurementCostType.PROJECT) {
      if (projectId == null) {
        throw new BusinessException("项目采购必须关联项目");
      }
      if (departmentId != null) {
        throw new BusinessException("项目采购不能同时关联部门");
      }
      Project project = projectRepository.findById(projectId)
          .orElseThrow(() -> new BusinessException("关联项目不存在"));
      if (project.getApprovalStatus() != ProjectApprovalStatus.APPROVED
          || project.getStage() == ProjectStage.CLOSED) {
        throw new BusinessException("只能选择已审批且未关闭的项目");
      }
      return new CostTarget(project.getCode(), project.getName());
    }
    if (departmentId == null) {
      throw new BusinessException("部门采购必须关联成本部门");
    }
    if (projectId != null) {
      throw new BusinessException("部门采购不能同时关联项目");
    }
    SystemOrganization department = organizationRepository.findById(departmentId)
        .orElseThrow(() -> new BusinessException("成本部门不存在"));
    if (!department.isEnabled() || !"DEPARTMENT".equals(department.getType())) {
      throw new BusinessException("请选择有效的部门作为成本归属");
    }
    return new CostTarget(department.getCode(), department.getName());
  }

  private void validateProjectBudget(
      ProcurementCostType costType, UUID projectId, BigDecimal requestedAmount, UUID excludedRequestId
  ) {
    if (costType != ProcurementCostType.PROJECT || projectId == null) return;
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new BusinessException("关联项目不存在"));
    BigDecimal occupied = requestRepository.findAll().stream()
        .filter(item -> item.getProjectId() != null && item.getProjectId().equals(projectId))
        .filter(item -> excludedRequestId == null || !item.getId().equals(excludedRequestId))
        .filter(item -> item.getStatus() != PurchaseRequestStatus.CANCELLED)
        .filter(item -> item.getApprovalStatus() != ApprovalStatus.REJECTED)
        .map(item -> amount(item.getTotalAmount()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal available = amount(project.getBudgetAmount()).subtract(occupied);
    if (requestedAmount.compareTo(available) > 0) {
      throw new BusinessException(
          "项目预算不足：剩余可申请 " + available + "，本次申请 " + requestedAmount + "，请调整金额或发起超预算审批");
    }
  }

  private void validateOrderCostTarget(PurchaseOrder order) {
    if (order.getCostType() == ProcurementCostType.PROJECT) {
      if (order.getProjectId() == null) {
        throw new BusinessException("采购订单缺少关联项目，不能收货");
      }
      Project project = projectRepository.findById(order.getProjectId())
          .orElseThrow(() -> new BusinessException("采购订单关联项目不存在"));
      if (project.getApprovalStatus() != ProjectApprovalStatus.APPROVED
          || project.getStage() == ProjectStage.CLOSED) {
        throw new BusinessException("关联项目已关闭或未审批，不能归集采购成本");
      }
      return;
    }
    if (order.getCostType() != ProcurementCostType.DEPARTMENT || order.getDepartmentId() == null) {
      throw new BusinessException("采购订单缺少成本部门，不能收货");
    }
  }

  private ProcurementCostAllocation postCostAllocation(
      PurchaseOrder order,
      GoodsReceipt receipt,
      BigDecimal receiptAmount,
      java.time.LocalDate incurredDate
  ) {
    ProcurementCostAllocation allocation = new ProcurementCostAllocation();
    allocation.setOrderId(order.getId());
    allocation.setReceiptId(receipt.getId());
    allocation.setCostType(order.getCostType());
    allocation.setProjectId(order.getProjectId());
    allocation.setDepartmentId(order.getDepartmentId());
    allocation.setTargetCode(order.getCostTargetCode());
    allocation.setTargetName(order.getCostTargetName());
    allocation.setPartName(order.getPartName());
    allocation.setAmount(receiptAmount);
    allocation.setIncurredDate(incurredDate);
    ProcurementCostAllocation saved = costAllocationRepository.save(allocation);

    if (order.getCostType() == ProcurementCostType.PROJECT) {
      Project project = projectRepository.findById(order.getProjectId())
          .orElseThrow(() -> new BusinessException("采购订单关联项目不存在"));
      ProjectCostEntry entry = new ProjectCostEntry();
      entry.setProjectId(project.getId());
      entry.setCategory(ProjectCostCategory.MATERIAL);
      entry.setSourceType(ProjectCostSource.PROCUREMENT);
      entry.setSourceNo(receipt.getCode());
      entry.setDescription("采购入库：" + order.getPartName());
      entry.setAmount(receiptAmount);
      entry.setIncurredDate(incurredDate);
      projectCostRepository.save(entry);
      project.setActualCost(amount(project.getActualCost()).add(receiptAmount));
      projectRepository.save(project);
    }
    return saved;
  }

  private UUID costTargetId(
      ProcurementCostType costType,
      UUID projectId,
      UUID departmentId
  ) {
    return costType == ProcurementCostType.PROJECT ? projectId : departmentId;
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal defaultTaxRate(BigDecimal value) {
    return value == null ? BigDecimal.valueOf(13) : value;
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }

  private record CostTarget(String code, String name) {}
}
