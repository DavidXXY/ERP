package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.QualificationAttachmentRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualificationAttachmentRecordRepository extends JpaRepository<QualificationAttachmentRecord, UUID> {
  Optional<QualificationAttachmentRecord> findByObjectKey(String objectKey);
}
