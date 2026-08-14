package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierPortalAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierPortalAccountRepository extends JpaRepository<SupplierPortalAccount, UUID> {
  Optional<SupplierPortalAccount> findByEmailIgnoreCase(String email);
  boolean existsByEmailIgnoreCase(String email);
  boolean existsBySupplierId(UUID supplierId);
  List<SupplierPortalAccount> findAllByOrderByCreatedAtDesc();
  List<SupplierPortalAccount> findBySupplierIdOrderByCreatedAtAsc(UUID supplierId);

  long countByStatus(String status);
}
