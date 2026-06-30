package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.Receivable;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {

  List<Receivable> findByCustomerIdOrderByDueDateAsc(UUID customerId);

  List<Receivable> findAllByOrderByDueDateAsc();

  boolean existsByCode(String code);
}
