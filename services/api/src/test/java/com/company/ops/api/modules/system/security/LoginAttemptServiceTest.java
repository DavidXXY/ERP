package com.company.ops.api.modules.system.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ops.api.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {
  @Test
  void locksAfterConfiguredFailuresAndClearsAfterSuccess() {
    var service = new LoginAttemptService(
        2,
        Duration.ofMinutes(15),
        Clock.fixed(Instant.parse("2026-07-18T00:00:00Z"), ZoneOffset.UTC)
    );

    service.failed("admin|127.0.0.1");
    service.assertAllowed("admin|127.0.0.1");
    service.failed("admin|127.0.0.1");

    assertThatThrownBy(() -> service.assertAllowed("admin|127.0.0.1"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("登录失败次数过多");

    service.succeeded("admin|127.0.0.1");
    service.assertAllowed("admin|127.0.0.1");
  }
}
