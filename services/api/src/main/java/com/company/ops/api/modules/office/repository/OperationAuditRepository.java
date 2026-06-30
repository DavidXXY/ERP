package com.company.ops.api.modules.office.repository;
import com.company.ops.api.modules.office.domain.OperationAudit; import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface OperationAuditRepository extends JpaRepository<OperationAudit, UUID> { List<OperationAudit> findTop200ByOrderByCreatedAtDesc(); }
