package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalConfigResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.CreateApprovalConfigRequest;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.UpdateApprovalConfigRequest;
import com.company.ops.api.modules.system.repository.ApprovalAssigneeConfigRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
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
    return repository.findAllByOrderByFlowCodeAscCreatedAtAsc().stream().map(this::toResponse).toList();
  }

  @Transactional
  public ApprovalConfigResponse create(CreateApprovalConfigRequest request) {
    ApprovalAssigneeConfig config = new ApprovalAssigneeConfig();
    applyConfig(config, request.flowCode(), request.flowName(), request.assigneeType(), request.userId(), request.roleId(), request.approvalMode(), request.sequenceNo(),
        request.conditionType(), request.minAmount(), request.maxAmount(), request.departmentName(), request.businessType(),
        request.projectCode(), request.supplierRisk(), request.customerLevel(), request.priority(), request.remark(), true, null);
    return toResponse(repository.save(config));
  }

  @Transactional
  public ApprovalConfigResponse update(UUID id, UpdateApprovalConfigRequest request) {
    ApprovalAssigneeConfig config = repository.findById(id).orElseThrow(() -> new BusinessException("审批配置不存在"));
    applyConfig(config, request.flowCode(), request.flowName(), request.assigneeType(), request.userId(), request.roleId(), request.approvalMode(), request.sequenceNo(),
        request.conditionType(), request.minAmount(), request.maxAmount(), request.departmentName(), request.businessType(),
        request.projectCode(), request.supplierRisk(), request.customerLevel(), request.priority(), request.remark(),
        request.enabled() == null || request.enabled(), id);
    return toResponse(repository.save(config));
  }

  @Transactional
  public void delete(UUID id) { repository.deleteById(id); }

  private void applyConfig(ApprovalAssigneeConfig config, String flowCode, String flowName, String assigneeType, UUID userId, UUID roleId,
                           String approvalMode, int sequenceNo, String conditionType, BigDecimal minAmount,
                           BigDecimal maxAmount, String departmentName, String businessType, String projectCode,
                           String supplierRisk, String customerLevel, Integer priority, String remark,
                           boolean enabled, UUID currentId) {
    if (!"USER".equals(assigneeType) && !"ROLE".equals(assigneeType)) throw new BusinessException("审批对象类型不正确");
    if ("USER".equals(assigneeType)) {
      if (userId == null || !userRepository.existsById(userId)) throw new BusinessException("审批人员不存在");
      roleId = null;
    } else {
      if (roleId == null || !roleRepository.existsById(roleId)) throw new BusinessException("审批角色不存在");
      userId = null;
    }
    if (!"PARALLEL".equals(approvalMode) && !"SEQUENTIAL".equals(approvalMode)) throw new BusinessException("审批模式不正确");
    var existing = repository.findByFlowCodeAndEnabledTrue(flowCode).stream()
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
    String assigneeName = "ROLE".equals(assigneeType)
        ? roleRepository.findById(config.getRoleId()).map(item -> item.getName()).orElse("已删除角色")
        : userRepository.findById(config.getUserId()).map(item -> item.getDisplayName()).orElse("已删除用户");
    return new ApprovalConfigResponse(config.getId(), config.getFlowCode(), config.getFlowName(), assigneeType,
        config.getUserId(), config.getRoleId(), assigneeName,
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
}
