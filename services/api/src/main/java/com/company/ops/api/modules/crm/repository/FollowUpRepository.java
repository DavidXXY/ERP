package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.FollowUp;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpRepository extends JpaRepository<FollowUp, UUID> {

  List<FollowUp> findAllByOrderByFollowedAtDesc();
}
