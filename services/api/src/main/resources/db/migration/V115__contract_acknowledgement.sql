ALTER TABLE procurement_contracts
  ADD COLUMN IF NOT EXISTS acknowledged_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS acknowledged_by_name VARCHAR(80);
