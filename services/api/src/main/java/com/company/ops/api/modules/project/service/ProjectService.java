package com.company.ops.api.modules.project.service;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
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
import com.company.ops.api.modules.project.dto.ProjectBudgetItemRequest;
import com.company.ops.api.modules.project.dto.ProjectBudgetItemResponse;
import com.company.ops.api.modules.project.dto.ProjectCostEntryResponse;
import com.company.ops.api.modules.project.dto.ProjectDetailResponse;
import com.company.ops.api.modules.project.dto.ProjectProfitabilityResponse;
import com.company.ops.api.modules.project.dto.ProjectManagerOption;
import com.company.ops.api.modules.project.dto.ProjectResponse;
import com.company.ops.api.modules.project.dto.ProjectStageRecordResponse;
import com.company.ops.api.modules.project.repository.ProjectBudgetItemRepository;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.repository.ProjectStageRecordRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.HashSet;
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
  private final DataScopeService dataScopeService;
  private final ServiceContractRepository contractRepository;
  private final DeleteGovernanceService deleteGovernanceService;
  private final SystemUserRepository userRepository;
  private final CodeGenerator codeGenerator;
  @PersistenceContext
  private EntityManager entityManager;

  public ProjectService(
      ServiceContractRepository contractRepository,
      ProjectRepository projectRepository,
      ProjectBudgetItemRepository budgetRepository,
      ProjectCostEntryRepository costRepository,
      ProjectCostLedgerService costLedger,
      ProjectStageRecordRepository stageRecordRepository,
      CustomerRepository customerRepository,
      DataScopeService dataScopeService,
      DeleteGovernanceService deleteGovernanceService,
      SystemUserRepository userRepository,
      CodeGenerator codeGenerator
  ) {
    this.projectRepository = projectRepository;
    this.budgetRepository = budgetRepository;
    this.costRepository = costRepository;
    this.costLedger = costLedger;
    this.stageRecordRepository = stageRecordRepository;
    this.customerRepository = customerRepository;
    this.dataScopeService = dataScopeService;
    this.contractRepository = contractRepository;
    this.deleteGovernanceService = deleteGovernanceService;
    this.userRepository = userRepository;
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
    validateBudgetItems(request.budgetItems());

    BigDecimal budgetAmount = request.budgetItems().stream()
        .map(ProjectBudgetItemRequest::plannedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    ServiceContract contract = null;
    if (request.contractId() != null) {
      contract = contractRepository.findById(request.contractId())
          .orElseThrow(() -> new BusinessException("关联合同不存在"));
      validateContractForProject(contract, request);
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

    List<ProjectBudgetItem> items = request.budgetItems().stream().map(item -> {
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
  public ProjectDetailResponse assignManager(UUID id, AssignProjectManagerRequest request) {
    Project project = requireVisibleProject(id);
    if (project.getApprovalStatus() != ProjectApprovalStatus.APPROVED) {
      throw new BusinessException("立项审批通过后才能分配项目经理");
    }
    SystemUser manager = requireProjectManager(request.managerUserId());
    project.setManagerUserId(manager.getId());
    project.setManagerName(manager.getDisplayName());
    UserPrincipal principal = dataScopeService.currentPrincipal();
    project.setManagerAssignedByUserId(principal == null ? null : principal.id());
    project.setManagerAssignedByName(principal == null ? "system" : principal.displayName());
    project.setManagerAssignedAt(OffsetDateTime.now());
    project.setManagerAssignmentComment(request.comment());
    return toDetail(projectRepository.save(project));
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
    project.setApprovalStatus(request.decision());
    project.setApprovalComment(request.comment());
    UserPrincipal principal = dataScopeService.currentPrincipal();
    project.setApproverUserId(principal == null ? null : principal.id());
    project.setApproverName(principal == null ? "system" : principal.displayName());
    project.setApprovedAt(request.decision() == ProjectApprovalStatus.APPROVED ? OffsetDateTime.now() : null);
    return toDetail(projectRepository.save(project));
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
      validateCloseout(project);
      project.setExecutionStatus(ProjectExecutionStatus.CLOSED);
      project.setStatusComment(request.comment());
      project.setStatusChangedAt(OffsetDateTime.now());
    }

    ProjectStage fromStage = project.getStage();
    int progress = progressForStage(request.targetStage());
    project.setStage(request.targetStage());
    project.setProgress(progress);
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
    project.setExecutionStatus(target);
    project.setStatusComment(request.comment());
    project.setStatusChangedAt(OffsetDateTime.now());
    return toDetail(projectRepository.save(project));
  }

  @Transactional
  public void deleteProject(UUID id) {
    Project project = requireManageableProject(id);
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
    return new ProjectResponse(
        project.getId(),
        project.getCustomerId(),
        customerName,
        project.getContractId(),
        contract == null ? null : contract.getCode(),
        contract == null ? null : contract.getProjectName(),
        contract == null ? null : contract.getStatus(),
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
        project.getWarrantyEndDate()
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
    if (projectRepository.existsByContractId(contract.getId())) {
      throw new BusinessException("该合同已关联项目，不能重复创建");
    }
    if (amount(contract.getAmount()).compareTo(amount(request.contractAmount())) != 0) {
      throw new BusinessException("项目合同金额必须与关联合同金额一致");
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

  private void validateCloseout(Project project) {
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
