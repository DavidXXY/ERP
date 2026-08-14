package com.company.ops.api.modules.system.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 系统健康检查响应 DTO。
 * 由 SystemHealthController 从 Map&lt;String,Object&gt; 收敛为强类型。
 * 可选字段通过 @JsonInclude(NON_NULL) 在为空时省略，保持与原 JSON 契约一致。
 */
public final class SystemHealthDtos {

  private SystemHealthDtos() {}

  public record HealthResponse(
      ApplicationInfo application,
      DependencyInfo dependencies,
      OperatingSystemInfo operatingSystem,
      CpuInfo cpu,
      MemoryInfo memory,
      JvmInfo jvm,
      List<DiskInfo> disk) {}

  public record ApplicationInfo(
      String appName,
      String version,
      String productVersion,
      String commitId,
      String buildTime,
      String activeProfiles,
      String storageType) {}

  public record DependencyInfo(
      String databaseDriver,
      String storageType) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record OperatingSystemInfo(
      String name,
      String version,
      String architecture,
      int availableProcessors,
      double systemLoadAverage,
      Double processCpuLoad,
      Double systemCpuLoad) {}

  public record CpuInfo(
      int availableProcessors,
      double systemLoadAverage,
      double processCpuLoad,
      double systemCpuLoad) {}

  public record MemoryUsageInfo(
      long init,
      long used,
      long committed,
      long max) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record MemoryInfo(
      MemoryUsageInfo heap,
      MemoryUsageInfo nonHeap,
      Long totalPhysicalMemory,
      Long freePhysicalMemory) {}

  public record JvmInfo(
      String javaVersion,
      String javaVendor,
      String jvmName,
      String jvmVersion,
      String jvmVendor,
      long uptime,
      long startTime) {}

  public record DiskInfo(
      String name,
      long totalSpace,
      long freeSpace,
      long usableSpace) {}
}
