package com.company.ops.api.modules.qualification.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.storage.FileStorageService.FilePolicy;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.Attachment;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
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

  public QualificationAttachmentService(FileStorageService storageService) {
    this.storageService = storageService;
  }

  public Attachment store(MultipartFile file, String operatorName) {
    try {
      var stored = storageService.store(file, "qualification", POLICY);
      return new Attachment(UUID.randomUUID().toString(), stored.originalName(), stored.contentType(), stored.sizeBytes(),
          "/api/qualification-files/" + stored.objectKey(), OffsetDateTime.now().toString(), operatorName);
    } catch (BusinessException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new BusinessException("资质附件保存失败");
    }
  }

  public org.springframework.core.io.Resource load(String objectKey) {
    return storageService.loadInNamespace("qualification", objectKey);
  }
}
