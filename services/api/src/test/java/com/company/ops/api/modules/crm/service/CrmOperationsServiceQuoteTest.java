package com.company.ops.api.modules.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.crm.domain.ApprovalDecision;
import com.company.ops.api.modules.crm.domain.QuoteCustomerDecision;
import com.company.ops.api.modules.crm.domain.QuotePlan;
import com.company.ops.api.modules.crm.domain.QuoteRevision;
import com.company.ops.api.modules.crm.domain.QuoteStatus;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ProcessQuoteApprovalRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateQuoteRequest;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.FollowUpRepository;
import com.company.ops.api.modules.crm.repository.OpportunityRepository;
import com.company.ops.api.modules.crm.repository.QuoteApprovalRecordRepository;
import com.company.ops.api.modules.crm.repository.QuotePlanRepository;
import com.company.ops.api.modules.crm.repository.QuoteRevisionRepository;
import com.company.ops.api.modules.crm.repository.ReceivableReceiptRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
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

  @InjectMocks
  private CrmOperationsService crmOperationsService;

  @Test
  void approvingQuoteOnlyCompletesInternalApproval() {
    UUID quoteId = UUID.randomUUID();
    QuotePlan quote = quote(quoteId, QuoteStatus.PENDING_APPROVAL, 2);
    when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
    when(quoteRepository.save(quote)).thenReturn(quote);
    when(quoteApprovalRepository.findFirstByQuoteIdOrderByDecidedAtDesc(quoteId))
        .thenReturn(Optional.empty());
    when(contractRepository.findByQuoteId(quoteId)).thenReturn(Optional.empty());

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

    var version4 = crmOperationsService.updateQuote(
        quoteId,
        new UpdateQuoteRequest(
            "调整后的服务范围",
            "季度服务",
            "签约40%，验收60%",
            new BigDecimal("880000"),
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
}
