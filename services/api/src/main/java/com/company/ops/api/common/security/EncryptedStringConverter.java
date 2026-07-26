package com.company.ops.api.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
  private static final String LEGACY_PREFIX = "ENC1:";
  private static final String PREFIX = "ENC2:";
  private static final int IV_BYTES = 12;
  private final String currentKeyId;
  private final SecretKeySpec currentKey;
  private final Map<String, SecretKeySpec> keys;
  private final SecureRandom random = new SecureRandom();

  public EncryptedStringConverter(
      @Value("${ops.security.data-encryption-key-id:primary}") String currentKeyId,
      @Value("${ops.security.data-encryption-key}") String secret,
      @Value("${ops.security.data-encryption-previous-keys:}") String previousKeys) {
    if (currentKeyId == null || !currentKeyId.matches("[A-Za-z0-9_-]{1,32}")) {
      throw new IllegalArgumentException("数据加密密钥标识格式不正确");
    }
    this.currentKeyId = currentKeyId;
    this.currentKey = deriveKey(secret);
    Map<String, SecretKeySpec> configuredKeys = new LinkedHashMap<>();
    configuredKeys.put(currentKeyId, currentKey);
    if (previousKeys != null && !previousKeys.isBlank()) {
      for (String item : previousKeys.split(";")) {
        if (item.isBlank()) continue;
        String[] pair = item.trim().split("=", 2);
        if (pair.length != 2 || !pair[0].matches("[A-Za-z0-9_-]{1,32}")) {
          throw new IllegalArgumentException("历史数据加密密钥格式应为 keyId=secret");
        }
        configuredKeys.putIfAbsent(pair[0], deriveKey(pair[1]));
      }
    }
    this.keys = Map.copyOf(configuredKeys);
  }

  private SecretKeySpec deriveKey(String secret) {
    if (secret == null || secret.length() < 32) {
      throw new IllegalArgumentException("数据加密密钥至少需要 32 个字符");
    }
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(secret.getBytes(StandardCharsets.UTF_8));
      return new SecretKeySpec(digest, "AES");
    } catch (Exception exception) {
      throw new IllegalStateException("无法初始化数据加密", exception);
    }
  }

  @Override
  public String convertToDatabaseColumn(String value) {
    if (value == null || value.isBlank() || value.startsWith(PREFIX) || value.startsWith(LEGACY_PREFIX)) return value;
    try {
      byte[] iv = new byte[IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, currentKey, new GCMParameterSpec(128, iv));
      byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
      byte[] payload = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
      return PREFIX + currentKeyId + ":" + Base64.getEncoder().encodeToString(payload);
    } catch (Exception exception) {
      throw new IllegalStateException("敏感数据加密失败", exception);
    }
  }

  @Override
  public String convertToEntityAttribute(String value) {
    if (value == null || (!value.startsWith(PREFIX) && !value.startsWith(LEGACY_PREFIX))) return value;
    try {
      if (value.startsWith(PREFIX)) {
        int separator = value.indexOf(':', PREFIX.length());
        if (separator < 0) throw new IllegalStateException("敏感数据密钥标识缺失");
        String keyId = value.substring(PREFIX.length(), separator);
        SecretKeySpec key = keys.get(keyId);
        if (key == null) throw new IllegalStateException("未配置历史数据加密密钥: " + keyId);
        return decrypt(value.substring(separator + 1), key);
      }
      String payload = value.substring(LEGACY_PREFIX.length());
      for (SecretKeySpec key : keys.values()) {
        try {
          return decrypt(payload, key);
        } catch (Exception ignored) {
          // ENC1 did not carry a key id, so each configured rotation key must be attempted.
        }
      }
      throw new IllegalStateException("没有密钥可以解密旧版敏感数据");
    } catch (Exception exception) {
      throw new IllegalStateException("敏感数据解密失败", exception);
    }
  }

  private String decrypt(String encodedPayload, SecretKeySpec key) throws Exception {
      byte[] payload = Base64.getDecoder().decode(encodedPayload);
      if (payload.length <= IV_BYTES) throw new IllegalArgumentException("敏感数据密文长度无效");
      byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
      byte[] encrypted = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
  }
}
