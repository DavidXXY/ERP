package com.company.ops.api.modules.ledger.repository;

import com.company.ops.api.modules.ledger.domain.AccountingAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, UUID> {
  List<AccountingAccount> findAllByOrderByCodeAsc();
  Optional<AccountingAccount> findByCode(String code);
  boolean existsByCode(String code);
}
