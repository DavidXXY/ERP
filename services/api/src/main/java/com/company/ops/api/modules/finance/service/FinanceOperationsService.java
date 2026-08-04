package com.company.ops.api.modules.finance.service;

import static com.company.ops.api.modules.finance.dto.FinanceOperationsDtos.*;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.finance.dto.FinanceAnalyticsDtos.TaxInvoiceLine;
import com.company.ops.api.modules.governance.domain.BusinessControlRecord;
import com.company.ops.api.modules.governance.domain.ControlType;
import com.company.ops.api.modules.governance.repository.BusinessControlRecordRepository;
import com.company.ops.api.modules.ledger.domain.AccountOpeningBalance;
import com.company.ops.api.modules.ledger.repository.AccountOpeningBalanceRepository;
import com.company.ops.api.modules.ledger.repository.AccountingEntryRepository;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceOperationsService {
  private static final Set<String> PERIOD_TYPES = Set.of("ACCRUAL", "AMORTIZATION", "DEPRECIATION", "PROFIT_CARRY_FORWARD");
  private static final Set<String> PARTNER_STATUSES = Set.of("MATCHED", "DISPUTED", "CONFIRMED");
  private static final Set<ControlType> BUDGET_TYPES = EnumSet.of(ControlType.PROJECT_FORECAST,
      ControlType.PURCHASE_BUDGET, ControlType.CASH_FORECAST, ControlType.BUSINESS_FORECAST, ControlType.FIXED_ASSET);

  private final JdbcTemplate jdbc;
  private final VoucherGenerationService voucherGeneration;
  private final AccountOpeningBalanceRepository openings;
  private final AccountingEntryRepository entries;
  private final BusinessControlRecordRepository controls;
  private final CustomerRepository customers;
  private final ReceivableRepository receivables;
  private final SupplierRepository suppliers;
  private final ProcurementPayableRepository payables;
  private final FinanceAnalyticsService analytics;
  private final ObjectMapper objectMapper;

  public FinanceOperationsService(JdbcTemplate jdbc, VoucherGenerationService voucherGeneration,
      AccountOpeningBalanceRepository openings, AccountingEntryRepository entries,
      BusinessControlRecordRepository controls, CustomerRepository customers,
      ReceivableRepository receivables, SupplierRepository suppliers,
      ProcurementPayableRepository payables, FinanceAnalyticsService analytics, ObjectMapper objectMapper) {
    this.jdbc = jdbc; this.voucherGeneration = voucherGeneration; this.openings = openings; this.entries = entries;
    this.controls = controls; this.customers = customers; this.receivables = receivables;
    this.suppliers = suppliers; this.payables = payables; this.analytics = analytics; this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public OperationsOverview overview() {
    String tenant = tenant();
    long pending = count("fin_period_end_jobs", "status='PENDING'");
    long failed = count("fin_voucher_generation_requests", "status='FAILED'");
    long partner = count("fin_partner_reconciliations", "status in ('PENDING','DISPUTED')");
    long tax = count("fin_tax_filings", "status<>'LOCKED'");
    long consolidation = count("fin_consolidation_runs", "status='DRAFT'");
    long snapshotCount = count("fin_report_snapshots", "1=1");
    BigDecimal budgetVariance = budgetVariance().stream().map(BudgetVarianceResponse::variance)
        .filter(value -> value.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal liquidity = jdbc.query("select forecast_cash from fin_cash_forecast_scenarios where tenant_id=? order by created_at desc",
        rs -> rs.next() ? money(rs.getBigDecimal(1)) : BigDecimal.ZERO, tenant);
    return new OperationsOverview(pending, failed, partner, tax, consolidation, snapshotCount, money(budgetVariance), liquidity);
  }

  public List<PeriodJobResponse> periodJobs(Integer year, Integer month) {
    String sql = "select * from fin_period_end_jobs where tenant_id=?";
    List<Object> args = new ArrayList<>(); args.add(tenant());
    if (year != null) { sql += " and fiscal_year=?"; args.add(year); }
    if (month != null) { sql += " and period_no=?"; args.add(month); }
    sql += " order by fiscal_year desc,period_no desc,created_at desc";
    return jdbc.query(sql, this::mapPeriodJob, args.toArray());
  }

  public PeriodJobResponse createPeriodJob(SavePeriodJobRequest request) {
    String type = upper(request.processType());
    if (!PERIOD_TYPES.contains(type)) throw new BusinessException("不支持的期末处理类型");
    if (request.debitAccountCode().trim().equals(request.creditAccountCode().trim())) throw new BusinessException("借贷科目不能相同");
    if (request.autoReverse() && request.reversalDate() == null) throw new BusinessException("自动转回必须设置转回日期");
    LocalDate periodEnd = YearMonth.of(request.fiscalYear(), request.periodNo()).atEndOfMonth();
    if (request.reversalDate() != null && !request.reversalDate().isAfter(periodEnd)) throw new BusinessException("转回日期必须晚于期末日期");
    UUID id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now();
    try {
      jdbc.update("insert into fin_period_end_jobs(id,tenant_id,fiscal_year,period_no,process_type,description,amount,"
              + "debit_account_code,credit_account_code,auto_reverse,reversal_date,status,idempotency_key,created_at,updated_at,created_by,version) "
              + "values(?,?,?,?,?,?,?,?,?,?,?,'PENDING',?,?,?,?,0)",
          id, tenant(), request.fiscalYear(), request.periodNo(), type, request.description().trim(), money(request.amount()),
          request.debitAccountCode().trim(), request.creditAccountCode().trim(), request.autoReverse(), request.reversalDate(),
          request.idempotencyKey().trim(), now, now, username());
    } catch (DuplicateKeyException ex) { throw new BusinessException("该幂等键已存在，请直接重试原任务"); }
    return requirePeriodJob(id);
  }

  public PeriodJobResponse executePeriodJob(UUID id) {
    PeriodJobResponse job = requirePeriodJob(id);
    if (Set.of("COMPLETED", "REVERSED").contains(job.status())) return job;
    LocalDate voucherDate = YearMonth.of(job.fiscalYear(), job.periodNo()).atEndOfMonth();
    try {
      UUID voucherId = voucherGeneration.generate(job.idempotencyKey(), "PERIOD_END",
          "PERIOD-" + job.id(), voucherDate, job.description(), job.debitAccountCode(), job.creditAccountCode(), job.amount());
      jdbc.update("update fin_period_end_jobs set status='COMPLETED',voucher_id=?,executed_at=?,executed_by=?,updated_at=? "
          + "where tenant_id=? and id=?", voucherId, OffsetDateTime.now(), displayName(), OffsetDateTime.now(), tenant(), id);
    } catch (RuntimeException ex) {
      jdbc.update("update fin_period_end_jobs set status='FAILED',updated_at=? where tenant_id=? and id=?", OffsetDateTime.now(), tenant(), id);
      throw ex;
    }
    return requirePeriodJob(id);
  }

  public List<PeriodJobResponse> reverseDueJobs(LocalDate requestedAsOf) {
    LocalDate asOf = requestedAsOf == null ? LocalDate.now() : requestedAsOf;
    List<PeriodJobResponse> due = periodJobs(null, null).stream()
        .filter(item -> item.autoReverse() && "COMPLETED".equals(item.status()))
        .filter(item -> item.reversalDate() != null && !item.reversalDate().isAfter(asOf)).toList();
    for (PeriodJobResponse job : due) {
      UUID reversalId = voucherGeneration.compensate(job.idempotencyKey(), job.reversalDate(), "期末处理到期自动转回");
      jdbc.update("update fin_period_end_jobs set status='REVERSED',reversal_voucher_id=?,updated_at=? where tenant_id=? and id=?",
          reversalId, OffsetDateTime.now(), tenant(), job.id());
    }
    return periodJobs(null, null);
  }

  @Transactional(readOnly = true)
  public OpeningValidationResponse validateOpening(int fiscalYear) {
    List<AccountOpeningBalance> rows = openings.findByFiscalYearOrderByAccountCodeAsc(fiscalYear);
    BigDecimal debit = sum(rows, AccountOpeningBalance::getDebitBalance);
    BigDecimal credit = sum(rows, AccountOpeningBalance::getCreditBalance);
    BigDecimal difference = debit.subtract(credit).abs();
    List<OpeningValidationLine> issues = new ArrayList<>();
    if (rows.isEmpty()) issues.add(new OpeningValidationLine("NO_OPENING", "HIGH", "该年度尚未录入期初余额", BigDecimal.ZERO));
    if (difference.signum() != 0) issues.add(new OpeningValidationLine("UNBALANCED", "HIGH", "期初借贷不平衡", difference));
    rows.stream().filter(item -> item.getDebitBalance().signum() > 0 && item.getCreditBalance().signum() > 0)
        .forEach(item -> issues.add(new OpeningValidationLine("DUAL_DIRECTION", "HIGH", "科目 " + item.getAccountCode() + " 同时存在借贷余额", BigDecimal.ZERO)));
    if (fiscalYear > 2000) {
      List<Object[]> prior = entries.aggregateByAccountBetween(LocalDate.of(fiscalYear - 1, 1, 1), LocalDate.of(fiscalYear - 1, 12, 31));
      if (!prior.isEmpty() && rows.isEmpty()) issues.add(new OpeningValidationLine("PRIOR_NOT_CARRIED", "HIGH", "上年存在发生额但本年未结转期初", BigDecimal.ZERO));
    }
    return new OpeningValidationResponse(fiscalYear, issues.isEmpty(), money(debit), money(credit), money(difference), issues);
  }

  @Transactional(readOnly = true)
  public List<BudgetVarianceResponse> budgetVariance() {
    return controls.findAllByOrderByCreatedAtDesc().stream().filter(item -> BUDGET_TYPES.contains(item.getControlType()))
        .map(item -> {
          BigDecimal budget = money(item.getBudgetAmount()); BigDecimal actual = money(item.getActualAmount());
          BigDecimal forecast = money(item.getForecastAmount()); BigDecimal latest = actual.max(forecast);
          BigDecimal variance = latest.subtract(budget); BigDecimal usage = budget.signum() == 0 ? BigDecimal.ZERO
              : latest.multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP);
          String status = budget.signum() == 0 ? "NO_BASELINE" : variance.signum() > 0 ? "OVERRUN" : usage.compareTo(BigDecimal.valueOf(90)) >= 0 ? "WARNING" : "NORMAL";
          return new BudgetVarianceResponse(item.getId(), item.getControlCode(), item.getName(), item.getOwner(), budget,
              money(item.getCommittedAmount()), actual, forecast, money(variance), usage, status);
        }).toList();
  }

  @Transactional(readOnly = true)
  public List<PartnerStatementResponse> partnerStatements(String requestedType, LocalDate requestedEnd) {
    String type = upper(requestedType); LocalDate end = requestedEnd == null ? LocalDate.now() : requestedEnd;
    if (!Set.of("CUSTOMER", "SUPPLIER").contains(type)) throw new BusinessException("往来类型只能是 CUSTOMER 或 SUPPLIER");
    Map<UUID, ReconciliationRef> refs = reconciliationRefs(type, end);
    if ("CUSTOMER".equals(type)) {
      Map<UUID, Customer> partners = customers.findAll().stream().collect(Collectors.toMap(Customer::getId, Function.identity()));
      return receivables.findAll().stream().filter(item -> !item.getDueDate().isAfter(end))
          .collect(Collectors.groupingBy(Receivable::getCustomerId)).entrySet().stream().map(entry -> {
            Customer partner = partners.get(entry.getKey()); BigDecimal balance = entry.getValue().stream()
                .map(item -> item.getAmount().subtract(item.getSettledAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
            return partnerStatement(type, entry.getKey(), partner == null ? "-" : partner.getCode(), partner == null ? "未知客户" : partner.getName(), end, balance, refs.get(entry.getKey()));
          }).sorted(Comparator.comparing(PartnerStatementResponse::partnerName)).toList();
    }
    Map<UUID, Supplier> partners = suppliers.findAll().stream().collect(Collectors.toMap(Supplier::getId, Function.identity()));
    return payables.findAll().stream().filter(item -> !item.getDueDate().isAfter(end)).filter(item -> item.getStatus() != PayableStatus.CANCELLED)
        .collect(Collectors.groupingBy(ProcurementPayable::getSupplierId)).entrySet().stream().map(entry -> {
          Supplier partner = partners.get(entry.getKey()); BigDecimal balance = entry.getValue().stream()
              .map(item -> item.getAmount().subtract(item.getPaidAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
          return partnerStatement(type, entry.getKey(), partner == null ? "-" : partner.getCode(), partner == null ? "未知供应商" : partner.getName(), end, balance, refs.get(entry.getKey()));
        }).sorted(Comparator.comparing(PartnerStatementResponse::partnerName)).toList();
  }

  public PartnerStatementResponse confirmPartner(String typeValue, UUID partnerId, LocalDate periodEnd, ConfirmPartnerRequest request) {
    String type = upper(typeValue); String status = upper(request.status());
    if (!Set.of("CUSTOMER", "SUPPLIER").contains(type)) throw new BusinessException("往来类型无效");
    if (!PARTNER_STATUSES.contains(status)) throw new BusinessException("确认状态无效");
    PartnerStatementResponse statement = partnerStatements(type, periodEnd).stream().filter(item -> item.partnerId().equals(partnerId))
        .findFirst().orElseThrow(() -> new BusinessException("往来余额不存在"));
    BigDecimal confirmed = money(request.statementBalance()); BigDecimal difference = confirmed.subtract(statement.ledgerBalance());
    ReconciliationRef existing = reconciliationRefs(type, periodEnd).get(partnerId); OffsetDateTime now = OffsetDateTime.now();
    if (existing == null) {
      jdbc.update("insert into fin_partner_reconciliations(id,tenant_id,partner_type,partner_id,partner_name,period_end,ledger_balance,"
              + "statement_balance,difference_amount,status,confirmation_note,confirmed_at,confirmed_by,created_at,updated_at,created_by,version) "
              + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)", UUID.randomUUID(), tenant(), type, partnerId, statement.partnerName(), periodEnd,
          statement.ledgerBalance(), confirmed, difference, status, request.note().trim(), now, displayName(), now, now, username());
    } else {
      jdbc.update("update fin_partner_reconciliations set ledger_balance=?,statement_balance=?,difference_amount=?,status=?,confirmation_note=?,"
              + "confirmed_at=?,confirmed_by=?,updated_at=? where tenant_id=? and id=?", statement.ledgerBalance(), confirmed, difference, status,
          request.note().trim(), now, displayName(), now, tenant(), existing.id());
    }
    return partnerStatements(type, periodEnd).stream().filter(item -> item.partnerId().equals(partnerId)).findFirst().orElseThrow();
  }

  public CashScenarioResponse createCashScenario(SaveCashScenarioRequest request) {
    LocalDate horizon = request.asOfDate().plusDays(request.horizonDays());
    BigDecimal expectedReceipts = receivables.findAll().stream().filter(item -> !item.getDueDate().isBefore(request.asOfDate()) && !item.getDueDate().isAfter(horizon))
        .map(item -> item.getAmount().subtract(item.getSettledAmount())).filter(value -> value.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal expectedPayments = payables.findAll().stream().filter(item -> item.getStatus() != PayableStatus.CANCELLED)
        .filter(item -> !item.getDueDate().isBefore(request.asOfDate()) && !item.getDueDate().isAfter(horizon))
        .map(item -> item.getAmount().subtract(item.getPaidAmount())).filter(value -> value.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal forecast = request.openingCash().add(expectedReceipts).add(request.receiptAdjustment())
        .subtract(expectedPayments).subtract(request.paymentAdjustment());
    UUID id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now();
    jdbc.update("insert into fin_cash_forecast_scenarios(id,tenant_id,name,as_of_date,horizon_days,opening_cash,expected_receipts,expected_payments,"
            + "receipt_adjustment,payment_adjustment,forecast_cash,status,assumptions,created_at,updated_at,created_by,version) "
            + "values(?,?,?,?,?,?,?,?,?,?,?,'DRAFT',?,?,?,?,0)", id, tenant(), request.name().trim(), request.asOfDate(), request.horizonDays(),
        money(request.openingCash()), money(expectedReceipts), money(expectedPayments), money(request.receiptAdjustment()), money(request.paymentAdjustment()),
        money(forecast), trim(request.assumptions()), now, now, username());
    return cashScenarios().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
  }

  public List<CashScenarioResponse> cashScenarios() {
    return jdbc.query("select * from fin_cash_forecast_scenarios where tenant_id=? order by created_at desc", (rs, n) -> new CashScenarioResponse(
        rs.getObject("id", UUID.class), rs.getString("name"), rs.getObject("as_of_date", LocalDate.class), rs.getInt("horizon_days"),
        money(rs.getBigDecimal("opening_cash")), money(rs.getBigDecimal("expected_receipts")), money(rs.getBigDecimal("expected_payments")),
        money(rs.getBigDecimal("receipt_adjustment")), money(rs.getBigDecimal("payment_adjustment")), money(rs.getBigDecimal("forecast_cash")),
        rs.getString("status"), rs.getString("assumptions"), rs.getObject("created_at", OffsetDateTime.class)), tenant());
  }

  public List<TaxFilingResponse> taxFilings() {
    return jdbc.query("select * from fin_tax_filings where tenant_id=? order by fiscal_year desc,period_no desc", this::mapTaxFiling, tenant());
  }

  public TaxFilingResponse reconcileTax(int year, int month) {
    YearMonth period = YearMonth.of(year, month); LocalDate from = period.atDay(1), to = period.atEndOfMonth();
    List<TaxInvoiceLine> lines = analytics.taxLedger(from, to, null, "NORMAL");
    BigDecimal output = lines.stream().filter(item -> "OUTPUT".equals(item.side())).map(TaxInvoiceLine::taxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal input = lines.stream().filter(item -> "INPUT".equals(item.side())).map(TaxInvoiceLine::taxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal payable = output.subtract(input); Map<String, Object[]> ledger = entries.aggregateByAccountBetween(from, to).stream()
        .collect(Collectors.toMap(row -> String.valueOf(row[0]), Function.identity()));
    BigDecimal outputLedger = accountBalance(ledger.get("222101"), false); BigDecimal inputLedger = accountBalance(ledger.get("22210101"), true);
    BigDecimal ledgerTax = outputLedger.subtract(inputLedger); BigDecimal difference = payable.subtract(ledgerTax);
    var existing = jdbc.query("select id,status from fin_tax_filings where tenant_id=? and fiscal_year=? and period_no=?",
        (rs, n) -> Map.entry(rs.getObject(1, UUID.class), rs.getString(2)), tenant(), year, month);
    if (!existing.isEmpty() && "LOCKED".equals(existing.get(0).getValue())) throw new BusinessException("该申报期间已锁定");
    OffsetDateTime now = OffsetDateTime.now(); UUID id = existing.isEmpty() ? UUID.randomUUID() : existing.get(0).getKey();
    String status = difference.signum() == 0 ? "RECONCILED" : "DRAFT";
    if (existing.isEmpty()) jdbc.update("insert into fin_tax_filings(id,tenant_id,fiscal_year,period_no,output_tax,input_tax,tax_payable,ledger_tax,difference_amount,"
            + "status,created_at,updated_at,created_by,version) values(?,?,?,?,?,?,?,?,?,?, ?,?,?,0)", id, tenant(), year, month, money(output), money(input),
        money(payable), money(ledgerTax), money(difference), status, now, now, username());
    else jdbc.update("update fin_tax_filings set output_tax=?,input_tax=?,tax_payable=?,ledger_tax=?,difference_amount=?,status=?,updated_at=? where tenant_id=? and id=?",
        money(output), money(input), money(payable), money(ledgerTax), money(difference), status, now, tenant(), id);
    return taxFilings().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
  }

  public TaxFilingResponse lockTax(int year, int month, LockTaxFilingRequest request) {
    TaxFilingResponse filing = taxFilings().stream().filter(item -> item.fiscalYear() == year && item.periodNo() == month).findFirst()
        .orElseThrow(() -> new BusinessException("请先完成税务勾稽"));
    if (filing.difference().signum() != 0) throw new BusinessException("税务与总账存在差异，不能锁定申报");
    String payload = json(filing); ReportSnapshotResponse snapshot = captureSnapshot(new CaptureSnapshotRequest("TAX_FILING",
        year + "-" + String.format("%02d", month), year, month, payload, "税务申报锁定证据"));
    jdbc.update("update fin_tax_filings set status='LOCKED',filing_reference=?,locked_at=?,locked_by=?,snapshot_id=?,updated_at=? where tenant_id=? and id=?",
        request.filingReference().trim(), OffsetDateTime.now(), displayName(), snapshot.id(), OffsetDateTime.now(), tenant(), filing.id());
    return taxFilings().stream().filter(item -> item.id().equals(filing.id())).findFirst().orElseThrow();
  }

  public List<ConsolidationResponse> consolidations() {
    return jdbc.query("select * from fin_consolidation_runs where tenant_id=? order by fiscal_year desc,period_no desc,created_at desc", (rs, n) -> new ConsolidationResponse(
        rs.getObject("id", UUID.class), rs.getInt("fiscal_year"), rs.getInt("period_no"), rs.getString("name"), rs.getInt("entity_count"),
        money(rs.getBigDecimal("combined_revenue")), money(rs.getBigDecimal("combined_expense")), money(rs.getBigDecimal("intercompany_revenue")),
        money(rs.getBigDecimal("intercompany_expense")), money(rs.getBigDecimal("consolidated_profit")), rs.getString("status"),
        rs.getObject("snapshot_id", UUID.class), rs.getObject("completed_at", OffsetDateTime.class), rs.getString("completed_by")), tenant());
  }

  public ConsolidationResponse createConsolidation(SaveConsolidationRequest request) {
    BigDecimal revenue = request.entities().stream().map(ConsolidationEntityInput::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal expense = request.entities().stream().map(ConsolidationEntityInput::expense).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal profit = revenue.subtract(request.intercompanyRevenue()).subtract(expense.subtract(request.intercompanyExpense()));
    UUID id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now();
    jdbc.update("insert into fin_consolidation_runs(id,tenant_id,fiscal_year,period_no,name,entity_count,combined_revenue,combined_expense,"
            + "intercompany_revenue,intercompany_expense,consolidated_profit,entity_payload,status,created_at,updated_at,created_by,version) "
            + "values(?,?,?,?,?,?,?,?,?,?,?,?,'DRAFT',?,?,?,0)", id, tenant(), request.fiscalYear(), request.periodNo(), request.name().trim(),
        request.entities().size(), money(revenue), money(expense), money(request.intercompanyRevenue()), money(request.intercompanyExpense()), money(profit),
        json(request.entities()), now, now, username());
    return consolidations().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
  }

  public ConsolidationResponse completeConsolidation(UUID id) {
    ConsolidationResponse run = consolidations().stream().filter(item -> item.id().equals(id)).findFirst()
        .orElseThrow(() -> new BusinessException("合并批次不存在"));
    if ("COMPLETED".equals(run.status())) return run;
    ReportSnapshotResponse snapshot = captureSnapshot(new CaptureSnapshotRequest("CONSOLIDATION", id.toString(), run.fiscalYear(), run.periodNo(), json(run), "合并及内部交易抵销底稿"));
    jdbc.update("update fin_consolidation_runs set status='COMPLETED',snapshot_id=?,completed_at=?,completed_by=?,updated_at=? where tenant_id=? and id=?",
        snapshot.id(), OffsetDateTime.now(), displayName(), OffsetDateTime.now(), tenant(), id);
    return consolidations().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
  }

  public ReportSnapshotResponse captureSnapshot(CaptureSnapshotRequest request) {
    String hash = sha256(request.payload()); String tenant = tenant();
    var existing = jdbc.query("select * from fin_report_snapshots where tenant_id=? and report_type=? and scope_key=? and content_hash=?",
        this::mapSnapshot, tenant, upper(request.reportType()), request.scopeKey().trim(), hash);
    if (!existing.isEmpty()) return existing.get(0);
    UUID id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now();
    try {
      jdbc.update("insert into fin_report_snapshots(id,tenant_id,report_type,scope_key,fiscal_year,period_no,payload,content_hash,evidence_note,"
              + "captured_at,captured_by,created_at,updated_at,created_by,version) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)", id, tenant,
          upper(request.reportType()), request.scopeKey().trim(), request.fiscalYear(), request.periodNo(), request.payload(), hash,
          trim(request.evidenceNote()), now, displayName(), now, now, username());
    } catch (DuplicateKeyException ignored) { return jdbc.query("select * from fin_report_snapshots where tenant_id=? and report_type=? and scope_key=? and content_hash=?", this::mapSnapshot, tenant, upper(request.reportType()), request.scopeKey().trim(), hash).get(0); }
    return snapshots().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
  }

  public List<ReportSnapshotResponse> snapshots() {
    return jdbc.query("select * from fin_report_snapshots where tenant_id=? order by captured_at desc", this::mapSnapshot, tenant());
  }

  public List<VoucherRequestResponse> voucherRequests() {
    return jdbc.query("select * from fin_voucher_generation_requests where tenant_id=? order by created_at desc", (rs, n) -> new VoucherRequestResponse(
        rs.getObject("id", UUID.class), rs.getString("idempotency_key"), rs.getString("source_type"), rs.getString("business_no"),
        rs.getString("status"), rs.getInt("attempt_count"), rs.getObject("voucher_id", UUID.class), rs.getString("last_error"),
        rs.getObject("last_attempt_at", OffsetDateTime.class), rs.getObject("completed_at", OffsetDateTime.class)), tenant());
  }

  public List<String> periodCloseBlockers(int year, int month) {
    String tenant = tenant(); List<String> blockers = new ArrayList<>();
    Integer pending = jdbc.queryForObject("select count(*) from fin_period_end_jobs where tenant_id=? and fiscal_year=? and period_no=? and status<>'REVERSED' and status<>'COMPLETED'", Integer.class, tenant, year, month);
    if (pending != null && pending > 0) blockers.add("存在 " + pending + " 项期末处理未完成");
    Integer tax = jdbc.queryForObject("select count(*) from fin_tax_filings where tenant_id=? and fiscal_year=? and period_no=? and status='LOCKED'", Integer.class, tenant, year, month);
    if (tax == null || tax == 0) blockers.add("税务申报尚未勾稽并锁定");
    OpeningValidationResponse validation = validateOpening(year);
    if (!validation.valid()) blockers.add("期初/历史数据校验未通过");
    return blockers;
  }

  public ReportSnapshotResponse capturePeriodClose(int year, int month, Object payload) {
    return captureSnapshot(new CaptureSnapshotRequest("PERIOD_CLOSE", year + "-" + String.format("%02d", month), year, month,
        json(payload), "月结控制与关账结果"));
  }

  private PartnerStatementResponse partnerStatement(String type, UUID id, String code, String name, LocalDate end,
      BigDecimal balance, ReconciliationRef ref) {
    BigDecimal confirmed = ref == null ? BigDecimal.ZERO : ref.statementBalance();
    return new PartnerStatementResponse(type, id, code, name, end, money(balance), confirmed,
        ref == null ? money(balance.negate()) : ref.difference(), ref == null ? "PENDING" : ref.status(), ref == null ? null : ref.id(),
        ref == null ? null : ref.note(), ref == null ? null : ref.confirmedAt(), ref == null ? null : ref.confirmedBy());
  }

  private Map<UUID, ReconciliationRef> reconciliationRefs(String type, LocalDate end) {
    return jdbc.query("select * from fin_partner_reconciliations where tenant_id=? and partner_type=? and period_end=?", (rs, n) -> new ReconciliationRef(
        rs.getObject("id", UUID.class), rs.getObject("partner_id", UUID.class), money(rs.getBigDecimal("statement_balance")),
        money(rs.getBigDecimal("difference_amount")), rs.getString("status"), rs.getString("confirmation_note"),
        rs.getObject("confirmed_at", OffsetDateTime.class), rs.getString("confirmed_by")), tenant(), type, end).stream()
        .collect(Collectors.toMap(ReconciliationRef::partnerId, Function.identity()));
  }

  private PeriodJobResponse requirePeriodJob(UUID id) {
    List<PeriodJobResponse> rows = jdbc.query("select * from fin_period_end_jobs where tenant_id=? and id=?", this::mapPeriodJob, tenant(), id);
    if (rows.isEmpty()) throw new BusinessException("期末处理任务不存在"); return rows.get(0);
  }
  private PeriodJobResponse mapPeriodJob(java.sql.ResultSet rs, int row) throws java.sql.SQLException { return new PeriodJobResponse(
      rs.getObject("id", UUID.class), rs.getInt("fiscal_year"), rs.getInt("period_no"), rs.getString("process_type"), rs.getString("description"),
      money(rs.getBigDecimal("amount")), rs.getString("debit_account_code"), rs.getString("credit_account_code"), rs.getBoolean("auto_reverse"),
      rs.getObject("reversal_date", LocalDate.class), rs.getString("status"), rs.getObject("voucher_id", UUID.class), rs.getObject("reversal_voucher_id", UUID.class),
      rs.getString("idempotency_key"), rs.getObject("executed_at", OffsetDateTime.class), rs.getString("executed_by")); }
  private TaxFilingResponse mapTaxFiling(java.sql.ResultSet rs, int row) throws java.sql.SQLException { return new TaxFilingResponse(
      rs.getObject("id", UUID.class), rs.getInt("fiscal_year"), rs.getInt("period_no"), money(rs.getBigDecimal("output_tax")), money(rs.getBigDecimal("input_tax")),
      money(rs.getBigDecimal("tax_payable")), money(rs.getBigDecimal("ledger_tax")), money(rs.getBigDecimal("difference_amount")), rs.getString("status"),
      rs.getString("filing_reference"), rs.getObject("locked_at", OffsetDateTime.class), rs.getString("locked_by"), rs.getObject("snapshot_id", UUID.class)); }
  private ReportSnapshotResponse mapSnapshot(java.sql.ResultSet rs, int row) throws java.sql.SQLException { return new ReportSnapshotResponse(
      rs.getObject("id", UUID.class), rs.getString("report_type"), rs.getString("scope_key"), (Integer) rs.getObject("fiscal_year"),
      (Integer) rs.getObject("period_no"), rs.getString("content_hash"), rs.getString("evidence_note"),
      rs.getObject("captured_at", OffsetDateTime.class), rs.getString("captured_by")); }
  private long count(String table, String predicate) { return jdbc.queryForObject("select count(*) from " + table + " where tenant_id=? and " + predicate, Long.class, tenant()); }
  private BigDecimal accountBalance(Object[] row, boolean debitNormal) { if (row == null) return BigDecimal.ZERO; BigDecimal debit = money((BigDecimal) row[2]); BigDecimal credit = money((BigDecimal) row[3]); return debitNormal ? debit.subtract(credit) : credit.subtract(debit); }
  private <T> BigDecimal sum(List<T> rows, Function<T, BigDecimal> mapper) { return rows.stream().map(mapper).map(FinanceOperationsService::money).reduce(BigDecimal.ZERO, BigDecimal::add); }
  private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ex) { throw new BusinessException("审计数据序列化失败"); } }
  private String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception ex) { throw new BusinessException("审计摘要计算失败"); } }
  private static BigDecimal money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }
  private String upper(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
  private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  private UserPrincipal principal() { Object value = SecurityContextHolder.getContext().getAuthentication().getPrincipal(); if (value instanceof UserPrincipal p) return p; throw new BusinessException("当前用户身份无效"); }
  private String tenant() { return principal().tenantId(); } private String username() { return principal().getUsername(); } private String displayName() { return principal().displayName(); }
  private record ReconciliationRef(UUID id, UUID partnerId, BigDecimal statementBalance, BigDecimal difference,
      String status, String note, OffsetDateTime confirmedAt, String confirmedBy) {}
}
