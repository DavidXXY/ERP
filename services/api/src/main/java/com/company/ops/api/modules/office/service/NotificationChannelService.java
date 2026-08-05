package com.company.ops.api.modules.office.service;

import com.company.ops.api.modules.office.domain.NotificationChannel;
import com.company.ops.api.modules.office.domain.NotificationChannelPreference;
import com.company.ops.api.modules.office.domain.NotificationDelivery;
import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.repository.NotificationChannelPreferenceRepository;
import com.company.ops.api.modules.office.repository.NotificationDeliveryRepository;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class NotificationChannelService {
  private static final Logger log = LoggerFactory.getLogger(NotificationChannelService.class);
  private final NotificationChannelPreferenceRepository preferences;
  private final NotificationDeliveryRepository deliveries;
  private final SystemNotificationRepository notifications;
  private final RestClient restClient;
  private final String webhookUrl;

  public NotificationChannelService(NotificationChannelPreferenceRepository preferences,
      NotificationDeliveryRepository deliveries, SystemNotificationRepository notifications,
      RestClient.Builder restClientBuilder,
      @Value("${ops.notifications.webhook-url:}") String webhookUrl) {
    this.preferences = preferences;
    this.deliveries = deliveries;
    this.notifications = notifications;
    this.restClient = restClientBuilder.build();
    this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
  }

  @Transactional(readOnly = true)
  public List<PreferenceResponse> preferences(UUID userId) {
    boolean enabled = preferences.findByUserIdAndChannel(userId, NotificationChannel.WEBHOOK)
        .map(NotificationChannelPreference::isEnabled).orElse(false);
    return List.of(new PreferenceResponse(NotificationChannel.WEBHOOK, enabled, !webhookUrl.isBlank()));
  }

  @Transactional
  public PreferenceResponse updatePreference(UUID userId, NotificationChannel channel, boolean enabled) {
    if (enabled && channel == NotificationChannel.WEBHOOK && webhookUrl.isBlank()) {
      throw new IllegalArgumentException("系统尚未配置通知 Webhook 地址");
    }
    NotificationChannelPreference item = preferences.findByUserIdAndChannel(userId, channel)
        .orElseGet(NotificationChannelPreference::new);
    item.setUserId(userId);
    item.setChannel(channel);
    item.setEnabled(enabled);
    preferences.save(item);
    return new PreferenceResponse(channel, enabled, !webhookUrl.isBlank());
  }

  @Transactional(readOnly = true)
  public List<DeliveryResponse> deliveries(UUID userId) {
    return deliveries.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(item -> new DeliveryResponse(item.getId(), item.getNotificationId(), item.getChannel(), item.getStatus(),
            item.getAttemptCount(), item.getLastAttemptAt(), item.getDeliveredAt(), item.getLastError()))
        .toList();
  }

  @Scheduled(fixedDelayString = "${ops.notifications.dispatch-interval-ms:60000}")
  @SchedulerLock(name = "notificationChannelDispatch", lockAtLeastFor = "PT5S", lockAtMostFor = "PT2M")
  public void dispatchRecent() {
    if (webhookUrl.isBlank()) return;
    List<NotificationChannelPreference> recipients = preferences.findByChannelAndEnabledTrue(NotificationChannel.WEBHOOK);
    if (recipients.isEmpty()) return;
    List<SystemNotification> recent = notifications.findByCreatedAtAfterOrderByCreatedAtAsc(OffsetDateTime.now().minusHours(24));
    for (SystemNotification notification : recent) {
      for (NotificationChannelPreference recipient : recipients) {
        if (notification.getTargetUserId() != null && !notification.getTargetUserId().equals(recipient.getUserId())) continue;
        dispatch(notification, recipient.getUserId());
      }
    }
  }

  @Transactional
  protected void dispatch(SystemNotification notification, UUID userId) {
    NotificationDelivery delivery = deliveries.findByNotificationIdAndUserIdAndChannel(
        notification.getId(), userId, NotificationChannel.WEBHOOK).orElseGet(NotificationDelivery::new);
    if ("DELIVERED".equals(delivery.getStatus()) || delivery.getAttemptCount() >= 3) return;
    delivery.setNotificationId(notification.getId());
    delivery.setUserId(userId);
    delivery.setChannel(NotificationChannel.WEBHOOK);
    delivery.setStatus("SENDING");
    delivery.setAttemptCount(delivery.getAttemptCount() + 1);
    delivery.setLastAttemptAt(OffsetDateTime.now());
    deliveries.save(delivery);
    try {
      Map<String, Object> body = new HashMap<>();
      body.put("notificationId", notification.getId());
      body.put("userId", userId);
      body.put("type", notification.getType());
      body.put("title", notification.getTitle());
      body.put("content", notification.getContent());
      body.put("createdAt", notification.getCreatedAt());
      restClient.post().uri(webhookUrl).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
      delivery.setStatus("DELIVERED");
      delivery.setDeliveredAt(OffsetDateTime.now());
      delivery.setLastError(null);
    } catch (RuntimeException ex) {
      delivery.setStatus("FAILED");
      String error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
      delivery.setLastError(error.substring(0, Math.min(error.length(), 500)));
      log.warn("Notification webhook delivery failed: notification={}, user={}, attempt={}", notification.getId(), userId, delivery.getAttemptCount());
    }
    deliveries.save(delivery);
  }

  public record PreferenceResponse(NotificationChannel channel, boolean enabled, boolean available) {}
  public record DeliveryResponse(UUID id, UUID notificationId, NotificationChannel channel, String status,
      int attemptCount, OffsetDateTime lastAttemptAt, OffsetDateTime deliveredAt, String lastError) {}
}
