package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.ServiceContract;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceContractRepository extends JpaRepository<ServiceContract, UUID> {

  List<ServiceContract> findByCustomerIdOrderByStartDateDesc(UUID customerId);

  List<ServiceContract> findAllByOrderByEndDateAsc();

  boolean existsByCode(String code);

  boolean existsByQuoteId(UUID quoteId);

  Optional<ServiceContract> findByQuoteId(UUID quoteId);

  @Query("SELECT COUNT(c) FROM ServiceContract c WHERE c.endDate <= :deadline AND c.status <> 'CLOSED'")
  long countRenewalRisks(@Param("deadline") LocalDate deadline);

}
