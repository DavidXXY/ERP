package com.company.ops.api.modules.mobile.dto;

import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.WorkOrderResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MobileWorkbenchResponse(
    OffsetDateTime generatedAt,
    long pendingApprovals,
    long unreadNotifications,
    long activeWorkOrders,
    long urgentWorkOrders,
    MobileCapabilities capabilities,
    List<MobileTodo> todos,
    List<WorkOrderResponse> workOrders
) {
  public record MobileCapabilities(
      boolean approvals,
      boolean notifications,
      boolean workOrders,
      boolean spares,
      boolean leaveApplication,
      boolean expenseApplication,
      boolean travelApplication,
      boolean offlineOperations
  ) {}

  public record MobileTodo(
      UUID id, String type, String title, String subtitle, String priority,
      BigDecimal amount, OffsetDateTime createdAt, String route
  ) {}
}
