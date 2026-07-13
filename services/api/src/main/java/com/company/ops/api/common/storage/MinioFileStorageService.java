package com.company.ops.api.common.storage;

import com.company.ops.api.common.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "ops.storage.type", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {
  private final MinioClient minioClient;
  private final String bucket;
  private final int presignedExpirySeconds;
  private final AtomicBoolean bucketReady = new AtomicBoolean(false);

  public MinioFileStorageService(
      @Value("${ops.storage.endpoint}") String endpoint,
      @Value("${ops.storage.access-key}") String accessKey,
      @Value("${ops.storage.secret-key}") String secretKey,
      @Value("${ops.storage.bucket}") String bucket,
      @Value("${ops.storage.presigned-expiry-seconds:600}") int presignedExpirySeconds
  ) {
    this.minioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build();
    this.bucket = bucket;
    this.presignedExpirySeconds = Math.max(60, Math.min(presignedExpirySeconds, 604800));
  }

  @Override
  public StoredFile store(MultipartFile file, String namespace, FilePolicy policy) {
    ValidatedFile validated = FileStorageService.validate(file, policy);
    ensureBucket();
    String objectKey = namespace + "/" + UUID.randomUUID() + validated.extension();
    try {
      minioClient.putObject(PutObjectArgs.builder()
          .bucket(bucket)
          .object(objectKey)
          .contentType(file.getContentType())
          .stream(file.getInputStream(), file.getSize(), -1)
          .build());
      return new StoredFile(
          validated.originalName(),
          objectKey.substring(namespace.length() + 1),
          objectKey,
          validated.extension(),
          file.getContentType(),
          file.getSize(),
          null
      );
    } catch (Exception exception) {
      throw new BusinessException("对象存储写入失败");
    }
  }

  @Override
  public Resource load(String relativePath) {
    ensureBucket();
    try {
      return new InputStreamResource(minioClient.getObject(GetObjectArgs.builder()
          .bucket(bucket)
          .object(normalizeObjectKey(relativePath))
          .build()));
    } catch (Exception exception) {
      throw new BusinessException("对象存储文件读取失败");
    }
  }

  @Override
  public void delete(String relativePath) {
    ensureBucket();
    try {
      minioClient.removeObject(RemoveObjectArgs.builder()
          .bucket(bucket)
          .object(normalizeObjectKey(relativePath))
          .build());
    } catch (Exception ignored) {
      // Metadata deletion should still proceed if the physical object is already missing.
    }
  }

  @Override
  public String temporaryUrl(String relativePath) {
    ensureBucket();
    try {
      return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
          .method(Method.GET)
          .bucket(bucket)
          .object(normalizeObjectKey(relativePath))
          .expiry(presignedExpirySeconds)
          .build());
    } catch (Exception exception) {
      throw new BusinessException("对象存储临时链接生成失败");
    }
  }

  private void ensureBucket() {
    if (bucketReady.get()) return;
    try {
      boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      if (!exists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
      }
      bucketReady.set(true);
    } catch (Exception exception) {
      throw new BusinessException("对象存储 bucket 不可用");
    }
  }

  public boolean isAvailable() {
    try {
      ensureBucket();
      return true;
    } catch (BusinessException exception) {
      return false;
    }
  }

  private String normalizeObjectKey(String relativePath) {
    if (relativePath == null || relativePath.isBlank() || relativePath.contains("..") || relativePath.startsWith("/")) {
      throw new BusinessException("文件路径非法");
    }
    return relativePath;
  }
}
