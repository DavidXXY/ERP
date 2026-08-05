package com.company.ops.api.modules.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.config.TenantConfig;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.maintenance.domain.WorkOrderPriority;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.domain.WorkOrderType;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.AssignWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.AcceptWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CheckInRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CompleteWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateCertificateRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateEquipmentRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreatePlanRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateScheduleRequest;
import com.company.ops.api.modules.maintenance.repository.WorkOrderRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:maintenance_flow;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@Import({MaintenanceService.class, CodeGenerator.class, TenantConfig.class})
class MaintenanceRealFlowIntegrationTest {
  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackages = {
      "com.company.ops.api.modules.maintenance.domain",
      "com.company.ops.api.modules.crm.domain",
      "com.company.ops.api.modules.system.domain"
  })
  @EnableJpaRepositories(basePackages = {
      "com.company.ops.api.modules.maintenance.repository",
      "com.company.ops.api.modules.crm.repository",
      "com.company.ops.api.modules.system.repository"
  })
  static class TestApplication {}

  @MockBean private DeleteGovernanceService deleteGovernanceService;
  @MockBean private FileStorageService fileStorageService;
  @Autowired private MaintenanceService service;
  @Autowired private CustomerRepository customers;
  @Autowired private ReceivableRepository receivables;
  @Autowired private SystemUserRepository users;
  @Autowired private WorkOrderRepository workOrders;

  @Test
  void generatesPlanAndEnforcesCertificateBeforeSchedulingAndAttendance() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    Customer customer = new Customer();
    customer.setCode("FLOW-" + suffix);
    customer.setName("真实流程客户");
    customer.setIndustry("工程服务");
    customer.setLevel(CustomerLevel.NORMAL);
    customer.setOwnerName("测试负责人");
    customer = customers.save(customer);

    SystemUser engineer = new SystemUser();
    engineer.setUsername("engineer-" + suffix);
    engineer.setDisplayName("现场工程师");
    engineer.setEnabled(true);
    engineer = users.save(engineer);

    var equipment = service.createEquipment(new CreateEquipmentRequest(
        customer.getId(), null, null, "配电设备", "电气", "M-1", "SN-" + suffix,
        "上海测试现场", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
        30, LocalDate.now(), "低压电工证", "集成测试"));
    var plan = service.createPlan(new CreatePlanRequest(
        equipment.id(), "月度巡检", "检查运行状态", WorkOrderType.INSPECTION,
        WorkOrderPriority.NORMAL, 30, true, LocalDate.now()));

    assertThat(service.generatePlans(plan.id()).generated()).isEqualTo(1);
    var order = workOrders.findAllByOrderByCreatedAtDesc().stream()
        .filter(item -> plan.id().equals(item.getMaintenancePlanId()) && equipment.id().equals(item.getEquipmentId()))
        .findFirst().orElseThrow();

    UUID engineerId = engineer.getId();
    assertThatThrownBy(() -> service.assign(order.getId(), new AssignWorkOrderRequest(engineerId, "现场工程师")))
        .isInstanceOf(BusinessException.class).hasMessageContaining("低压电工证");

    service.createCertificate(new CreateCertificateRequest(
        engineerId, "低压电工证", "CERT-" + suffix, LocalDate.now().minusMonths(1),
        LocalDate.now().plusYears(1), "应急管理部门", "有效"));
    var assigned = service.assign(order.getId(), new AssignWorkOrderRequest(engineerId, "现场工程师"));
    assertThat(assigned.status()).isEqualTo(WorkOrderStatus.ASSIGNED);

    OffsetDateTime scheduledAt = OffsetDateTime.of(LocalDate.now().plusDays(1), java.time.LocalTime.of(9, 0), ZoneOffset.ofHours(8));
    var schedule = service.createSchedule(new CreateScheduleRequest(order.getId(), engineerId, scheduledAt));
    assertThat(schedule.scheduledAt()).isEqualTo(scheduledAt);
    service.checkIn(order.getId(), new CheckInRequest(OffsetDateTime.now(), "上海测试现场"));

    assertThat(service.listSchedules()).anyMatch(item -> item.orderId().equals(order.getId())
        && "现场工程师".equals(item.engineerName()) && item.scheduledAt().isEqual(scheduledAt));
    assertThat(service.listAttendance()).anyMatch(item -> item.orderId().equals(order.getId()) && "上海测试现场".equals(item.checkInLocation()));

    service.complete(order.getId(), new CompleteWorkOrderRequest(
        null, null, null, null, null, null, new java.math.BigDecimal("800"), "巡检完成", "设备运行正常"));
    assertThat(service.listAttendance()).anyMatch(item -> item.orderId().equals(order.getId())
        && item.checkOutAt() != null);
    service.accept(order.getId(), new AcceptWorkOrderRequest(new java.math.BigDecimal("500"), "客户确认完成"));
    assertThat(receivables.existsBySourceNo(order.getCode())).isTrue();
    assertThatThrownBy(() -> service.assign(order.getId(), new AssignWorkOrderRequest(engineerId, "现场工程师")))
        .isInstanceOf(BusinessException.class).hasMessageContaining("待指派或待接单");
  }
}
