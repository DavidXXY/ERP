package com.company.ops.api.modules.risk.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RiskScheduler {
  private final RiskCenterService riskCenterService;

  public RiskScheduler(RiskCenterService riskCenterService) {
    this.riskCenterService = riskCenterService;
  }

  @Scheduled(cron = "${ops.risk.snapshot-cron:0 35 1 * * *}")
  public int snapshotDaily() {
    return riskCenterService.snapshotToday();
  }

  @Scheduled(cron = "${ops.risk.escalation-cron:0 10 * * * *}")
  public int scanEscalations() {
    return riskCenterService.escalateOverdue();
  }
}
