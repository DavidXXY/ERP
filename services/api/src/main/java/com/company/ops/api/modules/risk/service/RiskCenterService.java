package com.company.ops.api.modules.risk.service;

import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReceivableResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.RenewalResponse;
import com.company.ops.api.modules.crm.service.CrmOperationsService;
import com.company.ops.api.modules.finance.dto.FinancePayableResponse;
import com.company.ops.api.modules.finance.service.FinanceService;
import com.company.ops.api.modules.inventory.dto.ReplenishmentSuggestionResponse;
import com.company.ops.api.modules.inventory.service.InventoryService;
import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import com.company.ops.api.modules.maintenance.repository.WorkOrderRepository;
import com.company.ops.api.modules.office.dto.OfficeDtos.TodoItemResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.WarningItemResponse;
import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.office.service.OfficeService;
import com.company.ops.api.modules.procurement.dto.ProcurementMatchingResponse;
import com.company.ops.api.modules.procurement.service.ProcurementService;
import com.company.ops.api.modules.project.dto.ProjectProfitabilityResponse;
import com.company.ops.api.modules.project.service.ProjectService;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.WarningResponse;
import com.company.ops.api.modules.qualification.service.QualificationService;
import com.company.ops.api.modules.risk.domain.RiskRuleConfig;
import com.company.ops.api.modules.risk.domain.RiskSnapshot;
import com.company.ops.api.modules.risk.domain.RiskWorkflow;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskItemResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskModuleSummaryResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskRuleConfigResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskSummaryResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskTrendPointResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.UpdateRiskRuleConfigRequest;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskWorkflowResponse;
import com.company.ops.api.modules.risk.repository.RiskRuleConfigRepository;
import com.company.ops.api.modules.risk.repository.RiskSnapshotRepository;
import com.company.ops.api.modules.risk.repository.RiskWorkflowRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskCenterService {
  private final OfficeService officeService;
  private final InventoryService inventoryService;
  private final ProcurementService procurementService;
  private final ProjectService projectService;
  private final FinanceService financeService;
  private final CrmOperationsService crmOperationsService;
  private final QualificationService qualificationService;
  private final WorkOrderRepository workOrderRepository;
  private final RiskWorkflowRepository workflowRepository;
  private final RiskRuleConfigRepository ruleRepository;
  private final RiskSnapshotRepository snapshotRepository;
  private final SystemNotificationRepository notificationRepository;

  public RiskCenterService(
      OfficeService officeService,
      InventoryService inventoryService,
      ProcurementService procurementService,
      ProjectService projectService,
      FinanceService financeService,
      CrmOperationsService crmOperationsService,
      QualificationService qualificationService,
      WorkOrderRepository workOrderRepository,
      RiskWorkflowRepository workflowRepository,
      RiskRuleConfigRepository ruleRepository,
      RiskSnapshotRepository snapshotRepository,
      SystemNotificationRepository notificationRepository) {
    this.officeService = officeService;
    this.inventoryService = inventoryService;
    this.procurementService = procurementService;
    this.projectService = projectService;
    this.financeService = financeService;
    this.crmOperationsService = crmOperationsService;
    this.qualificationService = qualificationService;
    this.workOrderRepository = workOrderRepository;
    this.workflowRepository = workflowRepository;
    this.ruleRepository = ruleRepository;
    this.snapshotRepository = snapshotRepository;
    this.notificationRepository = notificationRepository;
  }

  @Transactional(readOnly = true)
  public List<RiskItemResponse> listItems() {
    return collectItems(workflowMap(), ruleMap());
  }

  @Transactional(readOnly = true)
  public RiskSummaryResponse summary(int days) {
    List<RiskItemResponse> items = collectItems(workflowMap(), ruleMap());
    List<RiskModuleSummaryResponse> modules = items.stream()
        .collect(Collectors.groupingBy(RiskItemResponse::module, Collectors.toList()))
        .entrySet().stream()
        .map(entry -> {
          List<RiskItemResponse> rows = entry.getValue();
          String moduleName = rows.isEmpty() ? entry.getKey() : rows.get(0).moduleName();
          return new RiskModuleSummaryResponse(entry.getKey(), moduleName, rows.size(),
              rows.stream().filter(item -> "HIGH".equals(item.severity())).count(),
              rows.stream().filter(item -> "OVERDUE".equals(item.status())).count(),
              rows.stream().filter(RiskItemResponse::slaOverdue).count(),
              rows.stream().map(RiskItemResponse::amount).reduce(BigDecimal.ZERO, this::add));
        })
        .sorted(Comparator.comparing(RiskModuleSummaryResponse::totalCount).reversed())
        .toList();
    LocalDate end = LocalDate.now();
    LocalDate start = end.minusDays(Math.max(1, days) - 1L);
    Map<LocalDate, List<RiskSnapshot>> snapshots = snapshotRepository.findBySnapshotDateBetweenOrderBySnapshotDateAsc(start, end)
        .stream().collect(Collectors.groupingBy(RiskSnapshot::getSnapshotDate));
    List<RiskTrendPointResponse> trends = new ArrayList<>();
    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
      List<RiskSnapshot> rows = snapshots.getOrDefault(date, List.of());
      trends.add(new RiskTrendPointResponse(date, rows.size(),
          rows.stream().filter(item -> "HIGH".equals(item.getSeverity())).count(),
          rows.stream().filter(item -> "OVERDUE".equals(item.getStatus())).count(),
          rows.stream().filter(item -> "CLOSED".equals(item.getWorkflowStatus()) || "IGNORED".equals(item.getWorkflowStatus())).count(),
          rows.stream().map(RiskSnapshot::getAmount).reduce(BigDecimal.ZERO, this::add)));
    }
    return new RiskSummaryResponse(items.size(),
        items.stream().filter(item -> "HIGH".equals(item.severity())).count(),
        items.stream().filter(item -> "OVERDUE".equals(item.status())).count(),
        items.stream().filter(RiskItemResponse::slaOverdue).count(),
        items.stream().filter(item -> item.workflow() != null && ("CLOSED".equals(item.workflow().status()) || "IGNORED".equals(item.workflow().status()))).count(),
        items.stream().map(RiskItemResponse::amount).reduce(BigDecimal.ZERO, this::add),
        modules,
        trends);
  }

  @Transactional
  public int snapshotToday() {
    LocalDate today = LocalDate.now();
    List<RiskItemResponse> items = collectItems(workflowMap(), ruleMap());
    int count = 0;
    for (RiskItemResponse item : items) {
      if (snapshotRepository.existsBySnapshotDateAndRiskKey(today, item.key())) continue;
      RiskSnapshot snapshot = new RiskSnapshot();
      snapshot.setSnapshotDate(today);
      snapshot.setRiskKey(item.key());
      snapshot.setModule(item.module());
      snapshot.setModuleName(item.moduleName());
      snapshot.setTitle(truncate(item.title(), 180));
      snapshot.setSubject(truncate(item.subject(), 300));
      snapshot.setSeverity(item.severity());
      snapshot.setStatus(item.status());
      snapshot.setWorkflowStatus(item.workflow() == null ? "UNCLAIMED" : item.workflow().status());
      snapshot.setAmount(item.amount());
      snapshotRepository.save(snapshot);
      count++;
    }
    return count;
  }

  @Transactional
  public int escalateOverdue() {
    List<RiskItemResponse> items = collectItems(workflowMap(), ruleMap());
    int count = 0;
    for (RiskItemResponse item : items) {
      if (!item.slaOverdue()) continue;
      String key = "risk-sla-" + item.key();
      if (notificationRepository.existsByDedupKey(key)) continue;
      SystemNotification notification = new SystemNotification();
      notification.setDedupKey(key);
      notification.setType("RISK_SLA");
      notification.setTitle("风险处理超时：" + truncate(item.title(), 80));
      notification.setContent(item.moduleName() + " · " + item.subject() + "，SLA 截止 " + item.dueAt().toLocalDate() + "，请尽快处理。");
      notification.setRelatedType("RISK");
      notification.setRead(false);
      notificationRepository.save(notification);
      count++;
    }
    return count;
  }

  @Transactional(readOnly = true)
  public List<RiskRuleConfigResponse> listRules() {
    Map<String, RiskRuleConfig> saved = ruleMap();
    List<RiskRuleConfigResponse> rows = defaultRules().values().stream()
        .map(rule -> toRuleResponse(saved.getOrDefault(rule.getRuleCode(), rule)))
        .collect(Collectors.toCollection(ArrayList::new));
    saved.values().stream()
        .filter(rule -> !defaultRules().containsKey(rule.getRuleCode()))
        .map(this::toRuleResponse)
        .forEach(rows::add);
    return rows;
  }

  @Transactional
  public RiskRuleConfigResponse saveRule(UpdateRiskRuleConfigRequest request) {
    RiskRuleConfig rule = ruleRepository.findByRuleCode(request.ruleCode()).orElseGet(RiskRuleConfig::new);
    rule.setRuleCode(request.ruleCode());
    rule.setName(request.name());
    rule.setModule(request.module());
    rule.setEnabled(request.enabled() == null || request.enabled());
    rule.setHighThreshold(request.highThreshold());
    rule.setMediumThreshold(request.mediumThreshold());
    rule.setWarningDays(request.warningDays());
    rule.setSlaHours(request.slaHours());
    rule.setDefaultOwner(trimToNull(request.defaultOwner()));
    rule.setEscalationOwner(trimToNull(request.escalationOwner()));
    rule.setRemark(trimToNull(request.remark()));
    return toRuleResponse(ruleRepository.save(rule));
  }

  private List<RiskItemResponse> collectItems(Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules) {
    List<RiskItemResponse> items = new ArrayList<>();
    loadOffice(items::add, workflows, rules);
    loadInventory(items::add, workflows, rules);
    loadProcurement(items::add, workflows, rules);
    loadProjects(items::add, workflows, rules);
    loadFinance(items::add, workflows, rules);
    loadQualification(items::add, workflows, rules);
    loadRenewals(items::add, workflows, rules);
    loadMaintenance(items::add, workflows, rules);
    return items.stream()
        .sorted(Comparator.comparingInt((RiskItemResponse item) -> severityRank(item.severity())).reversed()
            .thenComparing(item -> item.date() == null ? "" : item.date(), Comparator.reverseOrder()))
        .toList();
  }

  private void loadOffice(Consumer<RiskItemResponse> sink, Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules) {
    if (!canAny("office:approval:view", "office:notification:view", "office:expense:view", "office:outsource:view")) return;
    var workbench = officeService.workbench();
    for (WarningItemResponse item : workbench.warnings()) {
      String key = "office-warning-" + item.type() + "-" + item.id();
      sink.accept(item(key, "office", "OA协同", item.title(), item.content(), item.content(),
          severity(item.severity()), item.type().contains("OVERDUE") ? "OVERDUE" : "OPEN",
          null, date(item.createdAt()), item.route(), "office_warning", item.createdAt(), workflows, rules));
    }
    workbench.todos().stream().limit(20).forEach(item -> addOfficeTodo(sink, workflows, rules, item));
  }

  private void addOfficeTodo(Consumer<RiskItemResponse> sink, Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules, TodoItemResponse item) {
    String key = "office-todo-" + item.type() + "-" + item.id();
    sink.accept(item(key, "office", "OA协同", item.title(), item.subtitle(), "待办事项未处理",
        severity(item.priority()), "PENDING", item.amount(), date(item.createdAt()), item.route(), "office_todo", item.createdAt(), workflows, rules));
  }

  private void loadInventory(Consumer<RiskItemResponse> sink, Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules) {
    if (!can("inventory:view")) return;
    for (ReplenishmentSuggestionResponse item : inventoryService.replenishmentSuggestions()) {
      String key = "inventory-" + item.partId();
      sink.accept(item(key, "inventory", "库存管理", "补货建议", item.partName() + " · " + defaultText(item.partCode(), "-"),
          item.reason(), severity(item.priority()), "OPEN", item.suggestedQty(), null, "/inventory/analytics", "inventory_replenishment", null, workflows, rules));
    }
  }

  private void loadProcurement(Consumer<RiskItemResponse> sink, Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules) {
    if (!can("procurement:view")) return;
    for (ProcurementMatchingResponse item : procurementService.matching()) {
      if ("MATCHED".equals(item.matchStatus())) continue;
      String key = "procurement-" + item.orderId();
      sink.accept(item(key, "procurement", "供应链采购", matchStatusLabel(item.matchStatus()),
          defaultText(item.orderCode(), "-") + " · " + defaultText(item.supplierName(), "未知供应商"),
          item.riskMessage(), "AMOUNT_MISMATCH".equals(item.matchStatus()) ? "HIGH" : "MEDIUM",
          "RECEIVING".equals(item.matchStatus()) ? "PENDING" : "OPEN",
          firstAmount(item.payableAmount(), item.receiptAmount(), item.orderAmount()), null, "/procurement/p2p", "procurement_matching", null, workflows, rules));
    }
  }

  private void loadProjects(Consumer<RiskItemResponse> sink, Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules) {
    if (!can("project:view")) return;
    for (ProjectProfitabilityResponse item : projectService.profitability()) {
      String riskLevel = projectRiskLevel(item, rules.get("project_profitability"));
      if ("LOW".equals(riskLevel)) continue;
      String key = "project-" + item.projectId();
      sink.accept(item(key, "project", "项目管理", item.riskMessage(),
          item.projectName() + " · " + defaultText(item.customerName(), "未关联客户"),
          "毛利率 " + percent(item.grossMarginRate()) + "，预算使用 " + percent(item.budgetUsageRate()),
          severity(riskLevel), "OPEN", item.grossMargin(), null, "/projects/list", "project_profitability", null, workflows, rules));
    }
  }

  private void loadFinance(Consumer<RiskItemResponse> sink, Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules) {
    if (can("finance:receivable:view")) {
      for (ReceivableResponse item : crmOperationsService.listReceivables()) {
        boolean overdue = item.status() == ReceivableStatus.OVERDUE
            || amount(item.outstandingAmount()).compareTo(BigDecimal.ZERO) > 0 && isPast(item.dueDate());
        if (!overdue) continue;
        String key = "finance-receivable-" + item.id();
        sink.accept(item(key, "finance", "财务资金", "应收逾期",
            item.customerName() + " · " + defaultText(firstText(item.contractCode(), item.sourceNo()), "-"),
            "到期日 " + item.dueDate(), "HIGH", "OVERDUE", item.outstandingAmount(),
            date(item.dueDate()), "/finance/receivables", "finance_receivable_overdue", startAt(item.dueDate()), workflows, rules));
      }
    }
    if (can("finance:payable:view")) {
      for (FinancePayableResponse item : financeService.listPayables()) {
        if (!item.overdue()) continue;
        String key = "finance-payable-" + item.id();
        sink.accept(item(key, "finance", "财务资金", "应付逾期",
            defaultText(item.supplierName(), "-") + " · " + defaultText(item.orderCode(), "-"),
            "到期日 " + item.dueDate(), "MEDIUM", "OVERDUE", item.outstandingAmount(),
            date(item.dueDate()), "/finance/payables", "finance_payable_overdue", startAt(item.dueDate()), workflows, rules));
      }
    }
  }

  private void loadQualification(Consumer<RiskItemResponse> sink, Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules) {
    if (!can("qualification:warning:view")) return;
    for (WarningResponse item : qualificationService.warnings()) {
      String key = "qualification-" + item.sourceType() + "-" + item.sourceId() + "-" + item.warningType();
      sink.accept(item(key, "qualification", "资质管理", item.title(), item.sourceName(),
          "OVERDUE".equals(item.status()) ? "已逾期 " + Math.abs(item.daysLeft()) + " 天" : "剩余 " + item.daysLeft() + " 天",
          "DANGER".equals(item.level()) ? "HIGH" : "WARNING".equals(item.level()) ? "MEDIUM" : "LOW",
          "OVERDUE".equals(item.status()) ? "OVERDUE" : "OPEN", null, date(item.dueDate()), "/qualification/warnings", "qualification_expiry", startAt(item.dueDate()), workflows, rules));
    }
  }

  private void loadRenewals(Consumer<RiskItemResponse> sink, Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules) {
    if (!can("crm:renewal:view")) return;
    for (RenewalResponse item : crmOperationsService.listRenewals()) {
      if ("LOW".equals(item.renewalRisk())) continue;
      String key = "crm-renewal-" + item.contractId();
      sink.accept(item(key, "crm", "CRM", "EXPIRED".equals(item.renewalRisk()) ? "合同已到期" : "合同续约风险",
          item.customerName() + " · " + item.contractCode(),
          item.daysRemaining() < 0 ? "已到期 " + Math.abs(item.daysRemaining()) + " 天" : "剩余 " + item.daysRemaining() + " 天",
          "EXPIRED".equals(item.renewalRisk()) || "HIGH".equals(item.renewalRisk()) ? "HIGH" : "MEDIUM",
          item.daysRemaining() < 0 ? "OVERDUE" : "OPEN",
          firstAmount(item.outstandingAmount(), item.amount()), date(item.endDate()), "/crm/renewals", "crm_renewal", startAt(item.endDate()), workflows, rules));
    }
  }

  private void loadMaintenance(Consumer<RiskItemResponse> sink, Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules) {
    if (!can("maintenance:order:view")) return;
    LocalDate today = LocalDate.now();
    for (WorkOrder item : workOrderRepository.findAllByOrderByCreatedAtDesc()) {
      if (item.getPlannedDate() == null || !item.getPlannedDate().isBefore(today)) continue;
      if (item.getStatus() == WorkOrderStatus.ACCEPTED || item.getStatus() == WorkOrderStatus.CANCELLED) continue;
      long days = java.time.temporal.ChronoUnit.DAYS.between(item.getPlannedDate(), today);
      String key = "maintenance-overdue-" + item.getId();
      sink.accept(item(key, "maintenance", "维保服务", "维保工单超期", item.getCode() + " · " + item.getTitle(),
          "计划日期 " + item.getPlannedDate() + "，已超期 " + days + " 天",
          days >= 3 ? "HIGH" : "MEDIUM", "OVERDUE", item.getCostAmount(), date(item.getPlannedDate()),
          "/maintenance/work-orders", "maintenance_work_order_overdue", startAt(item.getPlannedDate()), workflows, rules));
    }
  }

  private RiskItemResponse item(String key, String module, String moduleName, String title, String subject,
      String description, String severity, String status, BigDecimal amount, String date, String route,
      String ruleCode, OffsetDateTime startAt, Map<String, RiskWorkflowResponse> workflows, Map<String, RiskRuleConfig> rules) {
    RiskRuleConfig rule = rules.get(ruleCode);
    RiskWorkflowResponse workflow = workflows.get(key);
    RiskWorkflowResponse effectiveWorkflow = workflow;
    if (workflow == null && rule != null && trimToNull(rule.getDefaultOwner()) != null) {
      effectiveWorkflow = new RiskWorkflowResponse(key, "UNCLAIMED", rule.getDefaultOwner(), null, null, "系统", null, null);
    }
    Integer slaHours = rule == null ? null : rule.getSlaHours();
    OffsetDateTime dueAt = slaHours == null ? null : (startAt == null ? OffsetDateTime.now() : startAt).plusHours(slaHours);
    boolean slaOverdue = dueAt != null && dueAt.isBefore(OffsetDateTime.now())
        && (effectiveWorkflow == null || !List.of("CLOSED", "IGNORED").contains(effectiveWorkflow.status()));
    return new RiskItemResponse(key, module, moduleName, title, subject, description, severity, status, amount, date, route,
        ruleCode, slaHours, dueAt, slaOverdue, effectiveWorkflow);
  }

  private RiskWorkflowResponse toWorkflowResponse(RiskWorkflow workflow) {
    return new RiskWorkflowResponse(
        workflow.getRiskKey(),
        workflow.getStatus(),
        workflow.getOwner(),
        workflow.getNote(),
        workflow.getReason(),
        workflow.getUpdatedByName(),
        workflow.getProcessedAt(),
        workflow.getUpdatedAt()
    );
  }

  private RiskRuleConfigResponse toRuleResponse(RiskRuleConfig rule) {
    return new RiskRuleConfigResponse(
        rule.getId(),
        rule.getRuleCode(),
        rule.getName(),
        rule.getModule(),
        rule.isEnabled(),
        rule.getHighThreshold(),
        rule.getMediumThreshold(),
        rule.getWarningDays(),
        rule.getSlaHours(),
        rule.getDefaultOwner(),
        rule.getEscalationOwner(),
        rule.getRemark()
    );
  }

  private Map<String, RiskWorkflowResponse> workflowMap() {
    return workflowRepository.findAllByOrderByUpdatedAtDesc().stream()
        .collect(Collectors.toMap(RiskWorkflow::getRiskKey, this::toWorkflowResponse, (left, right) -> left));
  }

  private Map<String, RiskRuleConfig> ruleMap() {
    Map<String, RiskRuleConfig> rules = new HashMap<>(defaultRules());
    ruleRepository.findAll().forEach(rule -> {
      if (rule.isEnabled()) rules.put(rule.getRuleCode(), rule);
      else rules.remove(rule.getRuleCode());
    });
    return rules;
  }

  private Map<String, RiskRuleConfig> defaultRules() {
    Map<String, RiskRuleConfig> rules = new HashMap<>();
    putDefault(rules, "office_warning", "OA预警", "office", null, null, null, 48, "综合管理员", "管理员");
    putDefault(rules, "office_todo", "OA待办", "office", null, null, null, 72, "综合管理员", "管理员");
    putDefault(rules, "inventory_replenishment", "库存补货", "inventory", null, null, null, 72, "仓库管理员", "运营负责人");
    putDefault(rules, "procurement_matching", "采购三单匹配", "procurement", null, null, null, 48, "采购负责人", "运营负责人");
    putDefault(rules, "project_profitability", "项目利润风险", "project", BigDecimal.valueOf(0), BigDecimal.valueOf(10), null, 72, "项目经理", "项目总监");
    putDefault(rules, "finance_receivable_overdue", "应收逾期", "finance", null, null, null, 24, "财务经理", "总经理");
    putDefault(rules, "finance_payable_overdue", "应付逾期", "finance", null, null, null, 48, "财务经理", "总经理");
    putDefault(rules, "qualification_expiry", "资质到期", "qualification", null, null, 30, 72, "资质管理员", "综合负责人");
    putDefault(rules, "crm_renewal", "合同续约", "crm", null, null, 30, 72, "客户经理", "销售负责人");
    putDefault(rules, "maintenance_work_order_overdue", "维保工单超期", "maintenance", null, null, null, 24, "维保主管", "运营负责人");
    return rules;
  }

  private void putDefault(Map<String, RiskRuleConfig> rules, String code, String name, String module,
      BigDecimal highThreshold, BigDecimal mediumThreshold, Integer warningDays, Integer slaHours,
      String defaultOwner, String escalationOwner) {
    RiskRuleConfig rule = new RiskRuleConfig();
    rule.setRuleCode(code);
    rule.setName(name);
    rule.setModule(module);
    rule.setEnabled(true);
    rule.setHighThreshold(highThreshold);
    rule.setMediumThreshold(mediumThreshold);
    rule.setWarningDays(warningDays);
    rule.setSlaHours(slaHours);
    rule.setDefaultOwner(defaultOwner);
    rule.setEscalationOwner(escalationOwner);
    rules.put(code, rule);
  }

  private boolean canAny(String... permissions) {
    for (String permission : permissions) if (can(permission)) return true;
    return false;
  }

  private boolean can(String permission) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) return false;
    return principal.roleCodes().contains("ADMIN") || principal.permissions().contains(permission);
  }

  private String severity(String value) {
    if ("HIGH".equals(value)) return "HIGH";
    if ("MEDIUM".equals(value)) return "MEDIUM";
    return "LOW";
  }

  private int severityRank(String severity) {
    if ("HIGH".equals(severity)) return 3;
    if ("MEDIUM".equals(severity)) return 2;
    return 1;
  }

  private String matchStatusLabel(String value) {
    return switch (value) {
      case "RECEIVING" -> "待入库";
      case "PAYABLE_MISSING" -> "缺应付";
      case "AMOUNT_MISMATCH" -> "金额不一致";
      case "CANCELLED" -> "已取消";
      default -> value;
    };
  }

  private BigDecimal firstAmount(BigDecimal... values) {
    for (BigDecimal value : values) if (value != null && value.compareTo(BigDecimal.ZERO) != 0) return value;
    return BigDecimal.ZERO;
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal add(BigDecimal left, BigDecimal right) {
    return amount(left).add(amount(right));
  }

  private String projectRiskLevel(ProjectProfitabilityResponse item, RiskRuleConfig rule) {
    if (rule == null || rule.getHighThreshold() == null || rule.getMediumThreshold() == null) return item.riskLevel();
    BigDecimal margin = amount(item.grossMarginRate());
    if (amount(item.grossMargin()).compareTo(BigDecimal.ZERO) < 0 || margin.compareTo(rule.getHighThreshold()) <= 0) return "HIGH";
    if (margin.compareTo(rule.getMediumThreshold()) <= 0) return "MEDIUM";
    return "LOW";
  }

  private String firstText(String... values) {
    for (String value : values) if (value != null && !value.isBlank()) return value;
    return null;
  }

  private String defaultText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private boolean isPast(LocalDate date) {
    return date != null && date.isBefore(LocalDate.now());
  }

  private OffsetDateTime startAt(LocalDate date) {
    if (date == null) return null;
    return date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
  }

  private String date(LocalDate date) {
    return date == null ? null : date.toString();
  }

  private String date(OffsetDateTime dateTime) {
    return dateTime == null ? null : dateTime.toLocalDate().toString();
  }

  private String percent(BigDecimal value) {
    return amount(value).setScale(1, java.math.RoundingMode.HALF_UP) + "%";
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }

  private String truncate(String value, int max) {
    if (value == null) return "";
    return value.length() <= max ? value : value.substring(0, max);
  }
}
