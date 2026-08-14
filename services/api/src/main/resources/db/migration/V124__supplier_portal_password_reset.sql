ALTER TABLE procurement_supplier_portal_accounts
  ADD COLUMN IF NOT EXISTS reset_token_hash VARCHAR(255),
  ADD COLUMN IF NOT EXISTS reset_token_expires_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS reset_token_used_at TIMESTAMP WITH TIME ZONE;
