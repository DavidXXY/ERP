package com.company.ops.api.modules.governance.repository;

import com.company.ops.api.modules.governance.domain.AccountingPeriod;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, UUID> {
  Optional<AccountingPeriod> findByFiscalYearAndPeriodNo(int fiscalYear, int periodNo);
  List<AccountingPeriod> findAllByOrderByFiscalYearDescPeriodNoDesc();
  long countByStatus(com.company.ops.api.modules.governance.domain.AccountingPeriodStatus status);
}
