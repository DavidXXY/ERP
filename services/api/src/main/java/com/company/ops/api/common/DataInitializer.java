package com.company.ops.api.common;

import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.repository.ApprovalAssigneeConfigRepository;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemPermissionRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
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
  private static final String PROJECT_DELETE = "project:delete";

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
    ensureProjectDeletePermission();
  }

  private void ensureProjectDeletePermission() {
    SystemPermission permission = permissionRepository.findAll().stream()
        .filter(item -> PROJECT_DELETE.equals(item.getCode()) && TENANT_ID.equals(item.getTenantId()))
        .findFirst()
        .orElseGet(() -> {
          SystemPermission created = new SystemPermission();
          created.setCode(PROJECT_DELETE);
          created.setName("Project delete");
          created.setModule("project");
          created.setBuiltIn(true);
          return permissionRepository.save(created);
        });

    roleRepository.findByCodeAndTenantIdWithPermissions("ADMIN", TENANT_ID).ifPresent(admin -> {
      if (admin.getPermissions().stream().noneMatch(item -> PROJECT_DELETE.equals(item.getCode()))) {
        admin.getPermissions().add(permission);
        roleRepository.save(admin);
        log.info("Granted project delete permission to ADMIN role");
      }
    });
  }
}
