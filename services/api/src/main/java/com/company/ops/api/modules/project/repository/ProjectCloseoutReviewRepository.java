package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.ProjectCloseoutReview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCloseoutReviewRepository extends JpaRepository<ProjectCloseoutReview, UUID> {

  List<ProjectCloseoutReview> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

  Optional<ProjectCloseoutReview> findFirstByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
