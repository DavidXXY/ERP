package com.company.ops.api.modules.mobile.service;

import com.company.ops.api.modules.maintenance.domain.WorkOrderPriority;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.service.MaintenanceService;
import com.company.ops.api.modules.mobile.dto.MobileWorkbenchResponse;
import com.company.ops.api.modules.mobile.dto.MobileWorkbenchResponse.MobileTodo;
import com.company.ops.api.modules.office.service.OfficeService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MobileWorkbenchService {
  private final MaintenanceService maintenanceService;
  private final OfficeService officeService;

  public MobileWorkbenchService(MaintenanceService maintenanceService, OfficeService officeService) {
    this.maintenanceService = maintenanceService;
    this.officeService = officeService;
  }

  @Transactional(readOnly = true)
  public MobileWorkbenchResponse workbench(UserPrincipal principal) {
    var approvals = officeService.listMyPendingApprovals();
    var allOrders = maintenanceService.listMobileWorkOrders(principal);
    var activeOrders = allOrders.stream()
        .filter(item -> item.status() != WorkOrderStatus.ACCEPTED && item.status() != WorkOrderStatus.CANCELLED)
        .toList();
    var todos = approvals.stream().limit(12).map(item -> new MobileTodo(
        item.id(), "APPROVAL", item.title(), item.approvalType().name() + " · " + item.applicantName(),
        "HIGH", item.amount(), item.createdAt(), "/pages/approvals/detail?id=" + item.id()
    )).toList();
    return new MobileWorkbenchResponse(
        OffsetDateTime.now(), approvals.size(), officeService.getUnreadNotificationCount(), activeOrders.size(),
        activeOrders.stream().filter(item -> item.priority() == WorkOrderPriority.URGENT).count(),
        todos, activeOrders.stream().limit(5).toList()
    );
  }
}
