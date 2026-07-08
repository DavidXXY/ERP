package com.company.ops.api.modules.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.hr.repository.EmployeeEducationRepository;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class HrExportImportServiceTest {
  @Mock private QualificationEmployeeRepository empRepo;
  @Mock private EmployeeEducationRepository eduRepo;
  @InjectMocks private HrExportImportService exportService;

  @Test
  void exportEmployeesExcel() {
    QualificationEmployee emp = new QualificationEmployee();
    emp.setId(UUID.randomUUID()); emp.setName("Zhang"); emp.setPhone("13800000000"); emp.setEmploymentStatus("ACTIVE");
    when(empRepo.findAll()).thenReturn(List.of(emp));
    Resource r = exportService.exportEmployeesExcel();
    assertThat(r).isNotNull(); try { assertThat(r.contentLength()).isGreaterThan(0); } catch (java.io.IOException e) { throw new RuntimeException(e); }
  }

  @Test
  void downloadImportTemplate() {
    Resource r = exportService.downloadImportTemplate();
    assertThat(r).isNotNull(); try { assertThat(r.contentLength()).isGreaterThan(0); } catch (java.io.IOException e) { throw new RuntimeException(e); }
  }
}
