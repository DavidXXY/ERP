package com.company.ops.api.modules.crm.service;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.ApprovalDecision;
import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.FollowUp;
import com.company.ops.api.modules.crm.domain.Opportunity;
import com.company.ops.api.modules.crm.domain.ContractChangeRequest;
import com.company.ops.api.modules.crm.domain.OpportunityStage;
import com.company.ops.api.modules.crm.domain.QuoteCustomerDecision;
import com.company.ops.api.modules.crm.domain.QuoteCostRequest;
import com.company.ops.api.modules.crm.domain.QuoteCostStatus;
import com.company.ops.api.modules.crm.domain.QuotePlan;
import com.company.ops.api.modules.crm.domain.QuoteApprovalRecord;
import com.company.ops.api.modules.crm.domain.QuoteRevision;
import com.company.ops.api.modules.crm.domain.QuoteStatus;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.domain.ReceivableReceipt;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.AdvanceOpportunityRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ContractResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ConvertQuoteRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateFollowUpRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateOpportunityRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateQuoteRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CustomerProfileResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.FollowUpResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.OpportunityResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ProcessQuoteApprovalRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ProcessQuoteCustomerResultRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ApproveQuoteCostRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteConversionResult;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteCostRequestResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateReceivableRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteRevisionResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReceivableResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RecordReceiptRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RegisterInvoiceRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RequestQuoteCostRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RenewalResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.SubmitQuoteCostRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateQuoteRequest;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.FollowUpRepository;
import com.company.ops.api.modules.crm.repository.ContractChangeRequestRepository;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ContractChangeResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateContractChangeRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ApprovalActionRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateContractRequest;


import com.company.ops.api.modules.crm.repository.OpportunityRepository;
import com.company.ops.api.modules.crm.repository.QuotePlanRepository;
import com.company.ops.api.modules.crm.repository.QuoteApprovalRecordRepository;
import com.company.ops.api.modules.crm.repository.QuoteCostRequestRepository;
import com.company.ops.api.modules.crm.repository.QuoteRevisionRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ReceivableReceiptRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.service.LedgerService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrmOperationsService {

  private CodeGenerator codeGenerator;
  private final CustomerRepository customerRepository;
  private final OpportunityRepository opportunityRepository;
  private final QuotePlanRepository quoteRepository;
  private final QuoteApprovalRecordRepository quoteApprovalRepository;
  private final QuoteCostRequestRepository quoteCostRequestRepository;
  private final QuoteRevisionRepository quoteRevisionRepository;
  private final FollowUpRepository followUpRepository;
  private final ServiceContractRepository contractRepository;
  private final ReceivableRepository receivableRepository;
  private final ReceivableReceiptRepository receiptRepository;
  private final LedgerService ledgerService;
  private final DeleteGovernanceService deleteGovernanceService;

  @jakarta.persistence.PersistenceContext
  private jakarta.persistence.EntityManager entityManager;

  @org.springframework.beans.factory.annotation.Autowired
  private ContractChangeRequestRepository changeRepository;

  public CrmOperationsService(CustomerRepository customerRepository,
      OpportunityRepository opportunityRepository,
      QuotePlanRepository quoteRepository,
      QuoteApprovalRecordRepository quoteApprovalRepository,
      QuoteCostRequestRepository quoteCostRequestRepository,
      QuoteRevisionRepository quoteRevisionRepository,
      FollowUpRepository followUpRepository,
      ServiceContractRepository contractRepository,
      ReceivableRepository receivableRepository,
      ReceivableReceiptRepository receiptRepository,
      LedgerService ledgerService,
      DeleteGovernanceService deleteGovernanceService,
                               CodeGenerator codeGenerator) {
    this.codeGenerator = codeGenerator;
    this.customerRepository = customerRepository;
    this.opportunityRepository = opportunityRepository;
    this.quoteRepository = quoteRepository;
    this.quoteApprovalRepository = quoteApprovalRepository;
    this.quoteCostRequestRepository = quoteCostRequestRepository;
    this.quoteRevisionRepository = quoteRevisionRepository;
    this.followUpRepository = followUpRepository;
    this.contractRepository = contractRepository;
    this.receivableRepository = receivableRepository;
    this.receiptRepository = receiptRepository;
    this.ledgerService = ledgerService;
    this.deleteGovernanceService = deleteGovernanceService;
  }

  @Transactional(readOnly = true)
  public List<OpportunityResponse> listOpportunities() {
    List<Opportunity> opportunities = deleteGovernanceService.visible("OPPORTUNITY", opportunityRepository.findAllByOrderByUpdatedAtDesc(), Opportunity::getId);
    Map<UUID, Customer> customers = customerMap(opportunities.stream()
        .map(Opportunity::getCustomerId)
        .toList());
    return opportunities.stream().map(item -> toOpportunity(item, customers)).toList();
  }

  @Transactional(readOnly = true)
  public OpportunityResponse getOpportunity(UUID id) {
    Opportunity opportunity = opportunityRepository.findById(id)
        .orElseThrow(() -> new BusinessException("商机不存在"));
    if (deleteGovernanceService.isHidden("OPPORTUNITY", id)) throw new BusinessException("商机不存在");
    return toOpportunity(opportunity, customerMap(nullableId(opportunity.getCustomerId())));
  }

  @Transactional
  public OpportunityResponse createOpportunity(CreateOpportunityRequest request) {
    String oppCode = request.code() != null ? request.code() : codeGenerator.generate("OPPORTUNITY");
    if (opportunityRepository.existsByCode(oppCode)) {
      throw new BusinessException("商机编码已存在");
    }
    validateCustomer(request.customerId());

    Opportunity opportunity = new Opportunity();
    opportunity.setCustomerId(request.customerId());
    opportunity.setCode(oppCode);
    opportunity.setSource(request.source());
    opportunity.setNeedSummary(request.needSummary());
    opportunity.setStage(request.stage() == null ? OpportunityStage.LEAD : request.stage());
    opportunity.setExpectedAmount(defaultAmount(request.expectedAmount()));
    opportunity.setProbability(request.probability() == null ? 10 : request.probability());
    opportunity.setNextAction(request.nextAction());
    opportunity.setNextActionAt(request.nextActionAt());
    opportunity.setOwnerName(request.ownerName());
    Opportunity saved = opportunityRepository.save(opportunity);
    return toOpportunity(saved, customerMap(nullableId(saved.getCustomerId())));
  }

  @Transactional
  public OpportunityResponse advanceOpportunity(UUID id, AdvanceOpportunityRequest request) {
    Opportunity opportunity = opportunityRepository.findById(id)
        .orElseThrow(() -> new BusinessException("商机不存在"));
    if (opportunity.getStage() == OpportunityStage.WON || opportunity.getStage() == OpportunityStage.LOST) {
      throw new BusinessException("已结束商机不能继续推进");
    }
    if ((request.stage() == OpportunityStage.NEGOTIATION || request.stage() == OpportunityStage.WON)
        && !quoteRepository.existsByOpportunityId(opportunity.getId())) {
      throw new BusinessException("请先为该商机创建报价方案才能继续推进");
    }
    opportunity.setStage(request.stage());
    opportunity.setNextAction(request.nextAction());
    opportunity.setNextActionAt(request.nextActionAt());
    opportunity.setProbability(request.probability());
    Opportunity saved = opportunityRepository.save(opportunity);
    return toOpportunity(saved, customerMap(nullableId(saved.getCustomerId())));
  }

  @Transactional(readOnly = true)
  public List<QuoteResponse> listQuotes() {
    List<QuotePlan> quotes = deleteGovernanceService.visible("QUOTE", quoteRepository.findAllByOrderByUpdatedAtDesc(), QuotePlan::getId);
    Map<UUID, Customer> customers = customerMap(quotes.stream().map(QuotePlan::getCustomerId).toList());
    Map<UUID, Opportunity> opportunities = opportunityMap(quotes.stream()
        .map(QuotePlan::getOpportunityId)
        .toList());
    return quotes.stream().map(item -> toQuote(item, customers, opportunities)).toList();
  }

  @Transactional(readOnly = true)
  public QuoteResponse getQuote(UUID id) {
    QuotePlan quote = quoteRepository.findById(id)
        .orElseThrow(() -> new BusinessException("报价方案不存在"));
    if (deleteGovernanceService.isHidden("QUOTE", id)) throw new BusinessException("报价不存在");
    return toQuote(
        quote,
        customerMap(nullableId(quote.getCustomerId())),
        opportunityMap(nullableId(quote.getOpportunityId()))
    );
  }

  @Transactional
  public QuoteResponse createQuote(CreateQuoteRequest request) {
    validateCustomer(request.customerId());
    Opportunity opportunity = null;
    if (request.opportunityId() != null) {
      opportunity = opportunityRepository.findById(request.opportunityId())
          .orElseThrow(() -> new BusinessException("关联商机不存在"));
      if (request.customerId() != null && opportunity.getCustomerId() != null
          && !request.customerId().equals(opportunity.getCustomerId())) {
        throw new BusinessException("报价客户与关联商机客户不一致");
      }
    }

    String quoteCode;
    if (request.code() != null) {
      quoteCode = request.code();
    } else if (opportunity != null && opportunity.getCode() != null) {
      quoteCode = deriveCode(opportunity.getCode(), "BJ");
    } else {
      quoteCode = codeGenerator.generate("QUOTE");
    }
    if (quoteRepository.existsByCode(quoteCode)) {
      throw new BusinessException("报价编码已存在");
    }

    UUID customerId = request.customerId() == null && opportunity != null
        ? opportunity.getCustomerId()
        : request.customerId();
    QuotePlan quote = new QuotePlan();
    quote.setCustomerId(customerId);
    quote.setOpportunityId(request.opportunityId());
    quote.setCode(quoteCode);
    quote.setServiceScope(request.serviceScope());
    quote.setInspectCycle(request.inspectCycle());
    quote.setPaymentNodes(request.paymentNodes());
    quote.setAmount(defaultAmount(request.amount()));
    quote.setTaxRate(defaultTaxRate(request.taxRate()));
    applyQuoteBudget(quote, request.laborBudget(), request.materialBudget(), request.subcontractBudget(), request.travelBudget(), request.otherBudget());
    quote.setVersionNo(1);
    quote.setStatus(QuoteStatus.DRAFT);
    QuotePlan saved = quoteRepository.save(quote);
    saveQuoteRevision(saved, "首次创建", request.editorName());
    Map<UUID, Customer> customers = customerMap(nullableId(saved.getCustomerId()));
    Map<UUID, Opportunity> opportunities = opportunity == null
        ? Map.of()
        : Map.of(opportunity.getId(), opportunity);
    return toQuote(saved, customers, opportunities);
  }

  @Transactional
  public QuoteResponse updateQuote(UUID id, UpdateQuoteRequest request) {
    QuotePlan quote = quoteRepository.findById(id)
        .orElseThrow(() -> new BusinessException("报价方案不存在"));
    if (quote.getStatus() == QuoteStatus.PENDING_APPROVAL) {
      throw new BusinessException("审批中的报价不能修改，请先完成审批");
    }
    if (quote.getStatus() == QuoteStatus.CONVERTED) {
      throw new BusinessException("已转合同的报价不能修改");
    }

    quote.setServiceScope(request.serviceScope());
    quote.setInspectCycle(request.inspectCycle());
    quote.setPaymentNodes(request.paymentNodes());
    quote.setAmount(defaultAmount(request.amount()));
    quote.setTaxRate(defaultTaxRate(request.taxRate()));
    QuoteCostRequest approvedCost = latestApprovedQuoteCost(quote.getId());
    if (approvedCost == null) {
      applyQuoteBudget(quote, request.laborBudget(), request.materialBudget(), request.subcontractBudget(), request.travelBudget(), request.otherBudget());
    } else {
      applyApprovedCostToQuote(quote, approvedCost);
    }
    quote.setVersionNo(Math.max(quote.getVersionNo(), 1) + 1);
    quote.setStatus(approvedCost == null ? QuoteStatus.DRAFT : QuoteStatus.COST_APPROVED);
    clearCustomerResult(quote);
    QuotePlan saved = quoteRepository.save(quote);
    saveQuoteRevision(saved, request.revisionNote(), request.editorName());
    return toQuote(
        saved,
        customerMap(nullableId(saved.getCustomerId())),
        opportunityMap(nullableId(saved.getOpportunityId()))
    );
  }

  @Transactional(readOnly = true)
  public List<QuoteRevisionResponse> listQuoteRevisions(UUID id) {
    if (!quoteRepository.existsById(id)) {
      throw new BusinessException("报价方案不存在");
    }
    return quoteRevisionRepository.findByQuoteIdOrderByVersionNoDesc(id).stream()
        .map(this::toQuoteRevision)
        .toList();
  }

  @Transactional
  public QuoteCostRequestResponse requestQuoteCost(UUID id, RequestQuoteCostRequest request) {
    QuotePlan quote = quoteRepository.findById(id)
        .orElseThrow(() -> new BusinessException("报价方案不存在"));
    if (quote.getStatus() == QuoteStatus.PENDING_APPROVAL || quote.getStatus() == QuoteStatus.APPROVED
        || quote.getStatus() == QuoteStatus.CUSTOMER_ACCEPTED || quote.getStatus() == QuoteStatus.CONVERTED) {
      throw new BusinessException("当前报价状态不能再发起项目询价");
    }
    quoteCostRequestRepository.findFirstByQuoteIdOrderByCreatedAtDesc(id).ifPresent(existing -> {
      if (existing.getStatus() == QuoteCostStatus.REQUESTED || existing.getStatus() == QuoteCostStatus.SUBMITTED) {
        throw new BusinessException("该报价已有待处理的项目询价单");
      }
    });

    QuoteCostRequest cost = new QuoteCostRequest();
    cost.setQuoteId(quote.getId());
    cost.setOpportunityId(quote.getOpportunityId());
    cost.setCustomerId(quote.getCustomerId());
    cost.setStatus(QuoteCostStatus.REQUESTED);
    cost.setRequestedBy(request.requestedBy());
    cost.setRequestedAt(OffsetDateTime.now());
    QuoteCostRequest saved = quoteCostRequestRepository.save(cost);
    quote.setStatus(QuoteStatus.COST_REQUESTED);
    quoteRepository.save(quote);
    return toQuoteCostRequest(saved);
  }

  @Transactional(readOnly = true)
  public List<QuoteCostRequestResponse> listQuoteCostRequests(UUID id) {
    if (!quoteRepository.existsById(id)) {
      throw new BusinessException("报价方案不存在");
    }
    return quoteCostRequestRepository.findByQuoteIdOrderByCreatedAtDesc(id).stream()
        .map(this::toQuoteCostRequest)
        .toList();
  }

  @Transactional
  public QuoteCostRequestResponse submitQuoteCost(UUID id, SubmitQuoteCostRequest request) {
    QuoteCostRequest cost = quoteCostRequestRepository.findById(id)
        .orElseThrow(() -> new BusinessException("项目询价单不存在"));
    if (cost.getStatus() == QuoteCostStatus.APPROVED) {
      throw new BusinessException("已审批通过的成本单不能重新填写");
    }
    QuotePlan quote = quoteRepository.findById(cost.getQuoteId())
        .orElseThrow(() -> new BusinessException("报价方案不存在"));
    cost.setProjectManager(request.projectManager());
    cost.setLaborCost(defaultAmount(request.laborCost()));
    cost.setLaborTaxRate(defaultCostTaxRate(request.laborTaxRate(), BigDecimal.valueOf(6)));
    cost.setMaterialCost(defaultAmount(request.materialCost()));
    cost.setMaterialTaxRate(defaultCostTaxRate(request.materialTaxRate(), BigDecimal.valueOf(13)));
    cost.setSubcontractCost(defaultAmount(request.subcontractCost()));
    cost.setSubcontractTaxRate(defaultCostTaxRate(request.subcontractTaxRate(), BigDecimal.valueOf(6)));
    cost.setTravelCost(defaultAmount(request.travelCost()));
    cost.setEquipmentCost(defaultAmount(request.equipmentCost()));
    cost.setEquipmentTaxRate(defaultCostTaxRate(request.equipmentTaxRate(), BigDecimal.valueOf(13)));
    cost.setRiskReserve(defaultAmount(request.riskReserve()));
    cost.setOtherCost(defaultAmount(request.otherCost()));
    cost.setSuggestedPrice(defaultAmount(request.suggestedPrice()));
    cost.setCostRemark(request.costRemark());
    cost.setSubmittedAt(OffsetDateTime.now());
    cost.setStatus(QuoteCostStatus.SUBMITTED);
    quote.setStatus(QuoteStatus.COSTING);
    quoteRepository.save(quote);
    return toQuoteCostRequest(quoteCostRequestRepository.save(cost));
  }

  @Transactional
  public QuoteCostRequestResponse approveQuoteCost(UUID id, ApproveQuoteCostRequest request) {
    QuoteCostRequest cost = quoteCostRequestRepository.findById(id)
        .orElseThrow(() -> new BusinessException("项目询价单不存在"));
    if (cost.getStatus() != QuoteCostStatus.SUBMITTED) {
      throw new BusinessException("只有已提交的成本单可以审批");
    }
    QuotePlan quote = quoteRepository.findById(cost.getQuoteId())
        .orElseThrow(() -> new BusinessException("报价方案不存在"));

    cost.setApprovedBy(request.approverName());
    cost.setApprovedAt(OffsetDateTime.now());
    cost.setApprovalComment(request.comment());
    if (request.decision() == ApprovalDecision.APPROVED) {
      cost.setStatus(QuoteCostStatus.APPROVED);
      applyApprovedCostToQuote(quote, cost);
      quote.setStatus(QuoteStatus.COST_APPROVED);
    } else {
      cost.setStatus(QuoteCostStatus.REJECTED);
      quote.setStatus(QuoteStatus.COST_REQUESTED);
    }
    quoteRepository.save(quote);
    return toQuoteCostRequest(quoteCostRequestRepository.save(cost));
  }

  @Transactional
  public QuoteResponse submitQuote(UUID id) {
    QuotePlan quote = quoteRepository.findById(id)
        .orElseThrow(() -> new BusinessException("报价方案不存在"));
    if (quote.getStatus() != QuoteStatus.DRAFT && quote.getStatus() != QuoteStatus.COST_APPROVED) {
      throw new BusinessException("只有草稿版本可以提交审批");
    }
    validateQuoteBudgetForSubmit(quote);
    quote.setStatus(QuoteStatus.PENDING_APPROVAL);
    QuotePlan saved = quoteRepository.save(quote);
    return toQuote(
        saved,
        customerMap(nullableId(saved.getCustomerId())),
        opportunityMap(nullableId(saved.getOpportunityId()))
    );
  }

  @Transactional
  public QuoteResponse processQuoteApproval(UUID id, ProcessQuoteApprovalRequest request) {
    QuotePlan quote = quoteRepository.findById(id)
        .orElseThrow(() -> new BusinessException("报价方案不存在"));
    if (quote.getStatus() != QuoteStatus.PENDING_APPROVAL) {
      throw new BusinessException("只有审批中的报价可以处理");
    }

    if (request.decision() == ApprovalDecision.APPROVED) {
      quote.setStatus(QuoteStatus.APPROVED);
    } else {
      quote.setStatus(QuoteStatus.REJECTED);
    }
    quoteRepository.save(quote);

    QuoteApprovalRecord approval = new QuoteApprovalRecord();
    approval.setQuoteId(quote.getId());
    approval.setQuoteVersion(quote.getVersionNo());
    approval.setDecision(request.decision());
    approval.setComment(request.comment());
    approval.setApproverName(request.approverName());
    approval.setDecidedAt(OffsetDateTime.now());
    quoteApprovalRepository.save(approval);

    Map<UUID, Customer> customers = customerMap(nullableId(quote.getCustomerId()));
    Map<UUID, Opportunity> opportunities = opportunityMap(nullableId(quote.getOpportunityId()));
    return toQuote(quote, customers, opportunities);
  }

  @Transactional
  public QuoteResponse processQuoteCustomerResult(
      UUID id,
      ProcessQuoteCustomerResultRequest request
  ) {
    QuotePlan quote = quoteRepository.findById(id)
        .orElseThrow(() -> new BusinessException("报价方案不存在"));
    if (quote.getStatus() != QuoteStatus.APPROVED) {
      throw new BusinessException("只有内部审批通过的报价可以登记客户结果");
    }
    quote.setCustomerDecision(request.decision());
    quote.setCustomerComment(request.comment());
    quote.setCustomerDecisionBy(request.operatorName());
    quote.setCustomerDecidedAt(OffsetDateTime.now());
    quote.setStatus(request.decision() == QuoteCustomerDecision.ACCEPTED
        ? QuoteStatus.CUSTOMER_ACCEPTED
        : QuoteStatus.CUSTOMER_DECLINED);
    QuotePlan saved = quoteRepository.save(quote);
    return toQuote(
        saved,
        customerMap(nullableId(saved.getCustomerId())),
        opportunityMap(nullableId(saved.getOpportunityId()))
    );
  }

  @Transactional
  public QuoteConversionResult convertQuote(UUID id, ConvertQuoteRequest request) {
    QuotePlan quote = quoteRepository.findById(id)
        .orElseThrow(() -> new BusinessException("报价方案不存在"));
    if (quote.getStatus() != QuoteStatus.CUSTOMER_ACCEPTED) {
      throw new BusinessException("只有客户已接受的报价可以转为合同");
    }

    ServiceContract contract = createContractFromAcceptedQuote(quote, request);
    Receivable receivable = createFirstReceivable(quote, contract, request);
    markOpportunityWon(quote, contract);
    quote.setStatus(QuoteStatus.CONVERTED);
    quoteRepository.save(quote);

    Map<UUID, Customer> customers = customerMap(nullableId(quote.getCustomerId()));
    Map<UUID, Opportunity> opportunities = opportunityMap(nullableId(quote.getOpportunityId()));
    return new QuoteConversionResult(
        toQuote(quote, customers, opportunities),
        toContract(contract, customers),
        receivable == null ? null : toReceivable(receivable, customers, Map.of(contract.getId(), contract))
    );
  }

  @Transactional(readOnly = true)
  public List<ContractResponse> listContracts() {
    List<ServiceContract> contracts = deleteGovernanceService.visible("CONTRACT", contractRepository.findAllByOrderByEndDateAsc(), ServiceContract::getId);
    Map<UUID, Customer> customers = customerMap(contracts.stream()
        .map(ServiceContract::getCustomerId)
        .toList());
    return contracts.stream().map(item -> toContract(item, customers)).toList();
  }

  @Transactional(readOnly = true)
  public ContractResponse getContract(UUID id) {
    ServiceContract contract = contractRepository.findById(id)
        .orElseThrow(() -> new BusinessException("合同不存在"));
    if (deleteGovernanceService.isHidden("CONTRACT", id)) throw new BusinessException("合同不存在");
    return toContract(contract, customerMap(nullableId(contract.getCustomerId())));
  }

  @Transactional(readOnly = true)
  public List<ReceivableResponse> listReceivables() {
    List<Receivable> receivables = receivableRepository.findAllByOrderByDueDateAsc();
    Map<UUID, Customer> customers = customerMap(receivables.stream()
        .map(Receivable::getCustomerId)
        .toList());
    Map<UUID, ServiceContract> contracts = contractMap(receivables.stream()
        .map(Receivable::getContractId)
        .toList());
    return receivables.stream().map(item -> toReceivable(item, customers, contracts)).toList();
  }

  @Transactional
  public ReceivableResponse registerInvoice(UUID id, RegisterInvoiceRequest request) {
    Receivable receivable = receivableRepository.findById(id)
        .orElseThrow(() -> new BusinessException("应收单不存在"));
    if (receivable.getStatus() == ReceivableStatus.SETTLED) {
      throw new BusinessException("已核销应收不能登记发票");
    }
    if (receivable.getInvoiceNo() != null && !receivable.getInvoiceNo().isBlank()) {
      throw new BusinessException("该应收已登记发票");
    }
    receivable.setInvoiceNo(request.invoiceNo());
    receivable.setInvoiceDate(request.invoiceDate());
    receivable.setStatus(receivable.getDueDate().isBefore(LocalDate.now())
        ? ReceivableStatus.OVERDUE
        : ReceivableStatus.PAYMENT_PENDING);
    Receivable saved = receivableRepository.save(receivable);
    return mapReceivable(saved);
  }

  @Transactional
  public ReceivableResponse recordReceipt(UUID id, RecordReceiptRequest request) {
    Receivable receivable = receivableRepository.findById(id)
        .orElseThrow(() -> new BusinessException("应收单不存在"));
    if (receivable.getStatus() == ReceivableStatus.SETTLED) {
      throw new BusinessException("该应收已全部核销");
    }
    if (receivable.getInvoiceNo() == null || receivable.getInvoiceNo().isBlank()) {
      throw new BusinessException("请先登记开票信息");
    }
    BigDecimal newSettledAmount = defaultAmount(receivable.getSettledAmount()).add(request.amount());
    if (newSettledAmount.compareTo(defaultAmount(receivable.getAmount())) > 0) {
      throw new BusinessException("本次回款超过剩余应收金额");
    }

    ReceivableReceipt receipt = new ReceivableReceipt();
    receipt.setReceivableId(receivable.getId());
    receipt.setAmount(request.amount());
    receipt.setReceivedDate(request.receivedDate());
    receipt.setReferenceNo(request.referenceNo());
    receipt.setRecorderName(request.recorderName());
    ReceivableReceipt savedReceipt = receiptRepository.save(receipt);

    receivable.setSettledAmount(newSettledAmount);
    if (newSettledAmount.compareTo(defaultAmount(receivable.getAmount())) >= 0) {
      receivable.setStatus(ReceivableStatus.SETTLED);
    } else {
      receivable.setStatus(receivable.getDueDate().isBefore(LocalDate.now())
          ? ReceivableStatus.OVERDUE
          : ReceivableStatus.PAYMENT_PENDING);
    }
    Receivable saved = receivableRepository.save(receivable);
    ledgerService.post("RECEIPT", savedReceipt.getId().toString(), request.receivedDate(),
        "客户回款 " + receivable.getCode(), List.of(
            new PostingLine("1002", "银行存款", request.amount(), BigDecimal.ZERO, request.referenceNo()),
            new PostingLine("1122", "应收账款", BigDecimal.ZERO, request.amount(), receivable.getCode())
        ));
    return mapReceivable(saved);
  }

  @Transactional(readOnly = true)
  public List<RenewalResponse> listRenewals() {
    LocalDate today = LocalDate.now();
    LocalDate horizon = today.plusDays(365);
    List<ServiceContract> contracts = contractRepository.findAllByOrderByEndDateAsc().stream()
        .filter(item -> item.getStatus() != ContractStatus.CLOSED)
        .filter(item -> !item.getEndDate().isAfter(horizon) || item.getStatus() != ContractStatus.ACTIVE)
        .toList();
    Map<UUID, Customer> customers = customerMap(contracts.stream()
        .map(ServiceContract::getCustomerId)
        .toList());
    Map<UUID, BigDecimal> outstandingByContract = receivableRepository.findAll().stream()
        .filter(item -> item.getContractId() != null && item.getStatus() != ReceivableStatus.SETTLED)
        .collect(Collectors.toMap(
            Receivable::getContractId,
            this::outstandingAmount,
            BigDecimal::add
        ));

    return contracts.stream().map(item -> {
      long days = ChronoUnit.DAYS.between(today, item.getEndDate());
      return new RenewalResponse(
          item.getId(),
          item.getCustomerId(),
          customerName(customers, item.getCustomerId()),
          item.getCode(),
          item.getProjectName(),
          item.getAmount(),
          item.getEndDate(),
          days,
          renewalRisk(item, days),
          outstandingByContract.getOrDefault(item.getId(), BigDecimal.ZERO),
          item.getStatus()
      );
    }).toList();
  }

  @Transactional(readOnly = true)
  public List<FollowUpResponse> listFollowUps() {
    List<FollowUp> followUps = deleteGovernanceService.visible("FOLLOW_UP", followUpRepository.findAllByOrderByFollowedAtDesc(), FollowUp::getId);
    Map<UUID, Customer> customers = customerMap(followUps.stream().map(FollowUp::getCustomerId).toList());
    Map<UUID, Opportunity> opportunities = opportunityMap(followUps.stream()
        .map(FollowUp::getOpportunityId)
        .toList());
    return followUps.stream().map(item -> toFollowUp(item, customers, opportunities)).toList();
  }

  @Transactional
  public FollowUpResponse createFollowUp(CreateFollowUpRequest request) {
    validateCustomer(request.customerId());
    Opportunity opportunity = null;
    if (request.opportunityId() != null) {
      opportunity = opportunityRepository.findById(request.opportunityId())
          .orElseThrow(() -> new BusinessException("关联商机不存在"));
      if (opportunity.getCustomerId() != null && !request.customerId().equals(opportunity.getCustomerId())) {
        throw new BusinessException("跟进客户与关联商机客户不一致");
      }
    }

    FollowUp followUp = new FollowUp();
    followUp.setCustomerId(request.customerId());
    followUp.setOpportunityId(request.opportunityId());
    followUp.setType(request.type());
    followUp.setSubject(request.subject());
    followUp.setContent(request.content());
    followUp.setFollowedAt(request.followedAt());
    followUp.setNextAction(request.nextAction());
    followUp.setOwnerName(request.ownerName());
    FollowUp saved = followUpRepository.save(followUp);
    return toFollowUp(
        saved,
        customerMap(nullableId(saved.getCustomerId())),
        opportunity == null ? Map.of() : Map.of(opportunity.getId(), opportunity)
    );
  }

  @Transactional(readOnly = true)
  public List<CustomerProfileResponse> listCustomerProfiles() {
    List<Customer> customers = customerRepository.findAllByOrderByCreatedAtDesc();
    List<Opportunity> opportunities = opportunityRepository.findAll();
    List<ServiceContract> contracts = contractRepository.findAll();
    List<Receivable> receivables = receivableRepository.findAll();

    return customers.stream().map(customer -> {
      List<Opportunity> customerOpportunities = opportunities.stream()
          .filter(item -> customer.getId().equals(item.getCustomerId()))
          .toList();
      List<ServiceContract> customerContracts = contracts.stream()
          .filter(item -> customer.getId().equals(item.getCustomerId()))
          .toList();
      List<Receivable> customerReceivables = receivables.stream()
          .filter(item -> customer.getId().equals(item.getCustomerId()))
          .toList();
      return new CustomerProfileResponse(
          customer.getId(),
          customer.getCode(),
          customer.getName(),
          customer.getIndustry(),
          customer.getLevel(),
          customer.getOwnerName(),
          customer.getRiskStatus(),
          customer.getPaymentHabit(),
          customerOpportunities.size(),
          sum(customerOpportunities.stream().map(Opportunity::getExpectedAmount).toList()),
          customerContracts.size(),
          sum(customerContracts.stream().map(ServiceContract::getAmount).toList()),
          sum(customerReceivables.stream()
              .filter(item -> item.getStatus() != ReceivableStatus.SETTLED)
              .map(this::outstandingAmount)
              .toList()),
          sum(customerReceivables.stream()
              .filter(item -> item.getStatus() == ReceivableStatus.OVERDUE)
              .map(this::outstandingAmount)
              .toList()),
          customerContracts.stream()
              .filter(item -> item.getStatus() != ContractStatus.CLOSED)
              .map(ServiceContract::getEndDate)
              .min(LocalDate::compareTo)
              .orElse(null)
      );
    }).toList();
  }

  @Transactional
  public void deleteOpportunity(UUID id) {
    if (!opportunityRepository.existsById(id)) {
      throw new BusinessException("商机不存在");
    }
    Opportunity opportunity = opportunityRepository.findById(id).orElse(null);
    if (!deleteGovernanceService.allowPhysicalDelete("OPPORTUNITY", id, opportunity == null ? id.toString() : opportunity.getCode())) {
      return;
    }
    // Cascade delete related records
    entityManager.createNativeQuery("DELETE FROM crm_follow_ups WHERE opportunity_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_revisions WHERE quote_id IN (SELECT id FROM crm_quote_plans WHERE opportunity_id = ?1)")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_approval_records WHERE quote_id IN (SELECT id FROM crm_quote_plans WHERE opportunity_id = ?1)")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_cost_requests WHERE quote_id IN (SELECT id FROM crm_quote_plans WHERE opportunity_id = ?1)")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_plans WHERE opportunity_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_opportunities WHERE id = ?1")
        .setParameter(1, id).executeUpdate();
  }

  @Transactional
  public void deleteQuote(UUID id) {
    if (!quoteRepository.existsById(id)) {
      throw new BusinessException("报价方案不存在");
    }
    QuotePlan quote = quoteRepository.findById(id).orElse(null);
    if (!deleteGovernanceService.allowPhysicalDelete("QUOTE", id, quote == null ? id.toString() : quote.getCode())) {
      return;
    }
    entityManager.createNativeQuery("DELETE FROM crm_quote_revisions WHERE quote_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_approval_records WHERE quote_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_cost_requests WHERE quote_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_plans WHERE id = ?1")
        .setParameter(1, id).executeUpdate();
  }

  @Transactional
  public void deleteContract(UUID id) {
    if (!contractRepository.existsById(id)) {
      throw new BusinessException("合同不存在");
    }
    ServiceContract contract = contractRepository.findById(id).orElse(null);
    if (!deleteGovernanceService.allowPhysicalDelete("CONTRACT", id, contract == null ? id.toString() : contract.getCode())) {
      return;
    }
    // Disassociate receivables (skip if fin_receivables table does not exist)
    try {
      entityManager.createNativeQuery("UPDATE fin_receivables SET contract_id = NULL WHERE contract_id = ?1")
          .setParameter(1, id).executeUpdate();
    } catch (Exception ignored) {}
    entityManager.createNativeQuery("UPDATE crm_quote_approval_records SET generated_contract_id = NULL WHERE generated_contract_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("UPDATE project_projects SET contract_id = NULL WHERE contract_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("UPDATE work_orders SET contract_id = NULL WHERE contract_id = ?1")
        .setParameter(1, id).executeUpdate();
    // Clean up attached files
    entityManager.createNativeQuery("DELETE FROM crm_attachment WHERE entity_type = 'CONTRACT' AND entity_id = ?1")
        .setParameter(1, id).executeUpdate();
    contractRepository.deleteById(id);
  }

  @Transactional
  public void deleteFollowUp(UUID id) {
    FollowUp f = followUpRepository.findById(id)
        .orElseThrow(() -> new BusinessException("跟进记录不存在"));
    if (!deleteGovernanceService.allowPhysicalDelete("FOLLOW_UP", id, f.getSubject() == null ? id.toString() : f.getSubject())) {
      return;
    }
    followUpRepository.delete(f);
  }

  private OpportunityResponse toOpportunity(Opportunity item, Map<UUID, Customer> customers) {
    return new OpportunityResponse(
        item.getId(),
        item.getCustomerId(),
        customerName(customers, item.getCustomerId()),
        item.getCode(),
        item.getSource(),
        item.getNeedSummary(),
        item.getStage(),
        item.getExpectedAmount(),
        item.getProbability(),
        item.getNextAction(),
        item.getNextActionAt(),
        item.getOwnerName(),
        item.getUpdatedAt()
    );
  }

  private QuoteResponse toQuote(
      QuotePlan item,
      Map<UUID, Customer> customers,
      Map<UUID, Opportunity> opportunities
  ) {
    Opportunity opportunity = item.getOpportunityId() == null ? null : opportunities.get(item.getOpportunityId());
    QuoteApprovalRecord approval = quoteApprovalRepository
        .findFirstByQuoteIdOrderByDecidedAtDesc(item.getId())
        .orElse(null);
    if (approval != null && approval.getQuoteVersion() != item.getVersionNo()) {
      approval = null;
    }
    UUID convertedContractId = contractRepository.findByQuoteId(item.getId())
        .map(ServiceContract::getId)
        .orElse(null);
    QuoteCostRequest latestCost = quoteCostRequestRepository.findFirstByQuoteIdOrderByCreatedAtDesc(item.getId())
        .orElse(null);
    return new QuoteResponse(
        item.getId(),
        item.getCustomerId(),
        customerName(customers, item.getCustomerId()),
        item.getOpportunityId(),
        opportunity == null ? null : opportunity.getCode(),
        item.getCode(),
        item.getServiceScope(),
        item.getInspectCycle(),
        item.getPaymentNodes(),
        item.getAmount(),
        defaultTaxRate(item.getTaxRate()),
        netAmount(item.getAmount(), item.getTaxRate()),
        defaultAmount(item.getLaborBudget()),
        defaultAmount(item.getMaterialBudget()),
        defaultAmount(item.getSubcontractBudget()),
        defaultAmount(item.getTravelBudget()),
        defaultAmount(item.getOtherBudget()),
        quoteBudgetAmount(item),
        quoteGrossMargin(item),
        quoteGrossMarginRate(item),
        item.getVersionNo(),
        item.getStatus(),
        approval == null ? null : approval.getComment(),
        approval == null ? null : approval.getApproverName(),
        approval == null ? null : approval.getDecidedAt(),
        item.getCustomerDecision(),
        item.getCustomerComment(),
        item.getCustomerDecisionBy(),
        item.getCustomerDecidedAt(),
        convertedContractId,
        latestCost == null ? null : toQuoteCostRequest(latestCost),
        item.getUpdatedAt()
    );
  }

  private ServiceContract createContractFromAcceptedQuote(
      QuotePlan quote,
      ConvertQuoteRequest request
  ) {
    if (quote.getCustomerId() == null) {
      throw new BusinessException("报价未关联客户，不能生成合同");
    }
    String contractCode = request.contractCode() != null
        ? request.contractCode()
        : (quote.getCode() != null ? deriveCode(quote.getCode(), "HT") : codeGenerator.generate("CONTRACT"));
    requireText(request.projectName(), "请输入合同项目名称");
    requireText(request.contractType(), "请选择合同类型");
    if (request.startDate() == null || request.endDate() == null) {
      throw new BusinessException("请输入合同起止日期");
    }
    if (request.endDate().isBefore(request.startDate())) {
      throw new BusinessException("合同结束日期不能早于开始日期");
    }
    if (contractCode != null && contractRepository.existsByCode(contractCode)) {
      throw new BusinessException("合同编号已存在");
    }
    if (contractRepository.existsByQuoteId(quote.getId())) {
      throw new BusinessException("该报价已生成合同");
    }

    ServiceContract contract = new ServiceContract();
    contract.setQuoteId(quote.getId());
    contract.setCustomerId(quote.getCustomerId());
    contract.setCode(contractCode);
    contract.setProjectName(request.projectName());
    contract.setContractType(request.contractType());
    contract.setAmount(defaultAmount(quote.getAmount()));
    contract.setTaxRate(defaultTaxRate(quote.getTaxRate()));
    contract.setStartDate(request.startDate());
    contract.setEndDate(request.endDate());
    contract.setServiceCycle(request.serviceCycle());
    contract.setStatus(ContractStatus.ACTIVE);
    return contractRepository.save(contract);
  }

  private Receivable createFirstReceivable(
      QuotePlan quote,
      ServiceContract contract,
      ConvertQuoteRequest request
  ) {
    BigDecimal amount = defaultAmount(request.firstReceivableAmount());
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    String receivableCode = request.receivableCode() != null ? request.receivableCode() : codeGenerator.generate("RECEIVABLE");
    if (request.firstReceivableDueDate() == null) {
      throw new BusinessException("请输入首期应收到期日");
    }
    if (amount.compareTo(defaultAmount(quote.getAmount())) > 0) {
      throw new BusinessException("首期应收不能超过合同总额");
    }
    if (receivableCode != null && receivableRepository.existsByCode(receivableCode)) {
      throw new BusinessException("应收单号已存在");
    }

    Receivable receivable = new Receivable();
    receivable.setCustomerId(quote.getCustomerId());
    receivable.setContractId(contract.getId());
    receivable.setCode(receivableCode);
    receivable.setSourceNo(contract.getCode());
    receivable.setAmount(amount);
    receivable.setDueDate(request.firstReceivableDueDate());
    receivable.setSettledAmount(BigDecimal.ZERO);
    receivable.setStatus(ReceivableStatus.INVOICE_PENDING);
    return receivableRepository.save(receivable);
  }

  private void markOpportunityWon(QuotePlan quote, ServiceContract contract) {
    if (quote.getOpportunityId() == null) {
      return;
    }
    opportunityRepository.findById(quote.getOpportunityId()).ifPresent(opportunity -> {
      opportunity.setStage(OpportunityStage.WON);
      opportunity.setProbability(100);
      opportunity.setNextAction("执行合同 " + contract.getCode());
      opportunity.setNextActionAt(contract.getStartDate());
      opportunityRepository.save(opportunity);
    });
  }

  private void clearCustomerResult(QuotePlan quote) {
    quote.setCustomerDecision(null);
    quote.setCustomerComment(null);
    quote.setCustomerDecisionBy(null);
    quote.setCustomerDecidedAt(null);
  }

  private QuoteCostRequest latestApprovedQuoteCost(UUID quoteId) {
    return quoteCostRequestRepository.findFirstByQuoteIdAndStatusOrderByCreatedAtDesc(quoteId, QuoteCostStatus.APPROVED)
        .orElse(null);
  }

  private void applyApprovedCostToQuote(QuotePlan quote, QuoteCostRequest cost) {
    quote.setLaborBudget(defaultAmount(cost.getLaborCost()));
    quote.setMaterialBudget(defaultAmount(cost.getMaterialCost()));
    quote.setSubcontractBudget(defaultAmount(cost.getSubcontractCost()));
    quote.setTravelBudget(defaultAmount(cost.getTravelCost()));
    quote.setOtherBudget(sum(List.of(cost.getEquipmentCost(), cost.getRiskReserve(), cost.getOtherCost())));
  }

  private QuoteCostRequestResponse toQuoteCostRequest(QuoteCostRequest cost) {
    return new QuoteCostRequestResponse(
        cost.getId(),
        cost.getQuoteId(),
        cost.getOpportunityId(),
        cost.getCustomerId(),
        cost.getStatus(),
        cost.getRequestedBy(),
        cost.getRequestedAt(),
        cost.getProjectManager(),
        defaultAmount(cost.getLaborCost()),
        defaultCostTaxRate(cost.getLaborTaxRate(), BigDecimal.valueOf(6)),
        defaultAmount(cost.getMaterialCost()),
        defaultCostTaxRate(cost.getMaterialTaxRate(), BigDecimal.valueOf(13)),
        defaultAmount(cost.getSubcontractCost()),
        defaultCostTaxRate(cost.getSubcontractTaxRate(), BigDecimal.valueOf(6)),
        defaultAmount(cost.getTravelCost()),
        defaultAmount(cost.getEquipmentCost()),
        defaultCostTaxRate(cost.getEquipmentTaxRate(), BigDecimal.valueOf(13)),
        defaultAmount(cost.getRiskReserve()),
        defaultAmount(cost.getOtherCost()),
        quoteCostTotal(cost),
        defaultAmount(cost.getSuggestedPrice()),
        cost.getCostRemark(),
        cost.getSubmittedAt(),
        cost.getApprovedBy(),
        cost.getApprovedAt(),
        cost.getApprovalComment()
    );
  }

  private void saveQuoteRevision(QuotePlan quote, String revisionNote, String editorName) {
    QuoteRevision revision = new QuoteRevision();
    revision.setQuoteId(quote.getId());
    revision.setVersionNo(quote.getVersionNo());
    revision.setCode(quote.getCode());
    revision.setServiceScope(quote.getServiceScope());
    revision.setInspectCycle(quote.getInspectCycle());
    revision.setPaymentNodes(quote.getPaymentNodes());
    revision.setAmount(defaultAmount(quote.getAmount()));
    revision.setTaxRate(defaultTaxRate(quote.getTaxRate()));
    revision.setLaborBudget(defaultAmount(quote.getLaborBudget()));
    revision.setMaterialBudget(defaultAmount(quote.getMaterialBudget()));
    revision.setSubcontractBudget(defaultAmount(quote.getSubcontractBudget()));
    revision.setTravelBudget(defaultAmount(quote.getTravelBudget()));
    revision.setOtherBudget(defaultAmount(quote.getOtherBudget()));
    revision.setStatus(quote.getStatus());
    revision.setRevisionNote(revisionNote);
    revision.setEditorName(editorName);
    revision.setRevisedAt(OffsetDateTime.now());
    quoteRevisionRepository.save(revision);
  }

  private QuoteRevisionResponse toQuoteRevision(QuoteRevision revision) {
    return new QuoteRevisionResponse(
        revision.getId(),
        revision.getVersionNo(),
        revision.getCode(),
        revision.getServiceScope(),
        revision.getInspectCycle(),
        revision.getPaymentNodes(),
        revision.getAmount(),
        defaultTaxRate(revision.getTaxRate()),
        netAmount(revision.getAmount(), revision.getTaxRate()),
        defaultAmount(revision.getLaborBudget()),
        defaultAmount(revision.getMaterialBudget()),
        defaultAmount(revision.getSubcontractBudget()),
        defaultAmount(revision.getTravelBudget()),
        defaultAmount(revision.getOtherBudget()),
        revisionBudgetAmount(revision),
        revisionGrossMargin(revision),
        revisionGrossMarginRate(revision),
        revision.getStatus(),
        revision.getRevisionNote(),
        revision.getEditorName(),
        revision.getRevisedAt()
    );
  }

  private ContractResponse toContract(ServiceContract item, Map<UUID, Customer> customers) {
    return new ContractResponse(
        item.getId(),
        item.getQuoteId(),
        item.getCustomerId(),
        customerName(customers, item.getCustomerId()),
        item.getCode(),
        item.getProjectName(),
        item.getContractType(),
        item.getAmount(),
        defaultTaxRate(item.getTaxRate()),
        netAmount(item.getAmount(), item.getTaxRate()),
        item.getStartDate(),
        item.getEndDate(),
        item.getServiceCycle(),
        item.getStatus()
    );
  }

  private ReceivableResponse toReceivable(
      Receivable item,
      Map<UUID, Customer> customers,
      Map<UUID, ServiceContract> contracts
  ) {
    return new ReceivableResponse(
        item.getId(),
        item.getCustomerId(),
        customerName(customers, item.getCustomerId()),
        item.getContractId(),
        item.getContractId() == null || contracts.get(item.getContractId()) == null
            ? item.getSourceNo()
            : contracts.get(item.getContractId()).getCode(),
        item.getCode(),
        item.getSourceNo(),
        item.getAmount(),
        item.getDueDate(),
        item.getInvoiceNo(),
        item.getInvoiceDate(),
        defaultAmount(item.getSettledAmount()),
        outstandingAmount(item),
        item.getStatus()
    );
  }

  private ReceivableResponse mapReceivable(Receivable item) {
    Map<UUID, Customer> customers = customerMap(nullableId(item.getCustomerId()));
    Map<UUID, ServiceContract> contracts = contractMap(nullableId(item.getContractId()));
    return toReceivable(item, customers, contracts);
  }

  private BigDecimal outstandingAmount(Receivable receivable) {
    return defaultAmount(receivable.getAmount()).subtract(defaultAmount(receivable.getSettledAmount()));
  }

  private void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(message);
    }
  }

  private FollowUpResponse toFollowUp(
      FollowUp item,
      Map<UUID, Customer> customers,
      Map<UUID, Opportunity> opportunities
  ) {
    Opportunity opportunity = item.getOpportunityId() == null ? null : opportunities.get(item.getOpportunityId());
    return new FollowUpResponse(
        item.getId(),
        item.getCustomerId(),
        customerName(customers, item.getCustomerId()),
        item.getOpportunityId(),
        opportunity == null ? null : opportunity.getCode(),
        item.getType(),
        item.getSubject(),
        item.getContent(),
        item.getFollowedAt(),
        item.getNextAction(),
        item.getOwnerName()
    );
  }

  private Map<UUID, Customer> customerMap(List<UUID> ids) {
    List<UUID> validIds = distinctIds(ids);
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return customerRepository.findAllById(validIds).stream()
        .collect(Collectors.toMap(Customer::getId, Function.identity()));
  }

  private Map<UUID, Opportunity> opportunityMap(List<UUID> ids) {
    List<UUID> validIds = distinctIds(ids);
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return opportunityRepository.findAllById(validIds).stream()
        .collect(Collectors.toMap(Opportunity::getId, Function.identity()));
  }

  private Map<UUID, ServiceContract> contractMap(List<UUID> ids) {
    List<UUID> validIds = distinctIds(ids);
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return contractRepository.findAllById(validIds).stream()
        .collect(Collectors.toMap(ServiceContract::getId, Function.identity()));
  }

  private List<UUID> distinctIds(List<UUID> ids) {
    return ids.stream().filter(id -> id != null).distinct().toList();
  }

  private List<UUID> nullableId(UUID id) {
    return id == null ? List.of() : List.of(id);
  }

  private String customerName(Map<UUID, Customer> customers, UUID id) {
    return id == null || customers.get(id) == null ? "未关联客户" : customers.get(id).getName();
  }

  private void validateCustomer(UUID customerId) {
    if (customerId != null && !customerRepository.existsById(customerId)) {
      throw new BusinessException("客户不存在");
    }
  }

  private String renewalRisk(ServiceContract contract, long daysRemaining) {
    if (daysRemaining < 0) {
      return "EXPIRED";
    }
    if (contract.getStatus() == ContractStatus.OVERDUE_RISK || daysRemaining <= 30) {
      return "HIGH";
    }
    if (contract.getStatus() == ContractStatus.RENEWAL_PENDING || daysRemaining <= 90) {
      return "MEDIUM";
    }
    return "LOW";
  }

  private BigDecimal sum(List<BigDecimal> values) {
    return values.stream().map(this::defaultAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal quoteCostTotal(QuoteCostRequest cost) {
    return sum(List.of(
        cost.getLaborCost(),
        cost.getMaterialCost(),
        cost.getSubcontractCost(),
        cost.getTravelCost(),
        cost.getEquipmentCost(),
        cost.getRiskReserve(),
        cost.getOtherCost()
    ));
  }

  private void applyQuoteBudget(
      QuotePlan quote,
      BigDecimal laborBudget,
      BigDecimal materialBudget,
      BigDecimal subcontractBudget,
      BigDecimal travelBudget,
      BigDecimal otherBudget
  ) {
    quote.setLaborBudget(defaultAmount(laborBudget));
    quote.setMaterialBudget(defaultAmount(materialBudget));
    quote.setSubcontractBudget(defaultAmount(subcontractBudget));
    quote.setTravelBudget(defaultAmount(travelBudget));
    quote.setOtherBudget(defaultAmount(otherBudget));
  }

  private void validateQuoteBudgetForSubmit(QuotePlan quote) {
    BigDecimal budget = quoteBudgetRawAmount(quote);
    if (budget.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException("请先在报价中确认项目预算，再提交审批");
    }
  }

  private BigDecimal quoteBudgetAmount(QuotePlan quote) {
    BigDecimal projectBudget = projectBudgetAmountForQuote(quote);
    return projectBudget.compareTo(BigDecimal.ZERO) > 0 ? projectBudget : quoteBudgetRawAmount(quote);
  }

  private BigDecimal quoteBudgetRawAmount(QuotePlan quote) {
    return sum(List.of(
        quote.getLaborBudget(),
        quote.getMaterialBudget(),
        quote.getSubcontractBudget(),
        quote.getTravelBudget(),
        quote.getOtherBudget()
    ));
  }

  private BigDecimal revisionBudgetAmount(QuoteRevision revision) {
    BigDecimal total = sum(List.of(
        revision.getLaborBudget(),
        revision.getMaterialBudget(),
        revision.getSubcontractBudget(),
        revision.getTravelBudget(),
        revision.getOtherBudget()
    ));
    return total;
  }

  private BigDecimal quoteGrossMargin(QuotePlan quote) {
    return defaultAmount(quote.getAmount()).subtract(quoteBudgetAmount(quote));
  }

  private BigDecimal revisionGrossMargin(QuoteRevision revision) {
    return defaultAmount(revision.getAmount()).subtract(revisionBudgetAmount(revision));
  }

  private BigDecimal quoteGrossMarginRate(QuotePlan quote) {
    return marginRate(defaultAmount(quote.getAmount()), quoteGrossMargin(quote));
  }

  private BigDecimal revisionGrossMarginRate(QuoteRevision revision) {
    return marginRate(defaultAmount(revision.getAmount()), revisionGrossMargin(revision));
  }

  private BigDecimal marginRate(BigDecimal amount, BigDecimal grossMargin) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }
    return grossMargin.multiply(BigDecimal.valueOf(100)).divide(amount, 2, RoundingMode.HALF_UP);
  }

  private BigDecimal projectBudgetAmountForQuote(QuotePlan quote) {
    if (quote == null || quote.getId() == null) {
      return BigDecimal.ZERO;
    }
    Object value = entityManager.createNativeQuery("""
        SELECT COALESCE(SUM(item.planned_amount), 0)
        FROM project_budget_items item
        JOIN project_projects project ON project.id = item.project_id
        JOIN crm_service_contracts contract ON contract.id = project.contract_id
        WHERE contract.quote_id = ?1
        """)
        .setParameter(1, quote.getId())
        .getSingleResult();
    if (value instanceof BigDecimal amount) {
      return amount;
    }
    return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
  }

  private BigDecimal defaultAmount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal defaultTaxRate(BigDecimal value) {
    return value == null ? BigDecimal.valueOf(13) : value;
  }

  private BigDecimal defaultCostTaxRate(BigDecimal value, BigDecimal defaultValue) {
    return value == null ? defaultValue : value;
  }

  private BigDecimal netAmount(BigDecimal amount, BigDecimal taxRate) {
    BigDecimal grossAmount = defaultAmount(amount);
    BigDecimal rate = defaultTaxRate(taxRate);
    BigDecimal divisor = BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
    if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
      return grossAmount.setScale(2, RoundingMode.HALF_UP);
    }
    return grossAmount.divide(divisor, 2, RoundingMode.HALF_UP);
  }


  @Transactional

  public ContractResponse renewContract(UUID id) {
    var contract = contractRepository.findById(id)
        .orElseThrow(() -> new BusinessException("合同不存在"));
    ServiceContract renewal = new ServiceContract();
    renewal.setCustomerId(contract.getCustomerId());
    renewal.setQuoteId(contract.getQuoteId());
    renewal.setProjectName(contract.getProjectName());
    renewal.setContractType(contract.getContractType());
    renewal.setAmount(contract.getAmount());
    renewal.setTaxRate(defaultTaxRate(contract.getTaxRate()));
    renewal.setServiceCycle(contract.getServiceCycle());
    renewal.setStartDate(java.time.LocalDate.now());
    renewal.setEndDate(java.time.LocalDate.now().plusYears(1));
    renewal.setStatus(ContractStatus.ACTIVE);
    renewal.setCode(codeGenerator.generate("CONTRACT"));
    // Close old contract
    contract.setStatus(ContractStatus.CLOSED);
    contractRepository.save(contract);
    var saved = contractRepository.save(renewal);
    return toContract(saved, java.util.Map.of());
  }
  public ReceivableResponse updateReceivable(UUID id, UpdateReceivableRequest request) {
    Receivable receivable = receivableRepository.findById(id)
        .orElseThrow(() -> new BusinessException("应收单不存在"));
    if (request.amount() != null) {
      BigDecimal newAmount = defaultAmount(request.amount());
      BigDecimal settled = receivable.getSettledAmount() != null ? receivable.getSettledAmount() : BigDecimal.ZERO;
      if (newAmount.compareTo(settled) < 0) {
        throw new BusinessException("修改后应收金额不能小于已收金额");
      }
      receivable.setAmount(newAmount);
    }
    if (request.dueDate() != null) receivable.setDueDate(request.dueDate());
    if (request.sourceNo() != null) receivable.setSourceNo(request.sourceNo());
    BigDecimal outstanding = receivable.getAmount().subtract(receivable.getSettledAmount() != null ? receivable.getSettledAmount() : BigDecimal.ZERO);
    LocalDate due = receivable.getDueDate();
    if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
      receivable.setStatus(ReceivableStatus.SETTLED);
    } else if (due != null && due.isBefore(LocalDate.now())) {
      receivable.setStatus(ReceivableStatus.OVERDUE);
    } else if (receivable.getInvoiceNo() != null) {
      receivable.setStatus(ReceivableStatus.PAYMENT_PENDING);
    } else {
      receivable.setStatus(ReceivableStatus.INVOICE_PENDING);
    }
    Receivable saved = receivableRepository.save(receivable);
    return toReceivable(saved,
        customerMap(nullableId(saved.getCustomerId())),
        contractMap(nullableId(saved.getContractId())));
  }


  @Transactional
  public ContractChangeResponse createContractChangeRequest(UUID contractId, CreateContractChangeRequest request) {
    ContractChangeRequest change = new ContractChangeRequest();
    change.setContractId(contractId);
    change.setChangeData(request.changeData());
    change.setReason(request.reason());
    change.setRequestedBy(request.requestedBy());
    change.setRequestedAt(java.time.OffsetDateTime.now());
    change.setStatus("PENDING");
    ContractChangeRequest saved = changeRepository.save(change);
    return toContractChangeResponse(saved);
  }

  @Transactional
  public ContractChangeResponse approveContractChange(UUID id, String operatorName, String comment) {
    ContractChangeRequest change = changeRepository.findById(id)
        .orElseThrow(() -> new BusinessException("变更请求不存在"));
    if (!"PENDING".equals(change.getStatus())) {
      throw new BusinessException("该变更请求已处理");
    }
    applyContractChanges(change);
    change.setStatus("APPROVED");
    change.setApprovedBy(operatorName);
    change.setApprovedAt(java.time.OffsetDateTime.now());
    change.setApprovalComment(comment);
    return toContractChangeResponse(changeRepository.save(change));
  }

  @Transactional
  public ContractChangeResponse rejectContractChange(UUID id, String operatorName, String comment) {
    ContractChangeRequest change = changeRepository.findById(id)
        .orElseThrow(() -> new BusinessException("变更请求不存在"));
    if (!"PENDING".equals(change.getStatus())) {
      throw new BusinessException("该变更请求已处理");
    }
    change.setStatus("REJECTED");
    change.setApprovedBy(operatorName);
    change.setApprovedAt(java.time.OffsetDateTime.now());
    change.setApprovalComment(comment);
    return toContractChangeResponse(changeRepository.save(change));
  }

  private void applyContractChanges(ContractChangeRequest change) {
    ServiceContract contract = contractRepository.findById(change.getContractId())
        .orElseThrow(() -> new BusinessException("合同不存在"));
    String data = change.getChangeData();
    if (data == null || data.isBlank()) return;
    try {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(data);
      if (root.has("projectName")) contract.setProjectName(root.get("projectName").asText());
      if (root.has("contractType")) contract.setContractType(root.get("contractType").asText());
      if (root.has("amount")) contract.setAmount(new java.math.BigDecimal(root.get("amount").asText()));
      if (root.has("taxRate")) contract.setTaxRate(new java.math.BigDecimal(root.get("taxRate").asText()));
      if (root.has("startDate")) contract.setStartDate(java.time.LocalDate.parse(root.get("startDate").asText()));
      if (root.has("endDate")) contract.setEndDate(java.time.LocalDate.parse(root.get("endDate").asText()));
      if (root.has("serviceCycle")) contract.setServiceCycle(root.get("serviceCycle").asText());
      contractRepository.save(contract);
    } catch (Exception e) {
      throw new BusinessException("变更数据解析失败: " + e.getMessage());
    }
  }

  @Transactional
  public OpportunityResponse updateOpportunity(UUID id, CreateOpportunityRequest request) {
    Opportunity opp = opportunityRepository.findById(id)
        .orElseThrow(() -> new BusinessException("\u5546\u673a\u4e0d\u5b58\u5728"));
    if (request.customerId() != null) opp.setCustomerId(request.customerId());
    if (request.needSummary() != null) opp.setNeedSummary(request.needSummary());
    if (request.expectedAmount() != null) opp.setExpectedAmount(request.expectedAmount());
    if (request.nextAction() != null) opp.setNextAction(request.nextAction());
    if (request.nextActionAt() != null) opp.setNextActionAt(request.nextActionAt());
    if (request.ownerName() != null) opp.setOwnerName(request.ownerName());
    return toOpportunity(opportunityRepository.save(opp), customerMap(nullableId(opp.getCustomerId())));
  }

  @Transactional
  public ContractResponse updateContract(UUID id, UpdateContractRequest request) {
    ServiceContract contract = contractRepository.findById(id)
        .orElseThrow(() -> new BusinessException("\u5408\u540c\u4e0d\u5b58\u5728"));
    if (request.projectName() != null) contract.setProjectName(request.projectName());
    if (request.contractType() != null) contract.setContractType(request.contractType());
    if (request.amount() != null) contract.setAmount(request.amount());
    if (request.taxRate() != null) contract.setTaxRate(defaultTaxRate(request.taxRate()));
    if (request.serviceCycle() != null) contract.setServiceCycle(request.serviceCycle());
    if (request.startDate() != null) contract.setStartDate(LocalDate.parse(request.startDate()));
    if (request.endDate() != null) contract.setEndDate(LocalDate.parse(request.endDate()));
    return toContract(contractRepository.save(contract), customerMap(nullableId(contract.getCustomerId())));
  }

  public java.util.List<ContractChangeResponse> listContractChanges(UUID contractId) {
    return changeRepository.findByContractIdOrderByCreatedAtDesc(contractId)
        .stream().map(this::toContractChangeResponse).toList();
  }

  private ContractChangeResponse toContractChangeResponse(ContractChangeRequest item) {
    return new ContractChangeResponse(
        item.getId(), item.getContractId(), item.getChangeData(), item.getReason(),
        item.getStatus(), item.getRequestedBy(), item.getRequestedAt(),
        item.getApprovedBy(), item.getApprovedAt(), item.getApprovalComment(),
        item.getCreatedAt()
    );
  }

  private String deriveCode(String sourceCode, String targetPrefix) {
    int firstDash = sourceCode.indexOf('-');
    if (firstDash < 0) return null;
    return targetPrefix + sourceCode.substring(firstDash);
  }
}
