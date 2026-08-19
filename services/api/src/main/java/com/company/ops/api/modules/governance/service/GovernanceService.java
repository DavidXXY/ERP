package com.company.ops.api.modules.governance.service;

import static com.company.ops.api.modules.governance.dto.GovernanceDtos.*;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.ReceivableReceipt;
import com.company.ops.api.modules.crm.repository.ReceivableReceiptRepository;
import com.company.ops.api.modules.finance.domain.PaymentRecord;
import com.company.ops.api.modules.finance.repository.PaymentRecordRepository;
import com.company.ops.api.modules.finance.service.FinanceOperationsService;
import com.company.ops.api.modules.governance.domain.*;
import com.company.ops.api.modules.governance.repository.AccountingPeriodRepository;
import com.company.ops.api.modules.governance.repository.BankStatementLineRepository;
import com.company.ops.api.modules.governance.repository.BusinessControlRecordRepository;
import com.company.ops.api.modules.governance.repository.GovernanceActionLogRepository;
import com.company.ops.api.modules.ledger.domain.VoucherStatus;
import com.company.ops.api.modules.ledger.repository.AccountingVoucherRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.Cacheable;

@Service
public class GovernanceService {
  private static final Set<ControlStatus> OPEN_STATUSES = EnumSet.of(ControlStatus.ACTIVE, ControlStatus.BLOCKED);
  private static final Set<ControlType> BUDGET_TYPES = EnumSet.of(
      ControlType.PROJECT_FORECAST, ControlType.PURCHASE_BUDGET, ControlType.CASH_FORECAST,
      ControlType.BUSINESS_FORECAST, ControlType.FIXED_ASSET);
  private static final Set<ControlType> DATE_REQUIRED_TYPES = EnumSet.of(
      ControlType.CONTRACT_MILESTONE, ControlType.REVENUE_OBLIGATION, ControlType.WARRANTY_RENEWAL,
      ControlType.PROJECT_WBS, ControlType.PROJECT_CLOSEOUT, ControlType.STOCK_COUNT,
      ControlType.SERVICE_SLA, ControlType.MASTER_DATA_CHANGE);

  private final BusinessControlRecordRepository controls;
  private final AccountingPeriodRepository periods;
  private final BankStatementLineRepository bankLines;
  private final AccountingVoucherRepository vouchers;
  private final PaymentRecordRepository payments;
  private final ReceivableReceiptRepository receipts;
  private final GovernanceActionLogRepository actionLogs;
  private final ObjectMapper objectMapper;
  private final FinanceOperationsService financeOperations;

  public GovernanceService(
      BusinessControlRecordRepository controls,
      AccountingPeriodRepository periods,
      BankStatementLineRepository bankLines,
      AccountingVoucherRepository vouchers,
      PaymentRecordRepository payments,
      ReceivableReceiptRepository receipts,
      GovernanceActionLogRepository actionLogs,
      ObjectMapper objectMapper,
      FinanceOperationsService financeOperations) {
    this.controls = controls;
    this.periods = periods;
    this.bankLines = bankLines;
    this.vouchers = vouchers;
    this.payments = payments;
    this.receipts = receipts;
    this.actionLogs = actionLogs;
    this.objectMapper = objectMapper;
    this.financeOperations = financeOperations;
  }

  @Transactional(readOnly = true)
  public List<ControlTypeResponse> controlTypes() {
    return List.of(ControlType.values()).stream()
        .map(type -> new ControlTypeResponse(type, type.domain(), type.label())).toList();
  }

  @Transactional(readOnly = true)
  public Page<ControlResponse> controls(ControlType type, ControlStatus status, String keyword, Pageable pageable) {
    return controls.search(type, status, keyword == null ? "" : keyword.trim(), pageable).map(this::toControl);
  }

  @Transactional
  public ControlResponse createControl(SaveControlRequest request) {
    validateControl(request);
    BusinessControlRecord item = new BusinessControlRecord();
    item.setControlCode(nextControlCode(request.controlType()));
    apply(item, request);
    BusinessControlRecord saved = controls.save(item);
    logAction("CONTROL", saved.getId(), saved.getControlCode(), "CREATE", null, saved.getStatus().name(), null);
    return toControl(saved);
  }

  @Transactional
  public ControlResponse updateControl(UUID id, SaveControlRequest request) {
    validateControl(request);
    BusinessControlRecord item = requireControl(id);
    if (item.getStatus() == ControlStatus.COMPLETED || item.getStatus() == ControlStatus.CANCELLED) {
      throw new BusinessException("已完成或已取消的控制记录不可修改");
    }
    if (item.getControlType() != request.controlType()) throw new BusinessException("控制类型创建后不可变更");
    apply(item, request);
    BusinessControlRecord saved = controls.save(item);
    logAction("CONTROL", saved.getId(), saved.getControlCode(), "UPDATE", saved.getStatus().name(), saved.getStatus().name(), null);
    return toControl(saved);
  }

  @Transactional
  public ControlResponse transition(UUID id, TransitionControlRequest request) {
    BusinessControlRecord item = requireControl(id);
    ControlStatus from = item.getStatus();
    ControlStatus target = request.status();
    if (!allowedTransitions(from).contains(target)) throw new BusinessException("不允许从 " + from + " 变更为 " + target);
    String note = trim(request.note());
    if ((target == ControlStatus.COMPLETED || target == ControlStatus.CANCELLED) && note == null) {
      throw new BusinessException("完成或取消时必须填写说明");
    }
    if (target == ControlStatus.ACTIVE) {
      validateActivation(item);
      if (item.getActivatedAt() == null) item.setActivatedAt(OffsetDateTime.now());
    }
    if (target == ControlStatus.COMPLETED) {
      item.setProgressPercent(new BigDecimal("100.00"));
      item.setCompletedAt(OffsetDateTime.now()); item.setCompletedBy(principalName()); item.setCompletionNote(note);
    } else if (target == ControlStatus.CANCELLED) {
      item.setCompletedAt(OffsetDateTime.now()); item.setCompletedBy(principalName()); item.setCompletionNote(note);
    } else if (target == ControlStatus.BLOCKED) {
      if (note == null) throw new BusinessException("标记阻塞时必须填写原因");
      item.setCompletionNote(note);
    }
    item.setStatus(target);
    BusinessControlRecord saved = controls.save(item);
    logAction("CONTROL", saved.getId(), saved.getControlCode(), "TRANSITION", from.name(), target.name(), note);
    return toControl(saved);
  }

  @Transactional
  public ControlResponse review(UUID id, ReviewControlRequest request) {
    BusinessControlRecord item = requireControl(id);
    if (!OPEN_STATUSES.contains(item.getStatus())) throw new BusinessException("只有执行中的记录可以复核");
    if (request.reviewedOn().isAfter(LocalDate.now())) throw new BusinessException("复核日期不能晚于今天");
    item.setLastReviewedOn(request.reviewedOn());
    if (item.getReviewFrequencyDays() != null) item.setNextReviewOn(request.reviewedOn().plusDays(item.getReviewFrequencyDays()));
    BusinessControlRecord saved = controls.save(item);
    logAction("CONTROL", saved.getId(), saved.getControlCode(), "REVIEW", saved.getStatus().name(), saved.getStatus().name(), trim(request.note()));
    return toControl(saved);
  }

  @Transactional(readOnly = true)
  public List<ControlExceptionResponse> exceptions() {
    LocalDate today = LocalDate.now();
    List<ControlExceptionResponse> result = new ArrayList<>();
    for (BusinessControlRecord item : controls.findByStatusInOrderByCreatedAtDesc(OPEN_STATUSES)) {
      if (!OPEN_STATUSES.contains(item.getStatus())) continue;
      if (item.getPlannedEnd() != null && item.getPlannedEnd().isBefore(today)) {
        addException(result, item, "OVERDUE", "HIGH", "计划完成日期已逾期", item.getPlannedEnd(), exposure(item));
      }
      if (item.getNextReviewOn() != null && !item.getNextReviewOn().isAfter(today)) {
        addException(result, item, "REVIEW_DUE", "MEDIUM", "周期复核已到期", item.getNextReviewOn(), BigDecimal.ZERO);
      }
      if (item.getEffectiveTo() != null && !item.getEffectiveTo().isAfter(today.plusDays(30))) {
        addException(result, item, "EXPIRING", item.getEffectiveTo().isBefore(today) ? "HIGH" : "MEDIUM",
            "生效期限将在30天内到期", item.getEffectiveTo(), exposure(item));
      }
      if (positive(item.getBudgetAmount()) && item.getCommittedAmount().compareTo(item.getBudgetAmount()) > 0) {
        addException(result, item, "COMMITMENT_OVERRUN", "HIGH", "承诺金额超过预算",
            item.getPlannedEnd(), item.getCommittedAmount().subtract(item.getBudgetAmount()));
      }
      BigDecimal latest = item.getForecastAmount().max(item.getActualAmount());
      if (positive(item.getBudgetAmount()) && latest.compareTo(item.getBudgetAmount()) > 0) {
        addException(result, item, "FORECAST_OVERRUN", "HIGH", "实际或预测金额超过预算",
            item.getPlannedEnd(), latest.subtract(item.getBudgetAmount()));
      }
    }
    return result.stream().sorted(Comparator.comparing(ControlExceptionResponse::severity)
        .thenComparing(item -> item.dueDate() == null ? LocalDate.MAX : item.dueDate())).toList();
  }

  @Transactional(readOnly = true)
  public List<GovernanceActionResponse> actions(String entityType, UUID entityId) {
    String type = entityType == null ? "" : entityType.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("CONTROL", "PERIOD", "BANK_LINE").contains(type)) throw new BusinessException("不支持的治理对象类型");
    return actionLogs.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(type, entityId).stream()
        .map(item -> new GovernanceActionResponse(item.getId(), item.getEntityType(), item.getEntityId(),
            item.getEntityNo(), item.getActionType(), item.getFromStatus(), item.getToStatus(),
            item.getOperatorName(), item.getNote(), item.getCreatedAt())).toList();
  }

  @Transactional(readOnly = true)
  @Cacheable("governanceOverview")
  public GovernanceOverview overview() {
    List<BusinessControlRecord> all = controls.findAllByOrderByCreatedAtDesc();
    List<ControlExceptionResponse> issues = exceptions();
    Set<UUID> exceptionIds = issues.stream().map(ControlExceptionResponse::controlId).collect(Collectors.toSet());
    Map<String, List<BusinessControlRecord>> byDomain = all.stream()
        .collect(Collectors.groupingBy(BusinessControlRecord::getBusinessDomain, LinkedHashMap::new, Collectors.toList()));
    List<DomainSummary> domains = byDomain.entrySet().stream().map(entry -> new DomainSummary(
        entry.getKey(), entry.getValue().size(),
        entry.getValue().stream().filter(item -> OPEN_STATUSES.contains(item.getStatus())).count(),
        entry.getValue().stream().filter(item -> exceptionIds.contains(item.getId())).count(),
        sum(entry.getValue(), this::exposure))).toList();
    BigDecimal budget = sum(all, BusinessControlRecord::getBudgetAmount);
    BigDecimal forecast = sum(all, BusinessControlRecord::getForecastAmount);
    long matchedBankLines = bankLines.countByReconciliationStatus(ReconciliationStatus.MATCHED);
    long unmatchedBankLines = bankLines.count() - matchedBankLines;
    return new GovernanceOverview(
        all.size(), count(all, ControlStatus.ACTIVE), count(all, ControlStatus.BLOCKED),
        issues.stream().filter(item -> "OVERDUE".equals(item.exceptionType())).map(ControlExceptionResponse::controlId).distinct().count(),
        all.stream().filter(item -> "HIGH".equals(item.getRiskLevel()) && OPEN_STATUSES.contains(item.getStatus())).count(),
        budget, sum(all, BusinessControlRecord::getCommittedAmount), sum(all, BusinessControlRecord::getActualAmount),
        forecast, forecast.subtract(budget),
        unmatchedBankLines, matchedBankLines,
        periods.countByStatus(AccountingPeriodStatus.CLOSED), domains);
  }

  @Transactional(readOnly = true)
  public List<PeriodResponse> periods() {
    return periods.findAllByOrderByFiscalYearDescPeriodNoDesc().stream().map(this::toPeriod).toList();
  }

  @Transactional
  public PeriodResponse openPeriod(OpenPeriodRequest request) {
    AccountingPeriod item = periods.findByFiscalYearAndPeriodNo(request.fiscalYear(), request.periodNo()).orElseGet(AccountingPeriod::new);
    if (item.getId() != null && item.getStatus() == AccountingPeriodStatus.CLOSED) throw new BusinessException("已关闭期间必须通过反结账重新打开");
    if (item.getId() != null && item.getPendingAction() != null) throw new BusinessException("该期间存在待复核操作，请先完成复核");
    item.setFiscalYear(request.fiscalYear()); item.setPeriodNo(request.periodNo()); item.setStatus(AccountingPeriodStatus.OPEN);
    if (item.getOpenedAt() == null) item.setOpenedAt(OffsetDateTime.now());
    AccountingPeriod saved = periods.save(item);
    logAction("PERIOD", saved.getId(), periodNo(saved), "OPEN", null, saved.getStatus().name(), null);
    return toPeriod(saved);
  }

  @Transactional(readOnly = true)
  public CloseReadinessResponse closeReadiness(int year, int month) {
    YearMonth ym = YearMonth.of(year, month);
    LocalDate from = ym.atDay(1), to = ym.atEndOfMonth();
    List<String> blockers = new ArrayList<>();
    long draftVouchers = vouchers.countByVoucherDateBetweenAndStatusIn(from, to,
        EnumSet.of(VoucherStatus.DRAFT, VoucherStatus.REVIEWED));
    if (draftVouchers > 0) blockers.add("存在 " + draftVouchers + " 张未记账凭证");
    long highRisk = controls.countHighRiskDue("HIGH", OPEN_STATUSES, to);
    if (highRisk > 0) blockers.add("存在 " + highRisk + " 项到期未闭环的高风险经营控制");
    long unmatched = bankLines.countByReconciliationStatusNotAndTransactionDateLessThanEqual(ReconciliationStatus.MATCHED, to);
    if (unmatched > 0) blockers.add("截至期末仍有 " + unmatched + " 条银行流水未完成对账");
    blockers.addAll(financeOperations.periodCloseBlockers(year, month));
    return new CloseReadinessResponse(blockers.isEmpty(), blockers);
  }

  @Transactional
  public PeriodResponse closePeriod(int year, int month, ClosePeriodRequest request) {
    AccountingPeriod item = periods.findByFiscalYearAndPeriodNo(year, month)
        .orElseThrow(() -> new BusinessException("请先开启该会计期间"));
    if (item.getStatus() == AccountingPeriodStatus.CLOSED) throw new BusinessException("该期间已经关账");
    CloseReadinessResponse readiness = closeReadiness(year, month);
    String reason = trim(request.reason());
    if (!readiness.ready() && !request.force()) throw new BusinessException(String.join("；", readiness.blockers()));
    if (!readiness.ready() && request.force() && reason == null) throw new BusinessException("强制关账必须填写原因");
    if (!readiness.ready()) {
      if (!"FORCE_CLOSE".equals(item.getPendingAction())) {
        requestPeriodAction(item, "FORCE_CLOSE", reason);
        AccountingPeriod requested = periods.save(item);
        logAction("PERIOD", requested.getId(), periodNo(requested), "REQUEST_FORCE_CLOSE",
            requested.getStatus().name(), requested.getStatus().name(), reason);
        return toPeriod(requested);
      }
      requireIndependentReviewer(item);
      reason = item.getActionRequestReason();
    }
    item.setStatus(AccountingPeriodStatus.CLOSING); item.setClosingStartedAt(OffsetDateTime.now());
    item.setStatus(AccountingPeriodStatus.CLOSED); item.setClosedAt(OffsetDateTime.now());
    item.setClosedBy(principalName()); item.setCloseReason(reason);
    clearPeriodAction(item);
    AccountingPeriod saved = periods.save(item);
    logAction("PERIOD", saved.getId(), periodNo(saved), "CLOSE", AccountingPeriodStatus.OPEN.name(), saved.getStatus().name(), reason);
    financeOperations.capturePeriodClose(year, month, Map.of(
        "period", periodNo(saved), "closedBy", saved.getClosedBy(), "closedAt", saved.getClosedAt(),
        "forced", request.force(), "reason", reason == null ? "" : reason,
        "readiness", readiness));
    return toPeriod(saved);
  }

  @Transactional
  public PeriodResponse reopenPeriod(int year, int month, ReopenPeriodRequest request) {
    AccountingPeriod item = periods.findByFiscalYearAndPeriodNo(year, month)
        .orElseThrow(() -> new BusinessException("会计期间不存在"));
    if (item.getStatus() != AccountingPeriodStatus.CLOSED) throw new BusinessException("只有已关账期间可以反结账");
    if (!"REOPEN".equals(item.getPendingAction())) {
      requestPeriodAction(item, "REOPEN", request.reason().trim());
      AccountingPeriod requested = periods.save(item);
      logAction("PERIOD", requested.getId(), periodNo(requested), "REQUEST_REOPEN",
          AccountingPeriodStatus.CLOSED.name(), AccountingPeriodStatus.CLOSED.name(), request.reason().trim());
      return toPeriod(requested);
    }
    requireIndependentReviewer(item);
    String approvedReason = item.getActionRequestReason();
    item.setStatus(AccountingPeriodStatus.OPEN); item.setReopenedAt(OffsetDateTime.now());
    item.setReopenedBy(principalName()); item.setReopenReason(approvedReason);
    clearPeriodAction(item);
    AccountingPeriod saved = periods.save(item);
    logAction("PERIOD", saved.getId(), periodNo(saved), "REOPEN", AccountingPeriodStatus.CLOSED.name(), saved.getStatus().name(), approvedReason);
    return toPeriod(saved);
  }

  private void requestPeriodAction(AccountingPeriod item, String action, String reason) {
    item.setPendingAction(action);
    item.setActionRequestedById(principalId());
    item.setActionRequestedBy(principalName());
    item.setActionRequestedAt(OffsetDateTime.now());
    item.setActionRequestReason(reason);
  }

  private void requireIndependentReviewer(AccountingPeriod item) {
    UUID currentId = principalId();
    boolean same = item.getActionRequestedById() != null && currentId != null
        ? item.getActionRequestedById().equals(currentId)
        : Objects.equals(item.getActionRequestedBy(), principalName());
    if (same) throw new BusinessException("该操作正在等待其他财务人员复核");
  }

  private void clearPeriodAction(AccountingPeriod item) {
    item.setPendingAction(null); item.setActionRequestedById(null); item.setActionRequestedBy(null);
    item.setActionRequestedAt(null); item.setActionRequestReason(null);
  }

  @Transactional(readOnly = true)
  public Page<BankLineResponse> bankLines(ReconciliationStatus status, Pageable pageable) {
    Page<BankStatementLine> page = status == null
        ? bankLines.findAllByOrderByTransactionDateDescCreatedAtDesc(pageable)
        : bankLines.findByReconciliationStatusOrderByTransactionDateDescCreatedAtDesc(status, pageable);
    return page.map(this::toBankLine);
  }

  @Transactional
  public BankImportResponse importBankStatement(ImportBankStatementRequest request) {
    int imported = 0, duplicates = 0, suggested = 0;
    for (ImportBankLine line : request.lines()) {
      String direction = line.direction().trim().toUpperCase(Locale.ROOT);
      if (!direction.equals("IN") && !direction.equals("OUT")) throw new BusinessException("流水方向只能是 IN 或 OUT");
      if (bankLines.findByAccountNoMaskedAndBankReference(line.accountNoMasked().trim(), line.bankReference().trim()).isPresent()) {
        duplicates++; continue;
      }
      BankStatementLine item = new BankStatementLine();
      item.setAccountNoMasked(line.accountNoMasked().trim()); item.setTransactionDate(line.transactionDate());
      item.setDirection(direction); item.setAmount(money(line.amount())); item.setCounterparty(trim(line.counterparty()));
      item.setBankReference(line.bankReference().trim()); item.setSummary(trim(line.summary()));
      if (suggestMatch(item)) suggested++;
      bankLines.save(item); imported++;
    }
    return new BankImportResponse(imported, duplicates, suggested);
  }

  @Transactional
  public BankLineResponse reconcile(UUID id, ReconcileBankLineRequest request) {
    BankStatementLine line = requireBankLine(id);
    String type = request.businessType().trim().toUpperCase(Locale.ROOT);
    validateBankMatch(line, type, request.businessId());
    if (bankLines.existsByMatchedBizTypeAndMatchedBizIdAndReconciliationStatus(type, request.businessId(), ReconciliationStatus.MATCHED)
        && !(type.equals(line.getMatchedBizType()) && request.businessId().equals(line.getMatchedBizId()))) {
      throw new BusinessException("该业务记录已经与其他银行流水完成匹配");
    }
    line.setMatchedBizType(type); line.setMatchedBizId(request.businessId()); line.setMatchedBizNo(request.businessNo().trim());
    line.setReconciliationStatus(ReconciliationStatus.MATCHED); line.setMatchedAt(OffsetDateTime.now());
    line.setMatchedBy(principalName()); line.setMatchNote(trim(request.note()));
    BankStatementLine saved = bankLines.save(line);
    logAction("BANK_LINE", saved.getId(), saved.getBankReference(), "RECONCILE", null, saved.getReconciliationStatus().name(), trim(request.note()));
    return toBankLine(saved);
  }

  @Transactional
  public BankLineResponse unreconcile(UUID id, String reason) {
    BankStatementLine line = requireBankLine(id);
    if (line.getReconciliationStatus() != ReconciliationStatus.MATCHED) throw new BusinessException("该流水尚未完成匹配");
    if (trim(reason) == null) throw new BusinessException("解除匹配必须填写原因");
    line.setReconciliationStatus(ReconciliationStatus.UNMATCHED); line.setMatchNote("解除匹配：" + reason.trim());
    line.setMatchedAt(null); line.setMatchedBy(null); line.setMatchedBizType(null); line.setMatchedBizId(null); line.setMatchedBizNo(null);
    BankStatementLine saved = bankLines.save(line);
    logAction("BANK_LINE", saved.getId(), saved.getBankReference(), "UNRECONCILE", ReconciliationStatus.MATCHED.name(), saved.getReconciliationStatus().name(), reason.trim());
    return toBankLine(saved);
  }

  private void apply(BusinessControlRecord item, SaveControlRequest request) {
    item.setControlType(request.controlType()); item.setBusinessDomain(request.controlType().domain());
    item.setBusinessId(request.businessId()); item.setBusinessNo(trim(request.businessNo())); item.setName(request.name().trim());
    item.setOwner(request.owner().trim()); item.setRiskLevel(normalizeRisk(request.riskLevel()));
    item.setPlannedStart(request.plannedStart()); item.setPlannedEnd(request.plannedEnd());
    item.setEffectiveFrom(request.effectiveFrom()); item.setEffectiveTo(request.effectiveTo());
    item.setBudgetAmount(money(request.budgetAmount())); item.setCommittedAmount(money(request.committedAmount()));
    item.setActualAmount(money(request.actualAmount())); item.setForecastAmount(money(request.forecastAmount()));
    item.setProgressPercent(percent(request.progressPercent())); item.setReviewFrequencyDays(request.reviewFrequencyDays());
    if (request.reviewFrequencyDays() != null && item.getLastReviewedOn() != null) {
      item.setNextReviewOn(item.getLastReviewedOn().plusDays(request.reviewFrequencyDays()));
    }
    item.setDetails(writeDetails(request.details()));
  }

  private void validateControl(SaveControlRequest request) {
    if (request.plannedStart() != null && request.plannedEnd() != null && request.plannedEnd().isBefore(request.plannedStart())) {
      throw new BusinessException("计划结束日期不能早于开始日期");
    }
    if (request.effectiveFrom() != null && request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
      throw new BusinessException("失效日期不能早于生效日期");
    }
    normalizeRisk(request.riskLevel()); writeDetails(request.details());
  }

  private void validateActivation(BusinessControlRecord item) {
    if (DATE_REQUIRED_TYPES.contains(item.getControlType()) && item.getPlannedEnd() == null) {
      throw new BusinessException(item.getControlType().label() + "必须设置计划完成日期");
    }
    if (BUDGET_TYPES.contains(item.getControlType()) && !positive(item.getBudgetAmount())) {
      throw new BusinessException(item.getControlType().label() + "必须设置预算或基准金额");
    }
  }

  private boolean suggestMatch(BankStatementLine line) {
    if ("IN".equals(line.getDirection())) {
      List<ReceivableReceipt> candidates = receipts.findAll().stream()
          .filter(item -> item.getAmount().compareTo(line.getAmount()) == 0)
          .filter(item -> Math.abs(ChronoUnit.DAYS.between(item.getReceivedDate(), line.getTransactionDate())) <= 3)
          .filter(item -> !bankLines.existsByMatchedBizTypeAndMatchedBizIdAndReconciliationStatus("RECEIPT", item.getId(), ReconciliationStatus.MATCHED)).toList();
      if (candidates.size() == 1) {
        ReceivableReceipt item = candidates.get(0); line.setReconciliationStatus(ReconciliationStatus.SUGGESTED);
        line.setMatchedBizType("RECEIPT"); line.setMatchedBizId(item.getId()); line.setMatchedBizNo(item.getReferenceNo()); return true;
      }
    } else {
      List<PaymentRecord> candidates = payments.findAll().stream()
          .filter(item -> item.getAmount().compareTo(line.getAmount()) == 0)
          .filter(item -> Math.abs(ChronoUnit.DAYS.between(item.getPaidDate(), line.getTransactionDate())) <= 3)
          .filter(item -> !bankLines.existsByMatchedBizTypeAndMatchedBizIdAndReconciliationStatus("PAYMENT", item.getId(), ReconciliationStatus.MATCHED)).toList();
      if (candidates.size() == 1) {
        PaymentRecord item = candidates.get(0); line.setReconciliationStatus(ReconciliationStatus.SUGGESTED);
        line.setMatchedBizType("PAYMENT"); line.setMatchedBizId(item.getId()); line.setMatchedBizNo(item.getCode()); return true;
      }
    }
    return false;
  }

  private void validateBankMatch(BankStatementLine line, String type, UUID id) {
    if ("RECEIPT".equals(type)) {
      if (!"IN".equals(line.getDirection())) throw new BusinessException("收款只能匹配银行入账流水");
      ReceivableReceipt receipt = receipts.findById(id).orElseThrow(() -> new BusinessException("收款记录不存在"));
      if (receipt.getAmount().compareTo(line.getAmount()) != 0) throw new BusinessException("银行流水与收款金额不一致");
    } else if ("PAYMENT".equals(type)) {
      if (!"OUT".equals(line.getDirection())) throw new BusinessException("付款只能匹配银行出账流水");
      PaymentRecord payment = payments.findById(id).orElseThrow(() -> new BusinessException("付款记录不存在"));
      if (payment.getAmount().compareTo(line.getAmount()) != 0) throw new BusinessException("银行流水与付款金额不一致");
    } else throw new BusinessException("银行流水仅支持匹配 RECEIPT 或 PAYMENT");
  }

  private Set<ControlStatus> allowedTransitions(ControlStatus from) {
    return switch (from) {
      case DRAFT -> EnumSet.of(ControlStatus.ACTIVE, ControlStatus.CANCELLED);
      case ACTIVE -> EnumSet.of(ControlStatus.BLOCKED, ControlStatus.COMPLETED, ControlStatus.CANCELLED);
      case BLOCKED -> EnumSet.of(ControlStatus.ACTIVE, ControlStatus.CANCELLED);
      case COMPLETED, CANCELLED -> EnumSet.noneOf(ControlStatus.class);
    };
  }

  private void addException(List<ControlExceptionResponse> result, BusinessControlRecord item,
      String type, String severity, String message, LocalDate dueDate, BigDecimal exposure) {
    result.add(new ControlExceptionResponse(item.getId() + ":" + type, item.getId(), item.getControlCode(),
        item.getControlType(), item.getBusinessDomain(), item.getName(), item.getOwner(), type, severity,
        message, dueDate, money(exposure)));
  }

  private BusinessControlRecord requireControl(UUID id) { return controls.findById(id).orElseThrow(() -> new BusinessException("经营控制记录不存在")); }
  private BankStatementLine requireBankLine(UUID id) { return bankLines.findById(id).orElseThrow(() -> new BusinessException("银行流水不存在")); }
  private String nextControlCode(ControlType type) {
    String prefix = "CTL-" + type.domain().substring(0, Math.min(3, type.domain().length())) + "-";
    String code;
    do { code = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT); }
    while (controls.existsByControlCode(code));
    return code;
  }
  private String normalizeRisk(String value) {
    String risk = value == null || value.isBlank() ? "LOW" : value.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("LOW", "MEDIUM", "HIGH").contains(risk)) throw new BusinessException("风险等级只能是 LOW、MEDIUM 或 HIGH");
    return risk;
  }
  private String writeDetails(Map<String, Object> details) {
    try { return objectMapper.writeValueAsString(details == null ? Map.of() : details); }
    catch (Exception ex) { throw new BusinessException("扩展业务数据不是有效JSON对象"); }
  }
  private Map<String, Object> readDetails(String value) {
    if (value == null || value.isBlank()) return Map.of();
    try { return objectMapper.readValue(value, new TypeReference<>() {}); }
    catch (Exception ex) { return Map.of("raw", value); }
  }
  private String principalName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) throw new AccessDeniedException("请先登录");
    return principal.displayName();
  }
  private UUID principalId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) throw new AccessDeniedException("请先登录");
    return principal.id();
  }
  private String principalNameOrSystem() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }
  private void logAction(String entityType, UUID entityId, String entityNo, String actionType,
      String fromStatus, String toStatus, String note) {
    GovernanceActionLog log = new GovernanceActionLog();
    log.setEntityType(entityType); log.setEntityId(entityId); log.setEntityNo(entityNo);
    log.setActionType(actionType); log.setFromStatus(fromStatus); log.setToStatus(toStatus);
    log.setOperatorName(principalNameOrSystem()); log.setNote(note);
    actionLogs.save(log);
  }
  private String periodNo(AccountingPeriod item) {
    return item.getFiscalYear() + "-" + String.format("%02d", item.getPeriodNo());
  }
  private BigDecimal money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }
  private BigDecimal percent(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }
  private boolean positive(BigDecimal value) { return value != null && value.compareTo(BigDecimal.ZERO) > 0; }
  private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  private long count(List<BusinessControlRecord> all, ControlStatus status) { return all.stream().filter(item -> item.getStatus() == status).count(); }
  private BigDecimal exposure(BusinessControlRecord item) {
    return item.getForecastAmount().max(item.getActualAmount()).max(item.getCommittedAmount()).subtract(item.getBudgetAmount()).max(BigDecimal.ZERO);
  }
  private BigDecimal sum(List<BusinessControlRecord> all, Function<BusinessControlRecord, BigDecimal> getter) {
    return all.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
  }

  private ControlResponse toControl(BusinessControlRecord item) {
    return new ControlResponse(item.getId(), item.getControlCode(), item.getControlType(), item.getControlType().label(),
        item.getBusinessDomain(), item.getBusinessId(), item.getBusinessNo(), item.getName(), item.getOwner(), item.getStatus(),
        item.getRiskLevel(), item.getPlannedStart(), item.getPlannedEnd(), item.getEffectiveFrom(), item.getEffectiveTo(),
        item.getBudgetAmount(), item.getCommittedAmount(), item.getActualAmount(), item.getForecastAmount(), item.getProgressPercent(),
        item.getReviewFrequencyDays(), item.getLastReviewedOn(), item.getNextReviewOn(), readDetails(item.getDetails()),
        item.getActivatedAt(), item.getCompletedAt(), item.getCompletedBy(), item.getCompletionNote(), item.getCreatedAt(), item.getUpdatedAt());
  }
  private PeriodResponse toPeriod(AccountingPeriod item) {
    return new PeriodResponse(item.getId(), item.getFiscalYear(), item.getPeriodNo(), item.getStatus(), item.getOpenedAt(),
        item.getClosingStartedAt(), item.getClosedAt(), item.getClosedBy(), item.getCloseReason(), item.getReopenedAt(),
        item.getReopenedBy(), item.getReopenReason(), item.getPendingAction(), item.getActionRequestedBy(),
        item.getActionRequestedAt(), item.getActionRequestReason());
  }
  private BankLineResponse toBankLine(BankStatementLine item) {
    return new BankLineResponse(item.getId(), item.getAccountNoMasked(), item.getTransactionDate(), item.getDirection(),
        item.getAmount(), item.getCounterparty(), item.getBankReference(), item.getSummary(), item.getReconciliationStatus(),
        item.getMatchedBizType(), item.getMatchedBizId(), item.getMatchedBizNo(), item.getMatchedAt(), item.getMatchedBy(), item.getMatchNote());
  }
}
