package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.Opportunity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

  List<Opportunity> findAllByOrderByUpdatedAtDesc();

  List<Opportunity> findByCustomerIdOrderByUpdatedAtDesc(UUID customerId);

  boolean existsByCode(String code);
  List<Opportunity> findByCustomerIdIn(Collection<UUID> customerIds);

}
