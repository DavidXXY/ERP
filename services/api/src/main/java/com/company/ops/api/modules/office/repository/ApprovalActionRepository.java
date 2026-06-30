package com.company.ops.api.modules.office.repository;
import com.company.ops.api.modules.office.domain.ApprovalAction; import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, UUID> { List<ApprovalAction> findByApprovalIdOrderByCreatedAtAsc(UUID approvalId); }
