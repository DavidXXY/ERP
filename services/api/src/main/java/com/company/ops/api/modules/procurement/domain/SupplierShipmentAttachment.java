package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "supplier_shipment_attachments")
public class SupplierShipmentAttachment extends BaseEntity {
  @Column(name = "shipment_id", nullable = false) private UUID shipmentId;
  @Column(name = "supplier_id", nullable = false) private UUID supplierId;
  @Column(name = "file_name", nullable = false, length = 255) private String fileName;
  @Column(name = "object_key", nullable = false, length = 500) private String objectKey;
  @Column(name = "content_type", length = 120) private String contentType;
  @Column(name = "size_bytes", nullable = false) private long sizeBytes;
  @Column(nullable = false, length = 64) private String sha256;

  public UUID getShipmentId() { return shipmentId; }
  public void setShipmentId(UUID v) { shipmentId = v; }
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID v) { supplierId = v; }
  public String getFileName() { return fileName; }
  public void setFileName(String v) { fileName = v; }
  public String getObjectKey() { return objectKey; }
  public void setObjectKey(String v) { objectKey = v; }
  public String getContentType() { return contentType; }
  public void setContentType(String v) { contentType = v; }
  public long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(long v) { sizeBytes = v; }
  public String getSha256() { return sha256; }
  public void setSha256(String v) { sha256 = v; }
}
