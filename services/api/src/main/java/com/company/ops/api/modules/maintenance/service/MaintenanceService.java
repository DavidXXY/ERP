package com.company.ops.api.modules.maintenance.service;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.maintenance.domain.EquipmentAsset;
import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import com.company.ops.api.modules.maintenance.domain.WorkOrderAttachment;
import com.company.ops.api.modules.maintenance.domain.WorkOrderAttachmentCategory;
import com.company.ops.api.modules.maintenance.domain.WorkOrderMaterial;
import com.company.ops.api.modules.maintenance.domain.WorkOrderMobileOperation;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatusLog;
import com.company.ops.api.modules.maintenance.domain.WorkOrderPriority;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.dto.MaintenanceDtos.*;
import com.company.ops.api.modules.maintenance.repository.EquipmentAssetRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderAttachmentRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderMaterialRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderMobileOperationRepository;
import com.company.ops.api.modules.maintenance.repository.WorkOrderStatusLogRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MaintenanceService {

  private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);
  private static final FileStorageService.FilePolicy MOBILE_IMAGE_POLICY = new FileStorageService.FilePolicy(
      15L * 1024 * 1024,
      Set.of(".jpg", ".jpeg", ".png", ".webp"),
      "现场图片不能超过15MB",
      "仅支持 JPG、PNG、WEBP 图片",
      true
  );

  private final WorkOrderRepository workOrderRepository;
  private final EquipmentAssetRepository equipmentRepository;
  private final WorkOrderStatusLogRepository statusLogRepository;
  private final WorkOrderAttachmentRepository attachmentRepository;
  private final WorkOrderMaterialRepository materialRepository;
  private final WorkOrderMobileOperationRepository mobileOperationRepository;
  private final CustomerRepository customerRepository;
  private final ServiceContractRepository contractRepository;
  private final CodeGenerator codeGenerator;
  private final DeleteGovernanceService deleteGovernanceService;
  private final FileStorageService storageService;
  private final SystemUserRepository userRepository;

  public MaintenanceService(
      WorkOrderRepository workOrderRepository,
      EquipmentAssetRepository equipmentRepository,
      WorkOrderStatusLogRepository statusLogRepository,
      WorkOrderAttachmentRepository attachmentRepository,
      WorkOrderMaterialRepository materialRepository,
      WorkOrderMobileOperationRepository mobileOperationRepository,
      CustomerRepository customerRepository,
      ServiceContractRepository contractRepository,
      CodeGenerator codeGenerator,
      DeleteGovernanceService deleteGovernanceService,
      FileStorageService storageService,
      SystemUserRepository userRepository) {
    this.workOrderRepository = workOrderRepository;
    this.equipmentRepository = equipmentRepository;
    this.statusLogRepository = statusLogRepository;
    this.attachmentRepository = attachmentRepository;
    this.materialRepository = materialRepository;
    this.mobileOperationRepository = mobileOperationRepository;
    this.customerRepository = customerRepository;
    this.contractRepository = contractRepository;
    this.codeGenerator = codeGenerator;
    this.deleteGovernanceService = deleteGovernanceService;
    this.storageService = storageService;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public DashboardResponse dashboard() {
    List<WorkOrder> orders = deleteGovernanceService.visible("WORK_ORDER", workOrderRepository.findAllByOrderByCreatedAtDesc(), WorkOrder::getId);
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
    return deleteGovernanceService.visible("WORK_ORDER", workOrderRepository.findAllByOrderByCreatedAtDesc(), WorkOrder::getId)
        .stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public WorkOrderResponse getWorkOrder(UUID id) {
    WorkOrder o = workOrderRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("工单不存在"));
    if (deleteGovernanceService.isHidden("WORK_ORDER", id)) throw new NoSuchElementException("工单不存在");
    return toResponse(o);
  }

  @Transactional(readOnly = true)
  public List<WorkOrderResponse> listMobileWorkOrders(UserPrincipal principal) {
    return deleteGovernanceService.visible("WORK_ORDER", workOrderRepository.findAllByOrderByCreatedAtDesc(), WorkOrder::getId)
        .stream()
        .filter(order -> canAccessMobile(order, principal))
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AssigneeOption> mobileAssignees() {
    return userRepository.findByEnabledTrueOrderByDisplayNameAsc().stream()
        .map(user -> new AssigneeOption(user.getId(), user.getDisplayName()))
        .toList();
  }

  @Transactional(readOnly = true)
  public WorkOrderResponse getMobileWorkOrder(UUID id, UserPrincipal principal) {
    WorkOrder order = requireMobileOrder(id, principal);
    return toResponse(order);
  }

  @Transactional
  public WorkOrderResponse acceptAssignment(UUID id, MobileOperationRequest request, UserPrincipal principal) {
    WorkOrder order = requireMobileOrder(id, principal);
    if (mobileOperationRepository.existsByOperationId(request.operationId())) return toResponse(order);
    if (order.getStatus() != WorkOrderStatus.ASSIGNED) throw new BusinessException("仅待接单工单可以接单");
    if (order.getAssignmentAcceptedAt() == null) {
      order.setAssignmentAcceptedAt(OffsetDateTime.now());
      workOrderRepository.save(order);
      addStatusLog(order, order.getStatus(), "移动端确认接单", principal.displayName());
    }
    recordOperation(order, request.operationId(), "ACCEPT_ASSIGNMENT", principal);
    return toResponse(order);
  }

  @Transactional
  public WorkOrderResponse mobileCheckIn(UUID id, MobileCheckInRequest request, UserPrincipal principal) {
    WorkOrder order = requireMobileOrder(id, principal);
    if (mobileOperationRepository.existsByOperationId(request.operationId())) return toResponse(order);
    if (order.getStatus() != WorkOrderStatus.ASSIGNED) throw new BusinessException("当前工单状态不允许签到");
    if (order.getAssignmentAcceptedAt() == null) throw new BusinessException("请先接单再进行现场签到");
    if (request.latitude().compareTo(BigDecimal.valueOf(-90)) < 0 || request.latitude().compareTo(BigDecimal.valueOf(90)) > 0
        || request.longitude().compareTo(BigDecimal.valueOf(-180)) < 0 || request.longitude().compareTo(BigDecimal.valueOf(180)) > 0) {
      throw new BusinessException("签到坐标不合法");
    }
    WorkOrderStatus from = order.getStatus();
    order.setCheckInAt(request.checkInAt());
    order.setCheckInLocation(request.checkInLocation().trim());
    order.setCheckInLatitude(request.latitude());
    order.setCheckInLongitude(request.longitude());
    order.setCheckInAccuracy(request.accuracy());
    order.setStartedAt(OffsetDateTime.now());
    order.setStatus(WorkOrderStatus.IN_PROGRESS);
    workOrderRepository.save(order);
    addStatusLog(order, from, "现场签到：" + request.checkInLocation().trim(), principal.displayName());
    recordOperation(order, request.operationId(), "CHECK_IN", principal);
    return toResponse(order);
  }

  @Transactional
  public WorkOrderResponse mobileComplete(UUID id, MobileCompleteWorkOrderRequest request, UserPrincipal principal) {
    WorkOrder order = requireMobileOrder(id, principal);
    if (mobileOperationRepository.existsByOperationId(request.operationId())) return toResponse(order);
    if (order.getStatus() != WorkOrderStatus.IN_PROGRESS) throw new BusinessException("仅进行中的工单可以提交完工");
    if (attachmentRepository.findByWorkOrderIdOrderByCreatedAtAsc(id).stream()
        .noneMatch(item -> item.getCategory() == WorkOrderAttachmentCategory.CUSTOMER_SIGNATURE)) {
      throw new BusinessException("请先上传客户签字");
    }
    WorkOrderStatus from = order.getStatus();
    order.setLaborHours(nvl(request.laborHours()));
    order.setLaborCost(nvl(request.laborCost()));
    order.setMaterialCost(nvl(request.materialCost()));
    order.setTravelCost(nvl(request.travelCost()));
    order.setOutsourcingCost(nvl(request.outsourcingCost()));
    order.setCostAmount(nvl(request.costAmount()));
    order.setBillableAmount(nvl(request.billableAmount()));
    order.setServiceResult(request.serviceResult().trim());
    order.setAcceptanceNote(trimToNull(request.remarks()));
    order.setCustomerSigner(request.customerSigner().trim());
    order.setCompletedAt(OffsetDateTime.now());
    order.setStatus(WorkOrderStatus.COMPLETED);
    workOrderRepository.save(order);
    replaceMaterials(order, request.materials());
    addStatusLog(order, from, "移动端提交完工", principal.displayName());
    recordOperation(order, request.operationId(), "COMPLETE", principal);
    return toResponse(order);
  }

  @Transactional
  public AttachmentResponse uploadMobileAttachment(UUID id, WorkOrderAttachmentCategory category,
      MultipartFile file, UserPrincipal principal) {
    WorkOrder order = requireMobileOrder(id, principal);
    if (order.getStatus() == WorkOrderStatus.ACCEPTED || order.getStatus() == WorkOrderStatus.CANCELLED) {
      throw new BusinessException("已结束工单不能继续上传现场附件");
    }
    var stored = storageService.store(file, "work-orders", MOBILE_IMAGE_POLICY);
    WorkOrderAttachment item = new WorkOrderAttachment();
    item.setWorkOrderId(id);
    item.setCategory(category == null ? WorkOrderAttachmentCategory.OTHER : category);
    item.setFileName(stored.originalName());
    item.setObjectKey(stored.objectKey());
    item.setContentType(stored.contentType());
    item.setFileSize(stored.sizeBytes());
    item.setUploadedBy(principal.displayName());
    return toAttachment(attachmentRepository.save(item));
  }

  @Transactional(readOnly = true)
  public WorkOrderAttachment requireMobileAttachment(UUID attachmentId, UserPrincipal principal) {
    WorkOrderAttachment item = attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new BusinessException("现场附件不存在"));
    requireMobileOrder(item.getWorkOrderId(), principal);
    return item;
  }

  public Resource loadMobileAttachment(WorkOrderAttachment item) {
    return storageService.loadInNamespace("work-orders", item.getObjectKey());
  }

  @Transactional(readOnly = true)
  public List<MaterialResponse> listMobileMaterials(UUID id, UserPrincipal principal) {
    requireMobileOrder(id, principal);
    return materialRepository.findByWorkOrderIdOrderByCreatedAtAsc(id).stream().map(this::toMaterial).toList();
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
    WorkOrderStatus from = o.getStatus();
    o.setAssigneeId(r.assigneeId());
    o.setEngineerName(r.assigneeName());
    o.setAssignmentAcceptedAt(null);
    o.setStatus(WorkOrderStatus.ASSIGNED);
    WorkOrder saved = workOrderRepository.save(o);
    addStatusLog(saved, from, "指派给 " + r.assigneeName(), currentOperator());
    return toResponse(saved);
  }

  @Transactional
  public WorkOrderResponse checkIn(UUID id, CheckInRequest r) {
    WorkOrder o = getOrder(id);
    WorkOrderStatus from = o.getStatus();
    o.setCheckInAt(r.checkInAt());
    o.setCheckInLocation(r.checkInLocation());
    o.setStartedAt(OffsetDateTime.now());
    o.setStatus(WorkOrderStatus.IN_PROGRESS);
    WorkOrder saved = workOrderRepository.save(o);
    addStatusLog(saved, from, "现场签到", currentOperator());
    return toResponse(saved);
  }

  @Transactional
  public WorkOrderResponse complete(UUID id, CompleteWorkOrderRequest r) {
    WorkOrder o = getOrder(id);
    WorkOrderStatus from = o.getStatus();
    o.setLaborHours(nvl(r.laborHours()));
    o.setLaborCost(nvl(r.laborCost()));
    o.setMaterialCost(nvl(r.materialCost()));
    o.setTravelCost(nvl(r.travelCost()));
    o.setOutsourcingCost(nvl(r.outsourcingCost()));
    o.setCostAmount(nvl(r.costAmount()));
    o.setBillableAmount(nvl(r.billableAmount()));
    o.setServiceResult(r.serviceResult());
    o.setAcceptanceNote(r.remarks());
    o.setCompletedAt(OffsetDateTime.now());
    o.setStatus(WorkOrderStatus.COMPLETED);
    WorkOrder saved = workOrderRepository.save(o);
    addStatusLog(saved, from, "提交完工", currentOperator());
    return toResponse(saved);
  }

  @Transactional
  public WorkOrderResponse accept(UUID id, AcceptWorkOrderRequest r) {
    WorkOrder o = getOrder(id);
    WorkOrderStatus from = o.getStatus();
    o.setCostAmount(nvl(r.actualCost()));
    o.setAcceptanceNote(r.remarks());
    o.setStatus(WorkOrderStatus.ACCEPTED);
    o.setAcceptedAt(OffsetDateTime.now());
    WorkOrder saved = workOrderRepository.save(o);
    addStatusLog(saved, from, "客户验收", currentOperator());
    return toResponse(saved);
  }

  @Transactional
  public void deleteWorkOrder(UUID id) {
    WorkOrder item = getOrder(id);
    if (!deleteGovernanceService.allowPhysicalDelete("WORK_ORDER", id, item.getCode() + " " + item.getTitle())) return;
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

  private WorkOrder requireMobileOrder(UUID id, UserPrincipal principal) {
    WorkOrder order = getOrder(id);
    if (deleteGovernanceService.isHidden("WORK_ORDER", id) || !canAccessMobile(order, principal)) {
      throw new BusinessException("无权访问该工单");
    }
    return order;
  }

  private boolean canAccessMobile(WorkOrder order, UserPrincipal principal) {
    if (principal == null) return false;
    if (principal.roleCodes().contains("ADMIN")) return true;
    return principal.id().equals(order.getAssigneeId())
        || (order.getEngineerName() != null && order.getEngineerName().equals(principal.displayName()));
  }

  private void recordOperation(WorkOrder order, String operationId, String operationType, UserPrincipal principal) {
    WorkOrderMobileOperation operation = new WorkOrderMobileOperation();
    operation.setWorkOrderId(order.getId());
    operation.setOperationId(operationId.trim());
    operation.setOperationType(operationType);
    operation.setOperatedBy(principal.id());
    mobileOperationRepository.save(operation);
  }

  private void replaceMaterials(WorkOrder order, List<MaterialRequest> requests) {
    materialRepository.deleteAll(materialRepository.findByWorkOrderIdOrderByCreatedAtAsc(order.getId()));
    if (requests == null || requests.isEmpty()) return;
    for (MaterialRequest request : requests) {
      WorkOrderMaterial material = new WorkOrderMaterial();
      material.setWorkOrderId(order.getId());
      material.setPartId(request.partId());
      material.setPartName(request.partName().trim());
      material.setQuantity(request.quantity());
      material.setUnitCost(request.unitCost());
      material.setAmount(request.quantity().multiply(request.unitCost()));
      materialRepository.save(material);
    }
  }

  private void addStatusLog(WorkOrder order, WorkOrderStatus from, String remark, String operator) {
    WorkOrderStatusLog logItem = new WorkOrderStatusLog();
    logItem.setWorkOrderId(order.getId());
    logItem.setFromStatus(from);
    logItem.setToStatus(order.getStatus());
    logItem.setOperatorName(operator == null || operator.isBlank() ? "系统" : operator);
    logItem.setRemark(remark);
    statusLogRepository.save(logItem);
  }

  private String currentOperator() {
    var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) return principal.displayName();
    return authentication == null ? "系统" : authentication.getName();
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
        o.getPlannedDate(), o.getSiteAddress(),
        o.getAssignmentAcceptedAt(),
        o.getCheckInAt(), o.getCheckInLocation(),
        o.getCheckInLatitude(), o.getCheckInLongitude(), o.getCheckInAccuracy(),
        o.getStartedAt(), o.getCompletedAt(), o.getAcceptedAt(),
        o.getCreatedAt(), o.getUpdatedAt(),
        o.getServiceResult(), o.getCustomerSigner(), o.getAcceptanceNote(),
        attachmentRepository.findByWorkOrderIdOrderByCreatedAtAsc(o.getId()).stream().map(this::toAttachment).toList(),
        materialRepository.findByWorkOrderIdOrderByCreatedAtAsc(o.getId()).stream().map(this::toMaterial).toList(),
        logs);
  }

  private AttachmentResponse toAttachment(WorkOrderAttachment item) {
    return new AttachmentResponse(
        item.getId(), item.getCategory(), item.getFileName(), item.getContentType(), item.getFileSize(),
        item.getUploadedBy(), item.getCreatedAt(), "/api/maintenance/mobile/attachments/" + item.getId() + "/content"
    );
  }

  private MaterialResponse toMaterial(WorkOrderMaterial item) {
    return new MaterialResponse(item.getId(), item.getPartId(), item.getPartName(), item.getQuantity(), item.getUnitCost(), item.getAmount());
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

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }

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
