package com.company.ops.api.modules.system.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.ops.api.modules.system.domain.SystemUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  private static final String SECRET = "01234567890123456789012345678901";

  @Test
  void tokenBecomesInvalidAfterAuthVersionChanges() {
    var service = new JwtService(SECRET, 30);
    SystemUser user = user(true);
    String token = service.createToken(new UserPrincipal(user));

    assertThat(service.isValid(token, new UserPrincipal(user))).isTrue();

    user.bumpAuthVersion();
    assertThat(service.isValid(token, new UserPrincipal(user))).isFalse();
  }

  @Test
  void tokenIsInvalidForDisabledUser() {
    var service = new JwtService(SECRET, 30);
    SystemUser user = user(true);
    String token = service.createToken(new UserPrincipal(user));

    user.setEnabled(false);

    assertThat(service.isValid(token, new UserPrincipal(user))).isFalse();
  }

  private SystemUser user(boolean enabled) {
    SystemUser user = new SystemUser();
    user.setId(UUID.randomUUID());
    user.setTenantId("default");
    user.setUsername("admin");
    user.setDisplayName("Administrator");
    user.setPasswordHash("encoded-password");
    user.setEnabled(enabled);
    return user;
  }
}
