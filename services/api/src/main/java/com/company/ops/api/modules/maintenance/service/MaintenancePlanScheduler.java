package com.company.ops.api.modules.maintenance.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaintenancePlanScheduler {
  private final MaintenanceService service;
  public MaintenancePlanScheduler(MaintenanceService service) { this.service = service; }

  @Scheduled(cron = "${ops.maintenance.plan-generation-cron:0 5 1 * * *}")
  @SchedulerLock(name = "maintenancePlanGeneration", lockAtLeastFor = "PT1M", lockAtMostFor = "PT15M")
  public void generateDueWorkOrders() {
    service.generatePlans(null);
  }
}
