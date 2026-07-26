package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.SystemNotification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemNotificationRepository extends JpaRepository<SystemNotification, UUID> {
  List<SystemNotification> findAllByOrderByCreatedAtDesc();
  long countByReadFalse();
  boolean existsByDedupKey(String dedupKey);

  @Query("select n from SystemNotification n where n.targetUserId is null or n.targetUserId = :userId order by n.createdAt desc")
  List<SystemNotification> findVisibleForUser(@Param("userId") UUID userId);

  @Query("select count(n) from SystemNotification n where n.read = false and (n.targetUserId is null or n.targetUserId = :userId)")
  long countUnreadForUser(@Param("userId") UUID userId);
}
