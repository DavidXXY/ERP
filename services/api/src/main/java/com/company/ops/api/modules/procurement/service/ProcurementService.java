package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.GoodsReceipt;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
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
import com.company.ops.api.modules.procurement.dto.ProcurementPayableResponse;
import com.company.ops.api.modules.procurement.dto.PurchaseOrderResponse;
import com.company.ops.api.modules.procurement.dto.PurchaseRequestResponse;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderResult;
import com.company.ops.api.modules.procurement.dto.SupplierResponse;
import com.company.ops.api.modules.procurement.repository.GoodsReceiptRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestApprovalRecordRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
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
import org.springframework.util.StringUtils;

@Service
public class ProcurementService {

  private final SupplierRepository supplierRepository;
  private final PurchaseRequestRepository requestRepository;
  private final PurchaseRequestApprovalRecordRepository requestApprovalRepository;
  private final PurchaseOrderRepository orderRepository;
  private final GoodsReceiptRepository receiptRepository;
  private final ProcurementPayableRepository payableRepository;
  private final InventoryPartRepository partRepository;
  private final StockMovementRepository movementRepository;

  public ProcurementService(
      SupplierRepository supplierRepository,
      PurchaseRequestRepository requestRepository,
      PurchaseRequestApprovalRecordRepository requestApprovalRepository,
      PurchaseOrderRepository orderRepository,
      GoodsReceiptRepository receiptRepository,
      ProcurementPayableRepository payableRepository,
      InventoryPartRepository partRepository,
      StockMovementRepository movementRepository
  ) {
    this.supplierRepository = supplierRepository;
    this.requestRepository = requestRepository;
    this.requestApprovalRepository = requestApprovalRepository;
    this.orderRepository = orderRepository;
    this.receiptRepository = receiptRepository;
    this.payableRepository = payableRepository;
    this.partRepository = partRepository;
    this.movementRepository = movementRepository;
  }

  @Transactional(readOnly = true)
  public List<SupplierResponse> listSuppliers() {
    return supplierRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toSupplierResponse)
        .toList();
  }

  @Transactional
  public SupplierResponse createSupplier(CreateSupplierRequest request) {
    if (supplierRepository.existsByCode(request.code())) {
      throw new BusinessException("供应商编码已存在");
    }
    Supplier supplier = new Supplier();
    supplier.setCode(request.code());
    supplier.setName(request.name());
    supplier.setCategory(request.category());
    supplier.setContactName(request.contactName());
    supplier.setPhone(request.phone());
    supplier.setSettlementTerms(request.settlementTerms());
    supplier.setRiskStatus(request.riskStatus() == null ? SupplierRiskStatus.NORMAL : request.riskStatus());
    return toSupplierResponse(supplierRepository.save(supplier));
  }

  @Transactional(readOnly = true)
  public List<PurchaseRequestResponse> listPurchaseRequests() {
    return requestRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toPurchaseRequestResponse)
        .toList();
  }

  @Transactional
  public PurchaseRequestResponse createPurchaseRequest(CreatePurchaseRequestRequest request) {
    if (requestRepository.existsByCode(request.code())) {
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

    PurchaseRequest purchaseRequest = new PurchaseRequest();
    purchaseRequest.setCode(request.code());
    purchaseRequest.setRequesterName(request.requesterName());
    purchaseRequest.setPartId(request.partId());
    purchaseRequest.setPartName(partName);
    purchaseRequest.setQuantity(request.quantity());
    purchaseRequest.setExpectedDate(request.expectedDate());
    purchaseRequest.setReason(request.reason());
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
    record.setApproverName(request.approverName());
    record.setDecidedAt(OffsetDateTime.now());
    requestApprovalRepository.save(record);
    return toPurchaseRequestResponse(purchaseRequest);
  }

  @Transactional(readOnly = true)
  public List<PurchaseOrderResponse> listPurchaseOrders() {
    List<PurchaseOrder> orders = orderRepository.findAllByOrderByCreatedAtDesc();
    return mapOrders(orders);
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
    order.setOrderAmount(purchaseRequest.getQuantity().multiply(request.unitPrice()));
    order.setExpectedDeliveryDate(request.expectedDeliveryDate() == null
        ? purchaseRequest.getExpectedDate()
        : request.expectedDeliveryDate());
    order.setStatus(PurchaseOrderStatus.ORDERED);

    purchaseRequest.setStatus(PurchaseRequestStatus.ORDERED);
    requestRepository.save(purchaseRequest);
    PurchaseOrder saved = orderRepository.save(order);
    return toPurchaseOrderResponse(saved, supplier, purchaseRequest);
  }

  @Transactional
  public ReceivePurchaseOrderResult receiveOrder(UUID id, ReceivePurchaseOrderRequest request) {
    PurchaseOrder order = orderRepository.findById(id)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() != PurchaseOrderStatus.ORDERED
        && order.getStatus() != PurchaseOrderStatus.PARTIAL_RECEIVED) {
      throw new BusinessException("当前订单状态不能收货");
    }
    if (order.getPartId() == null) {
      throw new BusinessException("订单未关联物料，不能入库");
    }
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
    receipt.setAmount(receiptAmount);
    receipt.setReceivedDate(request.receivedDate());
    receipt.setDeliveryNo(request.deliveryNo());
    receipt.setReceiverName(request.receiverName());
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
    payable.setPaidAmount(BigDecimal.ZERO);
    payable.setDueDate(request.payableDueDate());
    payable.setStatus(PayableStatus.PENDING);
    ProcurementPayable savedPayable = payableRepository.save(payable);

    return new ReceivePurchaseOrderResult(
        toPurchaseOrderResponse(savedOrder, supplier, purchaseRequest),
        toGoodsReceiptResponse(savedReceipt, savedOrder, part),
        toPayableResponse(savedPayable, supplier, savedOrder),
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

  private SupplierResponse toSupplierResponse(Supplier supplier) {
    return new SupplierResponse(
        supplier.getId(),
        supplier.getCode(),
        supplier.getName(),
        supplier.getCategory(),
        supplier.getContactName(),
        supplier.getPhone(),
        supplier.getSettlementTerms(),
        supplier.getRiskStatus()
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
        request.getExpectedDate(),
        request.getReason(),
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
        amount(order.getOrderAmount()),
        order.getExpectedDeliveryDate(),
        order.getStatus()
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
        receipt.getAmount(),
        receipt.getReceivedDate(),
        receipt.getDeliveryNo(),
        receipt.getReceiverName()
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
        amount(payable.getPaidAmount()),
        outstanding,
        payable.getDueDate(),
        payable.getStatus()
    );
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
