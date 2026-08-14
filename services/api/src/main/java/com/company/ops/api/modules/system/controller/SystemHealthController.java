package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.version.ApplicationVersion;
import com.company.ops.api.modules.system.dto.SystemHealthDtos.ApplicationInfo;
import com.company.ops.api.modules.system.dto.SystemHealthDtos.CpuInfo;
import com.company.ops.api.modules.system.dto.SystemHealthDtos.DependencyInfo;
import com.company.ops.api.modules.system.dto.SystemHealthDtos.DiskInfo;
import com.company.ops.api.modules.system.dto.SystemHealthDtos.HealthResponse;
import com.company.ops.api.modules.system.dto.SystemHealthDtos.JvmInfo;
import com.company.ops.api.modules.system.dto.SystemHealthDtos.MemoryInfo;
import com.company.ops.api.modules.system.dto.SystemHealthDtos.MemoryUsageInfo;
import com.company.ops.api.modules.system.dto.SystemHealthDtos.OperatingSystemInfo;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/health")
public class SystemHealthController {

  private final Environment environment;
  private final ApplicationVersion applicationVersion;

  @Value("${spring.application.name:ops-erp-api}")
  private String appName;

  @Value("${ops.storage.type:local}")
  private String storageType;

  @Value("${spring.datasource.driver-class-name:}")
  private String datasourceDriver;

  public SystemHealthController(Environment environment, ApplicationVersion applicationVersion) {
    this.environment = environment;
    this.applicationVersion = applicationVersion;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('system:health:view')")
  public ApiResponse<HealthResponse> getSystemHealth() {
    return ApiResponse.ok(new HealthResponse(
        getApplicationInfo(),
        getDependencyInfo(),
        getOperatingSystemInfo(),
        getCpuInfo(),
        getMemoryInfo(),
        getJvmInfo(),
        getDiskInfo()));
  }

  private ApplicationInfo getApplicationInfo() {
    return new ApplicationInfo(
        appName,
        applicationVersion.getDisplayVersion(),
        applicationVersion.getProductVersion(),
        applicationVersion.getCommitId(),
        applicationVersion.getBuildTime(),
        getActiveProfiles(),
        storageType);
  }

  private DependencyInfo getDependencyInfo() {
    return new DependencyInfo(datasourceDriver, storageType);
  }

  private String getActiveProfiles() {
    String[] profiles = environment.getActiveProfiles();
    if (profiles.length == 0) {
      profiles = environment.getDefaultProfiles();
    }
    return String.join(", ", Arrays.stream(profiles).filter((item) -> !item.isBlank()).toList());
  }

  private OperatingSystemInfo getOperatingSystemInfo() {
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    Double[] loads = optionalCpuLoads(osBean);
    return new OperatingSystemInfo(
        osBean.getName(),
        osBean.getVersion(),
        osBean.getArch(),
        osBean.getAvailableProcessors(),
        osBean.getSystemLoadAverage(),
        loads[0],
        loads[1]);
  }

  private CpuInfo getCpuInfo() {
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    Double[] loads = optionalCpuLoads(osBean);
    return new CpuInfo(
        osBean.getAvailableProcessors(),
        osBean.getSystemLoadAverage(),
        loads[0] == null ? -1 : loads[0],
        loads[1] == null ? -1 : loads[1]);
  }

  private MemoryInfo getMemoryInfo() {
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    MemoryUsage heap = memoryBean.getHeapMemoryUsage();
    MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
    Long[] physical = optionalPhysicalMemory();
    return new MemoryInfo(
        new MemoryUsageInfo(heap.getInit(), heap.getUsed(), heap.getCommitted(), heap.getMax()),
        new MemoryUsageInfo(nonHeap.getInit(), nonHeap.getUsed(), nonHeap.getCommitted(), nonHeap.getMax()),
        physical[0],
        physical[1]);
  }

  private JvmInfo getJvmInfo() {
    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    return new JvmInfo(
        System.getProperty("java.version"),
        System.getProperty("java.vendor"),
        runtime.getVmName(),
        runtime.getVmVersion(),
        runtime.getVmVendor(),
        runtime.getUptime(),
        runtime.getStartTime());
  }

  private List<DiskInfo> getDiskInfo() {
    File[] roots = File.listRoots();
    List<DiskInfo> disks = new ArrayList<>();
    for (File root : roots) {
      disks.add(new DiskInfo(
          "disk-" + (disks.size() + 1),
          root.getTotalSpace(),
          root.getFreeSpace(),
          root.getUsableSpace()));
    }
    return disks;
  }

  private Double[] optionalCpuLoads(OperatingSystemMXBean osBean) {
    Double processCpuLoad = null;
    Double systemCpuLoad = null;
    try {
      Class<?> clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
      if (clazz.isInstance(osBean)) {
        Object bean = clazz.cast(osBean);
        Object pcl = clazz.getMethod("getProcessCpuLoad").invoke(bean);
        Object scl = clazz.getMethod("getCpuLoad").invoke(bean);
        if (pcl instanceof Double) processCpuLoad = (Double) pcl;
        if (scl instanceof Double) systemCpuLoad = (Double) scl;
      }
    } catch (Exception ignored) {}
    return new Double[]{processCpuLoad, systemCpuLoad};
  }

  private Long[] optionalPhysicalMemory() {
    Long totalPhysicalMemory = null;
    Long freePhysicalMemory = null;
    try {
      Class<?> clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
      OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
      if (clazz.isInstance(osBean)) {
        Object bean = clazz.cast(osBean);
        Object total = clazz.getMethod("getTotalMemorySize").invoke(bean);
        Object free = clazz.getMethod("getFreeMemorySize").invoke(bean);
        if (total instanceof Long) totalPhysicalMemory = (Long) total;
        if (free instanceof Long) freePhysicalMemory = (Long) free;
      }
    } catch (Exception ignored) {}
    return new Long[]{totalPhysicalMemory, freePhysicalMemory};
  }
}
