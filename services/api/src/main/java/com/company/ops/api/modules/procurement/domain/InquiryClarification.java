package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_inquiry_clarifications")
public class InquiryClarification extends BaseEntity {
  @Column(name = "inquiry_id", nullable = false) private UUID inquiryId;
  @Column(name = "supplier_id", nullable = false) private UUID supplierId;
  @Column(name = "account_id", nullable = false) private UUID accountId;
  @Column(nullable = false, length = 1000) private String question;
  @Column(name = "asked_at", nullable = false) private OffsetDateTime askedAt;
  @Column(length = 2000) private String answer;
  @Column(name = "answered_by_name", length = 80) private String answeredByName;
  @Column(name = "answered_at") private OffsetDateTime answeredAt;
  @Column(nullable = false, length = 32) private String status = "OPEN";

  public UUID getInquiryId() { return inquiryId; }
  public void setInquiryId(UUID value) { inquiryId = value; }
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID value) { supplierId = value; }
  public UUID getAccountId() { return accountId; }
  public void setAccountId(UUID value) { accountId = value; }
  public String getQuestion() { return question; }
  public void setQuestion(String value) { question = value; }
  public OffsetDateTime getAskedAt() { return askedAt; }
  public void setAskedAt(OffsetDateTime value) { askedAt = value; }
  public String getAnswer() { return answer; }
  public void setAnswer(String value) { answer = value; }
  public String getAnsweredByName() { return answeredByName; }
  public void setAnsweredByName(String value) { answeredByName = value; }
  public OffsetDateTime getAnsweredAt() { return answeredAt; }
  public void setAnsweredAt(OffsetDateTime value) { answeredAt = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
}
