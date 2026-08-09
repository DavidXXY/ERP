package com.company.ops.api.common.storage;

import com.company.ops.api.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "ops.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {
  private final Path storageRoot;

  public LocalFileStorageService(@Value("${ops.storage.local-path:.local-data/uploads}") String storagePath) {
    this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
  }

  @Override
  public StoredFile store(MultipartFile file, String namespace, FilePolicy policy) {
    ValidatedFile validated = FileStorageService.validate(file, policy);
    try {
      Path folder = namespaceRoot(namespace);
      Files.createDirectories(folder);
      String objectKey = UUID.randomUUID().toString();
      Path target = folder.resolve(objectKey).normalize();
      ensureInside(folder, target);
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      return new StoredFile(validated.originalName(), objectKey, namespace + "/" + objectKey, validated.extension(), file.getContentType(), file.getSize(), target);
    } catch (IOException exception) {
      throw new BusinessException("文件保存失败");
    }
  }

  @Override
  public Resource load(String relativePath) {
    try {
      Path file = resolve(relativePath);
      if (!Files.exists(file)) throw new BusinessException("文件不存在");
      return new UrlResource(file.toUri());
    } catch (IOException exception) {
      throw new BusinessException("文件读取失败");
    }
  }

  public Path resolve(String relativePath) {
    try {
      Path relative = Path.of(relativePath);
      if (relative.isAbsolute() || relative.getNameCount() != 2 || !relative.equals(relative.normalize())) {
        throw new BusinessException("文件路径非法");
      }
      return resolveInNamespace(relative.getName(0).toString(), relative.getName(1).toString());
    } catch (InvalidPathException exception) {
      throw new BusinessException("文件路径非法");
    }
  }

  public Path resolveInNamespace(String namespace, String objectKey) {
    Path folder = namespaceRoot(namespace);
    Path file = folder.resolve(safeObjectKey(objectKey)).normalize();
    ensureInside(folder, file);
    return file;
  }

  @Override
  public void delete(String relativePath) {
    try {
      Files.deleteIfExists(resolve(relativePath));
    } catch (IOException ignored) {
      // Metadata deletion should still proceed if the physical file is already missing.
    }
  }

  private Path namespaceRoot(String namespace) {
    Path folder = switch (namespace) {
      case "crm" -> storageRoot.resolve("crm");
      case "office" -> storageRoot.resolve("office");
      case "qualification" -> storageRoot.resolve("qualification");
      case "supplier-portal" -> storageRoot.resolve("supplier-portal");
      case "supplier-quotes" -> storageRoot.resolve("supplier-quotes");
      case "procurement-orders" -> storageRoot.resolve("procurement-orders");
      case "work-orders" -> storageRoot.resolve("work-orders");
      default -> throw new BusinessException("文件路径非法");
    };
    ensureInside(storageRoot, folder);
    return folder;
  }

  private static String safeObjectKey(String objectKey) {
    try {
      Path key = Path.of(objectKey);
      if (key.isAbsolute() || key.getNameCount() != 1) throw new BusinessException("文件路径非法");
      String value = key.getFileName().toString();
      if (!value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,199}") || value.contains("..")) {
        throw new BusinessException("文件路径非法");
      }
      return value;
    } catch (InvalidPathException exception) {
      throw new BusinessException("文件路径非法");
    }
  }

  private static void ensureInside(Path root, Path child) {
    if (!child.startsWith(root)) throw new BusinessException("文件路径非法");
  }
}
