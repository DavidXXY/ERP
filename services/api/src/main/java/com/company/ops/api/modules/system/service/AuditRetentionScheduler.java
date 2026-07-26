package com.company.ops.api.modules.system.service;

import com.company.ops.api.modules.system.repository.SystemAuditLogRepository;
import java.time.OffsetDateTime;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuditRetentionScheduler {
  private final SystemAuditLogRepository repository;
  private final int retentionDays;
  private final int batchSize;
  private final int maxBatches;
  private final TransactionTemplate transactions;

  public AuditRetentionScheduler(SystemAuditLogRepository repository,
      PlatformTransactionManager transactionManager,
      @Value("${ops.audit.retention-days:365}") int retentionDays,
      @Value("${ops.audit.cleanup-batch-size:1000}") int batchSize,
      @Value("${ops.audit.cleanup-max-batches:20}") int maxBatches) {
    this.repository = repository;
    this.retentionDays = Math.max(30, retentionDays);
    this.batchSize = Math.max(100, Math.min(batchSize, 5000));
    this.maxBatches = Math.max(1, Math.min(maxBatches, 100));
    this.transactions = new TransactionTemplate(transactionManager);
  }

  @Scheduled(cron = "${ops.audit.cleanup-cron:0 45 2 * * *}")
  @SchedulerLock(name = "auditLogRetention", lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
  public int deleteExpired() {
    OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
    int deleted = 0;
    for (int batch = 0; batch < maxBatches; batch++) {
      Integer batchDeleted = transactions.execute(status -> {
        var ids = repository.findExpiredIds(cutoff, PageRequest.of(0, batchSize));
        if (!ids.isEmpty()) repository.deleteAllByIdInBatch(ids);
        return ids.size();
      });
      int count = batchDeleted == null ? 0 : batchDeleted;
      deleted += count;
      if (count < batchSize) break;
    }
    return deleted;
  }
}
