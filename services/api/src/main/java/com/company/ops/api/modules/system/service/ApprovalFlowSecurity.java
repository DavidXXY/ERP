package com.company.ops.api.modules.system.service;

import com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig;
import com.company.ops.api.modules.system.repository.ApprovalAssigneeConfigRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("approvalFlowSecurity")
public class ApprovalFlowSecurity {
  private final ApprovalAssigneeConfigRepository repository;

  public ApprovalFlowSecurity(ApprovalAssigneeConfigRepository repository) { this.repository = repository; }

  public boolean canApprove(String flowCode) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) return false;
    var configs = repository.findByFlowCodeAndEnabledTrue(flowCode);
    return configs.isEmpty() || configs.stream().anyMatch(item -> assigneeMatches(item, principal));
  }

  public UUID currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) return principal.id();
    return null;
  }

  public boolean canApprove(String flowCode, int completedApprovals) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) return false;
    UUID userId = principal.id();
    var configs = repository.findByFlowCodeAndEnabledTrue(flowCode);
    if (configs.isEmpty()) return true;
    boolean sequential = configs.stream().anyMatch(item -> "SEQUENTIAL".equals(item.getApprovalMode()));
    return configs.stream().filter(item -> !sequential || item.getSequenceNo() == completedApprovals + 1)
        .anyMatch(item -> assigneeMatches(item, principal));
  }

  public void requireApprover(String flowCode) {
    requireApprover(flowCode, 0);
  }

  public void requireApprover(String flowCode, int completedApprovals) {
    if (!canApprove(flowCode, completedApprovals)) throw new AccessDeniedException("当前账号未配置为该审批节点的审批人");
  }

  public boolean isSequential(String flowCode) {
    return repository.findByFlowCodeAndEnabledTrue(flowCode).stream().anyMatch(item -> "SEQUENTIAL".equals(item.getApprovalMode()));
  }

  public int stepCount(String flowCode) {
    return repository.findByFlowCodeAndEnabledTrue(flowCode).stream().mapToInt(item -> item.getSequenceNo()).max().orElse(0);
  }

  public ApprovalPlan resolve(ApprovalContext context) {
    List<ApprovalAssigneeConfig> matched = repository.findByFlowCodeAndEnabledTrue(context.flowCode()).stream()
        .filter(item -> matches(item, context))
        .sorted(Comparator.comparingInt(ApprovalAssigneeConfig::getPriority).thenComparingInt(ApprovalAssigneeConfig::getSequenceNo))
        .toList();
    if (matched.isEmpty()) matched = repository.findByFlowCodeAndEnabledTrue(context.flowCode()).stream()
        .filter(item -> "ANY".equals(item.getConditionType()))
        .sorted(Comparator.comparingInt(ApprovalAssigneeConfig::getPriority).thenComparingInt(ApprovalAssigneeConfig::getSequenceNo))
        .toList();
    boolean sequential = matched.stream().anyMatch(item -> "SEQUENTIAL".equals(item.getApprovalMode()));
    int stepCount = matched.stream().mapToInt(ApprovalAssigneeConfig::getSequenceNo).max().orElse(0);
    String mode = matched.isEmpty() ? null : sequential ? "SEQUENTIAL" : "PARALLEL";
    return new ApprovalPlan(matched, mode, stepCount, describe(matched));
  }

  public boolean canApprove(ApprovalContext context, int completedApprovals, UUID delegatedUserId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) return false;
    if (delegatedUserId != null && delegatedUserId.equals(principal.id())) return true;
    ApprovalPlan plan = resolve(context);
    if (plan.configs().isEmpty()) return true;
    int step = "SEQUENTIAL".equals(plan.mode()) ? completedApprovals + 1 : 1;
    return plan.configs().stream()
        .filter(item -> !"SEQUENTIAL".equals(plan.mode()) || item.getSequenceNo() == step)
        .anyMatch(item -> assigneeMatches(item, principal));
  }

  public void requireApprover(ApprovalContext context, int completedApprovals, UUID delegatedUserId) {
    if (!canApprove(context, completedApprovals, delegatedUserId)) throw new AccessDeniedException("当前账号不是该审批节点处理人");
  }

  private boolean matches(ApprovalAssigneeConfig item, ApprovalContext context) {
    if (item.getMinAmount() != null && amount(context.amount()).compareTo(item.getMinAmount()) < 0) return false;
    if (item.getMaxAmount() != null && amount(context.amount()).compareTo(item.getMaxAmount()) > 0) return false;
    if (!blank(item.getDepartmentName()) && !equalsText(item.getDepartmentName(), context.departmentName())) return false;
    if (!blank(item.getBusinessType()) && !equalsText(item.getBusinessType(), context.businessType())) return false;
    if (!blank(item.getProjectCode()) && !equalsText(item.getProjectCode(), context.projectCode())) return false;
    if (!blank(item.getSupplierRisk()) && !equalsText(item.getSupplierRisk(), context.supplierRisk())) return false;
    if (!blank(item.getCustomerLevel()) && !equalsText(item.getCustomerLevel(), context.customerLevel())) return false;
    return true;
  }

  private boolean assigneeMatches(ApprovalAssigneeConfig item, UserPrincipal principal) {
    if ("ROLE".equals(item.getAssigneeType())) {
      return item.getRoleId() != null && principal.roleIds().contains(item.getRoleId());
    }
    return item.getUserId() != null && principal.id().equals(item.getUserId());
  }

  private String describe(List<ApprovalAssigneeConfig> configs) {
    if (configs.isEmpty()) return "未命中专用规则，按原权限审批";
    ApprovalAssigneeConfig first = configs.get(0);
    return first.getFlowName() + " · " + first.getConditionType() + " · " + ("SEQUENTIAL".equals(first.getApprovalMode()) ? "多级审批" : "并行审批");
  }

  private boolean equalsText(String left, String right) {
    return right != null && left.trim().equalsIgnoreCase(right.trim());
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  public record ApprovalContext(String flowCode, BigDecimal amount, String departmentName, String businessType,
                                String projectCode, String supplierRisk, String customerLevel) {}
  public record ApprovalPlan(List<ApprovalAssigneeConfig> configs, String mode, int stepCount, String ruleText) {}
}
