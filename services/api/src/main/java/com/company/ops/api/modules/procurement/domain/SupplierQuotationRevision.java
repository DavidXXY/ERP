package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_supplier_quote_revisions")
public class SupplierQuotationRevision extends BaseEntity {
  @Column(name = "quote_id", nullable = false) private UUID quoteId;
  @Column(name = "version_no", nullable = false) private Integer versionNo;
  @Column(name = "submission_source", nullable = false, length = 32) private String submissionSource;
  @Column(name = "submitted_by_type", nullable = false, length = 32) private String submittedByType;
  @Column(name = "submitted_by_id") private UUID submittedById;
  @Column(name = "submitted_by_name", length = 80) private String submittedByName;
  @Column(name = "submitted_at", nullable = false) private OffsetDateTime submittedAt;
  @Column(name = "snapshot_json", nullable = false, columnDefinition = "text") private String snapshotJson;

  public UUID getQuoteId() { return quoteId; }
  public void setQuoteId(UUID quoteId) { this.quoteId = quoteId; }
  public Integer getVersionNo() { return versionNo; }
  public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
  public String getSubmissionSource() { return submissionSource; }
  public void setSubmissionSource(String submissionSource) { this.submissionSource = submissionSource; }
  public String getSubmittedByType() { return submittedByType; }
  public void setSubmittedByType(String submittedByType) { this.submittedByType = submittedByType; }
  public UUID getSubmittedById() { return submittedById; }
  public void setSubmittedById(UUID submittedById) { this.submittedById = submittedById; }
  public String getSubmittedByName() { return submittedByName; }
  public void setSubmittedByName(String submittedByName) { this.submittedByName = submittedByName; }
  public OffsetDateTime getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
  public String getSnapshotJson() { return snapshotJson; }
  public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
}
