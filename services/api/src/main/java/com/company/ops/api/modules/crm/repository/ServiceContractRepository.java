package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.ServiceContract;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface ServiceContractRepository extends JpaRepository<ServiceContract, UUID> {

  List<ServiceContract> findByCustomerIdOrderByStartDateDesc(UUID customerId);

  List<ServiceContract> findAllByOrderByEndDateAsc();

  @Query("select c from ServiceContract c where coalesce(c.startDate,c.endDate) between :startDate and :endDate order by c.endDate")
  List<ServiceContract> findByBusinessDateBetween(@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

  boolean existsByCode(String code);

  boolean existsByQuoteId(UUID quoteId);

  Optional<ServiceContract> findByQuoteId(UUID quoteId);
  List<ServiceContract> findByCodeContainingIgnoreCaseOrProjectNameContainingIgnoreCase(String code,String name,Pageable pageable);

  @Query("SELECT COUNT(c) FROM ServiceContract c WHERE c.endDate <= :deadline AND c.status <> 'CLOSED'")
  long countRenewalRisks(@Param("deadline") LocalDate deadline);

  @Query("select coalesce(sum(c.amount), 0) from ServiceContract c")
  BigDecimal sumContractAmount();

  @Query("select c.customerId, coalesce(sum(c.amount), 0) from ServiceContract c group by c.customerId")
  List<Object[]> aggregateAmountByCustomer();

}
