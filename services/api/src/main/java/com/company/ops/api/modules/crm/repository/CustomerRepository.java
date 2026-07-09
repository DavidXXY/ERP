package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.Customer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
  List<Customer> findAllByOrderByCreatedAtDesc();
  boolean existsByCode(String code);
}
