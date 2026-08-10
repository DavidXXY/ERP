ALTER TABLE procurement_contracts ADD COLUMN IF NOT EXISTS inquiry_id UUID;
ALTER TABLE procurement_contracts ADD COLUMN IF NOT EXISTS selected_quote_id UUID;
CREATE INDEX IF NOT EXISTS idx_proc_contract_inquiry ON procurement_contracts (tenant_id, inquiry_id);
CREATE INDEX IF NOT EXISTS idx_proc_contract_supplier_status ON procurement_contracts (tenant_id, supplier_id, status);
