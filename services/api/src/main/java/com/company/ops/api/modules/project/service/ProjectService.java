package com.company.ops.api.modules.project.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectBudgetItem;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.domain.ProjectStageRecord;
import com.company.ops.api.modules.project.dto.AdvanceProjectStageRequest;
import com.company.ops.api.modules.project.dto.CreateProjectCostRequest;
import com.company.ops.api.modules.project.dto.CreateProjectRequest;
import com.company.ops.api.modules.project.dto.ProcessProjectApprovalRequest;
import com.company.ops.api.modules.project.dto.ProjectBudgetItemRequest;
import com.company.ops.api.modules.project.dto.ProjectBudgetItemResponse;
import com.company.ops.api.modules.project.dto.ProjectCostEntryResponse;
import com.company.ops.api.modules.project.dto.ProjectDetailResponse;
import com.company.ops.api.modules.project.dto.ProjectResponse;
import com.company.ops.api.modules.project.dto.ProjectStageRecordResponse;
import com.company.ops.api.modules.project.repository.ProjectBudgetItemRepository;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.repository.ProjectStageRecordRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumMap;
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
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectBudgetItemRepository budgetRepository;
  private final ProjectCostEntryRepository costRepository;
  private final ProjectStageRecordRepository stageRecordRepository;
  private final CustomerRepository customerRepository;
  private final DataScopeService dataScopeService;
  private final ServiceContractRepository contractRepository;

  public ProjectService(
      ServiceContractRepository contractRepository,
      ProjectRepository projectRepository,
      ProjectBudgetItemRepository budgetRepository,
      ProjectCostEntryRepository costRepository,
      ProjectStageRecordRepository stageRecordRepository,
      CustomerRepository customerRepository,
      DataScopeService dataScopeService
  ) {
    this.projectRepository = projectRepository;
    this.budgetRepository = budgetRepository;
    this.costRepository = costRepository;
    this.stageRecordRepository = stageRecordRepository;
    this.customerRepository = customerRepository;
    this.dataScopeService = dataScopeService;
    this.contractRepository = contractRepository;
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> listProjects() {
    List<Project> projects = projectRepository.findAllByOrderByCreatedAtDesc();
    projects = projects.stream().filter(project -> dataScopeService.canViewOwner(project.getManagerName())).toList();
    Map<UUID, String> customerNames = loadCustomerNames(projects);
    return projects.stream()
        .map(project -> toResponse(project, customerNames.get(project.getCustomerId()), null))
        .toList();
  }

  @Transactional(readOnly = true)
  public ProjectDetailResponse getProject(UUID id) {
    Project project = requireProject(id);
    if (!dataScopeService.canViewOwner(project.getManagerName())) throw new BusinessException("无权查看该项目");
    return toDetail(project);
  }

  @Transactional
  public ProjectDetailResponse createProject(CreateProjectRequest request) {
    if (projectRepository.existsByCode(request.code())) {
      throw new BusinessException("项目编码已存在");
    }
    Customer customer = customerRepository.findById(request.customerId())
        .orElseThrow(() -> new BusinessException("客户不存在"));
    if (request.plannedEndDate().isBefore(request.plannedStartDate())) {
      throw new BusinessException("计划结束日期不能早于开始日期");
    }
    validateBudgetItems(request.budgetItems());

    BigDecimal budgetAmount = request.budgetItems().stream()
        .map(ProjectBudgetItemRequest::plannedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    Project project = new Project();
    project.setCustomerId(request.customerId());
    project.setCode(request.code());
    project.setName(request.name());
    project.setProjectType(request.projectType());
    project.setManagerName(request.managerName());
    project.setSiteAddress(request.siteAddress());
    project.setContractAmount(request.contractAmount());
    project.setPlannedStartDate(request.plannedStartDate());
    project.setPlannedEndDate(request.plannedEndDate());
    project.setStage(ProjectStage.INITIATED);
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
  public ProjectDetailResponse processApproval(UUID id, ProcessProjectApprovalRequest request) {
    Project project = requireProject(id);
    if (project.getApprovalStatus() != ProjectApprovalStatus.PENDING) {
      throw new BusinessException("该项目立项已处理");
    }
    if (request.decision() == ProjectApprovalStatus.PENDING) {
      throw new BusinessException("请选择通过或驳回");
    }
    project.setApprovalStatus(request.decision());
    project.setApprovalComment(request.comment());
    project.setApproverName(request.approverName());
    project.setApprovedAt(request.decision() == ProjectApprovalStatus.APPROVED ? OffsetDateTime.now() : null);
    return toDetail(projectRepository.save(project));
  }

  @Transactional
  public ProjectDetailResponse advanceStage(UUID id, AdvanceProjectStageRequest request) {
    Project project = requireProject(id);
    requireApproved(project);
    if (project.getStage() == ProjectStage.CLOSED) {
      throw new BusinessException("项目已关闭，不能继续推进");
    }
    ProjectStage expected = ProjectStage.values()[project.getStage().ordinal() + 1];
    if (request.targetStage() != expected) {
      throw new BusinessException("项目阶段必须按顺序推进，下一阶段应为" + expected.name());
    }
    if (request.progress() < project.getProgress()) {
      throw new BusinessException("项目进度不能回退");
    }
    if (request.targetStage() == ProjectStage.WARRANTY && project.getWarrantyEndDate() == null) {
      throw new BusinessException("进入质保阶段前必须填写质保截止日期");
    }
    if (request.targetStage() == ProjectStage.CLOSED && request.progress() != 100) {
      throw new BusinessException("关闭项目时进度必须为100%");
    }

    ProjectStage fromStage = project.getStage();
    project.setStage(request.targetStage());
    project.setProgress(request.progress());
    projectRepository.save(project);

    ProjectStageRecord record = new ProjectStageRecord();
    record.setProjectId(project.getId());
    record.setFromStage(fromStage);
    record.setToStage(request.targetStage());
    record.setProgress(request.progress());
    record.setComment(request.comment());
    record.setOperatorName(request.operatorName());
    record.setChangedAt(OffsetDateTime.now());
    stageRecordRepository.save(record);
    return toDetail(project);
  }

  @Transactional
  public ProjectDetailResponse createCost(UUID id, CreateProjectCostRequest request) {
    Project project = requireProject(id);
    requireApproved(project);
    if (project.getStage() == ProjectStage.CLOSED) {
      throw new BusinessException("项目已关闭，不能继续归集成本");
    }

    ProjectCostEntry entry = new ProjectCostEntry();
    entry.setProjectId(project.getId());
    entry.setCategory(request.category());
    entry.setSourceType(request.sourceType());
    entry.setSourceNo(request.sourceNo());
    entry.setDescription(request.description());
    entry.setAmount(request.amount());
    entry.setIncurredDate(request.incurredDate());
    costRepository.save(entry);

    project.setActualCost(amount(project.getActualCost()).add(request.amount()));
    projectRepository.save(project);
    return toDetail(project);
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
    List<ProjectStageRecordResponse> stages = stageRecordRepository.findByProjectIdOrderByChangedAtDesc(project.getId()).stream()
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
        project.getManagerName(),
        project.getSiteAddress(),
        amount(project.getContractAmount()),
        project.getPlannedStartDate(),
        project.getPlannedEndDate(),
        project.getStage(),
        project.getApprovalStatus(),
        project.getApprovalComment(),
        project.getApproverName(),
        project.getApprovedAt(),
        budgetAmount,
        actualCost,
        amount(project.getContractAmount()).subtract(actualCost),
        budgetAmount.subtract(actualCost),
        project.getProgress(),
        project.getWarrantyEndDate()
    );
  }

  private Project requireProject(UUID id) {
    return projectRepository.findById(id)
        .orElseThrow(() -> new BusinessException("项目不存在"));
  }

  private void requireApproved(Project project) {
    if (project.getApprovalStatus() != ProjectApprovalStatus.APPROVED) {
      throw new BusinessException("项目立项审批通过后才能执行");
    }
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

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
