package com.company.ops.api.modules.ledger.repository;

import com.company.ops.api.modules.ledger.domain.AccountingEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountingEntryRepository extends JpaRepository<AccountingEntry, UUID> {
  List<AccountingEntry> findByVoucherIdOrderByCreatedAtAsc(UUID voucherId);
  List<AccountingEntry> findByVoucherIdInOrderByVoucherIdAscCreatedAtAsc(Collection<UUID> voucherIds);

  @Query("SELECT e.accountCode, e.accountName, COALESCE(SUM(e.debit), 0), "
      + "COALESCE(SUM(e.credit), 0) FROM AccountingEntry e "
      + "JOIN AccountingVoucher v ON v.id = e.voucherId WHERE v.status IN ('POSTED','REVERSED') "
      + "GROUP BY e.accountCode, e.accountName ORDER BY e.accountCode")
  List<Object[]> aggregateByAccount();

  @Query("SELECT COALESCE(SUM(e.debit), 0) FROM AccountingEntry e JOIN AccountingVoucher v ON v.id = e.voucherId WHERE e.accountCode = :accountCode AND v.status IN ('POSTED','REVERSED')")
  BigDecimal sumDebitByAccountCode(@Param("accountCode") String accountCode);

  @Query("SELECT COALESCE(SUM(e.credit), 0) FROM AccountingEntry e JOIN AccountingVoucher v ON v.id = e.voucherId WHERE e.accountCode = :accountCode AND v.status IN ('POSTED','REVERSED')")
  BigDecimal sumCreditByAccountCode(@Param("accountCode") String accountCode);

  @Query("SELECT EXTRACT(YEAR FROM v.voucherDate), EXTRACT(MONTH FROM v.voucherDate), " +
         "COALESCE(SUM(CASE WHEN e.accountCode LIKE '6%' AND e.accountCode < '6400' THEN e.credit - e.debit ELSE 0 END), 0), " +
         "COALESCE(SUM(CASE WHEN e.accountCode LIKE '6%' AND e.accountCode >= '6400' THEN e.debit - e.credit ELSE 0 END), 0), " +
         "COALESCE(SUM(CASE WHEN e.accountCode = '1002' THEN e.debit ELSE 0 END), 0), " +
         "COALESCE(SUM(CASE WHEN e.accountCode = '1002' THEN e.credit ELSE 0 END), 0) " +
         "FROM AccountingEntry e JOIN AccountingVoucher v ON v.id = e.voucherId " +
         "WHERE v.voucherDate IS NOT NULL AND v.status IN ('POSTED','REVERSED') " +
         "GROUP BY EXTRACT(YEAR FROM v.voucherDate), EXTRACT(MONTH FROM v.voucherDate) " +
         "ORDER BY 1, 2")
  List<Object[]> aggregateMonthlyTrends();
}
