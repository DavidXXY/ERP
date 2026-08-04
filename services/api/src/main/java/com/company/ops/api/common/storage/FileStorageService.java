package com.company.ops.api.common.storage;

import com.company.ops.api.common.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
    if (policy.strictContentType()) validateSignature(file, extension);
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
    if ((".jpg".equals(extension) || ".jpeg".equals(extension)) && "image/jpeg".equals(contentType)) return;
    if (".png".equals(extension) && "image/png".equals(contentType)) return;
    if (".webp".equals(extension) && "image/webp".equals(contentType)) return;
    if (".pdf".equals(extension) && PDF_TYPES.contains(contentType)) return;
    if (".doc".equals(extension) && "application/msword".equals(contentType)) return;
    if (".docx".equals(extension)
        && "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) return;
    if (".xls".equals(extension) && "application/vnd.ms-excel".equals(contentType)) return;
    if (".xlsx".equals(extension)
        && "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType)) return;
    if (!policy.strictContentType()) return;
    throw new BusinessException("文件类型与扩展名不匹配");
  }

  private static void validateSignature(MultipartFile file, String extension) {
    try (InputStream input = file.getInputStream()) {
      byte[] header = input.readNBytes(12);
      boolean valid = switch (extension) {
        case ".jpg", ".jpeg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
        case ".png" -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
        case ".webp" -> header.length >= 12
            && asciiAt(header, 0, "RIFF") && asciiAt(header, 8, "WEBP");
        case ".pdf" -> asciiAt(header, 0, "%PDF-");
        case ".doc", ".xls" -> startsWith(header, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
        case ".docx" -> isOpenXml(file, "word/");
        case ".xlsx" -> isOpenXml(file, "xl/");
        default -> false;
      };
      if (!valid) throw new BusinessException("文件内容与扩展名不匹配");
    } catch (BusinessException exception) {
      throw exception;
    } catch (IOException exception) {
      throw new BusinessException("无法读取上传文件");
    }
  }

  private static boolean isOpenXml(MultipartFile file, String requiredPrefix) throws IOException {
    boolean contentTypes = false;
    boolean requiredDirectory = false;
    int entries = 0;
    try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null && entries++ < 10_000) {
        String name = entry.getName();
        if ("[Content_Types].xml".equals(name)) contentTypes = true;
        if (name.startsWith(requiredPrefix)) requiredDirectory = true;
        if (contentTypes && requiredDirectory) return true;
      }
    }
    return false;
  }

  private static boolean startsWith(byte[] bytes, int... expected) {
    if (bytes.length < expected.length) return false;
    for (int i = 0; i < expected.length; i++) {
      if ((bytes[i] & 0xFF) != expected[i]) return false;
    }
    return true;
  }

  private static boolean asciiAt(byte[] bytes, int offset, String value) {
    byte[] expected = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    return bytes.length >= offset + expected.length
        && Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + expected.length), expected);
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
