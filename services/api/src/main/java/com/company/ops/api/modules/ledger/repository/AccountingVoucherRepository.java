package com.company.ops.api.modules.ledger.repository;

import com.company.ops.api.modules.ledger.domain.AccountingVoucher;
import com.company.ops.api.modules.ledger.domain.VoucherStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountingVoucherRepository extends JpaRepository<AccountingVoucher, UUID> {
  Page<AccountingVoucher> findAllByOrderByVoucherDateDescCreatedAtDesc(Pageable pageable);
  Optional<AccountingVoucher> findByBizTypeAndBizNo(String bizType, String bizNo);
  boolean existsByCode(String code);

  @Query("""
      select count(v) as voucherCount,
             coalesce(sum(v.totalDebit), 0) as totalDebit,
             coalesce(sum(v.totalCredit), 0) as totalCredit
      from AccountingVoucher v
      where v.status in ('POSTED', 'REVERSED')
      """)
  VoucherTotals aggregateTotals();

  interface VoucherTotals {
    long getVoucherCount();
    BigDecimal getTotalDebit();
    BigDecimal getTotalCredit();
  }

  long countByVoucherDateBetweenAndStatusIn(
      LocalDate from, LocalDate to, Collection<VoucherStatus> statuses);
}
