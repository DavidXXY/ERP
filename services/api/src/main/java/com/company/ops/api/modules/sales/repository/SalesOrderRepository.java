package com.company.ops.api.modules.sales.repository;

import com.company.ops.api.modules.sales.domain.SalesOrder;
import com.company.ops.api.modules.sales.domain.SalesOrderStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {
  Page<SalesOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
  boolean existsByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from SalesOrder o where o.id = :id")
  Optional<SalesOrder> findByIdForUpdate(@Param("id") UUID id);

  @Query("select coalesce(sum(o.totalAmount), 0) from SalesOrder o "
      + "where o.customerId = :customerId and o.status in :statuses")
  BigDecimal sumOpenAmountByCustomer(@Param("customerId") UUID customerId,
      @Param("statuses") Collection<SalesOrderStatus> statuses);
}