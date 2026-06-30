package com.company.ops.api.modules.maintenance.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.maintenance.domain.EmployeeCertificate;
import com.company.ops.api.modules.maintenance.domain.EquipmentAsset;
import com.company.ops.api.modules.maintenance.domain.EquipmentStatus;
import com.company.ops.api.modules.maintenance.domain.FieldAttendance;
import com.company.ops.api.modules.maintenance.domain.FieldSchedule;
import com.company.ops.api.modules.maintenance.domain.MaintenancePlan;
import com.company.ops.api.modules.maintenance.domain.ScheduleStatus;
import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import com.company.ops.api.modules.maintenance.domain.WorkOrderMaterial;
import com.company.ops.api.modules.maintenance.domain.WorkOrderPriority;
import com.company.ops.api.modules.maintenance.domain.WorkOrderSource;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatusLog;
import com.company.ops.api.modules.maintenance.domain.WorkOrderType;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.AcceptWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.AssignWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.AttendanceResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CertificateResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CheckInRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CompleteWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateCertificateRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateEquipmentRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreatePlanRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateScheduleRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CreateWorkOrderRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.DashboardResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.CustomerOption;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.ContractOption;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.EmployeeOption;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.EquipmentResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.GeneratePlanRequest;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.GeneratePlanResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.MaterialResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.PartOption;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.PlanResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.ScheduleResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.ReferenceDataResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.StatusLogResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.WorkOrderResponse;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.UserOption;
import com.company.ops.api.modules.maintenance.repository.EmployeeCertificateRepository;
import com.company.ops.api.modules.maintenance.repository.EquipmentAssetRepository;
import com.company.ops.api.modules.maintenance.repository.FieldAttendanceRepository;
import com.company.ops.api.modules.maintenance.repository.FieldScheduleRepository;
import com.company.ops.api.modules.maintenance.repository.MaintenancePlanRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderMaterialRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderStatusLogRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceService {

  private final EquipmentAssetRepository assetRepository;
  private final MaintenancePlanRepository planRepository;
  private final WorkOrderRepository workOrderRepository;
  private final WorkOrderMaterialRepository materialRepository;
  private final WorkOrderStatusLogRepository statusLogRepository;
  private final EmployeeCertificateRepository certificateRepository;
  private final FieldScheduleRepository scheduleRepository;
  private final FieldAttendanceRepository attendanceRepository;
  private final CustomerRepository customerRepository;
  private final ServiceContractRepository contractRepository;
  private final SystemUserRepository userRepository;
  private final InventoryPartRepository partRepository;
  private final StockMovementRepository movementRepository;
  private final ReceivableRepository receivableRepository;
  private final LedgerService ledgerService;
  private final DataScopeService dataScopeService;

  public MaintenanceService(
      EquipmentAssetRepository assetRepository,
      MaintenancePlanRepository planRepository,
      WorkOrderRepository workOrderRepository,
      WorkOrderMaterialRepository materialRepository,
      WorkOrderStatusLogRepository statusLogRepository,
      EmployeeCertificateRepository certificateRepository,
      FieldScheduleRepository scheduleRepository,
      FieldAttendanceRepository attendanceRepository,
      CustomerRepository customerRepository,
      ServiceContractRepository contractRepository,
      SystemUserRepository userRepository,
      InventoryPartRepository partRepository,
      StockMovementRepository movementRepository,
      ReceivableRepository receivableRepository,
      LedgerService ledgerService,
      DataScopeService dataScopeService
  ) {
    this.assetRepository = assetRepository;
    this.planRepository = planRepository;
    this.workOrderRepository = workOrderRepository;
    this.materialRepository = materialRepository;
    this.statusLogRepository = statusLogRepository;
    this.certificateRepository = certificateRepository;
    this.scheduleRepository = scheduleRepository;
    this.attendanceRepository = attendanceRepository;
    this.customerRepository = customerRepository;
    this.contractRepository = contractRepository;
    this.userRepository = userRepository;
    this.partRepository = partRepository;
    this.movementRepository = movementRepository;
    this.receivableRepository = receivableRepository;
    this.ledgerService = ledgerService;
    this.dataScopeService = dataScopeService;
  }

  @Transactional(readOnly = true)
  public DashboardResponse dashboard() {
    LocalDate today = LocalDate.now();
    List<EquipmentAsset> assets = assetRepository.findAllByOrderByNextMaintenanceDateAsc();
    List<WorkOrder> orders = workOrderRepository.findAllByOrderByCreatedAtDesc();
    YearMonth month = YearMonth.from(today);
    BigDecimal monthCost = orders.stream()
        .filter(item -> item.getCompletedAt() != null && YearMonth.from(item.getCompletedAt()).equals(month))
        .map(WorkOrder::getCostAmount).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal monthBillable = orders.stream()
        .filter(item -> item.getAcceptedAt() != null && YearMonth.from(item.getAcceptedAt()).equals(month))
        .map(WorkOrder::getBillableAmount).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    long open = orders.stream().filter(item -> isOpen(item.getStatus())).count();
    long overdue = orders.stream().filter(item -> isOpen(item.getStatus()))
        .filter(item -> item.getPlannedDate() != null && item.getPlannedDate().isBefore(today)).count();
    long dueAssets = assets.stream().filter(item -> item.getNextMaintenanceDate() != null)
        .filter(item -> !item.getNextMaintenanceDate().isAfter(today)).count();
    long expiring = certificateRepository.findAllByOrderByExpiryDateAsc().stream()
        .filter(item -> !item.getExpiryDate().isBefore(today) && !item.getExpiryDate().isAfter(today.plusDays(30))).count();
    return new DashboardResponse(assets.size(), dueAssets, open, overdue, expiring, monthCost, monthBillable);
  }

  @Transactional(readOnly = true)
  public ReferenceDataResponse referenceData() {
    return new ReferenceDataResponse(
        customerRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(item -> new CustomerOption(item.getId(), item.getCode(), item.getName())).toList(),
        contractRepository.findAllByOrderByEndDateAsc().stream()
            .map(item -> new ContractOption(item.getId(), item.getCustomerId(), item.getCode(), item.getProjectName())).toList(),
        partRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(item -> new PartOption(item.getId(), item.getCode(), item.getName(), item.getModel(), item.getStockQty(), item.getUnitCost())).toList(),
        userRepository.findAll().stream().map(item -> new UserOption(item.getId(), item.getDisplayName(), item.isEnabled())).toList()
    );
  }

  @Transactional(readOnly = true)
  public List<EquipmentResponse> listEquipment() {
    List<EquipmentAsset> assets = assetRepository.findAllByOrderByNextMaintenanceDateAsc();
    List<WorkOrder> orders = workOrderRepository.findAllByOrderByCreatedAtDesc();
    Map<UUID, Customer> customers = customerMap(assets.stream().map(EquipmentAsset::getCustomerId).toList());
    Map<UUID, ServiceContract> contracts = contractMap(assets.stream().map(EquipmentAsset::getContractId).filter(id -> id != null).toList());
    Map<UUID, List<WorkOrder>> ordersByAsset = orders.stream().filter(item -> item.getEquipmentId() != null)
        .collect(Collectors.groupingBy(WorkOrder::getEquipmentId));
    return assets.stream().map(item -> {
      List<WorkOrder> history = ordersByAsset.getOrDefault(item.getId(), List.of());
      long faults = history.stream().filter(order -> order.getWorkType() == WorkOrderType.REPAIR).count();
      return toEquipmentResponse(item, customers.get(item.getCustomerId()), contracts.get(item.getContractId()), history.size(), faults);
    }).toList();
  }

  @Transactional
  public EquipmentResponse createEquipment(CreateEquipmentRequest request) {
    if (assetRepository.existsByCode(request.code())) throw new BusinessException("设备编号已存在");
    Customer customer = customerRepository.findById(request.customerId()).orElseThrow(() -> new BusinessException("客户不存在"));
    ServiceContract contract = validateContract(request.contractId(), customer.getId());
    EquipmentAsset asset = new EquipmentAsset();
    asset.setCustomerId(customer.getId());
    asset.setContractId(contract == null ? null : contract.getId());
    asset.setCode(request.code());
    asset.setName(request.name());
    asset.setCategory(request.category());
    asset.setModel(request.model());
    asset.setSerialNo(request.serialNo());
    asset.setSiteAddress(request.siteAddress());
    asset.setInstalledDate(request.installedDate());
    asset.setWarrantyEndDate(request.warrantyEndDate());
    asset.setMaintenanceCycleDays(request.maintenanceCycleDays());
    asset.setNextMaintenanceDate(request.nextMaintenanceDate());
    asset.setRequiredCertificate(request.requiredCertificate());
    asset.setNotes(request.notes());
    asset.setStatus(request.nextMaintenanceDate().isAfter(LocalDate.now()) ? EquipmentStatus.ACTIVE : EquipmentStatus.MAINTENANCE_DUE);
    EquipmentAsset saved = assetRepository.save(asset);
    if (request.createPlan()) {
      MaintenancePlan plan = new MaintenancePlan();
      plan.setCode("JH-" + request.code());
      plan.setAssetId(saved.getId());
      plan.setContractId(saved.getContractId());
      plan.setPlanName(saved.getName() + "定期服务");
      plan.setCycleDays(saved.getMaintenanceCycleDays());
      plan.setNextDueDate(saved.getNextMaintenanceDate());
      plan.setActive(true);
      planRepository.save(plan);
    }
    return toEquipmentResponse(saved, customer, contract, 0, 0);
  }

  @Transactional(readOnly = true)
  public List<PlanResponse> listPlans() {
    List<MaintenancePlan> plans = planRepository.findAllByOrderByNextDueDateAsc();
    Map<UUID, EquipmentAsset> assets = assetMap(plans.stream().map(MaintenancePlan::getAssetId).toList());
    Map<UUID, ServiceContract> contracts = contractMap(plans.stream().map(MaintenancePlan::getContractId).filter(id -> id != null).toList());
    return plans.stream().map(item -> toPlanResponse(item, assets.get(item.getAssetId()), contracts.get(item.getContractId()))).toList();
  }

  @Transactional
  public PlanResponse createPlan(CreatePlanRequest request) {
    if (planRepository.existsByCode(request.code())) throw new BusinessException("服务计划编号已存在");
    if (planRepository.existsByAssetIdAndActiveTrue(request.assetId())) throw new BusinessException("该设备已有生效中的服务计划");
    EquipmentAsset asset = assetRepository.findById(request.assetId()).orElseThrow(() -> new BusinessException("设备不存在"));
    MaintenancePlan plan = new MaintenancePlan();
    plan.setCode(request.code());
    plan.setAssetId(asset.getId());
    plan.setContractId(asset.getContractId());
    plan.setPlanName(request.planName());
    plan.setCycleDays(request.cycleDays());
    plan.setNextDueDate(request.nextDueDate());
    plan.setActive(true);
    MaintenancePlan saved = planRepository.save(plan);
    asset.setMaintenanceCycleDays(request.cycleDays());
    asset.setNextMaintenanceDate(request.nextDueDate());
    assetRepository.save(asset);
    ServiceContract contract = asset.getContractId() == null ? null : contractRepository.findById(asset.getContractId()).orElse(null);
    return toPlanResponse(saved, asset, contract);
  }

  @Transactional
  public GeneratePlanResponse generateDueWorkOrders(GeneratePlanRequest request) {
    List<MaintenancePlan> duePlans = planRepository.findByActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(request.throughDate());
    List<String> codes = new ArrayList<>();
    for (MaintenancePlan plan : duePlans) {
      EquipmentAsset asset = assetRepository.findById(plan.getAssetId()).orElse(null);
      if (asset == null || asset.getStatus() == EquipmentStatus.RETIRED) continue;
      String code = "GD-" + plan.getNextDueDate().toString().replace("-", "") + "-" + asset.getCode();
      if (!workOrderRepository.existsByCode(code)) {
        WorkOrder order = new WorkOrder();
        order.setCode(code);
        order.setSource(WorkOrderSource.MAINTENANCE_PLAN);
        order.setWorkType(WorkOrderType.INSPECTION);
        order.setPriority(WorkOrderPriority.NORMAL);
        order.setStatus(WorkOrderStatus.CREATED);
        order.setTitle(asset.getName() + "周期巡检");
        order.setCustomerId(asset.getCustomerId());
        order.setContractId(plan.getContractId());
        order.setEquipmentId(asset.getId());
        order.setMaintenancePlanId(plan.getId());
        order.setEquipmentName(asset.getName());
        order.setPlannedDate(plan.getNextDueDate());
        order.setSiteAddress(asset.getSiteAddress());
        order.setProblemDescription("按服务计划执行定期检查和保养");
        order.setRequiredCertificate(asset.getRequiredCertificate());
        order.setBillableAmount(BigDecimal.ZERO);
        order.setFreeWarranty(true);
        WorkOrder saved = workOrderRepository.save(order);
        log(saved, null, WorkOrderStatus.CREATED, request.operatorName(), "服务计划自动生成");
        codes.add(code);
      }
      plan.setLastGeneratedDate(plan.getNextDueDate());
      plan.setNextDueDate(plan.getNextDueDate().plusDays(plan.getCycleDays()));
      planRepository.save(plan);
      asset.setNextMaintenanceDate(plan.getNextDueDate());
      asset.setStatus(EquipmentStatus.ACTIVE);
      assetRepository.save(asset);
    }
    return new GeneratePlanResponse(codes.size(), codes);
  }

  @Transactional(readOnly = true)
  public List<WorkOrderResponse> listWorkOrders() {
    boolean includeUnassigned = dataScopeService.hasAuthority("maintenance:workorder:assign");
    return workOrderRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(item -> dataScopeService.canViewAssignee(item.getAssigneeId(), includeUnassigned))
        .map(this::toWorkOrderResponse).toList();
  }

  @Transactional(readOnly = true)
  public WorkOrderResponse getWorkOrder(UUID id) {
    WorkOrder item = workOrderRepository.findById(id).orElseThrow(() -> new BusinessException("工单不存在"));
    if (!dataScopeService.canViewAssignee(item.getAssigneeId(), dataScopeService.hasAuthority("maintenance:workorder:assign"))) throw new BusinessException("无权查看该工单");
    return toWorkOrderResponse(item);
  }

  @Transactional
  public WorkOrderResponse createWorkOrder(CreateWorkOrderRequest request) {
    if (workOrderRepository.existsByCode(request.code())) throw new BusinessException("工单编号已存在");
    Customer customer = customerRepository.findById(request.customerId()).orElseThrow(() -> new BusinessException("客户不存在"));
    ServiceContract contract = validateContract(request.contractId(), customer.getId());
    EquipmentAsset asset = null;
    if (request.equipmentId() != null) {
      asset = assetRepository.findById(request.equipmentId()).orElseThrow(() -> new BusinessException("设备不存在"));
      if (!asset.getCustomerId().equals(customer.getId())) throw new BusinessException("设备不属于所选客户");
    }
    WorkOrder order = new WorkOrder();
    order.setCode(request.code());
    order.setSource(request.source());
    order.setWorkType(request.workType());
    order.setPriority(request.priority());
    order.setStatus(WorkOrderStatus.CREATED);
    order.setTitle(request.title());
    order.setCustomerId(customer.getId());
    order.setContractId(contract == null ? null : contract.getId());
    order.setProjectId(request.projectId());
    order.setEquipmentId(asset == null ? null : asset.getId());
    order.setEquipmentName(asset == null ? null : asset.getName());
    order.setPlannedDate(request.plannedDate());
    order.setSiteAddress(request.siteAddress());
    order.setProblemDescription(request.problemDescription());
    order.setRequiredCertificate(asset == null ? null : asset.getRequiredCertificate());
    order.setBillableAmount(amount(request.billableAmount()));
    order.setFreeWarranty(request.freeWarranty());
    WorkOrder saved = workOrderRepository.save(order);
    log(saved, null, WorkOrderStatus.CREATED, request.operatorName(), "工单创建");
    return toWorkOrderResponse(saved);
  }

  @Transactional
  public WorkOrderResponse assignWorkOrder(UUID id, AssignWorkOrderRequest request) {
    WorkOrder order = lockOrder(id, WorkOrderStatus.CREATED);
    SystemUser user = userRepository.findById(request.assigneeId()).orElseThrow(() -> new BusinessException("工程师不存在"));
    if (!user.isEnabled()) throw new BusinessException("该工程师账号已停用");
    validateAssignment(user, order);
    WorkOrderStatus previous = order.getStatus();
    order.setAssigneeId(user.getId());
    order.setEngineerName(user.getDisplayName());
    order.setStatus(WorkOrderStatus.ASSIGNED);
    WorkOrder saved = workOrderRepository.save(order);
    log(saved, previous, WorkOrderStatus.ASSIGNED, request.operatorName(), "派工给" + user.getDisplayName());
    return toWorkOrderResponse(saved);
  }

  @Transactional
  public WorkOrderResponse checkIn(UUID id, CheckInRequest request) {
    WorkOrder order = lockOrder(id, WorkOrderStatus.ASSIGNED);
    OffsetDateTime now = OffsetDateTime.now();
    WorkOrderStatus previous = order.getStatus();
    order.setCheckInAt(now);
    order.setCheckInLocation(request.location());
    order.setStartedAt(now);
    order.setStatus(WorkOrderStatus.IN_PROGRESS);
    WorkOrder saved = workOrderRepository.save(order);
    FieldAttendance attendance = new FieldAttendance();
    attendance.setUserId(order.getAssigneeId());
    attendance.setWorkOrderId(order.getId());
    attendance.setCheckInAt(now);
    attendance.setCheckInLocation(request.location());
    attendanceRepository.save(attendance);
    log(saved, previous, WorkOrderStatus.IN_PROGRESS, request.operatorName(), "现场签到：" + request.location());
    return toWorkOrderResponse(saved);
  }

  @Transactional
  public WorkOrderResponse completeWorkOrder(UUID id, CompleteWorkOrderRequest request) {
    WorkOrder order = lockOrder(id, WorkOrderStatus.IN_PROGRESS);
    Set<UUID> uniquePartIds = new HashSet<>();
    request.materials().forEach(item -> {
      if (!uniquePartIds.add(item.partId())) throw new BusinessException("同一物料不能重复填写");
    });
    List<UUID> sortedPartIds = uniquePartIds.stream().sorted().toList();
    Map<UUID, InventoryPart> parts = sortedPartIds.stream().map(partRepository::findByIdForUpdate)
        .map(optional -> optional.orElseThrow(() -> new BusinessException("物料不存在")))
        .collect(Collectors.toMap(InventoryPart::getId, Function.identity()));
    List<WorkOrderMaterial> materials = request.materials().stream().map(item -> {
      InventoryPart part = parts.get(item.partId());
      if (part.getStockQty().compareTo(item.quantity()) < 0) throw new BusinessException(part.getName() + "库存不足，当前库存" + part.getStockQty());
      BigDecimal lineAmount = item.quantity().multiply(amount(part.getUnitCost()));
      WorkOrderMaterial line = new WorkOrderMaterial();
      line.setWorkOrderId(order.getId());
      line.setPartId(part.getId());
      line.setPartName(part.getName());
      line.setQuantity(item.quantity());
      line.setUnitCost(amount(part.getUnitCost()));
      line.setAmount(lineAmount);
      part.setStockQty(part.getStockQty().subtract(item.quantity()));
      StockMovement movement = new StockMovement();
      movement.setPartId(part.getId());
      movement.setMovementType(StockMovementType.OUTBOUND);
      movement.setQuantity(item.quantity());
      movement.setSourceNo(order.getCode());
      movement.setRemark("服务工单领料：" + order.getTitle());
      movementRepository.save(movement);
      return line;
    }).toList();
    materialRepository.saveAll(materials);
    partRepository.saveAll(parts.values());
    BigDecimal materialCost = materials.stream().map(WorkOrderMaterial::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    WorkOrderStatus previous = order.getStatus();
    order.setServiceResult(request.serviceResult());
    order.setLaborHours(request.laborHours());
    order.setLaborCost(request.laborCost());
    order.setMaterialCost(materialCost);
    order.setTravelCost(request.travelCost());
    order.setOutsourcingCost(request.outsourcingCost());
    order.setCostAmount(request.laborCost().add(materialCost).add(request.travelCost()).add(request.outsourcingCost()));
    order.setCompletedAt(OffsetDateTime.now());
    order.setStatus(WorkOrderStatus.COMPLETED);
    WorkOrder saved = workOrderRepository.save(order);
    attendanceRepository.findFirstByWorkOrderIdOrderByCheckInAtDesc(order.getId()).ifPresent(attendance -> {
      attendance.setCheckOutAt(OffsetDateTime.now());
      attendance.setCheckOutLocation(order.getSiteAddress());
      attendanceRepository.save(attendance);
    });
    if (order.getEquipmentId() != null) {
      assetRepository.findById(order.getEquipmentId()).ifPresent(asset -> {
        asset.setLastMaintenanceDate(LocalDate.now());
        asset.setNextMaintenanceDate(LocalDate.now().plusDays(asset.getMaintenanceCycleDays()));
        asset.setStatus(EquipmentStatus.ACTIVE);
        assetRepository.save(asset);
      });
    }
    log(saved, previous, WorkOrderStatus.COMPLETED, request.operatorName(), "现场作业完成");
    return toWorkOrderResponse(saved);
  }

  @Transactional
  public WorkOrderResponse acceptWorkOrder(UUID id, AcceptWorkOrderRequest request) {
    WorkOrder order = lockOrder(id, WorkOrderStatus.COMPLETED);
    WorkOrderStatus previous = order.getStatus();
    order.setCustomerSigner(request.customerSigner());
    order.setAcceptanceNote(request.acceptanceNote());
    order.setAcceptedAt(OffsetDateTime.now());
    order.setStatus(WorkOrderStatus.ACCEPTED);
    WorkOrder saved = workOrderRepository.save(order);
    if (!order.isFreeWarranty() && amount(order.getBillableAmount()).compareTo(BigDecimal.ZERO) > 0) {
      String receivableCode = "YS-" + order.getCode();
      if (!receivableRepository.existsByCode(receivableCode)) {
        Receivable receivable = new Receivable();
        receivable.setCode(receivableCode);
        receivable.setCustomerId(order.getCustomerId());
        receivable.setContractId(order.getContractId());
        receivable.setSourceNo(order.getCode());
        receivable.setAmount(order.getBillableAmount());
        receivable.setSettledAmount(BigDecimal.ZERO);
        receivable.setDueDate(LocalDate.now().plusDays(30));
        receivable.setStatus(ReceivableStatus.INVOICE_PENDING);
        receivableRepository.save(receivable);
        ledgerService.post("WORK_ORDER_REVENUE", order.getCode(), LocalDate.now(),
            "服务工单收入 " + order.getCode(), List.of(
                new PostingLine("1122", "应收账款", order.getBillableAmount(), BigDecimal.ZERO, order.getCode()),
                new PostingLine("6001", "服务收入", BigDecimal.ZERO, order.getBillableAmount(), order.getTitle())
            ));
      }
    }
    log(saved, previous, WorkOrderStatus.ACCEPTED, request.operatorName(), "客户验收：" + request.customerSigner());
    return toWorkOrderResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<EmployeeOption> eligibleEmployees(UUID workOrderId) {
    WorkOrder order = workOrderRepository.findById(workOrderId).orElseThrow(() -> new BusinessException("工单不存在"));
    return userRepository.findAll().stream().filter(SystemUser::isEnabled).map(user -> {
      List<String> validCertificates = certificateRepository.findByUserIdOrderByExpiryDateAsc(user.getId()).stream()
          .filter(cert -> !cert.getExpiryDate().isBefore(order.getPlannedDate()))
          .map(EmployeeCertificate::getCertificateType).distinct().toList();
      boolean available = scheduleRepository.findByUserIdAndWorkDate(user.getId(), order.getPlannedDate()).stream()
          .noneMatch(schedule -> schedule.getStatus() == ScheduleStatus.LEAVE);
      return new EmployeeOption(user.getId(), user.getDisplayName(), validCertificates, available);
    }).filter(option -> option.availableOnPlannedDate())
        .filter(option -> order.getRequiredCertificate() == null || order.getRequiredCertificate().isBlank()
            || option.validCertificates().contains(order.getRequiredCertificate()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CertificateResponse> listCertificates() {
    Map<UUID, SystemUser> users = userMap(certificateRepository.findAllByOrderByExpiryDateAsc().stream().map(EmployeeCertificate::getUserId).toList());
    return certificateRepository.findAllByOrderByExpiryDateAsc().stream().map(item -> toCertificateResponse(item, users.get(item.getUserId()))).toList();
  }

  @Transactional
  public CertificateResponse createCertificate(CreateCertificateRequest request) {
    if (certificateRepository.existsByCertificateNo(request.certificateNo())) throw new BusinessException("证书编号已存在");
    SystemUser user = userRepository.findById(request.userId()).orElseThrow(() -> new BusinessException("员工不存在"));
    EmployeeCertificate item = new EmployeeCertificate();
    item.setUserId(user.getId());
    item.setCertificateType(request.certificateType());
    item.setCertificateNo(request.certificateNo());
    item.setIssueDate(request.issueDate());
    item.setExpiryDate(request.expiryDate());
    item.setIssuingAuthority(request.issuingAuthority());
    return toCertificateResponse(certificateRepository.save(item), user);
  }

  @Transactional(readOnly = true)
  public List<ScheduleResponse> listSchedules() {
    List<FieldSchedule> schedules = scheduleRepository.findAllByOrderByWorkDateDesc();
    Map<UUID, SystemUser> users = userMap(schedules.stream().map(FieldSchedule::getUserId).toList());
    return schedules.stream().map(item -> toScheduleResponse(item, users.get(item.getUserId()))).toList();
  }

  @Transactional
  public ScheduleResponse createSchedule(CreateScheduleRequest request) {
    SystemUser user = userRepository.findById(request.userId()).orElseThrow(() -> new BusinessException("员工不存在"));
    FieldSchedule item = new FieldSchedule();
    item.setUserId(user.getId());
    item.setWorkDate(request.workDate());
    item.setShiftName(request.shiftName());
    item.setSiteName(request.siteName());
    item.setStatus(request.status());
    return toScheduleResponse(scheduleRepository.save(item), user);
  }

  @Transactional(readOnly = true)
  public List<AttendanceResponse> listAttendance() {
    List<FieldAttendance> attendance = attendanceRepository.findAllByOrderByCheckInAtDesc();
    Map<UUID, SystemUser> users = userMap(attendance.stream().map(FieldAttendance::getUserId).toList());
    Map<UUID, WorkOrder> orders = workOrderMap(attendance.stream().map(FieldAttendance::getWorkOrderId).toList());
    return attendance.stream().map(item -> new AttendanceResponse(item.getId(), item.getUserId(),
        name(users.get(item.getUserId())), item.getWorkOrderId(),
        orders.get(item.getWorkOrderId()) == null ? null : orders.get(item.getWorkOrderId()).getCode(),
        item.getCheckInAt(), item.getCheckOutAt(), item.getCheckInLocation(), item.getCheckOutLocation())).toList();
  }

  private void validateAssignment(SystemUser user, WorkOrder order) {
    if (order.getPlannedDate() == null) throw new BusinessException("工单缺少计划日期");
    boolean onLeave = scheduleRepository.findByUserIdAndWorkDate(user.getId(), order.getPlannedDate()).stream()
        .anyMatch(item -> item.getStatus() == ScheduleStatus.LEAVE);
    if (onLeave) throw new BusinessException("该工程师计划日期处于请假状态");
    if (order.getRequiredCertificate() != null && !order.getRequiredCertificate().isBlank()) {
      boolean qualified = certificateRepository.findByUserIdOrderByExpiryDateAsc(user.getId()).stream()
          .anyMatch(item -> item.getCertificateType().equals(order.getRequiredCertificate())
              && !item.getExpiryDate().isBefore(order.getPlannedDate()));
      if (!qualified) throw new BusinessException("该工程师缺少有效的" + order.getRequiredCertificate());
    }
  }

  private WorkOrder lockOrder(UUID id, WorkOrderStatus expected) {
    WorkOrder order = workOrderRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("工单不存在"));
    if (order.getStatus() != expected) throw new BusinessException("当前工单状态不能执行该操作");
    return order;
  }

  private void log(WorkOrder order, WorkOrderStatus from, WorkOrderStatus to, String operator, String remark) {
    WorkOrderStatusLog log = new WorkOrderStatusLog();
    log.setWorkOrderId(order.getId());
    log.setFromStatus(from);
    log.setToStatus(to);
    log.setOperatorName(operator);
    log.setRemark(remark);
    statusLogRepository.save(log);
  }

  private ServiceContract validateContract(UUID contractId, UUID customerId) {
    if (contractId == null) return null;
    ServiceContract contract = contractRepository.findById(contractId).orElseThrow(() -> new BusinessException("合同不存在"));
    if (!contract.getCustomerId().equals(customerId)) throw new BusinessException("合同不属于所选客户");
    return contract;
  }

  private EquipmentResponse toEquipmentResponse(EquipmentAsset item, Customer customer, ServiceContract contract, long orderCount, long faultCount) {
    EquipmentStatus status = item.getStatus();
    if (status == EquipmentStatus.ACTIVE && item.getNextMaintenanceDate() != null && !item.getNextMaintenanceDate().isAfter(LocalDate.now())) status = EquipmentStatus.MAINTENANCE_DUE;
    return new EquipmentResponse(item.getId(), item.getCustomerId(), name(customer), item.getContractId(),
        contract == null ? null : contract.getCode(), item.getCode(), item.getName(), item.getCategory(), item.getModel(),
        item.getSerialNo(), item.getSiteAddress(), item.getInstalledDate(), item.getWarrantyEndDate(),
        item.getMaintenanceCycleDays(), item.getLastMaintenanceDate(), item.getNextMaintenanceDate(), status,
        item.getRequiredCertificate(), item.getNotes(), orderCount, faultCount);
  }

  private PlanResponse toPlanResponse(MaintenancePlan item, EquipmentAsset asset, ServiceContract contract) {
    return new PlanResponse(item.getId(), item.getCode(), item.getAssetId(), asset == null ? null : asset.getCode(),
        asset == null ? null : asset.getName(), item.getContractId(), contract == null ? null : contract.getCode(),
        item.getPlanName(), item.getCycleDays(), item.getNextDueDate(), item.getLastGeneratedDate(), item.isActive(),
        item.isActive() && !item.getNextDueDate().isAfter(LocalDate.now()));
  }

  private WorkOrderResponse toWorkOrderResponse(WorkOrder item) {
    Customer customer = item.getCustomerId() == null ? null : customerRepository.findById(item.getCustomerId()).orElse(null);
    ServiceContract contract = item.getContractId() == null ? null : contractRepository.findById(item.getContractId()).orElse(null);
    EquipmentAsset asset = item.getEquipmentId() == null ? null : assetRepository.findById(item.getEquipmentId()).orElse(null);
    List<MaterialResponse> materials = materialRepository.findByWorkOrderIdOrderByCreatedAtAsc(item.getId()).stream()
        .map(line -> new MaterialResponse(line.getId(), line.getPartId(), line.getPartName(), line.getQuantity(), line.getUnitCost(), line.getAmount())).toList();
    List<StatusLogResponse> logs = statusLogRepository.findByWorkOrderIdOrderByCreatedAtAsc(item.getId()).stream()
        .map(log -> new StatusLogResponse(log.getId(), log.getFromStatus(), log.getToStatus(), log.getOperatorName(), log.getRemark(), log.getCreatedAt())).toList();
    return new WorkOrderResponse(item.getId(), item.getCode(), item.getSource(), item.getWorkType(), item.getPriority(),
        item.getStatus(), item.getTitle(), item.getCustomerId(), name(customer), item.getContractId(),
        contract == null ? null : contract.getCode(), item.getProjectId(), item.getEquipmentId(),
        asset == null ? null : asset.getCode(), item.getEquipmentName(), item.getPlannedDate(), item.getSiteAddress(),
        item.getProblemDescription(), item.getAssigneeId(), item.getEngineerName(), item.getRequiredCertificate(),
        item.getCheckInAt(), item.getCheckInLocation(), item.getStartedAt(), item.getCompletedAt(), item.getAcceptedAt(),
        item.getCustomerSigner(), item.getServiceResult(), item.getAcceptanceNote(), amount(item.getLaborHours()),
        amount(item.getLaborCost()), amount(item.getMaterialCost()), amount(item.getTravelCost()),
        amount(item.getOutsourcingCost()), amount(item.getCostAmount()), amount(item.getBillableAmount()),
        item.isFreeWarranty(), materials, logs);
  }

  private CertificateResponse toCertificateResponse(EmployeeCertificate item, SystemUser user) {
    LocalDate today = LocalDate.now();
    return new CertificateResponse(item.getId(), item.getUserId(), name(user), item.getCertificateType(),
        item.getCertificateNo(), item.getIssueDate(), item.getExpiryDate(), item.getIssuingAuthority(),
        item.getExpiryDate().isBefore(today), !item.getExpiryDate().isBefore(today) && !item.getExpiryDate().isAfter(today.plusDays(30)));
  }

  private ScheduleResponse toScheduleResponse(FieldSchedule item, SystemUser user) {
    return new ScheduleResponse(item.getId(), item.getUserId(), name(user), item.getWorkDate(), item.getShiftName(), item.getSiteName(), item.getStatus());
  }

  private boolean isOpen(WorkOrderStatus status) { return status != WorkOrderStatus.ACCEPTED && status != WorkOrderStatus.CANCELLED; }
  private BigDecimal amount(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
  private String name(Customer value) { return value == null ? null : value.getName(); }
  private String name(SystemUser value) { return value == null ? null : value.getDisplayName(); }
  private Map<UUID, Customer> customerMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : customerRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(Customer::getId, Function.identity())); }
  private Map<UUID, ServiceContract> contractMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : contractRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(ServiceContract::getId, Function.identity())); }
  private Map<UUID, EquipmentAsset> assetMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : assetRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(EquipmentAsset::getId, Function.identity())); }
  private Map<UUID, SystemUser> userMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : userRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(SystemUser::getId, Function.identity())); }
  private Map<UUID, WorkOrder> workOrderMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : workOrderRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(WorkOrder::getId, Function.identity())); }
}
