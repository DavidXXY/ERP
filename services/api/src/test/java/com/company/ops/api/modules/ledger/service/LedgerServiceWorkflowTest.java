package com.company.ops.api.modules.ledger.service;

import static com.company.ops.api.modules.ledger.dto.LedgerDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class LedgerServiceWorkflowTest {
  @Mock private AccountingVoucherRepository vouchers;
  @Mock private AccountingEntryRepository entries;
  @Mock private AccountingAccountRepository accounts;
  @Mock private AccountOpeningBalanceRepository openingBalances;
  @Mock private AccountingPeriodGuard periodGuard;

  @AfterEach
  void clearSecurity() { SecurityContextHolder.clearContext(); }

  @Test
  void enforcesMakerReviewerPosterSeparation() {
    AtomicReference<AccountingVoucher> savedVoucher = new AtomicReference<>();
    List<AccountingEntry> savedEntries = new ArrayList<>();
    when(vouchers.findByBizTypeAndBizNo("MANUAL", "M-001")).thenReturn(Optional.empty());
    when(vouchers.count()).thenReturn(0L); when(vouchers.existsByCode(any())).thenReturn(false);
    when(vouchers.save(any())).thenAnswer(invocation -> {
      AccountingVoucher item = invocation.getArgument(0);
      if (item.getId() == null) item.setId(UUID.randomUUID());
      savedVoucher.set(item); return item;
    });
    when(entries.saveAll(any())).thenAnswer(invocation -> {
      List<AccountingEntry> items = invocation.getArgument(0); savedEntries.clear(); savedEntries.addAll(items); return items;
    });
    when(entries.findByVoucherIdOrderByCreatedAtAsc(any())).thenAnswer(invocation -> List.copyOf(savedEntries));
    when(vouchers.findById(any())).thenAnswer(invocation -> Optional.ofNullable(savedVoucher.get()));
    when(accounts.findByCode(any())).thenAnswer(invocation -> {
      String code = invocation.getArgument(0);
      com.company.ops.api.modules.ledger.domain.AccountingAccount account =
          new com.company.ops.api.modules.ledger.domain.AccountingAccount();
      account.setCode(code); account.setName("6602".equals(code) ? "管理费用" : "应付账款");
      account.setCategory(code.startsWith("6") ? "EXPENSE" : "LIABILITY");
      account.setNormalDirection(code.startsWith("6") ? "DEBIT" : "CREDIT");
      account.setActive(true);
      return Optional.of(account);
    });
    LedgerService service = new LedgerService(vouchers, entries, periodGuard, accounts, openingBalances);
    CreateVoucherRequest request = new CreateVoucherRequest("MANUAL", "M-001", LocalDate.now(), "计提费用", List.of(
        new ManualPostingLine("6602", "管理费用", new BigDecimal("100"), BigDecimal.ZERO, "计提"),
        new ManualPostingLine("2202", "应付账款", BigDecimal.ZERO, new BigDecimal("100"), "计提")));

    authenticate("maker");
    VoucherResponse draft = service.createDraft(request);
    assertThat(draft.status()).isEqualTo(VoucherStatus.DRAFT);
    assertThatThrownBy(() -> service.review(draft.id())).isInstanceOf(BusinessException.class).hasMessageContaining("制单人与复核人");

    authenticate("reviewer");
    assertThat(service.review(draft.id()).status()).isEqualTo(VoucherStatus.REVIEWED);
    assertThatThrownBy(() -> service.postReviewed(draft.id())).isInstanceOf(BusinessException.class).hasMessageContaining("复核人与记账人");

    authenticate("poster");
    assertThat(service.postReviewed(draft.id()).status()).isEqualTo(VoucherStatus.POSTED);
  }

  @Test
  void separatesPeriodProfitFromYearToDateEquityAndIncludesOpeningBalances() {
    LocalDate from = LocalDate.of(2026, 7, 1);
    LocalDate to = LocalDate.of(2026, 7, 31);
    when(accounts.findAllByOrderByCodeAsc()).thenReturn(List.of(
        account("1002", "银行存款", "ASSET", "DEBIT", true),
        account("2202", "应付账款", "LIABILITY", "CREDIT", false),
        account("3001", "实收资本", "EQUITY", "CREDIT", false),
        account("6001", "主营业务收入", "REVENUE", "CREDIT", false),
        account("6602", "管理费用", "EXPENSE", "DEBIT", false)));
    AccountOpeningBalance capital = new AccountOpeningBalance();
    capital.setFiscalYear(2026); capital.setAccountCode("3001");
    capital.setDebitBalance(BigDecimal.ZERO); capital.setCreditBalance(new BigDecimal("1000"));
    when(openingBalances.findByFiscalYearOrderByAccountCodeAsc(2026)).thenReturn(List.of(capital));
    when(entries.aggregateByAccountBetween(eq(LocalDate.of(2026, 1, 1)), eq(to))).thenReturn(List.of(
        row("1002", "银行存款", "1400", "0"),
        row("2202", "应付账款", "0", "100"),
        row("6001", "主营业务收入", "0", "500"),
        row("6602", "管理费用", "200", "0")));
    when(entries.aggregateByAccountBetween(eq(from), eq(to))).thenReturn(List.of(
        row("1002", "银行存款", "400", "100"),
        row("6001", "主营业务收入", "0", "200"),
        row("6602", "管理费用", "50", "0")));

    LedgerService service = new LedgerService(vouchers, entries, periodGuard, accounts, openingBalances);
    FinancialStatements result = service.statements(from, to);

    assertThat(result.profit()).isEqualByComparingTo("150");
    assertThat(result.totalEquity()).isEqualByComparingTo("1300");
    assertThat(result.totalLiabilitiesAndEquity()).isEqualByComparingTo("1400");
    assertThat(result.totalAssets()).isEqualByComparingTo("1400");
    assertThat(result.netCashFlow()).isEqualByComparingTo("300");
    assertThat(result.equity()).anySatisfy(line -> {
      assertThat(line.accountCode()).isEqualTo("CURRENT_PROFIT");
      assertThat(line.accountName()).isEqualTo("本年累计利润");
      assertThat(line.balance()).isEqualByComparingTo("300");
    });
  }

  private AccountingAccount account(String code, String name, String category,
      String direction, boolean cash) {
    AccountingAccount item = new AccountingAccount();
    item.setCode(code); item.setName(name); item.setCategory(category);
    item.setNormalDirection(direction); item.setCashAccount(cash); item.setActive(true);
    return item;
  }

  private Object[] row(String code, String name, String debit, String credit) {
    return new Object[] {code, name, new BigDecimal(debit), new BigDecimal(credit)};
  }

  private void authenticate(String username) {
    SystemUser user = new SystemUser(); user.setId(UUID.randomUUID()); user.setTenantId("default");
    user.setUsername(username); user.setDisplayName(username); user.setPasswordHash("unused"); user.setEnabled(true);
    UserPrincipal principal = new UserPrincipal(user);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }
}
