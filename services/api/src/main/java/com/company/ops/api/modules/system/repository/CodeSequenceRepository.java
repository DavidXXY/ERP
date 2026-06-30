package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.CodeSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from CodeSequence c where c.entityType = :entityType")
  Optional<CodeSequence> findByEntityTypeForUpdate(@Param("entityType") String entityType);
}
