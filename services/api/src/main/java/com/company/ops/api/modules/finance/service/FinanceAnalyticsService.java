package com.company.ops.api.modules.finance.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;
import static com.company.ops.api.modules.finance.dto.FinanceAnalyticsDtos.*;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableReceipt;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableReceiptRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.finance.domain.PaymentApplicationStatus;
import com.company.ops.api.modules.finance.domain.PaymentApplication;
import com.company.ops.api.modules.finance.domain.PaymentRecord;
import com.company.ops.api.modules.finance.repository.PaymentApplicationRepository;
import com.company.ops.api.modules.finance.repository.PaymentRecordRepository;
import com.company.ops.api.modules.governance.domain.BankStatementLine;
import com.company.ops.api.modules.governance.domain.BusinessControlRecord;
import com.company.ops.api.modules.governance.domain.ControlStatus;
import com.company.ops.api.modules.governance.domain.ControlType;
import com.company.ops.api.modules.governance.domain.ReconciliationStatus;
import com.company.ops.api.modules.governance.repository.BankStatementLineRepository;
import com.company.ops.api.modules.governance.repository.BusinessControlRecordRepository;
import com.company.ops.api.modules.ledger.domain.AccountingVoucher;
import com.company.ops.api.modules.ledger.domain.VoucherStatus;
import com.company.ops.api.modules.ledger.repository.AccountingVoucherRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierInvoice;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoiceRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.modules.finance.service.FinanceOrganizationScopeService.Scope;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

@Service
public class FinanceAnalyticsService {
  private static final Set<ControlStatus> ACTIVE_CONTROL_STATUSES =
      Set.of(ControlStatus.ACTIVE, ControlStatus.BLOCKED);
  private static final Set<String> TAX_STATUSES = Set.of("NORMAL", "VOIDED", "RED_FLUSHED");

  private final ReceivableRepository receivables;
  private final ReceivableReceiptRepository receipts;
  private final ServiceContractRepository contracts;
  private final CustomerRepository customers;
  private final ProcurementPayableRepository payables;
  private final PaymentRecordRepository payments;
  private final PaymentApplicationRepository applications;
  private final SupplierInvoiceRepository supplierInvoices;
  private final SupplierRepository suppliers;
  private final AccountingVoucherRepository vouchers;
  private final BankStatementLineRepository bankLines;
  private final BusinessControlRecordRepository controls;
  private final LedgerService ledgerService;
  private final TaxFilingGuard taxFilingGuard;
  private final FinanceOrganizationScopeService organizationScopeService;

  public FinanceAnalyticsService(
      ReceivableRepository receivables,
      ReceivableReceiptRepository receipts,
      ServiceContractRepository contracts,
      CustomerRepository customers,
      ProcurementPayableRepository payables,
      PaymentRecordRepository payments,
      PaymentApplicationRepository applications,
      SupplierInvoiceRepository supplierInvoices,
      SupplierRepository suppliers,
      AccountingVoucherRepository vouchers,
      BankStatementLineRepository bankLines,
      BusinessControlRecordRepository controls,
      LedgerService ledgerService,
      TaxFilingGuard taxFilingGuard,
      FinanceOrganizationScopeService organizationScopeService) {
    this.receivables = receivables;
    this.receipts = receipts;
    this.contracts = contracts;
    this.customers = customers;
    this.payables = payables;
    this.payments = payments;
    this.applications = applications;
    this.supplierInvoices = supplierInvoices;
    this.suppliers = suppliers;
    this.vouchers = vouchers;
    this.bankLines = bankLines;
    this.controls = controls;
    this.ledgerService = ledgerService;
    this.taxFilingGuard = taxFilingGuard;
    this.organizationScopeService = organizationScopeService;
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "financeAnalytics", key = "{#requestedAsOf, #requestedYear, #organizationId, #includeDescendants}")
  public FinanceAnalyticsResponse analytics(LocalDate requestedAsOf, Integer requestedYear,
      UUID organizationId, boolean includeDescendants) {
    LocalDate asOf = requestedAsOf == null ? LocalDate.now() : requestedAsOf;
    int year = requestedYear == null ? asOf.getYear() : requestedYear;
    if (year < 2000 || year > 2200) throw new BusinessException("财务年度必须在 2000 到 2200 之间");

    Scope scope = organizationScopeService.resolve(organizationId, includeDescendants);
    List<Receivable> allReceivables = scope.unrestricted()
        ? receivables.findAll()
        : receivables.findByOrganizationIdIn(scope.organizationIds());
    Set<UUID> receivableIds = allReceivables.stream().map(Receivable::getId)
        .filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    List<ProcurementPayable> allPayables = scope.unrestricted()
        ? payables.findAll()
        : payables.findByOrganizationIdIn(scope.organizationIds());
    Set<UUID> payableIds = allPayables.stream().map(ProcurementPayable::getId)
        .filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    Set<UUID> orderIds = allPayables.stream().map(ProcurementPayable::getOrderId)
        .filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    List<ReceivableReceipt> allReceipts = receivableIds.isEmpty()
        ? List.of()
        : receipts.findByReceivableIdIn(receivableIds);
    List<PaymentRecord> allPayments = payableIds.isEmpty()
        ? List.of()
        : payments.findByPayableIdIn(payableIds);
    List<PaymentApplication> allApplications = payableIds.isEmpty()
        ? List.of()
        : applications.findByPayableIdIn(payableIds);
    List<SupplierInvoice> allSupplierInvoices = scopedInvoices(scope, payableIds, orderIds);

    Set<String> receivableCodes = allReceivables.stream().map(Receivable::getCode).collect(Collectors.toSet());
    Set<String> receiptNumbers = allReceipts.stream().map(ReceivableReceipt::getReferenceNo).collect(Collectors.toSet());
    Set<String> paymentCodes = allPayments.stream().map(PaymentRecord::getCode).collect(Collectors.toSet());
    Set<String> invoiceCodes = allSupplierInvoices.stream().map(SupplierInvoice::getCode).collect(Collectors.toSet());
    List<AccountingVoucher> allVouchers = vouchers.findAll().stream()
        .filter(item -> scope.unrestricted()
            || ("INVOICE".equals(item.getBizType()) && receivableCodes.contains(item.getBizNo()))
            || ("RECEIPT".equals(item.getBizType()) && receiptNumbers.contains(item.getBizNo()))
            || ("PAYMENT".equals(item.getBizType()) && paymentCodes.contains(item.getBizNo()))
            || ("SUPPLIER_INVOICE".equals(item.getBizType()) && invoiceCodes.contains(item.getBizNo())))
        .toList();
    Set<UUID> receiptIds = allReceipts.stream().map(ReceivableReceipt::getId)
        .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    Set<UUID> paymentIds = allPayments.stream().map(PaymentRecord::getId)
        .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    List<BankStatementLine> allBankLines = bankLines.findAll().stream()
        .filter(item -> scope.unrestricted()
            || ("RECEIPT".equals(item.getMatchedBizType()) && receiptIds.contains(item.getMatchedBizId()))
            || ("PAYMENT".equals(item.getMatchedBizType()) && paymentIds.contains(item.getMatchedBizId())))
        .toList();
    Set<String> ownerNames = organizationScopeService.ownerNames(scope);
    List<BusinessControlRecord> allControls = controls.findAll().stream()
        .filter(item -> scope.unrestricted() || ownerNames.contains(item.getOwner()))
        .toList();

    List<MonthlyCashFlow> monthly = monthlyCashFlow(year, asOf, allReceipts, allPayments);
    List<ForecastBucket> forecast = List.of(
        forecast("D7", "截至未来7天", 7, asOf, allReceivables, allPayables),
        forecast("D30", "截至未来30天", 30, asOf, allReceivables, allPayables),
        forecast("D60", "截至未来60天", 60, asOf, allReceivables, allPayables));
    List<AgingBucket> aging = List.of(
        aging("CURRENT", "未到期", Long.MIN_VALUE, 0, asOf, allReceivables, allPayables),
        aging("D1_30", "逾期1-30天", 1, 30, asOf, allReceivables, allPayables),
        aging("D31_60", "逾期31-60天", 31, 60, asOf, allReceivables, allPayables),
        aging("D61_PLUS", "逾期61天以上", 61, Long.MAX_VALUE, asOf, allReceivables, allPayables));
    ReconciliationSummary reconciliation = reconciliation(
        asOf, allReceipts, allPayments, allVouchers, allBankLines);
    TaxSummary tax = taxSummary(asOf, allReceivables, allSupplierInvoices);
    CashPlanSummary cashPlan = cashPlan(allControls);
    return new FinanceAnalyticsResponse(asOf, year, scope.info(), monthly, forecast, aging,
        reconciliation, tax, cashPlan, risks(asOf, allReceivables, allPayables,
            allBankLines, allSupplierInvoices, allApplications, cashPlan));
  }

  private List<SupplierInvoice> scopedInvoices(Scope scope, Set<UUID> payableIds, Set<UUID> orderIds) {
    if (scope.unrestricted()) return supplierInvoices.findAll();
    if (payableIds.isEmpty() && orderIds.isEmpty()) return List.of();
    if (payableIds.isEmpty()) return supplierInvoices.findByOrderIdIn(orderIds);
    if (orderIds.isEmpty()) return supplierInvoices.findByPayableIdIn(payableIds);
    Map<UUID, SupplierInvoice> merged = new java.util.LinkedHashMap<>();
    supplierInvoices.findByPayableIdIn(payableIds).forEach(item -> merged.putIfAbsent(item.getId(), item));
    supplierInvoices.findByOrderIdIn(orderIds).forEach(item -> merged.putIfAbsent(item.getId(), item));
    return new ArrayList<>(merged.values());
  }

  @Transactional(readOnly = true)
  public List<TaxInvoiceLine> taxLedger(LocalDate from, LocalDate to, String requestedSide, String requestedStatus) {
    LocalDate start = from == null ? LocalDate.of(LocalDate.now().getYear(), 1, 1) : from;
    LocalDate end = to == null ? LocalDate.now() : to;
    if (end.isBefore(start)) throw new BusinessException("税务台账结束日期不能早于开始日期");
    String side = normalizeOptional(requestedSide);
    String status = normalizeOptional(requestedStatus);
    if (side != null && !Set.of("OUTPUT", "INPUT").contains(side)) throw new BusinessException("发票方向只能是 OUTPUT 或 INPUT");
    if (status != null && !TAX_STATUSES.contains(status)) throw new BusinessException("不支持的发票税务状态");

    List<TaxInvoiceLine> result = new ArrayList<>();
    if (side == null || "OUTPUT".equals(side)) result.addAll(outputTaxLines(start, end));
    if (side == null || "INPUT".equals(side)) result.addAll(inputTaxLines(start, end));
    return result.stream()
        .filter(item -> status == null || status.equals(item.status()))
        .sorted(Comparator.comparing(TaxInvoiceLine::invoiceDate).reversed()
            .thenComparing(TaxInvoiceLine::businessNo))
        .toList();
  }

  @Transactional
  public TaxInvoiceLine adjustTaxInvoice(String requestedSide, UUID id, AdjustTaxInvoiceRequest request) {
    String side = requestedSide.trim().toUpperCase(Locale.ROOT);
    String target = request.status().trim().toUpperCase(Locale.ROOT);
    if (!Set.of("VOIDED", "RED_FLUSHED").contains(target)) {
      throw new BusinessException("发票调整状态只能选择作废或红冲");
    }
    if (request.adjustmentDate().isAfter(LocalDate.now())) throw new BusinessException("调整日期不能晚于今天");
    taxFilingGuard.assertUnlocked(request.adjustmentDate());
    String actor = currentName();
    OffsetDateTime adjustedAt = OffsetDateTime.now();
    if ("OUTPUT".equals(side)) {
      Receivable item = receivables.findById(id).orElseThrow(() -> new BusinessException("销项发票不存在"));
      requireAdjustable(item.getInvoiceNo(), item.getTaxStatus());
      ledgerService.reverseBusinessVoucher("INVOICE", item.getCode(), request.adjustmentDate(), request.reason());
      item.setTaxStatus(target); item.setTaxAdjustmentReason(request.reason().trim());
      item.setTaxAdjustedAt(adjustedAt); item.setTaxAdjustedBy(actor); receivables.save(item);
      return toOutputTaxLine(item, customerMap(List.of(item.getCustomerId())), contractMap(nullableList(item.getContractId())));
    }
    if ("INPUT".equals(side)) {
      SupplierInvoice item = supplierInvoices.findById(id).orElseThrow(() -> new BusinessException("进项发票不存在"));
      requireAdjustable(item.getInvoiceNo(), item.getTaxStatus());
      if (!"APPROVED".equals(item.getApprovalStatus())) throw new BusinessException("只有审核通过的进项发票可以作废或红冲");
      ledgerService.reverseBusinessVoucher("SUPPLIER_INVOICE", item.getCode(), request.adjustmentDate(), request.reason());
      item.setTaxStatus(target); item.setTaxAdjustmentReason(request.reason().trim());
      item.setTaxAdjustedAt(adjustedAt); item.setTaxAdjustedBy(actor); supplierInvoices.save(item);
      return toInputTaxLine(item, supplierMap(List.of(item.getSupplierId())));
    }
    throw new BusinessException("发票方向只能是 OUTPUT 或 INPUT");
  }

  private List<MonthlyCashFlow> monthlyCashFlow(int year, LocalDate asOf,
      List<ReceivableReceipt> allReceipts, List<PaymentRecord> allPayments) {
    return java.util.stream.IntStream.rangeClosed(1, 12).mapToObj(month -> {
      BigDecimal receipt = allReceipts.stream()
          .filter(item -> item.getReceivedDate().getYear() == year && item.getReceivedDate().getMonthValue() == month)
          .filter(item -> !item.getReceivedDate().isAfter(asOf))
          .map(ReceivableReceipt::getAmount).map(FinanceAnalyticsService::money).reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal payment = allPayments.stream()
          .filter(item -> item.getPaidDate().getYear() == year && item.getPaidDate().getMonthValue() == month)
          .filter(item -> !item.getPaidDate().isAfter(asOf))
          .map(PaymentRecord::getAmount).map(FinanceAnalyticsService::money).reduce(BigDecimal.ZERO, BigDecimal::add);
      return new MonthlyCashFlow(month, receipt, payment, receipt.subtract(payment));
    }).toList();
  }

  private ForecastBucket forecast(String key, String label, int days, LocalDate asOf,
      List<Receivable> allReceivables, List<ProcurementPayable> allPayables) {
    LocalDate horizon = asOf.plusDays(days);
    BigDecimal receivable = allReceivables.stream().filter(this::open)
        .filter(item -> !item.getDueDate().isAfter(horizon))
        .map(this::outstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal payable = allPayables.stream().filter(this::open)
        .filter(item -> !item.getDueDate().isAfter(horizon))
        .map(this::outstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new ForecastBucket(key, label, days, receivable, payable, receivable.subtract(payable));
  }

  private AgingBucket aging(String key, String label, long min, long max, LocalDate asOf,
      List<Receivable> allReceivables, List<ProcurementPayable> allPayables) {
    List<Receivable> receivableItems = allReceivables.stream().filter(this::open)
        .filter(item -> between(overdueDays(item.getDueDate(), asOf), min, max)).toList();
    List<ProcurementPayable> payableItems = allPayables.stream().filter(this::open)
        .filter(item -> between(overdueDays(item.getDueDate(), asOf), min, max)).toList();
    return new AgingBucket(key, label,
        receivableItems.stream().map(this::outstanding).reduce(BigDecimal.ZERO, BigDecimal::add),
        payableItems.stream().map(this::outstanding).reduce(BigDecimal.ZERO, BigDecimal::add),
        receivableItems.size(), payableItems.size());
  }

  private ReconciliationSummary reconciliation(LocalDate asOf, List<ReceivableReceipt> allReceipts,
      List<PaymentRecord> allPayments, List<AccountingVoucher> allVouchers,
      List<BankStatementLine> allBankLines) {
    BigDecimal businessReceipt = allReceipts.stream().filter(item -> !item.getReceivedDate().isAfter(asOf))
        .map(ReceivableReceipt::getAmount).map(FinanceAnalyticsService::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal businessPayment = allPayments.stream().filter(item -> !item.getPaidDate().isAfter(asOf))
        .map(PaymentRecord::getAmount).map(FinanceAnalyticsService::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal ledgerReceipt = voucherAmount(allVouchers, "RECEIPT", asOf);
    BigDecimal ledgerPayment = voucherAmount(allVouchers, "PAYMENT", asOf);
    List<ReconciliationItem> ledger = List.of(
        reconItem("RECEIPT", businessReceipt, ledgerReceipt),
        reconItem("PAYMENT", businessPayment, ledgerPayment),
        reconItem("NET_CASH", businessReceipt.subtract(businessPayment), ledgerReceipt.subtract(ledgerPayment)));
    List<BankStatementLine> imported = allBankLines.stream()
        .filter(item -> !item.getTransactionDate().isAfter(asOf)).toList();
    List<BankStatementLine> unmatched = imported.stream()
        .filter(item -> item.getReconciliationStatus() != ReconciliationStatus.MATCHED).toList();
    return new ReconciliationSummary(ledger, imported.size(),
        imported.stream().filter(item -> item.getReconciliationStatus() == ReconciliationStatus.MATCHED).count(),
        imported.stream().filter(item -> item.getReconciliationStatus() == ReconciliationStatus.SUGGESTED).count(),
        imported.stream().filter(item -> item.getReconciliationStatus() == ReconciliationStatus.UNMATCHED).count(),
        unmatched.stream().map(BankStatementLine::getAmount).map(FinanceAnalyticsService::money)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  private TaxSummary taxSummary(LocalDate asOf, List<Receivable> allReceivables,
      List<SupplierInvoice> allSupplierInvoices) {
    Map<UUID, ServiceContract> contractMap = contractMap(allReceivables.stream()
        .map(Receivable::getContractId).filter(java.util.Objects::nonNull).toList());
    List<TaxAmount> output = allReceivables.stream()
        .filter(item -> item.getInvoiceNo() != null && item.getInvoiceDate() != null && !item.getInvoiceDate().isAfter(asOf))
        .filter(item -> "NORMAL".equals(item.getTaxStatus()))
        .map(item -> tax(item.getAmount(), contractMap.get(item.getContractId()) == null
            ? BigDecimal.valueOf(13) : contractMap.get(item.getContractId()).getTaxRate())).toList();
    List<TaxAmount> input = allSupplierInvoices.stream()
        .filter(item -> !item.getInvoiceDate().isAfter(asOf))
        .filter(item -> "APPROVED".equals(item.getApprovalStatus()) && "VERIFIED".equals(item.getVerificationStatus()))
        .filter(item -> "NORMAL".equals(item.getTaxStatus()))
        .map(item -> tax(item.getAmount(), item.getTaxRate())).toList();
    BigDecimal outputGross = sumTax(output, TaxAmount::gross), outputNet = sumTax(output, TaxAmount::net);
    BigDecimal outputTax = sumTax(output, TaxAmount::tax), inputGross = sumTax(input, TaxAmount::gross);
    BigDecimal inputNet = sumTax(input, TaxAmount::net), inputTax = sumTax(input, TaxAmount::tax);
    long pendingOutput = allReceivables.stream().filter(this::open)
        .filter(item -> item.getInvoiceNo() == null || item.getInvoiceNo().isBlank()).count();
    long inputExceptions = allSupplierInvoices.stream().filter(item -> !item.getInvoiceDate().isAfter(asOf))
        .filter(item -> !"APPROVED".equals(item.getApprovalStatus()) || !"VERIFIED".equals(item.getVerificationStatus())).count();
    long adjusted = allReceivables.stream().filter(item -> !"NORMAL".equals(item.getTaxStatus())).count()
        + allSupplierInvoices.stream().filter(item -> !"NORMAL".equals(item.getTaxStatus())).count();
    return new TaxSummary(outputGross, outputNet, outputTax, inputGross, inputNet, inputTax,
        outputTax.subtract(inputTax), pendingOutput, inputExceptions, adjusted);
  }

  private CashPlanSummary cashPlan(List<BusinessControlRecord> allControls) {
    List<BusinessControlRecord> plans = allControls.stream()
        .filter(item -> item.getControlType() == ControlType.CASH_FORECAST)
        .filter(item -> ACTIVE_CONTROL_STATUSES.contains(item.getStatus())).toList();
    BigDecimal baseline = sumControls(plans, BusinessControlRecord::getBudgetAmount);
    BigDecimal committed = sumControls(plans, BusinessControlRecord::getCommittedAmount);
    BigDecimal actual = sumControls(plans, BusinessControlRecord::getActualAmount);
    BigDecimal forecast = sumControls(plans, BusinessControlRecord::getForecastAmount);
    return new CashPlanSummary(baseline, committed, actual, forecast,
        forecast.subtract(baseline), plans.size());
  }

  private List<FinanceRisk> risks(LocalDate asOf, List<Receivable> allReceivables,
      List<ProcurementPayable> allPayables, List<BankStatementLine> allBankLines,
      List<SupplierInvoice> allSupplierInvoices, List<PaymentApplication> allApplications,
      CashPlanSummary plan) {
    List<FinanceRisk> result = new ArrayList<>();
    List<Receivable> overdueReceivables = allReceivables.stream().filter(this::open)
        .filter(item -> item.getDueDate().isBefore(asOf)).toList();
    addRisk(result, "OVERDUE_RECEIVABLE", "HIGH", "RECEIVABLE", "逾期应收待催收",
        "按账龄和金额安排催收责任", overdueReceivables.stream().map(this::outstanding)
            .reduce(BigDecimal.ZERO, BigDecimal::add), overdueReceivables.size());
    List<ProcurementPayable> overduePayables = allPayables.stream().filter(this::open)
        .filter(item -> item.getDueDate().isBefore(asOf)).toList();
    addRisk(result, "OVERDUE_PAYABLE", "MEDIUM", "PAYABLE", "逾期应付待排程",
        "复核资金计划和供应商优先级", overduePayables.stream().map(this::outstanding)
            .reduce(BigDecimal.ZERO, BigDecimal::add), overduePayables.size());
    List<BankStatementLine> unmatched = allBankLines.stream().filter(item -> !item.getTransactionDate().isAfter(asOf))
        .filter(item -> item.getReconciliationStatus() != ReconciliationStatus.MATCHED).toList();
    addRisk(result, "UNMATCHED_BANK", "HIGH", "RECONCILIATION", "银行流水尚未对账",
        "处理未匹配和候选匹配流水", unmatched.stream().map(BankStatementLine::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add), unmatched.size());
    List<SupplierInvoice> taxExceptions = allSupplierInvoices.stream().filter(item -> !item.getInvoiceDate().isAfter(asOf))
        .filter(item -> !"APPROVED".equals(item.getApprovalStatus()) || !"VERIFIED".equals(item.getVerificationStatus())).toList();
    addRisk(result, "TAX_EXCEPTION", "MEDIUM", "TAX", "进项发票存在异常",
        "完成三单匹配、审核和验真", taxExceptions.stream().map(SupplierInvoice::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add), taxExceptions.size());
    long pending = allApplications.stream()
        .filter(item -> item.getStatus() == PaymentApplicationStatus.PENDING_APPROVAL).count();
    addRisk(result, "PENDING_PAYMENT", "MEDIUM", "PAYMENT", "付款申请待审批",
        "避免到期付款因审批积压延误", BigDecimal.ZERO, pending);
    if (plan.variance().signum() > 0) addRisk(result, "CASH_PLAN_GAP", "HIGH", "CASH_PLAN",
        "资金预测超过基准", "复核滚动资金计划并落实资金来源", plan.variance(), plan.activePlans());
    return result;
  }

  private List<TaxInvoiceLine> outputTaxLines(LocalDate start, LocalDate end) {
    List<Receivable> source = receivables.findAll().stream()
        .filter(item -> item.getInvoiceNo() != null && item.getInvoiceDate() != null)
        .filter(item -> !item.getInvoiceDate().isBefore(start) && !item.getInvoiceDate().isAfter(end)).toList();
    Map<UUID, Customer> customerMap = customerMap(source.stream().map(Receivable::getCustomerId).toList());
    Map<UUID, ServiceContract> contractMap = contractMap(source.stream().map(Receivable::getContractId)
        .filter(java.util.Objects::nonNull).toList());
    return source.stream().map(item -> toOutputTaxLine(item, customerMap, contractMap)).toList();
  }

  private List<TaxInvoiceLine> inputTaxLines(LocalDate start, LocalDate end) {
    List<SupplierInvoice> source = supplierInvoices.findAll().stream()
        .filter(item -> !item.getInvoiceDate().isBefore(start) && !item.getInvoiceDate().isAfter(end)).toList();
    Map<UUID, Supplier> supplierMap = supplierMap(source.stream().map(SupplierInvoice::getSupplierId).toList());
    return source.stream().map(item -> toInputTaxLine(item, supplierMap)).toList();
  }

  private TaxInvoiceLine toOutputTaxLine(Receivable item, Map<UUID, Customer> customerMap,
      Map<UUID, ServiceContract> contractMap) {
    ServiceContract contract = contractMap.get(item.getContractId());
    BigDecimal rate = contract == null ? BigDecimal.valueOf(13) : contract.getTaxRate();
    TaxAmount tax = tax(item.getAmount(), rate);
    Customer customer = customerMap.get(item.getCustomerId());
    return new TaxInvoiceLine(item.getId(), "OUTPUT", item.getCode(), item.getInvoiceNo(),
        customer == null ? "-" : customer.getName(), item.getInvoiceDate(), tax.gross(), tax.net(), tax.tax(),
        normalizeRate(rate), item.getTaxStatus(), "INVOICED", item.getTaxAdjustmentReason(),
        item.getTaxAdjustedAt(), item.getTaxAdjustedBy());
  }

  private TaxInvoiceLine toInputTaxLine(SupplierInvoice item, Map<UUID, Supplier> supplierMap) {
    TaxAmount tax = tax(item.getAmount(), item.getTaxRate());
    Supplier supplier = supplierMap.get(item.getSupplierId());
    return new TaxInvoiceLine(item.getId(), "INPUT", item.getCode(), item.getInvoiceNo(),
        supplier == null ? "-" : supplier.getName(), item.getInvoiceDate(), tax.gross(), tax.net(), tax.tax(),
        normalizeRate(item.getTaxRate()), item.getTaxStatus(), item.getVerificationStatus(),
        item.getTaxAdjustmentReason(), item.getTaxAdjustedAt(), item.getTaxAdjustedBy());
  }

  private void requireAdjustable(String invoiceNo, String status) {
    if (invoiceNo == null || invoiceNo.isBlank()) throw new BusinessException("发票尚未登记");
    if (!"NORMAL".equals(status)) throw new BusinessException("该发票已经作废或红冲");
  }

  private BigDecimal voucherAmount(List<AccountingVoucher> source, String bizType, LocalDate asOf) {
    return source.stream().filter(item -> item.getStatus() == VoucherStatus.POSTED)
        .filter(item -> bizType.equals(item.getBizType()) && !item.getVoucherDate().isAfter(asOf))
        .map(AccountingVoucher::getTotalDebit).map(FinanceAnalyticsService::money)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private ReconciliationItem reconItem(String key, BigDecimal business, BigDecimal ledger) {
    return new ReconciliationItem(key, business, ledger, business.subtract(ledger));
  }

  private boolean open(Receivable item) {
    return item.getStatus() != ReceivableStatus.SETTLED && outstanding(item).signum() > 0;
  }

  private boolean open(ProcurementPayable item) {
    return item.getStatus() != PayableStatus.PAID && item.getStatus() != PayableStatus.CANCELLED
        && outstanding(item).signum() > 0;
  }

  private BigDecimal outstanding(Receivable item) { return money(item.getAmount()).subtract(money(item.getSettledAmount())); }
  private BigDecimal outstanding(ProcurementPayable item) { return money(item.getAmount()).subtract(money(item.getPaidAmount())); }
  private long overdueDays(LocalDate dueDate, LocalDate asOf) { return ChronoUnit.DAYS.between(dueDate, asOf); }
  private boolean between(long value, long min, long max) { return value >= min && value <= max; }

  private TaxAmount tax(BigDecimal grossValue, BigDecimal rateValue) {
    BigDecimal gross = money(grossValue), rate = normalizeRate(rateValue);
    BigDecimal net = gross.divide(BigDecimal.ONE.add(rate.movePointLeft(2)), 2, RoundingMode.HALF_UP);
    return new TaxAmount(gross, net, gross.subtract(net));
  }

  private static BigDecimal normalizeRate(BigDecimal value) { return value == null ? BigDecimal.valueOf(13) : value; }
  private static BigDecimal money(BigDecimal value) { return amount(value); }
  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  private static <T> BigDecimal sumTax(List<T> items, Function<T, BigDecimal> mapper) {
    return items.stream().map(mapper).map(FinanceAnalyticsService::money).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal sumControls(List<BusinessControlRecord> items,
      Function<BusinessControlRecord, BigDecimal> mapper) {
    return items.stream().map(mapper).map(FinanceAnalyticsService::money).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void addRisk(List<FinanceRisk> result, String key, String severity, String category,
      String title, String description, BigDecimal exposure, long count) {
    if (count > 0 || money(exposure).signum() != 0) result.add(new FinanceRisk(
        key, severity, category, title, description, money(exposure), count));
  }

  private Map<UUID, Customer> customerMap(Collection<UUID> ids) {
    return customers.findAllById(ids).stream().collect(Collectors.toMap(Customer::getId, Function.identity()));
  }
  private Map<UUID, Supplier> supplierMap(Collection<UUID> ids) {
    return suppliers.findAllById(ids).stream().collect(Collectors.toMap(Supplier::getId, Function.identity()));
  }
  private Map<UUID, ServiceContract> contractMap(Collection<UUID> ids) {
    return contracts.findAllById(ids).stream().collect(Collectors.toMap(ServiceContract::getId, Function.identity()));
  }
  private List<UUID> nullableList(UUID id) { return id == null ? List.of() : List.of(id); }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
      return principal.displayName();
    }
    throw new BusinessException("请先登录");
  }

  private record TaxAmount(BigDecimal gross, BigDecimal net, BigDecimal tax) {}
}
