package com.company.ops.api.modules.finance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fin_revenue_recognitions")
public class RevenueRecognition extends BaseEntity {

  @Column(name = "contract_id", nullable = false)
  private UUID contractId;

  @Column(name = "milestone_id")
  private UUID milestoneId;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "recognize_date", nullable = false)
  private LocalDate recognizeDate;

  @Column(name = "recognized_by", length = 80)
  private String recognizedBy;

  @Column(length = 500)
  private String remark;

  public UUID getContractId() { return contractId; }
  public void setContractId(UUID contractId) { this.contractId = contractId; }
  public UUID getMilestoneId() { return milestoneId; }
  public void setMilestoneId(UUID milestoneId) { this.milestoneId = milestoneId; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public LocalDate getRecognizeDate() { return recognizeDate; }
  public void setRecognizeDate(LocalDate recognizeDate) { this.recognizeDate = recognizeDate; }
  public String getRecognizedBy() { return recognizedBy; }
  public void setRecognizedBy(String recognizedBy) { this.recognizedBy = recognizedBy; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
