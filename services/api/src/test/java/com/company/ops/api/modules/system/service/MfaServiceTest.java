package com.company.ops.api.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.TotpService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {
  @Mock private SystemUserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private TotpService totpService;
  private MfaService service;
  private UUID userId;
  private SystemUser user;

  @BeforeEach
  void setUp() {
    service = new MfaService(userRepository, passwordEncoder, totpService);
    userId = UUID.randomUUID();
    user = new SystemUser();
    user.setId(userId);
    user.setUsername("employee");
    user.setPasswordHash("encoded-password");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
  }

  @Test
  void setupAndEnableGenerateHashedRecoveryCodes() {
    when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);
    when(totpService.generateSecret()).thenReturn("BASE32SECRET");
    when(totpService.provisioningUri("employee", "BASE32SECRET")).thenReturn("otpauth://setup");
    when(totpService.verify("BASE32SECRET", "123456")).thenReturn(true);
    when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));

    var setup = service.beginSetup(userId, "current-password");
    var enabled = service.enable(userId, "123456");

    assertThat(setup.secret()).isEqualTo("BASE32SECRET");
    assertThat(setup.otpauthUri()).isEqualTo("otpauth://setup");
    assertThat(enabled.recoveryCodes()).hasSize(10).allMatch(code -> code.matches("[0-9A-F]{4}(-[0-9A-F]{4}){2}"));
    assertThat(user.isMfaEnabled()).isTrue();
    assertThat(user.getAuthVersion()).isEqualTo(1);
    assertThat(user.getMfaRecoveryCodes().lines()).hasSize(10).allMatch(hash -> hash.startsWith("hash:"));
    verify(userRepository, times(2)).save(user);
  }

  @Test
  void recoveryCodeIsConsumedAfterSuccessfulLogin() {
    user.setMfaEnabled(true);
    user.setMfaSecret("BASE32SECRET");
    user.setMfaRecoveryCodes("hash-one\nhash-two");
    when(totpService.verify("BASE32SECRET", "abcd-ef01-2345")).thenReturn(false);
    when(passwordEncoder.matches("ABCDEF012345", "hash-one")).thenReturn(true);

    assertThat(service.verifyLoginCode(userId, "abcd-ef01-2345")).isTrue();
    assertThat(user.getMfaRecoveryCodes()).isEqualTo("hash-two");
  }

  @Test
  void disableRequiresCurrentPasswordBeforeChangingMfaState() {
    user.setMfaEnabled(true);
    user.setMfaSecret("BASE32SECRET");
    when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

    assertThatThrownBy(() -> service.disable(userId, "wrong", "123456"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前密码不正确");
    assertThat(user.isMfaEnabled()).isTrue();
  }
}
