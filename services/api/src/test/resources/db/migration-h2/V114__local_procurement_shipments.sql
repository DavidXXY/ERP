CREATE TABLE IF NOT EXISTS procurement_shipments (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  order_id UUID NOT NULL REFERENCES procurement_purchase_orders(id) ON DELETE CASCADE,
  supplier_id UUID NOT NULL,
  delivery_no VARCHAR(80),
  carrier VARCHAR(80),
  expected_arrival DATE,
  remark VARCHAR(500),
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_proc_shipment_order
  ON procurement_shipments (tenant_id, order_id, created_at DESC);
