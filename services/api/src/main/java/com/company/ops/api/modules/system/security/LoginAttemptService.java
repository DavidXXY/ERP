package com.company.ops.api.modules.system.security;

import com.company.ops.api.common.exception.RateLimitException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LoginAttemptService {
  private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
  private static final int DEFAULT_MAX_LOCAL_ENTRIES = 10_000;
  private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
  private final AtomicLong operations = new AtomicLong();
  private final int maxAttempts;
  private final int maxLocalEntries;
  private final Duration lockDuration;
  private final Clock clock;
  private final StringRedisTemplate redis;
  private volatile Instant redisRetryAfter = Instant.MIN;

  @Autowired
  public LoginAttemptService(
      @Value("${ops.security.login-max-attempts:5}") int maxAttempts,
      @Value("${ops.security.login-lock-minutes:15}") long lockMinutes,
      ObjectProvider<StringRedisTemplate> redisProvider
  ) {
    this(maxAttempts, Duration.ofMinutes(lockMinutes), Clock.systemUTC(),
        DEFAULT_MAX_LOCAL_ENTRIES, redisProvider.getIfAvailable());
  }

  LoginAttemptService(int maxAttempts, Duration lockDuration, Clock clock) {
    this(maxAttempts, lockDuration, clock, DEFAULT_MAX_LOCAL_ENTRIES, null);
  }

  LoginAttemptService(int maxAttempts, Duration lockDuration, Clock clock, int maxLocalEntries) {
    this(maxAttempts, lockDuration, clock, maxLocalEntries, null);
  }

  private LoginAttemptService(int maxAttempts, Duration lockDuration, Clock clock,
      int maxLocalEntries, StringRedisTemplate redis) {
    this.maxAttempts = maxAttempts;
    this.lockDuration = lockDuration;
    this.clock = clock;
    this.maxLocalEntries = maxLocalEntries;
    this.redis = redis;
  }

  public void assertAllowed(String key) {
    assertAllowed(key, "登录失败次数过多，请稍后重试");
  }

  public void assertAllowed(String key, String message) {
    Long distributedCount = redisCount(key);
    if (distributedCount != null && distributedCount >= maxAttempts) {
      throw new RateLimitException(message);
    }
    cleanupLocal();
    Attempt attempt = attempts.get(key);
    if (attempt == null) return;
    Instant now = clock.instant();
    if (attempt.expiresAt().isAfter(now) && attempt.count() >= maxAttempts) {
      throw new RateLimitException(message);
    }
    if (!attempt.expiresAt().isAfter(now)) attempts.remove(key, attempt);
  }

  public void failed(String key) {
    Instant now = clock.instant();
    if (incrementRedis(key) != null) return;
    cleanupLocal();
    ensureCapacity();
    attempts.compute(key, (ignored, current) -> {
      int count = current == null || !current.expiresAt().isAfter(now) ? 1 : current.count() + 1;
      return new Attempt(count, now.plus(lockDuration), now);
    });
  }

  public void succeeded(String key) {
    deleteRedis(key);
    attempts.remove(key);
  }

  int localEntryCount() { return attempts.size(); }

  private Long redisCount(String key) {
    if (!redisAvailable()) return null;
    try {
      String value = redis.opsForValue().get(redisKey(key));
      return value == null ? 0L : Long.parseLong(value);
    } catch (RuntimeException exception) {
      markRedisUnavailable(exception);
      return null;
    }
  }

  private Long incrementRedis(String key) {
    if (!redisAvailable()) return null;
    try {
      String redisKey = redisKey(key);
      Long count = redis.opsForValue().increment(redisKey);
      redis.expire(redisKey, lockDuration);
      return count;
    } catch (RuntimeException exception) {
      markRedisUnavailable(exception);
      return null;
    }
  }

  private void deleteRedis(String key) {
    if (!redisAvailable()) return;
    try {
      redis.delete(redisKey(key));
    } catch (RuntimeException exception) {
      markRedisUnavailable(exception);
    }
  }

  private boolean redisAvailable() {
    return redis != null && !redisRetryAfter.isAfter(clock.instant());
  }

  private void markRedisUnavailable(RuntimeException exception) {
    redisRetryAfter = clock.instant().plusSeconds(30);
    log.warn("Redis login rate limiter unavailable; using bounded local fallback: {}", exception.getMessage());
  }

  private void cleanupLocal() {
    if (operations.incrementAndGet() % 64 != 0 && attempts.size() < maxLocalEntries) return;
    Instant now = clock.instant();
    attempts.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
  }

  private void ensureCapacity() {
    if (attempts.size() < maxLocalEntries) return;
    attempts.entrySet().stream()
        .min(java.util.Comparator.comparing(entry -> entry.getValue().updatedAt()))
        .ifPresent(entry -> attempts.remove(entry.getKey(), entry.getValue()));
  }

  private String redisKey(String key) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
      return "ops:login-attempt:" + HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private record Attempt(int count, Instant expiresAt, Instant updatedAt) {}
}
