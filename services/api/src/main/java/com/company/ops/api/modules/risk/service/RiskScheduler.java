package com.company.ops.api.modules.risk.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
public class RiskScheduler {
  private final RiskCenterService riskCenterService;

  public RiskScheduler(RiskCenterService riskCenterService) {
    this.riskCenterService = riskCenterService;
  }

  @Scheduled(cron = "${ops.risk.snapshot-cron:0 35 1 * * *}")
  @SchedulerLock(name = "riskSnapshotDaily", lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
  public void snapshotDaily() {
    riskCenterService.snapshotToday();
  }

  @Scheduled(cron = "${ops.risk.escalation-cron:0 10 * * * *}")
  @SchedulerLock(name = "riskEscalationScan", lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
  public void scanEscalations() {
    riskCenterService.escalateOverdue();
  }
}
