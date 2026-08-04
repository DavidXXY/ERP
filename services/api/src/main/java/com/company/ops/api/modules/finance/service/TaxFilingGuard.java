package com.company.ops.api.modules.finance.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TaxFilingGuard {
  private final JdbcTemplate jdbc;
  public TaxFilingGuard(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public void assertUnlocked(LocalDate date) {
    Object value = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (!(value instanceof UserPrincipal principal)) throw new BusinessException("当前用户身份无效");
    Integer count = jdbc.queryForObject("select count(*) from fin_tax_filings where tenant_id=? and fiscal_year=? "
        + "and period_no=? and status='LOCKED'", Integer.class, principal.tenantId(), date.getYear(), date.getMonthValue());
    if (count != null && count > 0) {
      throw new BusinessException("税务申报期间已锁定，不能修改该期间发票");
    }
  }
}
