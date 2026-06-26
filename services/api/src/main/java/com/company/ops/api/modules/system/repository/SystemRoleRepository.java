package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemRole;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemRoleRepository extends JpaRepository<SystemRole, UUID> {
}

