package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.SystemNotificationRead;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemNotificationReadRepository extends JpaRepository<SystemNotificationRead, UUID> {
  Optional<SystemNotificationRead> findByNotificationIdAndUserId(UUID notificationId, UUID userId);

  List<SystemNotificationRead> findByUserIdAndNotificationIdIn(UUID userId, Collection<UUID> notificationIds);

  @Query("""
      select count(n) from SystemNotification n
      where (n.targetUserId is null or n.targetUserId = :userId)
        and not exists (
          select r.id from SystemNotificationRead r
          where r.notificationId = n.id and r.userId = :userId
        )
      """)
  long countUnreadForUser(@Param("userId") UUID userId);
}
