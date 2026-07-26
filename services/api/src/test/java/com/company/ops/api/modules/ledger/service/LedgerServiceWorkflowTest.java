package com.company.ops.api.modules.ledger.service;

import static com.company.ops.api.modules.ledger.dto.LedgerDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.governance.service.AccountingPeriodGuard;
import com.company.ops.api.modules.ledger.domain.AccountingEntry;
import com.company.ops.api.modules.ledger.domain.AccountingVoucher;
import com.company.ops.api.modules.ledger.domain.VoucherStatus;
import com.company.ops.api.modules.ledger.repository.AccountingEntryRepository;
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
    LedgerService service = new LedgerService(vouchers, entries, periodGuard);
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

  private void authenticate(String username) {
    SystemUser user = new SystemUser(); user.setId(UUID.randomUUID()); user.setTenantId("default");
    user.setUsername(username); user.setDisplayName(username); user.setPasswordHash("unused"); user.setEnabled(true);
    UserPrincipal principal = new UserPrincipal(user);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }
}
