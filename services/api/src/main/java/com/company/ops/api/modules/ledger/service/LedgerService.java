package com.company.ops.api.modules.ledger.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;
import static com.company.ops.api.modules.ledger.dto.LedgerDtos.*;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.governance.service.AccountingPeriodGuard;
import com.company.ops.api.modules.ledger.domain.AccountingEntry;
import com.company.ops.api.modules.ledger.domain.AccountingAccount;
import com.company.ops.api.modules.ledger.domain.AccountOpeningBalance;
import com.company.ops.api.modules.ledger.domain.AccountingVoucher;
import com.company.ops.api.modules.ledger.domain.VoucherStatus;
import com.company.ops.api.modules.ledger.repository.AccountingEntryRepository;
import com.company.ops.api.modules.ledger.repository.AccountingAccountRepository;
import com.company.ops.api.modules.ledger.repository.AccountOpeningBalanceRepository;
import com.company.ops.api.modules.ledger.repository.AccountingVoucherRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

@Service
public class LedgerService {
  private final AccountingVoucherRepository voucherRepository;
  private final AccountingEntryRepository entryRepository;
  private final AccountingPeriodGuard periodGuard;
  private final AccountingAccountRepository accountRepository;
  private final AccountOpeningBalanceRepository openingBalanceRepository;

  public LedgerService(AccountingVoucherRepository voucherRepository,
      AccountingEntryRepository entryRepository, AccountingPeriodGuard periodGuard,
      AccountingAccountRepository accountRepository,
      AccountOpeningBalanceRepository openingBalanceRepository) {
    this.voucherRepository = voucherRepository;
    this.entryRepository = entryRepository;
    this.periodGuard = periodGuard;
    this.accountRepository = accountRepository;
    this.openingBalanceRepository = openingBalanceRepository;
  }

  @Transactional
  public AccountingVoucher post(String bizType, String bizNo, LocalDate date, String description, List<PostingLine> lines) {
    var existing = voucherRepository.findByBizTypeAndBizNo(bizType, bizNo);
    if (existing.isPresent()) {
      if (existing.get().getStatus() == VoucherStatus.DRAFT || existing.get().getStatus() == VoucherStatus.REVIEWED) {
        throw new BusinessException("该业务来源存在未记账的手工凭证，不能生成自动凭证");
      }
      return existing.get();
    }
    periodGuard.assertOpen(date);
    Totals totals = validateLines(lines);
    AccountingVoucher voucher = baseVoucher(bizType, bizNo, date, description, totals);
    Actor actor = currentActor(false);
    voucher.setStatus(VoucherStatus.POSTED); voucher.setPostedAt(OffsetDateTime.now()); voucher.setPostedBy(actor.displayName());
    voucher.setCreatedBy(actor.username());
    AccountingVoucher saved = voucherRepository.save(voucher);
    saveEntries(saved.getId(), lines);
    return saved;
  }

  @Transactional
  public VoucherResponse createDraft(CreateVoucherRequest request) {
    periodGuard.assertOpen(request.voucherDate());
    if (!"MANUAL".equals(request.bizType().trim().toUpperCase())) {
      throw new BusinessException("手工制单的业务类型必须是 MANUAL");
    }
    if (voucherRepository.findByBizTypeAndBizNo("MANUAL", request.bizNo().trim()).isPresent()) {
      throw new BusinessException("该业务来源已经存在会计凭证");
    }
    List<PostingLine> lines = request.lines().stream().map(line -> {
      AccountingAccount account = requireActiveAccount(line.accountCode());
      return new PostingLine(account.getCode(), account.getName(), line.debit(), line.credit(), trim(line.summary()));
    }).toList();
    Totals totals = validateLines(lines);
    AccountingVoucher voucher = baseVoucher("MANUAL", request.bizNo().trim(), request.voucherDate(), request.description().trim(), totals);
    Actor actor = currentActor(true);
    voucher.setStatus(VoucherStatus.DRAFT); voucher.setCreatedBy(actor.username());
    AccountingVoucher saved = voucherRepository.save(voucher);
    List<AccountingEntry> entries = saveEntries(saved.getId(), lines);
    return toResponse(saved, entries);
  }

  @Transactional
  public VoucherResponse review(UUID id) {
    AccountingVoucher voucher = requireVoucher(id);
    if (voucher.getStatus() != VoucherStatus.DRAFT) throw new BusinessException("只有草稿凭证可以复核");
    Actor actor = currentActor(true);
    if (actor.username().equals(voucher.getCreatedBy())) throw new BusinessException("制单人与复核人必须分离");
    validateLines(entryRepository.findByVoucherIdOrderByCreatedAtAsc(id).stream().map(this::toPostingLine).toList());
    voucher.setStatus(VoucherStatus.REVIEWED); voucher.setReviewedAt(OffsetDateTime.now()); voucher.setReviewedBy(actor.username());
    return response(voucherRepository.save(voucher));
  }

  @Transactional
  public VoucherResponse postReviewed(UUID id) {
    AccountingVoucher voucher = requireVoucher(id);
    if (voucher.getStatus() != VoucherStatus.REVIEWED) throw new BusinessException("只有已复核凭证可以记账");
    Actor actor = currentActor(true);
    if (actor.username().equals(voucher.getReviewedBy())) throw new BusinessException("复核人与记账人必须分离");
    periodGuard.assertOpen(voucher.getVoucherDate());
    voucher.setStatus(VoucherStatus.POSTED); voucher.setPostedAt(OffsetDateTime.now()); voucher.setPostedBy(actor.username());
    return response(voucherRepository.save(voucher));
  }

  @Transactional
  public VoucherResponse reverse(UUID id, ReverseVoucherRequest request) {
    AccountingVoucher original = requireVoucher(id);
    return reverseVoucher(original, request.reversalDate(), request.reason());
  }

  @Transactional
  public VoucherResponse reverseBusinessVoucher(String bizType, String bizNo, LocalDate reversalDate, String reason) {
    AccountingVoucher original = voucherRepository.findByBizTypeAndBizNo(bizType, bizNo)
        .orElseThrow(() -> new BusinessException("业务来源尚未生成会计凭证"));
    return reverseVoucher(original, reversalDate, reason);
  }

  private VoucherResponse reverseVoucher(AccountingVoucher original, LocalDate reversalDate, String reason) {
    if (original.getStatus() != VoucherStatus.POSTED) throw new BusinessException("只有已记账凭证可以冲销");
    periodGuard.assertOpen(reversalDate);
    Actor actor = currentActor(true);
    List<AccountingEntry> originalEntries = entryRepository.findByVoucherIdOrderByCreatedAtAsc(original.getId());
    List<PostingLine> reversingLines = originalEntries.stream().map(entry -> new PostingLine(
        entry.getAccountCode(), entry.getAccountName(), entry.getCredit(), entry.getDebit(), "冲销：" + entry.getSummary())).toList();
    Totals totals = validateLines(reversingLines);
    AccountingVoucher reversal = baseVoucher("REVERSAL", original.getCode(), reversalDate,
        "冲销 " + original.getCode() + "：" + reason.trim(), totals);
    reversal.setStatus(VoucherStatus.POSTED); reversal.setPostedAt(OffsetDateTime.now()); reversal.setPostedBy(actor.username());
    reversal.setCreatedBy(actor.username());
    AccountingVoucher savedReversal = voucherRepository.save(reversal);
    saveEntries(savedReversal.getId(), reversingLines);
    original.setStatus(VoucherStatus.REVERSED); original.setReversedAt(OffsetDateTime.now());
    original.setReversedBy(actor.username()); original.setReversalReason(reason.trim());
    original.setReversalVoucherId(savedReversal.getId());
    voucherRepository.save(original);
    return response(original);
  }

  @Transactional(readOnly = true)
  public Page<VoucherResponse> vouchers(Pageable pageable) {
    Page<AccountingVoucher> page = voucherRepository.findAllByOrderByVoucherDateDescCreatedAtDesc(pageable);
    Map<UUID, List<AccountingEntry>> entriesByVoucher = entryRepository
        .findByVoucherIdInOrderByVoucherIdAscCreatedAtAsc(page.getContent().stream().map(AccountingVoucher::getId).toList())
        .stream().collect(Collectors.groupingBy(AccountingEntry::getVoucherId));
    return page.map(voucher -> toResponse(voucher, entriesByVoucher.getOrDefault(voucher.getId(), List.of())));
  }

  @Transactional(readOnly = true)
  @Cacheable("ledgerOverview")
  public LedgerOverview overview() {
    FinancialStatements statements = statements();
    var totals = voucherRepository.aggregateTotals();
    return new LedgerOverview(totals.getVoucherCount(), amount(totals.getTotalDebit()), amount(totals.getTotalCredit()),
        statements.totalRevenue(), statements.totalExpense(), statements.profit(), statements.netCashFlow());
  }

  @Transactional(readOnly = true)
  public FinancialStatements statements() {
    return statements(null, LocalDate.now());
  }

  @Transactional(readOnly = true)
  public FinancialStatements statements(LocalDate requestedFrom, LocalDate requestedTo) {
    LocalDate to = requestedTo == null ? LocalDate.now() : requestedTo;
    LocalDate from = requestedFrom == null ? LocalDate.of(to.getYear(), 1, 1) : requestedFrom;
    if (to.isBefore(from)) throw new BusinessException("报表结束日期不能早于开始日期");
    if (from.getYear() != to.getYear()) throw new BusinessException("单次财务报表查询必须在同一会计年度内");

    Map<String, AccountingAccount> accountMap = accountRepository.findAllByOrderByCodeAsc().stream()
        .collect(Collectors.toMap(AccountingAccount::getCode, item -> item));
    List<AccountOpeningBalance> openings = openingBalanceRepository.findByFiscalYearOrderByAccountCodeAsc(to.getYear());
    LocalDate balanceStart = openings.isEmpty() ? LocalDate.of(1900, 1, 1) : LocalDate.of(to.getYear(), 1, 1);
    Map<String, MutableTotals> balanceTotals = totals(entryRepository.aggregateByAccountBetween(balanceStart, to));
    openings.forEach(item -> balanceTotals.computeIfAbsent(item.getAccountCode(), key -> new MutableTotals(
        key, accountMap.containsKey(key) ? accountMap.get(key).getName() : key))
        .add(item.getDebitBalance(), item.getCreditBalance()));
    Map<String, MutableTotals> periodTotals = totals(entryRepository.aggregateByAccountBetween(from, to));

    List<StatementLine> balanceLines = toLines(balanceTotals, accountMap);
    List<StatementLine> periodLines = toLines(periodTotals, accountMap);
    List<StatementLine> assets = filter(balanceLines, accountMap, "ASSET");
    List<StatementLine> liabilities = filter(balanceLines, accountMap, "LIABILITY");
    List<StatementLine> equity = new ArrayList<>(filter(balanceLines, accountMap, "EQUITY"));
    List<StatementLine> revenue = filter(periodLines, accountMap, "REVENUE");
    List<StatementLine> expenses = filter(periodLines, accountMap, "EXPENSE");
    BigDecimal totalRevenue = sumBalance(revenue), totalExpense = sumBalance(expenses);
    BigDecimal profit = totalRevenue.subtract(totalExpense);
    BigDecimal yearToDateProfit = sumBalance(filter(balanceLines, accountMap, "REVENUE"))
        .subtract(sumBalance(filter(balanceLines, accountMap, "EXPENSE")));
    if (yearToDateProfit.signum() != 0) equity.add(new StatementLine("CURRENT_PROFIT", "本年累计利润",
        yearToDateProfit.signum() < 0 ? yearToDateProfit.abs() : BigDecimal.ZERO,
        yearToDateProfit.signum() > 0 ? yearToDateProfit : BigDecimal.ZERO, yearToDateProfit));
    BigDecimal totalAssets = sumBalance(assets), totalLiabilities = sumBalance(liabilities);
    BigDecimal totalEquity = sumBalance(equity);
    BigDecimal cash = periodLines.stream().filter(item -> isCashAccount(item.accountCode(), accountMap))
        .map(StatementLine::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new FinancialStatements(from, to, assets, liabilities, equity, revenue, expenses,
        totalAssets, totalLiabilities, totalEquity, totalLiabilities.add(totalEquity),
        totalRevenue, totalExpense, profit, cash);
  }

  @Transactional(readOnly = true)
  public List<AccountResponse> accounts() {
    return accountRepository.findAllByOrderByCodeAsc().stream().map(this::toAccountResponse).toList();
  }

  @Transactional
  public AccountResponse saveAccount(UUID id, SaveAccountRequest request) {
    String code = request.code().trim();
    String category = normalizeCategory(request.category());
    String direction = normalizeDirection(request.normalDirection());
    AccountingAccount item = id == null ? new AccountingAccount()
        : accountRepository.findById(id).orElseThrow(() -> new BusinessException("会计科目不存在"));
    if (id == null && accountRepository.existsByCode(code)) throw new BusinessException("会计科目编码已存在");
    if (item.isSystemAccount()) {
      if (!item.getCode().equals(code) || !item.getCategory().equals(category)
          || !item.getNormalDirection().equals(direction) || !request.active()) {
        throw new BusinessException("系统科目的编码、类别、余额方向和启用状态不可修改");
      }
    }
    item.setCode(code); item.setName(request.name().trim()); item.setCategory(category);
    item.setNormalDirection(direction); item.setCashAccount(request.cashAccount()); item.setActive(request.active());
    return toAccountResponse(accountRepository.save(item));
  }

  @Transactional(readOnly = true)
  public List<OpeningBalanceResponse> openingBalances(int fiscalYear) {
    Map<String, AccountingAccount> accountMap = accountRepository.findAllByOrderByCodeAsc().stream()
        .collect(Collectors.toMap(AccountingAccount::getCode, item -> item));
    return openingBalanceRepository.findByFiscalYearOrderByAccountCodeAsc(fiscalYear).stream()
        .map(item -> toOpeningResponse(item, accountMap.get(item.getAccountCode()))).toList();
  }

  @Transactional
  public OpeningBalanceResponse saveOpeningBalance(SaveOpeningBalanceRequest request) {
    periodGuard.assertOpen(LocalDate.of(request.fiscalYear(), 1, 1));
    AccountingAccount account = requireActiveAccount(request.accountCode());
    if (request.debitBalance().signum() > 0 && request.creditBalance().signum() > 0) {
      throw new BusinessException("期初余额不能同时填写借方和贷方");
    }
    AccountOpeningBalance item = openingBalanceRepository
        .findByFiscalYearAndAccountCode(request.fiscalYear(), account.getCode())
        .orElseGet(AccountOpeningBalance::new);
    item.setFiscalYear(request.fiscalYear()); item.setAccountCode(account.getCode());
    item.setDebitBalance(amount(request.debitBalance())); item.setCreditBalance(amount(request.creditBalance()));
    item.setNote(trim(request.note()));
    return toOpeningResponse(openingBalanceRepository.save(item), account);
  }

  private AccountingVoucher baseVoucher(String bizType, String bizNo, LocalDate date, String description, Totals totals) {
    AccountingVoucher voucher = new AccountingVoucher();
    voucher.setCode(nextCode(date)); voucher.setBizType(bizType); voucher.setBizNo(bizNo);
    voucher.setVoucherDate(date); voucher.setDescription(description);
    voucher.setTotalDebit(totals.debit()); voucher.setTotalCredit(totals.credit());
    return voucher;
  }

  private List<AccountingEntry> saveEntries(UUID voucherId, List<PostingLine> lines) {
    return entryRepository.saveAll(lines.stream().map(line -> {
      AccountingEntry entry = new AccountingEntry(); entry.setVoucherId(voucherId);
      entry.setAccountCode(line.accountCode()); entry.setAccountName(line.accountName());
      entry.setDebit(amount(line.debit())); entry.setCredit(amount(line.credit())); entry.setSummary(line.summary());
      return entry;
    }).toList());
  }

  private Totals validateLines(List<PostingLine> lines) {
    if (lines == null || lines.size() < 2) throw new BusinessException("会计凭证至少需要两条分录");
    BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
    for (PostingLine line : lines) {
      if (line.accountCode() == null || line.accountCode().isBlank() || line.accountName() == null || line.accountName().isBlank()) {
        throw new BusinessException("会计科目编码和名称不能为空");
      }
      BigDecimal lineDebit = amount(line.debit()), lineCredit = amount(line.credit());
      if (lineDebit.signum() < 0 || lineCredit.signum() < 0) throw new BusinessException("借贷金额不能为负数");
      if ((lineDebit.signum() == 0) == (lineCredit.signum() == 0)) throw new BusinessException("每条分录必须且只能填写借方或贷方金额");
      debit = debit.add(lineDebit); credit = credit.add(lineCredit);
    }
    if (debit.signum() <= 0 || debit.compareTo(credit) != 0) throw new BusinessException("会计凭证借贷不平衡");
    return new Totals(debit, credit);
  }

  private AccountingAccount requireActiveAccount(String code) {
    AccountingAccount account = accountRepository.findByCode(code == null ? "" : code.trim())
        .orElseThrow(() -> new BusinessException("会计科目不存在：" + code));
    if (!account.isActive()) throw new BusinessException("会计科目已停用：" + account.getCode());
    return account;
  }

  private Map<String, MutableTotals> totals(List<Object[]> rows) {
    Map<String, MutableTotals> result = new LinkedHashMap<>();
    rows.forEach(row -> result.put((String) row[0], new MutableTotals(
        (String) row[0], (String) row[1]).add((BigDecimal) row[2], (BigDecimal) row[3])));
    return result;
  }

  private List<StatementLine> toLines(Map<String, MutableTotals> source,
      Map<String, AccountingAccount> accounts) {
    return source.values().stream().map(item -> {
      AccountingAccount account = accounts.get(item.code);
      boolean creditNature = account == null ? inferredCreditNature(item.code)
          : "CREDIT".equals(account.getNormalDirection());
      BigDecimal balance = creditNature ? item.credit.subtract(item.debit) : item.debit.subtract(item.credit);
      return new StatementLine(item.code, account == null ? item.name : account.getName(),
          item.debit, item.credit, balance);
    }).sorted(java.util.Comparator.comparing(StatementLine::accountCode)).toList();
  }

  private List<StatementLine> filter(List<StatementLine> lines,
      Map<String, AccountingAccount> accounts, String category) {
    return lines.stream().filter(item -> category.equals(accountCategory(item.accountCode(), accounts))).toList();
  }

  private String accountCategory(String code, Map<String, AccountingAccount> accounts) {
    AccountingAccount account = accounts.get(code);
    if (account != null) return account.getCategory();
    if (code.startsWith("1")) return "ASSET";
    if (code.startsWith("2")) return "LIABILITY";
    if (code.startsWith("3")) return "EQUITY";
    if (code.startsWith("6") && code.compareTo("6400") < 0) return "REVENUE";
    if (code.startsWith("6")) return "EXPENSE";
    return "OTHER";
  }

  private boolean inferredCreditNature(String code) {
    String category = accountCategory(code, Map.of());
    return Set.of("LIABILITY", "EQUITY", "REVENUE").contains(category);
  }

  private boolean isCashAccount(String code, Map<String, AccountingAccount> accounts) {
    AccountingAccount account = accounts.get(code);
    return account == null ? code.equals("1001") || code.equals("1002") : account.isCashAccount();
  }

  private String normalizeCategory(String value) {
    String category = value.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE").contains(category)) {
      throw new BusinessException("科目类别不支持");
    }
    return category;
  }

  private String normalizeDirection(String value) {
    String direction = value.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("DEBIT", "CREDIT").contains(direction)) throw new BusinessException("余额方向只能是借方或贷方");
    return direction;
  }

  private AccountResponse toAccountResponse(AccountingAccount item) {
    return new AccountResponse(item.getId(), item.getCode(), item.getName(), item.getCategory(),
        item.getNormalDirection(), item.isCashAccount(), item.isActive(), item.isSystemAccount());
  }

  private OpeningBalanceResponse toOpeningResponse(AccountOpeningBalance item, AccountingAccount account) {
    return new OpeningBalanceResponse(item.getId(), item.getFiscalYear(), item.getAccountCode(),
        account == null ? item.getAccountCode() : account.getName(), item.getDebitBalance(),
        item.getCreditBalance(), item.getNote());
  }

  private AccountingVoucher requireVoucher(UUID id) { return voucherRepository.findById(id).orElseThrow(() -> new BusinessException("会计凭证不存在")); }
  private VoucherResponse response(AccountingVoucher voucher) { return toResponse(voucher, entryRepository.findByVoucherIdOrderByCreatedAtAsc(voucher.getId())); }
  private PostingLine toPostingLine(AccountingEntry entry) { return new PostingLine(entry.getAccountCode(), entry.getAccountName(), entry.getDebit(), entry.getCredit(), entry.getSummary()); }
  private VoucherResponse toResponse(AccountingVoucher voucher, List<AccountingEntry> entries) {
    return new VoucherResponse(voucher.getId(), voucher.getCode(), voucher.getBizType(), voucher.getBizNo(), voucher.getVoucherDate(),
        voucher.getDescription(), voucher.getStatus(), voucher.getTotalDebit(), voucher.getTotalCredit(),
        entries.stream().map(e -> new EntryResponse(e.getId(), e.getAccountCode(), e.getAccountName(), e.getDebit(), e.getCredit(), e.getSummary())).toList(),
        voucher.getReviewedAt(), voucher.getReviewedBy(), voucher.getPostedAt(), voucher.getPostedBy(),
        voucher.getReversedAt(), voucher.getReversedBy(), voucher.getReversalReason(), voucher.getReversalVoucherId());
  }
  private String nextCode(LocalDate date) {
    String base = "PZ-" + date.toString().replace("-", "") + "-" + String.format("%04d", voucherRepository.count() + 1);
    String code = base; int suffix = 1;
    while (voucherRepository.existsByCode(code)) code = base + "-" + suffix++;
    return code;
  }
  private Actor currentActor(boolean required) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
      return new Actor(principal.getUsername(), principal.displayName());
    }
    if (required) throw new AccessDeniedException("请先登录");
    return new Actor("system", "系统");
  }
  private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  private BigDecimal sumBalance(List<StatementLine> lines) { return lines.stream().map(StatementLine::balance).reduce(BigDecimal.ZERO, BigDecimal::add); }
  private record Totals(BigDecimal debit, BigDecimal credit) {}
  private record Actor(String username, String displayName) {}
  private static final class MutableTotals {
    private final String code;
    private final String name;
    private BigDecimal debit = BigDecimal.ZERO;
    private BigDecimal credit = BigDecimal.ZERO;
    private MutableTotals(String code, String name) { this.code = code; this.name = name; }
    private MutableTotals add(BigDecimal debit, BigDecimal credit) {
      this.debit = this.debit.add(amount(debit));
      this.credit = this.credit.add(amount(credit));
      return this;
    }
  }
}
