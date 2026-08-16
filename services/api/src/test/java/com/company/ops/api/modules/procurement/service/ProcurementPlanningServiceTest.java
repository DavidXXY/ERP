package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiryRequest;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.domain.PurchaseRequest;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestStatus;
import com.company.ops.api.modules.procurement.repository.CentralPlanItemRepository;
import com.company.ops.api.modules.procurement.repository.CentralPlanRepository;
import com.company.ops.api.modules.procurement.repository.FrameworkAgreementItemRepository;
import com.company.ops.api.modules.procurement.repository.FrameworkAgreementRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRequestRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcurementPlanningServiceTest {
  @Test
  void generateSuggestionsAggregatesApprovedRequestsByPart() {
    FrameworkAgreementRepository agreements = mock(FrameworkAgreementRepository.class);
    FrameworkAgreementItemRepository agreementItems = mock(FrameworkAgreementItemRepository.class);
    CentralPlanRepository plans = mock(CentralPlanRepository.class);
    CentralPlanItemRepository planItems = mock(CentralPlanItemRepository.class);
    SupplierRepository suppliers = mock(SupplierRepository.class);
    InventoryPartRepository parts = mock(InventoryPartRepository.class);
    PurchaseRequestRepository requests = mock(PurchaseRequestRepository.class);
    PurchaseOrderRepository orders = mock(PurchaseOrderRepository.class);
    ProcurementInquiryRequestRepository inquiryRequests = mock(ProcurementInquiryRequestRepository.class);
    ProcurementService procurementService = mock(ProcurementService.class);

    UUID partId = UUID.randomUUID();
    InventoryPart part = new InventoryPart();
    part.setId(partId);
    part.setName("集采物料");
    part.setUnitCost(new BigDecimal("100"));

    PurchaseRequest r1 = request("CGSQ-1", partId, "集采物料", new BigDecimal("5"), new BigDecimal("80"));
    PurchaseRequest r2 = request("CGSQ-2", partId, "集采物料", new BigDecimal("3"), new BigDecimal("120"));
    PurchaseRequest r3 = request("CGSQ-3", partId, "集采物料", new BigDecimal("7"), new BigDecimal("90"));
    PurchaseRequest r4 = request("CGSQ-4", partId, "集采物料", new BigDecimal("9"), new BigDecimal("70"));
    when(requests.findByApprovalStatusAndStatusOrderByCreatedAtDesc(
        ApprovalStatus.APPROVED, PurchaseRequestStatus.APPROVED))
        .thenReturn(List.of(r1, r2, r3, r4));

    ProcurementInquiryRequest inquiryLink = new ProcurementInquiryRequest();
    inquiryLink.setRequestId(r3.getId());
    when(inquiryRequests.findByInquiryStatusIn(anyCollection())).thenReturn(List.of(inquiryLink));

    PurchaseOrder order = new PurchaseOrder();
    order.setRequestId(r4.getId());
    order.setOrderedQty(new BigDecimal("9"));
    when(orders.findByRequestIdNotNullAndStatusNot(PurchaseOrderStatus.CANCELLED))
        .thenReturn(List.of(order));

    when(parts.findAllById(any())).thenReturn(List.of(part));

    ProcurementPlanningService service = new ProcurementPlanningService(
        agreements, agreementItems, plans, planItems, suppliers, parts,
        requests, orders, inquiryRequests, mock(CodeGenerator.class), procurementService);

    var result = service.generateCentralPlanSuggestions(2026);

    assertThat(result.periodYear()).isEqualTo(2026);
    assertThat(result.items()).hasSize(1);
    var item = result.items().get(0);
    assertThat(item.partId()).isEqualTo(partId);
    assertThat(item.plannedQty()).isEqualByComparingTo("8");
    assertThat(item.requestCount()).isEqualTo(2);
    // (5*80 + 3*120) / 8 = 760/8 = 95
    assertThat(item.unitPrice()).isEqualByComparingTo("95.00");
    assertThat(item.estimatedAmount()).isEqualByComparingTo("760");
  }

  private PurchaseRequest request(String code, UUID partId, String partName,
      BigDecimal quantity, BigDecimal unitPrice) {
    PurchaseRequest request = new PurchaseRequest();
    request.setId(UUID.randomUUID());
    request.setCode(code);
    request.setPartId(partId);
    request.setPartName(partName);
    request.setQuantity(quantity);
    request.setUnitPrice(unitPrice);
    return request;
  }
}
