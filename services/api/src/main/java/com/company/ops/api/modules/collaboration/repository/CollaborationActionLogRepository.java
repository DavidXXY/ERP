package com.company.ops.api.modules.collaboration.repository;
import com.company.ops.api.modules.collaboration.domain.CollaborationActionLog;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface CollaborationActionLogRepository extends JpaRepository<CollaborationActionLog,UUID>{
  List<CollaborationActionLog> findTop100ByOrderByCreatedAtDesc();
  List<CollaborationActionLog> findBySourceTypeAndSourceIdOrderByCreatedAtDesc(String sourceType,UUID sourceId);
}
