package com.company.ops.api.modules.project.service;

import com.company.ops.api.common.delete.DeleteGovernanceService;
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
import com.company.ops.api.modules.project.dto.ProjectProfitabilityResponse;
import com.company.ops.api.modules.project.dto.ProjectResponse;
import com.company.ops.api.modules.project.dto.ProjectStageRecordResponse;
import com.company.ops.api.modules.project.repository.ProjectBudgetItemRepository;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.repository.ProjectStageRecordRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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
  private final ProjectStageRecordRepository stageRecordRepository;
  private final CustomerRepository customerRepository;
  private final DataScopeService dataScopeService;
  private final ServiceContractRepository contractRepository;
  private final DeleteGovernanceService deleteGovernanceService;
  @PersistenceContext
  private EntityManager entityManager;

  public ProjectService(
      ServiceContractRepository contractRepository,
      ProjectRepository projectRepository,
      ProjectBudgetItemRepository budgetRepository,
      ProjectCostEntryRepository costRepository,
      ProjectStageRecordRepository stageRecordRepository,
      CustomerRepository customerRepository,
      DataScopeService dataScopeService,
      DeleteGovernanceService deleteGovernanceService
  ) {
    this.projectRepository = projectRepository;
    this.budgetRepository = budgetRepository;
    this.costRepository = costRepository;
    this.stageRecordRepository = stageRecordRepository;
    this.customerRepository = customerRepository;
    this.dataScopeService = dataScopeService;
    this.contractRepository = contractRepository;
    this.deleteGovernanceService = deleteGovernanceService;
  }

  @Transactional(readOnly = true)
  public Page<ProjectResponse> listProjects(Pageable pageable) {
    Page<Project> projectPage = projectRepository.findAllByOrderByCreatedAtDesc(pageable);
    List<Project> visibleProjects = deleteGovernanceService.visible("PROJECT", projectPage.getContent(), Project::getId).stream()
        .filter(project -> dataScopeService.canViewOwner(project.getManagerName()))
        .toList();
    Map<UUID, String> customerNames = loadCustomerNames(visibleProjects);
    return new org.springframework.data.domain.PageImpl<>(
        visibleProjects.stream().map(project -> toResponse(project, customerNames.get(project.getCustomerId()), null)).toList(),
        pageable,
        visibleProjects.size()
    );
  }

  @Transactional(readOnly = true)
  public ProjectDetailResponse getProject(UUID id) {
    Project project = requireProject(id);
    if (deleteGovernanceService.isHidden("PROJECT", id)) throw new BusinessException("项目不存在");
    if (!dataScopeService.canViewOwner(project.getManagerName())) throw new BusinessException("无权查看该项目");
    return toDetail(project);
  }

  @Transactional(readOnly = true)
  public List<ProjectProfitabilityResponse> profitability() {
    List<Project> projects = projectRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(project -> dataScopeService.canViewOwner(project.getManagerName()))
        .toList();
    Map<UUID, String> customerNames = loadCustomerNames(projects);
    return projects.stream()
        .map(project -> toProfitability(project, customerNames.get(project.getCustomerId())))
        .toList();
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
    project.setCode(request.code() != null && !request.code().isBlank() ? request.code() : generateProjectCode());
    if (request.contractId() != null) {
      ServiceContract contract = contractRepository.findById(request.contractId()).orElseThrow(() -> new BusinessException("关联合同不存在"));
      project.setContractId(request.contractId());
    }
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

    BigDecimal newCost = amount(project.getActualCost()).add(request.amount());
    if (newCost.compareTo(amount(project.getBudgetAmount())) > 0) {
      long overPct = newCost.subtract(amount(project.getBudgetAmount()))
          .multiply(BigDecimal.valueOf(100))
          .divide(amount(project.getBudgetAmount()), 0, java.math.RoundingMode.HALF_UP)
          .longValue();
      throw new BusinessException("Cost exceeds budget by " + overPct + "%");
    }
    project.setActualCost(newCost);
    projectRepository.save(project);
    return toDetail(project);
  }

  @Transactional
  public void deleteProject(UUID id) {
    Project project = requireProject(id);
    if (!dataScopeService.canViewOwner(project.getManagerName())) {
      throw new BusinessException("无权删除该项目");
    }
    if (!deleteGovernanceService.allowPhysicalDelete("PROJECT", id, project.getCode() + " · " + project.getName())) {
      return;
    }
    entityManager.createNativeQuery("""
        DELETE FROM fin_payment_records
        WHERE payable_id IN (
          SELECT id FROM fin_procurement_payables
          WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id = ?1)
             OR receipt_id IN (SELECT id FROM procurement_goods_receipts WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id = ?1))
        )
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM fin_payment_applications
        WHERE payable_id IN (
          SELECT id FROM fin_procurement_payables
          WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id = ?1)
             OR receipt_id IN (SELECT id FROM procurement_goods_receipts WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id = ?1))
        )
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM fin_procurement_payables
        WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id = ?1)
           OR receipt_id IN (SELECT id FROM procurement_goods_receipts WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM procurement_cost_allocations
        WHERE project_id = ?1
           OR order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id = ?1)
           OR receipt_id IN (SELECT id FROM procurement_goods_receipts WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM procurement_goods_receipts
        WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id = ?1)
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM procurement_purchase_orders
        WHERE project_id = ?1
           OR request_id IN (SELECT id FROM procurement_purchase_requests WHERE project_id = ?1)
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM procurement_request_approval_records
        WHERE request_id IN (SELECT id FROM procurement_purchase_requests WHERE project_id = ?1)
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM procurement_purchase_requests WHERE project_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM inventory_return_lines
        WHERE return_id IN (
          SELECT id FROM inventory_return_orders
          WHERE project_id = ?1 OR issue_id IN (SELECT id FROM inventory_issue_orders WHERE project_id = ?1)
        )
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM inventory_return_orders
        WHERE project_id = ?1 OR issue_id IN (SELECT id FROM inventory_issue_orders WHERE project_id = ?1)
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM inventory_issue_lines
        WHERE issue_id IN (SELECT id FROM inventory_issue_orders WHERE project_id = ?1)
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM inventory_issue_orders WHERE project_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM oa_expense_claims
        WHERE project_id = ?1 OR work_order_id IN (SELECT id FROM work_orders WHERE project_id = ?1)
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM oa_outsource_orders
        WHERE project_id = ?1 OR work_order_id IN (SELECT id FROM work_orders WHERE project_id = ?1)
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM work_orders WHERE project_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM project_budget_items WHERE project_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM project_cost_entries WHERE project_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM project_stage_records WHERE project_id = ?1")
        .setParameter(1, id).executeUpdate();
    projectRepository.deleteById(id);
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
    } else if (budgetUsageRate.compareTo(BigDecimal.valueOf(85)) >= 0 && project.getProgress() < 85) {
      riskLevel = "MEDIUM";
      riskMessage = "预算消耗快于项目进度";
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



  private String generateProjectCode() {
    int count = projectRepository.countByCodeStartingWith("PRJ-");
    String seq = String.format("%05d", count + 1);
    return "PRJ-" + seq;
  }
}
