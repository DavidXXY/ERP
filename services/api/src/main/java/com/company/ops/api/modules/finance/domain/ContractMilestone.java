package com.company.ops.api.modules.finance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fin_contract_milestones")
public class ContractMilestone extends BaseEntity {

  @Column(name = "contract_id", nullable = false)
  private UUID contractId;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "planned_date")
  private LocalDate plannedDate;

  @Column(nullable = false, length = 24)
  private String status = "PENDING";

  @Column(name = "recognized_date")
  private LocalDate recognizedDate;

  @Column(name = "recognized_by", length = 80)
  private String recognizedBy;

  @Column(length = 500)
  private String remark;

  public UUID getContractId() { return contractId; }
  public void setContractId(UUID contractId) { this.contractId = contractId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public LocalDate getPlannedDate() { return plannedDate; }
  public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDate getRecognizedDate() { return recognizedDate; }
  public void setRecognizedDate(LocalDate recognizedDate) { this.recognizedDate = recognizedDate; }
  public String getRecognizedBy() { return recognizedBy; }
  public void setRecognizedBy(String recognizedBy) { this.recognizedBy = recognizedBy; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
