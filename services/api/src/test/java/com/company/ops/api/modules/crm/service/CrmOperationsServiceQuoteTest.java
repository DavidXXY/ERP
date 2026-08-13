package com.company.ops.api.modules.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.ApprovalDecision;
import com.company.ops.api.modules.crm.domain.ContractKind;
import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.QuoteCustomerDecision;
import com.company.ops.api.modules.crm.domain.QuoteCostRequest;
import com.company.ops.api.modules.crm.domain.QuoteCostStatus;
import com.company.ops.api.modules.crm.domain.QuotePlan;
import com.company.ops.api.modules.crm.domain.QuoteRevision;
import com.company.ops.api.modules.crm.domain.QuoteStatus;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableReceipt;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ProcessQuoteApprovalRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateChildOrderRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReceivablePlanRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RecordReceiptRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateQuoteRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateContractRequest;
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
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.service.ProjectService;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CrmOperationsServiceQuoteTest {

  private final UUID customerId = UUID.randomUUID();
  private final UUID ownerUserId = UUID.randomUUID();

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
  @Mock
  private DataScopeService dataScopeService;
  @Mock
  private QualificationEmployeeRepository qualificationEmployeeRepository;

  @InjectMocks
  private CrmOperationsService crmOperationsService;

  @org.junit.jupiter.api.BeforeEach
  void allowCustomerAccess() {
    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setCode("KH-001");
    customer.setName("测试客户");
    customer.setOwnerUserId(ownerUserId);
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(dataScopeService.canViewOwner(ownerUserId)).thenReturn(true);
  }

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
    when(dataScopeService.currentActorName()).thenReturn("真实审批人");

    var response = crmOperationsService.processQuoteApproval(
        quoteId,
        new ProcessQuoteApprovalRequest(ApprovalDecision.APPROVED, "同意对外发送", "审批人")
    );

    assertThat(response.status()).isEqualTo(QuoteStatus.APPROVED);
    assertThat(response.convertedContractId()).isNull();
    ArgumentCaptor<com.company.ops.api.modules.crm.domain.QuoteApprovalRecord> approvalCaptor =
        ArgumentCaptor.forClass(com.company.ops.api.modules.crm.domain.QuoteApprovalRecord.class);
    verify(quoteApprovalRepository).save(approvalCaptor.capture());
    assertThat(approvalCaptor.getValue().getApproverName()).isEqualTo("真实审批人");
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
    when(dataScopeService.currentActorName()).thenReturn("真实修改人");

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

  @Test
  void duplicateReceiptRetryIsIdempotent() {
    UUID receivableId = UUID.randomUUID();
    LocalDate receivedDate = LocalDate.of(2026, 8, 4);
    Receivable receivable = receivable(receivableId);
    ReceivableReceipt existing = new ReceivableReceipt();
    existing.setReceivableId(receivableId);
    existing.setAmount(new BigDecimal("250.00"));
    existing.setReceivedDate(receivedDate);
    existing.setReferenceNo("BANK-001");
    when(receivableRepository.findByIdForUpdate(receivableId)).thenReturn(Optional.of(receivable));
    when(receiptRepository.findByReferenceNo("BANK-001")).thenReturn(Optional.of(existing));

    var response = crmOperationsService.recordReceipt(receivableId,
        new RecordReceiptRequest(new BigDecimal("250.0"), receivedDate, " bank-001 ", "伪造登记人"));

    assertThat(response.settledAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    verify(receiptRepository, never()).saveAndFlush(any(ReceivableReceipt.class));
    verify(receivableRepository, never()).save(any(Receivable.class));
  }

  @Test
  void conflictingReceiptReferenceIsRejected() {
    UUID receivableId = UUID.randomUUID();
    Receivable receivable = receivable(receivableId);
    ReceivableReceipt existing = new ReceivableReceipt();
    existing.setReceivableId(UUID.randomUUID());
    existing.setAmount(new BigDecimal("250.00"));
    existing.setReceivedDate(LocalDate.of(2026, 8, 4));
    existing.setReferenceNo("BANK-002");
    when(receivableRepository.findByIdForUpdate(receivableId)).thenReturn(Optional.of(receivable));
    when(receiptRepository.findByReferenceNo("BANK-002")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> crmOperationsService.recordReceipt(receivableId,
        new RecordReceiptRequest(new BigDecimal("250.00"), LocalDate.of(2026, 8, 4),
            "BANK-002", "伪造登记人")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("银行流水号已用于其他回款记录");
  }

  @Test
  void receiptUsesAuthenticatedRecorderAndTranslatesUniqueConflict() {
    UUID receivableId = UUID.randomUUID();
    Receivable receivable = receivable(receivableId);
    when(receivableRepository.findByIdForUpdate(receivableId)).thenReturn(Optional.of(receivable));
    when(dataScopeService.currentActorName()).thenReturn("真实登记人");
    when(receiptRepository.saveAndFlush(any(ReceivableReceipt.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate reference"));

    assertThatThrownBy(() -> crmOperationsService.recordReceipt(receivableId,
        new RecordReceiptRequest(new BigDecimal("250.00"), LocalDate.of(2026, 8, 4),
            " bank-003 ", "伪造登记人")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("银行流水号已用于其他回款记录");

    ArgumentCaptor<ReceivableReceipt> receiptCaptor = ArgumentCaptor.forClass(ReceivableReceipt.class);
    verify(receiptRepository).saveAndFlush(receiptCaptor.capture());
    assertThat(receiptCaptor.getValue().getReferenceNo()).isEqualTo("BANK-003");
    assertThat(receiptCaptor.getValue().getRecorderName()).isEqualTo("真实登记人");
  }

  @Test
  void frameworkChildOrderCreatesReceivableAgainstChildOrder() {
    UUID frameworkId = UUID.randomUUID();
    ServiceContract framework = framework(frameworkId, new BigDecimal("500000"));
    Project parentProject = new Project();
    parentProject.setId(UUID.randomUUID());
    parentProject.setContractId(frameworkId);
    when(contractRepository.findByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
    when(contractRepository.findByParentContractIdOrderByStartDateDesc(frameworkId)).thenReturn(List.of());
    when(codeGenerator.generate("CONTRACT")).thenReturn("HT-ZDD-001");
    when(contractRepository.save(any(ServiceContract.class))).thenAnswer(invocation -> {
      ServiceContract saved = invocation.getArgument(0);
      saved.setId(UUID.randomUUID());
      return saved;
    });
    when(receivableRepository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(projectRepository.findLatestByContractId(frameworkId)).thenReturn(Optional.of(parentProject));

    var response = crmOperationsService.createChildOrder(frameworkId, new CreateChildOrderRequest(
        null, "一期维保订单", "维保订单", new BigDecimal("120000"), new BigDecimal("6"),
        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31), "每月",
        List.of(new ReceivablePlanRequest(null, new BigDecimal("120000"), LocalDate.of(2026, 11, 10)))
    ));

    assertThat(response.contractKind()).isEqualTo(ContractKind.CHILD_ORDER);
    assertThat(response.parentContractId()).isEqualTo(frameworkId);
    ArgumentCaptor<Receivable> captor = ArgumentCaptor.forClass(Receivable.class);
    verify(receivableRepository).save(captor.capture());
    assertThat(captor.getValue().getContractId()).isEqualTo(response.id());
    assertThat(captor.getValue().getAmount()).isEqualByComparingTo("120000");
    verify(projectService).createChildProjectFromOrder(
        any(ServiceContract.class), org.mockito.ArgumentMatchers.eq(parentProject),
        org.mockito.ArgumentMatchers.eq(new BigDecimal("120000")));
  }

  @Test
  void frameworkChildOrdersCannotExceedSpecifiedTotal() {
    UUID frameworkId = UUID.randomUUID();
    ServiceContract framework = framework(frameworkId, new BigDecimal("100000"));
    ServiceContract existing = new ServiceContract();
    existing.setAmount(new BigDecimal("80000"));
    when(contractRepository.findByIdForUpdate(frameworkId)).thenReturn(Optional.of(framework));
    when(contractRepository.findByParentContractIdOrderByStartDateDesc(frameworkId)).thenReturn(List.of(existing));

    assertThatThrownBy(() -> crmOperationsService.createChildOrder(frameworkId, new CreateChildOrderRequest(
        null, "超额订单", "维保订单", new BigDecimal("30000"), null,
        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31), null,
        List.of(new ReceivablePlanRequest(null, new BigDecimal("30000"), LocalDate.of(2026, 11, 10)))
    ))).isInstanceOf(BusinessException.class)
        .hasMessage("子订单累计金额不能超过框架订单总金额");

    verify(contractRepository, never()).save(any(ServiceContract.class));
    verify(receivableRepository, never()).save(any(Receivable.class));
  }

  @Test
  void activeContractCannotBypassChangeApproval() {
    UUID frameworkId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    ServiceContract order = new ServiceContract();
    order.setId(orderId);
    order.setParentContractId(frameworkId);
    order.setContractKind(ContractKind.CHILD_ORDER);
    order.setCustomerId(customerId);
    order.setCode("HT-ZDD-CHANGE");
    order.setProjectName("变更前子订单");
    order.setContractType("维保订单");
    order.setAmount(new BigDecimal("120000"));
    order.setTaxRate(new BigDecimal("6"));
    order.setStartDate(LocalDate.of(2026, 8, 1));
    order.setEndDate(LocalDate.of(2026, 10, 31));
    order.setStatus(ContractStatus.ACTIVE);
    when(contractRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> crmOperationsService.updateContract(orderId, new UpdateContractRequest(
        "变更后子订单", null, new BigDecimal("90000"), null, null, null, null)))
        .isInstanceOf(BusinessException.class).hasMessageContaining("合同变更申请");
    verify(contractRepository, never()).save(any(ServiceContract.class));
  }

  private ServiceContract framework(UUID id, BigDecimal amount) {
    ServiceContract framework = new ServiceContract();
    framework.setId(id);
    framework.setCustomerId(customerId);
    framework.setCode("HT-KJ-001");
    framework.setProjectName("年度框架订单");
    framework.setContractType("框架订单");
    framework.setContractKind(ContractKind.FRAMEWORK);
    framework.setAmount(amount);
    framework.setStatus(ContractStatus.ACTIVE);
    framework.setStartDate(LocalDate.of(2026, 1, 1));
    framework.setEndDate(LocalDate.of(2026, 12, 31));
    return framework;
  }

  private QuotePlan quote(UUID id, QuoteStatus status, int versionNo) {
    QuotePlan quote = new QuotePlan();
    quote.setId(id);
    quote.setCustomerId(customerId);
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

  private Receivable receivable(UUID id) {
    Receivable receivable = new Receivable();
    receivable.setId(id);
    receivable.setCustomerId(customerId);
    receivable.setCode("YS-2026-001");
    receivable.setSourceNo("HT-2026-001");
    receivable.setAmount(new BigDecimal("1000.00"));
    receivable.setSettledAmount(BigDecimal.ZERO);
    receivable.setDueDate(LocalDate.now().plusDays(30));
    receivable.setInvoiceNo("INV-001");
    receivable.setStatus(ReceivableStatus.PAYMENT_PENDING);
    return receivable;
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
