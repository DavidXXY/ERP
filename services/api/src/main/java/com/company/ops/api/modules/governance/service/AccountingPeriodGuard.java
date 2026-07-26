package com.company.ops.api.modules.governance.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.governance.domain.AccountingPeriodStatus;
import com.company.ops.api.modules.governance.repository.AccountingPeriodRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountingPeriodGuard {
  private final AccountingPeriodRepository periods;

  public AccountingPeriodGuard(AccountingPeriodRepository periods) { this.periods = periods; }

  @Transactional(readOnly = true)
  public void assertOpen(LocalDate date) {
    if (date == null) throw new BusinessException("业务日期不能为空");
    periods.findByFiscalYearAndPeriodNo(date.getYear(), date.getMonthValue()).ifPresent(period -> {
      if (period.getStatus() != AccountingPeriodStatus.OPEN) {
        throw new BusinessException("会计期间 " + date.getYear() + "-" + String.format("%02d", date.getMonthValue()) + " 已关账或正在关账");
      }
    });
  }
}
