package com.company.ops.api.modules.crm.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.storage.FileStorageService.FilePolicy;
import com.company.ops.api.modules.crm.domain.CrmAttachment;
import com.company.ops.api.modules.crm.repository.CrmAttachmentRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CrmAttachmentService {

  private static final long MAX_SIZE = 30L * 1024 * 1024;
  private static final Set<String> ALLOWED = Set.of(
    ".jpg", ".jpeg", ".png", ".webp", ".pdf", ".doc", ".docx", ".xls", ".xlsx"
  );
  private static final FilePolicy POLICY = new FilePolicy(
      MAX_SIZE,
      ALLOWED,
      "附件不能超过30MB",
      "仅支持图片、PDF、Word、Excel文件",
      true
  );

  private final FileStorageService storageService;
  private final CrmAttachmentRepository repository;

  public CrmAttachmentService(
    FileStorageService storageService,
    CrmAttachmentRepository repository
  ) {
    this.storageService = storageService;
    this.repository = repository;
  }

  public record AttachmentDto(
    UUID id, String entityType, UUID entityId, String attachmentType,
    String fileName, long fileSize, String mimeType, String uploadedAt, String uploadedBy
  ) {}

  public List<AttachmentDto> listByEntity(String entityType, UUID entityId) {
    return repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
      .stream().map(this::toDto).toList();
  }

  public List<AttachmentDto> listByEntityAndType(String entityType, UUID entityId, String attachmentType) {
    return repository.findByEntityTypeAndEntityIdAndAttachmentType(entityType, entityId, attachmentType)
      .stream().map(this::toDto).toList();
  }

  @Transactional
  public AttachmentDto upload(String entityType, UUID entityId, String attachmentType, MultipartFile file, String uploadedBy) {
    var stored = storageService.store(file, "crm", POLICY);
    CrmAttachment attachment = new CrmAttachment();
    attachment.setEntityType(entityType);
    attachment.setEntityId(entityId);
    attachment.setAttachmentType(attachmentType);
    attachment.setFileName(stored.originalName());
    attachment.setFilePath(stored.relativePath());
    attachment.setFileSize(stored.sizeBytes());
    attachment.setMimeType(stored.contentType());
    attachment.setUploadedBy(uploadedBy);
    return toDto(repository.save(attachment));
  }

  @Transactional
  public void delete(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    storageService.delete(att.getFilePath());
    repository.delete(att);
  }

  public org.springframework.core.io.Resource load(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    return storageService.load(att.getFilePath());
  }

  public String temporaryUrl(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    return storageService.temporaryUrl(att.getFilePath());
  }

  public String getFileName(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    return att.getFileName();
  }

  public String getMimeType(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    return att.getMimeType() == null || att.getMimeType().isBlank() ? "application/octet-stream" : att.getMimeType();
  }

  private AttachmentDto toDto(CrmAttachment a) {
    return new AttachmentDto(
      a.getId(), a.getEntityType(), a.getEntityId(), a.getAttachmentType(),
      a.getFileName(), a.getFileSize() != null ? a.getFileSize() : 0L, a.getMimeType(),
      a.getCreatedAt() != null ? a.getCreatedAt().toString() : "",
      a.getUploadedBy()
    );
  }
}
