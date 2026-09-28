package com.company.ops.api.modules.sales.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;
import static com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.service.CreditService;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.sales.domain.SalesOrder;
import com.company.ops.api.modules.sales.domain.SalesOrderLine;
import com.company.ops.api.modules.sales.domain.SalesOrderStatus;
import com.company.ops.api.modules.sales.domain.SalesReturnLine;
import com.company.ops.api.modules.sales.domain.SalesReturnOrder;
import com.company.ops.api.modules.sales.domain.SalesShipment;
import com.company.ops.api.modules.sales.domain.SalesShipmentLine;
import com.company.ops.api.modules.sales.dto.SalesDtos.*;
import com.company.ops.api.modules.sales.repository.SalesOrderLineRepository;
import com.company.ops.api.modules.sales.repository.SalesOrderRepository;
import com.company.ops.api.modules.sales.repository.SalesReturnLineRepository;
import com.company.ops.api.modules.sales.repository.SalesReturnOrderRepository;
import com.company.ops.api.modules.sales.repository.SalesShipmentLineRepository;
import com.company.ops.api.modules.sales.repository.SalesShipmentRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesService {

  private final SalesOrderRepository orderRepository;
  private final SalesOrderLineRepository orderLineRepository;
  private final SalesShipmentRepository shipmentRepository;
  private final SalesShipmentLineRepository shipmentLineRepository;
  private final SalesReturnOrderRepository returnRepository;
  private final SalesReturnLineRepository returnLineRepository;
  private final InventoryPartRepository partRepository;
  private final StockMovementRepository movementRepository;
  private final CustomerRepository customerRepository;
  private final ReceivableRepository receivableRepository;
  private final LedgerService ledgerService;
  private final CodeGenerator codeGenerator;
  private final CreditService creditService;

  public SalesService(SalesOrderRepository orderRepository,
      SalesOrderLineRepository orderLineRepository,
      SalesShipmentRepository shipmentRepository,
      SalesShipmentLineRepository shipmentLineRepository,
      SalesReturnOrderRepository returnRepository,
      SalesReturnLineRepository returnLineRepository,
      InventoryPartRepository partRepository,
      StockMovementRepository movementRepository,
      CustomerRepository customerRepository,
      ReceivableRepository receivableRepository,
      LedgerService ledgerService,
      CodeGenerator codeGenerator,
      CreditService creditService) {
    this.orderRepository = orderRepository;
    this.orderLineRepository = orderLineRepository;
    this.shipmentRepository = shipmentRepository;
    this.shipmentLineRepository = shipmentLineRepository;
    this.returnRepository = returnRepository;
    this.returnLineRepository = returnLineRepository;
    this.partRepository = partRepository;
    this.movementRepository = movementRepository;
    this.customerRepository = customerRepository;
    this.receivableRepository = receivableRepository;
    this.ledgerService = ledgerService;
    this.codeGenerator = codeGenerator;
    this.creditService = creditService;
  }

  @Transactional(readOnly = true)
  public Page<SalesOrderResponse> listOrders(Pageable pageable) {
    Page<SalesOrder> orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    Map<UUID, List<SalesOrderLine>> linesByOrder = orderLineRepository
        .findByOrderIdIn(orders.getContent().stream().map(SalesOrder::getId).toList())
        .stream().collect(Collectors.groupingBy(SalesOrderLine::getOrderId));
    Set<UUID> customerIds = orders.getContent().stream().map(SalesOrder::getCustomerId).collect(Collectors.toSet());
    Map<UUID, String> customerNames = customerNames(customerIds);
    return orders.map(o -> toOrderResponse(o, customerNames.get(o.getCustomerId()), linesByOrder.getOrDefault(o.getId(), List.of())));
  }

  @Transactional(readOnly = true)
  public SalesOrderResponse getOrder(UUID id) {
    SalesOrder order = requireOrder(id);
    List<SalesOrderLine> lines = orderLineRepository.findByOrderIdOrderByCreatedAtAsc(id);
    Map<UUID, String> customerNames = customerNames(Set.of(order.getCustomerId()));
    return toOrderResponse(order, customerNames.get(order.getCustomerId()), lines);
  }

  @Transactional
  public SalesOrderResponse createOrder(CreateSalesOrderRequest request) {
    String code = request.code() != null && !request.code().isBlank()
        ? request.code().trim() : codeGenerator.generate("SALES_ORDER");
    if (orderRepository.existsByCode(code)) throw new BusinessException("销售订单号已存在");
    validateUniqueParts(request.lines());

    SalesOrder order = new SalesOrder();
    order.setCode(code);
    order.setCustomerId(request.customerId());
    order.setContractId(request.contractId());
    order.setOrderDate(request.orderDate());
    order.setStatus(SalesOrderStatus.DRAFT);
    order.setRemark(request.remark());
    SalesOrder saved = orderRepository.save(order);

    BigDecimal total = BigDecimal.ZERO;
    for (SalesOrderLineRequest item : request.lines()) {
      InventoryPart part = partRepository.findById(item.partId())
          .orElseThrow(() -> new BusinessException("物料不存在"));
      BigDecimal lineAmount = item.quantity().multiply(amount(item.unitPrice()));
      SalesOrderLine line = new SalesOrderLine();
      line.setOrderId(saved.getId());
      line.setPartId(part.getId());
      line.setPartName(part.getName());
      line.setQuantity(item.quantity());
      line.setUnitPrice(amount(item.unitPrice()));
      line.setAmount(lineAmount);
      orderLineRepository.save(line);
      total = total.add(lineAmount);
    }
    saved.setTotalAmount(total);
    orderRepository.save(saved);

    Map<UUID, String> customerNames = customerNames(Set.of(request.customerId()));
    return toOrderResponse(saved, customerNames.get(request.customerId()), orderLineRepository.findByOrderIdOrderByCreatedAtAsc(saved.getId()));
  }

  @Transactional
  public SalesOrderResponse approveOrder(UUID id) {
    SalesOrder order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("销售订单不存在"));
    if (order.getStatus() != SalesOrderStatus.DRAFT) throw new BusinessException("只有草稿订单可以审批");
    creditService.assertCreditAvailable(order.getCustomerId(), amount(order.getTotalAmount()));
    order.setStatus(SalesOrderStatus.APPROVED);
    orderRepository.save(order);
    List<SalesOrderLine> lines = orderLineRepository.findByOrderIdOrderByCreatedAtAsc(id);
    Map<UUID, String> customerNames = customerNames(Set.of(order.getCustomerId()));
    return toOrderResponse(order, customerNames.get(order.getCustomerId()), lines);
  }

  @Transactional
  public SalesOrderResponse cancelOrder(UUID id) {
    SalesOrder order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("销售订单不存在"));
    if (order.getStatus() != SalesOrderStatus.DRAFT) throw new BusinessException("已审批订单不能取消，请通过后续流程处理");
    order.setStatus(SalesOrderStatus.CANCELLED);
    orderRepository.save(order);
    List<SalesOrderLine> lines = orderLineRepository.findByOrderIdOrderByCreatedAtAsc(id);
    Map<UUID, String> customerNames = customerNames(Set.of(order.getCustomerId()));
    return toOrderResponse(order, customerNames.get(order.getCustomerId()), lines);
  }

  @Transactional(readOnly = true)
  public Page<ShipmentResponse> listShipments(Pageable pageable) {
    Page<SalesShipment> shipments = shipmentRepository.findAllByOrderByCreatedAtDesc(pageable);
    Map<UUID, List<SalesShipmentLine>> lines = shipmentLineRepository
        .findByShipmentIdIn(shipments.getContent().stream().map(SalesShipment::getId).toList())
        .stream().collect(Collectors.groupingBy(SalesShipmentLine::getShipmentId));
    Map<UUID, SalesOrder> orders = orderRepository.findAllById(
        shipments.getContent().stream().map(SalesShipment::getOrderId).distinct().toList())
        .stream().collect(Collectors.toMap(SalesOrder::getId, Function.identity()));
    return shipments.map(s -> toShipmentResponse(s, orders.get(s.getOrderId()), lines.getOrDefault(s.getId(), List.of())));
  }

  @Transactional
  public ShipmentResponse createShipment(CreateShipmentRequest request) {
    String code = request.code() != null && !request.code().isBlank()
        ? request.code().trim() : codeGenerator.generate("SALES_SHIPMENT");
    if (shipmentRepository.existsByCode(code)) throw new BusinessException("发货单号已存在");
    SalesOrder order = orderRepository.findByIdForUpdate(request.orderId())
        .orElseThrow(() -> new BusinessException("销售订单不存在"));
    if (order.getStatus() != SalesOrderStatus.APPROVED && order.getStatus() != SalesOrderStatus.PARTIAL_SHIPPED) {
      throw new BusinessException("订单未审批或已发货完毕，不能发货");
    }
    creditService.assertNotBlocked(order.getCustomerId());
    Map<UUID, SalesOrderLine> orderLines = orderLineRepository.findByOrderIdOrderByCreatedAtAsc(order.getId())
        .stream().collect(Collectors.toMap(SalesOrderLine::getId, Function.identity()));
    validateShipmentLines(request, orderLines);

    SalesShipment shipment = new SalesShipment();
    shipment.setCode(code);
    shipment.setOrderId(order.getId());
    shipment.setCustomerId(order.getCustomerId());
    shipment.setContractId(order.getContractId());
    shipment.setShipmentDate(request.shipmentDate());
    shipment.setReceiverName(request.receiverName());
    shipment.setRemark(request.remark());
    SalesShipment saved = shipmentRepository.save(shipment);

    BigDecimal cogsTotal = BigDecimal.ZERO;
    List<SalesShipmentLine> shipmentLines = new java.util.ArrayList<>();
    for (ShipmentLineRequest item : request.lines()) {
      SalesOrderLine orderLine = orderLines.get(item.orderLineId());
      InventoryPart part = partRepository.findByIdForUpdate(orderLine.getPartId())
          .orElseThrow(() -> new BusinessException("物料不存在"));
      if (part.getStockQty().compareTo(item.quantity()) < 0) {
        throw new BusinessException(part.getName() + "库存不足，当前库存" + part.getStockQty());
      }
      BigDecimal unitCost = amount(part.getUnitCost());
      BigDecimal lineAmount = item.quantity().multiply(unitCost);
      SalesShipmentLine line = new SalesShipmentLine();
      line.setShipmentId(saved.getId());
      line.setOrderLineId(orderLine.getId());
      line.setPartId(part.getId());
      line.setPartName(part.getName());
      line.setQuantity(item.quantity());
      line.setUnitCost(unitCost);
      line.setAmount(lineAmount);
      shipmentLines.add(shipmentLineRepository.save(line));

      part.setStockQty(part.getStockQty().subtract(item.quantity()));
      partRepository.save(part);
      saveMovement(part.getId(), StockMovementType.OUTBOUND, item.quantity(), unitCost,
          code, "销售发货 " + code);
      orderLine.setShippedQty(orderLine.getShippedQty().add(item.quantity()));
      cogsTotal = cogsTotal.add(lineAmount);
    }
    orderLineRepository.saveAll(orderLines.values());
    saved.setTotalAmount(cogsTotal);
    shipmentRepository.save(saved);

    ledgerService.post("SALES_SHIPMENT", code, request.shipmentDate(),
        "销售发货结转成本 " + code,
        List.of(
            new PostingLine("6401", "主营业务成本", cogsTotal, BigDecimal.ZERO, "销售发货 " + code),
            new PostingLine("1405", "库存商品", BigDecimal.ZERO, cogsTotal, "销售发货 " + code)));

    boolean fullyShipped = orderLines.values().stream()
        .allMatch(line -> line.getShippedQty().compareTo(line.getQuantity()) >= 0);
    order.setStatus(fullyShipped ? SalesOrderStatus.SHIPPED : SalesOrderStatus.PARTIAL_SHIPPED);
    orderRepository.save(order);

    return toShipmentResponse(saved, order, shipmentLines);
  }

  @Transactional(readOnly = true)
  public Page<ReturnResponse> listReturns(Pageable pageable) {
    Page<SalesReturnOrder> returns = returnRepository.findAllByOrderByCreatedAtDesc(pageable);
    Map<UUID, List<SalesReturnLine>> lines = returnLineRepository
        .findByReturnIdIn(returns.getContent().stream().map(SalesReturnOrder::getId).toList())
        .stream().collect(Collectors.groupingBy(SalesReturnLine::getReturnId));
    return returns.map(r -> toReturnResponse(r, lines.getOrDefault(r.getId(), List.of())));
  }

  @Transactional
  public ReturnResponse createReturn(CreateReturnRequest request) {
    String code = request.code() != null && !request.code().isBlank()
        ? request.code().trim() : codeGenerator.generate("SALES_RETURN");
    if (returnRepository.existsByCode(code)) throw new BusinessException("退货单号已存在");
    SalesShipment shipment = shipmentRepository.findById(request.shipmentId())
        .orElseThrow(() -> new BusinessException("发货单不存在"));
    Map<UUID, SalesShipmentLine> shipmentLines = shipmentLineRepository.findByShipmentIdOrderByCreatedAtAsc(shipment.getId())
        .stream().collect(Collectors.toMap(SalesShipmentLine::getId, Function.identity()));
    validateReturnLines(request, shipmentLines);

    SalesReturnOrder returnOrder = new SalesReturnOrder();
    returnOrder.setCode(code);
    returnOrder.setShipmentId(shipment.getId());
    returnOrder.setOrderId(shipment.getOrderId());
    returnOrder.setCustomerId(shipment.getCustomerId());
    returnOrder.setContractId(shipment.getContractId());
    returnOrder.setReturnDate(request.returnDate());
    returnOrder.setReason(request.reason());
    SalesReturnOrder savedReturn = returnRepository.save(returnOrder);

    BigDecimal total = BigDecimal.ZERO;
    List<SalesReturnLine> returnLines = new java.util.ArrayList<>();
    for (ReturnLineRequest item : request.lines()) {
      SalesShipmentLine shipmentLine = shipmentLines.get(item.shipmentLineId());
      InventoryPart part = partRepository.findByIdForUpdate(shipmentLine.getPartId())
          .orElseThrow(() -> new BusinessException("物料不存在"));
      BigDecimal lineAmount = item.quantity().multiply(shipmentLine.getUnitCost());
      SalesReturnLine line = new SalesReturnLine();
      line.setReturnId(savedReturn.getId());
      line.setShipmentLineId(shipmentLine.getId());
      line.setPartId(part.getId());
      line.setPartName(part.getName());
      line.setQuantity(item.quantity());
      line.setUnitCost(shipmentLine.getUnitCost());
      line.setAmount(lineAmount);
      returnLines.add(returnLineRepository.save(line));

      part.setStockQty(part.getStockQty().add(item.quantity()));
      partRepository.save(part);
      saveMovement(part.getId(), StockMovementType.RETURN, item.quantity(), shipmentLine.getUnitCost(),
          code, "销售退货 " + code + " · " + request.reason());
      shipmentLine.setReturnedQty(shipmentLine.getReturnedQty().add(item.quantity()));
      total = total.add(lineAmount);
    }
    shipmentLineRepository.saveAll(shipmentLines.values());

    SalesOrder order = orderRepository.findByIdForUpdate(shipment.getOrderId())
        .orElseThrow(() -> new BusinessException("销售订单不存在"));
    Map<UUID, SalesOrderLine> orderLines = orderLineRepository.findByOrderIdOrderByCreatedAtAsc(order.getId())
        .stream().collect(Collectors.toMap(SalesOrderLine::getId, Function.identity()));
    for (ReturnLineRequest item : request.lines()) {
      SalesShipmentLine shipmentLine = shipmentLines.get(item.shipmentLineId());
      SalesOrderLine orderLine = orderLines.get(shipmentLine.getOrderLineId());
      orderLine.setReturnedQty(orderLine.getReturnedQty().add(item.quantity()));
    }
    orderLineRepository.saveAll(orderLines.values());
    boolean anyShipped = orderLines.values().stream().anyMatch(line -> line.getShippedQty().compareTo(BigDecimal.ZERO) > 0);
    order.setStatus(anyShipped ? SalesOrderStatus.PARTIAL_SHIPPED : SalesOrderStatus.APPROVED);
    orderRepository.save(order);

    savedReturn.setTotalAmount(total);
    returnRepository.save(savedReturn);

    ledgerService.post("SALES_RETURN", code, request.returnDate(),
        "销售退货冲回成本 " + code,
        List.of(
            new PostingLine("1405", "库存商品", total, BigDecimal.ZERO, "销售退货 " + code),
            new PostingLine("6401", "主营业务成本", BigDecimal.ZERO, total, "销售退货 " + code)));

    createReceivableCreditNote(savedReturn, total);
    return toReturnResponse(savedReturn, returnLines);
  }

  private void createReceivableCreditNote(SalesReturnOrder returnOrder, BigDecimal amount) {
    if (returnOrder.getContractId() == null || amount.signum() == 0) return;
    Receivable note = new Receivable();
    note.setCustomerId(returnOrder.getCustomerId());
    note.setContractId(returnOrder.getContractId());
    note.setCode(codeGenerator.generate("RECEIVABLE"));
    note.setSourceNo("销售退货 " + returnOrder.getCode());
    note.setAmount(amount.negate());
    note.setDueDate(returnOrder.getReturnDate());
    note.setSettledAmount(BigDecimal.ZERO);
    note.setStatus(ReceivableStatus.INVOICE_PENDING);
    receivableRepository.save(note);
  }

  private void validateShipmentLines(CreateShipmentRequest request, Map<UUID, SalesOrderLine> orderLines) {
    Set<UUID> seen = new HashSet<>();
    for (ShipmentLineRequest item : request.lines()) {
      if (!seen.add(item.orderLineId())) throw new BusinessException("同一订单明细不能重复发货");
      SalesOrderLine line = orderLines.get(item.orderLineId());
      if (line == null) throw new BusinessException("发货明细不属于该订单");
      BigDecimal available = line.getQuantity().subtract(line.getShippedQty());
      if (item.quantity().compareTo(available) > 0) {
        throw new BusinessException(line.getPartName() + "发货数量超过可发数量" + available);
      }
    }
  }

  private void validateReturnLines(CreateReturnRequest request, Map<UUID, SalesShipmentLine> shipmentLines) {
    Set<UUID> seen = new HashSet<>();
    for (ReturnLineRequest item : request.lines()) {
      if (!seen.add(item.shipmentLineId())) throw new BusinessException("同一发货明细不能重复退货");
      SalesShipmentLine line = shipmentLines.get(item.shipmentLineId());
      if (line == null) throw new BusinessException("退货明细不属于该发货单");
      BigDecimal available = line.getQuantity().subtract(line.getReturnedQty());
      if (item.quantity().compareTo(available) > 0) {
        throw new BusinessException(line.getPartName() + "退货数量超过可退数量" + available);
      }
    }
  }

  private void validateUniqueParts(List<SalesOrderLineRequest> lines) {
    Set<UUID> partIds = new HashSet<>();
    for (SalesOrderLineRequest line : lines) {
      if (!partIds.add(line.partId())) throw new BusinessException("同一物料不能重复出现在订单中");
    }
  }

  private void saveMovement(UUID partId, StockMovementType type, BigDecimal quantity,
      BigDecimal unitCost, String sourceNo, String remark) {
    StockMovement movement = new StockMovement();
    movement.setPartId(partId);
    movement.setMovementType(type);
    movement.setQuantity(quantity);
    movement.setUnitCost(amount(unitCost));
    movement.setAmount(quantity.multiply(amount(unitCost)));
    movement.setSourceNo(sourceNo);
    movement.setRemark(remark);
    movement.setOperatorName(currentName());
    movementRepository.save(movement);
  }

  private SalesOrder requireOrder(UUID id) {
    return orderRepository.findById(id).orElseThrow(() -> new BusinessException("销售订单不存在"));
  }

  private Map<UUID, String> customerNames(Set<UUID> ids) {
    if (ids.isEmpty()) return Map.of();
    return customerRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(Customer::getId, Customer::getName));
  }

  private SalesOrderResponse toOrderResponse(SalesOrder order, String customerName, List<SalesOrderLine> lines) {
    return new SalesOrderResponse(order.getId(), order.getCode(), order.getCustomerId(), customerName,
        order.getContractId(), order.getOrderDate(), order.getStatus(), amount(order.getTotalAmount()),
        order.getRemark(), lines.stream().map(l -> new SalesOrderLineResponse(l.getId(), l.getPartId(),
            l.getPartName(), l.getQuantity(), l.getUnitPrice(), l.getAmount(), l.getShippedQty(), l.getReturnedQty())).toList());
  }

  private ShipmentResponse toShipmentResponse(SalesShipment s, SalesOrder order, List<SalesShipmentLine> lines) {
    return new ShipmentResponse(s.getId(), s.getCode(), s.getOrderId(), order == null ? null : order.getCode(),
        s.getCustomerId(), s.getContractId(), s.getShipmentDate(), s.getReceiverName(), amount(s.getTotalAmount()),
        s.getRemark(), lines.stream().map(l -> new ShipmentLineResponse(l.getId(), l.getOrderLineId(), l.getPartId(),
            l.getPartName(), l.getQuantity(), l.getUnitCost(), l.getAmount(), l.getReturnedQty())).toList());
  }

  private ReturnResponse toReturnResponse(SalesReturnOrder r, List<SalesReturnLine> lines) {
    return new ReturnResponse(r.getId(), r.getCode(), r.getShipmentId(), r.getOrderId(), r.getCustomerId(),
        r.getContractId(), r.getReturnDate(), r.getReason(), amount(r.getTotalAmount()), r.getStatus(),
        lines.stream().map(l -> new ReturnLineResponse(l.getId(), l.getShipmentLineId(), l.getPartId(),
            l.getPartName(), l.getQuantity(), l.getUnitCost(), l.getAmount())).toList());
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }
}