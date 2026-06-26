package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.domain.PurchaseRequest;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestStatus;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseRequestRequest;
import com.company.ops.api.modules.procurement.dto.CreateSupplierRequest;
import com.company.ops.api.modules.procurement.dto.PurchaseOrderResponse;
import com.company.ops.api.modules.procurement.dto.PurchaseRequestResponse;
import com.company.ops.api.modules.procurement.dto.SupplierResponse;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProcurementService {

  private final SupplierRepository supplierRepository;
  private final PurchaseRequestRepository requestRepository;
  private final PurchaseOrderRepository orderRepository;
  private final InventoryPartRepository partRepository;

  public ProcurementService(
      SupplierRepository supplierRepository,
      PurchaseRequestRepository requestRepository,
      PurchaseOrderRepository orderRepository,
      InventoryPartRepository partRepository
  ) {
    this.supplierRepository = supplierRepository;
    this.requestRepository = requestRepository;
    this.orderRepository = orderRepository;
    this.partRepository = partRepository;
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
          .orElseThrow(() -> new BusinessException("备件不存在"));
      partName = part.getName();
    }
    if (!StringUtils.hasText(partName)) {
      throw new BusinessException("请选择备件或填写采购物料名称");
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

  @Transactional(readOnly = true)
  public List<PurchaseOrderResponse> listPurchaseOrders() {
    List<PurchaseOrder> orders = orderRepository.findAllByOrderByCreatedAtDesc();
    Map<UUID, Supplier> suppliers = supplierRepository.findAllById(
            orders.stream().map(PurchaseOrder::getSupplierId).distinct().toList()
        ).stream()
        .collect(Collectors.toMap(Supplier::getId, supplier -> supplier));
    Map<UUID, PurchaseRequest> requests = requestRepository.findAllById(
            orders.stream().map(PurchaseOrder::getRequestId).filter(id -> id != null).distinct().toList()
        ).stream()
        .collect(Collectors.toMap(PurchaseRequest::getId, request -> request));
    return orders.stream()
        .map(order -> toPurchaseOrderResponse(order, suppliers, requests))
        .toList();
  }

  @Transactional
  public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request) {
    if (orderRepository.existsByCode(request.code())) {
      throw new BusinessException("采购订单编码已存在");
    }
    Supplier supplier = supplierRepository.findById(request.supplierId())
        .orElseThrow(() -> new BusinessException("供应商不存在"));

    PurchaseRequest purchaseRequest = null;
    if (request.requestId() != null) {
      purchaseRequest = requestRepository.findById(request.requestId())
          .orElseThrow(() -> new BusinessException("采购申请不存在"));
      purchaseRequest.setStatus(PurchaseRequestStatus.ORDERED);
      requestRepository.save(purchaseRequest);
    }

    PurchaseOrder order = new PurchaseOrder();
    order.setCode(request.code());
    order.setSupplierId(request.supplierId());
    order.setRequestId(request.requestId());
    order.setOrderAmount(request.orderAmount() == null ? BigDecimal.ZERO : request.orderAmount());
    order.setExpectedDeliveryDate(request.expectedDeliveryDate());
    order.setStatus(PurchaseOrderStatus.ORDERED);

    PurchaseOrder saved = orderRepository.save(order);
    Map<UUID, Supplier> suppliers = Map.of(supplier.getId(), supplier);
    Map<UUID, PurchaseRequest> requests = purchaseRequest == null
        ? Map.of()
        : Map.of(purchaseRequest.getId(), purchaseRequest);
    return toPurchaseOrderResponse(saved, suppliers, requests);
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
        request.getApprovalStatus()
    );
  }

  private PurchaseOrderResponse toPurchaseOrderResponse(
      PurchaseOrder order,
      Map<UUID, Supplier> suppliers,
      Map<UUID, PurchaseRequest> requests
  ) {
    Supplier supplier = suppliers.get(order.getSupplierId());
    PurchaseRequest request = order.getRequestId() == null ? null : requests.get(order.getRequestId());
    return new PurchaseOrderResponse(
        order.getId(),
        order.getCode(),
        order.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        order.getRequestId(),
        request == null ? null : request.getCode(),
        order.getOrderAmount(),
        order.getExpectedDeliveryDate(),
        order.getStatus()
    );
  }
}
