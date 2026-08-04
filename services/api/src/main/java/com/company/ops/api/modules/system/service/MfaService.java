package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.dto.MfaDtos.ConfirmMfaResponse;
import com.company.ops.api.modules.system.dto.MfaDtos.MfaSetupResponse;
import com.company.ops.api.modules.system.dto.MfaDtos.MfaStatusResponse;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.TotpService;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MfaService {
  private static final int RECOVERY_CODE_COUNT = 10;
  private final SystemUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TotpService totpService;
  private final SecureRandom random = new SecureRandom();

  public MfaService(SystemUserRepository userRepository, PasswordEncoder passwordEncoder, TotpService totpService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.totpService = totpService;
  }

  @Transactional(readOnly = true)
  public MfaStatusResponse status(UUID userId) {
    SystemUser user = requireUser(userId);
    return new MfaStatusResponse(user.isMfaEnabled(), recoveryHashes(user).size());
  }

  @Transactional
  public MfaSetupResponse beginSetup(UUID userId, String currentPassword) {
    SystemUser user = requireUser(userId);
    verifyPassword(user, currentPassword);
    if (user.isMfaEnabled()) throw new BusinessException("多因素认证已启用");
    String secret = totpService.generateSecret();
    user.setMfaSecret(secret);
    user.setMfaRecoveryCodes(null);
    userRepository.save(user);
    return new MfaSetupResponse(secret, totpService.provisioningUri(user.getUsername(), secret));
  }

  @Transactional
  public ConfirmMfaResponse enable(UUID userId, String code) {
    SystemUser user = requireUser(userId);
    if (user.isMfaEnabled()) throw new BusinessException("多因素认证已启用");
    if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
      throw new BusinessException("请先开始多因素认证设置");
    }
    if (!totpService.verify(user.getMfaSecret(), code)) throw new BusinessException("动态验证码不正确");
    List<String> recoveryCodes = generateRecoveryCodes();
    user.setMfaRecoveryCodes(recoveryCodes.stream()
        .map(this::normalizeRecoveryCode)
        .map(passwordEncoder::encode)
        .reduce((left, right) -> left + "\n" + right)
        .orElse(""));
    user.setMfaEnabled(true);
    user.bumpAuthVersion();
    userRepository.save(user);
    return new ConfirmMfaResponse(recoveryCodes);
  }

  @Transactional
  public void disable(UUID userId, String currentPassword, String code) {
    SystemUser user = requireUser(userId);
    verifyPassword(user, currentPassword);
    requireEnabled(user);
    if (!verifyAndConsume(user, code)) throw new BusinessException("动态验证码或恢复码不正确");
    user.setMfaEnabled(false);
    user.setMfaSecret(null);
    user.setMfaRecoveryCodes(null);
    user.bumpAuthVersion();
    userRepository.save(user);
  }

  @Transactional
  public ConfirmMfaResponse regenerateRecoveryCodes(UUID userId, String currentPassword, String code) {
    SystemUser user = requireUser(userId);
    verifyPassword(user, currentPassword);
    requireEnabled(user);
    if (!verifyAndConsume(user, code)) throw new BusinessException("动态验证码或恢复码不正确");
    List<String> recoveryCodes = generateRecoveryCodes();
    user.setMfaRecoveryCodes(recoveryCodes.stream()
        .map(this::normalizeRecoveryCode)
        .map(passwordEncoder::encode)
        .reduce((left, right) -> left + "\n" + right)
        .orElse(""));
    userRepository.save(user);
    return new ConfirmMfaResponse(recoveryCodes);
  }

  @Transactional
  public boolean verifyLoginCode(UUID userId, String code) {
    SystemUser user = requireUser(userId);
    return !user.isMfaEnabled() || verifyAndConsume(user, code);
  }

  private boolean verifyAndConsume(SystemUser user, String code) {
    if (totpService.verify(user.getMfaSecret(), code)) return true;
    String normalized = normalizeRecoveryCode(code);
    List<String> hashes = new ArrayList<>(recoveryHashes(user));
    for (int index = 0; index < hashes.size(); index++) {
      if (passwordEncoder.matches(normalized, hashes.get(index))) {
        hashes.remove(index);
        user.setMfaRecoveryCodes(String.join("\n", hashes));
        userRepository.save(user);
        return true;
      }
    }
    return false;
  }

  private List<String> generateRecoveryCodes() {
    List<String> result = new ArrayList<>(RECOVERY_CODE_COUNT);
    for (int index = 0; index < RECOVERY_CODE_COUNT; index++) {
      byte[] bytes = new byte[6];
      random.nextBytes(bytes);
      String value = HexFormat.of().withUpperCase().formatHex(bytes);
      result.add(value.substring(0, 4) + "-" + value.substring(4, 8) + "-" + value.substring(8));
    }
    return List.copyOf(result);
  }

  private List<String> recoveryHashes(SystemUser user) {
    if (user.getMfaRecoveryCodes() == null || user.getMfaRecoveryCodes().isBlank()) return List.of();
    return user.getMfaRecoveryCodes().lines().filter(value -> !value.isBlank()).toList();
  }

  private String normalizeRecoveryCode(String code) {
    return code == null ? "" : code.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
  }

  private void verifyPassword(SystemUser user, String password) {
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BusinessException("当前密码不正确");
    }
  }

  private void requireEnabled(SystemUser user) {
    if (!user.isMfaEnabled()) throw new BusinessException("多因素认证尚未启用");
  }

  private SystemUser requireUser(UUID userId) {
    return userRepository.findById(userId).orElseThrow(() -> new BusinessException("账号不存在"));
  }
}
