package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "maintenance_equipment_assets")
public class EquipmentAsset extends BaseEntity {

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "contract_id")
  private UUID contractId;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false, length = 80)
  private String category;

  @Column(length = 120)
  private String model;

  @Column(name = "serial_no", length = 120)
  private String serialNo;

  @Column(name = "site_address", nullable = false, length = 300)
  private String siteAddress;

  @Column(name = "installed_date")
  private LocalDate installedDate;

  @Column(name = "warranty_end_date")
  private LocalDate warrantyEndDate;

  @Column(name = "maintenance_cycle_days", nullable = false)
  private Integer maintenanceCycleDays = 90;

  @Column(name = "last_maintenance_date")
  private LocalDate lastMaintenanceDate;

  @Column(name = "next_maintenance_date")
  private LocalDate nextMaintenanceDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private EquipmentStatus status = EquipmentStatus.ACTIVE;

  @Column(name = "required_certificate", length = 120)
  private String requiredCertificate;

  @Column(length = 500)
  private String notes;

  public UUID getCustomerId() { return customerId; }
  public void setCustomerId(UUID customerId) { this.customerId = customerId; }
  public UUID getContractId() { return contractId; }
  public void setContractId(UUID contractId) { this.contractId = contractId; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public String getModel() { return model; }
  public void setModel(String model) { this.model = model; }
  public String getSerialNo() { return serialNo; }
  public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
  public String getSiteAddress() { return siteAddress; }
  public void setSiteAddress(String siteAddress) { this.siteAddress = siteAddress; }
  public LocalDate getInstalledDate() { return installedDate; }
  public void setInstalledDate(LocalDate installedDate) { this.installedDate = installedDate; }
  public LocalDate getWarrantyEndDate() { return warrantyEndDate; }
  public void setWarrantyEndDate(LocalDate warrantyEndDate) { this.warrantyEndDate = warrantyEndDate; }
  public Integer getMaintenanceCycleDays() { return maintenanceCycleDays; }
  public void setMaintenanceCycleDays(Integer maintenanceCycleDays) { this.maintenanceCycleDays = maintenanceCycleDays; }
  public LocalDate getLastMaintenanceDate() { return lastMaintenanceDate; }
  public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) { this.lastMaintenanceDate = lastMaintenanceDate; }
  public LocalDate getNextMaintenanceDate() { return nextMaintenanceDate; }
  public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) { this.nextMaintenanceDate = nextMaintenanceDate; }
  public EquipmentStatus getStatus() { return status; }
  public void setStatus(EquipmentStatus status) { this.status = status; }
  public String getRequiredCertificate() { return requiredCertificate; }
  public void setRequiredCertificate(String requiredCertificate) { this.requiredCertificate = requiredCertificate; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}
