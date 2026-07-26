package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.SystemNotification;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SystemNotificationRepository extends JpaRepository<SystemNotification, UUID> {
  List<SystemNotification> findAllByOrderByCreatedAtDesc();
  long countByReadFalse();
  boolean existsByDedupKey(String dedupKey);
  Optional<SystemNotification> findByDedupKey(String dedupKey);
  void deleteByDedupKey(String dedupKey);

  @Query("select n from SystemNotification n where n.targetUserId is null or n.targetUserId = :userId order by n.createdAt desc")
  List<SystemNotification> findVisibleForUser(@Param("userId") UUID userId);

  @Query(value = "select n from SystemNotification n where n.targetUserId is null or n.targetUserId = :userId order by n.createdAt desc",
      countQuery = "select count(n) from SystemNotification n where n.targetUserId is null or n.targetUserId = :userId")
  Page<SystemNotification> findVisibleForUser(@Param("userId") UUID userId, Pageable pageable);

}
