package com.company.ops.api.modules.office.repository;
import com.company.ops.api.modules.office.domain.DocumentFile; import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface DocumentFileRepository extends JpaRepository<DocumentFile, UUID> { List<DocumentFile> findAllByOrderByCreatedAtDesc(); List<DocumentFile> findByBizTypeAndBizIdOrderByCreatedAtDesc(String bizType, UUID bizId); }
