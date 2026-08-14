package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端站内通知：供应商在门户提交报价、开票资料或发起申诉等动作时，
 * 向具备采购权限的管理端用户推送站内消息，形成门户与采购端的双向联动。
 */
@Service
public class ProcurementInternalNotifier {
  private final SystemUserRepository users;
  private final SystemNotificationRepository notifications;

  public ProcurementInternalNotifier(
      SystemUserRepository users,
      SystemNotificationRepository notifications
  ) {
    this.users = users;
    this.notifications = notifications;
  }

  @Transactional
  public void notifyProcurementStaff(
      String type,
      String title,
      String content,
      String relatedType,
      UUID relatedId,
      String dedupKey
  ) {
    if (notifications.existsByDedupKey(dedupKey)) return;
    List<SystemUser> recipients = users.findEnabledByPermission("procurement:view");
    if (recipients.isEmpty()) {
      notifications.save(notification(null, type, title, content, relatedType, relatedId, dedupKey));
      return;
    }
    for (SystemUser user : recipients) {
      notifications.save(notification(user.getId(), type, title, content, relatedType, relatedId, dedupKey));
    }
  }

  private SystemNotification notification(
      UUID targetUserId,
      String type,
      String title,
      String content,
      String relatedType,
      UUID relatedId,
      String dedupKey
  ) {
    SystemNotification item = new SystemNotification();
    item.setTargetUserId(targetUserId);
    item.setType(type);
    item.setTitle(title);
    item.setContent(content);
    item.setRelatedType(relatedType);
    item.setRelatedId(relatedId);
    item.setDedupKey(dedupKey);
    item.setRead(false);
    return item;
  }
}
