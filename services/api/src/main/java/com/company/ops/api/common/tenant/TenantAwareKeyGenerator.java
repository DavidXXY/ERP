package com.company.ops.api.common.tenant;

import java.lang.reflect.Method;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

/**
 * 在缓存键中强制加入当前租户，避免多租户之间共享同一个缓存条目。
 * 显式指定 {@code key = "..."} 的注解不经过本生成器，需在这些注解中自行加入租户。
 */
public final class TenantAwareKeyGenerator implements KeyGenerator {

  @Override
  public Object generate(Object target, Method method, Object... params) {
    Object naturalKey = SimpleKeyGenerator.generateKey(params);
    return new SimpleKey(TenantContext.currentTenant(), naturalKey);
  }
}
