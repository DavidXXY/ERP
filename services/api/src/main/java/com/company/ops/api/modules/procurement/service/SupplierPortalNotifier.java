package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.procurement.domain.SupplierPortalAccount;
import com.company.ops.api.modules.procurement.domain.SupplierPortalNotification;
import com.company.ops.api.modules.procurement.repository.SupplierPortalAccountRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalNotificationRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierPortalNotifier {
  private final SupplierPortalAccountRepository accounts;
  private final SupplierPortalNotificationRepository notifications;
  private final SupplierPortalEmailService emails;

  public SupplierPortalNotifier(
      SupplierPortalAccountRepository accounts,
      SupplierPortalNotificationRepository notifications,
      SupplierPortalEmailService emails
  ) {
    this.accounts = accounts;
    this.notifications = notifications;
    this.emails = emails;
  }

  @Transactional
  public void notify(
      UUID supplierId,
      String type,
      String title,
      String content,
      String relatedType,
      UUID relatedId
  ) {
    for (SupplierPortalAccount account : accounts.findBySupplierIdOrderByCreatedAtAsc(supplierId)) {
      if (!"ACTIVE".equals(account.getStatus())) continue;
      SupplierPortalNotification notification = new SupplierPortalNotification();
      notification.setTenantId(TenantContext.currentTenant());
      notification.setAccountId(account.getId());
      notification.setSupplierId(supplierId);
      notification.setType(type);
      notification.setTitle(title);
      notification.setContent(content);
      notification.setRelatedType(relatedType);
      notification.setRelatedId(relatedId);
      notification.setRead(false);
      notifications.save(notification);
      emails.send(account.getEmail(), title, content);
    }
  }
}
