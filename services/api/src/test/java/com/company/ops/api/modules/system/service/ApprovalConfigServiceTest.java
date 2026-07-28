package com.company.ops.api.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.dto.ApprovalConfigDtos.CreateApprovalConfigRequest;
import com.company.ops.api.modules.system.repository.ApprovalAssigneeConfigRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalConfigServiceTest {
  @Mock private ApprovalAssigneeConfigRepository repository;
  @Mock private SystemUserRepository userRepository;
  @Mock private SystemRoleRepository roleRepository;

  private ApprovalConfigService service;

  @BeforeEach
  void setUp() {
    service = new ApprovalConfigService(repository, userRepository, roleRepository);
  }

  @Test
  void createAllowsSimplePayloadAndAppliesAdvancedDefaults() {
    UUID roleId = UUID.randomUUID();
    SystemRole role = new SystemRole();
    role.setId(roleId);
    role.setName("部门负责人");

    when(repository.findByFlowCodeAndEnabledTrue("QUOTE")).thenReturn(List.of());
    when(roleRepository.existsById(roleId)).thenReturn(true);
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

    // The quick form only supplies the flow, assignee, mode and first step.
    CreateApprovalConfigRequest request = new CreateApprovalConfigRequest(
        "QUOTE", "报价审批", "ROLE", null, roleId, null, null, null, null,
        null, "SEQUENTIAL", 1, null, null, null, null, null, null, null, null,
        null, null
    );

    service.create(request);

    ArgumentCaptor<List<ApprovalAssigneeConfig>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captor.capture());
    ApprovalAssigneeConfig saved = captor.getValue().get(0);
    assertThat(saved.getConditionType()).isEqualTo("ANY");
    assertThat(saved.getStepPolicy()).isEqualTo("ANY_APPROVE");
    assertThat(saved.getPriority()).isEqualTo(100);
    assertThat(saved.getSequenceNo()).isEqualTo(1);
    assertThat(saved.getApprovalMode()).isEqualTo("SEQUENTIAL");
    assertThat(saved.getRoleId()).isEqualTo(roleId);
  }
}
