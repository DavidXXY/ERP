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
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Collection;

public interface ServiceContractRepository extends JpaRepository<ServiceContract, UUID> {

  List<ServiceContract> findByCustomerIdOrderByStartDateDesc(UUID customerId);

  List<ServiceContract> findAllByOrderByEndDateAsc();
  List<ServiceContract> findByStatusNotAndEndDateLessThanEqualOrderByEndDateAsc(
      com.company.ops.api.modules.crm.domain.ContractStatus status, LocalDate deadline);

  @Query("select c from ServiceContract c where coalesce(c.startDate,c.endDate) between :startDate and :endDate order by c.endDate")
  List<ServiceContract> findByBusinessDateBetween(@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

  boolean existsByCode(String code);

  boolean existsByQuoteId(UUID quoteId);

  Optional<ServiceContract> findByQuoteId(UUID quoteId);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from ServiceContract c where c.id = :id")
  Optional<ServiceContract> findByIdForUpdate(@Param("id") UUID id);
  List<ServiceContract> findByParentContractIdOrderByStartDateDesc(UUID parentContractId);
  Page<ServiceContract> findByParentContractIdOrderByStartDateDesc(UUID parentContractId, Pageable pageable);
  boolean existsByParentContractId(UUID parentContractId);
  List<ServiceContract> findByCodeContainingIgnoreCaseOrProjectNameContainingIgnoreCase(String code,String name,Pageable pageable);

  @Query("SELECT COUNT(c) FROM ServiceContract c WHERE c.endDate <= :deadline AND c.status <> 'CLOSED'")
  long countRenewalRisks(@Param("deadline") LocalDate deadline);

  @Query("select coalesce(sum(c.amount), 0) from ServiceContract c")
  BigDecimal sumContractAmount();

  @Query("select c.customerId, coalesce(sum(c.amount), 0) from ServiceContract c group by c.customerId")
  List<Object[]> aggregateAmountByCustomer();

  @Query("select c.customerId, coalesce(sum(c.amount), 0) from ServiceContract c where c.customerId in :customerIds group by c.customerId")
  List<Object[]> aggregateAmountByCustomerIn(Collection<UUID> customerIds);

  List<ServiceContract> findByCustomerIdIn(Collection<UUID> customerIds);

}
