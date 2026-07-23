package com.company.ops.api.common;

import com.company.ops.api.modules.system.domain.SystemPermission;
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

  @Autowired
  public DataInitializer(
      SystemPermissionRepository permissionRepository,
      SystemRoleRepository roleRepository
  ) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
  }

  public DataInitializer(
      SystemUserRepository userRepository,
      SystemRoleRepository roleRepository,
      SystemPermissionRepository permissionRepository,
      SystemOrganizationRepository organizationRepository,
      ApprovalAssigneeConfigRepository approvalAssigneeConfigRepository,
      PasswordEncoder passwordEncoder
  ) {
    this(permissionRepository, roleRepository);
  }

  @Override
  @Transactional
  public void run(String... args) {
    ensureRequiredAdminPermissions();
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
