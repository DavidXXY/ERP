package com.company.ops.api.modules.procurement.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderChange;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.dto.OrderChangeDtos.CreateOrderChangeRequest;
import com.company.ops.api.modules.procurement.dto.OrderChangeDtos.DecideOrderChangeRequest;
import com.company.ops.api.modules.procurement.dto.OrderChangeDtos.OrderChangeResponse;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderChangeRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 订单变更单：数量/单价/交期变更留痕，审批通过后应用到订单并推进订单版本。 */
@Service
public class ProcurementChangeService {

  private final PurchaseOrderChangeRepository changes;
  private final PurchaseOrderRepository orders;
  private final CodeGenerator codeGenerator;

  public ProcurementChangeService(
      PurchaseOrderChangeRepository changes,
      PurchaseOrderRepository orders,
      CodeGenerator codeGenerator
  ) {
    this.changes = changes;
    this.orders = orders;
    this.codeGenerator = codeGenerator;
  }

  @Transactional
  public OrderChangeResponse createChange(UUID orderId, CreateOrderChangeRequest request) {
    PurchaseOrder order = orders.findByIdForUpdate(orderId)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() == PurchaseOrderStatus.CANCELLED
        || order.getStatus() == PurchaseOrderStatus.CLOSED) {
      throw new BusinessException("已取消或已关闭的订单不能发起变更");
    }
    BigDecimal quantityAfter = request.quantityAfter() == null
        ? amount(order.getOrderedQty()) : amount(request.quantityAfter());
    BigDecimal priceAfter = request.unitPriceAfter() == null
        ? amount(order.getUnitPrice()) : amount(request.unitPriceAfter());
    LocalDate dateAfter = request.expectedDateAfter() == null
        ? order.getExpectedDeliveryDate() : request.expectedDateAfter();
    if (quantityAfter.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException("变更后数量必须大于 0");
    }
    if (priceAfter.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException("变更后单价必须大于 0");
    }
    if (quantityAfter.compareTo(amount(order.getReceivedQty())) < 0) {
      throw new BusinessException("变更后数量不能小于已到货数量 " + order.getReceivedQty());
    }
    boolean qtyChanged = quantityAfter.compareTo(amount(order.getOrderedQty())) != 0;
    boolean priceChanged = priceAfter.compareTo(amount(order.getUnitPrice())) != 0;
    boolean dateChanged = !Objects.equals(dateAfter, order.getExpectedDeliveryDate());
    if (!qtyChanged && !priceChanged && !dateChanged) {
      throw new BusinessException("变更内容与当前订单一致，无需发起变更");
    }
    String changeType = (qtyChanged && priceChanged) || (qtyChanged && dateChanged) || (priceChanged && dateChanged)
        ? "MIXED" : qtyChanged ? "QTY" : priceChanged ? "PRICE" : "DATE";

    PurchaseOrderChange change = new PurchaseOrderChange();
    change.setTenantId(TenantContext.currentTenant());
    change.setOrderId(orderId);
    change.setChangeNo(codeGenerator.generate("PURCHASE_ORDER_CHANGE"));
    change.setChangeType(changeType);
    change.setQuantityBefore(amount(order.getOrderedQty()));
    change.setQuantityAfter(quantityAfter);
    change.setUnitPriceBefore(amount(order.getUnitPrice()));
    change.setUnitPriceAfter(priceAfter);
    change.setExpectedDateBefore(order.getExpectedDeliveryDate());
    change.setExpectedDateAfter(dateAfter);
    change.setReason(request.reason());
    change.setStatus("PENDING");
    change.setCreatedByName(currentName());
    change.setOrderVersionBefore(order.getOrderVersion());
    return toResponse(changes.save(change), order.getCode());
  }

  @Transactional
  public List<OrderChangeResponse> listChanges(UUID orderId) {
    return changes.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
        .map(change -> toResponse(change, null))
        .toList();
  }

  @Transactional
  public OrderChangeResponse decideChange(UUID changeId, DecideOrderChangeRequest request) {
    PurchaseOrderChange change = changes.findById(changeId)
        .orElseThrow(() -> new BusinessException("变更单不存在"));
    if (!"PENDING".equals(change.getStatus())) {
      throw new BusinessException("该变更单已处理");
    }
    String decision = request.decision();
    if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
      throw new BusinessException("请选择通过或驳回");
    }
    change.setDecidedByName(currentName());
    change.setDecisionComment(request.comment());
    if ("REJECTED".equals(decision)) {
      change.setStatus("REJECTED");
      return toResponse(changes.save(change), null);
    }
    PurchaseOrder order = orders.findByIdForUpdate(change.getOrderId())
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() == PurchaseOrderStatus.CANCELLED
        || order.getStatus() == PurchaseOrderStatus.CLOSED) {
      throw new BusinessException("订单已取消或关闭，不能应用变更");
    }
    if (change.getQuantityAfter() != null
        && change.getQuantityAfter().compareTo(amount(order.getReceivedQty())) < 0) {
      throw new BusinessException("变更后数量不能小于已到货数量");
    }
    order.setOrderedQty(change.getQuantityAfter() == null
        ? order.getOrderedQty() : amount(change.getQuantityAfter()));
    order.setUnitPrice(change.getUnitPriceAfter() == null
        ? order.getUnitPrice() : amount(change.getUnitPriceAfter()));
    order.setExpectedDeliveryDate(change.getExpectedDateAfter() == null
        ? order.getExpectedDeliveryDate() : change.getExpectedDateAfter());
    order.setOrderAmount(amount(order.getOrderedQty()).multiply(amount(order.getUnitPrice())));
    order.setOrderVersion(order.getOrderVersion() == null ? 2 : order.getOrderVersion() + 1);
    orders.save(order);
    change.setStatus("APPROVED");
    change.setOrderVersionAfter(order.getOrderVersion());
    change.setAppliedAt(OffsetDateTime.now());
    return toResponse(changes.save(change), order.getCode());
  }

  private OrderChangeResponse toResponse(PurchaseOrderChange change, String orderCode) {
    return new OrderChangeResponse(
        change.getId(), change.getOrderId(), orderCode, change.getChangeNo(),
        change.getChangeType(), change.getQuantityBefore(), change.getQuantityAfter(),
        change.getUnitPriceBefore(), change.getUnitPriceAfter(),
        change.getExpectedDateBefore(), change.getExpectedDateAfter(),
        change.getReason(), change.getStatus(), change.getCreatedByName(),
        change.getDecidedByName(), change.getDecisionComment(),
        change.getOrderVersionBefore(), change.getOrderVersionAfter(),
        change.getAppliedAt(), change.getCreatedAt());
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }
}
