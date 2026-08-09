package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.FrameworkAgreement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrameworkAgreementRepository extends JpaRepository<FrameworkAgreement, UUID> {
  List<FrameworkAgreement> findAllByOrderByCreatedAtDesc();
  List<FrameworkAgreement> findBySupplierIdAndStatusOrderByCreatedAtDesc(UUID supplierId, String status);
  boolean existsByCode(String code);
}
