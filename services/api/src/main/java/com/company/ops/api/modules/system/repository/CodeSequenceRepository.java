package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.CodeSequence;
import com.company.ops.api.modules.system.domain.CodeSequenceId;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, CodeSequenceId> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from CodeSequence c where c.entityType = :entityType and c.tenantId = :tenantId")
  Optional<CodeSequence> findByEntityTypeAndTenantIdForUpdate(
      @Param("entityType") String entityType, @Param("tenantId") String tenantId);
}