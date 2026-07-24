package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcurementArrivalServiceTest {
  @Mock private PurchaseOrderRepository orders;
  @Mock private GoodsReceiptRepository receipts;
  @Mock private InventoryPartRepository parts;
  @InjectMocks private ProcurementArrivalService service;

  private PurchaseOrder order;
  private InventoryPart part;

  @BeforeEach
  void setUp() {
    part = new InventoryPart();
    part.setId(UUID.randomUUID());
    order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("PO-001");
    order.setPartId(part.getId());
    order.setApprovalStatus(ApprovalStatus.APPROVED);
    order.setStatus(PurchaseOrderStatus.ORDERED);
    order.setOrderedQty(BigDecimal.TEN);
    order.setReceivedQty(BigDecimal.ZERO);
    order.setUnitPrice(BigDecimal.valueOf(20));
    order.setTaxRate(BigDecimal.valueOf(13));
  }

  @Test
  void createsPendingArrivalOnce() {
    when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
    when(receipts.findByClientRequestId("req-1")).thenReturn(Optional.empty());
    when(parts.findById(part.getId())).thenReturn(Optional.of(part));
    when(receipts.findByOrderId(order.getId())).thenReturn(List.of());
    when(receipts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    GoodsReceipt result = service.register(order.getId(), request(" req-1 "));

    assertThat(result.getClientRequestId()).isEqualTo("req-1");
    assertThat(result.getInspectionStatus()).isEqualTo("PENDING");
    assertThat(result.getAmount()).isEqualByComparingTo("40");
    verify(receipts).save(result);
  }

  @Test
  void returnsExistingArrivalForRepeatedRequest() {
    GoodsReceipt existing = new GoodsReceipt();
    existing.setOrderId(order.getId());
    when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
    when(receipts.findByClientRequestId("req-1")).thenReturn(Optional.of(existing));

    assertThat(service.register(order.getId(), request("req-1"))).isSameAs(existing);
    verify(receipts, never()).save(any());
  }

  @Test
  void rejectsRequestIdAlreadyUsedByAnotherOrder() {
    GoodsReceipt existing = new GoodsReceipt();
    existing.setOrderId(UUID.randomUUID());
    when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
    when(receipts.findByClientRequestId("req-1")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.register(order.getId(), request("req-1")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("其他采购订单");
  }

  private ReceivePurchaseOrderRequest request(String requestId) {
    return new ReceivePurchaseOrderRequest(
        BigDecimal.valueOf(2), LocalDate.now(), "DN-1", "ignored",
        LocalDate.now().plusDays(30), requestId, null);
  }
}
