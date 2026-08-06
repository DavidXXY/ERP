package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierCategoryRepository extends JpaRepository<SupplierCategory, UUID> {
  List<SupplierCategory> findAllByOrderBySortOrderAscNameAsc();
  Optional<SupplierCategory> findByNameIgnoreCase(String name);
}
