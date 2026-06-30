package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "crm_quote_revisions")
public class QuoteRevision extends BaseEntity {

  @Column(name = "quote_id", nullable = false)
  private UUID quoteId;

  @Column(name = "version_no", nullable = false)
  private int versionNo;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "service_scope", nullable = false, length = 800)
  private String serviceScope;

  @Column(name = "inspect_cycle", length = 120)
  private String inspectCycle;

  @Column(name = "payment_nodes", length = 300)
  private String paymentNodes;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private QuoteStatus status;

  @Column(name = "revision_note", nullable = false, length = 500)
  private String revisionNote;

  @Column(name = "editor_name", nullable = false, length = 80)
  private String editorName;

  @Column(name = "revised_at", nullable = false)
  private OffsetDateTime revisedAt;

  public UUID getQuoteId() {
    return quoteId;
  }

  public void setQuoteId(UUID quoteId) {
    this.quoteId = quoteId;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public void setVersionNo(int versionNo) {
    this.versionNo = versionNo;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getServiceScope() {
    return serviceScope;
  }

  public void setServiceScope(String serviceScope) {
    this.serviceScope = serviceScope;
  }

  public String getInspectCycle() {
    return inspectCycle;
  }

  public void setInspectCycle(String inspectCycle) {
    this.inspectCycle = inspectCycle;
  }

  public String getPaymentNodes() {
    return paymentNodes;
  }

  public void setPaymentNodes(String paymentNodes) {
    this.paymentNodes = paymentNodes;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public QuoteStatus getStatus() {
    return status;
  }

  public void setStatus(QuoteStatus status) {
    this.status = status;
  }

  public String getRevisionNote() {
    return revisionNote;
  }

  public void setRevisionNote(String revisionNote) {
    this.revisionNote = revisionNote;
  }

  public String getEditorName() {
    return editorName;
  }

  public void setEditorName(String editorName) {
    this.editorName = editorName;
  }

  public OffsetDateTime getRevisedAt() {
    return revisedAt;
  }

  public void setRevisedAt(OffsetDateTime revisedAt) {
    this.revisedAt = revisedAt;
  }
}
