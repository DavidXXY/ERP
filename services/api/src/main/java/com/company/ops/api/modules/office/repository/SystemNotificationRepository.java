package com.company.ops.api.modules.office.repository;
import com.company.ops.api.modules.office.domain.SystemNotification; import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, UUID> { List<SystemNotification> findAllByOrderByCreatedAtDesc(); long countByReadFalse(); boolean existsByDedupKey(String dedupKey); }
