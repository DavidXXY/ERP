package com.company.ops.api.modules.governance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "biz_accounting_periods", uniqueConstraints = @UniqueConstraint(
    name = "uk_accounting_period_tenant", columnNames = {"tenant_id", "fiscal_year", "period_no"}))
public class AccountingPeriod extends BaseEntity {
  @Column(name = "fiscal_year", nullable = false) private int fiscalYear;
  @Column(name = "period_no", nullable = false) private int periodNo;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private AccountingPeriodStatus status = AccountingPeriodStatus.OPEN;
  @Column(name = "opened_at", nullable = false) private OffsetDateTime openedAt;
  @Column(name = "closing_started_at") private OffsetDateTime closingStartedAt;
  @Column(name = "closed_at") private OffsetDateTime closedAt;
  @Column(name = "closed_by", length = 80) private String closedBy;
  @Column(name = "close_reason", length = 500) private String closeReason;
  @Column(name = "reopened_at") private OffsetDateTime reopenedAt;
  @Column(name = "reopened_by", length = 80) private String reopenedBy;
  @Column(name = "reopen_reason", length = 500) private String reopenReason;
  @Column(name = "pending_action", length = 32) private String pendingAction;
  @Column(name = "action_requested_by_id") private UUID actionRequestedById;
  @Column(name = "action_requested_by", length = 80) private String actionRequestedBy;
  @Column(name = "action_requested_at") private OffsetDateTime actionRequestedAt;
  @Column(name = "action_request_reason", length = 500) private String actionRequestReason;

  public int getFiscalYear() { return fiscalYear; }
  public void setFiscalYear(int value) { fiscalYear = value; }
  public int getPeriodNo() { return periodNo; }
  public void setPeriodNo(int value) { periodNo = value; }
  public AccountingPeriodStatus getStatus() { return status; }
  public void setStatus(AccountingPeriodStatus value) { status = value; }
  public OffsetDateTime getOpenedAt() { return openedAt; }
  public void setOpenedAt(OffsetDateTime value) { openedAt = value; }
  public OffsetDateTime getClosingStartedAt() { return closingStartedAt; }
  public void setClosingStartedAt(OffsetDateTime value) { closingStartedAt = value; }
  public OffsetDateTime getClosedAt() { return closedAt; }
  public void setClosedAt(OffsetDateTime value) { closedAt = value; }
  public String getClosedBy() { return closedBy; }
  public void setClosedBy(String value) { closedBy = value; }
  public String getCloseReason() { return closeReason; }
  public void setCloseReason(String value) { closeReason = value; }
  public OffsetDateTime getReopenedAt() { return reopenedAt; }
  public void setReopenedAt(OffsetDateTime value) { reopenedAt = value; }
  public String getReopenedBy() { return reopenedBy; }
  public void setReopenedBy(String value) { reopenedBy = value; }
  public String getReopenReason() { return reopenReason; }
  public void setReopenReason(String value) { reopenReason = value; }
  public String getPendingAction() { return pendingAction; }
  public void setPendingAction(String value) { pendingAction = value; }
  public UUID getActionRequestedById() { return actionRequestedById; }
  public void setActionRequestedById(UUID value) { actionRequestedById = value; }
  public String getActionRequestedBy() { return actionRequestedBy; }
  public void setActionRequestedBy(String value) { actionRequestedBy = value; }
  public OffsetDateTime getActionRequestedAt() { return actionRequestedAt; }
  public void setActionRequestedAt(OffsetDateTime value) { actionRequestedAt = value; }
  public String getActionRequestReason() { return actionRequestReason; }
  public void setActionRequestReason(String value) { actionRequestReason = value; }
}
