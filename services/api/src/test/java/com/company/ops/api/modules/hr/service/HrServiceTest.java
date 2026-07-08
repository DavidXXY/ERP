package com.company.ops.api.modules.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.hr.domain.EmployeeEducation;
import com.company.ops.api.modules.hr.domain.LeaveRequest;
import com.company.ops.api.modules.hr.dto.HrDtos.EducationRequest;
import com.company.ops.api.modules.hr.dto.HrDtos.EducationResponse;
import com.company.ops.api.modules.hr.dto.HrDtos.LeaveRequestData;
import com.company.ops.api.modules.hr.dto.HrDtos.LeaveResponse;
import com.company.ops.api.modules.hr.dto.HrDtos.ApproveRequest;
import com.company.ops.api.modules.hr.repository.EmployeeEducationRepository;
import com.company.ops.api.modules.hr.repository.EmployeeLifecycleRecordRepository;
import com.company.ops.api.modules.hr.repository.EmployeeWorkExperienceRepository;
import com.company.ops.api.modules.hr.repository.EmergencyContactRepository;
import com.company.ops.api.modules.hr.repository.LeaveBalanceRepository;
import com.company.ops.api.modules.hr.repository.LeaveRequestRepository;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HrServiceTest {
  @Mock private QualificationEmployeeRepository empRepo;
  @Mock private EmployeeEducationRepository eduRepo;
  @Mock private EmployeeWorkExperienceRepository workRepo;
  @Mock private EmergencyContactRepository contactRepo;
  @Mock private EmployeeLifecycleRecordRepository lifeRepo;
  @Mock private LeaveRequestRepository leaveRepo;
  @Mock private LeaveBalanceRepository balRepo;
  @InjectMocks private HrService hrService;

  @Test
  void createEducation() {
    UUID empId = UUID.randomUUID();
    QualificationEmployee emp = new QualificationEmployee();
    emp.setId(empId); emp.setName("T");
    when(empRepo.findById(empId)).thenReturn(Optional.of(emp));
    when(eduRepo.save(any())).thenAnswer(i -> {
      EmployeeEducation e = i.getArgument(0);
      e.setId(UUID.randomUUID()); e.setEmployee(emp); return e;
    });
    EducationResponse resp = hrService.createEducation(empId,
        new EducationRequest("清华大学", "本科", "CS", LocalDate.of(2015,9,1), LocalDate.of(2019,7,1), true, ""));
    assertThat(resp).isNotNull();
    assertThat(resp.schoolName()).isEqualTo("清华大学");
  }

  @Test
  void approveLeave() {
    UUID empId = UUID.randomUUID();
    QualificationEmployee emp = new QualificationEmployee();
    emp.setId(empId); emp.setName("T"); emp.setEmploymentStatus("ACTIVE");
    LeaveRequest leave = new LeaveRequest();
    leave.setId(UUID.randomUUID()); leave.setEmployee(emp); leave.setStatus("PENDING"); leave.setTotalDays(3);
    when(leaveRepo.findById(leave.getId())).thenReturn(Optional.of(leave));
    when(leaveRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    LeaveResponse resp = hrService.approveLeave(leave.getId(), new ApproveRequest(true, "OK", "Admin"));
    assertThat(resp).isNotNull();
    assertThat(resp.status()).isEqualTo("APPROVED");
  }
}
