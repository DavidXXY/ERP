package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemAuditLog;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID> {
  Page<SystemAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query("""
      select log from SystemAuditLog log
      where (:keyword = ''
        or lower(coalesce(log.username, '')) like lower(concat('%', :keyword, '%'))
        or lower(log.requestPath) like lower(concat('%', :keyword, '%'))
        or lower(coalesce(log.bizModule, '')) like lower(concat('%', :keyword, '%'))
        or lower(coalesce(log.bizObject, '')) like lower(concat('%', :keyword, '%')))
        and log.createdAt >= :startTime
        and log.createdAt < :endTime
      order by log.createdAt desc
      """)
  Page<SystemAuditLog> search(
      @Param("keyword") String keyword,
      @Param("startTime") OffsetDateTime startTime,
      @Param("endTime") OffsetDateTime endTime,
      Pageable pageable);
}
