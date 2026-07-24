package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.version.ApplicationVersion;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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

  @Value("${ops.storage.local-path:}")
  private String localStoragePath;

  @Value("${spring.datasource.url:}")
  private String datasourceUrl;

  @Value("${spring.datasource.driver-class-name:}")
  private String datasourceDriver;

  @Value("${spring.data.redis.host:}")
  private String redisHost;

  @Value("${spring.data.redis.port:}")
  private String redisPort;

  public SystemHealthController(Environment environment, ApplicationVersion applicationVersion) {
    this.environment = environment;
    this.applicationVersion = applicationVersion;
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> getSystemHealth() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("application", getApplicationInfo());
    result.put("dependencies", getDependencyInfo());
    result.put("operatingSystem", getOperatingSystemInfo());
    result.put("cpu", getCpuInfo());
    result.put("memory", getMemoryInfo());
    result.put("jvm", getJvmInfo());
    result.put("disk", getDiskInfo());
    return ApiResponse.ok(result);
  }

  private Map<String, Object> getApplicationInfo() {
    Map<String, Object> app = new LinkedHashMap<>();
    app.put("appName", appName);
    app.put("version", applicationVersion.getDisplayVersion());
    app.put("productVersion", applicationVersion.getProductVersion());
    app.put("commitId", applicationVersion.getCommitId());
    app.put("buildTime", applicationVersion.getBuildTime());
    app.put("activeProfiles", getActiveProfiles());
    app.put("storageType", storageType);
    return app;
  }

  private Map<String, Object> getDependencyInfo() {
    Map<String, Object> deps = new LinkedHashMap<>();
    deps.put("databaseUrl", maskSensitiveUrl(datasourceUrl));
    deps.put("databaseDriver", datasourceDriver);
    deps.put("redisEndpoint", redisHost == null || redisHost.isBlank() ? "" : redisHost + ":" + redisPort);
    deps.put("storageType", storageType);
    deps.put("localStoragePath", localStoragePath);
    deps.put("tempDir", System.getProperty("java.io.tmpdir"));
    deps.put("workingDir", System.getProperty("user.dir"));
    return deps;
  }

  private String getActiveProfiles() {
    String[] profiles = environment.getActiveProfiles();
    if (profiles.length == 0) {
      profiles = environment.getDefaultProfiles();
    }
    return String.join(", ", Arrays.stream(profiles).filter((item) -> !item.isBlank()).toList());
  }

  private String maskSensitiveUrl(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value
        .replaceAll("(?i)(password=)[^;&]+", "$1******")
        .replaceAll("(?i)(pwd=)[^;&]+", "$1******")
        .replaceAll("(?i)(://[^:/?#]+:)[^@/]+(@)", "$1******$2");
  }

  private Map<String, Object> getOperatingSystemInfo() {
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("name", osBean.getName());
    info.put("version", osBean.getVersion());
    info.put("architecture", osBean.getArch());
    info.put("availableProcessors", osBean.getAvailableProcessors());
    info.put("systemLoadAverage", osBean.getSystemLoadAverage());
    try {
      Class<?> clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
      if (clazz.isInstance(osBean)) {
        Object bean = clazz.cast(osBean);
        info.put("processCpuLoad", clazz.getMethod("getProcessCpuLoad").invoke(bean));
        info.put("systemCpuLoad", clazz.getMethod("getCpuLoad").invoke(bean));
      }
    } catch (Exception ignored) {}
    return info;
  }

  private Map<String, Object> getCpuInfo() {
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    Map<String, Object> cpu = new LinkedHashMap<>();
    cpu.put("availableProcessors", osBean.getAvailableProcessors());
    cpu.put("systemLoadAverage", osBean.getSystemLoadAverage());
    double processCpuLoad = -1;
    double systemCpuLoad = -1;
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
    cpu.put("processCpuLoad", processCpuLoad);
    cpu.put("systemCpuLoad", systemCpuLoad);
    return cpu;
  }

  private Map<String, Object> getMemoryInfo() {
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    Map<String, Object> mem = new LinkedHashMap<>();

    // JVM heap
    MemoryUsage heap = memoryBean.getHeapMemoryUsage();
    Map<String, Object> heapMap = new LinkedHashMap<>();
    heapMap.put("init", heap.getInit());
    heapMap.put("used", heap.getUsed());
    heapMap.put("committed", heap.getCommitted());
    heapMap.put("max", heap.getMax());
    mem.put("heap", heapMap);

    // JVM non-heap
    MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
    Map<String, Object> nonHeapMap = new LinkedHashMap<>();
    nonHeapMap.put("init", nonHeap.getInit());
    nonHeapMap.put("used", nonHeap.getUsed());
    nonHeapMap.put("committed", nonHeap.getCommitted());
    nonHeapMap.put("max", nonHeap.getMax());
    mem.put("nonHeap", nonHeapMap);

    // Physical memory via com.sun.management if available
    try {
      Class<?> clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
      if (clazz.isInstance(osBean)) {
        Object bean = clazz.cast(osBean);
        mem.put("totalPhysicalMemory", clazz.getMethod("getTotalMemorySize").invoke(bean));
        mem.put("freePhysicalMemory", clazz.getMethod("getFreeMemorySize").invoke(bean));
      }
    } catch (Exception ignored) {}

    return mem;
  }

  private Map<String, Object> getJvmInfo() {
    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    Map<String, Object> jvm = new LinkedHashMap<>();
    jvm.put("javaVersion", System.getProperty("java.version"));
    jvm.put("javaVendor", System.getProperty("java.vendor"));
    jvm.put("jvmName", runtime.getVmName());
    jvm.put("jvmVersion", runtime.getVmVersion());
    jvm.put("jvmVendor", runtime.getVmVendor());
    jvm.put("uptime", runtime.getUptime());
    jvm.put("inputArguments", runtime.getInputArguments());
    jvm.put("startTime", runtime.getStartTime());
    return jvm;
  }

  private List<Map<String, Object>> getDiskInfo() {
    File[] roots = File.listRoots();
    List<Map<String, Object>> disks = new ArrayList<>();
    for (File root : roots) {
      Map<String, Object> disk = new LinkedHashMap<>();
      disk.put("path", root.getAbsolutePath());
      disk.put("totalSpace", root.getTotalSpace());
      disk.put("freeSpace", root.getFreeSpace());
      disk.put("usableSpace", root.getUsableSpace());
      disks.add(disk);
    }
    return disks;
  }
}
