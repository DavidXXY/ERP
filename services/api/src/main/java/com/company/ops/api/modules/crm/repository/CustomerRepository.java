package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
  List<Customer> findAllByOrderByCreatedAtDesc();
  boolean existsByCode(String code);
  Optional<Customer> findByCode(String code);
  List<Customer> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name,String code,Pageable pageable);
}
