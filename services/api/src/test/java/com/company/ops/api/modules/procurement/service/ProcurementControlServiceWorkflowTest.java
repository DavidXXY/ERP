package com.company.ops.api.modules.procurement.service;

import static com.company.ops.api.modules.procurement.dto.ProcurementControlDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.repository.*;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcurementControlServiceWorkflowTest {
  @Mock private ProcurementInquiryRepository inquiries;
  @Mock private ProcurementInquiryRequestRepository inquiryRequests;
  @Mock private ProcurementInquiryInvitationRepository invitations;
  @Mock private SupplierQuotationRepository quotes;
  @Mock private SupplierQuotationLineRepository quoteLines;
  @Mock private PurchaseRequestRepository requests;
  @Mock private SupplierRepository suppliers;
  @Mock private PurchaseOrderRepository orders;
  @Mock private GoodsReceiptRepository receipts;
  @Mock private InventoryPartRepository parts;
  @Mock private StockMovementRepository movements;
  @Mock private ProcurementPayableRepository payables;
  @Mock private ProcurementCostAllocationRepository costs;
  @Mock private ProcurementReturnOrderRepository returns;
  @Mock private SupplierInvoiceRepository invoices;
  @Mock private PurchaseRequestApprovalRecordRepository requestApprovals;
  @Mock private ProjectRepository projects;
  @Mock private ProcurementArrivalService arrivals;
  @Mock private LedgerService ledgerService;
  @Mock private DataScopeService dataScopeService;
  @Mock private SupplierPortalNotifier portalNotifier;
  @InjectMocks private ProcurementControlService service;

  @Test
  void cancelledOrderCannotBeInspected() {
    GoodsReceipt receipt = receipt();
    PurchaseOrder order = order(receipt.getOrderId(), PurchaseOrderStatus.CANCELLED);
    when(receipts.findById(receipt.getId())).thenReturn(Optional.of(receipt));
    when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> service.inspect(receipt.getId(), new InspectReceipt(
        BigDecimal.ONE, BigDecimal.ZERO, "质检员", null, LocalDate.now().plusDays(30))))
        .isInstanceOf(BusinessException.class).hasMessageContaining("已取消或关闭");
  }

  @Test
  void replacementResolutionCreatesNewPendingInspectionReceipt() {
    GoodsReceipt original = receipt();
    original.setQuantity(new BigDecimal("2"));
    PurchaseOrder order = order(original.getOrderId(), PurchaseOrderStatus.PARTIAL_RECEIVED);
    ProcurementReturnOrder returnOrder = new ProcurementReturnOrder();
    returnOrder.setId(UUID.randomUUID()); returnOrder.setCode("TH-001");
    returnOrder.setOrderId(order.getId()); returnOrder.setReceiptId(original.getId());
    returnOrder.setQuantity(new BigDecimal("2")); returnOrder.setAmount(new BigDecimal("200"));
    returnOrder.setStatus("OPEN");
    when(returns.findById(returnOrder.getId())).thenReturn(Optional.of(returnOrder));
    when(returns.save(returnOrder)).thenReturn(returnOrder);
    when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
    when(receipts.findById(original.getId())).thenReturn(Optional.of(original));
    when(receipts.countByOrderId(order.getId())).thenReturn(1L);
    when(receipts.save(any(GoodsReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.resolveReturn(returnOrder.getId(), new ResolveReturn(
        BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO, "补发并折让", "已确认", "采购员"));

    ArgumentCaptor<GoodsReceipt> captor = ArgumentCaptor.forClass(GoodsReceipt.class);
    verify(receipts).save(captor.capture());
    assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("1");
    assertThat(captor.getValue().getInspectionStatus()).isEqualTo("PENDING");
    assertThat(captor.getValue().getClientRequestId()).isEqualTo("RETURN:" + returnOrder.getId());
  }

  @Test
  void draftQuoteCannotBeSelectedEvenWhenAnotherValidQuoteExists() {
    UUID inquiryId = UUID.randomUUID();
    ProcurementInquiry inquiry = new ProcurementInquiry();
    inquiry.setId(inquiryId);
    inquiry.setStatus("OPEN");
    inquiry.setSourcingMethod("COMPETITIVE");
    inquiry.setMinQuoteCount(1);

    SupplierQuotation submitted = new SupplierQuotation();
    submitted.setId(UUID.randomUUID());
    submitted.setInquiryId(inquiryId);
    submitted.setSubmissionStatus("SUBMITTED");
    SupplierQuotation draft = new SupplierQuotation();
    draft.setId(UUID.randomUUID());
    draft.setInquiryId(inquiryId);
    draft.setSubmissionStatus("DRAFT");

    when(inquiries.findById(inquiryId)).thenReturn(Optional.of(inquiry));
    when(quotes.findByInquiryIdOrderByUnitPriceAsc(inquiryId)).thenReturn(java.util.List.of(submitted, draft));
    when(quotes.findById(draft.getId())).thenReturn(Optional.of(draft));

    assertThatThrownBy(() -> service.selectQuote(inquiryId, draft.getId(),
        new SelectSupplierQuote("采购员", "选择当前报价")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已提交");
  }

  @Test
  void expiredInquiryCannotInviteSuppliers() {
    UUID inquiryId = UUID.randomUUID();
    ProcurementInquiry inquiry = new ProcurementInquiry();
    inquiry.setId(inquiryId);
    inquiry.setStatus("OPEN");
    inquiry.setDeadline(LocalDate.now().minusDays(1));
    when(inquiries.findById(inquiryId)).thenReturn(Optional.of(inquiry));

    assertThatThrownBy(() -> service.inviteSuppliers(inquiryId,
        new InviteSuppliers(java.util.List.of(UUID.randomUUID()))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("截止日期");
  }

  @Test
  void openInquiryDeadlineCanBeExtended() {
    UUID inquiryId = UUID.randomUUID();
    ProcurementInquiry inquiry = new ProcurementInquiry();
    inquiry.setId(inquiryId);
    inquiry.setStatus("OPEN");
    inquiry.setDeadline(LocalDate.now().plusDays(1));
    LocalDate newDeadline = LocalDate.now().plusDays(7);
    when(inquiries.findById(inquiryId)).thenReturn(Optional.of(inquiry));
    when(inquiries.save(inquiry)).thenReturn(inquiry);

    service.updateInquiryDeadline(inquiryId, new UpdateInquiryDeadline(newDeadline));

    assertThat(inquiry.getDeadline()).isEqualTo(newDeadline);
    verify(inquiries).save(inquiry);
  }

  @Test
  void inquiryDeadlineCannotMoveIntoPast() {
    UUID inquiryId = UUID.randomUUID();
    ProcurementInquiry inquiry = new ProcurementInquiry();
    inquiry.setId(inquiryId);
    inquiry.setStatus("OPEN");
    when(inquiries.findById(inquiryId)).thenReturn(Optional.of(inquiry));

    assertThatThrownBy(() -> service.updateInquiryDeadline(
        inquiryId, new UpdateInquiryDeadline(LocalDate.now().minusDays(1))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不能早于今天");
  }

  private GoodsReceipt receipt() {
    GoodsReceipt receipt = new GoodsReceipt();
    receipt.setId(UUID.randomUUID()); receipt.setCode("DH-001"); receipt.setOrderId(UUID.randomUUID());
    receipt.setPartId(UUID.randomUUID()); receipt.setQuantity(BigDecimal.ONE);
    receipt.setUnitPrice(new BigDecimal("100")); receipt.setTaxRate(new BigDecimal("13"));
    receipt.setAmount(new BigDecimal("100")); receipt.setReceivedDate(LocalDate.now());
    receipt.setPayableDueDate(LocalDate.now().plusDays(30)); receipt.setInspectionStatus("PENDING");
    return receipt;
  }

  private PurchaseOrder order(UUID id, PurchaseOrderStatus status) {
    PurchaseOrder order = new PurchaseOrder();
    order.setId(id); order.setCode("CGDD-001"); order.setStatus(status);
    order.setApprovalStatus(ApprovalStatus.APPROVED); order.setUnitPrice(new BigDecimal("100"));
    order.setReceivedQty(BigDecimal.ZERO); order.setOrderedQty(new BigDecimal("2"));
    return order;
  }
}
