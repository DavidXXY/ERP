package com.company.ops.api.modules.qualification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeRequest;
import com.company.ops.api.modules.qualification.repository.CompanyQualificationRepository;
import com.company.ops.api.modules.qualification.repository.EmployeeContractRepository;
import com.company.ops.api.modules.qualification.repository.PersonnelCertificateRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.qualification.repository.QualificationPerformanceRepository;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
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

@ExtendWith(MockitoExtension.class)
class QualificationServiceOrganizationTest {

  @Mock private QualificationEmployeeRepository employeeRepository;
  @Mock private CompanyQualificationRepository companyRepository;
  @Mock private PersonnelCertificateRepository certificateRepository;
  @Mock private QualificationPerformanceRepository performanceRepository;
  @Mock private EmployeeContractRepository contractRepository;
  @Mock private SystemUserRepository systemUserRepository;
  @Mock private SystemOrganizationRepository organizationRepository;
  @Mock private ObjectMapper objectMapper;
  @InjectMocks private QualificationService qualificationService;

  @Test
  void employeeOrganizationChangeSynchronizesLinkedAccount() {
    UUID employeeId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();

    SystemOrganization organization = new SystemOrganization();
    organization.setId(organizationId);
    organization.setName("财务部");
    organization.setEnabled(true);

    SystemUser user = new SystemUser();
    user.setId(userId);

    QualificationEmployee employee = new QualificationEmployee();
    employee.setId(employeeId);
    employee.setName("测试员工");

    when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
    when(employeeRepository.findBySystemUser_Id(userId)).thenReturn(Optional.of(employee));
    when(systemUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(employeeRepository.save(employee)).thenReturn(employee);
    when(certificateRepository.findByEmployeeIdOrderByNameAsc(employeeId)).thenReturn(List.of());

    qualificationService.updateEmployee(employeeId, new EmployeeRequest(
        "测试员工", "EMP-001", organizationId, null, "会计", null, null, null,
        "ACTIVE", null, null, null, null, null, null, userId
    ));

    assertThat(employee.getOrganization()).isSameAs(organization);
    assertThat(employee.getDepartment()).isEqualTo("财务部");
    assertThat(user.getOrganization()).isSameAs(organization);
  }
}
