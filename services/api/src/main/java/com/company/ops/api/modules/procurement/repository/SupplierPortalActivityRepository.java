package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierPortalActivity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierPortalActivityRepository extends JpaRepository<SupplierPortalActivity, UUID> {
  List<SupplierPortalActivity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
  Optional<SupplierPortalActivity> findFirstByAccountIdAndActionOrderByCreatedAtDesc(UUID accountId, String action);
}
