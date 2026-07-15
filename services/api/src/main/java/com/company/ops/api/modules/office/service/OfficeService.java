package com.company.ops.api.modules.office.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.storage.FileStorageService.FilePolicy;
import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import com.company.ops.api.modules.maintenance.repository.WorkOrderRepository;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.office.domain.ApprovalAction;
import com.company.ops.api.modules.office.domain.ApprovalRequest;
import com.company.ops.api.modules.office.domain.ApprovalRuntimeNode;
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
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalAddSignRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalReturnRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalRuntimeNodeResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalResubmitRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalTransferRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalWithdrawRequest;
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
import com.company.ops.api.modules.office.dto.OfficeDtos.TodoItemResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.WarningItemResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.WorkbenchResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.WorkOrderOption;
import com.company.ops.api.modules.office.repository.ApprovalActionRepository;
import com.company.ops.api.modules.office.repository.ApprovalRequestRepository;
import com.company.ops.api.modules.office.repository.ApprovalRuntimeNodeRepository;
import com.company.ops.api.modules.office.repository.DocumentFileRepository;
import com.company.ops.api.modules.office.repository.ExpenseClaimRepository;
import com.company.ops.api.modules.office.repository.OutsourceOrderRepository;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import com.company.ops.api.modules.project.dto.CreateProjectCostRequest;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.service.ProjectService;
import com.company.ops.api.modules.system.domain.SystemAuditLog;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemAuditLogRepository;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.service.ApprovalFlowSecurity;
import com.company.ops.api.modules.system.service.ApprovalFlowSecurity.ApprovalContext;
import com.company.ops.api.modules.system.service.ApprovalFlowSecurity.ApprovalPlan;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import static com.company.ops.api.common.util.MoneyUtils.amount;

@Service
public class OfficeService {
  private static final FilePolicy DOCUMENT_POLICY = new FilePolicy(
      20L * 1024 * 1024,
      Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".txt", ".zip"),
      "单个文件不能超过20MB",
      "仅支持图片、PDF、Word、Excel、TXT 或 ZIP 档案",
      false
  );
  private final ApprovalRequestRepository approvalRepository;
  private final ApprovalActionRepository actionRepository;
  private final ApprovalRuntimeNodeRepository runtimeNodeRepository;
  private final ExpenseClaimRepository expenseRepository;
  private final OutsourceOrderRepository outsourceRepository;
  private final DocumentFileRepository documentRepository;
  private final SystemNotificationRepository notificationRepository;
  private final SupplierRepository supplierRepository;
  private final ProjectRepository projectRepository;
  private final WorkOrderRepository workOrderRepository;
  private final ProjectService projectService;
  private final CustomerRepository customerRepository;
  private final SystemUserRepository userRepository;
  private final SystemRoleRepository roleRepository;
  private final SystemOrganizationRepository organizationRepository;
  private final LedgerService ledgerService;
  private final SystemAuditLogRepository auditLogRepository;
  private final ApprovalFlowSecurity approvalFlowSecurity;
  private final FileStorageService storageService;
  private final DeleteGovernanceService deleteGovernanceService;
  @PersistenceContext
  private EntityManager entityManager;

  public OfficeService(ApprovalRequestRepository approvalRepository, ApprovalActionRepository actionRepository, ApprovalRuntimeNodeRepository runtimeNodeRepository,
                       ExpenseClaimRepository expenseRepository, OutsourceOrderRepository outsourceRepository,
                       DocumentFileRepository documentRepository, SystemNotificationRepository notificationRepository,
	                       SupplierRepository supplierRepository, CustomerRepository customerRepository,
	                       ProjectRepository projectRepository, WorkOrderRepository workOrderRepository,
	                       ProjectService projectService, SystemUserRepository userRepository, SystemRoleRepository roleRepository, SystemOrganizationRepository organizationRepository, LedgerService ledgerService, SystemAuditLogRepository auditLogRepository, ApprovalFlowSecurity approvalFlowSecurity,
	                       FileStorageService storageService,
	                       DeleteGovernanceService deleteGovernanceService) {
    this.approvalRepository = approvalRepository; this.actionRepository = actionRepository; this.runtimeNodeRepository = runtimeNodeRepository;
    this.expenseRepository = expenseRepository; this.outsourceRepository = outsourceRepository;
    this.documentRepository = documentRepository; this.notificationRepository = notificationRepository;
    this.supplierRepository = supplierRepository; this.customerRepository = customerRepository;
    this.projectRepository = projectRepository; this.workOrderRepository = workOrderRepository;
    this.projectService = projectService; this.userRepository = userRepository; this.roleRepository = roleRepository; this.organizationRepository = organizationRepository;
	    this.ledgerService = ledgerService;
	    this.auditLogRepository = auditLogRepository;
	    this.approvalFlowSecurity = approvalFlowSecurity;
	    this.storageService = storageService;
	    this.deleteGovernanceService = deleteGovernanceService;
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

  @Transactional(readOnly = true)
  public List<ApprovalResponse> listMyPendingApprovals() {
    return approvalRepository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING).stream()
        .filter(this::canCurrentUserApprove)
        .map(this::toApproval)
        .toList();
  }

  @Transactional(readOnly = true)
  public WorkbenchResponse workbench() {
    List<TodoItemResponse> todos = new ArrayList<>();
    approvalRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(item -> item.getStatus() == ApprovalStatus.PENDING)
        .forEach(item -> todos.add(new TodoItemResponse(
            "APPROVAL", item.getId(), item.getTitle(), item.getApprovalType().name() + " · " + item.getApplicantName(),
            amount(item.getAmount()), "HIGH", "/office/approvals", item.getCreatedAt()
        )));
    expenseRepository.findAllByOrderByExpenseDateDescCreatedAtDesc().stream()
        .filter(item -> item.getStatus() == ExpenseStatus.PENDING_APPROVAL)
        .forEach(item -> todos.add(new TodoItemResponse(
            "EXPENSE", item.getId(), "费用报销待审批 " + item.getCode(), item.getClaimantName() + " · " + item.getDescription(),
            amount(item.getAmount()), "MEDIUM", "/office/expenses", item.getCreatedAt()
        )));
    outsourceRepository.findAllByOrderByPlannedDateDescCreatedAtDesc().stream()
        .filter(item -> item.getStatus() == OutsourceStatus.PENDING_APPROVAL)
        .forEach(item -> todos.add(new TodoItemResponse(
            "OUTSOURCE", item.getId(), "外包服务待审批 " + item.getCode(), item.getServiceType() + " · " + item.getDescription(),
            amount(item.getAmount()), "MEDIUM", "/office/outsourcing", item.getCreatedAt()
        )));

    LocalDate today = LocalDate.now();
    List<WarningItemResponse> warnings = new ArrayList<>();
    approvalRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(item -> item.getStatus() == ApprovalStatus.PENDING)
        .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().isBefore(OffsetDateTime.now().minusDays(2)))
        .forEach(item -> warnings.add(new WarningItemResponse(
            "APPROVAL_OVERDUE", item.getId(), "审批超时", item.getTitle() + " 已等待超过2天", "HIGH", "/office/approvals", item.getCreatedAt()
        )));
    outsourceRepository.findAllByOrderByPlannedDateDescCreatedAtDesc().stream()
        .filter(item -> item.getStatus() == OutsourceStatus.APPROVED || item.getStatus() == OutsourceStatus.IN_PROGRESS)
        .filter(item -> item.getPlannedDate() != null && item.getPlannedDate().isBefore(today))
        .forEach(item -> warnings.add(new WarningItemResponse(
            "OUTSOURCE_OVERDUE", item.getId(), "外包服务逾期", item.getCode() + " 计划日期 " + item.getPlannedDate(), "MEDIUM", "/office/outsourcing", item.getCreatedAt()
        )));
    notificationRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(item -> !item.isRead())
        .limit(20)
        .forEach(item -> warnings.add(new WarningItemResponse(
            item.getType(), item.getId(), item.getTitle(), item.getContent(), "LOW", "/office/notifications", item.getCreatedAt()
        )));
    return new WorkbenchResponse(
        todos.stream().sorted((a, b) -> b.createdAt().compareTo(a.createdAt())).toList(),
        warnings.stream().sorted((a, b) -> b.createdAt().compareTo(a.createdAt())).toList(),
        todos.size(),
        warnings.stream().filter(item -> "HIGH".equals(item.severity())).count()
    );
  }

  @Transactional
  public ApprovalResponse createApproval(CreateApprovalRequest request) {
    ApprovalRequest saved = createApprovalEntity(request.code(), request.approvalType(), request.title(), request.sourceNo(), request.amount(), request.applicantName(), request.content(),
        request.departmentName(), request.businessType(), request.projectCode(), request.supplierRisk(), request.customerLevel());
    notify("APPROVAL", "新的审批待办", saved.getTitle(), "APPROVAL", saved.getId());
    return toApproval(saved);
  }

  @Transactional
  public ApprovalResponse processApproval(UUID id, ProcessApprovalRequest request) {
    if (request.decision() != ApprovalStatus.APPROVED && request.decision() != ApprovalStatus.REJECTED) throw new BusinessException("审批结论只能选择通过或驳回");
    ApprovalRequest approval = approvalRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("审批单不存在"));
    if (approval.getStatus() != ApprovalStatus.PENDING) throw new BusinessException("该审批单已处理");
    String flowCode = approval.getApprovalType().name();
    int completedApprovals = completedApprovals(id);
    List<ApprovalRuntimeNode> currentNodes = currentRuntimeNodes(approval);
    requireRuntimeApprover(currentNodes, approval.getDelegatedUserId());
    ApprovalRuntimeNode handled = selectRuntimeNode(currentNodes, approval.getDelegatedUserId());
    handled.setNodeStatus(request.decision() == ApprovalStatus.APPROVED ? "APPROVED" : "REJECTED");
    handled.setApproverId(approvalFlowSecurity.currentUserId());
    handled.setApproverName(request.approverName());
    handled.setApprovalComment(request.comment());
    handled.setCompletedAt(OffsetDateTime.now());
    runtimeNodeRepository.save(handled);
    if (request.decision() == ApprovalStatus.REJECTED) {
      currentNodes.stream().filter(item -> !"REJECTED".equals(item.getNodeStatus())).forEach(item -> { item.setNodeStatus("SKIPPED"); item.setCompletedAt(OffsetDateTime.now()); });
      runtimeNodeRepository.saveAll(currentNodes);
    } else if (stepSatisfied(currentNodes)) {
      currentNodes.stream().filter(item -> "PENDING".equals(item.getNodeStatus())).forEach(item -> { item.setNodeStatus("SKIPPED"); item.setCompletedAt(OffsetDateTime.now()); });
      runtimeNodeRepository.saveAll(currentNodes);
    }
    boolean finalStep = request.decision() == ApprovalStatus.REJECTED || noPendingRuntimeNodes(approval.getId());
    approval.setStatus(request.decision() == ApprovalStatus.REJECTED || finalStep ? request.decision() : ApprovalStatus.PENDING);
    approval.setApproverName(request.approverName());
    approval.setApprovalComment(request.comment()); approval.setProcessedAt(OffsetDateTime.now());
    approval.setCurrentStep(finalStep ? approval.getCurrentStep() : nextPendingStep(approval.getId()));
    approval.setCurrentApproverName(finalStep ? null : currentRuntimeApproverNames(approval.getId(), approval.getCurrentStep()));
    approval.setDelegatedUserId(null);
    ApprovalRequest saved = approvalRepository.save(approval);
    ApprovalAction action = new ApprovalAction(); action.setApprovalId(saved.getId()); action.setDecision(request.decision());
    action.setOperatorId(approvalFlowSecurity.currentUserId());
    action.setOperatorName(request.approverName()); action.setComment(request.comment()); action.setActionType(request.decision() == ApprovalStatus.APPROVED ? "APPROVE" : "REJECT");
    action.setStepNo(completedApprovals + 1); actionRepository.save(action);
    if (saved.getApprovalType() == ApprovalType.EXPENSE) processExpenseSource(saved);
    if (saved.getApprovalType() == ApprovalType.OUTSOURCE) processOutsourceSource(saved);
    if ("DELETE".equals(saved.getBusinessType())) processDeleteApproval(saved);
    notify("APPROVAL_RESULT", "审批结果：" + saved.getTitle(), request.decision() == ApprovalStatus.APPROVED ? "审批已通过" : "审批已驳回", "APPROVAL", saved.getId());
    return toApproval(saved);
  }

  @Transactional
  public ApprovalResponse transferApproval(UUID id, ApprovalTransferRequest request) {
    ApprovalRequest approval = approvalRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("审批单不存在"));
    if (approval.getStatus() != ApprovalStatus.PENDING) throw new BusinessException("该审批单已处理");
    int completed = completedApprovals(id);
    approvalFlowSecurity.requireApprover(approvalContext(approval), completed, approval.getDelegatedUserId(), approval.getApprovalConfigVersion());
    var target = userRepository.findById(request.targetUserId()).orElseThrow(() -> new BusinessException("转交人员不存在"));
    approval.setDelegatedUserId(request.targetUserId());
    approval.setCurrentApproverName(target.getDisplayName());
    ApprovalRequest saved = approvalRepository.save(approval);
    saveRuntimeAction(saved.getId(), "TRANSFER", request.operatorName(), request.comment(), completed + 1);
    notify("APPROVAL", "审批已转交", saved.getTitle() + " 转交给 " + target.getDisplayName(), "APPROVAL", saved.getId());
    return toApproval(saved);
  }

  @Transactional
  public ApprovalResponse addSignApproval(UUID id, ApprovalAddSignRequest request) {
    ApprovalRequest approval = approvalRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("审批单不存在"));
    if (approval.getStatus() != ApprovalStatus.PENDING) throw new BusinessException("该审批单已处理");
    int completed = completedApprovals(id);
    approvalFlowSecurity.requireApprover(approvalContext(approval), completed, approval.getDelegatedUserId(), approval.getApprovalConfigVersion());
    var target = userRepository.findById(request.targetUserId()).orElseThrow(() -> new BusinessException("加签人员不存在"));
    approval.setDelegatedUserId(request.targetUserId());
    approval.setCurrentApproverName(target.getDisplayName());
    ApprovalRequest saved = approvalRepository.save(approval);
    saveRuntimeAction(saved.getId(), "ADD_SIGN", request.operatorName(), request.comment(), completed + 1);
    notify("APPROVAL", "审批已加签", saved.getTitle() + " 加签给 " + target.getDisplayName(), "APPROVAL", saved.getId());
    return toApproval(saved);
  }

  @Transactional
  public ApprovalResponse withdrawApproval(UUID id, ApprovalWithdrawRequest request) {
    ApprovalRequest approval = approvalRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("审批单不存在"));
    if (approval.getStatus() != ApprovalStatus.PENDING) throw new BusinessException("只有待审批单据可以撤回");
    approval.setStatus(ApprovalStatus.REJECTED);
    approval.setApproverName(request.operatorName());
    approval.setApprovalComment("撤回：" + request.comment());
    approval.setProcessedAt(OffsetDateTime.now());
    approval.setCurrentApproverName(null);
    approval.setDelegatedUserId(null);
    ApprovalRequest saved = approvalRepository.save(approval);
    saveRuntimeAction(saved.getId(), "WITHDRAW", request.operatorName(), request.comment(), completedApprovals(id) + 1);
    if (saved.getApprovalType() == ApprovalType.EXPENSE) processExpenseSource(saved);
    if (saved.getApprovalType() == ApprovalType.OUTSOURCE) processOutsourceSource(saved);
    notify("APPROVAL_RESULT", "审批已撤回：" + saved.getTitle(), request.comment(), "APPROVAL", saved.getId());
    return toApproval(saved);
  }

  @Transactional
  public ApprovalResponse returnApproval(UUID id, ApprovalReturnRequest request) {
    ApprovalRequest approval = approvalRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("审批单不存在"));
    if (approval.getStatus() != ApprovalStatus.PENDING) throw new BusinessException("只有待审批单据可以退回");
    List<ApprovalRuntimeNode> currentNodes = currentRuntimeNodes(approval);
    requireRuntimeApprover(currentNodes, approval.getDelegatedUserId());
    int targetStep = Math.max(1, approval.getCurrentStep() == null ? 1 : approval.getCurrentStep() - 1);
    runtimeNodeRepository.findByApprovalIdOrderByStepNoAscCreatedAtAsc(id).stream()
        .filter(item -> item.getStepNo() >= targetStep)
        .forEach(item -> { item.setNodeStatus("PENDING"); item.setCompletedAt(null); item.setApproverId(null); item.setApproverName(null); item.setApprovalComment(null); });
    approval.setCurrentStep(targetStep);
    approval.setCurrentApproverName(currentRuntimeApproverNames(id, targetStep));
    ApprovalRequest saved = approvalRepository.save(approval);
    saveRuntimeAction(id, "RETURN", request.operatorName(), request.comment(), targetStep);
    notify("APPROVAL", "审批已退回", saved.getTitle() + " 退回到第 " + targetStep + " 步", "APPROVAL", saved.getId());
    return toApproval(saved);
  }

  @Transactional
  public ApprovalResponse resubmitApproval(UUID id, ApprovalResubmitRequest request) {
    ApprovalRequest approval = approvalRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("审批单不存在"));
    if (approval.getStatus() != ApprovalStatus.REJECTED) throw new BusinessException("只有已驳回/撤回审批可以重新提交");
    List<ApprovalRuntimeNode> nodes = runtimeNodeRepository.findByApprovalIdOrderByStepNoAscCreatedAtAsc(id);
    if (nodes.isEmpty()) throw new BusinessException("审批运行节点不存在，不能重新提交");
    nodes.forEach(item -> { item.setNodeStatus("PENDING"); item.setCompletedAt(null); item.setApproverId(null); item.setApproverName(null); item.setApprovalComment(null); });
    runtimeNodeRepository.saveAll(nodes);
    approval.setStatus(ApprovalStatus.PENDING); approval.setApplicantName(request.applicantName()); approval.setApprovalComment("重新提交：" + request.comment());
    approval.setProcessedAt(null); approval.setApproverName(null); approval.setCurrentStep(nextPendingStep(id)); approval.setCurrentApproverName(currentRuntimeApproverNames(id, approval.getCurrentStep()));
    ApprovalRequest saved = approvalRepository.save(approval);
    saveRuntimeAction(id, "RESUBMIT", request.applicantName(), request.comment(), approval.getCurrentStep());
    notify("APPROVAL", "审批重新提交", saved.getTitle(), "APPROVAL", saved.getId());
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
    ApprovalRequest approval = createApprovalEntity("SP-" + request.code(), ApprovalType.EXPENSE, "费用报销 " + request.code(), request.code(), request.amount(), request.claimantName(), request.description(),
        null, request.expenseType().name(), project == null ? null : project.getCode(), null, null);
    saved.setApprovalRequestId(approval.getId()); expenseRepository.save(saved);
    if (approval.getStatus() == ApprovalStatus.APPROVED) processExpenseSource(approval);
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
    ApprovalRequest approval = createApprovalEntity("SP-" + request.code(), ApprovalType.OUTSOURCE, "外包服务 " + request.code(), request.code(), request.amount(), request.applicantName(), request.description(),
        null, request.serviceType(), project == null ? null : project.getCode(), supplier.getRiskStatus() == null ? null : supplier.getRiskStatus().name(), null);
    saved.setApprovalRequestId(approval.getId()); outsourceRepository.save(saved);
    if (approval.getStatus() == ApprovalStatus.APPROVED) processOutsourceSource(approval);
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
    Page<DocumentFile> source;
    if (bizType != null && bizId != null) {
      source = documentRepository.findByBizTypeAndBizIdOrderByCreatedAtDesc(bizType, bizId, p);
    } else if (bizType != null) {
      source = documentRepository.findByBizTypeOrderByCreatedAtDesc(bizType, p);
    } else {
      source = documentRepository.findAllByOrderByCreatedAtDesc(p);
    }
    List<DocumentFile> visible = deleteGovernanceService.visible("OFFICE_DOCUMENT", source.getContent(), DocumentFile::getId);
    return new org.springframework.data.domain.PageImpl<>(
        visible.stream().map(this::toDocument).toList(),
        p,
        visible.size()
    );
  }

  @Transactional(readOnly = true)
  public List<DocumentResponse> listDocumentsByBiz(String bizType, UUID bizId) {
    return documentRepository.findByBizTypeAndBizIdOrderByCreatedAtDesc(bizType, bizId).stream()
        .filter(item -> !deleteGovernanceService.isHidden("OFFICE_DOCUMENT", item.getId()))
        .map(this::toDocument).toList();
  }

  @Transactional(readOnly = true)
  public long getDocumentCount(String bizType, UUID bizId) {
    return documentRepository.countByBizTypeAndBizId(bizType, bizId);
  }

  @Transactional
  public DocumentResponse storeDocument(String bizType, UUID bizId, MultipartFile file) {
    var stored = storageService.store(file, "office", DOCUMENT_POLICY);
    DocumentFile item = new DocumentFile(); item.setBizType(bizType); item.setBizId(bizId); item.setFileName(stored.originalName());
    item.setObjectKey(stored.objectKey()); item.setContentType(stored.contentType()); item.setSizeBytes(stored.sizeBytes());
    return toDocument(documentRepository.save(item));
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
    if (!deleteGovernanceService.allowPhysicalDelete("OFFICE_DOCUMENT", id, item.getFileName())) return;
    storageService.deleteInNamespace("office", item.getObjectKey());
    documentRepository.delete(item);
  }

  @Transactional
  public void deleteDocumentsByBiz(String bizType, UUID bizId) {
    List<DocumentFile> items = documentRepository.findByBizTypeAndBizIdOrderByCreatedAtDesc(bizType, bizId);
    for (DocumentFile item : items) {
      if (!deleteGovernanceService.allowPhysicalDelete("OFFICE_DOCUMENT", item.getId(), item.getFileName())) continue;
      storageService.deleteInNamespace("office", item.getObjectKey());
      documentRepository.delete(item);
    }
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
  public DocumentFile requireDocument(UUID id) {
    DocumentFile item = documentRepository.findById(id).orElseThrow(() -> new BusinessException("档案不存在"));
    if (deleteGovernanceService.isHidden("OFFICE_DOCUMENT", id)) throw new BusinessException("档案不存在");
    return item;
  }

  public Resource loadDocument(DocumentFile item) {
    return storageService.load("office/" + item.getObjectKey());
  }

  public Resource loadDocumentForPreview(DocumentFile item) {
    return storageService.load("office/" + item.getObjectKey());
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> notifications() { return notificationRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toNotification).toList(); }
  @Transactional
  public NotificationResponse readNotification(UUID id) {
    SystemNotification item = notificationRepository.findById(id).orElseThrow(() -> new BusinessException("消息不存在"));
    item.setRead(true); item.setReadAt(OffsetDateTime.now()); return toNotification(notificationRepository.save(item));
  }
  @Transactional(readOnly = true)

  private ApprovalRequest createApprovalEntity(String code, ApprovalType type, String title, String sourceNo, BigDecimal amount, String applicant, String content,
      String departmentName, String businessType, String projectCode, String supplierRisk, String customerLevel) {
    if (approvalRepository.existsByCode(code)) throw new BusinessException("审批单号已存在");
    ApprovalRequest item = new ApprovalRequest(); item.setCode(code); item.setApprovalType(type); item.setTitle(title);
    item.setSourceNo(sourceNo); item.setAmount(amount(amount)); item.setApplicantName(applicant); item.setContent(content); item.setStatus(ApprovalStatus.PENDING);
    item.setDepartmentName(trimToNull(departmentName)); item.setBusinessType(trimToNull(businessType)); item.setProjectCode(trimToNull(projectCode));
    item.setSupplierRisk(trimToNull(supplierRisk)); item.setCustomerLevel(trimToNull(customerLevel));
    ApprovalPlan plan = approvalFlowSecurity.resolve(approvalContext(item));
    validatePlan(plan);
    item.setApprovalConfigVersion(plan.versionNo()); item.setApprovalPlanSnapshot(planSnapshot(plan));
    boolean autoApproved = isAutoApprovalPlan(plan);
    item.setStatus(autoApproved ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING);
    item.setApprovalMode(plan.mode()); item.setCurrentStep(plan.configs().isEmpty() || autoApproved ? null : 1); item.setTotalSteps(plan.stepCount() == 0 ? null : plan.stepCount());
    item.setCurrentApproverName(autoApproved ? null : currentApproverNames(plan, 0)); item.setMatchedRuleText(ruleTextWithRuntime(plan));
    if (autoApproved) { item.setApproverName("系统自动审批"); item.setApprovalComment("命中自动通过规则"); item.setProcessedAt(OffsetDateTime.now()); }
    ApprovalRequest saved = approvalRepository.save(item);
    createRuntimeNodes(saved, plan);
    return saved;
  }

  private ApprovalContext approvalContext(ApprovalRequest item) {
    return new ApprovalContext(item.getApprovalType().name(), amount(item.getAmount()), item.getDepartmentName(), item.getBusinessType(),
        item.getProjectCode(), item.getSupplierRisk(), item.getCustomerLevel());
  }

  private int completedApprovals(UUID approvalId) {
    return (int) actionRepository.findByApprovalIdOrderByCreatedAtAsc(approvalId).stream()
        .filter(item -> item.getDecision() == ApprovalStatus.APPROVED)
        .filter(item -> item.getActionType() == null || "APPROVE".equals(item.getActionType()) || "ADD_SIGN_APPROVE".equals(item.getActionType()))
        .count();
  }

  private boolean canCurrentUserApprove(ApprovalRequest item) {
    try {
      return approvalFlowSecurity.canApprove(approvalContext(item), completedApprovals(item.getId()), item.getDelegatedUserId(), item.getApprovalConfigVersion());
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  private String currentApproverNames(ApprovalPlan plan, int completedApprovals) {
    if (plan.configs().isEmpty()) return null;
    int step = "SEQUENTIAL".equals(plan.mode()) ? completedApprovals + 1 : 1;
    return plan.configs().stream()
        .filter(item -> !"SEQUENTIAL".equals(plan.mode()) || item.getSequenceNo() == step)
        .map(this::assigneeName)
        .distinct()
        .collect(Collectors.joining("、"));
  }

  private void createRuntimeNodes(ApprovalRequest approval, ApprovalPlan plan) {
    OffsetDateTime now = OffsetDateTime.now();
    List<ApprovalRuntimeNode> nodes = new ArrayList<>();
    for (var config : plan.configs()) {
      for (RuntimeAssignee assignee : resolveRuntimeAssignees(approval, config)) {
        ApprovalRuntimeNode node = new ApprovalRuntimeNode();
        node.setApprovalId(approval.getId());
        node.setStepNo(config.getSequenceNo());
        node.setApprovalMode(plan.mode());
        node.setStepPolicy(config.getStepPolicy() == null ? "ANY_APPROVE" : config.getStepPolicy());
        node.setAssigneeType(assignee.type());
        node.setAssigneeId(assignee.id());
        node.setAssigneeName(assignee.name());
        node.setSourceType(config.getAssigneeType());
        node.setSourceValue(config.getDynamicAssignee() == null ? config.getAssigneeType() : config.getDynamicAssignee());
        node.setConditionText(conditionText(config));
        node.setSlaHours(config.getSlaHours());
        node.setDueAt(config.getSlaHours() == null ? null : now.plusHours(config.getSlaHours()));
        node.setEscalationRoleId(config.getEscalationRoleId());
        if ("AUTO".equals(config.getAssigneeType())) {
          node.setNodeStatus("APPROVED");
          node.setAssigneeName("系统自动审批");
          node.setApproverName("系统自动审批");
          node.setApprovalComment("命中自动通过规则");
          node.setCompletedAt(now);
        }
        nodes.add(node);
      }
    }
    runtimeNodeRepository.saveAll(nodes);
  }

  private List<RuntimeAssignee> resolveRuntimeAssignees(ApprovalRequest approval, com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig config) {
    if ("USER".equals(config.getAssigneeType())) return userRepository.findById(config.getUserId()).filter(SystemUser::isEnabled)
        .map(user -> List.of(new RuntimeAssignee("USER", user.getId(), user.getDisplayName()))).orElseGet(List::of);
    if ("ROLE".equals(config.getAssigneeType())) return roleRepository.findById(config.getRoleId())
        .map(role -> List.of(new RuntimeAssignee("ROLE", role.getId(), role.getName()))).orElseGet(List::of);
    if ("AUTO".equals(config.getAssigneeType())) return List.of(new RuntimeAssignee("AUTO", null, "系统自动审批"));
    List<RuntimeAssignee> exact = resolveExactDynamicAssignee(approval, config.getDynamicAssignee());
    if (!exact.isEmpty()) return exact;
    return dynamicRoleCodes(config.getDynamicAssignee()).stream()
        .map(code -> roleRepository.findByCodeAndTenantId(code, "default"))
        .filter(java.util.Optional::isPresent)
        .map(java.util.Optional::get)
        .findFirst()
        .map(role -> List.of(new RuntimeAssignee("ROLE", role.getId(), role.getName())))
        .orElseGet(List::of);
  }

  private List<RuntimeAssignee> resolveExactDynamicAssignee(ApprovalRequest approval, String dynamicAssignee) {
    String name = switch (dynamicAssignee == null ? "" : dynamicAssignee) {
      case "PROJECT_MANAGER" -> projectRepository.findByCode(approval.getProjectCode()).map(Project::getManagerName).orElse(null);
      case "CUSTOMER_OWNER" -> customerRepository.findByCode(approval.getSourceNo()).map(item -> item.getOwnerName()).orElse(null);
      case "DEPARTMENT_LEADER", "DIRECT_MANAGER" -> organizationRepository.findFirstByName(approval.getDepartmentName()).map(item -> item.getLeaderName()).orElse(null);
      default -> null;
    };
    if (name == null || name.isBlank()) return List.of();
    return userRepository.findByDisplayNameAndEnabledTrue(name).stream()
        .map(user -> new RuntimeAssignee("USER", user.getId(), user.getDisplayName()))
        .toList();
  }

  private List<String> dynamicRoleCodes(String value) {
    return switch (value == null ? "" : value) {
      case "FINANCE_MANAGER" -> List.of("FINANCE_MANAGER", "FINANCE_DIRECTOR", "CFO", "ADMIN");
      case "PROCUREMENT_MANAGER" -> List.of("PROCUREMENT_MANAGER", "PURCHASE_MANAGER", "ADMIN");
      case "HR_MANAGER" -> List.of("HR_MANAGER", "HR_ADMIN", "ADMIN");
      case "PROJECT_MANAGER" -> List.of("PROJECT_MANAGER", "PROJECT_DIRECTOR", "ADMIN");
      case "CUSTOMER_OWNER" -> List.of("SALES_MANAGER", "CRM_MANAGER", "ADMIN");
      case "DEPARTMENT_LEADER", "DIRECT_MANAGER" -> List.of("DEPARTMENT_MANAGER", "MANAGER", "ADMIN");
      default -> List.of("ADMIN");
    };
  }

  private List<ApprovalRuntimeNode> currentRuntimeNodes(ApprovalRequest approval) {
    ensureRuntimeNodes(approval);
    Integer step = approval.getCurrentStep() == null ? nextPendingStep(approval.getId()) : approval.getCurrentStep();
    List<ApprovalRuntimeNode> nodes = runtimeNodeRepository.findByApprovalIdAndStepNoOrderByCreatedAtAsc(approval.getId(), step == null ? 1 : step).stream()
        .filter(item -> "PENDING".equals(item.getNodeStatus()))
        .toList();
    if (nodes.isEmpty()) throw new BusinessException("当前审批节点不存在或已处理");
    return nodes;
  }

  private void ensureRuntimeNodes(ApprovalRequest approval) {
    if (!runtimeNodeRepository.findByApprovalIdOrderByStepNoAscCreatedAtAsc(approval.getId()).isEmpty()) return;
    ApprovalPlan plan = approvalFlowSecurity.resolve(approvalContext(approval), approval.getApprovalConfigVersion());
    createRuntimeNodes(approval, plan);
    if (approval.getCurrentStep() == null) approval.setCurrentStep(nextPendingStep(approval.getId()));
    if (approval.getCurrentApproverName() == null) approval.setCurrentApproverName(currentRuntimeApproverNames(approval.getId(), approval.getCurrentStep()));
    approvalRepository.save(approval);
  }

  private void requireRuntimeApprover(List<ApprovalRuntimeNode> nodes, UUID delegatedUserId) {
    if (delegatedUserId != null && delegatedUserId.equals(approvalFlowSecurity.currentUserId())) return;
    if (nodes.stream().noneMatch(this::runtimeNodeMatchesCurrentUser)) throw new org.springframework.security.access.AccessDeniedException("当前账号不是该审批节点处理人");
  }

  private ApprovalRuntimeNode selectRuntimeNode(List<ApprovalRuntimeNode> nodes, UUID delegatedUserId) {
    if (delegatedUserId != null && delegatedUserId.equals(approvalFlowSecurity.currentUserId())) return nodes.get(0);
    return nodes.stream().filter(this::runtimeNodeMatchesCurrentUser).findFirst().orElse(nodes.get(0));
  }

  private boolean runtimeNodeMatchesCurrentUser(ApprovalRuntimeNode node) {
    UUID currentUserId = approvalFlowSecurity.currentUserId();
    if (currentUserId == null) return false;
    if ("USER".equals(node.getAssigneeType())) return currentUserId.equals(node.getAssigneeId());
    if ("ROLE".equals(node.getAssigneeType())) return userRepository.findDetailById(currentUserId)
        .map(user -> user.getRoles().stream().map(SystemRole::getId).anyMatch(node.getAssigneeId()::equals))
        .orElse(false);
    return false;
  }

  private boolean stepSatisfied(List<ApprovalRuntimeNode> nodes) {
    String policy = nodes.stream().map(ApprovalRuntimeNode::getStepPolicy).filter(v -> v != null).findFirst().orElse("ANY_APPROVE");
    long approved = nodes.stream().filter(item -> "APPROVED".equals(item.getNodeStatus())).count();
    if ("ALL_APPROVE".equals(policy)) return approved >= nodes.size();
    if ("MAJORITY_APPROVE".equals(policy)) return approved * 2 > nodes.size();
    return approved > 0;
  }

  private boolean noPendingRuntimeNodes(UUID approvalId) {
    return runtimeNodeRepository.findByApprovalIdAndNodeStatusOrderByStepNoAscCreatedAtAsc(approvalId, "PENDING").isEmpty();
  }

  private Integer nextPendingStep(UUID approvalId) {
    return runtimeNodeRepository.findByApprovalIdAndNodeStatusOrderByStepNoAscCreatedAtAsc(approvalId, "PENDING").stream()
        .map(ApprovalRuntimeNode::getStepNo).min(Integer::compareTo).orElse(null);
  }

  private String currentRuntimeApproverNames(UUID approvalId, Integer stepNo) {
    if (stepNo == null) return null;
    return runtimeNodeRepository.findByApprovalIdAndStepNoOrderByCreatedAtAsc(approvalId, stepNo).stream()
        .filter(item -> "PENDING".equals(item.getNodeStatus()))
        .map(ApprovalRuntimeNode::getAssigneeName)
        .distinct()
        .collect(Collectors.joining("、"));
  }

  private String conditionText(com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig item) {
    List<String> parts = new ArrayList<>();
    parts.add(item.getConditionType());
    if (item.getMinAmount() != null || item.getMaxAmount() != null) parts.add("金额 " + (item.getMinAmount() == null ? "0" : item.getMinAmount()) + "-" + (item.getMaxAmount() == null ? "不限" : item.getMaxAmount()));
    if (item.getDepartmentName() != null) parts.add("部门 " + item.getDepartmentName());
    if (item.getBusinessType() != null) parts.add("业务 " + item.getBusinessType());
    if (item.getProjectCode() != null) parts.add("项目 " + item.getProjectCode());
    if (item.getSupplierRisk() != null) parts.add("供应商 " + item.getSupplierRisk());
    if (item.getCustomerLevel() != null) parts.add("客户 " + item.getCustomerLevel());
    return String.join(" · ", parts);
  }

  private String assigneeName(com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig item) {
    if ("ROLE".equals(item.getAssigneeType())) {
      return roleRepository.findById(item.getRoleId()).map(role -> role.getName()).orElse("已删除角色");
    }
    if ("DYNAMIC".equals(item.getAssigneeType())) return dynamicAssigneeName(item.getDynamicAssignee());
    if ("AUTO".equals(item.getAssigneeType())) return "自动通过";
    return userRepository.findById(item.getUserId()).map(user -> user.getDisplayName()).orElse("已删除用户");
  }

  private void validatePlan(ApprovalPlan plan) {
    for (var item : plan.configs()) {
      if ("ROLE".equals(item.getAssigneeType()) && item.getRoleId() != null && userRepository.countByRoles_Id(item.getRoleId()) == 0) {
        throw new BusinessException("审批流配置存在空角色：" + assigneeName(item) + "，请先给角色分配成员");
      }
      if (!"AUTO".equals(item.getAssigneeType()) && !"DYNAMIC".equals(item.getAssigneeType()) && currentSingleAssigneeMissing(item)) {
        throw new BusinessException("审批流配置存在无效审批对象，请检查人员/角色是否仍然存在");
      }
    }
  }

  private boolean currentSingleAssigneeMissing(com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig item) {
    if ("USER".equals(item.getAssigneeType())) return item.getUserId() == null || !userRepository.existsById(item.getUserId());
    if ("ROLE".equals(item.getAssigneeType())) return item.getRoleId() == null || !roleRepository.existsById(item.getRoleId());
    return false;
  }

  private boolean isAutoApprovalPlan(ApprovalPlan plan) {
    return !plan.configs().isEmpty() && plan.configs().stream().allMatch(item -> "AUTO".equals(item.getAssigneeType()) && "APPROVE".equals(item.getAutoAction()));
  }

  private String ruleTextWithRuntime(ApprovalPlan plan) {
    StringBuilder builder = new StringBuilder(plan.ruleText()).append(" · V").append(plan.versionNo());
    plan.configs().stream().map(com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig::getSlaHours).filter(v -> v != null).min(Integer::compareTo)
        .ifPresent(value -> builder.append(" · SLA ").append(value).append("小时"));
    plan.configs().stream().map(this::escalationRoleName).filter(value -> value != null).findFirst()
        .ifPresent(value -> builder.append(" · 超时升级至").append(value));
    return builder.toString();
  }

  private String escalationRoleName(com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig item) {
    return item.getEscalationRoleId() == null ? null : roleRepository.findById(item.getEscalationRoleId()).map(role -> role.getName()).orElse("已删除角色");
  }

  private String planSnapshot(ApprovalPlan plan) {
    String steps = plan.configs().stream()
        .map(item -> "{\"step\":" + item.getSequenceNo() + ",\"assignee\":\"" + json(assigneeName(item)) + "\",\"condition\":\"" + json(item.getConditionType()) + "\"}")
        .collect(Collectors.joining(","));
    return "{\"version\":" + plan.versionNo() + ",\"mode\":\"" + json(plan.mode()) + "\",\"ruleText\":\"" + json(ruleTextWithRuntime(plan)) + "\",\"steps\":[" + steps + "]}";
  }

  private String json(String value) {
    if (value == null) return "";
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String dynamicAssigneeName(String value) {
    return Map.of(
        "DEPARTMENT_LEADER", "部门负责人",
        "DIRECT_MANAGER", "直属上级",
        "PROJECT_MANAGER", "项目经理",
        "CUSTOMER_OWNER", "客户负责人",
        "FINANCE_MANAGER", "财务经理",
        "PROCUREMENT_MANAGER", "采购经理",
        "HR_MANAGER", "人事经理"
    ).getOrDefault(value, "动态审批人");
  }

  private void saveRuntimeAction(UUID approvalId, String actionType, String operatorName, String comment, int stepNo) {
    ApprovalAction action = new ApprovalAction();
    action.setApprovalId(approvalId);
    action.setDecision(ApprovalStatus.PENDING);
    action.setOperatorId(approvalFlowSecurity.currentUserId());
    action.setOperatorName(operatorName);
    action.setComment(comment);
    action.setActionType(actionType);
    action.setStepNo(stepNo);
    actionRepository.save(action);
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

  private void processDeleteApproval(ApprovalRequest approval) {
    if (approval.getStatus() == ApprovalStatus.APPROVED) {
      entityManager.createNativeQuery("""
          UPDATE sys_soft_delete_records
          SET status = 'APPROVED',
              approved_by = ?2,
              approved_at = ?3,
              updated_at = ?3
          WHERE approval_id = ?1 AND status = 'PENDING'
          """)
          .setParameter(1, approval.getId())
          .setParameter(2, approval.getApproverName())
          .setParameter(3, OffsetDateTime.now())
          .executeUpdate();
    } else if (approval.getStatus() == ApprovalStatus.REJECTED) {
      entityManager.createNativeQuery("""
          UPDATE sys_soft_delete_records
          SET status = 'RESTORED',
              restored_by = ?2,
              restored_at = ?3,
              updated_at = ?3
          WHERE approval_id = ?1 AND status = 'PENDING'
          """)
          .setParameter(1, approval.getId())
          .setParameter(2, approval.getApproverName())
          .setParameter(3, OffsetDateTime.now())
          .executeUpdate();
    }
  }

  public int getUnreadNotificationCount() {
    return (int) notificationRepository.countByReadFalse();
  }

  public int refreshNotifications() {
    long count = approvalRepository.findAll().stream()
        .filter(a -> a.getStatus() == com.company.ops.api.modules.office.domain.ApprovalStatus.PENDING)
        .count();
    return (int) count;
  }

  @Transactional
  public int scanApprovalSla() {
    OffsetDateTime now = OffsetDateTime.now();
    List<ApprovalRuntimeNode> overdue = runtimeNodeRepository.findByNodeStatusAndDueAtBeforeOrderByDueAtAsc("PENDING", now);
    int touched = 0;
    for (ApprovalRuntimeNode node : overdue) {
      if (node.getRemindedAt() == null) {
        approvalRepository.findById(node.getApprovalId()).ifPresent(approval ->
            notify("APPROVAL_SLA", "审批临近/已超时", approval.getTitle() + " 当前节点：" + node.getAssigneeName(), "APPROVAL", approval.getId()));
        node.setRemindedAt(now);
        touched++;
      }
      if (node.getEscalationRoleId() != null && node.getEscalatedAt() == null) {
        roleRepository.findById(node.getEscalationRoleId()).ifPresent(role -> {
          node.setAssigneeType("ROLE");
          node.setAssigneeId(role.getId());
          node.setAssigneeName(role.getName());
          node.setNodeStatus("PENDING");
          node.setEscalatedAt(now);
          approvalRepository.findById(node.getApprovalId()).ifPresent(approval -> {
            approval.setCurrentApproverName(currentRuntimeApproverNames(approval.getId(), approval.getCurrentStep()));
            approval.setMatchedRuleText((approval.getMatchedRuleText() == null ? "" : approval.getMatchedRuleText()) + " · 已升级至" + role.getName());
            approvalRepository.save(approval);
            notify("APPROVAL_ESCALATED", "审批已超时升级", approval.getTitle() + " 升级至 " + role.getName(), "APPROVAL", approval.getId());
          });
        });
        touched++;
      }
    }
    runtimeNodeRepository.saveAll(overdue);
    return touched;
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<AuditResponse> listAudits(org.springframework.data.domain.Pageable pageable) {
    return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
        .map(item -> new AuditResponse(
            item.getId(),
            item.getUsername(),
            item.getHttpMethod(),
            item.getRequestPath(),
            item.getResponseStatus(),
            item.getClientIp(),
            item.getDurationMs(),
            item.getQueryString(),
            item.getOperationType(),
            item.getBizModule(),
            item.getBizObject(),
            item.getCreatedAt()
        ));
        
  }


  private void notify(String type, String title, String content, String relatedType, UUID relatedId) {
    SystemNotification item = new SystemNotification(); item.setType(type); item.setTitle(title); item.setContent(content);
    item.setRelatedType(relatedType); item.setRelatedId(relatedId); item.setRead(false); notificationRepository.save(item);
  }
  private ApprovalResponse toApproval(ApprovalRequest item) { return new ApprovalResponse(item.getId(), item.getCode(), item.getApprovalType(), item.getTitle(), item.getSourceNo(), amount(item.getAmount()), item.getStatus(), item.getApplicantName(), item.getContent(), item.getApproverName(), item.getApprovalComment(), item.getProcessedAt(), item.getCreatedAt(), item.getDepartmentName(), item.getBusinessType(), item.getProjectCode(), item.getSupplierRisk(), item.getCustomerLevel(), item.getApprovalMode(), item.getCurrentStep(), item.getTotalSteps(), item.getCurrentApproverName(), item.getMatchedRuleText(), item.getApprovalConfigVersion(), item.getApprovalPlanSnapshot(), runtimeNodeRepository.findByApprovalIdOrderByStepNoAscCreatedAtAsc(item.getId()).stream().map(this::toRuntimeNode).toList(), actionRepository.findByApprovalIdOrderByCreatedAtAsc(item.getId()).stream().map(action -> new ApprovalActionResponse(action.getId(), action.getDecision(), action.getOperatorName(), action.getComment(), action.getActionType(), action.getStepNo(), action.getCreatedAt())).toList()); }
  private ApprovalRuntimeNodeResponse toRuntimeNode(ApprovalRuntimeNode item) { return new ApprovalRuntimeNodeResponse(item.getId(), item.getStepNo(), item.getNodeStatus(), item.getApprovalMode(), item.getStepPolicy(), item.getAssigneeType(), item.getAssigneeId(), item.getAssigneeName(), item.getSourceType(), item.getSourceValue(), item.getConditionText(), item.getSlaHours(), item.getDueAt(), item.getRemindedAt(), item.getEscalatedAt(), item.getCompletedAt(), item.getApproverName(), item.getApprovalComment()); }
  private ExpenseResponse toExpense(ExpenseClaim item, Project project, WorkOrder order) { return new ExpenseResponse(item.getId(), item.getCode(), item.getClaimantId(), item.getClaimantName(), item.getProjectId(), project == null ? null : project.getCode(), item.getWorkOrderId(), order == null ? null : order.getCode(), item.getExpenseType(), item.getAmount(), item.getExpenseDate(), item.getDescription(), item.getStatus(), item.getApprovalRequestId()); }
  private OutsourceResponse toOutsource(OutsourceOrder item, Supplier supplier, Project project, WorkOrder order) { return new OutsourceResponse(item.getId(), item.getCode(), item.getSupplierId(), supplier == null ? null : supplier.getName(), item.getProjectId(), project == null ? null : project.getCode(), item.getWorkOrderId(), order == null ? null : order.getCode(), item.getServiceType(), item.getDescription(), item.getAmount(), item.getPlannedDate(), item.getStatus(), item.getApprovalRequestId(), item.getAcceptanceNote()); }
  private DocumentResponse toDocument(DocumentFile item) { return new DocumentResponse(item.getId(), item.getBizType(), item.getBizId(), item.getFileName(), item.getContentType(), item.getSizeBytes(), item.getCreatedAt()); }
  private NotificationResponse toNotification(SystemNotification item) { return new NotificationResponse(item.getId(), item.getType(), item.getTitle(), item.getContent(), item.getRelatedType(), item.getRelatedId(), item.isRead(), item.getReadAt(), item.getCreatedAt()); }
  private BigDecimal amount(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
  private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  private Map<UUID, Supplier> supplierMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : supplierRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(Supplier::getId, Function.identity())); }
  private Map<UUID, Project> projectMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : projectRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(Project::getId, Function.identity())); }
  private Map<UUID, WorkOrder> workOrderMap(List<UUID> ids) { return ids.isEmpty() ? Map.of() : workOrderRepository.findAllById(ids.stream().distinct().toList()).stream().collect(Collectors.toMap(WorkOrder::getId, Function.identity())); }

  private record RuntimeAssignee(String type, UUID id, String name) {}
}
