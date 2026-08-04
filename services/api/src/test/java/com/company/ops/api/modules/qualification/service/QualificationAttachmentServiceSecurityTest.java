package com.company.ops.api.modules.qualification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.modules.qualification.domain.QualificationAttachmentRecord;
import com.company.ops.api.modules.qualification.repository.CompanyQualificationRepository;
import com.company.ops.api.modules.qualification.repository.EmployeeContractRepository;
import com.company.ops.api.modules.qualification.repository.PersonnelCertificateRepository;
import com.company.ops.api.modules.qualification.repository.QualificationAttachmentRecordRepository;
import com.company.ops.api.modules.qualification.repository.QualificationPerformanceRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class QualificationAttachmentServiceSecurityTest {
  @Mock private FileStorageService storageService;
  @Mock private QualificationAttachmentRecordRepository attachmentRepository;
  @Mock private CompanyQualificationRepository companyRepository;
  @Mock private PersonnelCertificateRepository certificateRepository;
  @Mock private EmployeeContractRepository contractRepository;
  @Mock private QualificationPerformanceRepository performanceRepository;
  @Mock private DataScopeService dataScopeService;
  private QualificationAttachmentService service;

  @BeforeEach
  void setUp() {
    service = new QualificationAttachmentService(storageService, attachmentRepository, companyRepository,
        certificateRepository, contractRepository, performanceRepository, dataScopeService);
  }

  @Test
  void uploaderCannotBypassPermissionsAfterFileIsLinked() {
    String objectKey = UUID.randomUUID() + ".pdf";
    UUID userId = UUID.randomUUID();
    QualificationAttachmentRecord record = new QualificationAttachmentRecord();
    record.setOwnerUserId(userId);
    record.setObjectKey(objectKey);
    when(dataScopeService.currentUserId()).thenReturn(userId);
    when(attachmentRepository.findByObjectKey(objectKey)).thenReturn(Optional.of(record));
    when(companyRepository.existsByAttachmentsJsonContaining(objectKey)).thenReturn(true);

    assertThatThrownBy(() -> service.load(objectKey))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("无权访问该资质附件");
  }
}
