package com.company.ops.api.modules.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.version.ApplicationVersion;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SystemHealthControllerTest {

  @Mock private Environment environment;
  @Mock private ApplicationVersion applicationVersion;
  private SystemHealthController controller;

  @BeforeEach
  void setUp() {
    controller = new SystemHealthController(environment, applicationVersion);
    ReflectionTestUtils.setField(controller, "appName", "ops-erp-api");
    ReflectionTestUtils.setField(controller, "storageType", "local");
    ReflectionTestUtils.setField(controller, "datasourceDriver", "org.postgresql.Driver");
  }

  @Test
  void requiresDedicatedHealthPermission() throws Exception {
    Method endpoint = SystemHealthController.class.getMethod("getSystemHealth");
    PreAuthorize authorization = endpoint.getAnnotation(PreAuthorize.class);

    assertThat(authorization).isNotNull();
    assertThat(authorization.value()).isEqualTo("hasAuthority('system:health:view')");
  }

  @Test
  void responseDoesNotExposeConnectionAddressesPathsOrJvmArguments() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
    when(applicationVersion.getDisplayVersion()).thenReturn("1.0.0+12345678");
    when(applicationVersion.getProductVersion()).thenReturn("1.0.0");
    when(applicationVersion.getCommitId()).thenReturn("12345678");
    when(applicationVersion.getBuildTime()).thenReturn("2026-08-04T08:00:00Z");
    String payload = controller.getSystemHealth().data().toString();

    assertThat(payload)
        .contains("application", "dependencies", "operatingSystem", "memory", "disk")
        .doesNotContain("jdbc:", "redis://", "inputArguments", "absolutePath", "user.dir");
  }
}
