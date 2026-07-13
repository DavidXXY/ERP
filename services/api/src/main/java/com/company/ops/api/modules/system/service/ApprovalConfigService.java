package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.ApprovalConfigResponse;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.CreateApprovalConfigRequest;
import com.company.ops.api.modules.system.repository.ApprovalAssigneeConfigRepository;
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

  public ApprovalConfigService(ApprovalAssigneeConfigRepository repository, SystemUserRepository userRepository) {
    this.repository = repository; this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<ApprovalConfigResponse> list() {
    return repository.findAllByOrderByFlowCodeAscCreatedAtAsc().stream().map(this::toResponse).toList();
  }

  @Transactional
  public ApprovalConfigResponse create(CreateApprovalConfigRequest request) {
    if (!userRepository.existsById(request.userId())) throw new BusinessException("审批人员不存在");
    var existing = repository.findByFlowCodeAndEnabledTrue(request.flowCode());
    if (!existing.isEmpty() && existing.stream().anyMatch(item -> !request.approvalMode().equals(item.getApprovalMode()))) {
      throw new BusinessException("同一审批流程不能混用同步审批和依次审批");
    }
    ApprovalAssigneeConfig config = new ApprovalAssigneeConfig();
    config.setFlowCode(request.flowCode()); config.setFlowName(request.flowName()); config.setUserId(request.userId());
    if (!"PARALLEL".equals(request.approvalMode()) && !"SEQUENTIAL".equals(request.approvalMode())) throw new BusinessException("审批模式不正确");
    if (request.sequenceNo() < 1) throw new BusinessException("审批顺序必须从1开始");
    String conditionType = request.conditionType() == null || request.conditionType().isBlank() ? "ANY" : request.conditionType();
    if (!List.of("ANY", "AMOUNT", "DEPARTMENT", "AMOUNT_AND_DEPARTMENT", "BUSINESS_TYPE", "PROJECT", "SUPPLIER_RISK", "CUSTOMER_LEVEL", "COMPOSITE").contains(conditionType)) throw new BusinessException("审批条件类型不正确");
    if ((conditionType.contains("AMOUNT")) && request.minAmount() == null && request.maxAmount() == null) throw new BusinessException("金额条件至少填写一个边界");
    if (request.minAmount() != null && request.maxAmount() != null && request.minAmount().compareTo(request.maxAmount()) > 0) throw new BusinessException("金额区间下限不能大于上限");
    if (conditionType.contains("DEPARTMENT") && (request.departmentName() == null || request.departmentName().isBlank())) throw new BusinessException("部门条件不能为空");
    config.setApprovalMode(request.approvalMode()); config.setSequenceNo(request.sequenceNo());
    config.setConditionType(conditionType);
    config.setMinAmount(nonNegative(request.minAmount(), "最小金额不能小于0"));
    config.setMaxAmount(nonNegative(request.maxAmount(), "最大金额不能小于0"));
    config.setDepartmentName(trimToNull(request.departmentName()));
    config.setBusinessType(trimToNull(request.businessType()));
    config.setProjectCode(trimToNull(request.projectCode()));
    config.setSupplierRisk(trimToNull(request.supplierRisk()));
    config.setCustomerLevel(trimToNull(request.customerLevel()));
    config.setPriority(request.priority() == null || request.priority() < 1 ? 100 : request.priority());
    config.setRemark(trimToNull(request.remark()));
    return toResponse(repository.save(config));
  }

  @Transactional
  public void delete(UUID id) { repository.deleteById(id); }

  private ApprovalConfigResponse toResponse(ApprovalAssigneeConfig config) {
    String userName = userRepository.findById(config.getUserId()).map(item -> item.getDisplayName()).orElse("已删除用户");
    return new ApprovalConfigResponse(config.getId(), config.getFlowCode(), config.getFlowName(), config.getUserId(), userName,
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
