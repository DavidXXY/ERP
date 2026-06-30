package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.Customer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  java.util.List<Customer> findAllByOrderByCreatedAtDesc();

  boolean existsByCode(String code);
}
