package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.MaterialCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, UUID> {

  List<MaterialCategory> findAllByOrderByNameAsc();

  Optional<MaterialCategory> findByNameIgnoreCase(String name);
}
