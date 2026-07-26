package com.company.ops.api.modules.system.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.company.ops.api.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import java.time.ZoneId;

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

  @Test
  void localFallbackExpiresAndRemainsBoundedForRandomUsernames() {
    MutableClock clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"));
    var service = new LoginAttemptService(5, Duration.ofMinutes(15), clock, 2);

    service.failed("random-1|127.0.0.1");
    service.failed("random-2|127.0.0.1");
    service.failed("random-3|127.0.0.1");
    assertThat(service.localEntryCount()).isLessThanOrEqualTo(2);

    clock.advance(Duration.ofMinutes(16));
    service.assertAllowed("random-3|127.0.0.1");
    service.failed("fresh|127.0.0.1");
    assertThat(service.localEntryCount()).isEqualTo(1);
  }

  private static final class MutableClock extends Clock {
    private Instant instant;
    private MutableClock(Instant instant) { this.instant = instant; }
    private void advance(Duration duration) { instant = instant.plus(duration); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return instant; }
  }
}
