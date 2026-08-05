package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.NotificationChannel;
import com.company.ops.api.modules.office.domain.NotificationChannelPreference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationChannelPreferenceRepository extends JpaRepository<NotificationChannelPreference, UUID> {
  Optional<NotificationChannelPreference> findByUserIdAndChannel(UUID userId, NotificationChannel channel);
  List<NotificationChannelPreference> findByChannelAndEnabledTrue(NotificationChannel channel);
}
