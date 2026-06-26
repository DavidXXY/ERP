package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.Supplier;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

  List<Supplier> findAllByOrderByCreatedAtDesc();

  boolean existsByCode(String code);
}
