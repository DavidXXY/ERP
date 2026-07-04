package com.company.ops.api.modules.office.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import com.company.ops.api.modules.maintenance.repository.WorkOrderRepository;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.office.domain.ApprovalAction;
import com.company.ops.api.modules.office.domain.ApprovalRequest;
import com.company.ops.api.modules.office.domain.ApprovalStatus;
import com.company.ops.api.modules.office.domain.ApprovalType;
import com.company.ops.api.modules.office.domain.DocumentFile;
import com.company.ops.api.modules.office.domain.ExpenseClaim;
import com.company.ops.api.modules.office.domain.ExpenseStatus;
import com.company.ops.api.modules.office.domain.ExpenseType;
import com.company.ops.api.modules.office.domain.OutsourceOrder;
import com.company.ops.api.modules.office.domain.OutsourceStatus;
import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.dto.OfficeDtos.AuditResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalActionResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.CompleteOutsourceRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.CreateApprovalRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.CreateExpenseRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.CreateOutsourceRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.DocumentResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.ExpenseResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.NotificationResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.OfficeOverview;
import com.company.ops.api.modules.office.dto.OfficeDtos.OfficeReferenceResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.OutsourceResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.ProcessApprovalRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.SupplierOption;
import com.company.ops.api.modules.office.dto.OfficeDtos.ProjectOption;
import com.company.ops.api.modules.office.dto.OfficeDtos.UserOption;
import com.company.ops.api.modules.office.dto.OfficeDtos.WorkOrderOption;
import com.company.ops.api.modules.office.repository.ApprovalActionRepository;
import com.company.ops.api.modules.office.repository.ApprovalRequestRepository;
import com.company.ops.api.modules.office.repository.DocumentFileRepository;
import com.company.ops.api.modules.office.repository.ExpenseClaimRepository;
import com.company.ops.api.modules.office.repository.OutsourceOrderRepository;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import com.company.ops.api.modules.project.dto.CreateProjectCostRequest;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.service.ProjectService;
import com.company.ops.api.modules.system.domain.SystemAuditLog;
import com.company.ops.api.modules.system.repository.SystemAuditLogRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OfficeService {
  private final ApprovalRequestRepository approvalRepository;
  private final ApprovalActionRepository actionRepository;
  private final ExpenseClaimRepository expenseRepository;
  private final OutsourceOrderRepository outsourceRepository;
  private final DocumentFileRepository documentRepository;
  private final SystemNotificationRepository notificationRepository;
  private final SupplierRepository supplierRepository;
  private final ProjectRepository projectRepository;
  private final WorkOrderRepository workOrderRepository;
  private final ProjectService projectService;
  private final SystemUserRepository userRepository;
  private final LedgerService ledgerService;
  private final SystemAuditLogRepository auditLogRepository;
  private final Path storageRoot;

  public OfficeService(ApprovalRequestRepository approvalRepository, ApprovalActionRepository actionRepository,
                       ExpenseClaimRepository expenseRepository, OutsourceOrderRepository outsourceRepository,
                       DocumentFileRepository documentRepository, SystemNotificationRepository notificationRepository,
                       SupplierRepository supplierRepository,
                       ProjectRepository projectRepository, WorkOrderRepository workOrderRepository,
                       ProjectService projectService, SystemUserRepository userRepository, LedgerService ledgerService, SystemAuditLogRepository auditLogRepository,
                       @Value("${ops.storage.local-path:.local-data/uploads}") String storagePath) {
    this.approvalRepository = approvalRepository; this.actionRepository = actionRepository;
    this.expenseRepository = expenseRepository; this.outsourceRepository = outsourceRepository;
    this.documentRepository = documentRepository; this.notificationRepository = notificationRepository;
    this.supplierRepository = supplierRepository;
    this.projectRepository = projectRepository; this.workOrderRepository = workOrderRepository;
    this.projectService = projectService; this.userRepository = userRepository;
    this.ledgerService = ledgerService;
    this.auditLogRepository = auditLogRepository;
    this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
  }

  @Transactional(readOnly = true)
  public OfficeOverview overview() {
    List<ApprovalRequest> approvals = approvalRepository.findAllByOrderByCreatedAtDesc();
    List<ExpenseClaim> expenses = expenseRepository.findAllByOrderByExpenseDateDescCreatedAtDesc();
    List<OutsourceOrder> outsourcing = outsourceRepository.findAllByOrderByPlannedDateDescCreatedAtDesc();
    return new OfficeOverview(
        approvals.stream().filter(item -> item.getStatus() == ApprovalStatus.PENDING).count(),
        expenses.stream().filter(item -> item.getStatus() == ExpenseStatus.PENDING_APPROVAL).map(ExpenseClaim::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
        expenses.stream().filter(item -> item.getStatus() == ExpenseStatus.APPROVED || item.getStatus() == ExpenseStatus.PAID).map(ExpenseClaim::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
        outsourcing.stream().filter(item -> item.getStatus() != OutsourceStatus.COMPLETED && item.getStatus() != OutsourceStatus.SETTLED && item.getStatus() != OutsourceStatus.REJECTED).count(),
        outsourcing.stream().map(OutsourceOrder::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
        notificationRepository.countByReadFalse(), documentRepository.count()
    );
  }

  @Transactional(readOnly = true)
  public OfficeReferenceResponse references() {
    return new OfficeReferenceResponse(
        supplierRepository.findAllByOrderByCreatedAtDesc().stream().map(item -> new SupplierOption(item.getId(), item.getCode(), item.getName())).toList(),
        projectRepository.findAllByOrderByCreatedAtDesc().stream().map(item -> new ProjectOption(item.getId(), item.getCode(), item.getName())).toList(),
        workOrderRepository.findAllByOrderByCreatedAtDesc().stream().map(item -> new WorkOrderOption(item.getId(), item.getCode(), item.getTitle())).toList(),
        userRepository.findAll().stream().map(item -> new UserOption(item.getId(), item.getDisplayName(), item.isEnabled())).toList()
    );
  }

  @Transactional(readOnly = true)
  public List<ApprovalResponse> listApprovals() { return approvalRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toApproval).toList(); }

  @Transactional
  public ApprovalResponse createApproval(CreateApprovalRequest request) {
    ApprovalRequest saved = createApprovalEntity(request.code(), request.approvalType(), request.title(), request.sourceNo(), request.amount(), request.applicantName(), request.content());
    notify("APPROVAL", "新的审批待办", saved.getTitle(), "APPROVAL", saved.getId());
    return toApproval(saved);
  }

  @Transactional
  public ApprovalResponse processApproval(UUID id, ProcessApprovalRequest request) {
    if (request.decision() != ApprovalStatus.APPROVED && request.decision() != ApprovalStatus.REJECTED) throw new BusinessException("审批结论只能选择通过或驳回");
    ApprovalRequest approval = approvalRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("审批单不存在"));
    if (approval.getStatus() != ApprovalStatus.PENDING) throw new BusinessException("该审批单已处理");
    approval.setStatus(request.decision()); approval.setApproverName(request.approverName());
    approval.setApprovalComment(request.comment()); approval.setProcessedAt(OffsetDateTime.now());
    ApprovalRequest saved = approvalRepository.save(approval);
    ApprovalAction action = new ApprovalAction(); action.setApprovalId(saved.getId()); action.setDecision(request.decision());
    action.setOperatorName(request.approverName()); action.setComment(request.comment()); actionRepository.save(action);
    if (saved.getApprovalType() == ApprovalType.EXPENSE) processExpenseSource(saved);
    if (saved.getApprovalType() == ApprovalType.OUTSOURCE) processOutsourceSource(saved);
    notify("APPROVAL_RESULT", "审批结果：" + saved.getTitle(), request.decision() == ApprovalStatus.APPROVED ? "审批已通过" : "审批已驳回", "APPROVAL", saved.getId());
    return toApproval(saved);
  }

  @Transactional(readOnly = true)
  public List<ExpenseResponse> listExpenses() {
    List<ExpenseClaim> items = expenseRepository.findAllByOrderByExpenseDateDescCreatedAtDesc();
    Map<UUID, Project> projects = projectMap(items.stream().map(ExpenseClaim::getProjectId).filter(id -> id != null).toList());
    Map<UUID, WorkOrder> orders = workOrderMap(items.stream().map(ExpenseClaim::getWorkOrderId).filter(id -> id != null).toList());
    return items.stream().map(item -> toExpense(
        item,
        item.getProjectId() == null ? null : projects.get(item.getProjectId()),
        item.getWorkOrderId() == null ? null : orders.get(item.getWorkOrderId())
    )).toList();
  }

  @Transactional
  public ExpenseResponse createExpense(CreateExpenseRequest request) {
    if (expenseRepository.existsByCode(request.code())) throw new BusinessException("报销单号已存在");
    Project project = request.projectId() == null ? null : projectRepository.findById(request.projectId()).orElseThrow(() -> new BusinessException("项目不存在"));
    WorkOrder order = request.workOrderId() == null ? null : workOrderRepository.findById(request.workOrderId()).orElseThrow(() -> new BusinessException("工单不存在"));
    ExpenseClaim item = new ExpenseClaim(); item.setCode(request.code()); item.setClaimantId(request.claimantId());
    item.setClaimantName(request.claimantName()); item.setProjectId(request.projectId()); item.setWorkOrderId(request.workOrderId());
    item.setExpenseType(request.expenseType()); item.setAmount(request.amount()); item.setExpenseDate(request.expenseDate());
    item.setDescription(request.description()); item.setStatus(ExpenseStatus.PENDING_APPROVAL);
    ExpenseClaim saved = expenseRepository.save(item);
    ApprovalRequest approval = createApprovalEntity("SP-" + request.code(), ApprovalType.EXPENSE, "费用报销 " + request.code(), request.code(), request.amount(), request.claimantName(), request.description());
    saved.setApprovalRequestId(approval.getId()); expenseRepository.save(saved);
    notify("APPROVAL", "费用报销待审批", request.code() + " · " + request.claimantName(), "EXPENSE", saved.getId());
    return toExpense(saved, project, order);
  }

  @Transactional(readOnly = true)
  public List<OutsourceResponse> listOutsource() {
    List<OutsourceOrder> items = outsourceRepository.findAllByOrderByPlannedDateDescCreatedAtDesc();
    Map<UUID, Supplier> suppliers = supplierMap(items.stream().map(OutsourceOrder::getSupplierId).toList());
    Map<UUID, Project> projects = projectMap(items.stream().map(OutsourceOrder::getProjectId).filter(id -> id != null).toList());
    Map<UUID, WorkOrder> orders = workOrderMap(items.stream().map(OutsourceOrder::getWorkOrderId).filter(id -> id != null).toList());
    return items.stream().map(item -> toOutsource(item, suppliers.get(item.getSupplierId()), projects.get(item.getProjectId()), orders.get(item.getWorkOrderId()))).toList();
  }

  @Transactional
  public OutsourceResponse createOutsource(CreateOutsourceRequest request) {
    if (outsourceRepository.existsByCode(request.code())) throw new BusinessException("外包单号已存在");
    Supplier supplier = supplierRepository.findById(request.supplierId()).orElseThrow(() -> new BusinessException("服务商不存在"));
    Project project = request.projectId() == null ? null : projectRepository.findById(request.projectId()).orElseThrow(() -> new BusinessException("项目不存在"));
    WorkOrder order = request.workOrderId() == null ? null : workOrderRepository.findById(request.workOrderId()).orElseThrow(() -> new BusinessException("工单不存在"));
    OutsourceOrder item = new OutsourceOrder(); item.setCode(request.code()); item.setSupplierId(request.supplierId());
    item.setProjectId(request.projectId()); item.setWorkOrderId(request.workOrderId()); item.setServiceType(request.serviceType());
    item.setDescription(request.description()); item.setAmount(request.amount()); item.setPlannedDate(request.plannedDate());
    item.setStatus(OutsourceStatus.PENDING_APPROVAL); OutsourceOrder saved = outsourceRepository.save(item);
    ApprovalRequest approval = createApprovalEntity("SP-" + request.code(), ApprovalType.OUTSOURCE, "外包服务 " + request.code(), request.code(), request.amount(), request.applicantName(), request.description());
    saved.setApprovalRequestId(approval.getId()); outsourceRepository.save(saved);
    notify("APPROVAL", "外包服务待审批", request.code() + " · " + supplier.getName(), "OUTSOURCE", saved.getId());
    return toOutsource(saved, supplier, project, order);
  }

  @Transactional
  public OutsourceResponse completeOutsource(UUID id, CompleteOutsourceRequest request) {
    OutsourceOrder item = outsourceRepository.findById(id).orElseThrow(() -> new BusinessException("外包单不存在"));
    if (item.getStatus() != OutsourceStatus.APPROVED && item.getStatus() != OutsourceStatus.IN_PROGRESS) throw new BusinessException("仅审批通过的外包单可以验收");
    if (item.getProjectId() != null) projectService.createCost(item.getProjectId(), new CreateProjectCostRequest(ProjectCostCategory.SUBCONTRACT, ProjectCostSource.SUBCONTRACT, item.getCode(), "外包服务：" + item.getDescription(), item.getAmount(), item.getPlannedDate()));
    if (item.getWorkOrderId() != null) workOrderRepository.findById(item.getWorkOrderId()).ifPresent(order -> {
      order.setOutsourcingCost(amount(order.getOutsourcingCost()).add(item.getAmount()));
      order.setCostAmount(amount(order.getCostAmount()).add(item.getAmount())); workOrderRepository.save(order);
    });
    ledgerService.post("OUTSOURCE_COST", item.getCode(), item.getPlannedDate(), "外包服务成本 " + item.getCode(), List.of(
        new PostingLine("6401", "外包服务成本", item.getAmount(), BigDecimal.ZERO, item.getDescription()),
        new PostingLine("2202", "应付账款", BigDecimal.ZERO, item.getAmount(), item.getCode())
    ));
    item.setStatus(OutsourceStatus.COMPLETED); item.setAcceptanceNote(request.acceptanceNote());
    OutsourceOrder saved = outsourceRepository.save(item);
    Supplier supplier = supplierRepository.findById(saved.getSupplierId()).orElse(null);
    Project project = saved.getProjectId() == null ? null : projectRepository.findById(saved.getProjectId()).orElse(null);
    WorkOrder order = saved.getWorkOrderId() == null ? null : workOrderRepository.findById(saved.getWorkOrderId()).orElse(null);
    notify("OUTSOURCE", "外包服务已验收", saved.getCode(), "OUTSOURCE", saved.getId());
    return toOutsource(saved, supplier, project, order);
  }

  @Transactional(readOnly = true)
  public List<SupplierOption> suppliers() { return supplierRepository.findAllByOrderByCreatedAtDesc().stream().map(item -> new SupplierOption(item.getId(), item.getCode(), item.getName())).toList(); }

  @Transactional(readOnly = true)
  public List<DocumentResponse> listDocuments() {
    return documentRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDocument).toList();
  }

  @Transactional(readOnly = true)
  public Page<DocumentResponse> listDocuments(
      String bizType, UUID bizId, int page, int size) {
    Pageable p = PageRequest.of(page, size);
    if (bizType != null && bizId != null) {
      return documentRepository.findByBizTypeAndBizIdOrderByCreatedAtDesc(bizType, bizId, p).map(this::toDocument);
    } else if (bizType != null) {
      return documentRepository.findByBizTypeOrderByCreatedAtDesc(bizType, p).map(this::toDocument);
    }
    return documentRepository.findAllByOrderByCreatedAtDesc(p).map(this::toDocument);
  }

  @Transactional(readOnly = true)
  public List<DocumentResponse> listDocumentsByBiz(String bizType, UUID bizId) {
    return documentRepository.findByBizTypeAndBizIdOrderByCreatedAtDesc(bizType, bizId).stream()
        .map(this::toDocument).toList();
  }

  @Transactional(readOnly = true)
  public long getDocumentCount(String bizType, UUID bizId) {
    return documentRepository.countByBizTypeAndBizId(bizType, bizId);
  }

  @Transactional
  public DocumentResponse storeDocument(String bizType, UUID bizId, MultipartFile file) {
    if (file.isEmpty()) throw new BusinessException("上传文件不能为空");
    if (file.getSize() > 20L * 1024 * 1024) throw new BusinessException("单个文件不能超过20MB");
    try {
      Files.createDirectories(storageRoot);
      String original = file.getOriginalFilename() == null ? "file" : Path.of(file.getOriginalFilename()).getFileName().toString();
      String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
      if (extension.length() > 12) extension = "";
      String objectKey = UUID.randomUUID() + extension;
      Path target = storageRoot.resolve(objectKey).normalize();
      if (!target.startsWith(storageRoot)) throw new BusinessException("文件路径非法");
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      DocumentFile item = new DocumentFile(); item.setBizType(bizType); item.setBizId(bizId); item.setFileName(original);
      item.setObjectKey(objectKey); item.setContentType(file.getContentType()); item.setSizeBytes(file.getSize());
      return toDocument(documentRepository.save(item));
    } catch (IOException error) { throw new BusinessException("文件保存失败"); }
  }

  @Transactional
  public List<DocumentResponse> storeDocuments(String bizType, UUID bizId, List<MultipartFile> files) {
    if (files == null || files.isEmpty()) throw new BusinessException("上传文件列表不能为空");
    List<DocumentResponse> results = new ArrayList<>();
    for (MultipartFile file : files) {
      results.add(storeDocument(bizType, bizId, file));
    }
    return results;
  }

  @Transactional
  public void deleteDocument(UUID id) {
    DocumentFile item = documentRepository.findById(id).orElseThrow(() -> new BusinessException("档案不存在"));
    try {
      Path file = storageRoot.resolve(item.getObjectKey()).normalize();
      if (file.startsWith(storageRoot)) Files.deleteIfExists(file);
    } catch (IOException error) { /* file already missing, still delete the record */ }
    documentRepository.delete(item);
  }

  @Transactional
  public void deleteDocumentsByBiz(String bizType, UUID bizId) {
    List<DocumentFile> items = documentRepository.findByBizTypeAndBizIdOrderByCreatedAtDesc(bizType, bizId);
    for (DocumentFile item : items) {
      try {
        Path file = storageRoot.resolve(item.getObjectKey()).normalize();
        if (file.startsWith(storageRoot)) Files.deleteIfExists(file);
      } catch (IOException error) { /* skip */ }
    }
    documentRepository.deleteByBizTypeAndBizId(bizType, bizId);
  }

  @Transactional
  public DocumentResponse updateDocumentName(UUID id, String newName) {
    if (newName == null || newName.isBlank()) throw new BusinessException("文件名不能为空");
    if (newName.length() > 240) throw new BusinessException("文件名不能超过240个字符");
    DocumentFile item = documentRepository.findById(id).orElseThrow(() -> new BusinessException("档案不存在"));
    item.setFileName(newName.trim());
    return toDocument(documentRepository.save(item));
  }

  @Transactional(readOnly = true)
  public DocumentFile requireDocument(UUID id) { return documentRepository.findById(id).orElseThrow(() -> new BusinessException("档案不存在")); }

  public Resource loadDocument(DocumentFile item) {
    try {
      Path file = storageRoot.resolve(item.getObjectKey()).normalize();
      if (!file.startsWith(storageRoot) || !Files.exists(file)) throw new BusinessException("档案文件不存在");
      return new UrlResource(file.toUri());
    } catch (IOException error) { throw new BusinessException("档案读取失败"); }
  }

  public Resource loadDocumentForPreview(DocumentFile item) {
    try {
      Path file = storageRoot.resolve(item.getObjectKey()).normalize();
      if (!file.startsWith(storageRoot) || !Files.exists(file)) throw new BusinessException("档案文件不存在");
      return new UrlResource(file.toUri());
    } catch (IOException error) { throw new BusinessException("档案读取失败"); }
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> notifications() { return notificationRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toNotification).toList(); }
  @Transactional
  public NotificationResponse readNotification(UUID id) {
    SystemNotification item = notificationRepository.findById(id).orElseThrow(() -> new BusinessException("消息不存在"));
    item.setRead(true); item.setReadAt(OffsetDateTime.now()); return toNotification(notificationRepository.save(item));
  }
  @Transactional(readOnly = true)

  private ApprovalRequest createApprovalEntity(String code, ApprovalType type, String title, String sourceNo, BigDecimal amount, String applicant, String content) {
    if (approvalRepository.existsByCode(code)) throw new BusinessException("审批单号已存在");
    ApprovalRequest item = new ApprovalRequest(); item.setCode(code); item.setApprovalType(type); item.setTitle(title);
    item.setSourceNo(sourceNo); item.setAmount(amount(amount)); item.setApplicantName(applicant); item.setContent(content); item.setStatus(ApprovalStatus.PENDING);
    return approvalRepository.save(item);
  }

  private void processExpenseSource(ApprovalRequest approval) {
    expenseRepository.findByApprovalRequestId(approval.getId()).ifPresent(expense -> {
      if (approval.getStatus() == ApprovalStatus.APPROVED) {
        expense.setStatus(ExpenseStatus.APPROVED);
        if (expense.getProjectId() != null) projectService.createCost(expense.getProjectId(), new CreateProjectCostRequest(
            expense.getExpenseType() == ExpenseType.TRAVEL || expense.getExpenseType() == ExpenseType.TRANSPORT || expense.getExpenseType() == ExpenseType.ACCOMMODATION ? ProjectCostCategory.TRAVEL : ProjectCostCategory.OTHER,
            ProjectCostSource.EXPENSE, expense.getCode(), "费用报销：" + expense.getDescription(), expense.getAmount(), expense.getExpenseDate()));
        if (expense.getWorkOrderId() != null) workOrderRepository.findById(expense.getWorkOrderId()).ifPresent(order -> {
          order.setTravelCost(amount(order.getTravelCost()).add(expense.getAmount())); order.setCostAmount(amount(order.getCostAmount()).add(expense.getAmount())); workOrderRepository.save(order);
        });
        String expenseAccount = expense.getExpenseType() == ExpenseType.TOOL ? "6602" : "6601";
        String expenseAccountName = expense.getExpenseType() == ExpenseType.TOOL ? "工具及办公费" : "差旅交通费";
        ledgerService.post("EXPENSE", expense.getCode(), expense.getExpenseDate(), "费用报销 " + expense.getCode(), List.of(
            new PostingLine(expenseAccount, expenseAccountName, expense.getAmount(), BigDecimal.ZERO, expense.getDescription()),
            new PostingLine("2241", "其他应付款", BigDecimal.ZERO, expense.getAmount(), expense.getClaimantName())
        ));
      } else expense.setStatus(ExpenseStatus.REJECTED);
      expenseRepository.save(expense);
    });
  }

  private void processOutsourceSource(ApprovalRequest approval) {
    outsourceRepository.findByApprovalRequestId(approval.getId()).ifPresent(item -> {
      item.setStatus(approval.getStatus() == ApprovalStatus.APPROVED ? OutsourceStatus.APPROVED : OutsourceStatus.REJECTED);
    });
  }

  public int refreshNotifications() {
    long count = approvalRepository.findAll().stream()
        .filter(a -> a.getStatus() == com.company.ops.api.modules.office.domain.ApprovalStatus.PENDING)
        .count();
    return (int) count;
  }

  @Transactional(readOnly = true)
  public List<AuditResponse> listAudits() {
    return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(item -> new AuditResponse(
            item.getId(),
            item.getUsername(),
            item.getHttpMethod(),
            item.getRequestPath(),
            item.getResponseStatus(),
            item.getClientIp(),
            item.getDurationMs(),
            item.getCreatedAt()
        ))
        .toList();
  }


  private void notify(String type, String title, String content, String relatedType, UUID relatedId) {
    SystemNotification item = new SystemNotification(); item.setType(type); item.setTitle(title); item.setContent(content);
    item.setRelatedType(relatedType); item.setRelatedId(relatedId); item.setRead(false); notificationRepository.save(item);
  }
  private ApprovalResponse toApproval(ApprovalRequest item) { return new ApprovalResponse(item.getId(), item.getCode(), item.getApprovalType(), item.getTitle(), item.getSourceNo(), amount(item.getAmount()), item.getStatus(), item.getApplicantName(), item.getContent(), item.getApproverName(), item.getApprovalComment(), item.getProcessedAt(), item.getCreatedAt(), actionRepository.findByApprovalIdOrderByCreatedAtAsc(item.getId()).stream().map(action -> new ApprovalActionResponse(action.getId(), action.getDecision(), action.getOperatorName(), action.getComment(), action.getCreatedAt())).toList()); }
  private ExpenseResponse toExpense(ExpenseClaim item, Project project, WorkOrder order) { return new ExpenseResponse(item.getId(), item.getCode(), item.getClaimantId(), item.getClaimantName(), item.getProjectId(), project == null ? null : project.getCode(), item.getWorkOrderId(), order == null ? null : order.getCode(), item.getExpenseType(), item.getAmount(), item.getExpenseDate(), item.getDescription(), item.getStatus(), item.getApprovalRequestId()); }
  private OutsourceResponse toOutsource(OutsourceOrder item, Supplier supplier, Project project, WorkOrder order) { return new OutsourceResponse(item.getId(), item.getCode(), item.getSupplierId(), supplier == null ? null : supplier.getName(), item.getProjectId(), project == null ? null : project.getCode(), item.getWorkOrderId(), order == null ? null : order.getCode(), item.getServiceType(), item.getDescription(), item.getAmount(), item.getPlannedDate(), item.getStatus(), item.getApprovalRequestId(), item.getAcceptanceNote()); }
  private DocumentResponse toDocument(DocumentFile item) { return new DocumentResponse(item.getId(), item.getBizType(), item.getBizId(), item.getFileName(), item.getContentType(), item.getSizeBytes(), item.getCreatedAt()); }
  private NotificationResponse toNotification(SystemNotification item) { return new NotificationResponse(item.getId(), item.getType(), item.getTitle(), item.getContent(), item.getRelatedType(), item.getRelatedId(), item.isRead(), item.getReadAt(), item.getCreatedAt()); }
  private BigDecimal amount(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
  private Map<UUID, Supplier> supplierMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : supplierRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(Supplier::getId, Function.identity())); }
  private Map<UUID, Project> projectMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : projectRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(Project::getId, Function.identity())); }
  private Map<UUID, WorkOrder> workOrderMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : workOrderRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(WorkOrder::getId, Function.identity())); }

}