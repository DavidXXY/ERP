package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.dto.ProcurementControlDtos.*;
import com.company.ops.api.modules.procurement.repository.*;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 采购退换货处理。
 * 从 ProcurementControlService 拆分而来，负责退换货查询、结案与应付冲减。
 */
@Service
public class ProcurementReturnService {

  private final ProcurementReturnOrderRepository returns;
  private final PurchaseOrderRepository orders;
  private final GoodsReceiptRepository receipts;
  private final ProcurementPayableRepository payables;
  private final PayableAdjustmentRepository adjustments;
  private final LedgerService ledgerService;
  private final SupplierPortalNotifier portalNotifier;

  public ProcurementReturnService(
      ProcurementReturnOrderRepository returns,
      PurchaseOrderRepository orders,
      GoodsReceiptRepository receipts,
      ProcurementPayableRepository payables,
      PayableAdjustmentRepository adjustments,
      LedgerService ledgerService,
      SupplierPortalNotifier portalNotifier) {
    this.returns = returns;
    this.orders = orders;
    this.receipts = receipts;
    this.payables = payables;
    this.adjustments = adjustments;
    this.ledgerService = ledgerService;
    this.portalNotifier = portalNotifier;
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

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }

  private static BigDecimal valueOr(BigDecimal value, BigDecimal fallback) {
    return value == null ? fallback : value;
  }

}
