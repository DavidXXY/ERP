package com.company.ops.api.modules.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import com.company.ops.api.modules.maintenance.domain.WorkOrderPriority;
import com.company.ops.api.modules.maintenance.domain.WorkOrderSource;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.domain.WorkOrderType;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.MobileOperationRequest;
import com.company.ops.api.modules.maintenance.repository.EquipmentAssetRepository;
import com.company.ops.api.modules.maintenance.repository.EmployeeCertificateRepository;
import com.company.ops.api.modules.maintenance.repository.FieldAttendanceRepository;
import com.company.ops.api.modules.maintenance.repository.FieldScheduleRepository;
import com.company.ops.api.modules.maintenance.repository.MaintenancePlanRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderAttachmentRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderMaterialRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderMobileOperationRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderStatusLogRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MaintenanceServiceMobileTest {
  @Test
  void assignedEngineerCanAcceptWorkOrderIdempotently() {
    WorkOrderRepository orders = mock(WorkOrderRepository.class);
    WorkOrderStatusLogRepository logs = mock(WorkOrderStatusLogRepository.class);
    WorkOrderAttachmentRepository attachments = mock(WorkOrderAttachmentRepository.class);
    WorkOrderMaterialRepository materials = mock(WorkOrderMaterialRepository.class);
    WorkOrderMobileOperationRepository operations = mock(WorkOrderMobileOperationRepository.class);
    DeleteGovernanceService deletion = mock(DeleteGovernanceService.class);
    UUID userId = UUID.randomUUID();
    WorkOrder order = order(userId);
    when(orders.findById(order.getId())).thenReturn(Optional.of(order));
    when(orders.save(order)).thenReturn(order);
    when(logs.findByWorkOrderIdOrderByCreatedAtAsc(order.getId())).thenReturn(List.of());
    when(attachments.findByWorkOrderIdOrderByCreatedAtAsc(order.getId())).thenReturn(List.of());
    when(materials.findByWorkOrderIdOrderByCreatedAtAsc(order.getId())).thenReturn(List.of());
    when(deletion.isHidden("WORK_ORDER", order.getId())).thenReturn(false);
    when(operations.existsByOperationId("accept-1")).thenReturn(false);
    MaintenanceService service = new MaintenanceService(
        orders, mock(EquipmentAssetRepository.class), mock(MaintenancePlanRepository.class),
        mock(EmployeeCertificateRepository.class), mock(FieldScheduleRepository.class),
        mock(FieldAttendanceRepository.class), logs, attachments, materials, operations,
        mock(CustomerRepository.class), mock(ServiceContractRepository.class), mock(CodeGenerator.class),
        deletion, mock(FileStorageService.class), mock(SystemUserRepository.class)
    );

    var response = service.acceptAssignment(order.getId(), new MobileOperationRequest("accept-1"), principal(userId));

    assertThat(response.assignmentAcceptedAt()).isNotNull();
    assertThat(response.status()).isEqualTo(WorkOrderStatus.ASSIGNED);
    verify(operations).save(any());
    verify(logs).save(any());
  }

  private WorkOrder order(UUID assigneeId) {
    WorkOrder order = new WorkOrder();
    order.setId(UUID.randomUUID());
    order.setTenantId("default");
    order.setCode("WO-MOBILE-1");
    order.setTitle("移动端工单");
    order.setSource(WorkOrderSource.MANUAL);
    order.setWorkType(WorkOrderType.REPAIR);
    order.setPriority(WorkOrderPriority.NORMAL);
    order.setStatus(WorkOrderStatus.ASSIGNED);
    order.setAssigneeId(assigneeId);
    order.setEngineerName("现场工程师");
    return order;
  }

  private UserPrincipal principal(UUID id) {
    SystemUser user = new SystemUser();
    user.setId(id);
    user.setTenantId("default");
    user.setUsername("engineer");
    user.setDisplayName("现场工程师");
    user.setEnabled(true);
    return new UserPrincipal(user);
  }
}
