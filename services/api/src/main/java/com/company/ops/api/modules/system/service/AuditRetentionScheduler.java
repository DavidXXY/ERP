package com.company.ops.api.modules.system.service;

import com.company.ops.api.modules.system.repository.SystemAuditLogRepository;
import java.time.OffsetDateTime;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditRetentionScheduler {
  private final SystemAuditLogRepository repository;
  private final int retentionDays;

  public AuditRetentionScheduler(SystemAuditLogRepository repository,
      @Value("${ops.audit.retention-days:365}") int retentionDays) {
    this.repository = repository;
    this.retentionDays = Math.max(30, retentionDays);
  }

  @Scheduled(cron = "${ops.audit.cleanup-cron:0 45 2 * * *}")
  @SchedulerLock(name = "auditLogRetention", lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
  @Transactional
  public int deleteExpired() {
    return repository.deleteBefore(OffsetDateTime.now().minusDays(retentionDays));
  }
}
