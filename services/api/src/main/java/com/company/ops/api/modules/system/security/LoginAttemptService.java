package com.company.ops.api.modules.system.security;

import com.company.ops.api.common.exception.RateLimitException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class LoginAttemptService {
  private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
  private final int maxAttempts;
  private final Duration lockDuration;
  private final Clock clock;

  @Autowired
  public LoginAttemptService(
      @Value("${ops.security.login-max-attempts:5}") int maxAttempts,
      @Value("${ops.security.login-lock-minutes:15}") long lockMinutes
  ) {
    this(maxAttempts, Duration.ofMinutes(lockMinutes), Clock.systemUTC());
  }

  LoginAttemptService(int maxAttempts, Duration lockDuration, Clock clock) {
    this.maxAttempts = maxAttempts;
    this.lockDuration = lockDuration;
    this.clock = clock;
  }

  public void assertAllowed(String key) {
    Attempt attempt = attempts.get(key);
    if (attempt == null) return;
    Instant now = clock.instant();
    if (attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(now)) {
      throw new RateLimitException("登录失败次数过多，请稍后重试");
    }
    if (attempt.lockedUntil() != null) attempts.remove(key, attempt);
  }

  public void failed(String key) {
    Instant now = clock.instant();
    attempts.compute(key, (ignored, current) -> {
      int count = current == null ? 1 : current.count() + 1;
      return new Attempt(count, count >= maxAttempts ? now.plus(lockDuration) : null);
    });
  }

  public void succeeded(String key) {
    attempts.remove(key);
  }

  private record Attempt(int count, Instant lockedUntil) {}
}
