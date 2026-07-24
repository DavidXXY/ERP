package com.company.ops.api.modules.procurement.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.GoodsReceipt;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.repository.GoodsReceiptRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Canonical, idempotent purchase-order arrival workflow shared by both API routes. */
@Service
public class ProcurementArrivalService {
  private final PurchaseOrderRepository orders;
  private final GoodsReceiptRepository receipts;
  private final InventoryPartRepository parts;

  public ProcurementArrivalService(
      PurchaseOrderRepository orders,
      GoodsReceiptRepository receipts,
      InventoryPartRepository parts
  ) {
    this.orders = orders;
    this.receipts = receipts;
    this.parts = parts;
  }

  @Transactional
  public GoodsReceipt register(UUID orderId, ReceivePurchaseOrderRequest request) {
    PurchaseOrder order = orders.findByIdForUpdate(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    String clientRequestId = StringUtils.hasText(request.clientRequestId())
        ? request.clientRequestId().trim() : null;
    if (clientRequestId != null) {
      GoodsReceipt existing = receipts.findByClientRequestId(clientRequestId).orElse(null);
      if (existing != null) {
        if (!Objects.equals(existing.getOrderId(), orderId)) {
          throw new BusinessException("幂等请求编号已被其他采购订单使用");
        }
        return existing;
      }
    }
    if (order.getApprovalStatus() != ApprovalStatus.APPROVED
        || (order.getStatus() != PurchaseOrderStatus.ORDERED
        && order.getStatus() != PurchaseOrderStatus.PARTIAL_RECEIVED)) {
      throw new BusinessException("只有审批通过且未关闭的订单可以登记到货");
    }
    if (order.getPartId() == null) {
      throw new BusinessException("订单未关联物料，不能入库");
    }
    InventoryPart part = parts.findById(order.getPartId())
        .orElseThrow(() -> new BusinessException("关联物料不存在"));
    BigDecimal pending = receipts.findByOrderId(orderId).stream()
        .filter(item -> "PENDING".equals(item.getInspectionStatus()))
        .map(GoodsReceipt::getQuantity)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (amount(order.getReceivedQty()).add(pending).add(request.quantity())
        .compareTo(amount(order.getOrderedQty())) > 0) {
      throw new BusinessException("到货数量超过订单剩余数量");
    }

    GoodsReceipt receipt = new GoodsReceipt();
    receipt.setCode("DH-" + order.getCode() + "-"
        + String.format("%02d", receipts.countByOrderId(orderId) + 1));
    receipt.setOrderId(orderId);
    receipt.setPartId(part.getId());
    receipt.setQuantity(request.quantity());
    receipt.setUnitPrice(amount(order.getUnitPrice()));
    receipt.setTaxRate(amount(order.getTaxRate()));
    receipt.setAmount(request.quantity().multiply(amount(order.getUnitPrice())));
    receipt.setReceivedDate(request.receivedDate());
    receipt.setDeliveryNo(request.deliveryNo());
    receipt.setReceiverName(currentName());
    receipt.setPayableDueDate(request.payableDueDate());
    receipt.setInspectionStatus("PENDING");
    receipt.setClientRequestId(clientRequestId);
    receipt.setAsnNo(request.asnNo());
    return receipts.save(receipt);
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }
}
