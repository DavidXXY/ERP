package com.company.ops.api.common.storage;

import com.company.ops.api.common.exception.BusinessException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
  Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
  Set<String> PDF_TYPES = Set.of("application/pdf");
  Set<String> OFFICE_TYPES = Set.of(
      "application/msword",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "application/vnd.ms-excel",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  );

  StoredFile store(MultipartFile file, String namespace, FilePolicy policy);

  Resource load(String relativePath);

  default String temporaryUrl(String relativePath) {
    return null;
  }

  default Resource loadInNamespace(String namespace, String objectKey) {
    return load(namespace + "/" + objectKey);
  }

  void delete(String relativePath);

  default void deleteInNamespace(String namespace, String objectKey) {
    delete(namespace + "/" + objectKey);
  }

  static ValidatedFile validate(MultipartFile file, FilePolicy policy) {
    if (file == null || file.isEmpty()) throw new BusinessException("上传文件不能为空");
    if (file.getSize() > policy.maxSizeBytes()) throw new BusinessException(policy.maxSizeMessage());
    String original = safeOriginalName(file);
    String extension = extensionOf(original);
    if (!policy.allowedExtensions().contains(extension)) throw new BusinessException(policy.allowedExtensionsMessage());
    validateContentType(extension, normalizeContentType(file.getContentType()), policy);
    return new ValidatedFile(original, extension);
  }

  private static String safeOriginalName(MultipartFile file) {
    String original = file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename();
    original = Path.of(original).getFileName().toString().trim();
    if (original.isBlank()) return "attachment";
    if (original.length() > 240) return original.substring(original.length() - 240);
    return original;
  }

  private static String extensionOf(String filename) {
    int index = filename.lastIndexOf('.');
    return index >= 0 ? filename.substring(index).toLowerCase(Locale.ROOT) : "";
  }

  private static String normalizeContentType(String contentType) {
    return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
  }

  private static void validateContentType(String extension, String contentType, FilePolicy policy) {
    if (contentType.isBlank() || "application/octet-stream".equals(contentType)) return;
    if (isImage(extension) && IMAGE_TYPES.contains(contentType)) return;
    if (".pdf".equals(extension) && PDF_TYPES.contains(contentType)) return;
    if (isOffice(extension) && OFFICE_TYPES.contains(contentType)) return;
    if (!policy.strictContentType()) return;
    throw new BusinessException("文件类型与扩展名不匹配");
  }

  private static boolean isImage(String extension) {
    return Set.of(".jpg", ".jpeg", ".png", ".webp").contains(extension);
  }

  private static boolean isOffice(String extension) {
    return Set.of(".doc", ".docx", ".xls", ".xlsx").contains(extension);
  }

  record StoredFile(
      String originalName,
      String objectKey,
      String relativePath,
      String extension,
      String contentType,
      long sizeBytes,
      Path path
  ) {}

  record FilePolicy(
      long maxSizeBytes,
      Set<String> allowedExtensions,
      String maxSizeMessage,
      String allowedExtensionsMessage,
      boolean strictContentType
  ) {
    public FilePolicy {
      allowedExtensions = Set.copyOf(allowedExtensions);
    }
  }

  record ValidatedFile(String originalName, String extension) {}
}
