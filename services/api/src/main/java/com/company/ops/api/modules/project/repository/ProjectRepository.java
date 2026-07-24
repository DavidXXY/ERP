package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectStage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

  Page<Project> findAllByOrderByCreatedAtDesc(Pageable pageable);

  List<Project> findAllByOrderByCreatedAtDesc();

  boolean existsByCode(String code);
  Optional<Project> findByCode(String code);
  List<Project> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code,String name,Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Project p where p.id = :id")
  Optional<Project> findByIdForUpdate(@Param("id") UUID id);
  boolean existsByContractId(UUID contractId);
  @Query(value = "select * from project_projects where contract_id = :contractId order by created_at desc limit 1", nativeQuery = true)
  Optional<Project> findLatestByContractId(@Param("contractId") UUID contractId);
  List<Project> findByContractIdIn(java.util.Collection<UUID> contractIds);
  List<Project> findByPlannedStartDateBetween(LocalDate startDate,LocalDate endDate);

  int countByCodeStartingWith(String prefix);

  @Query("select coalesce(sum(p.actualCost), 0) from Project p")
  BigDecimal sumActualCost();

  long countByStageNot(ProjectStage stage);

}
