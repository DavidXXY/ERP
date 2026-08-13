-- 供应商回传发货时的送货单照片/附件
CREATE TABLE IF NOT EXISTS supplier_shipment_attachments (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  shipment_id UUID NOT NULL REFERENCES procurement_shipments(id) ON DELETE CASCADE,
  supplier_id UUID NOT NULL REFERENCES procurement_suppliers(id),
  file_name VARCHAR(255) NOT NULL,
  object_key VARCHAR(500) NOT NULL,
  content_type VARCHAR(120),
  size_bytes BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_shipment_attachment_shipment
  ON supplier_shipment_attachments (tenant_id, shipment_id, created_at DESC);
