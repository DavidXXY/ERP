ALTER TABLE procurement_contracts ADD COLUMN IF NOT EXISTS order_id UUID;
ALTER TABLE procurement_contracts ADD COLUMN IF NOT EXISTS source_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL';
CREATE INDEX IF NOT EXISTS idx_proc_contract_order
  ON procurement_contracts (tenant_id, order_id);
