package com.company.ops.api.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动时校验关键密钥：生产环境禁止使用仓库中公开的弱默认值，避免 JWT 伪造与密文被解密。
 * 本地开发仍可使用默认值，但会输出警告。
 */
@Component
public class SecuritySecretsGuard implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(SecuritySecretsGuard.class);

  private static final Set<String> KNOWN_WEAK_SECRETS = Set.of(
      "ops-erp-local-development-secret-please-change",
      "ops-erp-local-data-encryption-key-change-me",
      "replace-with-at-least-32-characters-secret",
      "ops_erp",
      "ops_erp_minio_password",
      "ops_erp_minio");

  private static final List<String> PROD_CRITICAL_PROPS = List.of(
      "ops.security.jwt-secret",
      "ops.security.data-encryption-key",
      "spring.datasource.password",
      "ops.storage.secret-key");

  private final Environment environment;

  public SecuritySecretsGuard(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void run(ApplicationArguments args) {
    boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    for (String prop : PROD_CRITICAL_PROPS) {
      String value = environment.getProperty(prop);
      boolean weak = value == null || value.isBlank() || KNOWN_WEAK_SECRETS.contains(value);
      if (weak) {
        if (prod) {
          throw new IllegalStateException("生产环境禁止使用默认或弱密钥，请为 " + prop
              + " 配置唯一的高强度密钥（JWT/加密密钥至少 32 字符）");
        }
        log.warn("检测到密钥 {} 使用了仓库默认弱值，仅限本地开发；部署前必须替换", prop);
      }
    }
    if (prod && environment.getProperty("ops.security.jwt-secret", "").length() < 32) {
      throw new IllegalStateException("ops.security.jwt-secret 至少需要 32 个字符");
    }
  }
}
