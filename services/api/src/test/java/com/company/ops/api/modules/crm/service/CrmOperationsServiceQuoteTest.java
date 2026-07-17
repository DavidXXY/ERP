package com.company.ops.api.modules.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.ApprovalDecision;
import com.company.ops.api.modules.crm.domain.QuoteCustomerDecision;
import com.company.ops.api.modules.crm.domain.QuoteCostRequest;
import com.company.ops.api.modules.crm.domain.QuoteCostStatus;
import com.company.ops.api.modules.crm.domain.QuotePlan;
import com.company.ops.api.modules.crm.domain.QuoteRevision;
import com.company.ops.api.modules.crm.domain.QuoteStatus;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ProcessQuoteApprovalRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateQuoteRequest;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.CrmAttachmentRepository;
import com.company.ops.api.modules.crm.repository.FollowUpRepository;
import com.company.ops.api.modules.crm.repository.OpportunityRepository;
import com.company.ops.api.modules.crm.repository.QuoteApprovalRecordRepository;
import com.company.ops.api.modules.crm.repository.QuoteCostRequestRepository;
import com.company.ops.api.modules.crm.repository.QuotePlanRepository;
import com.company.ops.api.modules.crm.repository.QuoteRevisionRepository;
import com.company.ops.api.modules.crm.repository.ReceivableReceiptRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.service.ProjectService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrmOperationsServiceQuoteTest {

  @Mock
  private CustomerRepository customerRepository;
  @Mock
  private OpportunityRepository opportunityRepository;
  @Mock
  private QuotePlanRepository quoteRepository;
  @Mock
  private QuoteApprovalRecordRepository quoteApprovalRepository;
  @Mock
  private QuoteCostRequestRepository quoteCostRequestRepository;
  @Mock
  private QuoteRevisionRepository quoteRevisionRepository;
  @Mock
  private FollowUpRepository followUpRepository;
  @Mock
  private ServiceContractRepository contractRepository;
  @Mock
  private ReceivableRepository receivableRepository;
  @Mock
  private ReceivableReceiptRepository receiptRepository;
  @Mock
  private LedgerService ledgerService;
  @Mock
  private DeleteGovernanceService deleteGovernanceService;
  @Mock
  private ProjectService projectService;
  @Mock
  private ProjectRepository projectRepository;
  @Mock
  private CrmAttachmentRepository attachmentRepository;
  @Mock
  private CodeGenerator codeGenerator;

  @InjectMocks
  private CrmOperationsService crmOperationsService;

  @Test
  void submitQuoteRequiresConfirmedBudget() {
    UUID quoteId = UUID.randomUUID();
    QuotePlan quote = quote(quoteId, QuoteStatus.DRAFT, 1);
    quote.setLaborBudget(BigDecimal.ZERO);
    quote.setMaterialBudget(BigDecimal.ZERO);
    quote.setSubcontractBudget(BigDecimal.ZERO);
    quote.setTravelBudget(BigDecimal.ZERO);
    quote.setOtherBudget(BigDecimal.ZERO);
    when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

    assertThatThrownBy(() -> crmOperationsService.submitQuote(quoteId))
        .isInstanceOf(BusinessException.class)
        .hasMessage("售前成本已核对并审批通过后，才可以提交报价审批");

    verify(quoteRepository, never()).save(any(QuotePlan.class));
  }

  @Test
  void submitQuoteAllowsGrossMarginBelowFifteenPercent() {
    UUID quoteId = UUID.randomUUID();
    QuotePlan quote = quote(quoteId, QuoteStatus.COST_APPROVED, 1);
    quote.setLaborBudget(new BigDecimal("300000"));
    quote.setMaterialBudget(new BigDecimal("300000"));
    quote.setSubcontractBudget(new BigDecimal("100000"));
    quote.setTravelBudget(new BigDecimal("20000"));
    quote.setOtherBudget(BigDecimal.ZERO);
    when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
    when(quoteRepository.save(quote)).thenReturn(quote);
    when(quoteApprovalRepository.findFirstByQuoteIdOrderByDecidedAtDesc(quoteId))
        .thenReturn(Optional.empty());
    when(contractRepository.findByQuoteId(quoteId)).thenReturn(Optional.empty());
    when(quoteCostRequestRepository.findFirstByQuoteIdOrderByCreatedAtDesc(quoteId))
        .thenReturn(Optional.of(approvedCost(quoteId)));

    var response = crmOperationsService.submitQuote(quoteId);

    assertThat(response.status()).isEqualTo(QuoteStatus.PENDING_APPROVAL);
    assertThat(response.netAmount()).isEqualByComparingTo(new BigDecimal("707964.60"));
    assertThat(response.budgetAmount()).isEqualByComparingTo(new BigDecimal("720000"));
    assertThat(response.grossMargin()).isEqualByComparingTo(new BigDecimal("-12035.40"));
    verify(quoteRepository).save(quote);
  }

  @Test
  void approvingQuoteOnlyCompletesInternalApproval() {
    UUID quoteId = UUID.randomUUID();
    QuotePlan quote = quote(quoteId, QuoteStatus.PENDING_APPROVAL, 2);
    when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
    when(quoteRepository.save(quote)).thenReturn(quote);
    when(quoteApprovalRepository.findFirstByQuoteIdOrderByDecidedAtDesc(quoteId))
        .thenReturn(Optional.empty());
    when(contractRepository.findByQuoteId(quoteId)).thenReturn(Optional.empty());
    when(quoteCostRequestRepository.findFirstByQuoteIdOrderByCreatedAtDesc(quoteId))
        .thenReturn(Optional.empty());

    var response = crmOperationsService.processQuoteApproval(
        quoteId,
        new ProcessQuoteApprovalRequest(ApprovalDecision.APPROVED, "同意对外发送", "审批人")
    );

    assertThat(response.status()).isEqualTo(QuoteStatus.APPROVED);
    assertThat(response.convertedContractId()).isNull();
    verify(contractRepository, never()).save(any(ServiceContract.class));
    verify(receivableRepository, never()).save(any(Receivable.class));
    verify(opportunityRepository, never()).save(any());
  }

  @Test
  void quoteCanBeRevisedMultipleTimesAfterCustomerDeclines() {
    UUID quoteId = UUID.randomUUID();
    QuotePlan quote = quote(quoteId, QuoteStatus.CUSTOMER_DECLINED, 3);
    quote.setCustomerDecision(QuoteCustomerDecision.DECLINED);
    quote.setCustomerComment("客户要求调整范围");
    when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
    when(quoteRepository.save(any(QuotePlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(quoteApprovalRepository.findFirstByQuoteIdOrderByDecidedAtDesc(quoteId))
        .thenReturn(Optional.empty());
    when(contractRepository.findByQuoteId(quoteId)).thenReturn(Optional.empty());
    when(quoteCostRequestRepository.findFirstByQuoteIdOrderByCreatedAtDesc(quoteId))
        .thenReturn(Optional.empty());

    var version4 = crmOperationsService.updateQuote(
        quoteId,
        new UpdateQuoteRequest(
            "调整后的服务范围",
            "季度服务",
            "签约40%，验收60%",
            new BigDecimal("880000"),
            new BigDecimal("13"),
            new BigDecimal("160000"),
            new BigDecimal("280000"),
            new BigDecimal("80000"),
            new BigDecimal("24000"),
            new BigDecimal("16000"),
            "按客户意见调整服务范围",
            "客户经理A"
        )
    );
    var version5 = crmOperationsService.updateQuote(
        quoteId,
        new UpdateQuoteRequest(
            "最终服务范围",
            "月度服务",
            "签约30%，验收70%",
            new BigDecimal("920000"),
            new BigDecimal("13"),
            new BigDecimal("180000"),
            new BigDecimal("300000"),
            new BigDecimal("90000"),
            new BigDecimal("27000"),
            new BigDecimal("18000"),
            "增加月度服务并更新价格",
            "客户经理A"
        )
    );

    assertThat(version4.versionNo()).isEqualTo(4);
    assertThat(version5.versionNo()).isEqualTo(5);
    assertThat(version5.status()).isEqualTo(QuoteStatus.DRAFT);
    assertThat(version5.customerDecision()).isNull();

    ArgumentCaptor<QuoteRevision> revisionCaptor = ArgumentCaptor.forClass(QuoteRevision.class);
    verify(quoteRevisionRepository, org.mockito.Mockito.times(2)).save(revisionCaptor.capture());
    assertThat(revisionCaptor.getAllValues())
        .extracting(QuoteRevision::getVersionNo)
        .containsExactly(4, 5);
  }

  private QuotePlan quote(UUID id, QuoteStatus status, int versionNo) {
    QuotePlan quote = new QuotePlan();
    quote.setId(id);
    quote.setCode("BJ-2026-001");
    quote.setServiceScope("年度服务");
    quote.setInspectCycle("季度服务");
    quote.setPaymentNodes("签约30%，验收70%");
    quote.setAmount(new BigDecimal("800000"));
    quote.setLaborBudget(new BigDecimal("160000"));
    quote.setMaterialBudget(new BigDecimal("280000"));
    quote.setSubcontractBudget(new BigDecimal("80000"));
    quote.setTravelBudget(new BigDecimal("24000"));
    quote.setOtherBudget(new BigDecimal("16000"));
    quote.setVersionNo(versionNo);
    quote.setStatus(status);
    return quote;
  }

  private QuoteCostRequest approvedCost(UUID quoteId) {
    QuoteCostRequest cost = new QuoteCostRequest();
    cost.setId(UUID.randomUUID());
    cost.setQuoteId(quoteId);
    cost.setStatus(QuoteCostStatus.APPROVED);
    cost.setRequestedBy("销售");
    cost.setRequestedAt(java.time.OffsetDateTime.now());
    cost.setProjectManager("项目负责人");
    cost.setLaborCost(new BigDecimal("300000"));
    cost.setMaterialCost(new BigDecimal("300000"));
    cost.setSubcontractCost(new BigDecimal("100000"));
    cost.setTravelCost(new BigDecimal("20000"));
    cost.setEquipmentCost(BigDecimal.ZERO);
    cost.setRiskReserve(BigDecimal.ZERO);
    cost.setOtherCost(BigDecimal.ZERO);
    return cost;
  }
}
