package com.company.ops.api.modules.ledger.repository;

import com.company.ops.api.modules.ledger.domain.AccountingEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountingEntryRepository extends JpaRepository<AccountingEntry, UUID> {
  List<AccountingEntry> findByVoucherIdOrderByCreatedAtAsc(UUID voucherId);
  List<AccountingEntry> findAllByOrderByAccountCodeAsc();

  @Query("SELECT COALESCE(SUM(e.debit), 0) FROM AccountingEntry e WHERE e.accountCode = :accountCode")
  BigDecimal sumDebitByAccountCode(@Param("accountCode") String accountCode);

  @Query("SELECT COALESCE(SUM(e.credit), 0) FROM AccountingEntry e WHERE e.accountCode = :accountCode")
  BigDecimal sumCreditByAccountCode(@Param("accountCode") String accountCode);

  @Query("SELECT function('year', v.voucherDate), function('month', v.voucherDate), " +
         "COALESCE(SUM(CASE WHEN e.accountCode LIKE '6%' AND e.accountCode < '6400' THEN e.credit - e.debit ELSE 0 END), 0), " +
         "COALESCE(SUM(CASE WHEN e.accountCode LIKE '6%' AND e.accountCode >= '6400' THEN e.debit - e.credit ELSE 0 END), 0), " +
         "COALESCE(SUM(CASE WHEN e.accountCode = '1002' THEN e.debit ELSE 0 END), 0), " +
         "COALESCE(SUM(CASE WHEN e.accountCode = '1002' THEN e.credit ELSE 0 END), 0) " +
         "FROM AccountingEntry e JOIN AccountingVoucher v ON v.id = e.voucherId " +
         "WHERE v.voucherDate IS NOT NULL " +
         "GROUP BY function('year', v.voucherDate), function('month', v.voucherDate) " +
         "ORDER BY 1, 2")
  List<Object[]> aggregateMonthlyTrends();
}
