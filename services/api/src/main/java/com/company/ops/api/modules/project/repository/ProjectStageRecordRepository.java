package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.ProjectStageRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectStageRecordRepository extends JpaRepository<ProjectStageRecord, UUID> {

  List<ProjectStageRecord> findByProjectIdOrderByChangedAtDesc(UUID projectId);
}
