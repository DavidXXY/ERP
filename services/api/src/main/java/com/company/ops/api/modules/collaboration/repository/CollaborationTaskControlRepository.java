package com.company.ops.api.modules.collaboration.repository;
import com.company.ops.api.modules.collaboration.domain.CollaborationTaskControl;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface CollaborationTaskControlRepository extends JpaRepository<CollaborationTaskControl,UUID>{
  Optional<CollaborationTaskControl> findBySourceTypeAndSourceId(String sourceType,UUID sourceId);
  List<CollaborationTaskControl> findByStatusNotOrderByDueAtAsc(String status);
}
