package com.company.ops.api.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.qualification.domain.PersonnelCertificate;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.repository.EmployeeContractRepository;
import com.company.ops.api.modules.qualification.repository.PersonnelCertificateRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.ChangeMyPasswordRequest;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.MyCertificateRequest;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.UpdateMyProfileRequest;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PersonalSettingsServiceTest {
  @Mock private SystemUserRepository userRepository;
  @Mock private QualificationEmployeeRepository employeeRepository;
  @Mock private PersonnelCertificateRepository certificateRepository;
  @Mock private EmployeeContractRepository contractRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private ObjectMapper objectMapper;
  @InjectMocks private PersonalSettingsService personalSettingsService;

  @Test
  void passwordChangeRequiresCurrentPassword() {
    UUID userId = UUID.randomUUID();
    SystemUser user = user(userId);
    user.setPasswordHash("encoded-current");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "encoded-current")).thenReturn(false);

    assertThatThrownBy(() -> personalSettingsService.changePassword(
        userId, new ChangeMyPasswordRequest("wrong-password", "NewPassword123")
    )).isInstanceOf(BusinessException.class).hasMessage("当前密码不正确");
  }

  @Test
  void employeeCannotUpdateAnotherEmployeesCertificate() {
    UUID userId = UUID.randomUUID();
    UUID certificateId = UUID.randomUUID();
    QualificationEmployee currentEmployee = employee("当前员工");
    QualificationEmployee otherEmployee = employee("其他员工");
    PersonnelCertificate certificate = new PersonnelCertificate();
    certificate.setId(certificateId);
    certificate.setEmployee(otherEmployee);

    when(employeeRepository.findBySystemUser_Id(userId)).thenReturn(Optional.of(currentEmployee));
    when(certificateRepository.findById(certificateId)).thenReturn(Optional.of(certificate));

    MyCertificateRequest request = new MyCertificateRequest(
        "低压电工作业证", null, null, null, null, null, null, List.of(), null
    );
    assertThatThrownBy(() -> personalSettingsService.updateCertificate(userId, certificateId, request))
        .isInstanceOf(BusinessException.class)
        .hasMessage("无权操作其他员工的证书");
  }

  @Test
  void profilePhoneSynchronizesEmployeeFile() {
    UUID userId = UUID.randomUUID();
    SystemUser user = user(userId);
    QualificationEmployee employee = employee("当前员工");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(employeeRepository.findBySystemUser_Id(userId)).thenReturn(Optional.of(employee));
    when(userRepository.save(user)).thenReturn(user);

    personalSettingsService.updateProfile(
        userId, new UpdateMyProfileRequest("新的显示名", "13800000001", "employee@example.com")
    );

    assertThat(user.getDisplayName()).isEqualTo("新的显示名");
    assertThat(employee.getPhone()).isEqualTo("13800000001");
  }

  private SystemUser user(UUID id) {
    SystemUser user = new SystemUser();
    user.setId(id);
    user.setUsername("employee");
    user.setDisplayName("员工");
    user.setEnabled(true);
    return user;
  }

  private QualificationEmployee employee(String name) {
    QualificationEmployee employee = new QualificationEmployee();
    employee.setId(UUID.randomUUID());
    employee.setName(name);
    return employee;
  }
}
