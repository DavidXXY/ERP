package com.company.ops.api.modules.crm.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.ApprovalDecision;
import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.FollowUp;
import com.company.ops.api.modules.crm.domain.Opportunity;
import com.company.ops.api.modules.crm.domain.OpportunityStage;
import com.company.ops.api.modules.crm.domain.QuoteCustomerDecision;
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
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteConversionResult;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteRevisionResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReceivableResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RecordReceiptRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RegisterInvoiceRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RenewalResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.UpdateQuoteRequest;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.FollowUpRepository;
import com.company.ops.api.modules.crm.repository.OpportunityRepository;
import com.company.ops.api.modules.crm.repository.QuotePlanRepository;
import com.company.ops.api.modules.crm.repository.QuoteApprovalRecordRepository;
import com.company.ops.api.modules.crm.repository.QuoteRevisionRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ReceivableReceiptRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.service.LedgerService;
import java.math.BigDecimal;
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
  private final QuoteRevisionRepository quoteRevisionRepository;
  private final FollowUpRepository followUpRepository;
  private final ServiceContractRepository contractRepository;
  private final ReceivableRepository receivableRepository;
  private final ReceivableReceiptRepository receiptRepository;
  private final LedgerService ledgerService;

  @jakarta.persistence.PersistenceContext
  private jakarta.persistence.EntityManager entityManager;

  public CrmOperationsService(
      CustomerRepository customerRepository,
      OpportunityRepository opportunityRepository,
      QuotePlanRepository quoteRepository,
      QuoteApprovalRecordRepository quoteApprovalRepository,
      QuoteRevisionRepository quoteRevisionRepository,
      FollowUpRepository followUpRepository,
      ServiceContractRepository contractRepository,
      ReceivableRepository receivableRepository,
      ReceivableReceiptRepository receiptRepository,
      LedgerService ledgerService
  ) {
    this.codeGenerator = codeGenerator;
    this.customerRepository = customerRepository;
    this.opportunityRepository = opportunityRepository;
    this.quoteRepository = quoteRepository;
    this.quoteApprovalRepository = quoteApprovalRepository;
    this.quoteRevisionRepository = quoteRevisionRepository;
    this.followUpRepository = followUpRepository;
    this.contractRepository = contractRepository;
    this.receivableRepository = receivableRepository;
    this.receiptRepository = receiptRepository;
    this.ledgerService = ledgerService;
  }

  @Transactional(readOnly = true)
  public List<OpportunityResponse> listOpportunities() {
    List<Opportunity> opportunities = opportunityRepository.findAllByOrderByUpdatedAtDesc();
    Map<UUID, Customer> customers = customerMap(opportunities.stream()
        .map(Opportunity::getCustomerId)
        .toList());
    return opportunities.stream().map(item -> toOpportunity(item, customers)).toList();
  }

  @Transactional(readOnly = true)
  public OpportunityResponse getOpportunity(UUID id) {
    Opportunity opportunity = opportunityRepository.findById(id)
        .orElseThrow(() -> new BusinessException("商机不存在"));
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
    opportunity.setStage(request.stage());
    opportunity.setNextAction(request.nextAction());
    opportunity.setNextActionAt(request.nextActionAt());
    opportunity.setProbability(request.probability());
    Opportunity saved = opportunityRepository.save(opportunity);
    return toOpportunity(saved, customerMap(nullableId(saved.getCustomerId())));
  }

  @Transactional(readOnly = true)
  public List<QuoteResponse> listQuotes() {
    List<QuotePlan> quotes = quoteRepository.findAllByOrderByUpdatedAtDesc();
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
    quote.setVersionNo(Math.max(quote.getVersionNo(), 1) + 1);
    quote.setStatus(QuoteStatus.DRAFT);
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
  public QuoteResponse submitQuote(UUID id) {
    QuotePlan quote = quoteRepository.findById(id)
        .orElseThrow(() -> new BusinessException("报价方案不存在"));
    if (quote.getStatus() != QuoteStatus.DRAFT) {
      throw new BusinessException("只有草稿版本可以提交审批");
    }
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
    List<ServiceContract> contracts = contractRepository.findAllByOrderByEndDateAsc();
    Map<UUID, Customer> customers = customerMap(contracts.stream()
        .map(ServiceContract::getCustomerId)
        .toList());
    return contracts.stream().map(item -> toContract(item, customers)).toList();
  }

  @Transactional(readOnly = true)
  public ContractResponse getContract(UUID id) {
    ServiceContract contract = contractRepository.findById(id)
        .orElseThrow(() -> new BusinessException("合同不存在"));
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
    List<FollowUp> followUps = followUpRepository.findAllByOrderByFollowedAtDesc();
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
    // Cascade delete related records
    entityManager.createNativeQuery("DELETE FROM crm_follow_ups WHERE opportunity_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_revisions WHERE quote_id IN (SELECT id FROM crm_quote_plans WHERE opportunity_id = ?1)")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_approval_records WHERE quote_id IN (SELECT id FROM crm_quote_plans WHERE opportunity_id = ?1)")
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
    entityManager.createNativeQuery("DELETE FROM crm_quote_revisions WHERE quote_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_approval_records WHERE quote_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_plans WHERE id = ?1")
        .setParameter(1, id).executeUpdate();
  }

  @Transactional
  public void deleteContract(UUID id) {
    if (!contractRepository.existsById(id)) {
      throw new BusinessException("合同不存在");
    }
    // Disassociate receivables first (set contract_id to null via update)
    entityManager.createNativeQuery("UPDATE fin_receivables SET contract_id = NULL WHERE contract_id = ?1")
        .setParameter(1, id).executeUpdate();
    contractRepository.deleteById(id);
  }

  @Transactional
  public void deleteFollowUp(UUID id) {
    FollowUp f = followUpRepository.findById(id)
        .orElseThrow(() -> new BusinessException("跟进记录不存在"));
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

  private void saveQuoteRevision(QuotePlan quote, String revisionNote, String editorName) {
    QuoteRevision revision = new QuoteRevision();
    revision.setQuoteId(quote.getId());
    revision.setVersionNo(quote.getVersionNo());
    revision.setCode(quote.getCode());
    revision.setServiceScope(quote.getServiceScope());
    revision.setInspectCycle(quote.getInspectCycle());
    revision.setPaymentNodes(quote.getPaymentNodes());
    revision.setAmount(defaultAmount(quote.getAmount()));
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

  private BigDecimal defaultAmount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private String deriveCode(String sourceCode, String targetPrefix) {
    int firstDash = sourceCode.indexOf('-');
    if (firstDash < 0) return null;
    return targetPrefix + sourceCode.substring(firstDash);
  }
}
