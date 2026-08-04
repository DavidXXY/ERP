package com.company.ops.api.modules.qualification.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.storage.FileStorageService.FilePolicy;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.Attachment;
import com.company.ops.api.modules.qualification.domain.QualificationAttachmentRecord;
import com.company.ops.api.modules.qualification.repository.CompanyQualificationRepository;
import com.company.ops.api.modules.qualification.repository.EmployeeContractRepository;
import com.company.ops.api.modules.qualification.repository.PersonnelCertificateRepository;
import com.company.ops.api.modules.qualification.repository.QualificationAttachmentRecordRepository;
import com.company.ops.api.modules.qualification.repository.QualificationPerformanceRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QualificationAttachmentService {
  private static final long MAX_SIZE = 20L * 1024 * 1024;
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf");
  private static final FilePolicy POLICY = new FilePolicy(
      MAX_SIZE,
      ALLOWED_EXTENSIONS,
      "单个附件不能超过20MB",
      "仅支持 JPG、PNG、WebP 或 PDF 附件",
      true
  );
  private final FileStorageService storageService;
  private final QualificationAttachmentRecordRepository attachmentRepository;
  private final CompanyQualificationRepository companyRepository;
  private final PersonnelCertificateRepository certificateRepository;
  private final EmployeeContractRepository contractRepository;
  private final QualificationPerformanceRepository performanceRepository;
  private final DataScopeService dataScopeService;

  public QualificationAttachmentService(
      FileStorageService storageService,
      QualificationAttachmentRecordRepository attachmentRepository,
      CompanyQualificationRepository companyRepository,
      PersonnelCertificateRepository certificateRepository,
      EmployeeContractRepository contractRepository,
      QualificationPerformanceRepository performanceRepository,
      DataScopeService dataScopeService
  ) {
    this.storageService = storageService;
    this.attachmentRepository = attachmentRepository;
    this.companyRepository = companyRepository;
    this.certificateRepository = certificateRepository;
    this.contractRepository = contractRepository;
    this.performanceRepository = performanceRepository;
    this.dataScopeService = dataScopeService;
  }

  @Transactional
  public Attachment store(MultipartFile file) {
    String operatorName = dataScopeService.currentActorName();
    FileStorageService.StoredFile stored = null;
    try {
      stored = storageService.store(file, "qualification", POLICY);
      QualificationAttachmentRecord record = new QualificationAttachmentRecord();
      record.setObjectKey(stored.objectKey());
      record.setOwnerUserId(dataScopeService.currentUserId());
      record.setOriginalName(stored.originalName());
      record.setContentType(stored.contentType());
      record.setSizeBytes(stored.sizeBytes());
      attachmentRepository.save(record);
      return new Attachment(UUID.randomUUID().toString(), stored.originalName(), stored.contentType(), stored.sizeBytes(),
          "/api/qualification-files/" + stored.objectKey(), OffsetDateTime.now().toString(), operatorName);
    } catch (BusinessException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      if (stored != null) {
        try {
          storageService.delete(stored.relativePath());
        } catch (RuntimeException ignored) {
          // Preserve the original persistence failure; orphan cleanup is best effort.
        }
      }
      throw new BusinessException("资质附件保存失败");
    }
  }

  @Transactional(readOnly = true)
  public org.springframework.core.io.Resource load(String objectKey) {
    authorize(objectKey);
    return storageService.loadInNamespace("qualification", objectKey);
  }

  private void authorize(String objectKey) {
    if (objectKey == null || objectKey.isBlank() || objectKey.contains("/") || objectKey.contains("\\")) {
      throw new BusinessException("附件地址无效");
    }
    UUID currentUserId = dataScopeService.currentUserId();
    boolean uploadedByCurrentUser = attachmentRepository.findByObjectKey(objectKey)
        .map(QualificationAttachmentRecord::getOwnerUserId)
        .filter(currentUserId::equals).isPresent();
    boolean linked = false;

    if (companyRepository.existsByAttachmentsJsonContaining(objectKey)) {
      linked = true;
      if (hasEither("qualification:company:view", "qualification:company:manage")) return;
    }
    if (performanceRepository.existsByAttachmentsJsonContaining(objectKey)) {
      linked = true;
      if (hasEither("qualification:performance:view", "qualification:performance:manage")) return;
    }

    var certificate = certificateRepository.findFirstByAttachmentsJsonContaining(objectKey);
    if (certificate.isPresent()) {
      linked = true;
      if (hasEither("qualification:certificate:view", "qualification:certificate:manage")) return;
      var employeeUser = certificate.get().getEmployee().getSystemUser();
      if (employeeUser != null && currentUserId.equals(employeeUser.getId())) return;
    }

    var contract = contractRepository.findFirstByAttachmentsJsonContaining(objectKey);
    if (contract.isPresent()) {
      linked = true;
      if (hasEither("qualification:employee:view", "qualification:employee:manage")) return;
      var employeeUser = contract.get().getEmployee().getSystemUser();
      if (employeeUser != null && currentUserId.equals(employeeUser.getId())) return;
    }
    if (!linked && uploadedByCurrentUser) return;
    throw new AccessDeniedException("无权访问该资质附件");
  }

  private boolean hasEither(String first, String second) {
    return dataScopeService.hasAuthority(first) || dataScopeService.hasAuthority(second);
  }
}
