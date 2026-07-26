package com.company.ops.api.modules.governance.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.governance.domain.AccountingPeriod;
import com.company.ops.api.modules.governance.domain.AccountingPeriodStatus;
import com.company.ops.api.modules.governance.repository.AccountingPeriodRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountingPeriodGuardTest {
  @Mock private AccountingPeriodRepository periods;

  @Test
  void blocksWritesToClosedOrClosingPeriodsButAllowsUnmanagedPeriods() {
    AccountingPeriod closed = new AccountingPeriod();
    closed.setStatus(AccountingPeriodStatus.CLOSED);
    when(periods.findByFiscalYearAndPeriodNo(2026, 7)).thenReturn(Optional.of(closed));
    when(periods.findByFiscalYearAndPeriodNo(2026, 8)).thenReturn(Optional.empty());
    AccountingPeriodGuard guard = new AccountingPeriodGuard(periods);

    assertThatThrownBy(() -> guard.assertOpen(LocalDate.of(2026, 7, 26)))
        .isInstanceOf(BusinessException.class).hasMessageContaining("已关账或正在关账");
    assertThatCode(() -> guard.assertOpen(LocalDate.of(2026, 8, 1))).doesNotThrowAnyException();
  }
}
