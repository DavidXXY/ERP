package com.company.ops.api.modules.inventory.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "inventory_serial_numbers")
public class InventorySerialNumber extends BaseEntity {

  @Column(name = "part_id", nullable = false)
  private UUID partId;

  @Column(name = "serial_no", nullable = false, length = 64)
  private String serialNo;

  @Column(name = "batch_no", length = 64)
  private String batchNo;

  @Column(nullable = false, length = 16)
  private String status = "IN_STOCK";

  @Column(name = "inbound_source", length = 64)
  private String inboundSource;

  @Column(name = "outbound_source", length = 64)
  private String outboundSource;

  public UUID getPartId() { return partId; }
  public void setPartId(UUID partId) { this.partId = partId; }
  public String getSerialNo() { return serialNo; }
  public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
  public String getBatchNo() { return batchNo; }
  public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getInboundSource() { return inboundSource; }
  public void setInboundSource(String inboundSource) { this.inboundSource = inboundSource; }
  public String getOutboundSource() { return outboundSource; }
  public void setOutboundSource(String outboundSource) { this.outboundSource = outboundSource; }
}
