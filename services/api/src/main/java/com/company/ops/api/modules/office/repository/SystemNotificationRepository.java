package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.SystemNotification;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SystemNotificationRepository extends JpaRepository<SystemNotification, UUID> {
  List<SystemNotification> findByCreatedAtAfterOrderByCreatedAtAsc(OffsetDateTime cutoff);
  List<SystemNotification> findAllByOrderByCreatedAtDesc();
  long countByReadFalse();
  boolean existsByDedupKey(String dedupKey);
  Optional<SystemNotification> findByDedupKey(String dedupKey);
  void deleteByDedupKey(String dedupKey);
  void deleteByDedupKeyIn(Collection<String> dedupKeys);

  @Query("select n.dedupKey from SystemNotification n where n.dedupKey is not null")
  List<String> findAllDedupKeys();

  @Query("select n from SystemNotification n where n.targetUserId is null or n.targetUserId = :userId order by n.createdAt desc")
  List<SystemNotification> findVisibleForUser(@Param("userId") UUID userId);

  @Query(value = "select n from SystemNotification n where n.targetUserId is null or n.targetUserId = :userId order by n.createdAt desc",
      countQuery = "select count(n) from SystemNotification n where n.targetUserId is null or n.targetUserId = :userId")
  Page<SystemNotification> findVisibleForUser(@Param("userId") UUID userId, Pageable pageable);

}
