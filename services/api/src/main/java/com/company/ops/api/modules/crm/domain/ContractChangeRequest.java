package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "crm_contract_changes")
public class ContractChangeRequest extends BaseEntity {
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;
    @Column(name = "change_data", columnDefinition = "TEXT")
    private String changeData;
    @Column(length = 500)
    private String reason;
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    @Column(name = "requested_by", nullable = false, length = 80)
    private String requestedBy;
    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;
    @Column(name = "approved_by", length = 80)
    private String approvedBy;
    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
    @Column(name = "approval_comment", columnDefinition = "TEXT")
    private String approvalComment;
    public UUID getContractId() { return contractId; }
    public void setContractId(UUID v) { this.contractId = v; }
    public String getChangeData() { return changeData; }
    public void setChangeData(String v) { this.changeData = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String v) { this.requestedBy = v; }
    public OffsetDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(OffsetDateTime v) { this.requestedAt = v; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String v) { this.approvedBy = v; }
    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime v) { this.approvedAt = v; }
    public String getApprovalComment() { return approvalComment; }
    public void setApprovalComment(String v) { this.approvalComment = v; }
}