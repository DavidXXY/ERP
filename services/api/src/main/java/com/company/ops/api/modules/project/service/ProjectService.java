package com.company.ops.api.modules.project.service;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.domain.ContractKind;
import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.domain.QuoteCostRequest;
import com.company.ops.api.modules.crm.domain.QuoteCostStatus;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.QuoteCostRequestRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.collaboration.domain.ProjectHandover;
import com.company.ops.api.modules.collaboration.repository.ProjectHandoverRepository;
import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.CloseoutReviewStatus;
import com.company.ops.api.modules.project.domain.ProjectCloseoutReview;
import com.company.ops.api.modules.project.domain.ProjectBudgetItem;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import com.company.ops.api.modules.project.domain.ProjectExecutionStatus;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.domain.ProjectStageRecord;
import com.company.ops.api.modules.project.dto.AdvanceProjectStageRequest;
import com.company.ops.api.modules.project.dto.AssignProjectManagerRequest;
import com.company.ops.api.modules.project.dto.ChangeProjectExecutionStatusRequest;
import com.company.ops.api.modules.project.dto.CreateProjectCostRequest;
import com.company.ops.api.modules.project.dto.CreateProjectRequest;
import com.company.ops.api.modules.project.dto.ProcessProjectApprovalRequest;
import com.company.ops.api.modules.project.dto.PrepareChildProjectRequest;
import com.company.ops.api.modules.project.dto.CloseoutReviewRequest;
import com.company.ops.api.modules.project.dto.ProcessCloseoutReviewRequest;
import com.company.ops.api.modules.project.dto.ProjectCloseoutReviewResponse;
import com.company.ops.api.modules.project.dto.RollbackProjectStageRequest;
import com.company.ops.api.modules.project.dto.UpdateProjectCostRequest;
import com.company.ops.api.modules.project.dto.UpdateProjectRequest;
import com.company.ops.api.modules.project.dto.ProjectBudgetItemRequest;
import com.company.ops.api.modules.project.dto.ProjectBudgetItemResponse;
import com.company.ops.api.modules.project.dto.ProjectCostEntryResponse;
import com.company.ops.api.modules.project.dto.ProjectDetailResponse;
import com.company.ops.api.modules.project.dto.ProjectProfitabilityResponse;
import com.company.ops.api.modules.project.dto.ProjectManagerOption;
import com.company.ops.api.modules.project.dto.ProjectResponse;
import com.company.ops.api.modules.project.dto.ProjectStageRecordResponse;
import com.company.ops.api.modules.project.repository.ProjectBudgetItemRepository;
import com.company.ops.api.modules.project.repository.ProjectCloseoutReviewRepository;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.repository.ProjectStageRecordRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.company.ops.api.common.util.MoneyUtils.amount;

@Service
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectBudgetItemRepository budgetRepository;
  private final ProjectCostEntryRepository costRepository;
  private final ProjectCostLedgerService costLedger;
  private final ProjectStageRecordRepository stageRecordRepository;
  private final CustomerRepository customerRepository;
  private final QuoteCostRequestRepository quoteCostRepository;
  private final ProjectHandoverRepository handoverRepository;
  private final ProjectCloseoutReviewRepository closeoutReviewRepository;
  private final DataScopeService dataScopeService;
  private final ServiceContractRepository contractRepository;
  private final ReceivableRepository receivableRepository;
  private final DeleteGovernanceService deleteGovernanceService;
  private final SystemUserRepository userRepository;
  private final SystemNotificationRepository notificationRepository;
  private final CodeGenerator codeGenerator;
  @PersistenceContext
  private EntityManager entityManager;

  public ProjectService(
      ServiceContractRepository contractRepository,
      ReceivableRepository receivableRepository,
      QuoteCostRequestRepository quoteCostRepository,
      ProjectHandoverRepository handoverRepository,
      ProjectCloseoutReviewRepository closeoutReviewRepository,
      ProjectRepository projectRepository,
      ProjectBudgetItemRepository budgetRepository,
      ProjectCostEntryRepository costRepository,
      ProjectCostLedgerService costLedger,
      ProjectStageRecordRepository stageRecordRepository,
      CustomerRepository customerRepository,
      DataScopeService dataScopeService,
      DeleteGovernanceService deleteGovernanceService,
      SystemUserRepository userRepository,
      SystemNotificationRepository notificationRepository,
      CodeGenerator codeGenerator
  ) {
    this.projectRepository = projectRepository;
    this.budgetRepository = budgetRepository;
    this.costRepository = costRepository;
    this.costLedger = costLedger;
    this.stageRecordRepository = stageRecordRepository;
    this.customerRepository = customerRepository;
    this.quoteCostRepository = quoteCostRepository;
    this.handoverRepository = handoverRepository;
    this.closeoutReviewRepository = closeoutReviewRepository;
    this.dataScopeService = dataScopeService;
    this.contractRepository = contractRepository;
    this.receivableRepository = receivableRepository;
    this.deleteGovernanceService = deleteGovernanceService;
    this.userRepository = userRepository;
    this.notificationRepository = notificationRepository;
    this.codeGenerator = codeGenerator;
  }

  @Transactional(readOnly = true)
  public List<ProjectManagerOption> managerOptions() {
    return userRepository.findEnabledByRoleCode("PROJECT_MANAGER").stream()
        .map(user -> new ProjectManagerOption(user.getId(), user.getUsername(), user.getDisplayName()))
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<ProjectResponse> listProjects(
      String keyword,
      ProjectApprovalStatus approvalStatus,
      ProjectStage stage,
      ProjectExecutionStatus executionStatus,
      Pageable pageable
  ) {
    Specification<Project> specification = projectSpecification(
        keyword, approvalStatus, stage, executionStatus, deleteGovernanceService.hiddenIds("PROJECT"));
    Page<Project> projectPage = projectRepository.findAll(specification, pageable);
    Map<UUID, String> customerNames = loadCustomerNames(projectPage.getContent());
    Map<UUID, ServiceContract> contracts = loadContracts(projectPage.getContent());
    return projectPage.map(project -> toResponse(
        project, customerNames.get(project.getCustomerId()), contracts.get(project.getContractId())));
  }

  @Transactional(readOnly = true)
  public Page<ProjectDetailResponse> listPortfolio(
      String keyword,
      ProjectApprovalStatus approvalStatus,
      ProjectStage stage,
      ProjectExecutionStatus executionStatus,
      Pageable pageable
  ) {
    Page<Project> projectPage = projectRepository.findAll(projectSpecification(
        keyword, approvalStatus, stage, executionStatus, deleteGovernanceService.hiddenIds("PROJECT")), pageable);
    List<Project> rows = projectPage.getContent();
    if (rows.isEmpty()) return new PageImpl<>(List.of(), pageable, projectPage.getTotalElements());
    List<UUID> projectIds = rows.stream().map(Project::getId).toList();
    Map<UUID, List<ProjectBudgetItem>> budgets = budgetRepository.findByProjectIdIn(projectIds).stream()
        .collect(Collectors.groupingBy(ProjectBudgetItem::getProjectId));
    Map<UUID, List<ProjectCostEntry>> costs = costRepository.findByProjectIdIn(projectIds).stream()
        .collect(Collectors.groupingBy(ProjectCostEntry::getProjectId));
    Map<UUID, List<ProjectStageRecord>> stages = stageRecordRepository.findByProjectIdIn(projectIds).stream()
        .collect(Collectors.groupingBy(ProjectStageRecord::getProjectId));
    Map<UUID, String> customerNames = loadCustomerNames(rows);
    Map<UUID, ServiceContract> contracts = loadContracts(rows);
    List<ProjectDetailResponse> content = rows.stream().map(project -> toDetail(
        project,
        customerNames.get(project.getCustomerId()),
        contracts.get(project.getContractId()),
        budgets.getOrDefault(project.getId(), List.of()),
        costs.getOrDefault(project.getId(), List.of()),
        stages.getOrDefault(project.getId(), List.of()))).toList();
    return new PageImpl<>(content, pageable, projectPage.getTotalElements());
  }

  @Transactional(readOnly = true)
  public ProjectDetailResponse getProject(UUID id) {
    Project project = requireVisibleProject(id);
    return toDetail(project);
  }

  @Transactional(readOnly = true)
  public List<ProjectProfitabilityResponse> profitability() {
    Set<UUID> hiddenIds = deleteGovernanceService.hiddenIds("PROJECT");
    boolean allScope = dataScopeService.hasAllDataScope();
    Set<UUID> visibleUserIds = dataScopeService.visibleUserIds();
    Set<String> visibleNames = dataScopeService.visibleOwnerNames();
    boolean canApprove = dataScopeService.hasAuthority("project:approve");
    List<Project> projects = projectRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(project -> !hiddenIds.contains(project.getId()))
        .filter(project -> canViewProject(project, allScope, visibleUserIds, visibleNames, canApprove))
        .toList();
    Map<UUID, String> customerNames = loadCustomerNames(projects);
    return projects.stream()
        .map(project -> toProfitability(project, customerNames.get(project.getCustomerId())))
        .toList();
  }

  @Transactional
  public ProjectDetailResponse createProject(CreateProjectRequest request) {
    Customer customer = customerRepository.findById(request.customerId())
        .orElseThrow(() -> new BusinessException("客户不存在"));
    if (request.plannedEndDate().isBefore(request.plannedStartDate())) {
      throw new BusinessException("计划结束日期不能早于开始日期");
    }
    List<ProjectBudgetItemRequest> budgetItems = request.budgetItems();
    if (budgetItems.isEmpty() && request.quoteId() != null) {
      budgetItems = budgetItemsFromQuote(request.quoteId(), request.customerId());
    }
    validateBudgetItems(budgetItems);

    BigDecimal budgetAmount = budgetItems.stream()
        .map(ProjectBudgetItemRequest::plannedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    ServiceContract contract = null;
    if (request.contractId() != null) {
      contract = contractRepository.findById(request.contractId())
          .orElseThrow(() -> new BusinessException("关联合同不存在"));
      validateContractForProject(contract, request);
    }
    Project parentProject = null;
    if (request.parentProjectId() != null) {
      parentProject = requireVisibleProject(request.parentProjectId());
      validateParentProject(parentProject, request);
    }
    SystemUser manager = request.managerUserId() == null ? null : requireProjectManager(request.managerUserId());
    String projectCode = resolveProjectCode(request.code(), contract);
    if (projectRepository.existsByCode(projectCode)) {
      throw new BusinessException("项目编码已存在");
    }

    Project project = new Project();
    project.setCustomerId(request.customerId());
    project.setSalesOwnerUserId(customer.getOwnerUserId());
    project.setSalesOrganizationId(dataScopeService.organizationIdForUser(customer.getOwnerUserId()));
    project.setCode(projectCode);
    project.setParentProjectId(parentProject == null ? null : parentProject.getId());
    if (contract != null) {
      project.setContractId(request.contractId());
    }
    project.setName(request.name());
    project.setProjectType(request.projectType());
    project.setManagerUserId(manager == null ? null : manager.getId());
    project.setManagerName(manager == null ? "待项目管理部门分配" : manager.getDisplayName());
    project.setSiteAddress(request.siteAddress());
    project.setContractAmount(request.contractAmount());
    project.setPlannedStartDate(request.plannedStartDate());
    project.setPlannedEndDate(request.plannedEndDate());
    project.setStage(ProjectStage.ENTRY);
    project.setExecutionStatus(ProjectExecutionStatus.ACTIVE);
    project.setApprovalStatus(ProjectApprovalStatus.PENDING);
    project.setBudgetAmount(budgetAmount);
    project.setActualCost(BigDecimal.ZERO);
    project.setProgress(0);
    project.setWarrantyEndDate(request.warrantyEndDate());
    Project saved = projectRepository.save(project);

    List<ProjectBudgetItem> items = budgetItems.stream().map(item -> {
      ProjectBudgetItem entity = new ProjectBudgetItem();
      entity.setProjectId(saved.getId());
      entity.setCategory(item.category());
      entity.setPlannedAmount(item.plannedAmount());
      entity.setRemark(item.remark());
      return entity;
    }).toList();
    budgetRepository.saveAll(items);
    return toDetail(saved);
  }

  @Transactional
  public Project createChildProjectFromOrder(
      ServiceContract order, Project parentProject, BigDecimal frameworkAmount
  ) {
    Project existing = projectRepository.findLatestByContractId(order.getId()).orElse(null);
    if (existing != null) return existing;
    if (order.getContractKind() != ContractKind.CHILD_ORDER || order.getParentContractId() == null) {
      throw new BusinessException("只有框架子订单可以自动生成子项目");
    }
    if (!order.getParentContractId().equals(parentProject.getContractId())) {
      throw new BusinessException("子订单与框架项目不匹配");
    }
    if (!order.getCustomerId().equals(parentProject.getCustomerId())) {
      throw new BusinessException("框架项目与子项目客户不一致");
    }
    if (parentProject.getParentProjectId() != null) {
      throw new BusinessException("框架订单必须关联一级项目");
    }
    if (parentProject.getExecutionStatus() == ProjectExecutionStatus.CANCELLED
        || parentProject.getExecutionStatus() == ProjectExecutionStatus.CLOSED) {
      throw new BusinessException("已取消或已关闭的框架项目不能生成子项目");
    }

    Customer customer = customerRepository.findById(order.getCustomerId())
        .orElseThrow(() -> new BusinessException("客户不存在"));
    String projectCode = resolveProjectCode(null, order);
    if (projectRepository.existsByCode(projectCode)) {
      throw new BusinessException("子项目编码已存在");
    }

    Project child = new Project();
    child.setCustomerId(order.getCustomerId());
    child.setContractId(order.getId());
    child.setParentProjectId(parentProject.getId());
    child.setSalesOwnerUserId(customer.getOwnerUserId());
    child.setSalesOrganizationId(dataScopeService.organizationIdForUser(customer.getOwnerUserId()));
    child.setCode(projectCode);
    child.setName(order.getProjectName());
    child.setProjectType(parentProject.getProjectType());
    child.setManagerUserId(parentProject.getManagerUserId());
    child.setManagerName(parentProject.getManagerUserId() == null
        ? "待项目管理部门分配" : parentProject.getManagerName());
    if (parentProject.getManagerUserId() != null) {
      child.setManagerAssignedByName("系统自动继承");
      child.setManagerAssignedAt(OffsetDateTime.now());
      child.setManagerAssignmentComment("继承框架项目 " + parentProject.getCode() + " 的项目经理");
    }
    child.setSiteAddress(parentProject.getSiteAddress());
    child.setContractAmount(amount(order.getAmount()));
    child.setPlannedStartDate(order.getStartDate());
    child.setPlannedEndDate(order.getEndDate());
    child.setStage(ProjectStage.ENTRY);
    child.setExecutionStatus(ProjectExecutionStatus.ACTIVE);
    child.setApprovalStatus(ProjectApprovalStatus.PENDING);
    child.setBudgetAmount(BigDecimal.ZERO);
    child.setActualCost(BigDecimal.ZERO);
    child.setProgress(0);
    Project saved = projectRepository.save(child);

    parentProject.setContractAmount(amount(frameworkAmount));
    projectRepository.save(parentProject);
    notifyChildProjectCreated(saved, parentProject, order);
    return saved;
  }

  @Transactional
  public void synchronizeProjectFromContract(ServiceContract contract, BigDecimal hierarchyAmount) {
    Project linked = projectRepository.findLatestByContractId(contract.getId()).orElse(null);
    if (linked == null) return;
    linked.setName(contract.getProjectName());
    linked.setContractAmount(contract.getContractKind() == ContractKind.FRAMEWORK
        ? amount(hierarchyAmount) : amount(contract.getAmount()));
    linked.setPlannedStartDate(contract.getStartDate());
    linked.setPlannedEndDate(contract.getEndDate());
    projectRepository.save(linked);

    if (contract.getContractKind() == ContractKind.CHILD_ORDER && linked.getParentProjectId() != null) {
      Project parent = projectRepository.findByIdForUpdate(linked.getParentProjectId())
          .orElseThrow(() -> new BusinessException("框架项目不存在"));
      parent.setContractAmount(amount(hierarchyAmount));
      projectRepository.save(parent);
    }
  }

  @Transactional
  public ProjectDetailResponse assignManager(UUID id, AssignProjectManagerRequest request) {
    Project project = requireVisibleProject(id);
    if (project.getApprovalStatus() == ProjectApprovalStatus.REJECTED) {
      throw new BusinessException("已驳回项目请先完善资料并重新提交");
    }
    if (project.getExecutionStatus() == ProjectExecutionStatus.CANCELLED
        || project.getExecutionStatus() == ProjectExecutionStatus.CLOSED) {
      throw new BusinessException("已取消或已结项的项目不能变更项目经理");
    }
    SystemUser manager = requireProjectManager(request.managerUserId());
    UUID previousManagerId = project.getManagerUserId();
    project.setManagerUserId(manager.getId());
    project.setManagerName(manager.getDisplayName());
    UserPrincipal principal = dataScopeService.currentPrincipal();
    project.setManagerAssignedByUserId(principal == null ? null : principal.id());
    project.setManagerAssignedByName(principal == null ? "system" : principal.displayName());
    project.setManagerAssignedAt(OffsetDateTime.now());
    project.setManagerAssignmentComment(request.comment());
    Project saved = projectRepository.save(project);
    notifyManagerChanged(saved, previousManagerId, manager);

    if (project.getParentProjectId() == null && Boolean.TRUE.equals(request.syncChildProjects())) {
      for (Project child : projectRepository.findByParentProjectId(project.getId())) {
        if (child.getExecutionStatus() == ProjectExecutionStatus.CANCELLED
            || child.getExecutionStatus() == ProjectExecutionStatus.CLOSED) continue;
        UUID childPreviousManagerId = child.getManagerUserId();
        child.setManagerUserId(manager.getId());
        child.setManagerName(manager.getDisplayName());
        child.setManagerAssignedByUserId(principal == null ? null : principal.id());
        child.setManagerAssignedByName(principal == null ? "system" : principal.displayName());
        child.setManagerAssignedAt(OffsetDateTime.now());
        child.setManagerAssignmentComment("随框架项目 " + project.getCode() + " 同步负责人");
        notifyManagerChanged(projectRepository.save(child), childPreviousManagerId, manager);
      }
    }
    return toDetail(saved);
  }

  @Transactional
  public ProjectDetailResponse updateProject(UUID id, UpdateProjectRequest request) {
    Project project = requireVisibleProject(id);
    if (project.getApprovalStatus() == ProjectApprovalStatus.APPROVED) {
      throw new BusinessException("已审批项目不能直接修改，请通过预算变更或合同变更流程调整");
    }
    if (request.plannedEndDate().isBefore(request.plannedStartDate())) {
      throw new BusinessException("计划结束日期不能早于开始日期");
    }
    validateBudgetItems(request.budgetItems());
    BigDecimal budgetAmount = request.budgetItems().stream()
        .map(ProjectBudgetItemRequest::plannedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    ServiceContract contract = project.getContractId() == null ? null
        : contractRepository.findById(project.getContractId()).orElse(null);
    if (contract != null) {
      if (request.plannedStartDate().isBefore(contract.getStartDate())
          || request.plannedEndDate().isAfter(contract.getEndDate())) {
        throw new BusinessException("项目计划周期必须在合同有效期内");
      }
      if (contract.getContractKind() != ContractKind.FRAMEWORK
          && amount(contract.getAmount()).compareTo(amount(request.contractAmount())) != 0) {
        throw new BusinessException("项目合同金额必须与关联合同金额一致");
      }
    }

    project.setName(request.name());
    project.setSiteAddress(request.siteAddress().trim());
    project.setContractAmount(amount(request.contractAmount()));
    project.setPlannedStartDate(request.plannedStartDate());
    project.setPlannedEndDate(request.plannedEndDate());
    project.setWarrantyEndDate(request.warrantyEndDate());
    project.setBudgetAmount(budgetAmount);
    project.setApprovalStatus(ProjectApprovalStatus.PENDING);
    project.setApprovalComment(null);
    project.setApproverName(null);
    project.setApproverUserId(null);
    project.setApprovedAt(null);
    projectRepository.save(project);

    budgetRepository.deleteByProjectId(project.getId());
    budgetRepository.saveAll(request.budgetItems().stream().map(item -> {
      ProjectBudgetItem entity = new ProjectBudgetItem();
      entity.setProjectId(project.getId());
      entity.setCategory(item.category());
      entity.setPlannedAmount(item.plannedAmount());
      entity.setRemark(item.remark());
      return entity;
    }).toList());
    return toDetail(project);
  }

  @Transactional
  public ProjectDetailResponse prepareChildProject(UUID id, PrepareChildProjectRequest request) {
    Project project = requireVisibleProject(id);
    if (project.getParentProjectId() == null) {
      throw new BusinessException("只有框架子项目需要完善立项资料");
    }
    BigDecimal budgetAmount = request.budgetItems().stream()
        .map(ProjectBudgetItemRequest::plannedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException("子项目至少需要一项有效预算");
    }
    return updateProject(id, new UpdateProjectRequest(
        project.getName(),
        request.siteAddress(),
        project.getContractAmount(),
        request.plannedStartDate(),
        request.plannedEndDate(),
        request.warrantyEndDate(),
        request.budgetItems()
    ));
  }

  @Transactional
  public ProjectDetailResponse processApproval(UUID id, ProcessProjectApprovalRequest request) {
    Project project = requireVisibleProject(id);
    if (project.getApprovalStatus() != ProjectApprovalStatus.PENDING) {
      throw new BusinessException("该立项审批已处理");
    }
    if (request.decision() == ProjectApprovalStatus.PENDING) {
      throw new BusinessException("请选择通过或驳回");
    }
    if (request.decision() == ProjectApprovalStatus.APPROVED && project.getParentProjectId() != null) {
      if (project.getManagerUserId() == null) throw new BusinessException("请先分配项目经理");
      if (amount(project.getBudgetAmount()).compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException("请先完善子项目预算");
      }
      if (project.getSiteAddress() == null || project.getSiteAddress().isBlank()) {
        throw new BusinessException("请先完善子项目现场地址");
      }
    }
    project.setApprovalStatus(request.decision());
    project.setApprovalComment(request.comment());
    UserPrincipal principal = dataScopeService.currentPrincipal();
    project.setApproverUserId(principal == null ? null : principal.id());
    project.setApproverName(principal == null ? "system" : principal.displayName());
    project.setApprovedAt(request.decision() == ProjectApprovalStatus.APPROVED ? OffsetDateTime.now() : null);
    Project saved = projectRepository.save(project);
    notifyApprovalDecision(saved, request.decision(), request.comment());
    return toDetail(saved);
  }

  private void notifyApprovalDecision(Project project, ProjectApprovalStatus decision, String comment) {
    String title = decision == ProjectApprovalStatus.APPROVED
        ? "立项审批已通过" : "立项审批被驳回";
    String content = project.getCode() + " · " + project.getName() + " "
        + (decision == ProjectApprovalStatus.APPROVED
            ? "已通过审批，可进入执行" : "已被驳回，请完善资料后重新提交")
        + (comment == null || comment.isBlank() ? "" : "；审批意见：" + comment);
    Set<UUID> recipients = new LinkedHashSet<>();
    if (project.getManagerUserId() != null) recipients.add(project.getManagerUserId());
    if (project.getSalesOwnerUserId() != null) recipients.add(project.getSalesOwnerUserId());
    for (UUID userId : recipients) {
      saveTargetedProjectNotification(project, userId, title, content,
          "PROJECT_APPROVAL_DECISION:" + project.getId() + ":" + userId + ":" + decision);
    }
  }

  @Transactional
  public ProjectDetailResponse advanceStage(UUID id, AdvanceProjectStageRequest request) {
    Project project = requireManageableProject(id);
    requireApproved(project);
    if (project.getExecutionStatus() != ProjectExecutionStatus.ACTIVE) {
      throw new BusinessException("只有执行中的项目可以推进阶段");
    }
    ProjectStage expected = nextStage(project.getStage());
    if (expected == null) throw new BusinessException("项目已关闭，不能继续推进");
    if (request.targetStage() != expected) {
      throw new BusinessException("项目阶段必须按顺序推进，下一阶段应为" + expected.name());
    }
    if (request.targetStage() == ProjectStage.WARRANTY && project.getWarrantyEndDate() == null) {
      throw new BusinessException("进入质保阶段前必须填写质保截止日期");
    }
    if (request.targetStage() == ProjectStage.CLOSED) {
      requireCloseoutApproved(project);
      validateFrameworkHierarchyClosed(project);
      validateCloseout(project);
      project.setExecutionStatus(ProjectExecutionStatus.CLOSED);
      project.setStatusComment(request.comment());
      project.setStatusChangedAt(OffsetDateTime.now());
      project.setActualEndDate(LocalDate.now());
    }

    ProjectStage fromStage = project.getStage();
    int progress = progressForStage(request.targetStage());
    project.setStage(request.targetStage());
    project.setProgress(progress);
    if (request.targetStage() == ProjectStage.CONSTRUCTION && project.getActualStartDate() == null) {
      project.setActualStartDate(LocalDate.now());
    }
    projectRepository.save(project);

    ProjectStageRecord record = new ProjectStageRecord();
    record.setProjectId(project.getId());
    record.setFromStage(fromStage);
    record.setToStage(request.targetStage());
    record.setProgress(progress);
    record.setComment(request.comment());
    record.setOperatorName(currentOperatorName());
    record.setChangedAt(OffsetDateTime.now());
    stageRecordRepository.save(record);
    return toDetail(project);
  }

  @Transactional
  public ProjectDetailResponse createCost(UUID id, CreateProjectCostRequest request) {
    Project project = requireManageableProject(id);
    requireApproved(project);
    if (project.getExecutionStatus() != ProjectExecutionStatus.ACTIVE) {
      throw new BusinessException("只有执行中的项目可以归集成本");
    }

    costLedger.record(project.getId(), request.category(), request.sourceType(), request.sourceNo(),
        request.description(), request.amount(), request.incurredDate());
    return toDetail(project);
  }

  @Transactional
  public ProjectDetailResponse changeExecutionStatus(UUID id, ChangeProjectExecutionStatusRequest request) {
    Project project = requireManageableProject(id);
    requireApproved(project);
    ProjectExecutionStatus current = project.getExecutionStatus();
    ProjectExecutionStatus target = request.status();
    boolean allowed = (current == ProjectExecutionStatus.ACTIVE
        && (target == ProjectExecutionStatus.PAUSED || target == ProjectExecutionStatus.CANCELLED))
        || (current == ProjectExecutionStatus.PAUSED
        && (target == ProjectExecutionStatus.ACTIVE || target == ProjectExecutionStatus.CANCELLED));
    if (!allowed) {
      throw new BusinessException("不允许从" + current.name() + "变更为" + target.name());
    }
    if (target == ProjectExecutionStatus.CANCELLED) {
      validateFrameworkHierarchyClosed(project);
    }
    project.setExecutionStatus(target);
    project.setStatusComment(request.comment());
    project.setStatusChangedAt(OffsetDateTime.now());
    return toDetail(projectRepository.save(project));
  }

  @Transactional
  public ProjectDetailResponse updateCost(UUID id, UUID costId, UpdateProjectCostRequest request) {
    Project project = requireManageableProject(id);
    requireApproved(project);
    if (project.getExecutionStatus() != ProjectExecutionStatus.ACTIVE) {
      throw new BusinessException("只有执行中的项目可以更正成本");
    }
    costLedger.update(project.getId(), costId, request.category(), request.description(),
        request.amount(), request.incurredDate());
    return toDetail(project);
  }

  @Transactional
  public ProjectDetailResponse deleteCost(UUID id, UUID costId) {
    Project project = requireManageableProject(id);
    requireApproved(project);
    if (project.getExecutionStatus() != ProjectExecutionStatus.ACTIVE) {
      throw new BusinessException("只有执行中的项目可以删除成本");
    }
    costLedger.delete(project.getId(), costId);
    return toDetail(project);
  }

  @Transactional
  public ProjectDetailResponse rollbackStage(UUID id, RollbackProjectStageRequest request) {
    Project project = requireManageableProject(id);
    requireApproved(project);
    if (project.getExecutionStatus() != ProjectExecutionStatus.ACTIVE) {
      throw new BusinessException("只有执行中的项目可以回退阶段");
    }
    ProjectStage target = previousStage(project.getStage());
    if (target == null) {
      throw new BusinessException("当前阶段不能回退");
    }
    ProjectStage fromStage = project.getStage();
    int progress = progressForStage(target);
    project.setStage(target);
    project.setProgress(progress);
    projectRepository.save(project);

    ProjectStageRecord record = new ProjectStageRecord();
    record.setProjectId(project.getId());
    record.setFromStage(fromStage);
    record.setToStage(target);
    record.setProgress(progress);
    record.setComment(request.comment() + "（阶段回退）");
    record.setOperatorName(currentOperatorName());
    record.setChangedAt(OffsetDateTime.now());
    stageRecordRepository.save(record);
    return toDetail(project);
  }

  @Transactional
  public ProjectCloseoutReviewResponse requestCloseout(UUID id, CloseoutReviewRequest request) {
    Project project = requireManageableProject(id);
    requireApproved(project);
    if (project.getStage() != ProjectStage.WARRANTY) {
      throw new BusinessException("只有质保阶段的项目可以提交结项申请");
    }
    if (project.getExecutionStatus() != ProjectExecutionStatus.ACTIVE) {
      throw new BusinessException("只有执行中的项目可以提交结项申请");
    }
    validateCloseout(project);
    closeoutReviewRepository.findFirstByProjectIdOrderByCreatedAtDesc(id)
        .filter(review -> review.getStatus() == CloseoutReviewStatus.PENDING)
        .ifPresent(review -> { throw new BusinessException("已有待复核的结项申请，请先完成复核"); });

    ProjectCloseoutReview review = new ProjectCloseoutReview();
    review.setProjectId(id);
    review.setStatus(CloseoutReviewStatus.PENDING);
    review.setRequestComment(request.comment());
    review.setRequestedBy(currentOperatorName());
    review.setRequestedAt(OffsetDateTime.now());
    return toCloseoutReview(closeoutReviewRepository.save(review));
  }

  @Transactional
  public ProjectCloseoutReviewResponse reviewCloseout(UUID id, ProcessCloseoutReviewRequest request) {
    Project project = requireVisibleProject(id);
    ProjectCloseoutReview review = closeoutReviewRepository.findFirstByProjectIdOrderByCreatedAtDesc(id)
        .orElseThrow(() -> new BusinessException("尚未提交结项申请"));
    if (review.getStatus() != CloseoutReviewStatus.PENDING) {
      throw new BusinessException("该结项申请已复核");
    }
    if (request.decision() != CloseoutReviewStatus.APPROVED
        && request.decision() != CloseoutReviewStatus.REJECTED) {
      throw new BusinessException("请选择通过或驳回");
    }
    review.setStatus(request.decision());
    review.setReviewComment(request.comment());
    review.setReviewedBy(currentOperatorName());
    review.setReviewedAt(OffsetDateTime.now());
    ProjectCloseoutReview saved = closeoutReviewRepository.save(review);
    if (request.decision() == CloseoutReviewStatus.APPROVED) {
      validateCloseout(project);
      notifyCloseoutReview(project, saved);
    }
    return toCloseoutReview(saved);
  }

  @Transactional(readOnly = true)
  public ProjectCloseoutReviewResponse getCloseoutReview(UUID id) {
    requireVisibleProject(id);
    return closeoutReviewRepository.findFirstByProjectIdOrderByCreatedAtDesc(id)
        .map(this::toCloseoutReview)
        .orElse(null);
  }

  @Transactional
  public void deleteProject(UUID id) {
    Project project = requireManageableProject(id);
    if (projectRepository.existsByParentProjectId(id)) {
      throw new BusinessException("框架项目仍有关联子项目，不能删除");
    }
    deleteGovernanceService.requestSoftDelete("PROJECT", id, project.getCode() + " · " + project.getName());
  }

  private ProjectDetailResponse toDetail(Project project) {
    String customerName = customerRepository.findById(project.getCustomerId())
        .map(Customer::getName)
        .orElse(null);
    ServiceContract contract = project.getContractId() == null ? null : contractRepository.findById(project.getContractId()).orElse(null);
    return toDetail(project, customerName, contract);
  }

  private ProjectDetailResponse toDetail(Project project, String customerName, ServiceContract contract) {
    List<ProjectBudgetItem> budgetItems = budgetRepository.findByProjectIdOrderByCategoryAsc(project.getId());
    List<ProjectCostEntry> costEntries = costRepository.findByProjectIdOrderByIncurredDateDescCreatedAtDesc(project.getId());
    List<ProjectStageRecord> stageRecords = stageRecordRepository.findByProjectIdOrderByChangedAtDesc(project.getId());
    return toDetail(project, customerName, contract, budgetItems, costEntries, stageRecords);
  }

  private ProjectDetailResponse toDetail(
      Project project,
      String customerName,
      ServiceContract contract,
      List<ProjectBudgetItem> budgetItems,
      List<ProjectCostEntry> costEntries,
      List<ProjectStageRecord> stageRecords
  ) {
    Map<ProjectCostCategory, BigDecimal> actualByCategory = new EnumMap<>(ProjectCostCategory.class);
    costEntries.forEach(item -> actualByCategory.merge(item.getCategory(), item.getAmount(), BigDecimal::add));
    List<ProjectBudgetItemResponse> budgets = budgetItems.stream()
        .map(item -> {
          BigDecimal actual = actualByCategory.getOrDefault(item.getCategory(), BigDecimal.ZERO);
          return new ProjectBudgetItemResponse(
              item.getId(),
              item.getCategory(),
              item.getPlannedAmount(),
              actual,
              item.getPlannedAmount().subtract(actual),
              item.getRemark()
          );
        })
        .toList();
    List<ProjectCostEntryResponse> costs = costEntries.stream()
        .map(item -> new ProjectCostEntryResponse(
            item.getId(),
            item.getCategory(),
            item.getSourceType(),
            item.getSourceNo(),
            item.getDescription(),
            item.getAmount(),
            item.getIncurredDate()
        ))
        .toList();
    List<ProjectStageRecordResponse> stages = stageRecords.stream()
        .map(item -> new ProjectStageRecordResponse(
            item.getId(),
            item.getFromStage(),
            item.getToStage(),
            item.getProgress(),
            item.getComment(),
            item.getOperatorName(),
            item.getChangedAt()
        ))
        .toList();
    return new ProjectDetailResponse(toResponse(project, customerName, contract), budgets, costs, stages);
  }

  private ProjectResponse toResponse(Project project, String customerName, ServiceContract contract) {
    BigDecimal actualCost = amount(project.getActualCost());
    BigDecimal budgetAmount = amount(project.getBudgetAmount());
    Project parent = project.getParentProjectId() == null ? null
        : projectRepository.findById(project.getParentProjectId()).orElse(null);
    return new ProjectResponse(
        project.getId(),
        project.getCustomerId(),
        customerName,
        project.getContractId(),
        contract == null ? null : contract.getCode(),
        contract == null ? null : contract.getProjectName(),
        contract == null ? null : contract.getStatus(),
        project.getParentProjectId(),
        parent == null ? null : parent.getCode(),
        parent == null ? null : parent.getName(),
        Math.toIntExact(projectRepository.countByParentProjectId(project.getId())),
        project.getCode(),
        project.getName(),
        project.getProjectType(),
        project.getManagerUserId(),
        project.getManagerName(),
        project.getManagerAssignedByUserId(),
        project.getManagerAssignedByName(),
        project.getManagerAssignedAt(),
        project.getManagerAssignmentComment(),
        project.getSiteAddress(),
        amount(project.getContractAmount()),
        project.getPlannedStartDate(),
        project.getPlannedEndDate(),
        project.getStage(),
        project.getApprovalStatus(),
        project.getApprovalComment(),
        project.getApproverName(),
        project.getApprovedAt(),
        project.getApproverUserId(),
        project.getExecutionStatus(),
        project.getStatusComment(),
        project.getStatusChangedAt(),
        budgetAmount,
        actualCost,
        amount(project.getContractAmount()).subtract(actualCost),
        budgetAmount.subtract(actualCost),
        project.getProgress(),
        project.getWarrantyEndDate(),
        project.getActualStartDate(),
        project.getActualEndDate()
    );
  }

  private boolean canViewProject(Project project) {
    return canViewProject(project, dataScopeService.hasAllDataScope(), dataScopeService.visibleUserIds(),
        dataScopeService.visibleOwnerNames(), dataScopeService.hasAuthority("project:approve"));
  }

  private boolean canViewProject(Project project, boolean allScope, Set<UUID> visibleUserIds,
                                 Set<String> visibleNames, boolean canApprove) {
    if (allScope) return true;
    if (project.getManagerUserId() != null && visibleUserIds.contains(project.getManagerUserId())) return true;
    if (project.getManagerUserId() == null && visibleNames.contains(project.getManagerName())) return true;
    return canApprove
        && (project.getApprovalStatus() == ProjectApprovalStatus.PENDING
            || project.getManagerName() == null
            || project.getManagerName().startsWith("待"));
  }

  private ProjectProfitabilityResponse toProfitability(Project project, String customerName) {
    BigDecimal contractAmount = amount(project.getContractAmount());
    BigDecimal budgetAmount = amount(project.getBudgetAmount());
    BigDecimal actualCost = amount(project.getActualCost());
    BigDecimal grossMargin = contractAmount.subtract(actualCost);
    BigDecimal grossMarginRate = contractAmount.compareTo(BigDecimal.ZERO) == 0
        ? BigDecimal.ZERO
        : grossMargin.multiply(BigDecimal.valueOf(100)).divide(contractAmount, 2, RoundingMode.HALF_UP);
    BigDecimal budgetUsageRate = budgetAmount.compareTo(BigDecimal.ZERO) == 0
        ? BigDecimal.ZERO
        : actualCost.multiply(BigDecimal.valueOf(100)).divide(budgetAmount, 2, RoundingMode.HALF_UP);
    String riskLevel = "LOW";
    String riskMessage = "项目毛利和预算使用正常";
    if (budgetAmount.compareTo(BigDecimal.ZERO) > 0 && actualCost.compareTo(budgetAmount) > 0) {
      riskLevel = "HIGH";
      riskMessage = "实际成本已超过预算";
    } else if (grossMargin.compareTo(BigDecimal.ZERO) < 0) {
      riskLevel = "HIGH";
      riskMessage = "项目已亏损";
    } else if (budgetUsageRate.compareTo(BigDecimal.valueOf(85)) >= 0) {
      riskLevel = "MEDIUM";
      riskMessage = "预算使用率较高";
    }
    return new ProjectProfitabilityResponse(
        project.getId(), project.getCode(), project.getName(), customerName, project.getStage(), project.getApprovalStatus(),
        contractAmount, budgetAmount, actualCost, grossMargin, grossMarginRate, budgetUsageRate, riskLevel, riskMessage
    );
  }

  private Project requireProject(UUID id) {
    return projectRepository.findById(id)
        .orElseThrow(() -> new BusinessException("项目不存在"));
  }

  private Project requireVisibleProject(UUID id) {
    Project project = requireProject(id);
    if (deleteGovernanceService.isHidden("PROJECT", id)) throw new BusinessException("项目不存在");
    if (!canViewProject(project)) throw new BusinessException("无权查看该项目");
    return project;
  }

  private Project requireManageableProject(UUID id) {
    Project project = requireVisibleProject(id);
    if (dataScopeService.hasAllDataScope()) return project;
    boolean assignedVisible = project.getManagerUserId() != null
        && dataScopeService.visibleUserIds().contains(project.getManagerUserId());
    boolean legacyVisible = project.getManagerUserId() == null
        && dataScopeService.canViewOwner(project.getManagerName());
    if (!assignedVisible && !legacyVisible) throw new BusinessException("无权管理该项目");
    return project;
  }

  private void requireApproved(Project project) {
    if (project.getApprovalStatus() != ProjectApprovalStatus.APPROVED) {
      throw new BusinessException("项目经理分配后才能执行");
    }
  }

  private int progressForStage(ProjectStage stage) {
    return switch (stage) {
      case INITIATED, BIDDING, ENTRY -> 0;
      case CONSTRUCTION -> 20;
      case COMMISSIONING -> 45;
      case INITIAL_ACCEPTANCE -> 65;
      case FINAL_ACCEPTANCE -> 85;
      case WARRANTY, CLOSED -> 100;
    };
  }

  private ProjectStage nextStage(ProjectStage current) {
    return switch (current) {
      case INITIATED -> ProjectStage.BIDDING;
      case BIDDING -> ProjectStage.ENTRY;
      case ENTRY -> ProjectStage.CONSTRUCTION;
      case CONSTRUCTION -> ProjectStage.COMMISSIONING;
      case COMMISSIONING -> ProjectStage.INITIAL_ACCEPTANCE;
      case INITIAL_ACCEPTANCE -> ProjectStage.FINAL_ACCEPTANCE;
      case FINAL_ACCEPTANCE -> ProjectStage.WARRANTY;
      case WARRANTY -> ProjectStage.CLOSED;
      case CLOSED -> null;
    };
  }

  private ProjectStage previousStage(ProjectStage current) {
    return switch (current) {
      case INITIATED, ENTRY -> null;
      case BIDDING -> ProjectStage.INITIATED;
      case CONSTRUCTION -> ProjectStage.ENTRY;
      case COMMISSIONING -> ProjectStage.CONSTRUCTION;
      case INITIAL_ACCEPTANCE -> ProjectStage.COMMISSIONING;
      case FINAL_ACCEPTANCE -> ProjectStage.INITIAL_ACCEPTANCE;
      case WARRANTY -> ProjectStage.FINAL_ACCEPTANCE;
      case CLOSED -> null;
    };
  }

  private void validateBudgetItems(List<ProjectBudgetItemRequest> items) {
    Set<ProjectCostCategory> categories = new HashSet<>();
    for (ProjectBudgetItemRequest item : items) {
      if (!categories.add(item.category())) {
        throw new BusinessException("同一预算分类不能重复");
      }
    }
  }

  private Map<UUID, String> loadCustomerNames(List<Project> projects) {
    List<UUID> customerIds = projects.stream()
        .map(Project::getCustomerId)
        .filter(id -> id != null)
        .distinct()
        .toList();
    if (customerIds.isEmpty()) {
      return Map.of();
    }
    return customerRepository.findAllById(customerIds).stream()
        .collect(Collectors.toMap(Customer::getId, Customer::getName, (left, right) -> left));
  }

  private Map<UUID, ServiceContract> loadContracts(List<Project> projects) {
    List<UUID> contractIds = projects.stream()
        .map(Project::getContractId)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .toList();
    if (contractIds.isEmpty()) return Map.of();
    return contractRepository.findAllById(contractIds).stream()
        .collect(Collectors.toMap(ServiceContract::getId, contract -> contract));
  }

  private Specification<Project> projectSpecification(
      String keyword,
      ProjectApprovalStatus approvalStatus,
      ProjectStage stage,
      ProjectExecutionStatus executionStatus,
      Set<UUID> hiddenIds
  ) {
    Set<UUID> visibleUserIds = dataScopeService.visibleUserIds();
    Set<String> visibleNames = dataScopeService.visibleOwnerNames();
    boolean allScope = dataScopeService.hasAllDataScope();
    boolean canApprove = dataScopeService.hasAuthority("project:approve");
    String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    return (root, query, cb) -> {
      List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
      if (!normalizedKeyword.isEmpty()) {
        String like = "%" + normalizedKeyword + "%";
        predicates.add(cb.or(
            cb.like(cb.lower(root.get("code")), like),
            cb.like(cb.lower(root.get("name")), like),
            cb.like(cb.lower(root.get("managerName")), like)));
      }
      if (approvalStatus != null) predicates.add(cb.equal(root.get("approvalStatus"), approvalStatus));
      if (stage != null) predicates.add(cb.equal(root.get("stage"), stage));
      if (executionStatus != null) predicates.add(cb.equal(root.get("executionStatus"), executionStatus));
      if (!hiddenIds.isEmpty()) predicates.add(cb.not(root.get("id").in(hiddenIds)));
      if (!allScope) {
        List<jakarta.persistence.criteria.Predicate> scopes = new ArrayList<>();
        if (!visibleUserIds.isEmpty()) scopes.add(root.get("managerUserId").in(visibleUserIds));
        if (!visibleNames.isEmpty()) {
          scopes.add(cb.and(cb.isNull(root.get("managerUserId")), root.get("managerName").in(visibleNames)));
        }
        if (canApprove) {
          scopes.add(cb.equal(root.get("approvalStatus"), ProjectApprovalStatus.PENDING));
          scopes.add(cb.isNull(root.get("managerUserId")));
        }
        predicates.add(scopes.isEmpty() ? cb.disjunction() : cb.or(scopes.toArray(jakarta.persistence.criteria.Predicate[]::new)));
      }
      return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }

  private void validateContractForProject(ServiceContract contract, CreateProjectRequest request) {
    if (!contract.getCustomerId().equals(request.customerId())) {
      throw new BusinessException("关联合同与项目客户不一致");
    }
    if (contract.getStatus() != ContractStatus.ACTIVE) {
      throw new BusinessException("只有生效中的合同可以创建项目");
    }
    ContractKind kind = contract.getContractKind() == null ? ContractKind.STANDARD : contract.getContractKind();
    if (kind == ContractKind.FRAMEWORK && request.parentProjectId() != null) {
      throw new BusinessException("框架订单只能创建一级项目");
    }
    if (kind == ContractKind.CHILD_ORDER && request.parentProjectId() == null) {
      throw new BusinessException("框架子订单必须关联框架项目创建子项目");
    }
    if (projectRepository.existsByContractId(contract.getId())) {
      throw new BusinessException("该合同已关联项目，不能重复创建");
    }
    if (kind != ContractKind.FRAMEWORK
        && amount(contract.getAmount()).compareTo(amount(request.contractAmount())) != 0) {
      throw new BusinessException("项目合同金额必须与关联合同金额一致");
    }
  }

  private void notifyChildProjectCreated(Project child, Project parent, ServiceContract order) {
    Set<UUID> recipients = new LinkedHashSet<>();
    if (parent.getManagerUserId() != null) {
      recipients.add(parent.getManagerUserId());
    } else {
      userRepository.findEnabledByRoleCode("PROJECT_MANAGER").stream()
          .map(SystemUser::getId)
          .filter(java.util.Objects::nonNull)
          .forEach(recipients::add);
    }
    for (UUID userId : recipients) {
      String dedupKey = "FRAMEWORK_CHILD_PROJECT:" + child.getId() + ":" + userId;
      if (notificationRepository.existsByDedupKey(dedupKey)) continue;
      SystemNotification notification = new SystemNotification();
      notification.setType("PROJECT");
      notification.setTitle("框架子项目已创建");
      notification.setContent(parent.getCode() + " / " + child.getCode()
          + " · " + child.getName() + " · 子订单 " + order.getCode()
          + " · 请完善立项资料并推进审批");
      notification.setTargetUserId(userId);
      notification.setRelatedType("PROJECT");
      notification.setRelatedId(child.getId());
      notification.setDedupKey(dedupKey);
      notification.setRead(false);
      notificationRepository.save(notification);
    }
  }

  private void notifyManagerChanged(Project project, UUID previousManagerId, SystemUser manager) {
    String eventKey = project.getManagerAssignedAt() == null
        ? Long.toString(System.currentTimeMillis())
        : Long.toString(project.getManagerAssignedAt().toInstant().toEpochMilli());
    saveTargetedProjectNotification(
        project,
        manager.getId(),
        "项目负责人已分配",
        project.getCode() + " · " + project.getName() + " 已分配给您",
        "PROJECT_MANAGER_ASSIGNED:" + project.getId() + ":" + manager.getId() + ":" + eventKey);
    if (previousManagerId != null && !previousManagerId.equals(manager.getId())) {
      saveTargetedProjectNotification(
          project,
          previousManagerId,
          "项目负责人已变更",
          project.getCode() + " · " + project.getName() + " 已转交给 " + manager.getDisplayName(),
          "PROJECT_MANAGER_REMOVED:" + project.getId() + ":" + previousManagerId + ":" + eventKey);
    }
  }

  private void saveTargetedProjectNotification(
      Project project, UUID userId, String title, String content, String dedupKey
  ) {
    if (notificationRepository.existsByDedupKey(dedupKey)) return;
    SystemNotification notification = new SystemNotification();
    notification.setType("PROJECT");
    notification.setTitle(title);
    notification.setContent(content);
    notification.setTargetUserId(userId);
    notification.setRelatedType("PROJECT");
    notification.setRelatedId(project.getId());
    notification.setDedupKey(dedupKey);
    notification.setRead(false);
    notificationRepository.save(notification);
  }

  private void validateParentProject(Project parent, CreateProjectRequest request) {
    if (!parent.getCustomerId().equals(request.customerId())) {
      throw new BusinessException("父项目与子项目客户不一致");
    }
    if (parent.getParentProjectId() != null) {
      throw new BusinessException("暂不支持三级项目，子项目下不能继续创建项目");
    }
    if (parent.getExecutionStatus() == ProjectExecutionStatus.CANCELLED
        || parent.getExecutionStatus() == ProjectExecutionStatus.CLOSED) {
      throw new BusinessException("已取消或已关闭的项目不能新增子项目");
    }
  }

  private void validateFrameworkHierarchyClosed(Project project) {
    if (project.getParentProjectId() != null) return;
    List<Project> children = projectRepository.findByParentProjectId(project.getId());
    if (children.isEmpty()) return;
    boolean hasOpenChild = children.stream().anyMatch(child ->
        child.getExecutionStatus() != ProjectExecutionStatus.CLOSED
            && child.getExecutionStatus() != ProjectExecutionStatus.CANCELLED);
    if (hasOpenChild) {
      throw new BusinessException("仍有未结项或未取消的子项目，框架项目不能关闭或取消");
    }
    if (project.getContractId() == null) return;
    List<UUID> childContractIds = contractRepository
        .findByParentContractIdOrderByStartDateDesc(project.getContractId()).stream()
        .map(ServiceContract::getId)
        .toList();
    if (childContractIds.isEmpty()) return;
    boolean hasOutstanding = receivableRepository.findByContractIdIn(childContractIds).stream()
        .anyMatch(item -> amount(item.getAmount()).compareTo(amount(item.getSettledAmount())) > 0);
    if (hasOutstanding) {
      throw new BusinessException("仍有子订单应收未结清，框架项目不能关闭或取消");
    }
  }

  private SystemUser requireProjectManager(UUID userId) {
    SystemUser user = userRepository.findDetailById(userId)
        .orElseThrow(() -> new BusinessException("项目经理用户不存在"));
    if (!user.isEnabled()) throw new BusinessException("项目经理用户已停用");
    boolean allowedRole = user.getRoles().stream()
        .anyMatch(role -> "PROJECT_MANAGER".equals(role.getCode()) || "ADMIN".equals(role.getCode()));
    if (!allowedRole) throw new BusinessException("所选用户不具备项目经理角色");
    return user;
  }

  private String currentOperatorName() {
    UserPrincipal principal = dataScopeService.currentPrincipal();
    return principal == null ? "system" : principal.displayName();
  }

  private void requireCloseoutApproved(Project project) {
    ProjectCloseoutReview review = closeoutReviewRepository
        .findFirstByProjectIdOrderByCreatedAtDesc(project.getId()).orElse(null);
    if (review == null || review.getStatus() != CloseoutReviewStatus.APPROVED) {
      throw new BusinessException("请先提交结项申请并完成结项复核，再关闭项目");
    }
  }

  private ProjectCloseoutReviewResponse toCloseoutReview(ProjectCloseoutReview review) {
    return new ProjectCloseoutReviewResponse(
        review.getId(),
        review.getProjectId(),
        review.getStatus(),
        review.getRequestComment(),
        review.getReviewComment(),
        review.getRequestedBy(),
        review.getRequestedAt(),
        review.getReviewedBy(),
        review.getReviewedAt()
    );
  }

  private void notifyCloseoutReview(Project project, ProjectCloseoutReview review) {
    Set<UUID> recipients = new LinkedHashSet<>();
    if (project.getManagerUserId() != null) recipients.add(project.getManagerUserId());
    if (project.getSalesOwnerUserId() != null) recipients.add(project.getSalesOwnerUserId());
    String content = project.getCode() + " · " + project.getName()
        + " 结项申请已通过复核，可在质保阶段推进关闭项目";
    for (UUID userId : recipients) {
      saveTargetedProjectNotification(project, userId, "结项复核已通过", content,
          "PROJECT_CLOSEOUT_APPROVED:" + project.getId() + ":" + userId + ":" + review.getId());
    }
  }

  private List<ProjectBudgetItemRequest> budgetItemsFromQuote(UUID quoteId, UUID customerId) {
    QuoteCostRequest quote = quoteCostRepository.findById(quoteId)
        .orElseThrow(() -> new BusinessException("售前成本核算不存在"));
    if (quote.getStatus() != QuoteCostStatus.APPROVED) {
      throw new BusinessException("只有已审批通过的售前成本核算可以带入预算");
    }
    if (!customerId.equals(quote.getCustomerId())) {
      throw new BusinessException("售前成本核算与项目客户不一致");
    }
    BigDecimal other = amount(quote.getEquipmentCost())
        .add(amount(quote.getRiskReserve()))
        .add(amount(quote.getOtherCost()));
    return List.of(
        new ProjectBudgetItemRequest(ProjectCostCategory.LABOR, amount(quote.getLaborCost()), "售前成本核算 · 人工"),
        new ProjectBudgetItemRequest(ProjectCostCategory.MATERIAL, amount(quote.getMaterialCost()), "售前成本核算 · 材料"),
        new ProjectBudgetItemRequest(ProjectCostCategory.SUBCONTRACT, amount(quote.getSubcontractCost()), "售前成本核算 · 外包"),
        new ProjectBudgetItemRequest(ProjectCostCategory.TRAVEL, amount(quote.getTravelCost()), "售前成本核算 · 差旅"),
        new ProjectBudgetItemRequest(ProjectCostCategory.OTHER, other, "售前成本核算 · 设备/风险/其他")
    );
  }

  private void validateCloseout(Project project) {
    if (handoverRepository.existsByProjectId(project.getId())) {
      ProjectHandover handover = handoverRepository.findByProjectId(project.getId()).orElse(null);
      if (handover == null || !"ACCEPTED".equals(handover.getStatus())) {
        throw new BusinessException("项目交接尚未完成，不能结项");
      }
    }
    long openWorkOrders = nativeCount(
        "SELECT COUNT(*) FROM work_orders WHERE project_id = ?1 AND status NOT IN ('ACCEPTED', 'CANCELLED')",
        project.getId());
    if (openWorkOrders > 0) throw new BusinessException("仍有未验收或未取消的工单，不能结项");
    long openPurchaseOrders = nativeCount(
        "SELECT COUNT(*) FROM procurement_purchase_orders WHERE project_id = ?1 AND status NOT IN ('CLOSED', 'CANCELLED')",
        project.getId());
    if (openPurchaseOrders > 0) throw new BusinessException("仍有未关闭采购订单，不能结项");
    long outstandingPayables = nativeCount(
        """
        SELECT COUNT(*) FROM fin_procurement_payables payable
        WHERE payable.amount > payable.paid_amount
          AND (
            payable.order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id = ?1)
            OR payable.receipt_id IN (
              SELECT receipt.id FROM procurement_goods_receipts receipt
              JOIN procurement_purchase_orders purchase_order ON purchase_order.id = receipt.order_id
              WHERE purchase_order.project_id = ?1
            )
          )
        """,
        project.getId());
    if (outstandingPayables > 0) throw new BusinessException("项目仍有未结清采购应付，不能结项");
    if (project.getContractId() != null) {
      long outstandingReceivables = nativeCount(
          "SELECT COUNT(*) FROM fin_receivables WHERE contract_id = ?1 AND status <> 'SETTLED'",
          project.getContractId());
      if (outstandingReceivables > 0) throw new BusinessException("关联合同仍有未结清应收，不能结项");
    }
  }

  private long nativeCount(String sql, UUID id) {
    Number count = (Number) entityManager.createNativeQuery(sql).setParameter(1, id).getSingleResult();
    return count.longValue();
  }



  private String resolveProjectCode(String requestedCode, ServiceContract contract) {
    if (requestedCode != null && !requestedCode.isBlank()) {
      return requestedCode.trim();
    }
    if (contract != null && contract.getCode() != null) {
      int firstDash = contract.getCode().indexOf('-');
      if (firstDash >= 0 && firstDash < contract.getCode().length() - 1) {
        return "XM" + contract.getCode().substring(firstDash);
      }
    }
    return codeGenerator.generate("PROJECT");
  }
}
