package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierChangeRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierChangeRequestRepository extends JpaRepository<SupplierChangeRequest, UUID> {
  List<SupplierChangeRequest> findAllByOrderByCreatedAtDesc();
  boolean existsBySupplierIdAndStatus(UUID supplierId, String status);
}
