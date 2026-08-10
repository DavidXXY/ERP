package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierPortalNotification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierPortalNotificationRepository
    extends JpaRepository<SupplierPortalNotification, UUID> {

  List<SupplierPortalNotification> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

  long countByAccountIdAndReadFalse(UUID accountId);
}
