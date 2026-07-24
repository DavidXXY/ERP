package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.DocumentFile;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, UUID> {
  List<DocumentFile> findAllByOrderByCreatedAtDesc();
  Page<DocumentFile> findAllByOrderByCreatedAtDesc(Pageable pageable);

  List<DocumentFile> findByBizTypeAndBizIdOrderByCreatedAtDesc(String bizType, UUID bizId);
  Page<DocumentFile> findByBizTypeAndBizIdOrderByCreatedAtDesc(String bizType, UUID bizId, Pageable pageable);

  Page<DocumentFile> findByBizTypeOrderByCreatedAtDesc(String bizType, Pageable pageable);

  List<DocumentFile> findByBizTypeOrderByCreatedAtDesc(String bizType);

  long countByBizTypeAndBizId(String bizType, UUID bizId);

  @Query("select d.bizId, count(d) from DocumentFile d where d.bizType = :bizType and d.bizId in :bizIds group by d.bizId")
  List<Object[]> countByBizTypeAndBizIdIn(
      @Param("bizType") String bizType,
      @Param("bizIds") Collection<UUID> bizIds
  );

  void deleteByBizTypeAndBizId(String bizType, UUID bizId);
}
