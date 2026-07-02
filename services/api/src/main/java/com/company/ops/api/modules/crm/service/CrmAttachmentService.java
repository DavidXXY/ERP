package com.company.ops.api.modules.crm.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.CrmAttachment;
import com.company.ops.api.modules.crm.repository.CrmAttachmentRepository;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CrmAttachmentService {

  private static final long MAX_SIZE = 30L * 1024 * 1024;
  private static final Set<String> ALLOWED = Set.of(
    ".jpg", ".jpeg", ".png", ".webp", ".pdf", ".doc", ".docx", ".xls", ".xlsx"
  );

  private final Path storageRoot;
  private final CrmAttachmentRepository repository;

  public CrmAttachmentService(
    @Value("${ops.storage.local-path:.local-data/uploads}") String storagePath,
    CrmAttachmentRepository repository
  ) {
    this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize().resolve("crm");
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
    if (file.isEmpty()) throw new BusinessException("文件不能为空");
    if (file.getSize() > MAX_SIZE) throw new BusinessException("附件不能超过30MB");
    String original = file.getOriginalFilename();
    if (original == null) original = "attachment";
    original = Path.of(original).getFileName().toString();
    String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
    if (!ALLOWED.contains(ext)) throw new BusinessException("仅支持图片、PDF、Word、Excel文件");
    try {
      Files.createDirectories(storageRoot);
      String objectKey = UUID.randomUUID() + ext;
      Path target = storageRoot.resolve(objectKey).normalize();
      if (!target.startsWith(storageRoot)) throw new BusinessException("路径非法");
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

      CrmAttachment attachment = new CrmAttachment();
      attachment.setEntityType(entityType);
      attachment.setEntityId(entityId);
      attachment.setAttachmentType(attachmentType);
      attachment.setFileName(original);
      attachment.setFilePath("crm/" + objectKey);
      attachment.setFileSize(file.getSize());
      attachment.setMimeType(file.getContentType());
      attachment.setUploadedBy(uploadedBy);
      return toDto(repository.save(attachment));
    } catch (IOException e) {
      throw new BusinessException("附件保存失败");
    }
  }

  @Transactional
  public void delete(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    try {
      Path filePath = storageRoot.resolve(Path.of(att.getFilePath()).getFileName());
      Files.deleteIfExists(filePath);
    } catch (IOException ignored) {}
    repository.delete(att);
  }

  public Path getFilePath(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    return storageRoot.resolve(Path.of(att.getFilePath()).getFileName());
  }

  public String getFileName(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    return att.getFileName();
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
