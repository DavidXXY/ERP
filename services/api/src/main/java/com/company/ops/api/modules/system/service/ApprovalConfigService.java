package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalFlowDiagnostic;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalFlowVersionResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalFlowPreviewResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalFlowPreviewStep;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.BatchPreviewApprovalFlowRequest;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.CopyApprovalFlowRequest;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalConfigResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.CreateApprovalConfigRequest;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.PreviewApprovalFlowRequest;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.UpdateApprovalConfigRequest;
import com.company.ops.api.modules.system.repository.ApprovalAssigneeConfigRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalConfigService {
  private final ApprovalAssigneeConfigRepository repository;
  private final SystemUserRepository userRepository;
  private final SystemRoleRepository roleRepository;

  public ApprovalConfigService(ApprovalAssigneeConfigRepository repository, SystemUserRepository userRepository,
                               SystemRoleRepository roleRepository) {
    this.repository = repository; this.userRepository = userRepository; this.roleRepository = roleRepository;
  }

  @Transactional(readOnly = true)
  public List<ApprovalConfigResponse> list() {
    return repository.findAllByOrderByFlowCodeAscCreatedAtAsc().stream()
        .collect(Collectors.groupingBy(ApprovalAssigneeConfig::getFlowCode))
        .values().stream()
        .flatMap(items -> latestEnabled(items).stream())
        .sorted(Comparator.comparing(ApprovalAssigneeConfig::getFlowCode).thenComparingInt(ApprovalAssigneeConfig::getSequenceNo).thenComparingInt(ApprovalAssigneeConfig::getPriority))
        .map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ApprovalFlowPreviewResponse preview(PreviewApprovalFlowRequest request) {
    List<ApprovalAssigneeConfig> enabled = latestEnabled(repository.findByFlowCodeAndEnabledTrue(request.flowCode()));
    List<ApprovalAssigneeConfig> matched = enabled.stream()
        .filter(item -> matches(item, request))
        .sorted(Comparator.comparingInt(ApprovalAssigneeConfig::getSequenceNo).thenComparingInt(ApprovalAssigneeConfig::getPriority))
        .toList();
    if (matched.isEmpty()) {
      matched = enabled.stream()
          .filter(item -> "ANY".equals(item.getConditionType()))
          .sorted(Comparator.comparingInt(ApprovalAssigneeConfig::getSequenceNo).thenComparingInt(ApprovalAssigneeConfig::getPriority))
          .toList();
    }
    String flowName = matched.stream().findFirst().map(ApprovalAssigneeConfig::getFlowName).orElse(request.flowCode());
    String mode = matched.stream().anyMatch(item -> "SEQUENTIAL".equals(item.getApprovalMode())) ? "SEQUENTIAL" : matched.isEmpty() ? null : "PARALLEL";
    Map<Integer, List<ApprovalAssigneeConfig>> byStep = matched.stream()
        .collect(Collectors.groupingBy(ApprovalAssigneeConfig::getSequenceNo, LinkedHashMap::new, Collectors.toList()));
    List<ApprovalFlowPreviewStep> steps = byStep.entrySet().stream()
        .map(entry -> new ApprovalFlowPreviewStep(
            entry.getKey(),
            entry.getValue().stream().map(this::assigneeName).distinct().toList(),
            entry.getValue().stream().map(this::conditionSummary).distinct().toList(),
            entry.getValue().stream().map(ApprovalAssigneeConfig::getSlaHours).filter(v -> v != null).min(Integer::compareTo).orElse(null),
            entry.getValue().stream().map(this::escalationRoleName).filter(v -> v != null).findFirst().orElse(null),
            entry.getValue().stream().anyMatch(this::autoApproved)
        ))
        .toList();
    int totalSteps = matched.stream().mapToInt(ApprovalAssigneeConfig::getSequenceNo).max().orElse(0);
    int versionNo = enabled.stream().mapToInt(ApprovalAssigneeConfig::getVersionNo).max().orElse(1);
    String ruleText = matched.isEmpty() ? "未命中专用规则，按原权限审批" : flowName + " · " + ("SEQUENTIAL".equals(mode) ? "多级审批" : "并行审批");
    return new ApprovalFlowPreviewResponse(request.flowCode(), flowName, mode, totalSteps, versionNo, ruleText, steps);
  }

  @Transactional(readOnly = true)
  public List<ApprovalFlowPreviewResponse> batchPreview(BatchPreviewApprovalFlowRequest request) {
    return request.items().stream().map(this::preview).toList();
  }

  @Transactional
  public ApprovalConfigResponse create(CreateApprovalConfigRequest request) {
    int nextVersion = nextVersion(request.flowCode());
    List<ApprovalAssigneeConfig> next = latestEnabled(repository.findByFlowCodeAndEnabledTrue(request.flowCode())).stream().map(item -> cloneConfig(item, nextVersion)).collect(Collectors.toList());
    ApprovalAssigneeConfig config = new ApprovalAssigneeConfig(); config.setVersionNo(nextVersion);
    applyConfig(config, request.flowCode(), request.flowName(), request.assigneeType(), request.userId(), request.roleId(), request.dynamicAssignee(),
        request.autoAction(), request.slaHours(), request.escalationRoleId(), request.stepPolicy(), request.approvalMode(), request.sequenceNo(),
        request.conditionType(), request.minAmount(), request.maxAmount(), request.departmentName(), request.businessType(),
        request.projectCode(), request.supplierRisk(), request.customerLevel(), request.priority(), request.remark(), true, null);
    next.add(config);
    repository.saveAll(next);
    return toResponse(config);
  }

  @Transactional
  public ApprovalConfigResponse update(UUID id, UpdateApprovalConfigRequest request) {
    ApprovalAssigneeConfig config = repository.findById(id).orElseThrow(() -> new BusinessException("审批配置不存在"));
    int nextVersion = nextVersion(config.getFlowCode());
    List<ApprovalAssigneeConfig> next = latestEnabled(repository.findByFlowCodeAndEnabledTrue(config.getFlowCode())).stream()
        .filter(item -> !item.getId().equals(id))
        .map(item -> cloneConfig(item, nextVersion))
        .collect(Collectors.toList());
    ApprovalAssigneeConfig updated = new ApprovalAssigneeConfig(); updated.setVersionNo(nextVersion);
    applyConfig(updated, request.flowCode(), request.flowName(), request.assigneeType(), request.userId(), request.roleId(), request.dynamicAssignee(),
        request.autoAction(), request.slaHours(), request.escalationRoleId(), request.stepPolicy(), request.approvalMode(), request.sequenceNo(),
        request.conditionType(), request.minAmount(), request.maxAmount(), request.departmentName(), request.businessType(),
        request.projectCode(), request.supplierRisk(), request.customerLevel(), request.priority(), request.remark(),
        request.enabled() == null || request.enabled(), id);
    next.add(updated);
    repository.saveAll(next);
    return toResponse(updated);
  }

  @Transactional
  public void delete(UUID id) {
    ApprovalAssigneeConfig config = repository.findById(id).orElseThrow(() -> new BusinessException("审批配置不存在"));
    int nextVersion = nextVersion(config.getFlowCode());
    List<ApprovalAssigneeConfig> next = latestEnabled(repository.findByFlowCodeAndEnabledTrue(config.getFlowCode())).stream()
        .filter(item -> !item.getId().equals(id))
        .map(item -> cloneConfig(item, nextVersion))
        .toList();
    repository.saveAll(next);
  }

  @Transactional
  public List<ApprovalConfigResponse> copyFlow(CopyApprovalFlowRequest request) {
    List<ApprovalAssigneeConfig> source = latestEnabled(repository.findByFlowCodeAndEnabledTrue(request.sourceFlowCode()));
    if (source.isEmpty()) throw new BusinessException("源审批流没有可复制的规则");
    List<ApprovalAssigneeConfig> existing = latestEnabled(repository.findByFlowCodeAndEnabledTrue(request.targetFlowCode()));
    if (!existing.isEmpty() && !request.overwrite()) throw new BusinessException("目标审批流已有规则，请勾选覆盖后再复制");
    int nextVersion = nextVersion(request.targetFlowCode());
    List<ApprovalAssigneeConfig> copied = source.stream().map(item -> {
      ApprovalAssigneeConfig copy = cloneConfig(item, nextVersion);
      copy.setFlowCode(request.targetFlowCode());
      copy.setFlowName(request.targetFlowName());
      return copy;
    }).toList();
    return repository.saveAll(copied).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<ApprovalFlowDiagnostic> diagnostics() {
    return repository.findAllByOrderByFlowCodeAscCreatedAtAsc().stream()
        .collect(Collectors.groupingBy(ApprovalAssigneeConfig::getFlowCode))
        .entrySet().stream()
        .flatMap(entry -> diagnose(entry.getKey(), latestEnabled(entry.getValue())).stream())
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ApprovalFlowVersionResponse> versions(String flowCode) {
    return repository.findByFlowCodeAndEnabledTrue(flowCode).stream()
        .collect(Collectors.groupingBy(ApprovalAssigneeConfig::getVersionNo))
        .entrySet().stream()
        .sorted(Map.Entry.<Integer, List<ApprovalAssigneeConfig>>comparingByKey().reversed())
        .map(entry -> {
          List<ApprovalAssigneeConfig> items = entry.getValue();
          ApprovalAssigneeConfig first = items.get(0);
          String status = items.stream().map(ApprovalAssigneeConfig::getPublishStatus).filter(v -> v != null).findFirst().orElse("PUBLISHED");
          return new ApprovalFlowVersionResponse(flowCode, first.getFlowName(), entry.getKey(), items.size(), status);
        }).toList();
  }

  @Transactional
  public List<ApprovalConfigResponse> publish(String flowCode) {
    List<ApprovalAssigneeConfig> latest = latestEnabled(repository.findByFlowCodeAndEnabledTrue(flowCode));
    if (latest.isEmpty()) throw new BusinessException("审批流没有可发布的规则");
    List<ApprovalFlowDiagnostic> problems = diagnose(flowCode, latest).stream().filter(item -> "HIGH".equals(item.severity())).toList();
    if (!problems.isEmpty()) throw new BusinessException("审批流存在高风险问题，不能发布：" + problems.get(0).message());
    latest.forEach(item -> item.setPublishStatus("PUBLISHED"));
    return repository.saveAll(latest).stream().map(this::toResponse).toList();
  }

  @Transactional
  public List<ApprovalConfigResponse> rollback(String flowCode, int versionNo) {
    List<ApprovalAssigneeConfig> source = repository.findByFlowCodeAndVersionNoAndEnabledTrue(flowCode, versionNo);
    if (source.isEmpty()) throw new BusinessException("指定审批流版本不存在");
    int nextVersion = nextVersion(flowCode);
    return repository.saveAll(source.stream().map(item -> {
      ApprovalAssigneeConfig copy = cloneConfig(item, nextVersion);
      copy.setPublishStatus("PUBLISHED");
      return copy;
    }).toList()).stream().map(this::toResponse).toList();
  }

  private void applyConfig(ApprovalAssigneeConfig config, String flowCode, String flowName, String assigneeType, UUID userId, UUID roleId,
                           String dynamicAssignee, String autoAction, Integer slaHours, UUID escalationRoleId,
                           String stepPolicy,
                           String approvalMode, int sequenceNo, String conditionType, BigDecimal minAmount,
                           BigDecimal maxAmount, String departmentName, String businessType, String projectCode,
                           String supplierRisk, String customerLevel, Integer priority, String remark,
                           boolean enabled, UUID currentId) {
    if (!List.of("USER", "ROLE", "DYNAMIC", "AUTO").contains(assigneeType)) throw new BusinessException("审批对象类型不正确");
    if ("USER".equals(assigneeType)) {
      if (userId == null || !userRepository.existsById(userId)) throw new BusinessException("审批人员不存在");
      roleId = null; dynamicAssignee = null; autoAction = null;
    } else if ("ROLE".equals(assigneeType)) {
      if (roleId == null || !roleRepository.existsById(roleId)) throw new BusinessException("审批角色不存在");
      userId = null; dynamicAssignee = null; autoAction = null;
    } else if ("DYNAMIC".equals(assigneeType)) {
      if (!List.of("DEPARTMENT_LEADER", "DIRECT_MANAGER", "PROJECT_MANAGER", "CUSTOMER_OWNER", "FINANCE_MANAGER", "PROCUREMENT_MANAGER", "HR_MANAGER").contains(dynamicAssignee)) throw new BusinessException("动态审批对象不正确");
      userId = null; roleId = null; autoAction = null;
    } else {
      if (!"APPROVE".equals(autoAction)) throw new BusinessException("自动审批动作不正确");
      userId = null; roleId = null; dynamicAssignee = null;
    }
    if (slaHours != null && slaHours < 1) throw new BusinessException("SLA小时数必须大于0");
    if (escalationRoleId != null && !roleRepository.existsById(escalationRoleId)) throw new BusinessException("升级角色不存在");
    String normalizedStepPolicy = stepPolicy == null || stepPolicy.isBlank() ? "ANY_APPROVE" : stepPolicy;
    if (!List.of("ANY_APPROVE", "ALL_APPROVE", "MAJORITY_APPROVE").contains(normalizedStepPolicy)) throw new BusinessException("节点通过策略不正确");
    if (!"PARALLEL".equals(approvalMode) && !"SEQUENTIAL".equals(approvalMode)) throw new BusinessException("审批模式不正确");
    var existing = latestEnabled(repository.findByFlowCodeAndEnabledTrue(flowCode)).stream()
        .filter(item -> currentId == null || !item.getId().equals(currentId))
        .toList();
    if (enabled && !existing.isEmpty() && existing.stream().anyMatch(item -> !approvalMode.equals(item.getApprovalMode()))) {
      throw new BusinessException("同一审批流程不能混用同步审批和依次审批");
    }
    if (sequenceNo < 1) throw new BusinessException("审批顺序必须从1开始");
    String normalizedConditionType = conditionType == null || conditionType.isBlank() ? "ANY" : conditionType;
    if (!List.of("ANY", "AMOUNT", "DEPARTMENT", "AMOUNT_AND_DEPARTMENT", "BUSINESS_TYPE", "PROJECT", "SUPPLIER_RISK", "CUSTOMER_LEVEL", "COMPOSITE").contains(normalizedConditionType)) throw new BusinessException("审批条件类型不正确");
    if ((normalizedConditionType.contains("AMOUNT")) && minAmount == null && maxAmount == null) throw new BusinessException("金额条件至少填写一个边界");
    if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) throw new BusinessException("金额区间下限不能大于上限");
    if (normalizedConditionType.contains("DEPARTMENT") && (departmentName == null || departmentName.isBlank())) throw new BusinessException("部门条件不能为空");
    config.setFlowCode(flowCode); config.setFlowName(flowName); config.setAssigneeType(assigneeType); config.setUserId(userId); config.setRoleId(roleId);
    config.setDynamicAssignee(dynamicAssignee); config.setAutoAction(autoAction); config.setSlaHours(slaHours); config.setEscalationRoleId(escalationRoleId); config.setStepPolicy(normalizedStepPolicy);
    config.setApprovalMode(approvalMode); config.setSequenceNo(sequenceNo); config.setConditionType(normalizedConditionType);
    config.setMinAmount(nonNegative(minAmount, "最小金额不能小于0"));
    config.setMaxAmount(nonNegative(maxAmount, "最大金额不能小于0"));
    config.setDepartmentName(trimToNull(departmentName));
    config.setBusinessType(trimToNull(businessType));
    config.setProjectCode(trimToNull(projectCode));
    config.setSupplierRisk(trimToNull(supplierRisk));
    config.setCustomerLevel(trimToNull(customerLevel));
    config.setPriority(priority == null || priority < 1 ? 100 : priority);
    config.setRemark(trimToNull(remark));
    config.setEnabled(enabled);
  }

  private ApprovalConfigResponse toResponse(ApprovalAssigneeConfig config) {
    String assigneeType = config.getAssigneeType() == null ? "USER" : config.getAssigneeType();
    String assigneeName = assigneeName(config);
    return new ApprovalConfigResponse(config.getId(), config.getFlowCode(), config.getFlowName(), assigneeType,
        config.getUserId(), config.getRoleId(), assigneeName, config.getVersionNo(),
        config.getDynamicAssignee(), config.getAutoAction(), config.getSlaHours(), config.getEscalationRoleId(), escalationRoleName(config),
        config.getStepPolicy() == null ? "ANY_APPROVE" : config.getStepPolicy(), config.getPublishStatus() == null ? "PUBLISHED" : config.getPublishStatus(),
        config.getApprovalMode(), config.getSequenceNo(), config.getConditionType(), config.getMinAmount(), config.getMaxAmount(),
        config.getDepartmentName(), config.getBusinessType(), config.getProjectCode(), config.getSupplierRisk(),
        config.getCustomerLevel(), config.getPriority(), config.getRemark(), config.isEnabled());
  }

  private BigDecimal nonNegative(BigDecimal value, String message) {
    if (value != null && value.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException(message);
    return value;
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }

  private boolean matches(ApprovalAssigneeConfig item, PreviewApprovalFlowRequest request) {
    if (item.getMinAmount() != null && amount(request.amount()).compareTo(item.getMinAmount()) < 0) return false;
    if (item.getMaxAmount() != null && amount(request.amount()).compareTo(item.getMaxAmount()) > 0) return false;
    if (!blank(item.getDepartmentName()) && !equalsText(item.getDepartmentName(), request.departmentName())) return false;
    if (!blank(item.getBusinessType()) && !equalsText(item.getBusinessType(), request.businessType())) return false;
    if (!blank(item.getProjectCode()) && !equalsText(item.getProjectCode(), request.projectCode())) return false;
    if (!blank(item.getSupplierRisk()) && !equalsText(item.getSupplierRisk(), request.supplierRisk())) return false;
    if (!blank(item.getCustomerLevel()) && !equalsText(item.getCustomerLevel(), request.customerLevel())) return false;
    return true;
  }

  private String assigneeName(ApprovalAssigneeConfig config) {
    if ("ROLE".equals(config.getAssigneeType())) return roleRepository.findById(config.getRoleId()).map(item -> item.getName()).orElse("已删除角色");
    if ("DYNAMIC".equals(config.getAssigneeType())) return dynamicAssigneeName(config.getDynamicAssignee());
    if ("AUTO".equals(config.getAssigneeType())) return "自动通过";
    return userRepository.findById(config.getUserId()).map(item -> item.getDisplayName()).orElse("已删除用户");
  }

  private String conditionSummary(ApprovalAssigneeConfig item) {
    StringBuilder builder = new StringBuilder(item.getConditionType());
    if (item.getMinAmount() != null || item.getMaxAmount() != null) builder.append(" 金额 ").append(item.getMinAmount() == null ? "0" : item.getMinAmount()).append("-").append(item.getMaxAmount() == null ? "不限" : item.getMaxAmount());
    if (!blank(item.getDepartmentName())) builder.append(" 部门 ").append(item.getDepartmentName());
    if (!blank(item.getBusinessType())) builder.append(" 业务 ").append(item.getBusinessType());
    if (!blank(item.getProjectCode())) builder.append(" 项目 ").append(item.getProjectCode());
    if (!blank(item.getSupplierRisk())) builder.append(" 供应商 ").append(item.getSupplierRisk());
    if (!blank(item.getCustomerLevel())) builder.append(" 客户 ").append(item.getCustomerLevel());
    return builder.toString();
  }

  private int nextVersion(String flowCode) {
    return repository.findByFlowCodeAndEnabledTrue(flowCode).stream().mapToInt(ApprovalAssigneeConfig::getVersionNo).max().orElse(0) + 1;
  }

  private List<ApprovalAssigneeConfig> latestEnabled(List<ApprovalAssigneeConfig> items) {
    int version = items.stream().mapToInt(ApprovalAssigneeConfig::getVersionNo).max().orElse(0);
    return items.stream().filter(item -> item.getVersionNo() == version).toList();
  }

  private ApprovalAssigneeConfig cloneConfig(ApprovalAssigneeConfig source, int versionNo) {
    ApprovalAssigneeConfig copy = new ApprovalAssigneeConfig();
    copy.setVersionNo(versionNo);
    copy.setFlowCode(source.getFlowCode()); copy.setFlowName(source.getFlowName()); copy.setAssigneeType(source.getAssigneeType());
    copy.setUserId(source.getUserId()); copy.setRoleId(source.getRoleId()); copy.setDynamicAssignee(source.getDynamicAssignee());
    copy.setAutoAction(source.getAutoAction()); copy.setSlaHours(source.getSlaHours()); copy.setEscalationRoleId(source.getEscalationRoleId());
    copy.setStepPolicy(source.getStepPolicy()); copy.setPublishStatus(source.getPublishStatus());
    copy.setApprovalMode(source.getApprovalMode()); copy.setSequenceNo(source.getSequenceNo()); copy.setConditionType(source.getConditionType());
    copy.setMinAmount(source.getMinAmount()); copy.setMaxAmount(source.getMaxAmount()); copy.setDepartmentName(source.getDepartmentName());
    copy.setBusinessType(source.getBusinessType()); copy.setProjectCode(source.getProjectCode()); copy.setSupplierRisk(source.getSupplierRisk());
    copy.setCustomerLevel(source.getCustomerLevel()); copy.setPriority(source.getPriority()); copy.setRemark(source.getRemark()); copy.setEnabled(source.isEnabled());
    return copy;
  }

  private List<ApprovalFlowDiagnostic> diagnose(String flowCode, List<ApprovalAssigneeConfig> items) {
    String flowName = items.stream().findFirst().map(ApprovalAssigneeConfig::getFlowName).orElse(flowCode);
    java.util.ArrayList<ApprovalFlowDiagnostic> result = new java.util.ArrayList<>();
    if (items.isEmpty()) {
      result.add(new ApprovalFlowDiagnostic(flowCode, flowName, "HIGH", "没有启用规则，审批将回退到旧权限逻辑"));
      return result;
    }
    if (items.stream().noneMatch(item -> item.getSequenceNo() == 1 && "ANY".equals(item.getConditionType()))) {
      result.add(new ApprovalFlowDiagnostic(flowCode, flowName, "MEDIUM", "缺少第1步默认规则，部分单据可能没有明确审批对象"));
    }
    if (items.stream().anyMatch(item -> "ROLE".equals(item.getAssigneeType()) && item.getRoleId() != null && userRepository.countByRoles_Id(item.getRoleId()) == 0)) {
      result.add(new ApprovalFlowDiagnostic(flowCode, flowName, "HIGH", "存在没有成员的审批角色，发起审批时会被拦截"));
    }
    if (hasAmountOverlap(items)) result.add(new ApprovalFlowDiagnostic(flowCode, flowName, "MEDIUM", "同一步金额区间存在重叠，请确认优先级是否符合预期"));
    if (items.stream().anyMatch(item -> "AUTO".equals(item.getAssigneeType()))) {
      result.add(new ApprovalFlowDiagnostic(flowCode, flowName, "LOW", "存在自动通过规则，请确认条件足够收窄"));
    }
    return result;
  }

  private boolean hasAmountOverlap(List<ApprovalAssigneeConfig> items) {
    return items.stream().collect(Collectors.groupingBy(ApprovalAssigneeConfig::getSequenceNo)).values().stream().anyMatch(step -> {
      List<ApprovalAssigneeConfig> ranges = step.stream()
          .filter(item -> item.getMinAmount() != null || item.getMaxAmount() != null)
          .sorted(Comparator.comparing(item -> amount(item.getMinAmount())))
          .toList();
      for (int i = 1; i < ranges.size(); i++) {
        BigDecimal previousEnd = ranges.get(i - 1).getMaxAmount() == null ? new BigDecimal("999999999999999") : ranges.get(i - 1).getMaxAmount();
        if (amount(ranges.get(i).getMinAmount()).compareTo(previousEnd) <= 0) return true;
      }
      return false;
    });
  }

  private String escalationRoleName(ApprovalAssigneeConfig config) {
    return config.getEscalationRoleId() == null ? null : roleRepository.findById(config.getEscalationRoleId()).map(item -> item.getName()).orElse("已删除角色");
  }

  private boolean autoApproved(ApprovalAssigneeConfig config) {
    return "AUTO".equals(config.getAssigneeType()) && "APPROVE".equals(config.getAutoAction());
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

  private boolean equalsText(String left, String right) {
    return right != null && left.trim().equalsIgnoreCase(right.trim());
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
