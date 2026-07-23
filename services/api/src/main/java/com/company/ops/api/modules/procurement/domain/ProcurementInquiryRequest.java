package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "procurement_inquiry_requests")
public class ProcurementInquiryRequest extends BaseEntity {

  @Column(name = "inquiry_id", nullable = false)
  private UUID inquiryId;

  @Column(name = "request_id", nullable = false)
  private UUID requestId;

  @Column(name = "requested_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal requestedQty;

  public UUID getInquiryId() {
    return inquiryId;
  }

  public void setInquiryId(UUID inquiryId) {
    this.inquiryId = inquiryId;
  }

  public UUID getRequestId() {
    return requestId;
  }

  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  public BigDecimal getRequestedQty() {
    return requestedQty;
  }

  public void setRequestedQty(BigDecimal requestedQty) {
    this.requestedQty = requestedQty;
  }
}

