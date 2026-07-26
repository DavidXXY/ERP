package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import java.util.List;
import java.util.Collection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProcurementPayableRepository extends JpaRepository<ProcurementPayable, UUID> {

  List<ProcurementPayable> findAllByOrderByDueDateAsc();
  Page<ProcurementPayable> findAllByOrderByDueDateAsc(Pageable pageable);
  List<ProcurementPayable> findByDueDateBetweenOrderByDueDateAsc(LocalDate startDate,LocalDate endDate);
  List<ProcurementPayable> findByOrderId(UUID orderId);
  List<ProcurementPayable> findByOrderIdIn(Collection<UUID> orderIds);
  Optional<ProcurementPayable> findByReceiptId(UUID receiptId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select payable from ProcurementPayable payable where payable.id = :id")
  Optional<ProcurementPayable> findByIdForUpdate(@Param("id") UUID id);

  @Query("select coalesce(sum(p.amount - p.paidAmount), 0) from ProcurementPayable p")
  BigDecimal sumOutstandingAmount();

  @Query("""
      select coalesce(sum(p.amount), 0) as totalAmount,
             coalesce(sum(p.paidAmount), 0) as paidAmount,
             coalesce(sum(case when p.dueDate < :today
               and p.status not in (:paidStatus, :cancelledStatus)
               then p.amount - p.paidAmount else 0 end), 0) as overdueAmount
      from ProcurementPayable p
      """)
  FinanceOverviewTotals aggregateFinanceOverview(@Param("today") LocalDate today,
      @Param("paidStatus") PayableStatus paidStatus,
      @Param("cancelledStatus") PayableStatus cancelledStatus);

  interface FinanceOverviewTotals {
    BigDecimal getTotalAmount();
    BigDecimal getPaidAmount();
    BigDecimal getOverdueAmount();
  }

  @Query("select p.supplierId, coalesce(sum(p.amount), 0), coalesce(sum(p.paidAmount), 0) "
      + "from ProcurementPayable p where p.supplierId in :supplierIds and p.status <> :cancelled "
      + "group by p.supplierId")
  List<Object[]> aggregateBySupplierIdIn(@Param("supplierIds") Collection<UUID> supplierIds,
      @Param("cancelled") PayableStatus cancelled);
}
