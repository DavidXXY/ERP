package com.company.ops.api.modules.ledger.repository;

import com.company.ops.api.modules.ledger.domain.AccountOpeningBalance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountOpeningBalanceRepository extends JpaRepository<AccountOpeningBalance, UUID> {
  List<AccountOpeningBalance> findByFiscalYearOrderByAccountCodeAsc(int fiscalYear);
  Optional<AccountOpeningBalance> findByFiscalYearAndAccountCode(int fiscalYear, String accountCode);
}
