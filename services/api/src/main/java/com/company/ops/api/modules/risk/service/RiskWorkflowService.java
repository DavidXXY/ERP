package com.company.ops.api.modules.risk.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.risk.domain.RiskWorkflow;
import com.company.ops.api.modules.risk.domain.RiskWorkflowAction;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskWorkflowActionResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.BatchUpdateRiskWorkflowRequest;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskWorkflowResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.UpdateRiskWorkflowRequest;
import com.company.ops.api.modules.risk.repository.RiskWorkflowActionRepository;
import com.company.ops.api.modules.risk.repository.RiskWorkflowRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskWorkflowService {
  private static final List<String> STATUSES = List.of("UNCLAIMED", "CLAIMED", "PROCESSING", "IGNORED", "CLOSED");
  private final RiskWorkflowRepository repository;
  private final RiskWorkflowActionRepository actionRepository;

  public RiskWorkflowService(RiskWorkflowRepository repository, RiskWorkflowActionRepository actionRepository) {
    this.repository = repository;
    this.actionRepository = actionRepository;
  }

  @Transactional(readOnly = true)
  public List<RiskWorkflowResponse> list() {
    return repository.findAllByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<RiskWorkflowActionResponse> actions(String riskKey) {
    return actionRepository.findByRiskKeyOrderByCreatedAtDesc(riskKey).stream().map(this::toActionResponse).toList();
  }

  @Transactional
  public RiskWorkflowResponse update(UpdateRiskWorkflowRequest request) {
    return updateOne(request.riskKey(), request.status(), request.owner(), request.note(), request.reason(),
        request.rootCause(), request.responsibleDepartment(), request.handlingHours(), request.recurrence(), request.preventionAction());
  }

  @Transactional
  public List<RiskWorkflowResponse> batchUpdate(BatchUpdateRiskWorkflowRequest request) {
    if (!STATUSES.contains(request.status())) throw new BusinessException("风险处理状态不正确");
    return request.riskKeys().stream()
        .distinct()
        .map(riskKey -> updateOne(riskKey, request.status(), request.owner(), request.note(), request.reason(),
            request.rootCause(), request.responsibleDepartment(), request.handlingHours(), request.recurrence(), request.preventionAction()))
        .toList();
  }

  private RiskWorkflowResponse updateOne(String riskKey, String status, String ownerValue, String noteValue, String reasonValue,
      String rootCauseValue, String responsibleDepartmentValue, Integer handlingHoursValue, Boolean recurrenceValue, String preventionActionValue) {
    if (!STATUSES.contains(status)) throw new BusinessException("风险处理状态不正确");
    RiskWorkflow workflow = repository.findByRiskKey(riskKey).orElseGet(() -> {
      RiskWorkflow created = new RiskWorkflow();
      created.setRiskKey(riskKey);
      return created;
    });
    String previousStatus = workflow.getStatus();
    String owner = trimToNull(ownerValue);
    String note = trimToNull(noteValue);
    String reason = trimToNull(reasonValue);
    String rootCause = trimToNull(rootCauseValue);
    String responsibleDepartment = trimToNull(responsibleDepartmentValue);
    String preventionAction = trimToNull(preventionActionValue);
    Integer handlingHours = handlingHoursValue;
    if ("CLOSED".equals(status) && handlingHours == null && workflow.getCreatedAt() != null) {
      handlingHours = Math.max(0, (int) Duration.between(workflow.getCreatedAt(), OffsetDateTime.now()).toHours());
    }
    String operator = currentDisplayName();
    workflow.setStatus(status);
    workflow.setOwner(owner);
    workflow.setNote(note);
    workflow.setReason(reason);
    workflow.setRootCause(rootCause);
    workflow.setResponsibleDepartment(responsibleDepartment);
    workflow.setHandlingHours(handlingHours);
    workflow.setRecurrence(recurrenceValue);
    workflow.setPreventionAction(preventionAction);
    workflow.setUpdatedByName(operator);
    workflow.setProcessedAt(OffsetDateTime.now());
    RiskWorkflow saved = repository.save(workflow);
    RiskWorkflowAction action = new RiskWorkflowAction();
    action.setRiskKey(riskKey);
    action.setFromStatus(previousStatus);
    action.setToStatus(status);
    action.setOperatorName(operator);
    action.setOwner(owner);
    action.setNote(note);
    action.setReason(reason);
    action.setRootCause(rootCause);
    action.setResponsibleDepartment(responsibleDepartment);
    action.setHandlingHours(handlingHours);
    action.setRecurrence(recurrenceValue);
    action.setPreventionAction(preventionAction);
    actionRepository.save(action);
    return toResponse(saved);
  }

  private RiskWorkflowResponse toResponse(RiskWorkflow workflow) {
    return new RiskWorkflowResponse(
        workflow.getRiskKey(),
        workflow.getStatus(),
        workflow.getOwner(),
        workflow.getNote(),
        workflow.getReason(),
        workflow.getUpdatedByName(),
        workflow.getProcessedAt(),
        workflow.getUpdatedAt(),
        workflow.getRootCause(),
        workflow.getResponsibleDepartment(),
        workflow.getHandlingHours(),
        workflow.getRecurrence(),
        workflow.getPreventionAction()
    );
  }

  private RiskWorkflowActionResponse toActionResponse(RiskWorkflowAction action) {
    return new RiskWorkflowActionResponse(
        action.getRiskKey(),
        action.getFromStatus(),
        action.getToStatus(),
        action.getOperatorName(),
        action.getOwner(),
        action.getNote(),
        action.getReason(),
        action.getRootCause(),
        action.getResponsibleDepartment(),
        action.getHandlingHours(),
        action.getRecurrence(),
        action.getPreventionAction(),
        action.getCreatedAt()
    );
  }

  private String currentDisplayName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) return "系统";
    return principal.displayName();
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }
}
