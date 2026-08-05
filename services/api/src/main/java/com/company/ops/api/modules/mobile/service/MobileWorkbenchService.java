package com.company.ops.api.modules.mobile.service;

import com.company.ops.api.modules.maintenance.domain.WorkOrderPriority;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.WorkOrderResponse;
import com.company.ops.api.modules.maintenance.service.MaintenanceService;
import com.company.ops.api.modules.mobile.dto.MobileWorkbenchResponse;
import com.company.ops.api.modules.mobile.dto.MobileWorkbenchResponse.MobileCapabilities;
import com.company.ops.api.modules.mobile.dto.MobileWorkbenchResponse.MobileTodo;
import com.company.ops.api.modules.office.service.OfficeService;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalResponse;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
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
    var capabilities = capabilities(principal);
    List<ApprovalResponse> approvals = capabilities.approvals()
        ? officeService.listMyPendingApprovals()
        : List.of();
    List<WorkOrderResponse> allOrders = capabilities.workOrders()
        ? maintenanceService.listMobileWorkOrders(principal)
        : List.of();
    var activeOrders = allOrders.stream()
        .filter(item -> item.status() != WorkOrderStatus.ACCEPTED && item.status() != WorkOrderStatus.CANCELLED)
        .toList();
    var todos = approvals.stream().limit(12).map(item -> new MobileTodo(
        item.id(), "APPROVAL", item.title(), item.approvalType().name() + " · " + item.applicantName(),
        "HIGH", item.amount(), item.createdAt(), "/pages/approvals/detail?id=" + item.id()
    )).toList();
    return new MobileWorkbenchResponse(
        OffsetDateTime.now(), approvals.size(),
        capabilities.notifications() ? officeService.getUnreadNotificationCount() : 0,
        activeOrders.size(), activeOrders.stream().filter(item -> item.priority() == WorkOrderPriority.URGENT).count(),
        capabilities,
        todos, activeOrders.stream().limit(5).toList()
    );
  }

  private MobileCapabilities capabilities(UserPrincipal principal) {
    boolean admin = principal.roleCodes().contains("ADMIN");
    boolean workOrders = admin || hasAny(principal, "maintenance:view", "maintenance:order:manage");
    boolean notifications = admin || hasAny(principal, "office:notification:view");
    return new MobileCapabilities(
        true,
        notifications,
        workOrders,
        admin || hasAny(principal, "inventory:view"),
        true,
        admin || hasAny(principal, "office:expense:create"),
        admin || hasAny(principal, "office:travel:create"),
        workOrders
    );
  }

  private boolean hasAny(UserPrincipal principal, String... permissions) {
    return java.util.Arrays.stream(permissions).anyMatch(principal.permissions()::contains);
  }
}
