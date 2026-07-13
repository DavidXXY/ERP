package com.company.ops.api.common.storage;

import com.company.ops.api.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
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
      String objectKey = UUID.randomUUID() + validated.extension();
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
    Path file = storageRoot.resolve(relativePath).normalize();
    ensureInside(storageRoot, file);
    return file;
  }

  public Path resolveInNamespace(String namespace, String objectKey) {
    Path folder = namespaceRoot(namespace);
    Path file = folder.resolve(Path.of(objectKey).getFileName()).normalize();
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
    Path folder = storageRoot.resolve(namespace).normalize();
    ensureInside(storageRoot, folder);
    return folder;
  }

  private static void ensureInside(Path root, Path child) {
    if (!child.startsWith(root)) throw new BusinessException("文件路径非法");
  }
}
