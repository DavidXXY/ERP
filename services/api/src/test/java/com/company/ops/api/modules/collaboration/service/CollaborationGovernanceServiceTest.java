package com.company.ops.api.modules.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.collaboration.domain.CollaborationTaskControl;
import com.company.ops.api.modules.collaboration.dto.CollaborationDtos.TodoActionRequest;
import com.company.ops.api.modules.collaboration.repository.CollaborationActionLogRepository;
import com.company.ops.api.modules.collaboration.repository.CollaborationTaskControlRepository;
import com.company.ops.api.modules.collaboration.repository.ProjectBudgetVersionRepository;
import com.company.ops.api.modules.collaboration.repository.ProjectStaffAssignmentRepository;
import com.company.ops.api.modules.collaboration.repository.ProjectTimesheetRepository;
import com.company.ops.api.modules.collaboration.repository.TimesheetPeriodLockRepository;
import com.company.ops.api.modules.hr.repository.LeaveRequestRepository;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.service.ProjectCostLedgerService;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CollaborationGovernanceServiceTest {
  @Mock private CollaborationTaskControlRepository controls;
  @Mock private CollaborationActionLogRepository actions;
  @Mock private ProjectTimesheetRepository timesheets;
  @Mock private TimesheetPeriodLockRepository periodLocks;
  @Mock private ProjectBudgetVersionRepository budgetVersions;
  @Mock private ProjectStaffAssignmentRepository assignments;
  @Mock private ProjectRepository projects;
  @Mock private ProjectCostLedgerService costLedger;
  @Mock private SystemUserRepository users;
  @Mock private SystemNotificationRepository notifications;
  @Mock private QualificationEmployeeRepository employees;
  @Mock private LeaveRequestRepository leaveRequests;
  @Mock private DataScopeService dataScopeService;
  @InjectMocks private CollaborationGovernanceService service;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void rejectsMutationWhenCanonicalTodoDoesNotExist() {
    authenticate(UUID.randomUUID());
    UUID sourceId = UUID.randomUUID();
    when(controls.findBySourceTypeAndSourceId("APPROVAL", sourceId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.actOnTodo("APPROVAL", sourceId, request("COMPLETE")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不存在或已失效");
  }

  @Test
  void rejectsNonAssigneeMutation() {
    authenticate(UUID.randomUUID());
    UUID sourceId = UUID.randomUUID();
    CollaborationTaskControl control = control(sourceId, UUID.randomUUID());
    when(controls.findBySourceTypeAndSourceId("APPROVAL", sourceId))
        .thenReturn(Optional.of(control));

    assertThatThrownBy(() -> service.actOnTodo("APPROVAL", sourceId, request("COMPLETE")))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("本人经办");
  }

  @Test
  void allowsAssigneeToCompleteTodo() {
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    UUID sourceId = UUID.randomUUID();
    CollaborationTaskControl control = control(sourceId, userId);
    when(controls.findBySourceTypeAndSourceId("APPROVAL", sourceId))
        .thenReturn(Optional.of(control));

    var result = service.actOnTodo("APPROVAL", sourceId, request("COMPLETE"));

    assertThat(result.get("status")).isEqualTo("DONE");
    assertThat(control.getCompletedAt()).isNotNull();
    verify(controls).save(control);
  }

  private CollaborationTaskControl control(UUID sourceId, UUID assigneeId) {
    CollaborationTaskControl control = new CollaborationTaskControl();
    control.setSourceType("APPROVAL");
    control.setSourceId(sourceId);
    control.setAssigneeUserId(assigneeId);
    return control;
  }

  private TodoActionRequest request(String action) {
    return new TodoActionRequest(action, null, List.of(), null, null);
  }

  private void authenticate(UUID userId) {
    SystemUser user = new SystemUser();
    user.setId(userId);
    user.setTenantId("test");
    user.setUsername("tester");
    user.setDisplayName("测试用户");
    user.setPasswordHash("-");
    UserPrincipal principal = new UserPrincipal(user);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }
}
