package com.company.ops.api.modules.qualification.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.Attachment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QualificationAttachmentService {
  private static final long MAX_SIZE = 20L * 1024 * 1024;
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf");
  private final Path storageRoot;

  public QualificationAttachmentService(@Value("${ops.storage.local-path:.local-data/uploads}") String storagePath) {
    this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize().resolve("qualification");
  }

  public Attachment store(MultipartFile file, String operatorName) {
    if (file.isEmpty()) throw new BusinessException("上传文件不能为空");
    if (file.getSize() > MAX_SIZE) throw new BusinessException("单个附件不能超过20MB");
    String original = file.getOriginalFilename() == null ? "attachment" : Path.of(file.getOriginalFilename()).getFileName().toString();
    String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
    if (!ALLOWED_EXTENSIONS.contains(extension)) throw new BusinessException("仅支持 JPG、PNG、WebP 或 PDF 附件");
    try {
      Files.createDirectories(storageRoot);
      String objectKey = UUID.randomUUID() + extension;
      Path target = storageRoot.resolve(objectKey).normalize();
      if (!target.startsWith(storageRoot)) throw new BusinessException("附件路径非法");
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      return new Attachment(UUID.randomUUID().toString(), original, file.getContentType(), file.getSize(),
          "/qualification-files/" + objectKey, OffsetDateTime.now().toString(), operatorName);
    } catch (IOException exception) {
      throw new BusinessException("资质附件保存失败");
    }
  }
}
