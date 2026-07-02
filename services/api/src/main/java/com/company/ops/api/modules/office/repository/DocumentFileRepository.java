package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.DocumentFile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, UUID> {
  List<DocumentFile> findAllByOrderByCreatedAtDesc();
  Page<DocumentFile> findAllByOrderByCreatedAtDesc(Pageable pageable);

  List<DocumentFile> findByBizTypeAndBizIdOrderByCreatedAtDesc(String bizType, UUID bizId);
  Page<DocumentFile> findByBizTypeAndBizIdOrderByCreatedAtDesc(String bizType, UUID bizId, Pageable pageable);

  Page<DocumentFile> findByBizTypeOrderByCreatedAtDesc(String bizType, Pageable pageable);

  List<DocumentFile> findByBizTypeOrderByCreatedAtDesc(String bizType);

  long countByBizTypeAndBizId(String bizType, UUID bizId);

  void deleteByBizTypeAndBizId(String bizType, UUID bizId);
}
