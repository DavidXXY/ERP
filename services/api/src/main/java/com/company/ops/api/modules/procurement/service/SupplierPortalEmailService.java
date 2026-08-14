package com.company.ops.api.modules.procurement.service;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 供应商门户站内通知的邮件通道：SMTP 未配置时静默跳过。 */
@Service
public class SupplierPortalEmailService {

  private static final Logger log = LoggerFactory.getLogger(SupplierPortalEmailService.class);

  private final boolean enabled;
  private final String from;
  private final JavaMailSender mailSender;

  public SupplierPortalEmailService(
      @Value("${ops.notifications.smtp.enabled:false}") boolean enabled,
      @Value("${ops.notifications.smtp.from:}") String from,
      @Value("${ops.notifications.smtp.host:localhost}") String host,
      @Value("${ops.notifications.smtp.port:25}") int port,
      @Value("${ops.notifications.smtp.username:}") String username,
      @Value("${ops.notifications.smtp.password:}") String password,
      ObjectProvider<JavaMailSender> autoConfigured
  ) {
    this.enabled = enabled;
    this.from = StringUtils.hasText(from) ? from.trim() : "ops-erp@localhost";
    if (!enabled) {
      this.mailSender = null;
      return;
    }
    JavaMailSender existing = autoConfigured.getIfAvailable();
    if (existing != null) {
      this.mailSender = existing;
      return;
    }
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    if (StringUtils.hasText(username)) {
      sender.setUsername(username.trim());
    }
    if (StringUtils.hasText(password)) {
      sender.setPassword(password);
    }
    Properties props = sender.getJavaMailProperties();
    props.put("mail.smtp.auth", StringUtils.hasText(username));
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.connectiontimeout", "5000");
    props.put("mail.smtp.timeout", "10000");
    this.mailSender = sender;
  }

  /** 返回 null 表示邮件通道未启用（未尝试发送），true 成功，false 失败。 */
  public Boolean send(String to, String title, String content) {
    if (!enabled || mailSender == null || !StringUtils.hasText(to)) {
      return null;
    }
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(from);
      message.setTo(to.trim());
      message.setSubject(title);
      message.setText(content);
      mailSender.send(message);
      log.info("Supplier notification email sent: to={}, title={}", to, title);
      return true;
    } catch (RuntimeException ex) {
      log.warn("Supplier notification email failed: to={}, title={}, error={}",
          to, title, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
      return false;
    }
  }

  /** 站内通知附带的邮件：异步发送，不阻塞业务接口；不启用 SMTP 时静默跳过。 */
  @Async("mailTaskExecutor")
  public void sendAsync(String to, String title, String content) {
    if (!enabled || mailSender == null || !StringUtils.hasText(to)) return;
    for (int attempt = 1; attempt <= 2; attempt++) {
      try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to.trim());
        message.setSubject(title);
        message.setText(content);
        mailSender.send(message);
        log.info("Supplier notification email sent (async): to={}, title={}", to, title);
        return;
      } catch (RuntimeException ex) {
        if (attempt == 2) {
          log.warn("Supplier notification email failed (async): to={}, title={}, error={}",
              to, title, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
          return;
        }
        try {
          Thread.sleep(1000L * attempt);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }
}
