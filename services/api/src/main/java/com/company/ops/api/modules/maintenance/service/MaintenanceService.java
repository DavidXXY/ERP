package com.company.ops.api.modules.maintenance.service;

import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.maintenance.domain.EquipmentAsset;
import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import com.company.ops.api.modules.maintenance.domain.WorkOrderPriority;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.*;
import com.company.ops.api.modules.maintenance.repository.EquipmentAssetRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderStatusLogRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceService {

  private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

  private final WorkOrderRepository workOrderRepository;
  private final EquipmentAssetRepository equipmentRepository;
  private final WorkOrderStatusLogRepository statusLogRepository;
  private final CustomerRepository customerRepository;
  private final ServiceContractRepository contractRepository;
  private final CodeGenerator codeGenerator;

  public MaintenanceService(
      WorkOrderRepository workOrderRepository,
      EquipmentAssetRepository equipmentRepository,
      WorkOrderStatusLogRepository statusLogRepository,
      CustomerRepository customerRepository,
      ServiceContractRepository contractRepository,
      CodeGenerator codeGenerator) {
    this.workOrderRepository = workOrderRepository;
    this.equipmentRepository = equipmentRepository;
    this.statusLogRepository = statusLogRepository;
    this.customerRepository = customerRepository;
    this.contractRepository = contractRepository;
    this.codeGenerator = codeGenerator;
  }

  @Transactional(readOnly = true)
  public DashboardResponse dashboard() {
    List<WorkOrder> orders = workOrderRepository.findAllByOrderByCreatedAtDesc();
    long open = countOpen(orders);
    long closed = countClosed(orders);
    long urgent = orders.stream()
        .filter(o -> o.getPriority() == WorkOrderPriority.URGENT && isOpen(o)).count();
    return new DashboardResponse(open, closed, urgent, equipmentRepository.count());
  }

  @Transactional(readOnly = true)
  public ReferenceDataResponse references() {
    List<CustomerOption> customers = customerRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(c -> new CustomerOption(c.getId(), c.getName())).toList();
    List<EquipmentOption> equipment = equipmentRepository.findAllByOrderByNextMaintenanceDateAsc().stream()
        .map(a -> new EquipmentOption(a.getId(), a.getCode(), a.getName())).toList();
    List<ContractOption> contracts = contractRepository.findAllByOrderByEndDateAsc().stream()
        .map(c -> new ContractOption(c.getId(), c.getProjectName())).toList();
    return new ReferenceDataResponse(customers, equipment, contracts);
  }

  @Transactional(readOnly = true)
  public List<WorkOrderResponse> listWorkOrders() {
    return workOrderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public WorkOrderResponse getWorkOrder(UUID id) {
    WorkOrder o = workOrderRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("工单不存在"));
    return toResponse(o);
  }

  @Transactional
  public WorkOrderResponse createWorkOrder(CreateWorkOrderRequest r) {
    WorkOrder o = new WorkOrder();
    o.setCode(codeGenerator.generate("WORK_ORDER"));
    o.setTitle(r.title());
    o.setProblemDescription(r.description());
    o.setCustomerId(r.customerId());
    o.setEquipmentId(r.equipmentId());
    if (r.equipmentId() != null) {
      equipmentRepository.findById(r.equipmentId()).ifPresent(asset -> {
        o.setEquipmentName(asset.getName());
        o.setPlannedDate(asset.getNextMaintenanceDate());
        o.setContractId(asset.getContractId());
      });
    }
    o.setWorkType(r.workType());
    o.setPriority(r.priority());
    o.setSource(r.source());
    o.setStatus(WorkOrderStatus.CREATED);
    return toResponse(workOrderRepository.save(o));
  }

  @Transactional
  public WorkOrderResponse assign(UUID id, AssignWorkOrderRequest r) {
    WorkOrder o = getOrder(id);
    o.setAssigneeId(r.assigneeId());
    o.setEngineerName(r.assigneeName());
    o.setStatus(WorkOrderStatus.ASSIGNED);
    return toResponse(workOrderRepository.save(o));
  }

  @Transactional
  public WorkOrderResponse checkIn(UUID id, CheckInRequest r) {
    WorkOrder o = getOrder(id);
    o.setCheckInAt(r.checkInAt());
    o.setCheckInLocation(r.checkInLocation());
    o.setStartedAt(OffsetDateTime.now());
    o.setStatus(WorkOrderStatus.IN_PROGRESS);
    return toResponse(workOrderRepository.save(o));
  }

  @Transactional
  public WorkOrderResponse complete(UUID id, CompleteWorkOrderRequest r) {
    WorkOrder o = getOrder(id);
    o.setLaborHours(nvl(r.laborHours()));
    o.setLaborCost(nvl(r.laborCost()));
    o.setMaterialCost(nvl(r.materialCost()));
    o.setTravelCost(nvl(r.travelCost()));
    o.setOutsourcingCost(nvl(r.outsourcingCost()));
    o.setCostAmount(nvl(r.costAmount()));
    o.setBillableAmount(nvl(r.billableAmount()));
    o.setServiceResult(r.serviceResult());
    o.setAcceptanceNote(r.remarks());
    o.setStatus(WorkOrderStatus.COMPLETED);
    return toResponse(workOrderRepository.save(o));
  }

  @Transactional
  public WorkOrderResponse accept(UUID id, AcceptWorkOrderRequest r) {
    WorkOrder o = getOrder(id);
    o.setCostAmount(nvl(r.actualCost()));
    o.setAcceptanceNote(r.remarks());
    o.setStatus(WorkOrderStatus.ACCEPTED);
    o.setCompletedAt(OffsetDateTime.now());
    return toResponse(workOrderRepository.save(o));
  }

  @Transactional
  public void deleteWorkOrder(UUID id) {
    workOrderRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<EquipmentResponse> listEquipment() {
    return equipmentRepository.findAllByOrderByNextMaintenanceDateAsc().stream()
        .map(this::toEquipResponse).toList();
  }

  // -- Stubs --
  public List<PlanResponse> listPlans() { return List.of(); }
  public List<CertificateResponse> listCertificates() { return List.of(); }
  public List<ScheduleResponse> listSchedules() { return List.of(); }
  public List<AttendanceResponse> listAttendance() { return List.of(); }

  // ── internal ──

  private WorkOrder getOrder(UUID id) {
    return workOrderRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("工单不存在"));
  }

  private WorkOrderResponse toResponse(WorkOrder o) {
    String cn = o.getCustomerId() == null ? null :
        customerRepository.findById(o.getCustomerId()).map(Customer::getName).orElse(null);
    String ec = null, en = null;
    if (o.getEquipmentId() != null) {
      EquipmentAsset a = equipmentRepository.findById(o.getEquipmentId()).orElse(null);
      if (a != null) { ec = a.getCode(); en = a.getName(); }
    }
    var logs = statusLogRepository.findByWorkOrderIdOrderByCreatedAtAsc(o.getId()).stream()
        .map(l -> new StatusLogResponse(l.getId(), l.getFromStatus(), l.getToStatus(),
            l.getOperatorName(), l.getRemark(), l.getCreatedAt()))
        .toList();
    return new WorkOrderResponse(
        o.getId(), o.getCode(), o.getTitle(), o.getProblemDescription(),
        o.getCustomerId(), cn, o.getEquipmentId(), ec, en,
        o.getWorkType(), o.getPriority(), o.getSource(), o.getStatus(),
        o.getAssigneeId(), o.getEngineerName(),
        o.getLaborHours(), o.getLaborCost(), o.getCostAmount(),
        o.getBillableAmount(), o.getCostAmount(),
        o.getPlannedDate(),
        o.getStartedAt(), o.getCompletedAt(), o.getAcceptedAt(),
        o.getCreatedAt(), o.getUpdatedAt(), o.getAcceptanceNote(), logs);
  }

  private EquipmentResponse toEquipResponse(EquipmentAsset a) {
    String cn = a.getCustomerId() == null ? null :
        customerRepository.findById(a.getCustomerId()).map(Customer::getName).orElse(null);
    long cnt = workOrderRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(o -> a.getId().equals(o.getEquipmentId())).count();
    return new EquipmentResponse(
        a.getId(), a.getCode(), a.getName(), a.getCustomerId(), cn,
        a.getCategory(), a.getModel(), a.getSerialNo(),
        a.getSiteAddress(), a.getInstalledDate(),
        a.getWarrantyEndDate(), a.getMaintenanceCycleDays(),
        a.getLastMaintenanceDate(), a.getNextMaintenanceDate(),
        a.getStatus(), cnt);
  }

  private BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

  private boolean isOpen(WorkOrder o) {
    return o.getStatus() != WorkOrderStatus.ACCEPTED
        && o.getStatus() != WorkOrderStatus.CANCELLED;
  }

  private long countOpen(List<WorkOrder> orders) {
    return orders.stream().filter(this::isOpen).count();
  }

  private long countClosed(List<WorkOrder> orders) {
    return orders.stream().filter(o -> o.getStatus() == WorkOrderStatus.ACCEPTED).count();
  }
}
