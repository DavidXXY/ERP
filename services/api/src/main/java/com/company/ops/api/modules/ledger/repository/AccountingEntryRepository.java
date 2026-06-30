package com.company.ops.api.modules.ledger.repository;
import com.company.ops.api.modules.ledger.domain.AccountingEntry; import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountingEntryRepository extends JpaRepository<AccountingEntry,UUID>{List<AccountingEntry> findByVoucherIdOrderByCreatedAtAsc(UUID voucherId); List<AccountingEntry> findAllByOrderByAccountCodeAsc();}
