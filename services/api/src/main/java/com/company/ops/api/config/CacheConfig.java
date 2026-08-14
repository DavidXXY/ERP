package com.company.ops.api.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * 热点只读数据缓存。生产默认使用 Redis；测试通过 ops.cache.enabled=false 关闭，
 * 或通过 ops.cache.type=local 使用进程内缓存，避免依赖外部 Redis。
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "ops.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

  private static final Map<String, Duration> TTL_BY_CACHE = Map.of(
      "biDashboard", Duration.ofSeconds(60),
      "biCompanyDashboard", Duration.ofSeconds(60),
      "financeAnalytics", Duration.ofSeconds(60),
      "financeOverview", Duration.ofSeconds(60),
      "governanceOverview", Duration.ofSeconds(60),
      "ledgerOverview", Duration.ofSeconds(60),
      "financeOperationsOverview", Duration.ofSeconds(60),
      "riskSummary", Duration.ofSeconds(60));

  @Bean
  @ConditionalOnProperty(name = "ops.cache.type", havingValue = "redis", matchIfMissing = true)
  public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(),
        ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);

    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
    RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
        .entryTtl(Duration.ofMinutes(5))
        .disableCachingNullValues();

    var builder = RedisCacheManager.builder(connectionFactory).cacheDefaults(defaults);
    TTL_BY_CACHE.forEach((name, ttl) ->
        builder.withCacheConfiguration(name, defaults.entryTtl(ttl)));
    return builder.build();
  }

  @Bean
  @ConditionalOnProperty(name = "ops.cache.type", havingValue = "local")
  public CacheManager localCacheManager() {
    return new ConcurrentMapCacheManager(TTL_BY_CACHE.keySet().toArray(new String[0]));
  }
}
