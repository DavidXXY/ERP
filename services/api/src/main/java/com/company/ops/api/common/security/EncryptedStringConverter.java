package com.company.ops.api.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
  private static final String PREFIX = "ENC1:";
  private static final int IV_BYTES = 12;
  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public EncryptedStringConverter(@Value("${ops.security.data-encryption-key}") String secret) {
    if (secret == null || secret.length() < 32) {
      throw new IllegalArgumentException("数据加密密钥至少需要 32 个字符");
    }
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(secret.getBytes(StandardCharsets.UTF_8));
      this.key = new SecretKeySpec(digest, "AES");
    } catch (Exception exception) {
      throw new IllegalStateException("无法初始化数据加密", exception);
    }
  }

  @Override
  public String convertToDatabaseColumn(String value) {
    if (value == null || value.isBlank() || value.startsWith(PREFIX)) return value;
    try {
      byte[] iv = new byte[IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
      byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
      byte[] payload = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
      return PREFIX + Base64.getEncoder().encodeToString(payload);
    } catch (Exception exception) {
      throw new IllegalStateException("敏感数据加密失败", exception);
    }
  }

  @Override
  public String convertToEntityAttribute(String value) {
    if (value == null || !value.startsWith(PREFIX)) return value;
    try {
      byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
      byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
      byte[] encrypted = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new IllegalStateException("敏感数据解密失败", exception);
    }
  }
}
