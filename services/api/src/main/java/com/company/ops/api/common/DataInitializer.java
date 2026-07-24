package com.company.ops.api.common;

import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.ApprovalAssigneeConfigRepository;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemPermissionRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
  private static final String TENANT_ID = "default";
  private static final List<RequiredPermission> REQUIRED_ADMIN_PERMISSIONS = List.of(
      new RequiredPermission("project:delete", "项目删除", "project"),
      new RequiredPermission("project:stage:update", "项目阶段推进", "project"),
      new RequiredPermission("project:cost:create", "项目成本归集", "project"),
      new RequiredPermission("crm:quote:cost", "报价成本询价", "crm"),
      new RequiredPermission("maintenance:order:delete", "工单删除", "maintenance")
  );

  private final SystemPermissionRepository permissionRepository;
  private final SystemRoleRepository roleRepository;
  private final SystemUserRepository userRepository;
  private final SystemOrganizationRepository organizationRepository;
  private final PasswordEncoder passwordEncoder;
  private final String bootstrapUsername;
  private final String bootstrapPassword;
  private final String bootstrapDisplayName;

  @Autowired
  public DataInitializer(
      SystemUserRepository userRepository,
      SystemRoleRepository roleRepository,
      SystemPermissionRepository permissionRepository,
      SystemOrganizationRepository organizationRepository,
      PasswordEncoder passwordEncoder,
      @Value("${ops.bootstrap-admin.username:admin}") String bootstrapUsername,
      @Value("${ops.bootstrap-admin.password:}") String bootstrapPassword,
      @Value("${ops.bootstrap-admin.display-name:系统管理员}") String bootstrapDisplayName
  ) {
    this.userRepository = userRepository;
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
    this.organizationRepository = organizationRepository;
    this.passwordEncoder = passwordEncoder;
    this.bootstrapUsername = bootstrapUsername;
    this.bootstrapPassword = bootstrapPassword;
    this.bootstrapDisplayName = bootstrapDisplayName;
  }

  public DataInitializer(
      SystemUserRepository userRepository,
      SystemRoleRepository roleRepository,
      SystemPermissionRepository permissionRepository,
      SystemOrganizationRepository organizationRepository,
      ApprovalAssigneeConfigRepository approvalAssigneeConfigRepository,
      PasswordEncoder passwordEncoder
  ) {
    this(
        userRepository,
        roleRepository,
        permissionRepository,
        organizationRepository,
        passwordEncoder,
        "admin",
        "",
        "系统管理员"
    );
  }

  @Override
  @Transactional
  public void run(String... args) {
    ensureRequiredAdminPermissions();
    ensureBootstrapAdmin();
  }

  private void ensureBootstrapAdmin() {
    if (bootstrapPassword == null || bootstrapPassword.isBlank()) {
      if (userRepository.count() == 0) {
        log.warn("No system user exists. Set BOOTSTRAP_ADMIN_PASSWORD for the first deployment");
      }
      return;
    }
    if (bootstrapPassword.length() < 12) {
      throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters");
    }

    String username = bootstrapUsername == null || bootstrapUsername.isBlank()
        ? "admin"
        : bootstrapUsername.trim();
    if (userRepository.existsByUsername(username)) {
      log.info("Bootstrap administrator already exists; credentials were not changed");
      return;
    }

    SystemRole adminRole = roleRepository.findByCodeAndTenantIdWithPermissions("ADMIN", TENANT_ID)
        .orElseThrow(() -> new IllegalStateException("ADMIN role is missing from the database baseline"));

    SystemUser administrator = new SystemUser();
    administrator.setTenantId(TENANT_ID);
    administrator.setUsername(username);
    administrator.setDisplayName(
        bootstrapDisplayName == null || bootstrapDisplayName.isBlank()
            ? "系统管理员"
            : bootstrapDisplayName.trim()
    );
    administrator.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
    administrator.setEnabled(true);
    administrator.setOrganization(organizationRepository.findByCodeAndTenantId("ROOT", TENANT_ID));
    administrator.getRoles().add(adminRole);
    userRepository.save(administrator);
    log.info("Created bootstrap administrator account: {}", username);
  }

  private void ensureRequiredAdminPermissions() {
    Map<String, SystemPermission> available = new LinkedHashMap<>();
    permissionRepository.findAll().stream()
        .filter(item -> TENANT_ID.equals(item.getTenantId()))
        .forEach(item -> available.put(item.getCode(), item));

    for (RequiredPermission required : REQUIRED_ADMIN_PERMISSIONS) {
      if (!permissionRepository.existsByCodeAndTenantId(required.code(), TENANT_ID)) {
          SystemPermission created = new SystemPermission();
          created.setCode(required.code());
          created.setName(required.name());
          created.setModule(required.module());
          created.setBuiltIn(true);
          SystemPermission saved = permissionRepository.save(created);
          available.put(required.code(), saved == null ? created : saved);
      }
    }

    roleRepository.findByCodeAndTenantIdWithPermissions("ADMIN", TENANT_ID).ifPresent(admin -> {
      boolean changed = false;
      for (RequiredPermission required : REQUIRED_ADMIN_PERMISSIONS) {
        SystemPermission permission = available.get(required.code());
        if (permission != null && admin.getPermissions().stream()
            .noneMatch(item -> required.code().equals(item.getCode()))) {
          admin.getPermissions().add(permission);
          changed = true;
        }
      }
      if (changed) {
        roleRepository.save(admin);
        log.info("Granted missing required permissions to ADMIN role");
      }
    });
  }

  private record RequiredPermission(String code, String name, String module) {}
}
