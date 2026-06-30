package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "work_orders")
public class WorkOrder extends BaseEntity {

  @Column(name = "customer_id") private UUID customerId;
  @Column(name = "contract_id") private UUID contractId;
  @Column(name = "project_id") private UUID projectId;
  @Column(name = "equipment_id") private UUID equipmentId;
  @Column(name = "maintenance_plan_id") private UUID maintenancePlanId;
  @Column(nullable = false, length = 64) private String code;
  @Enumerated(EnumType.STRING) @Column(name = "source", nullable = false, length = 80) private WorkOrderSource source;
  @Enumerated(EnumType.STRING) @Column(name = "work_type", nullable = false, length = 80) private WorkOrderType workType;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private WorkOrderPriority priority;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private WorkOrderStatus status;
  @Column(nullable = false, length = 180) private String title;
  @Column(name = "equipment_name", length = 160) private String equipmentName;
  @Column(name = "planned_date") private LocalDate plannedDate;
  @Column(name = "site_address", length = 300) private String siteAddress;
  @Column(name = "problem_description", length = 1000) private String problemDescription;
  @Column(name = "assignee_id") private UUID assigneeId;
  @Column(name = "engineer_name", length = 80) private String engineerName;
  @Column(name = "required_cert", length = 120) private String requiredCertificate;
  @Column(name = "check_in_at") private OffsetDateTime checkInAt;
  @Column(name = "check_in_location", length = 300) private String checkInLocation;
  @Column(name = "started_at") private OffsetDateTime startedAt;
  @Column(name = "completed_at") private OffsetDateTime completedAt;
  @Column(name = "accepted_at") private OffsetDateTime acceptedAt;
  @Column(name = "customer_signer", length = 80) private String customerSigner;
  @Column(name = "service_result", length = 1500) private String serviceResult;
  @Column(name = "acceptance_note", length = 500) private String acceptanceNote;
  @Column(name = "labor_hours", nullable = false, precision = 10, scale = 2) private BigDecimal laborHours = BigDecimal.ZERO;
  @Column(name = "labor_cost", nullable = false, precision = 14, scale = 2) private BigDecimal laborCost = BigDecimal.ZERO;
  @Column(name = "material_cost", nullable = false, precision = 14, scale = 2) private BigDecimal materialCost = BigDecimal.ZERO;
  @Column(name = "travel_cost", nullable = false, precision = 14, scale = 2) private BigDecimal travelCost = BigDecimal.ZERO;
  @Column(name = "outsourcing_cost", nullable = false, precision = 14, scale = 2) private BigDecimal outsourcingCost = BigDecimal.ZERO;
  @Column(name = "cost_amount", nullable = false, precision = 14, scale = 2) private BigDecimal costAmount = BigDecimal.ZERO;
  @Column(name = "billable_amount", nullable = false, precision = 14, scale = 2) private BigDecimal billableAmount = BigDecimal.ZERO;
  @Column(name = "free_warranty", nullable = false) private boolean freeWarranty;

  public UUID getCustomerId() { return customerId; } public void setCustomerId(UUID value) { customerId = value; }
  public UUID getContractId() { return contractId; } public void setContractId(UUID value) { contractId = value; }
  public UUID getProjectId() { return projectId; } public void setProjectId(UUID value) { projectId = value; }
  public UUID getEquipmentId() { return equipmentId; } public void setEquipmentId(UUID value) { equipmentId = value; }
  public UUID getMaintenancePlanId() { return maintenancePlanId; } public void setMaintenancePlanId(UUID value) { maintenancePlanId = value; }
  public String getCode() { return code; } public void setCode(String value) { code = value; }
  public WorkOrderSource getSource() { return source; } public void setSource(WorkOrderSource value) { source = value; }
  public WorkOrderType getWorkType() { return workType; } public void setWorkType(WorkOrderType value) { workType = value; }
  public WorkOrderPriority getPriority() { return priority; } public void setPriority(WorkOrderPriority value) { priority = value; }
  public WorkOrderStatus getStatus() { return status; } public void setStatus(WorkOrderStatus value) { status = value; }
  public String getTitle() { return title; } public void setTitle(String value) { title = value; }
  public String getEquipmentName() { return equipmentName; } public void setEquipmentName(String value) { equipmentName = value; }
  public LocalDate getPlannedDate() { return plannedDate; } public void setPlannedDate(LocalDate value) { plannedDate = value; }
  public String getSiteAddress() { return siteAddress; } public void setSiteAddress(String value) { siteAddress = value; }
  public String getProblemDescription() { return problemDescription; } public void setProblemDescription(String value) { problemDescription = value; }
  public UUID getAssigneeId() { return assigneeId; } public void setAssigneeId(UUID value) { assigneeId = value; }
  public String getEngineerName() { return engineerName; } public void setEngineerName(String value) { engineerName = value; }
  public String getRequiredCertificate() { return requiredCertificate; } public void setRequiredCertificate(String value) { requiredCertificate = value; }
  public OffsetDateTime getCheckInAt() { return checkInAt; } public void setCheckInAt(OffsetDateTime value) { checkInAt = value; }
  public String getCheckInLocation() { return checkInLocation; } public void setCheckInLocation(String value) { checkInLocation = value; }
  public OffsetDateTime getStartedAt() { return startedAt; } public void setStartedAt(OffsetDateTime value) { startedAt = value; }
  public OffsetDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(OffsetDateTime value) { completedAt = value; }
  public OffsetDateTime getAcceptedAt() { return acceptedAt; } public void setAcceptedAt(OffsetDateTime value) { acceptedAt = value; }
  public String getCustomerSigner() { return customerSigner; } public void setCustomerSigner(String value) { customerSigner = value; }
  public String getServiceResult() { return serviceResult; } public void setServiceResult(String value) { serviceResult = value; }
  public String getAcceptanceNote() { return acceptanceNote; } public void setAcceptanceNote(String value) { acceptanceNote = value; }
  public BigDecimal getLaborHours() { return laborHours; } public void setLaborHours(BigDecimal value) { laborHours = value; }
  public BigDecimal getLaborCost() { return laborCost; } public void setLaborCost(BigDecimal value) { laborCost = value; }
  public BigDecimal getMaterialCost() { return materialCost; } public void setMaterialCost(BigDecimal value) { materialCost = value; }
  public BigDecimal getTravelCost() { return travelCost; } public void setTravelCost(BigDecimal value) { travelCost = value; }
  public BigDecimal getOutsourcingCost() { return outsourcingCost; } public void setOutsourcingCost(BigDecimal value) { outsourcingCost = value; }
  public BigDecimal getCostAmount() { return costAmount; } public void setCostAmount(BigDecimal value) { costAmount = value; }
  public BigDecimal getBillableAmount() { return billableAmount; } public void setBillableAmount(BigDecimal value) { billableAmount = value; }
  public boolean isFreeWarranty() { return freeWarranty; } public void setFreeWarranty(boolean value) { freeWarranty = value; }
}
