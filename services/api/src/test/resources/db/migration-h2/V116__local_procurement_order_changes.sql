CREATE TABLE IF NOT EXISTS procurement_order_changes (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  order_id UUID NOT NULL REFERENCES procurement_purchase_orders(id) ON DELETE CASCADE,
  change_no VARCHAR(64) NOT NULL,
  change_type VARCHAR(24) NOT NULL,
  quantity_before NUMERIC(14,2),
  quantity_after NUMERIC(14,2),
  unit_price_before NUMERIC(14,2),
  unit_price_after NUMERIC(14,2),
  expected_date_before DATE,
  expected_date_after DATE,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  created_by_name VARCHAR(80) NOT NULL,
  decided_by_name VARCHAR(80),
  decision_comment VARCHAR(500),
  order_version_before INTEGER NOT NULL,
  order_version_after INTEGER,
  applied_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_proc_order_changes_order
  ON procurement_order_changes (tenant_id, order_id, created_at DESC);

ALTER TABLE procurement_goods_receipts ADD COLUMN IF NOT EXISTS carrier VARCHAR(80);
