package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  @EntityGraph(attributePaths = {"contacts", "sites"})
  @Query("select c from Customer c where c.id = :id")
  Optional<Customer> findWithContactsAndSitesById(@Param("id") UUID id);

  @EntityGraph(attributePaths = {"contacts", "sites"})
  java.util.List<Customer> findAllByOrderByCreatedAtDesc();

  boolean existsByCode(String code);
}
