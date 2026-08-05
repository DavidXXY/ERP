package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.NotificationChannel;
import com.company.ops.api.modules.office.domain.NotificationDelivery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
  Optional<NotificationDelivery> findByNotificationIdAndUserIdAndChannel(UUID notificationId, UUID userId, NotificationChannel channel);
  List<NotificationDelivery> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
}
