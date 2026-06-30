package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.ServiceContract;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceContractRepository extends JpaRepository<ServiceContract, UUID> {

  List<ServiceContract> findByCustomerIdOrderByStartDateDesc(UUID customerId);

  List<ServiceContract> findAllByOrderByEndDateAsc();

  boolean existsByCode(String code);

  boolean existsByQuoteId(UUID quoteId);

  Optional<ServiceContract> findByQuoteId(UUID quoteId);
}
