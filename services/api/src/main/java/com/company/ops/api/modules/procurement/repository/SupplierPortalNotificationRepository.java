package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierPortalNotification;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierPortalNotificationRepository
    extends JpaRepository<SupplierPortalNotification, UUID> {

  List<SupplierPortalNotification> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

  List<SupplierPortalNotification> findTop100ByAccountIdOrderByCreatedAtDesc(UUID accountId);

  List<SupplierPortalNotification> findTop100ByAccountIdAndCreatedAtBeforeOrderByCreatedAtDesc(
      UUID accountId, OffsetDateTime before);

  long countByAccountIdAndReadFalse(UUID accountId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update SupplierPortalNotification n set n.read = true, n.readAt = :now "
      + "where n.accountId = :accountId and n.read = false")
  int markAllRead(@Param("accountId") UUID accountId, @Param("now") OffsetDateTime now);
}
