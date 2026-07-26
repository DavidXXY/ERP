package com.company.ops.api.modules.ledger.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;
import static com.company.ops.api.modules.ledger.dto.LedgerDtos.*;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.governance.service.AccountingPeriodGuard;
import com.company.ops.api.modules.ledger.domain.AccountingEntry;
import com.company.ops.api.modules.ledger.domain.AccountingVoucher;
import com.company.ops.api.modules.ledger.domain.VoucherStatus;
import com.company.ops.api.modules.ledger.repository.AccountingEntryRepository;
import com.company.ops.api.modules.ledger.repository.AccountingVoucherRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {
  private final AccountingVoucherRepository voucherRepository;
  private final AccountingEntryRepository entryRepository;
  private final AccountingPeriodGuard periodGuard;

  public LedgerService(AccountingVoucherRepository voucherRepository,
      AccountingEntryRepository entryRepository, AccountingPeriodGuard periodGuard) {
    this.voucherRepository = voucherRepository;
    this.entryRepository = entryRepository;
    this.periodGuard = periodGuard;
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
    List<PostingLine> lines = request.lines().stream().map(line -> new PostingLine(
        line.accountCode().trim(), line.accountName().trim(), line.debit(), line.credit(), trim(line.summary()))).toList();
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
    if (original.getStatus() != VoucherStatus.POSTED) throw new BusinessException("只有已记账凭证可以冲销");
    periodGuard.assertOpen(request.reversalDate());
    Actor actor = currentActor(true);
    List<AccountingEntry> originalEntries = entryRepository.findByVoucherIdOrderByCreatedAtAsc(id);
    List<PostingLine> reversingLines = originalEntries.stream().map(entry -> new PostingLine(
        entry.getAccountCode(), entry.getAccountName(), entry.getCredit(), entry.getDebit(), "冲销：" + entry.getSummary())).toList();
    Totals totals = validateLines(reversingLines);
    AccountingVoucher reversal = baseVoucher("REVERSAL", original.getCode(), request.reversalDate(),
        "冲销 " + original.getCode() + "：" + request.reason().trim(), totals);
    reversal.setStatus(VoucherStatus.POSTED); reversal.setPostedAt(OffsetDateTime.now()); reversal.setPostedBy(actor.username());
    reversal.setCreatedBy(actor.username());
    AccountingVoucher savedReversal = voucherRepository.save(reversal);
    saveEntries(savedReversal.getId(), reversingLines);
    original.setStatus(VoucherStatus.REVERSED); original.setReversedAt(OffsetDateTime.now());
    original.setReversedBy(actor.username()); original.setReversalReason(request.reason().trim());
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
  public LedgerOverview overview() {
    FinancialStatements statements = statements();
    var totals = voucherRepository.aggregateTotals();
    return new LedgerOverview(totals.getVoucherCount(), amount(totals.getTotalDebit()), amount(totals.getTotalCredit()),
        statements.totalRevenue(), statements.totalExpense(), statements.profit(), statements.netCashFlow());
  }

  @Transactional(readOnly = true)
  public FinancialStatements statements() {
    List<StatementLine> lines = entryRepository.aggregateByAccount().stream().map(row -> {
      String code = (String) row[0]; String name = (String) row[1];
      BigDecimal debit = amount((BigDecimal) row[2]); BigDecimal credit = amount((BigDecimal) row[3]);
      boolean creditNature = code.startsWith("2") || (code.startsWith("6") && code.compareTo("6400") < 0);
      return new StatementLine(code, name, debit, credit, creditNature ? credit.subtract(debit) : debit.subtract(credit));
    }).toList();
    List<StatementLine> assets = lines.stream().filter(i -> i.accountCode().startsWith("1")).toList();
    List<StatementLine> liabilities = lines.stream().filter(i -> i.accountCode().startsWith("2")).toList();
    List<StatementLine> revenue = lines.stream().filter(i -> i.accountCode().startsWith("6") && i.accountCode().compareTo("6400") < 0).toList();
    List<StatementLine> expenses = lines.stream().filter(i -> i.accountCode().startsWith("6") && i.accountCode().compareTo("6400") >= 0).toList();
    BigDecimal totalAssets = sumBalance(assets), totalLiabilities = sumBalance(liabilities);
    BigDecimal totalRevenue = sumBalance(revenue), totalExpense = sumBalance(expenses);
    BigDecimal cash = lines.stream().filter(i -> i.accountCode().equals("1002")).map(StatementLine::balance).findFirst().orElse(BigDecimal.ZERO);
    return new FinancialStatements(assets, liabilities, revenue, expenses, totalAssets, totalLiabilities,
        totalRevenue, totalExpense, totalRevenue.subtract(totalExpense), cash);
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
}
