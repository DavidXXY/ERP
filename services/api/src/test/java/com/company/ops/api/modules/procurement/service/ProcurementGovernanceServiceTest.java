package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.procurement.domain.ProcurementContract;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiry;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierQuotation;
import com.company.ops.api.modules.procurement.domain.SupplierQuotationLine;
import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import com.company.ops.api.modules.procurement.dto.ProcurementGovernanceDtos.CreateContract;
import com.company.ops.api.modules.procurement.repository.GoodsReceiptRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementActionLogRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementCollaborationEventRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementContractRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierChangeRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoiceRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPerformanceReviewRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationLineRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.procurement.dto.ProcurementGovernanceDtos.ReviewAction;
import java.math.BigDecimal;
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
class ProcurementGovernanceServiceTest {
  @Mock private ProcurementContractRepository contracts;
  @Mock private SupplierChangeRequestRepository supplierChanges;
  @Mock private SupplierPerformanceReviewRepository reviews;
  @Mock private ProcurementCollaborationEventRepository collaborationEvents;
  @Mock private ProcurementActionLogRepository actionLogs;
  @Mock private SupplierRepository suppliers;
  @Mock private PurchaseOrderRepository orders;
  @Mock private GoodsReceiptRepository receipts;
  @Mock private SupplierInvoiceRepository invoices;
  @Mock private PurchaseRequestRepository purchaseRequests;
  @Mock private InventoryPartRepository parts;
  @Mock private ProcurementService procurementService;
  @Mock private ProcurementInquiryRepository inquiries;
  @Mock private SupplierQuotationRepository quotes;
  @Mock private SupplierQuotationLineRepository quoteLines;
  @InjectMocks private ProcurementGovernanceService service;

  private UUID supplierId;
  private UUID inquiryId;
  private UUID quoteId;
  private Supplier supplier;
  private ProcurementInquiry inquiry;
  private SupplierQuotation quote;
  private CreateContract request;

  @BeforeEach
  void setUp() {
    supplierId = UUID.randomUUID();
    inquiryId = UUID.randomUUID();
    quoteId = UUID.randomUUID();
    supplier = new Supplier();
    supplier.setId(supplierId);
    supplier.setAdmissionStatus("APPROVED");
    supplier.setRiskStatus(SupplierRiskStatus.NORMAL);
    inquiry = new ProcurementInquiry();
    inquiry.setId(inquiryId);
    inquiry.setStatus("AWARDED");
    inquiry.setSelectedQuoteId(quoteId);
    quote = new SupplierQuotation();
    quote.setId(quoteId);
    quote.setInquiryId(inquiryId);
    quote.setSupplierId(supplierId);
    quote.setSelected(true);
    quote.setFreightAmount(BigDecimal.ZERO);
    quote.setOtherCostAmount(BigDecimal.ZERO);
    request = new CreateContract("HT-001", "设备采购合同", supplierId,
        BigDecimal.valueOf(200), inquiryId, quoteId, "CNY", null, null, null, null);

    lenient().when(suppliers.findById(supplierId)).thenReturn(Optional.of(supplier));
    lenient().when(contracts.findFirstByContractNoOrderByVersionNoDesc(anyString())).thenReturn(Optional.empty());
    lenient().when(inquiries.findById(inquiryId)).thenReturn(Optional.of(inquiry));
    lenient().when(quotes.findById(quoteId)).thenReturn(Optional.of(quote));
    lenient().when(quoteLines.findByQuoteIdOrderByCreatedAtAsc(quoteId)).thenReturn(List.of(line(2, 100)));
    lenient().when(contracts.existsByInquiryIdAndStatusNotIn(any(), any())).thenReturn(false);
    lenient().when(contracts.save(any(ProcurementContract.class))).thenAnswer(invocation -> {
      ProcurementContract saved = invocation.getArgument(0);
      if (saved.getId() == null) saved.setId(UUID.randomUUID());
      return saved;
    });
  }

  @Test
  void createsContractFromAwardedQuote() {
    ProcurementContract result = service.createContract(request);

    assertThat(result.getSupplierId()).isEqualTo(supplierId);
    assertThat(result.getInquiryId()).isEqualTo(inquiryId);
    assertThat(result.getSelectedQuoteId()).isEqualTo(quoteId);
    assertThat(result.getAmount()).isEqualByComparingTo("200");
    assertThat(result.getStatus()).isEqualTo("DRAFT");
  }

  @Test
  void rejectsQuoteThatIsNotTheAwardedSupplierQuote() {
    quote.setSelected(false);

    assertThatThrownBy(() -> service.createContract(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("中标报价与供应商或询价单不一致");
  }

  @Test
  void rejectsDuplicateContractForInquiry() {
    when(contracts.existsByInquiryIdAndStatusNotIn(any(), any())).thenReturn(true);

    assertThatThrownBy(() -> service.createContract(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已经建立采购合同");
  }

  @Test
  void rejectsAmountDifferentFromAwardedQuote() {
    CreateContract mismatched = new CreateContract("HT-002", "设备采购合同", supplierId,
        BigDecimal.valueOf(201), inquiryId, quoteId, "CNY", null, null, null, null);

    assertThatThrownBy(() -> service.createContract(mismatched))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("合同金额必须与中标报价总额一致");
  }

  @Test
  void submitsDraftContractForApproval() {
    ProcurementContract contract = contract("HT-003");
    when(contracts.findById(contract.getId())).thenReturn(Optional.of(contract));
    when(contracts.save(contract)).thenReturn(contract);

    ProcurementContract result = service.submitContract(contract.getId());

    assertThat(result.getStatus()).isEqualTo("PENDING_APPROVAL");
    assertThat(result.getApprovalStatus()).isEqualTo("PENDING");
    assertThat(result.getSubmittedByName()).isNotBlank();
  }

  @Test
  void approvingPendingContractActivatesIt() {
    ProcurementContract contract = contract("HT-004");
    contract.setStatus("PENDING_APPROVAL");
    when(contracts.findById(contract.getId())).thenReturn(Optional.of(contract));
    when(contracts.save(contract)).thenReturn(contract);

    ProcurementContract result = service.reviewContract(
        contract.getId(), new ReviewAction("APPROVED", "同意"));

    assertThat(result.getStatus()).isEqualTo("ACTIVE");
    assertThat(result.getApprovalStatus()).isEqualTo("APPROVED");
  }

  @Test
  void rejectingPendingContractMarksItRejected() {
    ProcurementContract contract = contract("HT-005");
    contract.setStatus("PENDING_APPROVAL");
    when(contracts.findById(contract.getId())).thenReturn(Optional.of(contract));
    when(contracts.save(contract)).thenReturn(contract);

    ProcurementContract result = service.reviewContract(
        contract.getId(), new ReviewAction("REJECTED", "金额有误"));

    assertThat(result.getStatus()).isEqualTo("REJECTED");
    assertThat(result.getApprovalStatus()).isEqualTo("REJECTED");
  }

  private SupplierQuotationLine line(int quantity, int unitPrice) {
    SupplierQuotationLine line = new SupplierQuotationLine();
    line.setQuantity(BigDecimal.valueOf(quantity));
    line.setUnitPrice(BigDecimal.valueOf(unitPrice));
    return line;
  }

  private ProcurementContract contract(String contractNo) {
    ProcurementContract contract = new ProcurementContract();
    contract.setId(UUID.randomUUID());
    contract.setContractNo(contractNo);
    contract.setName("设备采购合同");
    contract.setSupplierId(supplierId);
    contract.setAmount(BigDecimal.valueOf(200));
    contract.setStatus("DRAFT");
    contract.setApprovalStatus("PENDING");
    return contract;
  }
}
