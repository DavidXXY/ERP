package com.company.ops.api.modules.mobile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.maintenance.service.MaintenanceService;
import com.company.ops.api.modules.office.service.OfficeService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobileWorkbenchServiceTest {

  @Mock private MaintenanceService maintenanceService;
  @Mock private OfficeService officeService;
  @Mock private UserPrincipal principal;

  @InjectMocks private MobileWorkbenchService service;

  @Test
  void workbenchDoesNotQueryUnauthorizedNotificationOrWorkOrderData() {
    when(principal.roleCodes()).thenReturn(List.of("EMPLOYEE"));
    when(principal.permissions()).thenReturn(List.of());
    when(officeService.listMyPendingApprovals()).thenReturn(List.of());

    var result = service.workbench(principal);

    assertThat(result.unreadNotifications()).isZero();
    assertThat(result.activeWorkOrders()).isZero();
    assertThat(result.capabilities().notifications()).isFalse();
    assertThat(result.capabilities().workOrders()).isFalse();
    verify(officeService, never()).getUnreadNotificationCount();
    verify(maintenanceService, never()).listMobileWorkOrders(principal);
  }
}
