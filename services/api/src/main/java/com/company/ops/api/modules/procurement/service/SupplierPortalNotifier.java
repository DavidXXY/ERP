package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.procurement.domain.SupplierPortalAccount;
import com.company.ops.api.modules.procurement.domain.SupplierPortalNotification;
import com.company.ops.api.modules.procurement.repository.SupplierPortalAccountRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalNotificationRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierPortalNotifier {
  private final SupplierPortalAccountRepository accounts;
  private final SupplierPortalNotificationRepository notifications;
  private final SupplierPortalEmailService emails;
  private final String portalUrl;

  public SupplierPortalNotifier(
      SupplierPortalAccountRepository accounts,
      SupplierPortalNotificationRepository notifications,
      SupplierPortalEmailService emails,
      @Value("${ops.notifications.portal-url:http://localhost:5176}") String portalUrl
  ) {
    this.accounts = accounts;
    this.notifications = notifications;
    this.emails = emails;
    this.portalUrl = portalUrl;
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
      emails.sendAsync(account.getEmail(), title, content);
    }
  }

  /** 通过邮件送达询价邀请与一次性注册码；返回是否真实发送成功。 */
  @Transactional
  public Boolean deliverInvitation(
      UUID supplierId,
      String to,
      String registrationCode,
      String inquiryCode,
      String deadline
  ) {
    StringBuilder content = new StringBuilder();
    content.append("贵司收到一条新的询价邀请：询价单 ").append(inquiryCode);
    if (deadline != null && !deadline.isBlank()) {
      content.append("，报价截止 ").append(deadline);
    }
    content.append("。\n\n请通过以下地址登录供应商门户处理：").append(portalUrl);
    if (registrationCode != null && !registrationCode.isBlank()) {
      content.append("\n\n贵司尚未注册门户账号，注册时请使用本次邀请注册码：")
          .append(registrationCode)
          .append("（注册码 7 天内有效，仅可绑定一个门户账号）。");
    }
    return emails.send(to, "询价邀请 " + inquiryCode, content.toString());
  }
}
