package com.company.ops.api.common.tenant;

public final class TenantContext {
  public static final String DEFAULT_TENANT = "default";
  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private TenantContext() {}

  public static String currentTenant() {
    String tenant = CURRENT.get();
    return tenant == null ? DEFAULT_TENANT : tenant;
  }

  public static Scope use(String tenantId) {
    String normalized = normalize(tenantId);
    String previous = CURRENT.get();
    CURRENT.set(normalized);
    return () -> {
      if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    };
  }

  private static String normalize(String tenantId) {
    if (tenantId == null || !tenantId.matches("[A-Za-z0-9_-]{1,64}")) {
      throw new IllegalArgumentException("非法租户标识");
    }
    return tenantId;
  }

  public interface Scope extends AutoCloseable {
    @Override void close();
  }
}
