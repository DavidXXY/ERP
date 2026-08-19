package com.company.ops.api.modules.finance.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableReceipt;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableReceiptRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.finance.domain.PaymentRecord;
import com.company.ops.api.modules.finance.dto.FinanceAnalyticsDtos.FinanceOrganizationNode;
import com.company.ops.api.modules.finance.dto.FinanceContributionDtos.ContributionDataQuality;
import com.company.ops.api.modules.finance.dto.FinanceContributionDtos.ContributionProjectLine;
import com.company.ops.api.modules.finance.dto.FinanceContributionDtos.ContributionSalesperson;
import com.company.ops.api.modules.finance.dto.FinanceContributionDtos.ContributionScope;
import com.company.ops.api.modules.finance.dto.FinanceContributionDtos.ContributionSummary;
import com.company.ops.api.modules.finance.dto.FinanceContributionDtos.FinanceContributionResponse;
import com.company.ops.api.modules.finance.dto.FinanceContributionDtos.MonthlyContribution;
import com.company.ops.api.modules.finance.repository.PaymentRecordRepository;
import com.company.ops.api.modules.finance.service.FinanceOrganizationScopeService.Scope;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceContributionService {
  private static final String ORGANIZATION = "ORGANIZATION";
  private static final String USER = "USER";

  private final FinanceOrganizationScopeService organizationScopeService;
  private final DataScopeService dataScopeService;
  private final SystemUserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final ProjectCostEntryRepository costRepository;
  private final CustomerRepository customerRepository;
  private final ReceivableRepository receivableRepository;
  private final ReceivableReceiptRepository receiptRepository;
  private final PurchaseOrderRepository orderRepository;
  private final ProcurementPayableRepository payableRepository;
  private final PaymentRecordRepository paymentRepository;

  public FinanceContributionService(
      FinanceOrganizationScopeService organizationScopeService,
      DataScopeService dataScopeService,
      SystemUserRepository userRepository,
      ProjectRepository projectRepository,
      ProjectCostEntryRepository costRepository,
      CustomerRepository customerRepository,
      ReceivableRepository receivableRepository,
      ReceivableReceiptRepository receiptRepository,
      PurchaseOrderRepository orderRepository,
      ProcurementPayableRepository payableRepository,
      PaymentRecordRepository paymentRepository) {
    this.organizationScopeService = organizationScopeService;
    this.dataScopeService = dataScopeService;
    this.userRepository = userRepository;
    this.projectRepository = projectRepository;
    this.costRepository = costRepository;
    this.customerRepository = customerRepository;
    this.receivableRepository = receivableRepository;
    this.receiptRepository = receiptRepository;
    this.orderRepository = orderRepository;
    this.payableRepository = payableRepository;
    this.paymentRepository = paymentRepository;
  }

  @Transactional(readOnly = true)
  public List<ContributionSalesperson> salespeople(
      UUID organizationId, boolean includeDescendants) {
    Scope scope = organizationScopeService.resolve(organizationId, includeDescendants);
    if (scope.organizationIds().isEmpty()) return List.of();
    Map<UUID, FinanceOrganizationNode> organizations = visibleOrganizationMap();
    Set<UUID> attributedUsers = projectRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(item -> scope.includes(item.getSalesOrganizationId()))
        .map(Project::getSalesOwnerUserId)
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
    return userRepository.findByOrganization_IdIn(scope.organizationIds()).stream()
        .filter(item -> item.isEnabled() || attributedUsers.contains(item.getId()))
        .filter(item -> attributedUsers.contains(item.getId()) || hasSalesRole(item))
        .map(item -> salesperson(item, organizations))
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(ContributionSalesperson::displayName))
        .toList();
  }

  @Transactional(readOnly = true)
  public FinanceContributionResponse analytics(
      String requestedSubjectType,
      UUID subjectId,
      boolean includeDescendants,
      LocalDate requestedAsOf,
      Integer requestedYear) {
    LocalDate asOf = requestedAsOf == null ? LocalDate.now() : requestedAsOf;
    int year = requestedYear == null ? asOf.getYear() : requestedYear;
    if (year < 2000 || year > 2200) {
      throw new BusinessException("财务年度必须在 2000 到 2200 之间");
    }
    String subjectType = normalizeSubjectType(requestedSubjectType);
    Selection selection = selection(subjectType, subjectId, includeDescendants);
    List<Project> allProjects = projectRepository.findAllByOrderByCreatedAtDesc();
    List<Project> projects = allProjects.stream().filter(selection::includes).toList();
    Set<UUID> projectIds = projects.stream().map(Project::getId)
        .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    Set<UUID> contractIds = projects.stream().map(Project::getContractId)
        .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

    List<ProjectCostEntry> costs = projectIds.isEmpty() ? List.of()
        : costRepository.findByProjectIdIn(projectIds).stream()
            .filter(item -> !item.getIncurredDate().isAfter(asOf)).toList();
    List<Receivable> selectedReceivables = scopedReceivables(selection);
    List<Receivable> projectReceivables = contractIds.isEmpty() ? List.of()
        : selectedReceivables.stream().filter(item -> contractIds.contains(item.getContractId())).toList();
    Set<UUID> receivableIds = selectedReceivables.stream().map(Receivable::getId)
        .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    List<ReceivableReceipt> receipts = receivableIds.isEmpty() ? List.of()
        : receiptRepository.findByReceivableIdIn(receivableIds).stream()
            .filter(item -> !item.getReceivedDate().isAfter(asOf)).toList();

    List<PurchaseOrder> orders = projectIds.isEmpty() ? List.of()
        : orderRepository.findByProjectIdIn(projectIds);
    Map<UUID, UUID> projectByOrder = orders.stream().collect(Collectors.toMap(
        PurchaseOrder::getId, PurchaseOrder::getProjectId, (left, right) -> left));
    List<ProcurementPayable> payables = projectByOrder.isEmpty() ? List.of()
        : payableRepository.findByOrderIdIn(projectByOrder.keySet()).stream()
            .filter(item -> item.getStatus() != PayableStatus.CANCELLED).toList();
    Set<UUID> payableIds = payables.stream().map(ProcurementPayable::getId)
        .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    List<PaymentRecord> payments = payableIds.isEmpty() ? List.of()
        : paymentRepository.findByPayableIdIn(payableIds).stream()
            .filter(item -> !item.getPaidDate().isAfter(asOf)).toList();

    Map<UUID, BigDecimal> costByProject = sumBy(costs, ProjectCostEntry::getProjectId,
        ProjectCostEntry::getAmount);
    Map<UUID, UUID> projectByReceivable = projectReceivables.stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(Receivable::getId,
            item -> projectForContract(projects, item.getContractId()), (left, right) -> left));
    Map<UUID, BigDecimal> receiptByProject = sumBy(receipts.stream()
            .filter(item -> projectByReceivable.containsKey(item.getReceivableId())).toList(),
        item -> projectByReceivable.get(item.getReceivableId()), ReceivableReceipt::getAmount);
    Map<UUID, UUID> projectByPayable = payables.stream()
        .filter(item -> projectByOrder.containsKey(item.getOrderId()))
        .collect(Collectors.toMap(ProcurementPayable::getId,
            item -> projectByOrder.get(item.getOrderId()), (left, right) -> left));
    Map<UUID, BigDecimal> paymentByProject = sumBy(payments,
        item -> projectByPayable.get(item.getPayableId()), PaymentRecord::getAmount);
    Map<UUID, BigDecimal> receivableOutstandingByProject = sumBy(projectReceivables,
        item -> projectByReceivable.get(item.getId()), this::receivableOutstanding);
    Map<UUID, BigDecimal> payableOutstandingByProject = sumBy(payables,
        item -> projectByOrder.get(item.getOrderId()), this::payableOutstanding);

    Map<UUID, String> customerNames = customerRepository.findAllById(projects.stream()
            .map(Project::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet())).stream()
        .collect(Collectors.toMap(Customer::getId, Customer::getName));
    Map<UUID, String> ownerNames = userRepository.findAllById(projects.stream()
            .map(Project::getSalesOwnerUserId).filter(Objects::nonNull).collect(Collectors.toSet())).stream()
        .collect(Collectors.toMap(SystemUser::getId, SystemUser::getDisplayName));
    List<ContributionProjectLine> projectLines = projects.stream()
        .map(item -> projectLine(item, customerNames.get(item.getCustomerId()),
            ownerNames.get(item.getSalesOwnerUserId()), costByProject.get(item.getId()),
            receiptByProject.get(item.getId()), paymentByProject.get(item.getId()),
            receivableOutstandingByProject.get(item.getId()),
            payableOutstandingByProject.get(item.getId())))
        .sorted(Comparator.comparing(ContributionProjectLine::grossProfit).reversed())
        .toList();

    ContributionSummary summary = summary(projectLines, selectedReceivables, receipts, payables, payments);
    long unlinkedReceivables = selectedReceivables.stream()
        .filter(item -> item.getContractId() == null || !contractIds.contains(item.getContractId())).count();
    ContributionDataQuality dataQuality = new ContributionDataQuality(
        selection.unrestricted()
            ? allProjects.stream().filter(item -> item.getSalesOwnerUserId() == null
                || item.getSalesOrganizationId() == null).count()
            : 0,
        selection.unrestricted()
            ? selectedReceivables.stream().filter(item -> item.getSalesOwnerUserId() == null
                || item.getOrganizationId() == null).count()
            : 0,
        unlinkedReceivables,
        "利润按合同额减截至统计日的项目成本；现金按实际回款和项目采购实际付款归集");
    return new FinanceContributionResponse(asOf, year, selection.scope(), summary,
        monthly(year, asOf, receipts, payments), projectLines, dataQuality);
  }

  private List<Receivable> scopedReceivables(Selection selection) {
    if (selection.unrestricted()) return receivableRepository.findAll();
    if (USER.equals(selection.subjectType())) return receivableRepository.findBySalesOwnerUserId(selection.userId());
    return receivableRepository.findByOrganizationIdIn(selection.organizationScope().organizationIds());
  }

  private Selection selection(String subjectType, UUID subjectId, boolean includeDescendants) {
    if (ORGANIZATION.equals(subjectType)) {
      Scope scope = organizationScopeService.resolve(subjectId, includeDescendants);
      String name = subjectId == null ? "全部销售归属" : scope.info().organizationName();
      return new Selection(subjectType, null, scope,
          new ContributionScope(subjectType, subjectId, name, scope.info().organizationPath(),
              includeDescendants, scope.organizationIds().size(), "销售归属快照"));
    }
    if (subjectId == null) throw new BusinessException("请选择销售人员");
    SystemUser user = userRepository.findById(subjectId)
        .orElseThrow(() -> new BusinessException("所选销售人员不存在"));
    if (!dataScopeService.canViewOwner(subjectId)) {
      throw new AccessDeniedException("无权查看所选销售人员的经营贡献");
    }
    UUID organizationId = user.getOrganization() == null ? null : user.getOrganization().getId();
    if (organizationId == null) throw new BusinessException("所选销售人员尚未归属组织");
    Scope scope = organizationScopeService.resolve(organizationId, false);
    return new Selection(subjectType, subjectId, scope,
        new ContributionScope(subjectType, subjectId, user.getDisplayName(),
            scope.info().organizationPath() + " / " + user.getDisplayName(), false, 1, "销售归属快照"));
  }

  private ContributionSummary summary(
      List<ContributionProjectLine> projects,
      List<Receivable> receivables,
      List<ReceivableReceipt> receipts,
      List<ProcurementPayable> payables,
      List<PaymentRecord> payments) {
    BigDecimal contract = sum(projects, ContributionProjectLine::contractAmount);
    BigDecimal cost = sum(projects, ContributionProjectLine::actualCost);
    BigDecimal profit = contract.subtract(cost);
    BigDecimal received = sum(receipts, ReceivableReceipt::getAmount);
    BigDecimal paid = sum(payments, PaymentRecord::getAmount);
    BigDecimal receivableOutstanding = sum(receivables, this::receivableOutstanding);
    BigDecimal payableOutstanding = sum(payables, this::payableOutstanding);
    return new ContributionSummary(contract, cost, profit, rate(profit, contract), received, paid,
        received.subtract(paid), receivableOutstanding, payableOutstanding,
        rate(received, sum(receivables, Receivable::getAmount)), projects.size());
  }

  private ContributionProjectLine projectLine(
      Project project,
      String customerName,
      String ownerName,
      BigDecimal requestedCost,
      BigDecimal requestedReceived,
      BigDecimal requestedPaid,
      BigDecimal requestedReceivableOutstanding,
      BigDecimal requestedPayableOutstanding) {
    BigDecimal contract = money(project.getContractAmount());
    BigDecimal cost = money(requestedCost);
    BigDecimal profit = contract.subtract(cost);
    BigDecimal received = money(requestedReceived);
    BigDecimal paid = money(requestedPaid);
    return new ContributionProjectLine(project.getId(), project.getCode(), project.getName(),
        customerName, project.getStage().name(), ownerName, contract, cost, profit,
        rate(profit, contract), received, paid, received.subtract(paid),
        money(requestedReceivableOutstanding), money(requestedPayableOutstanding));
  }

  private List<MonthlyContribution> monthly(
      int year,
      LocalDate asOf,
      List<ReceivableReceipt> receipts,
      List<PaymentRecord> payments) {
    return java.util.stream.IntStream.rangeClosed(1, 12).mapToObj(month -> {
      BigDecimal received = receipts.stream()
          .filter(item -> item.getReceivedDate().getYear() == year
              && item.getReceivedDate().getMonthValue() == month
              && !item.getReceivedDate().isAfter(asOf))
          .map(ReceivableReceipt::getAmount).map(FinanceContributionService::money)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal paid = payments.stream()
          .filter(item -> item.getPaidDate().getYear() == year
              && item.getPaidDate().getMonthValue() == month
              && !item.getPaidDate().isAfter(asOf))
          .map(PaymentRecord::getAmount).map(FinanceContributionService::money)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      return new MonthlyContribution(month, received, paid, received.subtract(paid));
    }).toList();
  }

  private Map<UUID, FinanceOrganizationNode> visibleOrganizationMap() {
    Map<UUID, FinanceOrganizationNode> result = new HashMap<>();
    for (FinanceOrganizationNode root : organizationScopeService.visibleOrganizations()) {
      flatten(root, result);
    }
    return result;
  }

  private void flatten(FinanceOrganizationNode node, Map<UUID, FinanceOrganizationNode> result) {
    result.put(node.id(), node);
    node.children().forEach(child -> flatten(child, result));
  }

  private ContributionSalesperson salesperson(
      SystemUser user, Map<UUID, FinanceOrganizationNode> organizations) {
    if (user.getOrganization() == null) return null;
    FinanceOrganizationNode organization = organizations.get(user.getOrganization().getId());
    if (organization == null) return null;
    return new ContributionSalesperson(user.getId(), user.getDisplayName(), organization.id(),
        organization.name(), organization.fullPath(), user.isEnabled());
  }

  private boolean hasSalesRole(SystemUser user) {
    return user.getRoles().stream().anyMatch(role -> role.getCode().startsWith("SALES"));
  }

  private String normalizeSubjectType(String value) {
    String normalized = value == null ? ORGANIZATION : value.trim().toUpperCase(java.util.Locale.ROOT);
    if (!Set.of(ORGANIZATION, USER).contains(normalized)) {
      throw new BusinessException("分析对象只能是部门或销售人员");
    }
    return normalized;
  }

  private UUID projectForContract(List<Project> projects, UUID contractId) {
    return projects.stream().filter(item -> Objects.equals(item.getContractId(), contractId))
        .map(Project::getId).findFirst().orElse(null);
  }

  private BigDecimal receivableOutstanding(Receivable item) {
    return money(item.getAmount()).subtract(money(item.getSettledAmount())).max(BigDecimal.ZERO);
  }

  private BigDecimal payableOutstanding(ProcurementPayable item) {
    return money(item.getAmount()).subtract(money(item.getPaidAmount())).max(BigDecimal.ZERO);
  }

  private static BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
    return denominator.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
        : numerator.multiply(BigDecimal.valueOf(100))
            .divide(denominator, 2, RoundingMode.HALF_UP);
  }

  private static <T> BigDecimal sum(Collection<T> values, Function<T, BigDecimal> amount) {
    return values.stream().map(amount).map(FinanceContributionService::money)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static <T> Map<UUID, BigDecimal> sumBy(
      Collection<T> values,
      Function<T, UUID> key,
      Function<T, BigDecimal> amount) {
    return values.stream().filter(item -> key.apply(item) != null).collect(Collectors.groupingBy(
        key, Collectors.reducing(BigDecimal.ZERO,
            item -> money(amount.apply(item)), BigDecimal::add)));
  }

  private static BigDecimal money(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
  }

  private record Selection(
      String subjectType,
      UUID userId,
      Scope organizationScope,
      ContributionScope scope) {
    boolean includes(Project project) {
      return USER.equals(subjectType)
          ? Objects.equals(userId, project.getSalesOwnerUserId())
          : organizationScope.includes(project.getSalesOrganizationId());
    }

    boolean includes(UUID organizationId, UUID salesOwnerUserId) {
      return USER.equals(subjectType)
          ? Objects.equals(userId, salesOwnerUserId)
          : organizationScope.includes(organizationId);
    }

    boolean unrestricted() {
      return ORGANIZATION.equals(subjectType) && organizationScope.unrestricted();
    }
  }
}
